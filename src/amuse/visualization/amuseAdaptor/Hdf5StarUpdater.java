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
import openglCommon.math.VectorFMath;
import openglCommon.models.Model;
import openglCommon.models.base.Sphere;

public class Hdf5StarUpdater {
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
        for (final FileFormat f : Hdf5StarUpdater.openFiles) {
            // close file resource
            try {
                f.close();
            } catch (final Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        Hdf5StarUpdater.openFiles.clear();
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

        Hdf5StarUpdater.openFiles.add(file);

        return (Group) ((javax.swing.tree.DefaultMutableTreeNode) file.getRootNode()).getUserObject();
    }

    public static HashMap<Long, Star> read(HashMap<Long, Star> starMap, int subdivision, String evo, String gravLast,
            String gravNext) throws FileOpeningException {
        final Model starModelBase = new Sphere(null, subdivision, 1f, new VecF3(0, 0, 0));

        final HashMap<String, Dataset> datasets = new HashMap<String, Dataset>();

        List<HObject> memberList = Hdf5StarUpdater.getRoot(evo).getMemberList();
        Hdf5StarUpdater.traverse("evo", datasets, memberList);

        memberList = Hdf5StarUpdater.getRoot(gravLast).getMemberList();
        Hdf5StarUpdater.traverse("gravLast", datasets, memberList);

        memberList = Hdf5StarUpdater.getRoot(gravNext).getMemberList();
        Hdf5StarUpdater.traverse("gravNext", datasets, memberList);

        final Dataset keysSet = datasets.get("evo/particles/0000000001/keys");
        final int numParticles = (int) keysSet.getDims()[0];

        long[] keys;
        double[] luminosity, realRadius, x0, y0, z0, vx0, vy0, vz0, x1, y1, z1, vx1, vy1, vz1;

        try {
            keys = (long[]) datasets.get("gravLast/particles/0000000001/keys").read();
            luminosity = (double[]) datasets.get("evo/particles/0000000001/attributes/luminosity").read();
            realRadius = (double[]) datasets.get("evo/particles/0000000001/attributes/radius").read();

            x0 = (double[]) datasets.get("gravLast/particles/0000000001/attributes/x").read();
            y0 = (double[]) datasets.get("gravLast/particles/0000000001/attributes/y").read();
            z0 = (double[]) datasets.get("gravLast/particles/0000000001/attributes/z").read();

            vx0 = (double[]) datasets.get("gravLast/particles/0000000001/attributes/vx").read();
            vy0 = (double[]) datasets.get("gravLast/particles/0000000001/attributes/vy").read();
            vz0 = (double[]) datasets.get("gravLast/particles/0000000001/attributes/vz").read();

            x1 = (double[]) datasets.get("gravNext/particles/0000000001/attributes/x").read();
            y1 = (double[]) datasets.get("gravNext/particles/0000000001/attributes/y").read();
            z1 = (double[]) datasets.get("gravNext/particles/0000000001/attributes/z").read();

            vx1 = (double[]) datasets.get("gravNext/particles/0000000001/attributes/vx").read();
            vy1 = (double[]) datasets.get("gravNext/particles/0000000001/attributes/vy").read();
            vz1 = (double[]) datasets.get("gravNext/particles/0000000001/attributes/vz").read();

            float factor = (float) 0.50E12;
            // float factor = (float) 1.21E12;

            for (int i = 0; i < numParticles; i++) {
                final Long key = keys[i];
                final VecF3 locationLast = new VecF3((float) x0[i], (float) y0[i], (float) z0[i]);
                final VecF3 locationNext = new VecF3((float) x1[i], (float) y1[i], (float) z1[i]);
                final VecF3 velocityLast = new VecF3((float) vx0[i] * factor, (float) vy0[i] * factor, (float) vz0[i]
                        * factor);
                final VecF3 velocityNext = new VecF3((float) vx1[i] * factor, (float) vy1[i] * factor, (float) vz1[i]
                        * factor);

                float length = VectorFMath.length(locationNext.sub(locationLast));

                // System.out.println(length /
                // VectorFMath.length(velocityLast));
                // System.out.println(vx0[i] * 1000000000000f);

                final Star newStar = new Star(key, starModelBase, locationLast, locationNext, velocityLast,
                        velocityNext, realRadius[i], luminosity[i]);
                starMap.put(key, newStar);
            }
        } catch (final Exception e) {
            System.err.println("General Exception cought in Hdf5StarReader.");
            e.printStackTrace();
        }

        for (final Dataset d : datasets.values()) {
            d.clear();
            d.close(0);
        }

        Hdf5StarUpdater.closeFiles();

        return starMap;
    }

    protected static void traverse(String prefix, HashMap<String, Dataset> result, List<HObject> memberList) {
        for (final HObject o : memberList) {
            if (o instanceof Group) {
                Hdf5StarUpdater.traverse(prefix, result, ((Group) o).getMemberList());
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

    public Hdf5StarUpdater() {
    }
}
