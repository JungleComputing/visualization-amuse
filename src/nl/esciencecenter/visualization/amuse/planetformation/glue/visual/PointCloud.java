package nl.esciencecenter.visualization.amuse.planetformation.glue.visual;

import java.nio.FloatBuffer;

import javax.media.opengl.GL3;

import nl.esciencecenter.visualization.amuse.planetformation.glExt.VBO;
import nl.esciencecenter.visualization.openglCommon.datastructures.GLSLAttrib;
import nl.esciencecenter.visualization.openglCommon.shaders.Program;

import com.jogamp.common.nio.Buffers;

public class PointCloud {
    private VBO               vbo;
    private boolean           initialized;

    private final int         numParticles;
    private final FloatBuffer coordinates;
    private final FloatBuffer colors;

    public PointCloud(int numParticles, FloatBuffer coordinates,
            FloatBuffer colors) {
        this.numParticles = numParticles;
        this.coordinates = coordinates;
        this.colors = colors;
    }

    public void init(GL3 gl) {
        System.out.println("coordinates buffer size " + coordinates.capacity());
        System.out.println("colors buffer size " + colors.capacity());

        if (!initialized) {
            GLSLAttrib vAttrib = new GLSLAttrib(coordinates, "MCvertex",
                    Buffers.SIZEOF_FLOAT, 3);

            GLSLAttrib cAttrib = new GLSLAttrib(colors, "MCcolor",
                    Buffers.SIZEOF_FLOAT, 4);

            vbo = new VBO(gl, vAttrib, cAttrib);
            initialized = true;
        }

        System.out.println("post vbo creation");
    }

    public void draw(GL3 gl, Program program) {
        vbo.bind(gl);

        program.linkAttribs(gl, vbo.getAttribs());

        gl.glDrawArrays(GL3.GL_POINTS, 0, numParticles);
    }

    public void dispose(GL3 gl) {
        vbo.delete(gl);
    }

    public int getSize() {
        return numParticles;
    }
}
