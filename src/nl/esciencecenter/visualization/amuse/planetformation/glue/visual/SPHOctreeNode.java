package nl.esciencecenter.visualization.amuse.planetformation.glue.visual;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.media.opengl.GL3;

import nl.esciencecenter.visualization.amuse.planetformation.glue.data.GlueSceneDescription;
import nl.esciencecenter.visualization.openglCommon.exceptions.UninitializedException;
import nl.esciencecenter.visualization.openglCommon.input.InputHandler;
import nl.esciencecenter.visualization.openglCommon.math.MatF4;
import nl.esciencecenter.visualization.openglCommon.math.MatrixFMath;
import nl.esciencecenter.visualization.openglCommon.math.VecF3;
import nl.esciencecenter.visualization.openglCommon.math.VecF4;
import nl.esciencecenter.visualization.openglCommon.models.Model;
import nl.esciencecenter.visualization.openglCommon.scenegraph.OctreeElement;
import nl.esciencecenter.visualization.openglCommon.shaders.Program;

public class SPHOctreeNode {
    private final int MAX_OCTREE_DEPTH = 25;

    private static enum Octant {
        PPP, PPN, PNP, PNN, NPP, NPN, NNP, NNN
    };

    private class SPHOctreeElement extends OctreeElement {
        private final VecF4 color;

        public SPHOctreeElement(VecF3 center, VecF4 color) {
            super(center);
            this.color = color;
        }

        public VecF4 getColor() {
            return color;
        }
    }

    protected final int                         maxElements;
    protected final ArrayList<SPHOctreeElement> elements;
    protected final VecF3                       center;
    protected final float                       cubeSize;
    protected final int                         depth;
    protected final Model                       baseModel;
    protected final MatF4                       TMatrix;
    protected final float                       modelScale;

    protected SPHOctreeNode                     ppp, ppn, pnp, pnn, npp, npn,
            nnp, nnn;
    protected int                               childCounter;
    protected boolean                           subdivided  = false;
    protected boolean                           initialized = false;
    protected VecF4                             color;

    private final GlueSceneDescription          description;

    public SPHOctreeNode(GlueSceneDescription description,
            SPHOctreeNode parent, Octant octant, Model baseModel,
            int maxElements, int depth, VecF3 corner, float size) {
        this.description = description;
        this.baseModel = baseModel;
        this.maxElements = maxElements;
        this.depth = depth;
        this.center = corner.add(new VecF3(size, size, size));
        this.cubeSize = size;
        this.modelScale = size * 2f;
        this.elements = new ArrayList<SPHOctreeElement>();
        this.childCounter = 0;

        this.TMatrix = MatrixFMath.translate(center);
    }

