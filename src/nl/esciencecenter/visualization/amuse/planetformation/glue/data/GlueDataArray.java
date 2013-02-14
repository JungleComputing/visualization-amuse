package nl.esciencecenter.visualization.amuse.planetformation.glue.data;

import nl.esciencecenter.visualization.amuse.planetformation.glue.Scene;
import nl.esciencecenter.visualization.openglCommon.exceptions.UninitializedException;
import ucar.nc2.NetcdfFile;

public class GlueDataArray implements Runnable {
    private final NetcdfFile            ncFile_bin, ncFile_gas;
    private boolean                     initialized = false;

    private GlueParticleArray      particles;
    private AmuseGasDataArray           gasses;

    private final GlueSceneDescription description;
    private final Scene                 glueScene;

    public GlueDataArray(GlueSceneDescription description,
            NetcdfFile frameFile_bin, NetcdfFile frameFile_gas) {
        this.description = description;
        this.ncFile_bin = frameFile_bin;
        this.ncFile_gas = frameFile_gas;
        this.glueScene = null;
    }

    public GlueDataArray(GlueSceneDescription description, Scene scene) {
        this.description = description;
        this.ncFile_bin = null;
        this.ncFile_gas = null;
        this.glueScene = scene;
    }

    @Override
    public void run() {
        if (!initialized && ncFile_bin != null && ncFile_gas != null
                && glueScene == null) {
            particles = new GlueParticleArray(ncFile_bin);
            gasses = new AmuseGasDataArray(ncFile_gas);

            particles.run();
            gasses.run();

            initialized = true;
        } else {
            particles = new GlueParticleArray(glueScene.getStars());
            gasses = new AmuseGasDataArray(glueScene.getSphGas());

            particles.run();
            gasses.run();

            initialized = true;
        }
    }

    public GlueSceneDescription getDescription() {
        return description;
    }

    public float[][] getGasData() throws UninitializedException {
        if (initialized) {
            return gasses.getData();
        }

        throw new UninitializedException();
    }

    public float[][] getParticleData() throws UninitializedException {
        if (initialized) {
            return particles.getData();
        }

        throw new UninitializedException();
    }
}