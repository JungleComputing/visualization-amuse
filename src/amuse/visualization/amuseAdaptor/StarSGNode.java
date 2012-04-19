package amuse.visualization.amuseAdaptor;

import java.util.HashMap;

import javax.media.opengl.GL3;

import openglCommon.datastructures.Material;
import openglCommon.math.MatF4;
import openglCommon.math.MatrixFMath;
import openglCommon.math.VecF3;
import openglCommon.models.Model;
import openglCommon.scenegraph.SGNode;
import openglCommon.shaders.Program;

public class StarSGNode extends SGNode {
    protected HashMap<Model, Material> materials;

    public StarSGNode() {
        materials = new HashMap<Model, Material>();
    }

    public StarSGNode(StarSGNode other) {
        children = other.children;
        materials = other.materials;
        models = other.models;
        TMatrix = other.TMatrix;
    }

    @Override
    public void draw(GL3 gl, Program program, MatF4 MVMatrix) {
        final MatF4 newM = MVMatrix.mul(TMatrix);

        for (final Model m : models) {
            m.setMaterial(materials.get(m));
            ((Star) m).draw(gl, program, newM);
        }

        for (final SGNode child : children) {
            child.draw(gl, program, newM);
        }
    }

    public void setModel(Model model, Material mat) {
        models.clear();
        models.add(model);

        materials.clear();
        materials.put(model, mat);
    }

    @Override
    public void setTranslation(VecF3 translation) {
        TMatrix = MatrixFMath.translate(translation);
    }
}
