package nl.esciencecenter.visualization.amuse.planetformation.glue.data;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.media.opengl.GL3;

import nl.esciencecenter.visualization.amuse.planetformation.data.AmuseGasOctreeNode;
import nl.esciencecenter.visualization.amuse.planetformation.glue.Planet;
import nl.esciencecenter.visualization.amuse.planetformation.glue.PointGas;
import nl.esciencecenter.visualization.amuse.planetformation.glue.SPHGas;
import nl.esciencecenter.visualization.amuse.planetformation.glue.Scene;
import nl.esciencecenter.visualization.amuse.planetformation.glue.Sphere;
import nl.esciencecenter.visualization.amuse.planetformation.glue.Star;
import nl.esciencecenter.visualization.amuse.planetformation.glue.visual.PlanetModel;
import nl.esciencecenter.visualization.amuse.planetformation.glue.visual.PointCloud;
import nl.esciencecenter.visualization.amuse.planetformation.glue.visual.SphereModel;
import nl.esciencecenter.visualization.amuse.planetformation.glue.visual.StarModel;
import nl.esciencecenter.visualization.openglCommon.datastructures.GLSLAttrib;
import nl.esciencecenter.visualization.openglCommon.datastructures.VBO;
import nl.esciencecenter.visualization.openglCommon.exceptions.UninitializedException;
import nl.esciencecenter.visualization.openglCommon.math.MatF4;
import nl.esciencecenter.visualization.openglCommon.models.Model;
import nl.esciencecenter.visualization.openglCommon.shaders.Program;
import nl.esciencecenter.visualization.openglCommon.swing.ColormapInterpreter;
import nl.esciencecenter.visualization.openglCommon.swing.ColormapInterpreter.Color;
import nl.esciencecenter.visualization.openglCommon.swing.ColormapInterpreter.Dimensions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jogamp.common.nio.Buffers;

public class GlueScene {
    private final static Logger          logger      = LoggerFactory
                                                             .getLogger(GlueScene.class);

    private final GlueSceneDescription   description;

    private final ArrayList<StarModel>   stars;
    private final ArrayList<PlanetModel> planets;
    private final ArrayList<SphereModel> spheres;

    private final PointCloud             gasParticles;
    private final AmuseGasOctreeNode     gasOctree;

    private boolean                      initialized = false;
    private VBO                          vbo;

    public GlueScene(GlueSceneDescription description, Scene scene,
            Model starBaseModel, Model planetBaseModel, Model sphereBaseModel) {
        this.description = description;

        Star[] glueStars = scene.getStars();
        this.stars = new ArrayList<StarModel>();
        for (Star glueStar : glueStars) {
            StarModel starModel = new StarModel(starBaseModel, glueStar);
            stars.add(starModel);
        }

        Planet[] gluePlanets = scene.getPlanets();
        this.planets = new ArrayList<PlanetModel>();
        for (Planet gluePlanet : gluePlanets) {
            PlanetModel planetModel = new PlanetModel(planetBaseModel,
                    gluePlanet);
            planets.add(planetModel);
        }

        Sphere[] glueSpheres = scene.getSpheres();
        this.spheres = new ArrayList<SphereModel>();
        for (Sphere glueSphere : glueSpheres) {
            SphereModel sphereModel = new SphereModel(sphereBaseModel,
                    glueSphere);
            spheres.add(sphereModel);
        }

        PointGas[] pointGasses = scene.getPointGas();
        int numPointGasParticles = pointGasses.length;
        FloatBuffer pointGasCoords = Buffers
                .newDirectFloatBuffer(numPointGasParticles * 3);
        FloatBuffer pointGasColors = Buffers
                .newDirectFloatBuffer(numPointGasParticles * 4);
        for (PointGas gluePointGas : scene.getPointGas()) {
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

        SPHGas[] sphGasses = scene.getSphGas();
        this.gasOctree = new 
    }

    FloatBuffer toBuffer(float[][] particles) {
        FloatBuffer result = FloatBuffer.allocate(particles.length * 4);

        for (int i = 0; i < particles.length; i++) {
            for (int j = 0; j < 4; j++) {
                result.put(particles[i][j]);
            }
        }

        result.rewind();

        return result;
    }

    public synchronized void drawStars(GL3 gl, Program starProgram,
            MatF4 MVMatrix) {
        for (StarModel s : stars) {
            s.draw(gl, starProgram, MVMatrix);
        }
    }

    public synchronized void drawGas(GL3 gl, Program gasProgram, MatF4 MVMatrix) {
        gasProgram.setUniformMatrix("global_MVMatrix", MVMatrix);
        gasProgram.setUniform("gas_opacity_factor",
                settings.getGasOpacityFactor());

        // gasOctree.draw(gl, gasProgram);

        if (!initialized) {
            GLSLAttrib vAttrib = new GLSLAttrib(toBuffer(gasParticles),
                    "MCvertex", GLSLAttrib.SIZE_FLOAT, 4);

            GLSLAttrib cAttrib = new GLSLAttrib(gasColors(gasParticles,
                    gasOctree), "MCcolor", GLSLAttrib.SIZE_FLOAT, 4);

            vbo = new VBO(gl, vAttrib, cAttrib);
            initialized = true;
        }

        try {
            gasProgram.use(gl);
        } catch (UninitializedException e) {
            e.printStackTrace();
        }

        vbo.bind(gl);

        gasProgram.linkAttribs(gl, vbo.getAttribs());

        gl.glDrawArrays(GL3.GL_POINTS, 0, gasParticles.length);
    }

    public synchronized void cleanup(GL3 gl) {
        vbo.delete(gl);
    }

    FloatBuffer gasColors(float[][] particles, AmuseGasOctreeNode root) {
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

    FloatBuffer toBuffer(float[][] particles) {
        FloatBuffer result = FloatBuffer.allocate(particles.length * 4);

        for (int i = 0; i < particles.length; i++) {
            for (int j = 0; j < 4; j++) {
                result.put(particles[i][j]);
            }
        }

        result.rewind();

        return result;
    }

    public GlueSceneDescription getDescription() {
        return description;
    }
}
