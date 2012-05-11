package amuse.visualization.amuseAdaptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.media.opengl.GL3;

import ncsa.hdf.object.Dataset;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.HObject;
import openglCommon.exceptions.FileOpeningException;
import openglCommon.exceptions.UninitializedException;
import openglCommon.math.MatF4;
import openglCommon.math.VecF3;
import openglCommon.models.Model;
import openglCommon.shaders.Program;

public class Hdf5Frame {
    private final int keyFrame;
    private final String namePrefix;

    private final Model starModel, gasModel;

    private final StarMap stars;
    private final GasMap gasses;

    private ArrayList<Star> starResult;
    private AmuseGasOctreeNode gasResult;

    private boolean initialized, doneProcessing;

    public Hdf5Frame(Model starModel, Model gasModel, String namePrefix, int keyFrame) {
        this.starModel = starModel;
        this.gasModel = gasModel;
        this.namePrefix = namePrefix;
        this.keyFrame = keyFrame;

        this.stars = new StarMap();
        this.gasses = new GasMap();

        initialized = false;
        doneProcessing = false;
    }

    public synchronized void init() throws FileOpeningException {
        if (!initialized) {
            long[] skeys, gkeys;
            double[] luminosity, realRadius, sx, sy, sz, svx, svy, svz, gx, gy, gz, gvx, gvy, gvz, gu;

            final HashMap<String, Dataset> datasets = new HashMap<String, Dataset>();
            List<HObject> evoMemberList, gravMemberList, gasMemberList;
            FileFormat evoFile, gravFile, gasFile;

            evoFile = Hdf5Util.getFile(Hdf5Util.getEvoFileName(namePrefix, keyFrame));
            evoMemberList = Hdf5Util.getRoot(evoFile).getMemberList();
            Hdf5Util.traverse("evo", datasets, evoMemberList);

            gravFile = Hdf5Util.getFile(Hdf5Util.getGravFileName(namePrefix, keyFrame));
            gravMemberList = Hdf5Util.getRoot(gravFile).getMemberList();
            Hdf5Util.traverse("grav", datasets, gravMemberList);

            gasFile = Hdf5Util.getFile(Hdf5Util.getGasFileName(namePrefix, keyFrame));
            gasMemberList = Hdf5Util.getRoot(gasFile).getMemberList();
            Hdf5Util.traverse("gas", datasets, gasMemberList);

            try {
                skeys = (long[]) datasets.get("grav/particles/0000000001/keys").read();
                luminosity = (double[]) datasets.get("evo/particles/0000000001/attributes/luminosity").read();
                realRadius = (double[]) datasets.get("evo/particles/0000000001/attributes/radius").read();

                sx = (double[]) datasets.get("grav/particles/0000000001/attributes/x").read();
                sy = (double[]) datasets.get("grav/particles/0000000001/attributes/y").read();
                sz = (double[]) datasets.get("grav/particles/0000000001/attributes/z").read();

                svx = (double[]) datasets.get("grav/particles/0000000001/attributes/vx").read();
                svy = (double[]) datasets.get("grav/particles/0000000001/attributes/vy").read();
                svz = (double[]) datasets.get("grav/particles/0000000001/attributes/vz").read();

                gkeys = (long[]) datasets.get("gas/particles/0000000001/keys").read();

                gx = (double[]) datasets.get("gas/particles/0000000001/attributes/x").read();
                gy = (double[]) datasets.get("gas/particles/0000000001/attributes/y").read();
                gz = (double[]) datasets.get("gas/particles/0000000001/attributes/z").read();

                gvx = (double[]) datasets.get("gas/particles/0000000001/attributes/vx").read();
                gvy = (double[]) datasets.get("gas/particles/0000000001/attributes/vy").read();
                gvz = (double[]) datasets.get("gas/particles/0000000001/attributes/vz").read();

                gu = (double[]) datasets.get("gas/particles/0000000001/attributes/u").read();

                for (int i = 0; i < skeys.length; i++) {
                    final Long key = skeys[i];
                    final VecF3 location = new VecF3((float) sx[i], (float) sy[i], (float) sz[i]);
                    final VecF3 velocity = Astrophysics.velocityToCorrectUnits(svx[i], svy[i], svz[i]);

                    stars.put(key, new Star(starModel, location, velocity, luminosity[i], realRadius[i]));
                }

                for (int i = 0; i < gkeys.length; i++) {
                    final long key = gkeys[i];

                    VecF3 location = new VecF3((float) gx[i], (float) gy[i], (float) gz[i]);
                    final VecF3 velocity = Astrophysics.velocityToCorrectUnits(gvx[i], gvy[i], gvz[i]);

                    gasses.put(key, new Gas(location, velocity, gu[i]));
                }
            } catch (final Exception e) {
                System.err.println("General Exception cought in Hdf5Frame.");
                e.printStackTrace();
            }

            for (final Dataset d : datasets.values()) {
                d.clear();
                d.close(0);
            }

            Hdf5Util.closeFile(evoFile);
            Hdf5Util.closeFile(gravFile);
            Hdf5Util.closeFile(gasFile);

            initialized = true;
        }
    }

    public void process() throws UninitializedException {
        if (!initialized)
            throw new UninitializedException();

        starResult = stars.process();
        gasResult = gasses.process(gasModel);

        doneProcessing = true;
    }

    public void drawStars(GL3 gl, Program starProgram, MatF4 MVMatrix) throws UninitializedException {
        if (!doneProcessing)
            process();

        starModel.init(gl);

        try {
            for (Star s : starResult) {
                s.init();
                s.draw(gl, starProgram, MVMatrix);
            }
        } catch (UninitializedException e) {
            e.printStackTrace();
        }
    }

    public void drawGas(GL3 gl, Program gasProgram, MatF4 MVMatrix) throws UninitializedException {
        if (!doneProcessing)
            process();

        gasModel.init(gl);
        gasResult.init(gl);

        try {
            gasResult.draw(gl, gasProgram, MVMatrix);
        } catch (UninitializedException e) {
            e.printStackTrace();
        }
    }

    public int getNumber() {
        return keyFrame;
    }
}
