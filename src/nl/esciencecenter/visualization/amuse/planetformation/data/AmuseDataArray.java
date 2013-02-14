package nl.esciencecenter.visualization.amuse.planetformation.data;

import nl.esciencecenter.visualization.amuse.planetformation.glue.Scene;
import nl.esciencecenter.visualization.openglCommon.exceptions.UninitializedException;
import ucar.nc2.NetcdfFile;

public class AmuseDataArray implements Runnable {
    private final NetcdfFile            ncFile_bin, ncFile_gas;
    private boolean                     initialized = false;

    private AmuseParticleDataArray      particles;
    private AmuseGasDataArray           gasses;

    private final AmuseSceneDescription description;
    private final Scene                 glueScene;

    public AmuseDataArray(AmuseSceneDescription description,
            NetcdfFile frameFile_bin, NetcdfFile frameFile_gas) {
        this.description = description;
        this.ncFile_bin = frameFile_bin;
        this.ncFile_gas = frameFile_gas;
        this.glueScene = null;
    }

    public AmuseDataArray(AmuseSceneDescription description, Scene scene) {
        this.description = description;
        this.ncFile_bin = null;
        this.ncFile_gas = null;
        this.glueScene = scene;
    }

    @Override
    public void run() {
        if (!initialized && ncFile_bin != null && ncFile_gas != null
                && glueScene == null) {
            particles = new AmuseParticleDataArray(ncFile_bin);
            gasses = new AmuseGasDataArray(ncFile_gas);

            particles.run();
            gasses.run();

            initialized = true;
        } else {
            particles = new AmuseParticleDataArray(glueScene.getStars());
            gasses = new AmuseGasDataArray(glueScene.getSphGas());

            particles.run();
            gasses.run();

            initialized = true;
        }
    }

    public AmuseSceneDescription getDescription() {
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