package amuse.visualization.amuseAdaptor;

import java.util.ArrayList;

import javax.media.opengl.GL3;

import openglCommon.datastructures.Material;
import openglCommon.exceptions.UninitializedException;
import openglCommon.math.MatF4;
import openglCommon.math.MatrixFMath;
import openglCommon.math.VecF3;
import openglCommon.math.VecF4;
import openglCommon.models.Model;
import openglCommon.shaders.Program;
import openglCommon.util.InputHandler;
import amuse.visualization.AmuseSettings;

public class AmuseGasOctreeNode {
    private final static AmuseSettings settings = AmuseSettings.getInstance();

    protected final int maxElements;
    protected final ArrayList<AmuseGasOctreeElement> elements;
    protected final VecF3 center;
    protected final float cubeSize;
    protected final int depth;
    protected final Model model;
    protected final MatF4 TMatrix;
    protected final float scale;

    protected AmuseGasOctreeNode ppp, ppn, pnp, pnn, npp, npn, nnp, nnn;
    protected int childCounter;
    protected boolean subdivided = false;
    protected boolean initialized = false;
    protected boolean drawable = false;
    protected VecF4 color;
    protected int subdivision;

    private double total_u;
    private float density;

    public AmuseGasOctreeNode(Model baseModel, int maxElements, int depth, int subdivision, VecF3 corner, float halfSize) {
        this.model = baseModel;
        this.maxElements = maxElements;
        this.depth = depth;
        this.subdivision = subdivision;
        this.center = corner.add(new VecF3(halfSize, halfSize, halfSize));
        this.cubeSize = halfSize;
        this.TMatrix = MatrixFMath.translate(center);
        this.scale = halfSize * 2f;
        this.elements = new ArrayList<AmuseGasOctreeElement>();
        this.childCounter = 0;

        density = 0f;
        total_u = 0.0;
    }

    public void init(GL3 gl) {
        if (!initialized) {
            model.init(gl);

            if (subdivided) {
                ppp.init(gl);
                ppn.init(gl);
                pnp.init(gl);
                pnn.init(gl);
                npp.init(gl);
                npn.init(gl);
                nnp.init(gl);
                nnn.init(gl);
            }
        }

        initialized = true;
    }

