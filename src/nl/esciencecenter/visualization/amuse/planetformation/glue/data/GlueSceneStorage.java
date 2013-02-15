package nl.esciencecenter.visualization.amuse.planetformation.glue.data;

import java.nio.ByteBuffer;
import java.util.HashMap;

import javax.media.opengl.GL3;

import nl.esciencecenter.visualization.amuse.planetformation.glue.GlueConstants;
import nl.esciencecenter.visualization.amuse.planetformation.interfaces.SceneDescription;
import nl.esciencecenter.visualization.amuse.planetformation.interfaces.SceneStorage;
import nl.esciencecenter.visualization.amuse.planetformation.interfaces.VisualScene;
import nl.esciencecenter.visualization.openglCommon.datastructures.Material;
import nl.esciencecenter.visualization.openglCommon.math.VecF3;
import nl.esciencecenter.visualization.openglCommon.models.Model;
import nl.esciencecenter.visualization.openglCommon.models.Sphere;

import com.jogamp.common.nio.Buffers;

public class GlueSceneStorage implements SceneStorage {
    private GlueSceneDescription                            oldScene    = null;
    private GlueSceneDescription                            newScene    = null;
    private HashMap<GlueSceneDescription, GlueScene>        sceneStorage;
    private final HashMap<GlueSceneDescription, ByteBuffer> legendStorage;

    private final GlueDatasetManager                        manager;

    private final Model                                     starBaseModel;
    private final Model                                     planetBaseModel;
    private final Model                                     sphereBaseModel;
    private final Model                                     sphOctreeBaseModel;

    private boolean                                         initialized = false;

    private final ByteBuffer                                EMPTY_LEGEND_BUFFER;

    public GlueSceneStorage(GlueDatasetManager manager) {
        EMPTY_LEGEND_BUFFER = Buffers.newDirectByteBuffer(1 * 500 * 4);

        sceneStorage = new HashMap<GlueSceneDescription, GlueScene>();
        legendStorage = new HashMap<GlueSceneDescription, ByteBuffer>();

        this.starBaseModel = new Sphere(Material.random(),
                GlueConstants.STAR_SUBDIVISION, 1f, new VecF3(0, 0, 0), true);
        this.planetBaseModel = new Sphere(Material.random(),
                GlueConstants.PLANET_SUBDIVISION, 1f, new VecF3(0, 0, 0), true);
        this.sphereBaseModel = new Sphere(Material.random(),
                GlueConstants.SPHERE_SUBDIVISION, 1f, new VecF3(0, 0, 0), true);
        this.sphOctreeBaseModel = new Sphere(Material.random(),
                GlueConstants.OCTREE_MODEL_SUBDIVISION, 1f, new VecF3(0, 0, 0),
                true);

        this.manager = manager;
    }

    @Override
    public void init(GL3 gl) {
        if (!initialized) {
            starBaseModel.init(gl);
            planetBaseModel.init(gl);
            sphereBaseModel.init(gl);
            sphOctreeBaseModel.init(gl);

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
        GlueScene result = null;

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
        HashMap<GlueSceneDescription, GlueScene> newSceneStore = new HashMap<GlueSceneDescription, GlueScene>();

        for (GlueSceneDescription description : sceneStorage.keySet()) {
            if (description == oldScene || description == newScene) {
                newSceneStore.put(description, sceneStorage.get(description));
            }
        }
        sceneStorage = newSceneStore;

        oldScene = newScene;
        newScene = (GlueSceneDescription) newDescription;
        if (!sceneStorage.containsValue(newScene)) {
            manager.buildScene(newScene);
        }
    }

    public boolean doneWithLastRequest() {
        boolean failure = false;

        if (sceneStorage.get(newScene) == null) {
            failure = true;
        }

        return !failure;
    }

    public void setLegendImage(GlueSceneDescription description,
            ByteBuffer outBuf) {
        legendStorage.put(description, outBuf);
    }

    public void setScene(GlueSceneDescription description, GlueScene scene) {
        sceneStorage.put(description, scene);
    }

    public Model getStarBaseModel() {
        return starBaseModel;
    }

    public Model getPlanetBaseModel() {
        return planetBaseModel;
    }

    public Model getSphereBaseModel() {
        return sphereBaseModel;
    }

    public Model getSPHOctreeBaseModel() {
        return sphOctreeBaseModel;
    }
}
