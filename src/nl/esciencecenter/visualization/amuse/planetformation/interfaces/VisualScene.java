package nl.esciencecenter.visualization.amuse.planetformation.interfaces;

import javax.media.opengl.GL3;

import nl.esciencecenter.visualization.openglCommon.math.MatF4;
import nl.esciencecenter.visualization.openglCommon.shaders.ShaderProgram;

public interface VisualScene {

    void dispose(GL3 gl);

    void drawGasPointCloud(GL3 gl, ShaderProgram program, MatF4 mv);

    void drawSpheres(GL3 gl, ShaderProgram program, MatF4 mv);

    void drawStars(GL3 gl, ShaderProgram program, MatF4 mv);

    void drawGasOctree(GL3 gl, ShaderProgram program, MatF4 mv);

    SceneDescription getDescription();

}
