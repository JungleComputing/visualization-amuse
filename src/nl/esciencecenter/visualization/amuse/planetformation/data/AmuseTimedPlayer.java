package nl.esciencecenter.visualization.amuse.planetformation.data;

import java.io.File;
import java.io.IOException;

import javax.media.opengl.GL3;
import javax.swing.JFormattedTextField;

import nl.esciencecenter.visualization.amuse.planetformation.AmuseSettings;
import nl.esciencecenter.visualization.amuse.planetformation.glue.data.GlueTimedPlayer;
import nl.esciencecenter.visualization.amuse.planetformation.interfaces.SceneStorage;
import nl.esciencecenter.visualization.amuse.planetformation.interfaces.TimedPlayer;
import nl.esciencecenter.visualization.openglCommon.input.InputHandler;
import nl.esciencecenter.visualization.openglCommon.math.VecF3;
import nl.esciencecenter.visualization.openglCommon.swing.CustomJSlider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmuseTimedPlayer implements TimedPlayer {
    private AmuseDatasetManager       dsManager;
    private AmuseSceneStorage         sceneStorage;

    private states                    currentState       = states.UNOPENED;
    private final static Logger       logger             = LoggerFactory
                                                                 .getLogger(GlueTimedPlayer.class);

    private final AmuseSettings       settings           = AmuseSettings
                                                                 .getInstance();
    private int                       frameNumber;

    private final boolean             running            = true;
    private boolean                   initialized        = false;
    private final boolean             fileLessMode       = false;

    private long                      startTime, stopTime;

    private final CustomJSlider       timeBar;
    private final JFormattedTextField frameCounter;

    private final InputHandler        inputHandler;

    private boolean                   needsScreenshot    = false;
    private String                    screenshotFilename = "";

    private final long                waittime           = settings
                                                                 .getWaitTimeMovie();

    public AmuseTimedPlayer(CustomJSlider timeBar,
            JFormattedTextField frameCounter) {
        this.timeBar = timeBar;
        this.frameCounter = frameCounter;
        this.inputHandler = InputHandler.getInstance();
    }

    public void init(File file_bin, File file_gas) {
        this.dsManager = new AmuseDatasetManager(file_bin, file_gas, 1, 4);
        this.sceneStorage = dsManager.getSceneStorage();

        frameNumber = dsManager.getFrameNumberOfIndex(0);
        final int initialMaxBar = dsManager.getNumFrames() - 1;

        timeBar.setMaximum(initialMaxBar);
        timeBar.setMinimum(0);

        updateFrame(frameNumber, true);

        initialized = true;
    }

    private void setCurrentState(states newState) {
        currentState = newState;
    }

    @Override
    public synchronized void oneBack() {
        stop();

        try {
            int newFrameNumber = dsManager.getPreviousFrameNumber(frameNumber);
            updateFrame(newFrameNumber, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void oneForward() {
        stop();

        try {
            int newFrameNumber = dsManager.getNextFrameNumber(frameNumber);
            updateFrame(newFrameNumber, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void redraw() {
        if (initialized) {
            updateFrame(frameNumber, true);
            setCurrentState(states.REDRAWING);
        }
    }

    @Override
    public synchronized void rewind() {
        stop();
        updateFrame(0, false);
    }

    @Override
    public void run() {
        if (!initialized && !fileLessMode) {
            System.err.println("HDFTimer started while not initialized.");
            System.exit(1);
        }

        inputHandler.setRotation(new VecF3(settings.getInitialRotationX(),
                settings.getInitialRotationY(), 0f));
        inputHandler.setViewDist(settings.getInitialZoom());

        // inputHandler.setRotation(new VecF3(bezierPoints.get(0).get(0),
        // bezierPoints.get(0).get(1), 0f));
        // inputHandler.setViewDist(bezierPoints.get(0).get(2));

        int frame = settings.getInitialSimulationFrame();
        updateFrame(frame, true);

        stop();

        while (running) {
            if ((currentState == states.PLAYING)
                    || (currentState == states.REDRAWING)
                    || (currentState == states.MOVIEMAKING)) {
                try {
                    if (!isScreenshotNeeded()) {
                        startTime = System.currentTimeMillis();

                        if (currentState == states.MOVIEMAKING) {
                            final VecF3 rotation = inputHandler.getRotation();
                            if (settings.getMovieRotate()) {
                                rotation.set(
                                        1,
                                        rotation.get(1)
                                                + settings
                                                        .getMovieRotationSpeedDef());
                                inputHandler.setRotation(rotation);
                                setScreenshotNeeded(true);
                            } else {
                                setScreenshotNeeded(true);
                            }
                        }

                        // Forward frame
                        if (currentState != states.REDRAWING) {
                            try {
                                int newFrameNumber = dsManager
                                        .getNextFrameNumber(frameNumber);

                                if (sceneStorage.doneWithLastRequest()) {
                                    updateFrame(newFrameNumber, false);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        // Wait for the _rest_ of the timeframe
                        stopTime = System.currentTimeMillis();
                        long spentTime = stopTime - startTime;

                        if (spentTime < waittime) {
                            Thread.sleep(waittime - spentTime);
                        }
                    }
                } catch (final InterruptedException e) {
                    System.err.println("Interrupted while playing.");
                }
            } else if (currentState == states.STOPPED) {
                try {
                    Thread.sleep(100);
                } catch (final InterruptedException e) {
                    System.err.println("Interrupted while stopped.");
                }
            } else if (currentState == states.REDRAWING) {
                setCurrentState(states.STOPPED);
            } else if (currentState == states.WAITINGONFRAME) {
                try {
                    Thread.sleep(settings.getWaitTimeRetry());
                    setCurrentState(states.PLAYING);
                } catch (final InterruptedException e) {
                    System.err.println("Interrupted while waiting.");
                }
            }
        }
    }

    @Override
    public synchronized void setFrame(int value, boolean overrideUpdate) {
        stop();

        updateFrame(dsManager.getFrameNumberOfIndex(value), overrideUpdate);
    }

    private synchronized void updateFrame(int newFrameNumber,
            boolean overrideUpdate) {
        if (dsManager != null) {
            if (newFrameNumber != frameNumber || overrideUpdate) {

                frameNumber = newFrameNumber;
                settings.setCurrentFrameNumber(newFrameNumber);

                this.timeBar.setValue(dsManager
                        .getIndexOfFrameNumber(newFrameNumber));
                this.frameCounter.setValue(dsManager
                        .getIndexOfFrameNumber(newFrameNumber));
            }
        }
    }

    @Override
    public SceneStorage getSceneStorage() {
        return sceneStorage;
    }

    @Override
    public synchronized void start() {
        setCurrentState(states.PLAYING);
    }

    @Override
    public synchronized void stop() {
        setCurrentState(states.STOPPED);
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public synchronized boolean isPlaying() {
        if ((currentState == states.PLAYING)
                || (currentState == states.MOVIEMAKING)) {
            return true;
        }

        return false;
    }

    @Override
    public synchronized void movieMode() {
        setCurrentState(states.MOVIEMAKING);
    }

    @Override
    public synchronized void setScreenshotNeeded(boolean value) {
        if (value) {
            final VecF3 rotation = inputHandler.getRotation();
            final float viewDist = inputHandler.getViewDist();

            System.out.println("Simulation frame: " + frameNumber
                    + ", Rotation x: " + rotation.get(0) + " y: "
                    + rotation.get(1) + " , viewDist: " + viewDist);

            screenshotFilename = settings.getScreenshotPath()
                    + String.format("%05d", (frameNumber)) + ".png";
        }
        needsScreenshot = value;
    }

    @Override
    public synchronized boolean isScreenshotNeeded() {
        return needsScreenshot;
    }

    @Override
    public synchronized String getScreenshotFileName() {
        return screenshotFilename;
    }

    @Override
    public void close() {
        initialized = false;
        frameNumber = 0;
        timeBar.setValue(0);
        frameCounter.setValue(0);
        timeBar.setMaximum(0);
    }

    @Override
    public int getFrameNumber() {
        return frameNumber;
    }

    @Override
    public void delete(GL3 gl) {
        // TODO Auto-generated method stub
    }
}
