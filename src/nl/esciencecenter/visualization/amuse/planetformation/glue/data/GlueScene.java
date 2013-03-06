package nl.esciencecenter.visualization.amuse.planetformation.glue.data;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.media.opengl.GL3;

import nl.esciencecenter.visualization.amuse.planetformation.glue.GlueConstants;
import nl.esciencecenter.visualization.amuse.planetformation.glue.Planet;
import nl.esciencecenter.visualization.amuse.planetformation.glue.PointGas;
import nl.esciencecenter.visualization.amuse.planetformation.glue.SPHGas;
import nl.esciencecenter.visualization.amuse.planetformation.glue.Scene;
import nl.esciencecenter.visualization.amuse.planetformation.glue.Sphere;
import nl.esciencecenter.visualization.amuse.planetformation.glue.Star;
import nl.esciencecenter.visualization.amuse.planetformation.glue.visual.PlanetModel;
import nl.esciencecenter.visualization.amuse.planetformation.glue.visual.PointCloud;
import nl.esciencecenter.visualization.amuse.planetformation.glue.visual.SPHOctreeNode;
import nl.esciencecenter.visualization.amuse.planetformation.glue.visual.SphereModel;
import nl.esciencecenter.visualization.amuse.planetformation.glue.visual.StarModel;
import nl.esciencecenter.visualization.amuse.planetformation.interfaces.SceneDescription;
import nl.esciencecenter.visualization.amuse.planetformation.interfaces.VisualScene;
import nl.esciencecenter.visualization.openglCommon.exceptions.UninitializedException;
import nl.esciencecenter.visualization.openglCommon.math.MatF4;
import nl.esciencecenter.visualization.openglCommon.math.VecF3;
import nl.esciencecenter.visualization.openglCommon.math.VecF4;
import nl.esciencecenter.visualization.openglCommon.models.Model;
import nl.esciencecenter.visualization.openglCommon.shaders.Program;
import nl.esciencecenter.visualization.openglCommon.swing.ColormapInterpreter;
import nl.esciencecenter.visualization.openglCommon.swing.ColormapInterpreter.Color;
import nl.esciencecenter.visualization.openglCommon.swing.ColormapInterpreter.Dimensions;

import com.jogamp.common.nio.Buffers;

public class GlueScene implements Runnable, VisualScene {
    private final GlueSceneStorage     sceneStore;
    private final GlueSceneDescription description;
    private final Scene                scene;

    private ArrayList<StarModel>       stars;
    private ArrayList<PlanetModel>     planets;
    private ArrayList<SphereModel>     spheres;

    private PointCloud                 gasParticles;
    private SPHOctreeNode              gasOctree;

    private boolean                    initialized = false;

    public GlueScene(GlueSceneStorage sceneStore,
            GlueSceneDescription description, Scene scene) {
        this.sceneStore = sceneStore;
        this.description = description;
        this.scene = scene;
    }

