package amuse.visualization.amuseAdaptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ncsa.hdf.object.Dataset;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.HObject;
import openglCommon.datastructures.Material;
import openglCommon.exceptions.FileOpeningException;
import openglCommon.math.VecF3;
import openglCommon.models.base.Sphere;
import amuse.visualization.AmuseSettings;
import amuse.visualization.amuseAdaptor.exceptions.KeyframeUnavailableException;

public class Hdf5GasCloudUpdater implements Runnable {
    private final AmuseSettings        settings = AmuseSettings.getInstance();

    private HashMap<Long, GasParticle> gasMap;
    private String                     gasNamePrefix;
    private int                        currentFrame, lastFrame;

    public Hdf5GasCloudUpdater(String gasNamePrefix) {
        this.gasMap = new HashMap<Long, GasParticle>();
        this.gasNamePrefix = gasNamePrefix;
        this.currentFrame = 0;
        this.lastFrame = Hdf5Util.getNumGasFiles(Hdf5Util.getGasFileName(gasNamePrefix, currentFrame));
    }

    public AmuseGasOctreeNode getOctreeAt(int keyFrame, int interpolationStep) throws KeyframeUnavailableException {
        final int gasSubdivision = settings.getGasSubdivision();
        final int gasParticlesPerOctreeNode = settings.getGasParticlesPerOctreeNode();

        final float edge = settings.getOctreeEdges();

        AmuseGasOctreeNode root = new AmuseGasOctreeNode(
                new Sphere(Material.random(), gasSubdivision, 1f, new VecF3()), gasParticlesPerOctreeNode, 0,
                gasSubdivision, new VecF3(-edge, -edge, -edge), edge);

        for (Map.Entry<Long, GasParticle> entry : gasMap.entrySet()) {
            GasParticle p = entry.getValue();
            p.setCurrentKeyFrame(keyFrame);

            VecF3 location = p.getLocation(interpolationStep);

            if (!((location.get(0) < -edge) && (location.get(0) > edge) && (location.get(1) < -edge)
                    && (location.get(1) > edge) && (location.get(2) < -edge) && (location.get(2) > edge))) {
                root.addElement(new AmuseGasOctreeElement(location, p.getEnergy(interpolationStep)));
            }
        }

        root.finalizeAdding();

        return root;
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
        final HashMap<String, Dataset> datasets = new HashMap<String, Dataset>();
        List<HObject> memberList;

        double[] x, y, z, vx, vy, vz, u;
        long[] keys;

        while (true) {
            if (currentFrame <= lastFrame) {
                try {
                    FileFormat file = Hdf5Util.getFile(Hdf5Util.getGasFileName(gasNamePrefix, currentFrame));
                    memberList = Hdf5Util.getRoot(file).getMemberList();
                    Hdf5Util.traverse("gas", datasets, memberList);

                    try {
                        keys = (long[]) datasets.get("gas/particles/0000000001/keys").read();

                        x = (double[]) datasets.get("gas/particles/0000000001/attributes/x").read();
                        y = (double[]) datasets.get("gas/particles/0000000001/attributes/y").read();
                        z = (double[]) datasets.get("gas/particles/0000000001/attributes/z").read();

                        vx = (double[]) datasets.get("gas/particles/0000000001/attributes/vx").read();
                        vy = (double[]) datasets.get("gas/particles/0000000001/attributes/vy").read();
                        vz = (double[]) datasets.get("gas/particles/0000000001/attributes/vz").read();

                        u = (double[]) datasets.get("gas/particles/0000000001/attributes/u").read();

                        for (int i = 0; i < keys.length; i++) {
                            final long key = (long) keys[i];

                            VecF3 location = Astrophysics.locationToScreenCoord(x[i], y[i], z[i]);
                            VecF3 velocity = Astrophysics.locationToScreenCoord(vx[i], vy[i], vz[i]);

                            // System.out.println(i + " key: " + key);

                            if (!gasMap.containsKey(key)) {
                                gasMap.put(key, new GasParticle());
                            }
                            gasMap.get(key).addKeyframe(currentFrame, location, velocity, (float) u[i]);
                        }
                    } catch (final Exception e) {
                        System.err.println("General Exception cought in Hdf5GasCloudReader.");
                        e.printStackTrace();
                    }

                    for (final Dataset d : datasets.values()) {
                        d.clear();
                        d.close(0);
                    }

                    Hdf5Util.closeFile(file);
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
                this.lastFrame = Hdf5Util.getNumGasFiles(Hdf5Util.getGasFileName(gasNamePrefix, currentFrame));
            }
        }
    }
}
