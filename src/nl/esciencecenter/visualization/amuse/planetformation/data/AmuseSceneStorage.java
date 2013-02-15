package nl.esciencecenter.visualization.amuse.planetformation.data;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import javax.media.opengl.GL3;

import nl.esciencecenter.visualization.amuse.planetformation.AmuseSettings;
import nl.esciencecenter.visualization.amuse.planetformation.glExt.GasCube;
import nl.esciencecenter.visualization.amuse.planetformation.glExt.GasModel;
import nl.esciencecenter.visualization.amuse.planetformation.interfaces.SceneDescription;
import nl.esciencecenter.visualization.amuse.planetformation.interfaces.SceneStorage;
import nl.esciencecenter.visualization.amuse.planetformation.interfaces.VisualScene;
import nl.esciencecenter.visualization.openglCommon.datastructures.Material;
import nl.esciencecenter.visualization.openglCommon.math.VecF3;
import nl.esciencecenter.visualization.openglCommon.models.Model;
import nl.esciencecenter.visualization.openglCommon.models.Sphere;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jogamp.common.nio.Buffers;

public class AmuseSceneStorage implements SceneStorage {
    private final static Logger                              logger      = LoggerFactory
                                                                                 .getLogger(AmuseSceneStorage.class);
    private final AmuseSettings                              settings    = AmuseSettings
                                                                                 .getInstance();

    private AmuseSceneDescription                            oldScene    = null;
    private AmuseSceneDescription                            newScene    = null;
    private HashMap<AmuseSceneDescription, AmuseScene>       sceneStorage;
    private final HashMap<AmuseSceneDescription, ByteBuffer> legendStorage;

    private final AmuseDatasetManager                        manager;

    private final Model                                      starBaseModel;
    private final GasModel                                   gasBaseModel;

    private boolean                                          initialized = false;

    private final ByteBuffer                                 EMPTY_LEGEND_BUFFER;

    public AmuseSceneStorage(AmuseDatasetManager manager) {
        EMPTY_LEGEND_BUFFER = Buffers.newDirectByteBuffer(1 * 500 * 4);

        sceneStorage = new HashMap<AmuseSceneDescription, AmuseScene>();
        legendStorage = new HashMap<AmuseSceneDescription, ByteBuffer>();

        this.starBaseModel = new Sphere(Material.random(),
                settings.getStarSubdivision(), 1f, new VecF3(0, 0, 0), false);
        // this.gasBaseModel = new GasSphere(settings.getGasSubdivision(), 1f,
        // new VecF3(0, 0, 0));
        this.gasBaseModel = new GasCube(1f, new VecF3(0, 0, 0));

        this.manager = manager;
    }

    @Override
    public void init(GL3 gl) {
        if (!initialized) {
            starBaseModel.init(gl);
            gasBaseModel.init(gl);

            initialized = true;
        }
    }

    @Override
    public synchronized ByteBuffer getLegendImage() {
        ByteBuffer result = null;
        if (legendStorage.containsKey(newScene)) {
            legendStorage.remove(oldScene);

            result = legendStorage.get(newScene);
        } else {
            result = legendStorage.get(oldScene);
        }

        if (result != null) {
            return result;
        } else {
            return EMPTY_LEGEND_BUFFER;
        }
    }

    @Override
    public synchronized VisualScene getScene() {
        AmuseScene result = null;

        if (sceneStorage.containsKey(newScene)) {
            sceneStorage.remove(oldScene);

            result = sceneStorage.get(newScene);
        } else {
            result = sceneStorage.get(oldScene);
        }

        return result;
    }

    @Override
    public synchronized void requestNewConfiguration(
            SceneDescription newDescription) {
        HashMap<AmuseSceneDescription, AmuseScene> newSceneStore = new HashMap<AmuseSceneDescription, AmuseScene>();

        for (AmuseSceneDescription description : sceneStorage.keySet()) {
            if (description == oldScene || description == newScene) {
                newSceneStore.put(description, sceneStorage.get(description));
            }
        }
        sceneStorage = newSceneStore;

        oldScene = newScene;
        newScene = (AmuseSceneDescription) newDescription;
        if (!sceneStorage.containsValue(newScene)) {
            manager.buildScene(newScene);
        }
    }

    public void setScene(AmuseSceneDescription description,
            AmuseGasOctreeNode root, ArrayList<Star> starList) {
        AmuseScene scene = new AmuseScene(description, starList, root);

        sceneStorage.put(description, scene);
    }

    public void setScene(AmuseSceneDescription description,
            float[][] gasParticles, ArrayList<Star> starList) {
        AmuseScene scene = new AmuseScene(description, starList, gasParticles);

        sceneStorage.put(description, scene);
    }

    public boolean doneWithLastRequest() {
        boolean failure = false;

        if (sceneStorage.get(newScene) == null) {
            failure = true;
        }

        return !failure;
    }

    public GasModel getGasBaseModel() {
        return gasBaseModel;
    }

    public Model getStarBaseModel() {
        return starBaseModel;
    }

    public void setScene(AmuseSceneDescription description,
            AmuseGasOctreeNode root, float[][] gasParticles,
            ArrayList<Star> starList) {
        AmuseScene scene = new AmuseScene(description, starList, root,
                gasParticles);

        sceneStorage.put(description, scene);
    }

    public void setLegendImage(AmuseSceneDescription description,
            ByteBuffer outBuf) {
        legendStorage.put(description, outBuf);
    }

    // public void setScene(AmuseSceneDescription description, Scene scene) {
    // sceneStorage.put(description, new AmuseScene(description, scene));
    // }
}