    @Override
    public void run() {
        Star[] glueStars = scene.getStars();
        Model starBaseModel = sceneStore.getStarBaseModel();
        if (glueStars != null) {
            this.stars = new ArrayList<StarModel>();
            for (Star glueStar : glueStars) {
                StarModel starModel = new StarModel(starBaseModel, glueStar);
                stars.add(starModel);
            }
        }

        Planet[] gluePlanets = scene.getPlanets();
        Model planetBaseModel = sceneStore.getPlanetBaseModel();
        if (gluePlanets != null) {
            this.planets = new ArrayList<PlanetModel>();
            for (Planet gluePlanet : gluePlanets) {
                PlanetModel planetModel = new PlanetModel(planetBaseModel,
                        gluePlanet);
                planets.add(planetModel);
            }
        }

        Sphere[] glueSpheres = scene.getSpheres();
        Model sphereBaseModel = sceneStore.getSphereBaseModel();
        if (glueSpheres != null) {
            this.spheres = new ArrayList<SphereModel>();
            for (Sphere glueSphere : glueSpheres) {
                SphereModel sphereModel = new SphereModel(sphereBaseModel,
                        glueSphere);
                spheres.add(sphereModel);
            }
        }

        PointGas[] pointGasses = scene.getPointGas();
        if (pointGasses != null) {
            int numPointGasParticles = pointGasses.length;

            FloatBuffer pointGasCoords = Buffers
                    .newDirectFloatBuffer(numPointGasParticles * 3);
            FloatBuffer pointGasColors = Buffers
                    .newDirectFloatBuffer(numPointGasParticles * 4);

            for (PointGas gluePointGas : pointGasses) {
                float[] coords = gluePointGas.getCoordinates();
                float[] color = gluePointGas.getColor();

                for (int i = 0; i < 3; i++) {
                    pointGasCoords.put(coords[i]);
                }

                for (int i = 0; i < 4; i++) {
                    pointGasColors.put(color[i]);
                }
            }
            gasParticles = new PointCloud(numPointGasParticles, pointGasCoords,
                    pointGasColors);
        }

        SPHGas[] sphGasses = scene.getSphGas();
        if (sphGasses != null) {
            Model octreeBaseModel = sceneStore.getSPHOctreeBaseModel();
            this.gasOctree = new SPHOctreeNode(octreeBaseModel, 0, new VecF3(),
                    GlueConstants.INITIAL_OCTREE_SIZE, description);
            for (SPHGas glueSPHGas : sphGasses) {
                float[] rawCoords = glueSPHGas.getCoordinates();
                float[] rawColor = glueSPHGas.getColor();

                VecF3 coords = new VecF3(rawCoords[0], rawCoords[1], rawCoords[2]);
                VecF4 color = new VecF4(rawColor[0], rawColor[1], rawColor[2], rawColor[3]);

                this.gasOctree.addElement(coords, color);
            }
        }

        sceneStore.setScene(description, this);
    }

    @Override
    public synchronized void drawStars(GL3 gl, Program program, MatF4 MVMatrix) {
        if (stars != null) {
            for (StarModel s : stars) {
                s.draw(gl, program, MVMatrix);
            }
        }
    }

    public synchronized void drawPlanets(GL3 gl, Program program, MatF4 MVMatrix) {
        if (planets != null) {
            for (PlanetModel p : planets) {
                p.draw(gl, program, MVMatrix);
            }
        }
    }

    @Override
    public synchronized void drawSpheres(GL3 gl, Program program, MatF4 MVMatrix) {
        if (spheres != null) {
            for (SphereModel s : spheres) {
                s.draw(gl, program, MVMatrix);
            }
        }
    }

    @Override
    public synchronized void drawGasPointCloud(GL3 gl, Program program,
            MatF4 MVMatrix) {
        if (gasParticles != null) {
            program.setUniformMatrix("MVMatrix", MVMatrix);

            try {
                program.use(gl);
            } catch (UninitializedException e) {
                e.printStackTrace();
            }

            gasParticles.draw(gl, program);
        }
    }

    @Override
    public synchronized void drawGasOctree(GL3 gl, Program program,
            MatF4 MVMatrix) {
        if (gasOctree != null) {
            program.setUniformMatrix("MVMatrix", MVMatrix);
            gasOctree.draw(gl, program);
        }
    }

    private FloatBuffer gasColors(float[][] particles, SPHOctreeNode root) {
        FloatBuffer result = FloatBuffer.allocate(particles.length * 4);

        for (int i = 0; i < particles.length; i++) {
            float rho = particles[i][3];
            Color myColor = ColormapInterpreter.getColor(description
                    .getColorMap(), new Dimensions(description.getLowerBound(),
                    description.getUpperBound()), rho);

            result.put(myColor.red);
            result.put(myColor.green);
            result.put(myColor.blue);
            result.put(1f);
        }

        result.rewind();

        return result;
    }

    public void init(GL3 gl) {
        if (!initialized) {
            if (stars != null) {
                for (StarModel s : stars) {
                    s.init();
                }
            }

            if (planets != null) {
                for (PlanetModel p : planets) {
                    p.init();
                }
            }

            if (spheres != null) {
                for (SphereModel s : spheres) {
                    s.init();
                }
            }

            if (gasParticles != null) {
                gasParticles.init(gl);
            }

            if (gasOctree != null) {
                gasOctree.init();
            }

            initialized = true;
        }
    }

    @Override
    public void dispose(GL3 gl) {
        gasParticles.dispose(gl);

        initialized = false;
    }

    @Override
    public SceneDescription getDescription() {
        return description;
    }
}
