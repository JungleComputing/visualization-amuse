package amuse.visualization.amuseAdaptor;

import java.util.HashMap;
import java.util.LinkedList;

import openglCommon.datastructures.Material;
import openglCommon.math.VecF3;
import openglCommon.models.Model;
import openglCommon.models.base.Sphere;
import amuse.visualization.AmuseSettings;

public class Hdf5FrameManager {
    private final AmuseSettings settings = AmuseSettings.getInstance();
    private final int lowestFrame;

    private Hdf5Frame frame0;
    private Hdf5Frame frame1;
    private HashMap<Integer, Hdf5Frame> frameWindow;

    private Model starModelBase;
    private Model gasModelBase;

    private final String namePrefix;

    private final int nThreads;
    private final PoolWorker[] threads;
    private final LinkedList<Runnable> queue;

    public void execute(Runnable r) {
        synchronized (queue) {
            queue.addLast(r);
            queue.notify();
        }
    }

    private class PoolWorker extends Thread {
        public void run() {
            Runnable r;

            while (true) {
                synchronized (queue) {
                    while (queue.isEmpty()) {
                        try {
                            queue.wait();
                        } catch (InterruptedException ignored) {
                        }
                    }

                    r = (Runnable) queue.removeFirst();
                }

                // If we don't catch RuntimeException,
                // the pool could leak threads
                try {
                    r.run();
                } catch (RuntimeException e) {
                    // You might want to log something here
                }
            }
        }
    }

    public Hdf5FrameManager(int lowestFrame, String namePrefix) {
        this.lowestFrame = lowestFrame;
        this.namePrefix = namePrefix;

        this.nThreads = 5;
        queue = new LinkedList<Runnable>();
        threads = new PoolWorker[nThreads];

        for (int i = 0; i < nThreads; i++) {
            threads[i] = new PoolWorker();
            threads[i].setPriority(Thread.MIN_PRIORITY);
            threads[i].start();
        }

        resetModels();
    }

    public void resetModels() {
        this.starModelBase = new Sphere(Material.random(), settings.getStarSubdivision(), 1f, new VecF3(0, 0, 0));
        this.gasModelBase = new Sphere(Material.random(), settings.getGasSubdivision(), 1f, new VecF3(0, 0, 0));

        this.frame0 = new Hdf5Frame(starModelBase, gasModelBase, namePrefix, lowestFrame);
        this.frame1 = new Hdf5Frame(starModelBase, gasModelBase, namePrefix, lowestFrame + 1);
        this.frameWindow = new HashMap<Integer, Hdf5Frame>();
    }

    private HashMap<Integer, Hdf5Frame> getWindow(int frameNumber) {
        HashMap<Integer, Hdf5Frame> newFrameWindow = new HashMap<Integer, Hdf5Frame>();
        for (int i = frameNumber; i < settings.getPreprocessAmount() + frameNumber; i++) {
            Hdf5Frame frame;

            if (i == 0) {
                frame = frame0;
            } else if (i == 1) {
                frame = frame1;
            } else if (frameWindow.containsKey(i)) {
                frame = frameWindow.get(i);
            } else {
                frame = new Hdf5Frame(starModelBase, gasModelBase, namePrefix, i);
            }

            newFrameWindow.put(i, frame);
        }

        for (int i = frameNumber; i < settings.getPreprocessAmount() + frameNumber - 1; i++) {
            if (settings.getBezierInterpolation()) {
                Hdf5Frame frame0 = newFrameWindow.get(i);
                Hdf5Frame frame1 = newFrameWindow.get(i + 1);

                frame0.setinterpolationFrame(frame1);
            }

            execute(newFrameWindow.get(i));
        }

        return newFrameWindow;
    }

    public Hdf5Frame getFrame(int frameNumber) {
        frameWindow = getWindow(frameNumber);

        Hdf5Frame frame = frameWindow.get(frameNumber);

        return frame;
    }
}
