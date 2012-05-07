package amuse.visualization.amuseAdaptor;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.List;

import ncsa.hdf.object.Dataset;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.Group;
import ncsa.hdf.object.HObject;
import openglCommon.exceptions.FileOpeningException;

public class Hdf5Util {
    private final static String evoNamePostfix  = ".evo";
    private final static String gravNamePostfix = ".grav";
    private final static String gasNamePostfix  = ".gas";

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

    private static String intToString(int input) {
        String result = "";
        if (input < 10) {
            result += "00000" + input;
        } else if ((input >= 10) && (input < 100)) {
            result += "0000" + input;
        } else if ((input >= 100) && (input < 1000)) {
            result += "000" + input;
        } else if ((input >= 1000) && (input < 10000)) {
            result += "00" + input;
        } else if ((input >= 10000) && (input < 100000)) {
            result += "0" + input;
        } else {
            result += input;
        }

        return result;
    }

    public static String getEvoFileName(String prefix, int currentFrame) {
        return prefix + intToString(currentFrame) + evoNamePostfix;
    }

    public static String getGravFileName(String prefix, int currentFrame) {
        return prefix + intToString(currentFrame) + gravNamePostfix;
    }

    public static String getGasFileName(String prefix, int currentFrame) {
        return prefix + intToString(currentFrame) + gasNamePostfix;
    }

    public static int getNumGasFiles(String fileName) {
        File file = new File(fileName);
        String path = file.getParent();
        final String[] ls = new File(path).list(new ExtFilter(gasNamePostfix));

        return ls.length;
    }

    public static int getNumEvoFiles(String fileName) {
        File file = new File(fileName);
        String path = file.getParent();
        final String[] ls = new File(path).list(new ExtFilter(evoNamePostfix));

        return ls.length;
    }

    public static int getNumGravFiles(String fileName) {
        File file = new File(fileName);
        String path = file.getParent();
        final String[] ls = new File(path).list(new ExtFilter(gravNamePostfix));

        return ls.length;
    }

    public static void closeFile(FileFormat file) {
        try {
            file.close();
        } catch (final Exception e) {
            System.err.println("Error closing file: " + file.getPath());
            e.printStackTrace();
        }
    }

    public static FileFormat getFile(String filename) throws FileOpeningException {
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

        return file;
    }

    public static Group getRoot(FileFormat file) {
        return (Group) ((javax.swing.tree.DefaultMutableTreeNode) file.getRootNode()).getUserObject();
    }

    protected static void traverse(String prefix, HashMap<String, Dataset> result, List<HObject> memberList) {
        for (final HObject o : memberList) {
            if (o instanceof Group) {
                traverse(prefix, result, ((Group) o).getMemberList());
            } else if (o instanceof Dataset) {
                // System.out.println(prefix + o.getFullName() + " : "
                // + ((Dataset) o).getDatatype().getDatatypeDescription());
                result.put(prefix + o.getFullName(), (Dataset) o);
                ((Dataset) o).init();
            } else {
                System.err.println("Unknown object type discovered: " + o.getFullName());
                System.exit(1);
            }
        }
    }
}
