package nl.esciencecenter.visualization.amuse.planetformation.data;

import java.io.IOException;

import nl.esciencecenter.visualization.amuse.planetformation.glue.Particle;
import nl.esciencecenter.visualization.amuse.planetformation.netcdf.NetCDFUtil;
import nl.esciencecenter.visualization.openglCommon.exceptions.UninitializedException;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

public class AmuseParticleDataArray implements Runnable {
    private final NetcdfFile ncFile;
    private boolean          initialized = false;
    private float[][]        data;
    private int              size;
    private final Particle[] particles;

    public AmuseParticleDataArray(NetcdfFile frameFile) {
        this.ncFile = frameFile;
        this.particles = null;
    }

    public AmuseParticleDataArray(Particle[] particles) {
        this.ncFile = null;
        this.particles = particles;
    }

    @Override
    public void run() {
        if (!initialized && ncFile != null) {
            Variable ncdfVar_index = ncFile
                    .findVariable("particles/0000000001/keys");

            Variable ncdfVar_x = ncFile
                    .findVariable("particles/0000000001/attributes/x");
            Variable ncdfVar_y = ncFile
                    .findVariable("particles/0000000001/attributes/y");
            Variable ncdfVar_z = ncFile
                    .findVariable("particles/0000000001/attributes/z");
            Variable ncdfVar_radius = ncFile
                    .findVariable("particles/0000000001/attributes/radius");

            size = ncdfVar_x.getShape()[0];

            data = new float[size - 1][5];

            try {
                Array ncdfArray1D_index = ncdfVar_index.read();

                Array ncdfArray1D_x = ncdfVar_x.read();
                Array ncdfArray1D_y = ncdfVar_y.read();
                Array ncdfArray1D_z = ncdfVar_z.read();
                Array ncdfArray1D_radius = ncdfVar_radius.read();

                int[] result_index = (int[]) ncdfArray1D_index
                        .get1DJavaArray(int.class);

                double[] result_x = (double[]) ncdfArray1D_x
                        .get1DJavaArray(double.class);
                double[] result_y = (double[]) ncdfArray1D_y
                        .get1DJavaArray(double.class);
                double[] result_z = (double[]) ncdfArray1D_z
                        .get1DJavaArray(double.class);
                double[] result_radius = (double[]) ncdfArray1D_radius
                        .get1DJavaArray(double.class);

                for (int i = 0; i < size - 1; i++) {
                    float x = (float) result_x[i];
                    float y = (float) result_y[i];
                    float z = (float) result_z[i];
                    float r = (float) result_radius[i];

                    data[i][0] = x;
                    data[i][1] = y;
                    data[i][2] = z;
                    data[i][3] = r;
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            NetCDFUtil.close(ncFile);

            initialized = true;
        } else {
            size = particles.length;

            data = new float[size - 1][4];

            for (int i = 0; i < size - 1; i++) {
                float x = particles[i].getCoordinates()[0];
                float y = particles[i].getCoordinates()[1];
                float z = particles[i].getCoordinates()[2];
                float r = particles[i].getRadius();

                data[i][0] = x;
                data[i][1] = y;
                data[i][2] = z;
                data[i][3] = r;
            }

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