package amuse.visualization.amuseAdaptor;

import java.util.HashMap;
import java.util.List;

import javax.media.opengl.GL3;

import ncsa.hdf.object.Dataset;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.HObject;
import openglCommon.exceptions.FileOpeningException;
import openglCommon.math.MatF4;
import openglCommon.math.VecF3;
import openglCommon.models.Model;
import openglCommon.shaders.Program;
import amuse.visualization.AmuseSettings;

public class Hdf5Frame implements Runnable {
    private static final AmuseSettings settings = AmuseSettings.getInstance();

    private final int keyFrame;
    private final String namePrefix;

    private final Model starModel, gasModel;

    private final StarMap stars;
    private final GasMap gasses;

    private AmuseGasOctreeNode gasResult;
    private AmuseGasOctreeNode[] gasInterpolatedResult;

    private boolean initialized, doneProcessing, interpolated;

    private Hdf5Frame interpolationFrame = null;

    private boolean error;
    private String errMessage;

    public Hdf5Frame(Model starModel, Model gasModel, String namePrefix, int keyFrame) {
        this.starModel = starModel;
        this.gasModel = gasModel;
        this.namePrefix = namePrefix;
        this.keyFrame = keyFrame;

        this.stars = new StarMap();
        this.gasses = new GasMap();

        initialized = false;
        doneProcessing = false;
        interpolated = false;

        error = false;
        errMessage = "";
    }

    public synchronized void init() {
        if (!initialized) {
            try {
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

                // System.err.println("read frame: " + keyFrame);
                initialized = true;

            } catch (FileOpeningException e) {
                error = true;
                errMessage = "Failed to open file.";
            }
        }
    }

    public synchronized void process() {
        if (!initialized) {
            init();
        }

        if (!doneProcessing) {
            if (interpolationFrame == null) {
                stars.process();

                if (!settings.getGasStarInfluencedColor()) {
                    gasResult = gasses.process(gasModel);
                } else {
                    gasResult = gasses.process(gasModel, stars);
                }
            } else {
                stars.process(interpolationFrame.stars);

                if (!settings.getGasStarInfluencedColor()) {
                    gasInterpolatedResult = gasses.process(gasModel, interpolationFrame.gasses);
                } else {
                    gasInterpolatedResult = gasses.process(gasModel, interpolationFrame.gasses, stars);
                }

                interpolated = true;
            }

            // System.err.println("processed frame: " + keyFrame);
            doneProcessing = true;
        }
    }

    public synchronized void drawStars(GL3 gl, Program starProgram, MatF4 MVMatrix) {
        if (!doneProcessing) {
            process();
        }

        starModel.init(gl);

        stars.draw(gl, starProgram, MVMatrix);
    }

    public synchronized void drawStars(GL3 gl, Program starProgram, MatF4 MVMatrix, int step) {
        if (!doneProcessing) {
            process();
        }

        if (!interpolated) {
            drawStars(gl, starProgram, MVMatrix);
        }

        starModel.init(gl);

        stars.draw(gl, starProgram, MVMatrix, step);
    }

    public synchronized void drawGas(GL3 gl, Program gasProgram, MatF4 MVMatrix) {
        if (!doneProcessing) {
            process();
        }

        gasModel.init(gl);

        gasResult.draw(gl, gasProgram, MVMatrix);
    }

    public synchronized void drawGas(GL3 gl, Program gasProgram, MatF4 MVMatrix, int step) {
        if (!doneProcessing) {
            process();
        }

        if (!interpolated) {
            drawGas(gl, gasProgram, MVMatrix);
        }

        gasModel.init(gl);

        gasInterpolatedResult[step].draw(gl, gasProgram, MVMatrix);
    }

    public synchronized int getNumber() {
        return keyFrame;
    }

    public synchronized void setinterpolationFrame(Hdf5Frame iFrame) {
        this.interpolationFrame = iFrame;
        this.doneProcessing = false;
    }

    public synchronized boolean isDone() {
        if (error || doneProcessing) {
            return true;
        }

        return false;
    }

    public synchronized boolean isError() {
        if (error) {
            return true;
        }

        return false;
    }

    public synchronized String getError() {
        if (error) {
            return errMessage;
        }

        return "NO ERROR";
    }

    @Override
    public void run() {
        init();
        process();
    }
}
