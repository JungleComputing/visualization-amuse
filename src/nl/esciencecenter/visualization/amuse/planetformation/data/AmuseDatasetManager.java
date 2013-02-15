package nl.esciencecenter.visualization.amuse.planetformation.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import nl.esciencecenter.visualization.openglCommon.exceptions.UninitializedException;
import nl.esciencecenter.visualization.openglCommon.io.netcdf.NetCDFUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.nc2.NetcdfFile;

public class AmuseDatasetManager {
    private final static Logger              logger = LoggerFactory
                                                            .getLogger(AmuseDatasetManager.class);

    private final IOPoolWorker[]             ioThreads;
    private final CPUPoolWorker[]            cpuThreads;
    private final LinkedList<Runnable>       cpuQueue;
    private final LinkedList<AmuseDataArray> ioQueue;

    private final ArrayList<Integer>         availableFrameSequenceNumbers;
    private final AmuseSceneStorage          sceneStorage;

    private final File                       initial_file_bin;

    private final File                       initial_file_gas;

    public void IOJobExecute(AmuseDataArray r) {
        synchronized (ioQueue) {
            ioQueue.addLast(r);
            ioQueue.notify();
        }
    }

    private class IOPoolWorker extends Thread {
        @Override
        public void run() {
            AmuseDataArray runnable;

            while (true) {
                synchronized (ioQueue) {
                    while (ioQueue.isEmpty()) {
                        try {
                            ioQueue.wait();
                        } catch (InterruptedException ignored) {
                        }
                    }

                    runnable = ioQueue.removeFirst();
                }

                // If we don't catch RuntimeException,
                // the pool could leak threads
                try {
                    runnable.run();
                    float[][] particleData = runnable.getParticleData();
                    float[][] gasData = runnable.getGasData();

                    logger.debug("Particles: " + particleData.length);
                    logger.debug("Gasses   : " + gasData.length);

                    AmuseSceneBuilder builder = new AmuseSceneBuilder(
                            sceneStorage, runnable.getDescription(),
                            particleData, gasData);
                    CPUJobExecute(builder);

                    LegendTextureBuilder legendBuilder = new LegendTextureBuilder(
                            sceneStorage, runnable);
                    CPUJobExecute(legendBuilder);
                } catch (RuntimeException e) {
                    logger.error("Runtime exception in IOPoolworker", e);

                } catch (UninitializedException e) {
                    logger.error("Uninitialized exception in IOPoolWorker", e);
                }
            }
        }
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

    public AmuseDatasetManager(File file_bin, File file_gas, int numIOThreads,
            int numCPUThreads) {
        this.initial_file_bin = file_bin;
        this.initial_file_gas = file_gas;

        ioQueue = new LinkedList<AmuseDataArray>();
        cpuQueue = new LinkedList<Runnable>();

        ioThreads = new IOPoolWorker[numIOThreads];
        cpuThreads = new CPUPoolWorker[numCPUThreads];

        for (int i = 0; i < numIOThreads; i++) {
            ioThreads[i] = new IOPoolWorker();
            ioThreads[i].setPriority(Thread.MIN_PRIORITY);
            ioThreads[i].start();
        }
        for (int i = 0; i < numIOThreads; i++) {
            cpuThreads[i] = new CPUPoolWorker();
            cpuThreads[i].setPriority(Thread.MIN_PRIORITY);
            cpuThreads[i].start();
        }

        availableFrameSequenceNumbers = new ArrayList<Integer>();
        sceneStorage = new AmuseSceneStorage(this);

        try {
            while (file_bin != null) {
                int nr = NetCDFUtil.getFrameNumber(file_bin);
                if (NetCDFUtil.getSeqFile(initial_file_gas, nr) != null) {
                    availableFrameSequenceNumbers.add(nr);
                }

                file_bin = NetCDFUtil.getSeqNextFile(file_bin);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void buildScene(AmuseSceneDescription description) {
        int frameNumber = description.getFrameNumber();
        if (frameNumber < 0
                || frameNumber >= availableFrameSequenceNumbers
                        .get(availableFrameSequenceNumbers.size() - 1)) {
            logger.warn("buildImages : Requested frameNumber  " + frameNumber
                    + " out of range.");
        }

        try {
            File file_bin = NetCDFUtil
                    .getSeqFile(initial_file_bin, frameNumber);
            File file_gas = NetCDFUtil
                    .getSeqFile(initial_file_gas, frameNumber);

            NetcdfFile ncFileBin = NetCDFUtil.open(file_bin);
            NetcdfFile ncFileGas = NetCDFUtil.open(file_gas);

            IOJobExecute(new AmuseDataArray(description, ncFileBin, ncFileGas));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public AmuseSceneStorage getSceneStorage() {
        return sceneStorage;
    }

    public int getFrameNumberOfIndex(int index) {
        return availableFrameSequenceNumbers.get(index);
    }

    public int getIndexOfFrameNumber(int frameNumber) {
        return availableFrameSequenceNumbers.indexOf(frameNumber);
    }

    public int getPreviousFrameNumber(int frameNumber) throws IOException {
        int nextNumber = getIndexOfFrameNumber(frameNumber) - 1;

        if (nextNumber >= 0
                && nextNumber < availableFrameSequenceNumbers.size()) {
            return getFrameNumberOfIndex(nextNumber);
        } else {
            throw new IOException("Frame number not available: " + nextNumber);
        }
    }

    public int getNextFrameNumber(int frameNumber) throws IOException {
        int nextNumber = getIndexOfFrameNumber(frameNumber) + 1;

        if (nextNumber >= 0
                && nextNumber < availableFrameSequenceNumbers.size()) {
            return getFrameNumberOfIndex(nextNumber);
        } else {
            throw new IOException("Frame number not available: " + nextNumber);
        }
    }

    public int getNumFrames() {
        return availableFrameSequenceNumbers.size();
    }
}
