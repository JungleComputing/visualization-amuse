package nl.esciencecenter.visualization.amuse.planetformation.data;

import java.util.ArrayList;
import java.util.HashMap;

import javax.media.opengl.GL3;

import nl.esciencecenter.visualization.amuse.planetformation.AmuseSettings;
import nl.esciencecenter.visualization.amuse.planetformation.glExt.GasCube;
import nl.esciencecenter.visualization.amuse.planetformation.glExt.GasModel;
import openglCommon.datastructures.Material;
import openglCommon.math.VecF3;
import openglCommon.models.Model;
import openglCommon.models.base.Sphere;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmuseSceneStorage {
    private final static Logger                        logger      = LoggerFactory
                                                                           .getLogger(AmuseSceneStorage.class);
    private final AmuseSettings                        settings    = AmuseSettings
                                                                           .getInstance();

    private AmuseSceneDescription                      oldScene    = null;
    private AmuseSceneDescription                      newScene    = null;
    private HashMap<AmuseSceneDescription, AmuseScene> sceneStorage;

    private final AmuseDatasetManager                  manager;

    private final Model                                starBaseModel;
    private final GasModel                             gasBaseModel;

    private boolean                                    initialized = false;

    public AmuseSceneStorage(AmuseDatasetManager manager) {
        sceneStorage = new HashMap<AmuseSceneDescription, AmuseScene>();
        this.starBaseModel = new Sphere(Material.random(),
                settings.getStarSubdivision(), 1f, new VecF3(0, 0, 0));
        // this.gasBaseModel = new GasSphere(settings.getGasSubdivision(), 1f,
        // new VecF3(0, 0, 0));
        this.gasBaseModel = new GasCube(1f, new VecF3(0, 0, 0));

        this.manager = manager;
    }

    public void init(GL3 gl) {
        if (!initialized) {
            starBaseModel.init(gl);
            gasBaseModel.init(gl);

            initialized = true;
        }
    }

    public synchronized AmuseScene getScene() {
        AmuseScene result = null;

        if (sceneStorage.containsKey(newScene)) {
            sceneStorage.remove(oldScene);

            result = sceneStorage.get(newScene);
        } else {
            result = sceneStorage.get(oldScene);
        }

        return result;
    }

    public synchronized void requestNewConfiguration(
            AmuseSceneDescription newDescription) {
        HashMap<AmuseSceneDescription, AmuseScene> newSceneStore = new HashMap<AmuseSceneDescription, AmuseScene>();

        for (AmuseSceneDescription description : sceneStorage.keySet()) {
            if (description == oldScene || description == newScene) {
                newSceneStore.put(description, sceneStorage.get(description));
            }
        }
        sceneStorage = newSceneStore;

        oldScene = newScene;
        newScene = newDescription;
        if (!sceneStorage.containsValue(newScene)) {
            manager.buildScene(newScene);
        }
    }

    public void setScene(AmuseSceneDescription description,
            AmuseGasOctreeNode root, ArrayList<Star> starList) {
        AmuseScene scene = new AmuseScene(starList, root);

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
}
