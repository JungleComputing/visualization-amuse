package nl.esciencecenter.visualization.amuse.planetformation.data;

import java.util.ArrayList;

import javax.media.opengl.GL3;

import nl.esciencecenter.visualization.amuse.planetformation.AmuseSettings;
import nl.esciencecenter.visualization.amuse.planetformation.glExt.GasModel;
import openglCommon.math.MatF4;
import openglCommon.math.MatrixFMath;
import openglCommon.math.VecF3;
import openglCommon.math.VecF4;
import openglCommon.shaders.Program;
import openglCommon.util.InputHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmuseGasOctreeNode {
    private final static Logger        logger   = LoggerFactory
                                                        .getLogger(AmuseGasOctreeNode.class);
    private final static AmuseSettings settings = AmuseSettings.getInstance();

    private static enum Octant {
        PPP, PPN, PNP, PNN, NPP, NPN, NNP, NNN
    };

    private static enum Side {
        FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM
    };

    protected final int                              maxElements;
    protected final ArrayList<AmuseGasOctreeElement> elements;
    protected final VecF3                            center;
    protected final float                            cubeSize;
    protected final int                              depth;
    protected final GasModel                         model;
    protected final MatF4                            TMatrix;
    protected final float                            scale;

    protected AmuseGasOctreeNode                     ppp, ppn, pnp, pnn, npp,
            npn, nnp, nnn;
    protected int                                    childCounter;
    protected boolean                                subdivided = false;
    protected boolean                                drawable   = false;
    protected VecF4                                  color;

    private double                                   total_mass;
    private double                                   total_u;

    private final AmuseSceneDescription              description;

    private final AmuseGasOctreeNode                 parent;
    private final Octant                             myOctant;

    public AmuseGasOctreeNode(AmuseSceneDescription description,
            AmuseGasOctreeNode parent, Octant octant, GasModel baseModel,
            int maxElements, int depth, VecF3 corner, float halfSize) {
        this.description = description;
        this.parent = parent;
        this.myOctant = octant;
        this.model = baseModel;
        this.maxElements = maxElements;
        this.depth = depth;
        this.center = corner.add(new VecF3(halfSize, halfSize, halfSize));
        this.cubeSize = halfSize;
        this.scale = halfSize * 2f;
        this.elements = new ArrayList<AmuseGasOctreeElement>();
        this.childCounter = 0;

        total_mass = 0.0;
        total_u = 0.0;

        if (settings.isGasRandomCenterOffset()) {
            VecF3 random_offset_center = ((new VecF3((float) Math.random(),
                    (float) Math.random(), (float) Math.random()))
                    .mul(halfSize)).add(center);
            this.TMatrix = MatrixFMath.translate(random_offset_center);
        } else {
            this.TMatrix = MatrixFMath.translate(center);

        }
    }

    public void addElement(AmuseGasOctreeElement element) {
        final VecF3 location = element.getCenter();
        final double u = element.getEnergy();
        final double dens = element.getMass();

        if ((location.get(0) > (center.get(0) - cubeSize))
                && (location.get(1) > (center.get(1) - cubeSize))
                && (location.get(2) > (center.get(2) - cubeSize))
                && (location.get(0) < (center.get(0) + cubeSize))
                && (location.get(1) < (center.get(1) + cubeSize))
                && (location.get(2) < (center.get(2) + cubeSize))) {

            if (subdivided) {
                addElementSubdivided(element);
            } else {
                if ((childCounter > maxElements)) {
                    if (depth < settings.getMaxOctreeDepth()) {
                        subDiv();
                        total_u = 0.0;
                        total_mass = 0.0;
                        childCounter = 0;
                    } else {
                        System.out.println("Max division!");
                    }
                } else {
                    elements.add(element);
                    total_u += u;
                    total_mass += dens;

                    childCounter++;
                }
            }
        }
    }

    public void addElementSubdivided(AmuseGasOctreeElement element) {
        VecF3 location = element.getCenter();
        if (location.get(0) < center.get(0)) {
            if (location.get(1) < center.get(1)) {
                if (location.get(2) < center.get(2)) {
                    nnn.addElement(element);
                } else {
                    nnp.addElement(element);
                }
            } else {
                if (location.get(2) < center.get(2)) {
                    npn.addElement(element);
                } else {
                    npp.addElement(element);
                }
            }
        } else {
            if (location.get(1) < center.get(1)) {
                if (location.get(2) < center.get(2)) {
                    pnn.addElement(element);
                } else {
                    pnp.addElement(element);
                }
            } else {
                if (location.get(2) < center.get(2)) {
                    ppn.addElement(element);
                } else {
                    ppp.addElement(element);
                }
            }
        }
    }

    protected void subDiv() {
        float size = cubeSize / 2f;
        ppp = new AmuseGasOctreeNode(description, this, Octant.PPP, model,
                maxElements, depth + 1, center.add(new VecF3(0f, 0f, 0f)), size);
        ppn = new AmuseGasOctreeNode(description, this, Octant.PPN, model,
                maxElements, depth + 1,
                center.add(new VecF3(0f, 0f, -cubeSize)), size);
        pnp = new AmuseGasOctreeNode(description, this, Octant.PNP, model,
                maxElements, depth + 1,
                center.add(new VecF3(0f, -cubeSize, 0f)), size);
        pnn = new AmuseGasOctreeNode(description, this, Octant.PNN, model,
                maxElements, depth + 1, center.add(new VecF3(0f, -cubeSize,
                        -cubeSize)), size);
        npp = new AmuseGasOctreeNode(description, this, Octant.NPP, model,
                maxElements, depth + 1,
                center.add(new VecF3(-cubeSize, 0f, 0f)), size);
        npn = new AmuseGasOctreeNode(description, this, Octant.NPN, model,
                maxElements, depth + 1, center.add(new VecF3(-cubeSize, 0f,
                        -cubeSize)), size);
        nnp = new AmuseGasOctreeNode(description, this, Octant.NNP, model,
                maxElements, depth + 1, center.add(new VecF3(-cubeSize,
                        -cubeSize, 0f)), size);
        nnn = new AmuseGasOctreeNode(description, this, Octant.NNN, model,
                maxElements, depth + 1, center.add(new VecF3(-cubeSize,
                        -cubeSize, -cubeSize)), size);

        for (AmuseGasOctreeElement element : elements) {
            addElementSubdivided(element);
        }

        elements.clear();

        subdivided = true;
    }

    protected Octant octantAt(Side side) {
        switch (myOctant) {
        case PPP:
            switch (side) {
            case FRONT:
            case BACK:
                return Octant.PPN;

            case LEFT:
            case RIGHT:
                return Octant.NPP;

            case BOTTOM:
            case TOP:
                return Octant.PNP;

            default:
                return null;
            }
        case NNN:
            switch (side) {
            case FRONT:
            case BACK:
                return Octant.NNP;

            case LEFT:
            case RIGHT:
                return Octant.PNN;

            case BOTTOM:
            case TOP:
                return Octant.NPN;

            default:
                return null;
            }
        case NNP:
            switch (side) {
            case FRONT:
            case BACK:
                return Octant.NNN;

            case LEFT:
            case RIGHT:
                return Octant.PNP;

            case BOTTOM:
            case TOP:
                return Octant.NPP;

            default:
                return null;
            }
        case NPN:
            switch (side) {
            case FRONT:
            case BACK:
                return Octant.NPP;

            case LEFT:
            case RIGHT:
                return Octant.PPN;

            case BOTTOM:
            case TOP:
                return Octant.NNN;

            default:
                return null;
            }
        case NPP:
            switch (side) {
            case FRONT:
            case BACK:
                return Octant.NPN;

            case LEFT:
            case RIGHT:
                return Octant.PPP;

            case BOTTOM:
            case TOP:
                return Octant.NNP;

            default:
                return null;
            }
        case PNN:
            switch (side) {
            case FRONT:
            case BACK:
                return Octant.PNP;

            case LEFT:
            case RIGHT:
                return Octant.NNN;

            case BOTTOM:
            case TOP:
                return Octant.PPN;

            default:
                return null;
            }
        case PNP:
            switch (side) {
            case FRONT:
            case BACK:
                return Octant.PNN;

            case LEFT:
            case RIGHT:
                return Octant.NNP;

            case BOTTOM:
            case TOP:
                return Octant.PPP;

            default:
                return null;
            }
        case PPN:
            switch (side) {
            case FRONT:
            case BACK:
                return Octant.PPP;

            case LEFT:
            case RIGHT:
                return Octant.NPN;

            case BOTTOM:
            case TOP:
                return Octant.PNN;

            default:
                return null;
            }
        default:
            return null;
        }
    }

    protected AmuseGasOctreeNode getChild(Octant octant) {
        if (subdivided) {
            switch (octant) {
            case NNN:
                return nnn;
            case NNP:
                return nnp;
            case NPN:
                return npn;
            case NPP:
                return npp;
            case PNN:
                return pnn;
            case PNP:
                return pnp;
            case PPN:
                return ppn;
            case PPP:
                return ppp;
            default:
                return null;
            }
        } else {
            return this;
        }
    }

    protected AmuseGasOctreeNode getNeighbourAt(Side side) {
        switch (side) {
        case FRONT:
            switch (myOctant) {
            case NNN:
                return nnp;
            case NNP:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(side).getChild(octantAt(side));
            case NPN:
                return npp;
            case NPP:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(side).getChild(octantAt(side));
            case PNN:
                return pnp;
            case PNP:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(side).getChild(octantAt(side));
            case PPN:
                return ppp;
            case PPP:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(side).getChild(octantAt(side));
            default:
                return null;
            }
        case BACK:
            switch (myOctant) {
            case NNN:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(side).getChild(octantAt(side));
            case NNP:
                return nnn;
            case NPN:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(side).getChild(octantAt(side));
            case NPP:
                return npn;
            case PNN:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(side).getChild(octantAt(side));
            case PNP:
                return pnn;
            case PPN:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(side).getChild(octantAt(side));
            case PPP:
                return ppn;
            default:
                return null;
            }
        case BOTTOM:
            switch (myOctant) {
            case NNN:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(side).getChild(octantAt(side));
            case NNP:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(side).getChild(octantAt(side));
            case NPN:
                return nnn;
            case NPP:
                return nnp;
            case PNN:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(side).getChild(octantAt(side));
            case PNP:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(side).getChild(octantAt(side));
            case PPN:
                return pnn;
            case PPP:
                return pnp;
            default:
                return null;
            }
        case TOP:
            switch (myOctant) {
            case NNN:
                return npn;
            case NNP:
                return npp;
            case NPN:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(side).getChild(octantAt(side));
            case NPP:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(side).getChild(octantAt(side));
            case PNN:
                return ppn;
            case PNP:
                return ppp;
            case PPN:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(side).getChild(octantAt(side));
            case PPP:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(side).getChild(octantAt(side));
            default:
                return null;
            }
        case LEFT:
            switch (myOctant) {
            case NNN:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(side).getChild(octantAt(side));
            case NNP:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(side).getChild(octantAt(side));
            case NPN:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(side).getChild(octantAt(side));
            case NPP:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(side).getChild(octantAt(side));
            case PNN:
                return nnn;
            case PNP:
                return nnp;
            case PPN:
                return npn;
            case PPP:
                return npp;
            default:
                return null;
            }
        case RIGHT:
            switch (myOctant) {
            case NNN:
                return pnn;
            case NNP:
                return pnp;
            case NPN:
                return ppn;
            case NPP:
                return ppp;
            case PNN:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(side).getChild(octantAt(side));
            case PNP:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(side).getChild(octantAt(side));
            case PPN:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(side).getChild(octantAt(side));
            case PPP:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(side).getChild(octantAt(side));
            default:
                return null;
            }
        default:
            return null;
        }
    }

    public void draw(GL3 gl, Program gasProgram, MatF4 MVMatrix) {
        if (subdivided) {
            draw_sorted(gl, gasProgram, MVMatrix);
        } else {
            if (color.get(3) > 0.1f) {
                gasProgram.setUniform("node_scale", scale);
                gasProgram.setUniformVector("node_color", color);
                gasProgram.setUniformMatrix("node_MVmatrix", TMatrix);

                model.draw(gl, gasProgram);
            }
        }
    }

    protected void draw_unsorted(GL3 gl, Program program, MatF4 MVMatrix) {
        nnn.draw(gl, program, MVMatrix);
        pnn.draw(gl, program, MVMatrix);
        npn.draw(gl, program, MVMatrix);
        nnp.draw(gl, program, MVMatrix);
        ppn.draw(gl, program, MVMatrix);
        npp.draw(gl, program, MVMatrix);
        pnp.draw(gl, program, MVMatrix);
        ppp.draw(gl, program, MVMatrix);
    }

    protected void draw_sorted(GL3 gl, Program program, MatF4 MVMatrix) {
        InputHandler inputHandler = InputHandler.getInstance();

        if (inputHandler.getCurrentOctant() == InputHandler.octants.NNN) {
            ppp.draw(gl, program, MVMatrix);

            npp.draw(gl, program, MVMatrix);
            pnp.draw(gl, program, MVMatrix);
            ppn.draw(gl, program, MVMatrix);

            nnp.draw(gl, program, MVMatrix);
            pnn.draw(gl, program, MVMatrix);
            npn.draw(gl, program, MVMatrix);

            nnn.draw(gl, program, MVMatrix);
        } else if (inputHandler.getCurrentOctant() == InputHandler.octants.NNP) {
            ppn.draw(gl, program, MVMatrix);

            npn.draw(gl, program, MVMatrix);
            pnn.draw(gl, program, MVMatrix);
            ppp.draw(gl, program, MVMatrix);

            nnn.draw(gl, program, MVMatrix);
            pnp.draw(gl, program, MVMatrix);
            npp.draw(gl, program, MVMatrix);

            nnp.draw(gl, program, MVMatrix);
        } else if (inputHandler.getCurrentOctant() == InputHandler.octants.NPN) {
            pnp.draw(gl, program, MVMatrix);

            nnp.draw(gl, program, MVMatrix);
            ppp.draw(gl, program, MVMatrix);
            pnn.draw(gl, program, MVMatrix);

            npp.draw(gl, program, MVMatrix);
            ppn.draw(gl, program, MVMatrix);
            nnn.draw(gl, program, MVMatrix);

            npn.draw(gl, program, MVMatrix);
        } else if (inputHandler.getCurrentOctant() == InputHandler.octants.NPP) {
            pnn.draw(gl, program, MVMatrix);

            nnn.draw(gl, program, MVMatrix);
            ppn.draw(gl, program, MVMatrix);
            pnp.draw(gl, program, MVMatrix);

            npn.draw(gl, program, MVMatrix);
            ppp.draw(gl, program, MVMatrix);
            nnp.draw(gl, program, MVMatrix);

            npp.draw(gl, program, MVMatrix);
        } else if (inputHandler.getCurrentOctant() == InputHandler.octants.PNN) {
            npp.draw(gl, program, MVMatrix);

            ppp.draw(gl, program, MVMatrix);
            nnp.draw(gl, program, MVMatrix);
            npn.draw(gl, program, MVMatrix);

            pnp.draw(gl, program, MVMatrix);
            nnn.draw(gl, program, MVMatrix);
            ppn.draw(gl, program, MVMatrix);

            pnn.draw(gl, program, MVMatrix);
        } else if (inputHandler.getCurrentOctant() == InputHandler.octants.PNP) {
            npn.draw(gl, program, MVMatrix);

            ppn.draw(gl, program, MVMatrix);
            nnn.draw(gl, program, MVMatrix);
            npp.draw(gl, program, MVMatrix);

            pnn.draw(gl, program, MVMatrix);
            nnp.draw(gl, program, MVMatrix);
            ppp.draw(gl, program, MVMatrix);

            pnp.draw(gl, program, MVMatrix);
        } else if (inputHandler.getCurrentOctant() == InputHandler.octants.PPN) {
            nnp.draw(gl, program, MVMatrix);

            pnp.draw(gl, program, MVMatrix);
            npp.draw(gl, program, MVMatrix);
            nnn.draw(gl, program, MVMatrix);

            ppp.draw(gl, program, MVMatrix);
            npn.draw(gl, program, MVMatrix);
            pnn.draw(gl, program, MVMatrix);

            ppn.draw(gl, program, MVMatrix);
        } else if (inputHandler.getCurrentOctant() == InputHandler.octants.PPP) {
            nnn.draw(gl, program, MVMatrix);

            pnn.draw(gl, program, MVMatrix);
            npn.draw(gl, program, MVMatrix);
            nnp.draw(gl, program, MVMatrix);

            ppn.draw(gl, program, MVMatrix);
            npp.draw(gl, program, MVMatrix);
            pnp.draw(gl, program, MVMatrix);

            ppp.draw(gl, program, MVMatrix);
        }
    }

    public void finalizeAdding() {
        elements.clear();

        if (subdivided) {
            ppp.finalizeAdding();
            ppn.finalizeAdding();
            pnp.finalizeAdding();
            pnn.finalizeAdding();
            npp.finalizeAdding();
            npn.finalizeAdding();
            nnp.finalizeAdding();
            nnn.finalizeAdding();
        } else {
            float particle_density = (childCounter / (cubeSize * cubeSize * cubeSize));

            color = Astrophysics.gasColor(description, particle_density,
                    (float) total_mass, (float) total_u, childCounter);

            drawable = true;
        }
    }

    public float calcMaxdensity(float current_max) {
        if (subdivided) {
            current_max = ppp.calcMaxdensity(current_max);
            current_max = ppn.calcMaxdensity(current_max);
            current_max = pnp.calcMaxdensity(current_max);
            current_max = pnn.calcMaxdensity(current_max);
            current_max = npp.calcMaxdensity(current_max);
            current_max = npn.calcMaxdensity(current_max);
            current_max = nnp.calcMaxdensity(current_max);
            current_max = nnn.calcMaxdensity(current_max);
        }

        float particle_density = (childCounter / (cubeSize * cubeSize * cubeSize));

        if (current_max < particle_density) {
            return particle_density;
        } else {
            return current_max;
        }
    }
}
