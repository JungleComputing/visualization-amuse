package amuse.visualization.amuseAdaptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ncsa.hdf.object.Dataset;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.HObject;
import openglCommon.datastructures.Material;
import openglCommon.exceptions.FileOpeningException;
import openglCommon.math.VecF3;
import openglCommon.models.Model;
import openglCommon.models.base.Sphere;
import amuse.visualization.AmuseSettings;
import amuse.visualization.amuseAdaptor.exceptions.KeyframeUnavailableException;

public class Hdf5StarUpdater implements Runnable {
    private final AmuseSettings  settings = AmuseSettings.getInstance();

    private HashMap<Long, Star2> starMap;
    private String               starNamePrefix;
    private int                  currentFrame, lastFrame;

    public Hdf5StarUpdater(String starNamePrefix) {
        this.starMap = new HashMap<Long, Star2>();
        this.starNamePrefix = starNamePrefix;
        this.currentFrame = 0;
        this.lastFrame = Hdf5Util.getNumEvoFiles(Hdf5Util.getEvoFileName(starNamePrefix, currentFrame));
    }

    public ArrayList<Star2> getStarsAt(int keyFrame, int interpolationStep) throws KeyframeUnavailableException {
        ArrayList<Star2> stars = new ArrayList<Star2>();
        for (Map.Entry<Long, Star2> entry : starMap.entrySet()) {
            Star2 s = entry.getValue();
            s.setCurrentKeyFrame(keyFrame);
            s.setCurrentInterpolatedFrame(interpolationStep);
            stars.add(s);
        }

        return stars;
    }

    public int getCurrentFrame() {
        return currentFrame;
    }

    public int getLastFrame() {
        return lastFrame;
    }

    public boolean doneReading() {
        if (currentFrame == lastFrame) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void run() {
        final Model starModelBase = new Sphere(Material.random(), settings.getStarSubdivision(), 1f, new VecF3(0, 0, 0));

        long[] keys;
        double[] luminosity, realRadius, x, y, z, vx, vy, vz;

        final HashMap<String, Dataset> datasets = new HashMap<String, Dataset>();
        List<HObject> evoMemberList, gravMemberList;
        FileFormat evoFile, gravFile;

        while (true) {
            if (currentFrame <= lastFrame) {
                try {
                    evoFile = Hdf5Util.getFile(Hdf5Util.getEvoFileName(starNamePrefix, currentFrame));
                    evoMemberList = Hdf5Util.getRoot(evoFile).getMemberList();
                    Hdf5Util.traverse("evo", datasets, evoMemberList);

                    gravFile = Hdf5Util.getFile(Hdf5Util.getGravFileName(starNamePrefix, currentFrame));
                    gravMemberList = Hdf5Util.getRoot(gravFile).getMemberList();
                    Hdf5Util.traverse("grav", datasets, gravMemberList);

                    try {
                        keys = (long[]) datasets.get("grav/particles/0000000001/keys").read();
                        luminosity = (double[]) datasets.get("evo/particles/0000000001/attributes/luminosity").read();
                        realRadius = (double[]) datasets.get("evo/particles/0000000001/attributes/radius").read();

                        x = (double[]) datasets.get("grav/particles/0000000001/attributes/x").read();
                        y = (double[]) datasets.get("grav/particles/0000000001/attributes/y").read();
                        z = (double[]) datasets.get("grav/particles/0000000001/attributes/z").read();

                        vx = (double[]) datasets.get("grav/particles/0000000001/attributes/vx").read();
                        vy = (double[]) datasets.get("grav/particles/0000000001/attributes/vy").read();
                        vz = (double[]) datasets.get("grav/particles/0000000001/attributes/vz").read();

                        // float factor = (float) 0.50E12;
                        float factor = (float) 1.21E12;

                        for (int i = 0; i < keys.length; i++) {
                            final Long key = keys[i];
                            final VecF3 location = new VecF3((float) x[i], (float) y[i], (float) z[i]);
                            final VecF3 velocity = new VecF3((float) vx[i] * factor, (float) vy[i] * factor,
                                    (float) vz[i] * factor);

                            if (!starMap.containsKey(key)) {
                                starMap.put(key, new Star2(starModelBase));
                            }
                            starMap.get(key).addKeyframe(currentFrame, (float) luminosity[i], (float) realRadius[i],
                                    location, velocity);
                        }
                    } catch (final Exception e) {
                        System.err.println("General Exception cought in Hdf5StarUpdater.");
                        e.printStackTrace();
                    }

                    for (final Dataset d : datasets.values()) {
                        d.clear();
                        d.close(0);
                    }

                    Hdf5Util.closeFile(evoFile);
                    Hdf5Util.closeFile(gravFile);
                } catch (FileOpeningException e1) {
                    e1.printStackTrace();
                }
                currentFrame++;
            } else {
                try {
                    Thread.sleep(settings.getWaitTimeRetry());
                } catch (InterruptedException e) {
                    // TODO IGNORE ?
                    e.printStackTrace();
                }
                this.lastFrame = Hdf5Util.getNumEvoFiles(Hdf5Util.getEvoFileName(starNamePrefix, currentFrame));
            }
        }
    }
}
