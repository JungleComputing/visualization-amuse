package amuse.visualization.amuseAdaptor;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ncsa.hdf.object.Dataset;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.Group;
import ncsa.hdf.object.HObject;
import openglCommon.exceptions.FileOpeningException;
import openglCommon.math.VecF3;
import openglCommon.models.Model;
import openglCommon.models.base.Sphere;

public class Hdf5StarReader {
    static class ExtFilter implements FilenameFilter {
        private final String ext;

        public ExtFilter(String ext) {
            this.ext = ext;
        }

        @Override
        public boolean accept(File dir, String name) {
            return (name.endsWith(ext));
        }
    }

    private static ArrayList<FileFormat> openFiles = new ArrayList<FileFormat>();

    protected static void closeFiles() {
        for (final FileFormat f : Hdf5StarReader.openFiles) {
            // close file resource
            try {
                f.close();
            } catch (final Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        Hdf5StarReader.openFiles.clear();
    }

    protected static int getNumFiles(String path, String namePostfix) {
        final String[] ls = new File(path).list(new ExtFilter(namePostfix));

        return ls.length;
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
            throw new FileOpeningException("Failed to open file: " + filename);
        }

        Hdf5StarReader.openFiles.add(file);

        return (Group) ((javax.swing.tree.DefaultMutableTreeNode) file.getRootNode()).getUserObject();
    }

    public static ArrayList<Star> read(int subdivision, String evo, String grav) throws FileOpeningException {
        final Model starModelBase = new Sphere(null, subdivision, 1f, new VecF3(0, 0, 0));
        final ArrayList<Star> stars = new ArrayList<Star>();

        final HashMap<String, Dataset> datasets = new HashMap<String, Dataset>();

        List<HObject> memberList = Hdf5StarReader.getRoot(evo).getMemberList();
        Hdf5StarReader.traverse("evo", datasets, memberList);

        memberList = Hdf5StarReader.getRoot(grav).getMemberList();
        Hdf5StarReader.traverse("grav", datasets, memberList);

        final Dataset keysSet = datasets.get("evo/particles/0000000001/keys");
        final int numParticles = (int) keysSet.getDims()[0];

        double[] luminosity, realRadius, x, y, z;

        try {
            luminosity = (double[]) datasets.get("evo/particles/0000000001/attributes/luminosity").read();
            realRadius = (double[]) datasets.get("evo/particles/0000000001/attributes/radius").read();

            x = (double[]) datasets.get("grav/particles/0000000001/attributes/x").read();
            y = (double[]) datasets.get("grav/particles/0000000001/attributes/y").read();
            z = (double[]) datasets.get("grav/particles/0000000001/attributes/z").read();

            for (int i = 0; i < numParticles; i++) {
                final VecF3 location = Astrophysics.locationToScreenCoord(x[i], y[i], z[i]);
                final Star newStar = new Star(starModelBase, location, realRadius[i], luminosity[i]);
                stars.add(newStar);

            }
        } catch (final Exception e) {
            System.err.println("General Exception cought in Hdf5StarReader.");
            e.printStackTrace();
        }

        for (final Dataset d : datasets.values()) {
            d.clear();
            d.close(0);
        }

        Hdf5StarReader.closeFiles();

        return stars;
    }

    protected static void traverse(String prefix, HashMap<String, Dataset> result, List<HObject> memberList) {
        for (final HObject o : memberList) {
            if (o instanceof Group) {
                Hdf5StarReader.traverse(prefix, result, ((Group) o).getMemberList());
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

    public Hdf5StarReader() {
    }
}
