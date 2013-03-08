package nl.esciencecenter.visualization.amuse.planetformation.glue.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import nl.esciencecenter.visualization.amuse.planetformation.glue.Scene;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlueDatasetManager {
    private final static Logger logger = LoggerFactory.getLogger(GlueDatasetManager.class);

    // private final IOPoolWorker[] ioThreads;
    private final CPUPoolWorker[] cpuThreads;
    private final LinkedList<Runnable> cpuQueue;
    // private final LinkedList<GlueDataArray> ioQueue;

    private final ArrayList<Integer> availableFrameSequenceNumbers;
    private final HashMap<Integer, Scene> glueSceneStorage;
    private final GlueSceneStorage sceneStorage;

    // public void IOJobExecute(GlueDataArray r) {
    // synchronized (ioQueue) {
    // ioQueue.addLast(r);
    // ioQueue.notify();
    // }
    // }

    private class IOPoolWorker extends Thread {
        // @Override
        // public void run() {
        // GlueDataArray runnable;
        //
        // while (true) {
        // synchronized (ioQueue) {
        // while (ioQueue.isEmpty()) {
        // try {
        // ioQueue.wait();
        // } catch (InterruptedException ignored) {
        // }
        // }
        //
        // runnable = ioQueue.removeFirst();
        // }
        //
        // // If we don't catch RuntimeException,
        // // the pool could leak threads
        // try {
        // runnable.run();
        // float[][] particleData = runnable.getParticleData();
        // float[][] gasData = runnable.getGasData();
        //
        // logger.debug("Particles: " + particleData.length);
        // logger.debug("Gasses   : " + gasData.length);
        //
        // AmuseSceneBuilder builder = new AmuseSceneBuilder(
        // sceneStorage, runnable.getDescription(),
        // particleData, gasData);
        // CPUJobExecute(builder);
        //
        // LegendTextureBuilder legendBuilder = new LegendTextureBuilder(
        // sceneStorage, runnable);
        // CPUJobExecute(legendBuilder);
        // } catch (RuntimeException e) {
        // logger.error("Runtime exception in IOPoolworker", e);
        //
        // } catch (UninitializedException e) {
        // logger.error("Uninitialized exception in IOPoolWorker", e);
        // }
        // }
        // }
    }

    public void CPUJobExecute(Runnable r) {
        synchronized (cpuQueue) {
            cpuQueue.addLast(r);
            cpuQueue.notify();
        }
    }

    private class CPUPoolWorker extends Thread {
        @Override
        public void run() {
            Runnable r;

            while (true) {
                synchronized (cpuQueue) {
                    while (cpuQueue.isEmpty()) {
                        try {
                            cpuQueue.wait();
                        } catch (InterruptedException ignored) {
                        }
                    }

                    r = cpuQueue.removeFirst();
                }

                // If we don't catch RuntimeException,
                // the pool could leak threads
                try {
                    r.run();
                } catch (RuntimeException e) {
                    logger.error("Runtime exception in IOPoolworker", e);
                }
            }
        }
    }

    public GlueDatasetManager(int numIOThreads, int numCPUThreads) {
        // ioQueue = new LinkedList<GlueDataArray>();
        cpuQueue = new LinkedList<Runnable>();

        // ioThreads = new IOPoolWorker[numIOThreads];
        cpuThreads = new CPUPoolWorker[numCPUThreads];

        // for (int i = 0; i < numIOThreads; i++) {
        // ioThreads[i] = new IOPoolWorker();
        // ioThreads[i].setPriority(Thread.MIN_PRIORITY);
        // ioThreads[i].start();
        // }
        for (int i = 0; i < numIOThreads; i++) {
            cpuThreads[i] = new CPUPoolWorker();
            cpuThreads[i].setPriority(Thread.MIN_PRIORITY);
            cpuThreads[i].start();
        }

        availableFrameSequenceNumbers = new ArrayList<Integer>();
        sceneStorage = new GlueSceneStorage(this);
        glueSceneStorage = new HashMap<Integer, Scene>();
    }

    public void buildScene(GlueSceneDescription description) {
        int frameNumber = description.getFrameNumber();
        if (frameNumber < 0
                || frameNumber >= availableFrameSequenceNumbers.get(availableFrameSequenceNumbers.size() - 1)) {
            logger.warn("buildImages : Requested frameNumber  " + frameNumber + " out of range.");
        }

        CPUJobExecute(new GlueScene(sceneStorage, description, glueSceneStorage.get(frameNumber)));
        // CPUJobExecute(new LegendTextureBuilder(sceneStorage, description));
    }

    public GlueSceneStorage getSceneStorage() {
        return sceneStorage;
    }

    public int getFrameNumberOfIndex(int index) throws IndexNotAvailableException {
        if (availableFrameSequenceNumbers.contains(index)) {
            return availableFrameSequenceNumbers.get(index);
        } else {
            throw new IndexNotAvailableException();
        }
    }

    public int getIndexOfFrameNumber(int frameNumber) {
        return availableFrameSequenceNumbers.indexOf(frameNumber);
    }

    public int getPreviousFrameNumber(int frameNumber) throws IOException {
        int nextNumber = getIndexOfFrameNumber(frameNumber) - 1;

        if (nextNumber >= 0 && nextNumber < availableFrameSequenceNumbers.size()) {
            try {
                return getFrameNumberOfIndex(nextNumber);
            } catch (IndexNotAvailableException e) {
                throw new IOException("Frame number not available: " + nextNumber);
            }
        } else {
            throw new IOException("Frame number not available: " + nextNumber);
        }
    }

    public int getNextFrameNumber(int frameNumber) throws IOException {
        int nextNumber = getIndexOfFrameNumber(frameNumber) + 1;

        if (nextNumber >= 0 && nextNumber < availableFrameSequenceNumbers.size()) {
            try {
                return getFrameNumberOfIndex(nextNumber);
            } catch (IndexNotAvailableException e) {
                throw new IOException("Frame number not available: " + nextNumber);
            }
        } else {
            throw new IOException("Frame number not available: " + nextNumber);
        }
    }

    public int getNumFrames() {
        return availableFrameSequenceNumbers.size();
    }

    public void addScene(Scene scene) {
        glueSceneStorage.put(availableFrameSequenceNumbers.size(), scene);
        availableFrameSequenceNumbers.add(availableFrameSequenceNumbers.size());

        // String filename = "bla";
        // try {
        // File myFile = new File(filename);
        // myFile.createNewFile();
        //
        // FileOutputStream fos = new FileOutputStream(myFile);
        // ObjectOutputStream oos = new ObjectOutputStream(fos);
        // oos.writeObject(scene);
        // oos.close();
        //
        // } catch (FileNotFoundException e) {
        // e.printStackTrace();
        // } catch (IOException e) {
        // e.printStackTrace();
        // }

    }
}
