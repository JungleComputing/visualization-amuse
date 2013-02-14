package nl.esciencecenter.visualization.amuse.planetformation.glue.data;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import javax.media.opengl.GL3;

import nl.esciencecenter.visualization.amuse.planetformation.AmuseSettings;
import nl.esciencecenter.visualization.amuse.planetformation.data.AmuseGasOctreeNode;
import nl.esciencecenter.visualization.amuse.planetformation.glExt.GasCube;
import nl.esciencecenter.visualization.amuse.planetformation.glExt.GasModel;
import nl.esciencecenter.visualization.amuse.planetformation.glue.Scene;
import nl.esciencecenter.visualization.openglCommon.datastructures.Material;
import nl.esciencecenter.visualization.openglCommon.math.VecF3;
import nl.esciencecenter.visualization.openglCommon.models.Model;
import nl.esciencecenter.visualization.openglCommon.models.Sphere;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jogamp.common.nio.Buffers;

public class AmuseSceneStorage {
    private final static Logger                              logger      = LoggerFactory
                                                                                 .getLogger(AmuseSceneStorage.class);
    private final AmuseSettings                              settings    = AmuseSettings
                                                                                 .getInstance();

    private GlueSceneDescription                            oldScene    = null;
    private GlueSceneDescription                            newScene    = null;
    private HashMap<GlueSceneDescription, GlueScene>       sceneStorage;
    private final HashMap<GlueSceneDescription, ByteBuffer> legendStorage;

    private final GlueDatasetManager                         manager;

    private final Model                                      starBaseModel;
    private final GasModel                                   gasBaseModel;

    private boolean                                          initialized = false;

    private final ByteBuffer                                 EMPTY_LEGEND_BUFFER;

    public AmuseSceneStorage(GlueDatasetManager manager) {
        EMPTY_LEGEND_BUFFER = Buffers.newDirectByteBuffer(1 * 500 * 4);

        sceneStorage = new HashMap<GlueSceneDescription, GlueScene>();
        legendStorage = new HashMap<GlueSceneDescription, ByteBuffer>();

        this.starBaseModel = new Sphere(Material.random(),
                settings.getStarSubdivision(), 1f, new VecF3(0, 0, 0), false);
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

    public synchronized GlueScene getScene() {
        GlueScene result = null;

        if (sceneStorage.containsKey(newScene)) {
            sceneStorage.remove(oldScene);

            result = sceneStorage.get(newScene);
        } else {
            result = sceneStorage.get(oldScene);
        }

        return result;
    }

    public synchronized void requestNewConfiguration(
            GlueSceneDescription newDescription) {
        HashMap<GlueSceneDescription, GlueScene> newSceneStore = new HashMap<GlueSceneDescription, GlueScene>();

        for (GlueSceneDescription description : sceneStorage.keySet()) {
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

    public void setScene(GlueSceneDescription description,
            AmuseGasOctreeNode root, ArrayList<StarModel> starList) {
        GlueScene scene = new GlueScene(description, starList, root);

        sceneStorage.put(description, scene);
    }

    public void setScene(GlueSceneDescription description,
            float[][] gasParticles, ArrayList<StarModel> starList) {
        GlueScene scene = new GlueScene(description, starList, gasParticles);

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

    public void setScene(GlueSceneDescription description,
            AmuseGasOctreeNode root, float[][] gasParticles,
            ArrayList<StarModel> starList) {
        GlueScene scene = new GlueScene(description, starList, root,
                gasParticles);

        sceneStorage.put(description, scene);
    }

    public void setLegendImage(GlueSceneDescription description,
            ByteBuffer outBuf) {
        legendStorage.put(description, outBuf);
    }

    public void setScene(GlueSceneDescription description, Scene scene) {
        sceneStorage.put(description, new GlueScene(description, scene));
    }
}
