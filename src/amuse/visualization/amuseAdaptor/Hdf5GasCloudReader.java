package amuse.visualization.amuseAdaptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ncsa.hdf.object.Dataset;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.Group;
import ncsa.hdf.object.HObject;
import openglCommon.exceptions.FileOpeningException;
import amuse.visualization.AmuseSettings;

public class Hdf5GasCloudReader {
    private final static AmuseSettings   settings  = AmuseSettings.getInstance();

    private static ArrayList<FileFormat> openFiles = new ArrayList<FileFormat>();

    // public long[] keys;

    protected static void closeFiles() {
        for (final FileFormat f : Hdf5GasCloudReader.openFiles) {
            // close file resource
            try {
                f.close();
            } catch (final Exception e) {
                System.err.println("Error closing file: " + f.getPath());
                e.printStackTrace();
            }
        }

        Hdf5GasCloudReader.openFiles.clear();
    }

    protected static Group getRoot(String filename) throws FileOpeningException {
        // retrieve an instance of H5File
        final FileFormat fileFormat = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
        if (fileFormat == null) {
            throw new FileOpeningException("Cannot find HDF5 FileFormat.");
        }

        // open the file with read and write access
        FileFormat file = null;
        try {
            file = fileFormat.createInstance(filename, FileFormat.READ);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        if (file == null) {
            throw new FileOpeningException("Failed to open file, file is null: " + filename);
        }

        // open the file and retrieve the file structure
        try {
            file.open();
        } catch (final Exception e) {
            throw new FileOpeningException("Failed to open file: " + filename, e);
        }

        Hdf5GasCloudReader.openFiles.add(file);

        return (Group) ((javax.swing.tree.DefaultMutableTreeNode) file.getRootNode()).getUserObject();
    }

    public static void read(AmuseGasOctreeNode cubeRoot, String gasName) throws FileOpeningException {

        final HashMap<String, Dataset> result = new HashMap<String, Dataset>();

        final List<HObject> memberList = Hdf5GasCloudReader.getRoot(gasName).getMemberList();
        Hdf5GasCloudReader.traverse("gas", result, memberList);

        final Dataset keysSet = result.get("gas/particles/0000000001/keys");
        final int numParticles = (int) keysSet.getDims()[0];

        double[] x, y, z, u;

        // keys = (long[]) keysSet.read();
        try {
            x = (double[]) result.get("gas/particles/0000000001/attributes/x").read();
            y = (double[]) result.get("gas/particles/0000000001/attributes/y").read();
            z = (double[]) result.get("gas/particles/0000000001/attributes/z").read();
            u = (double[]) result.get("gas/particles/0000000001/attributes/u").read();

            for (int i = 0; i < numParticles; i++) {
                final float px = (float) (x[i] / 10E14);
                final float py = (float) (y[i] / 10E14);
                final float pz = (float) (z[i] / 10E14);

                if (!((px < -Hdf5GasCloudReader.settings.getOctreeEdges())
                        && (px > Hdf5GasCloudReader.settings.getOctreeEdges())
                        && (py < -Hdf5GasCloudReader.settings.getOctreeEdges())
                        && (py > Hdf5GasCloudReader.settings.getOctreeEdges())
                        && (pz < -Hdf5GasCloudReader.settings.getOctreeEdges()) && (pz > Hdf5GasCloudReader.settings
                        .getOctreeEdges()))) {
                    cubeRoot.addElement(new AmuseGasOctreeElement(Astrophysics.locationToScreenCoord(x[i], y[i], z[i]),
                            u[i]));
                }
            }
            cubeRoot.finalizeAdding();
        } catch (final FileOpeningException e) {
            throw e;
        } catch (final Exception e) {
            System.err.println("General Exception cought in Hdf5GasCloudReader.");
            e.printStackTrace();
        }

        for (final Dataset d : result.values()) {
            d.clear();
            d.close(0);
        }

        Hdf5GasCloudReader.closeFiles();
    }

    protected static void traverse(String prefix, HashMap<String, Dataset> result, List<HObject> memberList) {
        for (final HObject o : memberList) {
            if (o instanceof Group) {
                Hdf5GasCloudReader.traverse(prefix, result, ((Group) o).getMemberList());
            } else if (o instanceof Dataset) {
                // System.out.println(prefix+o.getFullName());
                result.put(prefix + o.getFullName(), (Dataset) o);
                ((Dataset) o).init();
            } else {
                System.err.println("Unknown object type discovered: " + o.getFullName());
                System.exit(1);
            }
        }
    }

    public Hdf5GasCloudReader() {
    }
}