    public void addElement(SPHOctreeElement element) {
        final VecF3 location = element.getCenter();

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
                    if (depth < MAX_OCTREE_DEPTH) {
                        subDiv();
                        childCounter = 0;
                    } else {
                        System.out
                                .println("Octree is maximally divided, please change MAX_OCTREE_DEPTH constant if you are certain.");
                    }
                } else {
                    elements.add(element);

                    childCounter++;
                }
            }
        }
    }

    public void addElementSubdivided(SPHOctreeElement element) {
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
        float childSize = cubeSize / 2f;

        ppp = new SPHOctreeNode(description, this, Octant.PPP, baseModel,
                maxElements, depth + 1, center.add(new VecF3(0f, 0f, 0f)),
                childSize);
        ppn = new SPHOctreeNode(description, this, Octant.PPN, baseModel,
                maxElements, depth + 1,
                center.add(new VecF3(0f, 0f, -cubeSize)), childSize);
        pnp = new SPHOctreeNode(description, this, Octant.PNP, baseModel,
                maxElements, depth + 1,
                center.add(new VecF3(0f, -cubeSize, 0f)), childSize);
        pnn = new SPHOctreeNode(description, this, Octant.PNN, baseModel,
                maxElements, depth + 1, center.add(new VecF3(0f, -cubeSize,
                        -cubeSize)), childSize);
        npp = new SPHOctreeNode(description, this, Octant.NPP, baseModel,
                maxElements, depth + 1,
                center.add(new VecF3(-cubeSize, 0f, 0f)), childSize);
        npn = new SPHOctreeNode(description, this, Octant.NPN, baseModel,
                maxElements, depth + 1, center.add(new VecF3(-cubeSize, 0f,
                        -cubeSize)), childSize);
        nnp = new SPHOctreeNode(description, this, Octant.NNP, baseModel,
                maxElements, depth + 1, center.add(new VecF3(-cubeSize,
                        -cubeSize, 0f)), childSize);
        nnn = new SPHOctreeNode(description, this, Octant.NNN, baseModel,
                maxElements, depth + 1, center.add(new VecF3(-cubeSize,
                        -cubeSize, -cubeSize)), childSize);

        for (SPHOctreeElement element : elements) {
            addElementSubdivided(element);
        }

        elements.clear();

        subdivided = true;
    }

    protected SPHOctreeNode getChild(Octant octant) {
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

    public void draw(GL3 gl, Program program) {
        if (subdivided) {
            draw_sorted(gl, program);
        } else {
            float alpha = color.get(3);
            if (alpha > 0.01f) {
                program.setUniform("node_scale", modelScale);
                program.setUniformVector("node_color", color);
                program.setUniformMatrix("node_MVmatrix", TMatrix);

                try {
                    program.use(gl);
                } catch (UninitializedException e) {
                    e.printStackTrace();
                }

                baseModel.draw(gl, program);
            }
        }
    }

    protected void draw_unsorted(GL3 gl, Program program) {
        nnn.draw(gl, program);
        pnn.draw(gl, program);
        npn.draw(gl, program);
        nnp.draw(gl, program);
        ppn.draw(gl, program);
        npp.draw(gl, program);
        pnp.draw(gl, program);
        ppp.draw(gl, program);
    }

    protected void draw_sorted(GL3 gl, Program program) {
        InputHandler inputHandler = InputHandler.getInstance();

        if (inputHandler.getCurrentOctant() == InputHandler.octants.NNN) {
            ppp.draw(gl, program);

            npp.draw(gl, program);
            pnp.draw(gl, program);
            ppn.draw(gl, program);

            nnp.draw(gl, program);
            pnn.draw(gl, program);
            npn.draw(gl, program);

            nnn.draw(gl, program);
        } else if (inputHandler.getCurrentOctant() == InputHandler.octants.NNP) {
            ppn.draw(gl, program);

            npn.draw(gl, program);
            pnn.draw(gl, program);
            ppp.draw(gl, program);

            nnn.draw(gl, program);
            pnp.draw(gl, program);
            npp.draw(gl, program);

            nnp.draw(gl, program);
        } else if (inputHandler.getCurrentOctant() == InputHandler.octants.NPN) {
            pnp.draw(gl, program);

            nnp.draw(gl, program);
            ppp.draw(gl, program);
            pnn.draw(gl, program);

            npp.draw(gl, program);
            ppn.draw(gl, program);
            nnn.draw(gl, program);

            npn.draw(gl, program);
        } else if (inputHandler.getCurrentOctant() == InputHandler.octants.NPP) {
            pnn.draw(gl, program);

            nnn.draw(gl, program);
            ppn.draw(gl, program);
            pnp.draw(gl, program);

            npn.draw(gl, program);
            ppp.draw(gl, program);
            nnp.draw(gl, program);

            npp.draw(gl, program);
        } else if (inputHandler.getCurrentOctant() == InputHandler.octants.PNN) {
            npp.draw(gl, program);

            ppp.draw(gl, program);
            nnp.draw(gl, program);
            npn.draw(gl, program);

            pnp.draw(gl, program);
            nnn.draw(gl, program);
            ppn.draw(gl, program);

            pnn.draw(gl, program);
        } else if (inputHandler.getCurrentOctant() == InputHandler.octants.PNP) {
            npn.draw(gl, program);

            ppn.draw(gl, program);
            nnn.draw(gl, program);
            npp.draw(gl, program);

            pnn.draw(gl, program);
            nnp.draw(gl, program);
            ppp.draw(gl, program);

            pnp.draw(gl, program);
        } else if (inputHandler.getCurrentOctant() == InputHandler.octants.PPN) {
            nnp.draw(gl, program);

            pnp.draw(gl, program);
            npp.draw(gl, program);
            nnn.draw(gl, program);

            ppp.draw(gl, program);
            npn.draw(gl, program);
            pnn.draw(gl, program);

            ppn.draw(gl, program);
        } else if (inputHandler.getCurrentOctant() == InputHandler.octants.PPP) {
            nnn.draw(gl, program);

            pnn.draw(gl, program);
            npn.draw(gl, program);
            nnp.draw(gl, program);

            ppn.draw(gl, program);
            npp.draw(gl, program);
            pnp.draw(gl, program);

            ppp.draw(gl, program);
        }
    }

    public void init() {
        if (!initialized) {
            if (subdivided) {
                ppp.init();
                ppn.init();
                pnp.init();
                pnn.init();
                npp.init();
                npn.init();
                nnp.init();
                nnn.init();
            } else {
                // float particle_density = (childCounter / (scale * scale *
                // scale));

                VecF4 tmpColor = new VecF4();
                for (SPHOctreeElement element : elements) {
                    tmpColor.add(element.getColor());
                }
                color = tmpColor.div(elements.size());
            }

            elements.clear();
            initialized = true;
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

    public FloatBuffer getColor(float[] gasParticle) {
        if (subdivided) {
            if (gasParticle[0] < center.get(0)) {
                if (gasParticle[1] < center.get(1)) {
                    if (gasParticle[2] < center.get(2)) {
                        return nnn.getColor(gasParticle);
                    } else {
                        return nnp.getColor(gasParticle);
                    }
                } else {
                    if (gasParticle[2] < center.get(2)) {
                        return npn.getColor(gasParticle);
                    } else {
                        return npp.getColor(gasParticle);
                    }
                }
            } else {
                if (gasParticle[1] < center.get(1)) {
                    if (gasParticle[2] < center.get(2)) {
                        return pnn.getColor(gasParticle);
                    } else {
                        return pnp.getColor(gasParticle);
                    }
                } else {
                    if (gasParticle[2] < center.get(2)) {
                        return ppn.getColor(gasParticle);
                    } else {
                        return ppp.getColor(gasParticle);
                    }
                }
            }
        } else {
            return color.asBuffer();
        }
    }
}
