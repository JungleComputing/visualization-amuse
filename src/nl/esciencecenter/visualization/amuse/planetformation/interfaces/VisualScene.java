package nl.esciencecenter.visualization.amuse.planetformation.interfaces;

import javax.media.opengl.GL3;

import nl.esciencecenter.visualization.openglCommon.math.MatF4;
import nl.esciencecenter.visualization.openglCommon.shaders.Program;

public interface VisualScene {

    void dispose(GL3 gl);

    void drawGasPointCloud(GL3 gl, Program gasShader, MatF4 mv);

    void drawStars(GL3 gl, Program animatedTurbulenceShader, MatF4 mv);

    SceneDescription getDescription();

}