    public void addElement(AmuseGasOctreeElement element) {
        final VecF3 location = element.getCenter();
        final double u = element.getEnergy();

        if ((location.get(0) > (center.get(0) - cubeSize)) && (location.get(1) > (center.get(1) - cubeSize))
                && (location.get(2) > (center.get(2) - cubeSize)) && (location.get(0) < (center.get(0) + cubeSize))
                && (location.get(1) < (center.get(1) + cubeSize)) && (location.get(2) < (center.get(2) + cubeSize))) {
            if ((childCounter > maxElements) && !subdivided) {
                if (depth < settings.getMaxOctreeDepth()) {
                    subDiv();
                    total_u = 0.0;
                } else {
                    System.out.println("Max division!");
                }
            }
            if (subdivided) {
                addElementSubdivided(element);
            } else {
                elements.add(element);
                total_u += u;
            }
            childCounter++;
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
        ppp = new AmuseGasOctreeNode(model, maxElements, depth + 1, subdivision, center.add(new VecF3(0f, 0f, 0f)),
                size);
        ppn = new AmuseGasOctreeNode(model, maxElements, depth + 1, subdivision,
                center.add(new VecF3(0f, 0f, -cubeSize)), size);
        pnp = new AmuseGasOctreeNode(model, maxElements, depth + 1, subdivision,
                center.add(new VecF3(0f, -cubeSize, 0f)), size);
        pnn = new AmuseGasOctreeNode(model, maxElements, depth + 1, subdivision, center.add(new VecF3(0f, -cubeSize,
                -cubeSize)), size);
        npp = new AmuseGasOctreeNode(model, maxElements, depth + 1, subdivision,
                center.add(new VecF3(-cubeSize, 0f, 0f)), size);
        npn = new AmuseGasOctreeNode(model, maxElements, depth + 1, subdivision, center.add(new VecF3(-cubeSize, 0f,
                -cubeSize)), size);
        nnp = new AmuseGasOctreeNode(model, maxElements, depth + 1, subdivision, center.add(new VecF3(-cubeSize,
                -cubeSize, 0f)), size);
        nnn = new AmuseGasOctreeNode(model, maxElements, depth + 1, subdivision, center.add(new VecF3(-cubeSize,
                -cubeSize, -cubeSize)), size);

        for (AmuseGasOctreeElement element : elements) {
            addElementSubdivided(element);
        }

        elements.clear();

        subdivided = true;
    }

    public void draw(GL3 gl, Program program, MatF4 MVMatrix) throws UninitializedException {
        if (initialized) {
            if (subdivided) {
                draw_sorted(gl, program, MVMatrix);
            } else {
                if (density > settings.getEpsilon()) {
                    model.setScale(scale);

                    Material material = model.getMaterial();
                    material.setColor(color);
                    material.setTransparency(density * settings.getGasOpacityFactor());
                    model.setMaterial(material);

                    MatF4 newM = MVMatrix.mul(TMatrix);

                    model.draw(gl, program, newM);
                }
            }
        } else {
            throw new UninitializedException();
        }
    }

    protected void draw_unsorted(GL3 gl, Program program, MatF4 MVMatrix) throws UninitializedException {
        nnn.draw(gl, program, MVMatrix);
        pnn.draw(gl, program, MVMatrix);
        npn.draw(gl, program, MVMatrix);
        nnp.draw(gl, program, MVMatrix);
        ppn.draw(gl, program, MVMatrix);
        npp.draw(gl, program, MVMatrix);
        pnp.draw(gl, program, MVMatrix);
        ppp.draw(gl, program, MVMatrix);
    }

    protected void draw_sorted(GL3 gl, Program program, MatF4 MVMatrix) throws UninitializedException {
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
            density = (childCounter / (cubeSize * cubeSize * cubeSize));
            color = Astrophysics.gasColor(density, (float) total_u, childCounter);

            drawable = true;
        }
    }

    // public void finalizeAdding(ArrayList<Star> stars) {
    // if (subdivided) {
    // ppp.finalizeAdding(stars);
    // ppn.finalizeAdding(stars);
    // pnp.finalizeAdding(stars);
    // pnn.finalizeAdding(stars);
    // npp.finalizeAdding(stars);
    // npn.finalizeAdding(stars);
    // nnp.finalizeAdding(stars);
    // nnn.finalizeAdding(stars);
    // } else {
    // density = (childCounter / (cubeSize * cubeSize * cubeSize));
    //
    // VecF3 finalColor = new VecF3();
    // final HashMap<VecF3, Float> effectiveColors = new HashMap<VecF3,
    // Float>();
    // float totalEffectiveColor = 0f;
    //
    // // System.out.println("New Gas particle ------------------");
    //
    // for (final Star s : stars) {
    // final VecF3 location = s.getLocation();
    // final float distance = VectorFMath.length(location.sub(center));
    // final float radius = s.getRadius();
    // final float effectFactor = radius / distance;
    //
    // if (effectFactor > 0.01f) {
    //
    // final VecF3 color = s.getColor().stripAlpha();
    // if (effectiveColors.containsKey(color)) {
    // final float newFactor = effectiveColors.get(color) + effectFactor;
    // effectiveColors.put(color, newFactor);
    // } else {
    // effectiveColors.put(color, effectFactor);
    // }
    // totalEffectiveColor += effectFactor;
    // }
    // }
    //
    // for (final Map.Entry<VecF3, Float> entry : effectiveColors.entrySet()) {
    // VecF3 color = entry.getKey();
    // final float factor = entry.getValue() / totalEffectiveColor;
    //
    // color = color.mul(factor);
    //
    // finalColor = finalColor.add(color);
    // }
    //
    // color = new VecF4(finalColor, density);
    // drawable = true;
    // }
    // }
}
