package nl.esciencecenter.visualization.amuse.planetformation.data;

import java.util.ArrayList;

import nl.esciencecenter.visualization.amuse.planetformation.AmuseSettings;
import openglCommon.math.VecF3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmuseSceneBuilder implements Runnable {
    private final static Logger         logger   = LoggerFactory
                                                         .getLogger(AmuseSceneBuilder.class);
    private final AmuseSettings         settings = AmuseSettings.getInstance();

    private final AmuseSceneStorage     sceneStore;

    private final AmuseSceneDescription description;
    private final float[][]             particles, gas;
    private boolean                     initialized;

    public AmuseSceneBuilder(AmuseSceneStorage sceneStore,
            AmuseSceneDescription description, float[][] particles,
            float[][] gas) {
        this.description = description;
        this.sceneStore = sceneStore;
        this.particles = particles;
        this.gas = gas;
    }

    @Override
    public void run() {
        if (!initialized) {
            final int gasParticlesPerOctreeNode = settings
                    .getGasParticlesPerOctreeNode();
            final float edge = settings.getOctreeEdges();

            AmuseGasOctreeNode root = new AmuseGasOctreeNode(description, null,
                    null, sceneStore.getGasBaseModel(),
                    gasParticlesPerOctreeNode, 0,
                    new VecF3(-edge, -edge, -edge), edge);

            for (float[] gasElement : gas) {
                VecF3 location = Astrophysics.auLocationToScreenCoord(
                        gasElement[0], gasElement[1], gasElement[2]);
                float energy = gasElement[3];
                float density = gasElement[4];

                root.addElement(new AmuseGasOctreeElement(location, energy,
                        density));
            }

            root.finalizeAdding();

            ArrayList<Star> starList = new ArrayList<Star>();

            for (float[] particlesElement : particles) {
                VecF3 location = Astrophysics.auLocationToScreenCoord(
                        particlesElement[0], particlesElement[1],
                        particlesElement[2]);
                float radius = particlesElement[3];

                logger.debug("adding star with radius " + radius + " at "
                        + location);

                Star s = new Star(sceneStore.getStarBaseModel(), location,
                        settings.getStarDefaultLuminosity(), radius);

                starList.add(s);
            }

            sceneStore.setScene(description, root, starList);

            initialized = true;
        }
    }
}
