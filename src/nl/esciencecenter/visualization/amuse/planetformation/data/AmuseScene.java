package nl.esciencecenter.visualization.amuse.planetformation.data;

import java.util.ArrayList;

import javax.media.opengl.GL3;

import nl.esciencecenter.visualization.amuse.planetformation.AmuseSettings;
import openglCommon.math.MatF4;
import openglCommon.shaders.Program;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmuseScene {
    private final static AmuseSettings settings = AmuseSettings.getInstance();
    private final static Logger        logger   = LoggerFactory
                                                        .getLogger(AmuseScene.class);

    private final ArrayList<Star>      stars;
    private final AmuseGasOctreeNode   gasOctree;

    public AmuseScene(ArrayList<Star> stars, AmuseGasOctreeNode gasOctree) {
        this.stars = stars;
        this.gasOctree = gasOctree;

        logger.debug("Scene constructed with " + stars.size()
                + " particles and " + gasOctree.childCounter + " gasses.");
    }

    public synchronized void drawStars(GL3 gl, Program starProgram,
            MatF4 MVMatrix) {
        for (Star s : stars) {
            s.draw(gl, starProgram, MVMatrix);
        }
    }

    public synchronized void drawGas(GL3 gl, Program gasProgram, MatF4 MVMatrix) {
        gasProgram.setUniformMatrix("global_MVMatrix", MVMatrix);
        gasProgram.setUniform("gas_opacity_factor",
                settings.getGasOpacityFactor());

        gasOctree.draw(gl, gasProgram, MVMatrix);
    }
}
