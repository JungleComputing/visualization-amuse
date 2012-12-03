package nl.esciencecenter.visualization.amuse.planetformation.glExt;

import java.nio.FloatBuffer;

import javax.media.opengl.GL3;

import openglCommon.datastructures.GLSLAttrib;
import openglCommon.datastructures.VBO;
import openglCommon.exceptions.UninitializedException;
import openglCommon.shaders.Program;

public class GasModel {
    protected FloatBuffer vertices;
    protected VBO         vbo;
    protected int         numVertices;

    private boolean       initialized = false;

    public GasModel() {
        vertices = null;
        numVertices = 0;
    }

    public void init(GL3 gl) {
        if (!initialized) {
            GLSLAttrib vAttrib = new GLSLAttrib(vertices, "MCvertex",
                    GLSLAttrib.SIZE_FLOAT, 4);

            vbo = new VBO(gl, vAttrib);
        }
        initialized = true;
    }

    public void delete(GL3 gl) {
        vertices = null;

        if (initialized) {
            vbo.delete(gl);
        }
    }

    public VBO getVBO() {
        return vbo;
    }

    public int getNumVertices() {
        return numVertices;
    }

    public void draw(GL3 gl, Program program) {
        try {
            program.use(gl);
        } catch (UninitializedException e) {
            e.printStackTrace();
        }

        vbo.bind(gl);

        program.linkAttribs(gl, vbo.getAttribs());

        gl.glDrawArrays(GL3.GL_TRIANGLES, 0, numVertices);
    }
}
