package nl.esciencecenter.visualization.amuse.planetformation.glue.visual;

import java.util.ArrayList;

import javax.media.opengl.GL3;

import nl.esciencecenter.visualization.amuse.planetformation.AmuseSettings;
import nl.esciencecenter.visualization.amuse.planetformation.glue.GlueConstants;
import nl.esciencecenter.visualization.amuse.planetformation.glue.data.GlueSceneDescription;
import nl.esciencecenter.visualization.openglCommon.exceptions.UninitializedException;
import nl.esciencecenter.visualization.openglCommon.input.InputHandler;
import nl.esciencecenter.visualization.openglCommon.math.MatF4;
import nl.esciencecenter.visualization.openglCommon.math.MatrixFMath;
import nl.esciencecenter.visualization.openglCommon.math.VecF3;
import nl.esciencecenter.visualization.openglCommon.math.VecF4;
import nl.esciencecenter.visualization.openglCommon.models.Model;
import nl.esciencecenter.visualization.openglCommon.shaders.ShaderProgram;
import nl.esciencecenter.visualization.openglCommon.swing.ColormapInterpreter;
import nl.esciencecenter.visualization.openglCommon.swing.ColormapInterpreter.Color;
import nl.esciencecenter.visualization.openglCommon.swing.ColormapInterpreter.Dimensions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SPHOctreeNode {
    private final static AmuseSettings settings = AmuseSettings.getInstance();
    private final static Logger        logger   = LoggerFactory
                                                        .getLogger(SPHOctreeNode.class);

    public static enum Octant {
        PPP, PPN, PNP, PNN, NPP, NPN, NNP, NNN
    };

    private class SPHOctreeElement {
        private final VecF3 center;
        private final VecF4 color;

        public SPHOctreeElement(VecF3 center, VecF4 color) {
            this.center = center;
            this.color = color;
        }

        public VecF3 getCenter() {
            return center;
        }

        public VecF4 getColor() {
            return color;
        }
    }

    private final ArrayList<SPHOctreeElement> elements;
    private final VecF3                       center;
    private final float                       cubeSize;
    private final int                         depth;
    private final Model                       baseModel;
    private final MatF4                       TMatrix;
    private final float                       modelScale;

    private SPHOctreeNode                     ppp, ppn, pnp, npp, pnn, npn,
                                              nnp, nnn;
    private int                               childCounter;
    private boolean                           subdivided  = false;
    private boolean                           initialized = false;
    private VecF4                             color;

    private final GlueSceneDescription        description;
    private float                             density;

    public SPHOctreeNode(Model baseModel, int depth, VecF3 center, float size, GlueSceneDescription description) {
        this.baseModel = baseModel;
        this.depth = depth;
        this.center = center;
        this.cubeSize = size;
        this.modelScale = size * 2f;
        this.elements = new ArrayList<SPHOctreeElement>();
        this.childCounter = 0;
        this.description = description;

        this.TMatrix = MatrixFMath.translate(center);
    }

    public void addElement(VecF3 coordinates, VecF4 color) {
        SPHOctreeElement element = new SPHOctreeElement(coordinates, color);
        addElement(element);
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
                if ((childCounter > GlueConstants.MAX_ELEMENTS)) {
                    if (depth < GlueConstants.MAX_OCTREE_DEPTH) {
                        subDiv();
                        childCounter = 0;
                    } else {
                        logger.warn("Octree is maximally divided, please change MAX_OCTREE_DEPTH constant if you are certain.");
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

        VecF3 pppCenter = center
                .add(new VecF3(childSize, childSize, childSize));
        VecF3 ppnCenter = center
                .add(new VecF3(childSize, childSize, -childSize));
        VecF3 pnpCenter = center
                .add(new VecF3(childSize, -childSize, childSize));
        VecF3 nppCenter = center
                .add(new VecF3(-childSize, childSize, childSize));
        VecF3 pnnCenter = center.add(new VecF3(childSize, -childSize,
                -childSize));
        VecF3 npnCenter = center.add(new VecF3(-childSize, childSize,
                -childSize));
        VecF3 nnpCenter = center.add(new VecF3(-childSize, -childSize,
                childSize));
        VecF3 nnnCenter = center.add(new VecF3(-childSize, -childSize,
                -childSize));

        ppp = new SPHOctreeNode(baseModel, depth + 1, pppCenter, childSize, description);
        ppn = new SPHOctreeNode(baseModel, depth + 1, ppnCenter, childSize, description);
        pnp = new SPHOctreeNode(baseModel, depth + 1, pnpCenter, childSize, description);
        npp = new SPHOctreeNode(baseModel, depth + 1, nppCenter, childSize, description);
        pnn = new SPHOctreeNode(baseModel, depth + 1, pnnCenter, childSize, description);
        npn = new SPHOctreeNode(baseModel, depth + 1, npnCenter, childSize, description);
        nnp = new SPHOctreeNode(baseModel, depth + 1, nnpCenter, childSize, description);
        nnn = new SPHOctreeNode(baseModel, depth + 1, nnnCenter, childSize, description);

        for (SPHOctreeElement element : elements) {
            addElementSubdivided(element);
        }

        elements.clear();

        subdivided = true;
    }

    public void draw(GL3 gl, ShaderProgram program) {
        if (subdivided) {
            draw_sorted(gl, program);
        } else {

            float delta = description.getUpperBound() -
                    description.getLowerBound();

            float alpha = 0f;
            if (density > description.getLowerBound() && density <
                    description.getUpperBound()) {
                alpha = density / delta;
            } else if (density >= description.getUpperBound()) {
                alpha = 1f;
            }

            Dimensions d = new Dimensions(description.getLowerBound(), description.getUpperBound());

            Color c = ColormapInterpreter.getColor(description.getColorMap(), d, density);

            VecF4 newColor = new VecF4(c.red, c.green, c.blue, alpha);

            // System.err.println("alpha " + alpha);
            if (alpha > 0.01f) {
                program.setUniform("node_scale", modelScale);
                program.setUniformVector("Color", newColor);
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

    protected void draw_unsorted(GL3 gl, ShaderProgram program) {
        nnn.draw(gl, program);
        pnn.draw(gl, program);
        npn.draw(gl, program);
        nnp.draw(gl, program);
        ppn.draw(gl, program);
        npp.draw(gl, program);
        pnp.draw(gl, program);
        ppp.draw(gl, program);
    }

    protected void draw_sorted(GL3 gl, ShaderProgram program) {
        InputHandler inputHandler = InputHandler.getInstance();

        if (inputHandler.getCurrentViewOctant() == InputHandler.octants.NNN) {
            ppp.draw(gl, program);

            npp.draw(gl, program);
            pnp.draw(gl, program);
            ppn.draw(gl, program);

            nnp.draw(gl, program);
            pnn.draw(gl, program);
            npn.draw(gl, program);

            nnn.draw(gl, program);
        } else if (inputHandler.getCurrentViewOctant() == InputHandler.octants.NNP) {
            ppn.draw(gl, program);

            npn.draw(gl, program);
            pnn.draw(gl, program);
            ppp.draw(gl, program);

            nnn.draw(gl, program);
            pnp.draw(gl, program);
            npp.draw(gl, program);

            nnp.draw(gl, program);
        } else if (inputHandler.getCurrentViewOctant() == InputHandler.octants.NPN) {
            pnp.draw(gl, program);

            nnp.draw(gl, program);
            ppp.draw(gl, program);
            pnn.draw(gl, program);

            npp.draw(gl, program);
            ppn.draw(gl, program);
            nnn.draw(gl, program);

            npn.draw(gl, program);
        } else if (inputHandler.getCurrentViewOctant() == InputHandler.octants.NPP) {
            pnn.draw(gl, program);

            nnn.draw(gl, program);
            ppn.draw(gl, program);
            pnp.draw(gl, program);

            npn.draw(gl, program);
            ppp.draw(gl, program);
            nnp.draw(gl, program);

            npp.draw(gl, program);
        } else if (inputHandler.getCurrentViewOctant() == InputHandler.octants.PNN) {
            npp.draw(gl, program);

            ppp.draw(gl, program);
            nnp.draw(gl, program);
            npn.draw(gl, program);

            pnp.draw(gl, program);
            nnn.draw(gl, program);
            ppn.draw(gl, program);

            pnn.draw(gl, program);
        } else if (inputHandler.getCurrentViewOctant() == InputHandler.octants.PNP) {
            npn.draw(gl, program);

            ppn.draw(gl, program);
            nnn.draw(gl, program);
            npp.draw(gl, program);

            pnn.draw(gl, program);
            nnp.draw(gl, program);
            ppp.draw(gl, program);

            pnp.draw(gl, program);
        } else if (inputHandler.getCurrentViewOctant() == InputHandler.octants.PPN) {
            nnp.draw(gl, program);

            pnp.draw(gl, program);
            npp.draw(gl, program);
            nnn.draw(gl, program);

            ppp.draw(gl, program);
            npn.draw(gl, program);
            pnn.draw(gl, program);

            ppn.draw(gl, program);
        } else if (inputHandler.getCurrentViewOctant() == InputHandler.octants.PPP) {
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
                npp.init();
                pnn.init();
                npn.init();
                nnp.init();
                nnn.init();
            } else {
                VecF4 tmpColor = new VecF4();
                for (SPHOctreeElement element : elements) {
                    tmpColor = tmpColor.add(element.getColor());
                }
                color = tmpColor.div(elements.size());
                density = (elements.size() / (cubeSize * cubeSize * cubeSize)) / 100000;

                // System.err.println("density " + density);
            }

            elements.clear();
            initialized = true;
        }
    }
}
