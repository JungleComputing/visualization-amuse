package nl.esciencecenter.visualization.amuse.planetformation.data;

import java.io.IOException;

import nl.esciencecenter.visualization.amuse.planetformation.netcdf.NetCDFUtil;
import openglCommon.exceptions.UninitializedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

public class AmuseGasDataArray implements Runnable {
    private final static Logger logger      = LoggerFactory
                                                    .getLogger(AmuseGasDataArray.class);
    private final NetcdfFile    ncFile;
    private boolean             initialized = false;
    private float[][]           data;
    private int                 size;

    public AmuseGasDataArray(NetcdfFile frameFile) {
        this.ncFile = frameFile;
    }

    @Override
    public void run() {
        if (!initialized) {
            Variable ncdfVar_x = ncFile
                    .findVariable("particles/0000000001/attributes/x");
            Variable ncdfVar_y = ncFile
                    .findVariable("particles/0000000001/attributes/y");
            Variable ncdfVar_z = ncFile
                    .findVariable("particles/0000000001/attributes/z");
            Variable ncdfVar_u = ncFile
                    .findVariable("particles/0000000001/attributes/u");
            Variable ncdfVar_mass = ncFile
                    .findVariable("particles/0000000001/attributes/mass");

            size = ncdfVar_x.getShape()[0];

            data = new float[size][5];

            logger.debug("Gas data size: " + size);

            try {
                Array ncdfArray1D_x = ncdfVar_x.read();
                Array ncdfArray1D_y = ncdfVar_y.read();
                Array ncdfArray1D_z = ncdfVar_z.read();
                Array ncdfArray1D_u = ncdfVar_u.read();
                Array ncdfArray1D_mass = ncdfVar_mass.read();

                double[] result_x = (double[]) ncdfArray1D_x
                        .get1DJavaArray(double.class);
                double[] result_y = (double[]) ncdfArray1D_y
                        .get1DJavaArray(double.class);
                double[] result_z = (double[]) ncdfArray1D_z
                        .get1DJavaArray(double.class);
                double[] result_u = (double[]) ncdfArray1D_u
                        .get1DJavaArray(double.class);
                double[] result_mass = (double[]) ncdfArray1D_mass
                        .get1DJavaArray(double.class);

                for (int i = 0; i < size; i++) {
                    data[i][0] = (float) result_x[i];
                    data[i][1] = (float) result_y[i];
                    data[i][2] = (float) result_z[i];
                    data[i][3] = (float) result_u[i];
                    data[i][4] = (float) result_mass[i];
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            NetCDFUtil.close(ncFile);

            initialized = true;

        }
    }

    public float[][] getData() throws UninitializedException {
        if (initialized) {
            return data;
        }

        throw new UninitializedException();
    }

    public int getSize() throws UninitializedException {
        if (initialized) {
            return size;
        }

        throw new UninitializedException();
    }
}