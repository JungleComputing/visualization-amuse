package nl.esciencecenter.visualization.amuse.planetformation.glue.data;

import nl.esciencecenter.visualization.amuse.planetformation.glue.Particle;
import nl.esciencecenter.visualization.openglCommon.exceptions.UninitializedException;

public class GlueParticleArray implements Runnable {
    private boolean          initialized = false;
    private float[][]        data;
    private int              size;
    private final Particle[] particles;

    public GlueParticleArray(Particle[] particles) {
        this.particles = particles;
    }

    @Override
    public void run() {
        if (!initialized) {
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