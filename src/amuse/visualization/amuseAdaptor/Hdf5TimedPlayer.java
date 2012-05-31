package amuse.visualization.amuseAdaptor;

import javax.media.opengl.GL3;
import javax.swing.JFormattedTextField;
import javax.swing.JSlider;

import openglCommon.math.VecF3;
import openglCommon.util.CustomJSlider;
import openglCommon.util.InputHandler;
import amuse.visualization.AmuseSettings;
import amuse.visualization.AmuseWindow;

public class Hdf5TimedPlayer implements Runnable {
    public static enum states {
        UNOPENED, UNINITIALIZED, INITIALIZED, STOPPED, REDRAWING, SNAPSHOTTING, MOVIEMAKING, WAITINGONFRAME, PLAYING
    }

    private final AmuseSettings settings = AmuseSettings.getInstance();

    private states currentState = states.UNOPENED;
    private int frameNumber, interpolationStep;

    private final int maxInterpolation = settings.getBezierInterpolationSteps();

    private Hdf5Frame currentFrame;

    private boolean running = true;

    private String path = null;

    private long startTime, stopTime;

    private final JSlider timeBar;
    private final JFormattedTextField frameCounter;

    private boolean initialized = false;
    private InputHandler inputHandler;

    private AmuseWindow amuseWindow;

    private Hdf5FrameManager frameManager;

    public Hdf5TimedPlayer(CustomJSlider timeBar, JFormattedTextField frameCounter) {
        this.timeBar = timeBar;
        this.frameCounter = frameCounter;
    }

    public Hdf5TimedPlayer(AmuseWindow amuseWindow, JSlider timeBar, JFormattedTextField frameCounter) {
        this.amuseWindow = amuseWindow;
        inputHandler = InputHandler.getInstance();
        this.timeBar = timeBar;
        this.frameCounter = frameCounter;
    }

    public void close() {
        running = false;
        initialized = false;
        frameNumber = 0;
        timeBar.setValue(0);
        frameCounter.setValue(0);
        timeBar.setMaximum(0);
    }

    public void delete(GL3 gl) {
        // TODO
    }

    public int getFrameNumber() {
        return frameNumber;
    }

    public int getInterpolationStep() {
        return interpolationStep;
    }

    public Hdf5Frame getFrame() {
        return currentFrame;
    }

    public void init() {
        if (path == null) {
            System.err.println("HDFTimer initialized with no open file.");
            System.exit(1);
        }

        final int initialMaxBar = Hdf5Util.getNumFiles(path);
        timeBar.setMaximum(initialMaxBar);

        setFrame(settings.getInitialSimulationFrame(), true);

        initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public synchronized boolean isPlaying() {
        if ((currentState == states.PLAYING) || (currentState == states.MOVIEMAKING)) {
            return true;
        }

        return false;
    }

    public synchronized void movieMode() {
        currentState = states.MOVIEMAKING;
    }

    public void oneBack() {
        setFrame(frameNumber - 1, false);
    }

    public void oneForward() {
        setFrame(frameNumber + 1, false);
    }

    public void open(String path, String namePrefix) {
        this.path = path;
        this.frameManager = new Hdf5FrameManager(0, namePrefix);
    }

    public synchronized void redraw() {
        if (initialized) {
            updateFrame(frameNumber, true);
            currentState = states.REDRAWING;
        }
    }

    public void rewind() {
        setFrame(0, false);
    }

    @Override
    public void run() {
        if (!initialized) {
            System.err.println("HDFTimer started while not initialized.");
            System.exit(1);
        }

        inputHandler.setRotation(new VecF3(settings.getInitialRotationX(), settings.getInitialRotationY(), 0f));
        inputHandler.setViewDist(settings.getInitialZoom());

        stop();

        while (running) {
            if ((currentState == states.PLAYING) || (currentState == states.REDRAWING)
                    || (currentState == states.MOVIEMAKING)) {
                try {
                    startTime = System.currentTimeMillis();

                    if (currentState == states.MOVIEMAKING) {
                        if (settings.getMovieRotate()) {
                            final VecF3 rotation = inputHandler.getRotation();
                            System.out.println("Simulation frame: " + frameNumber + ", Rotation x: " + rotation.get(0)
                                    + " y: " + rotation.get(1));
                            amuseWindow.makeSnapshot(String.format("%05d", (frameNumber)));

                            rotation.set(1, rotation.get(1) + settings.getMovieRotationSpeedDef());
                            inputHandler.setRotation(rotation);
                        } else {
                            amuseWindow.makeSnapshot(String.format("%05d", frameNumber));
                        }
                    }

                    // Forward either frame or interpolation step, depending on
                    // settings
                    if (currentState != states.REDRAWING) {
                        if (settings.getBezierInterpolation()) {
                            interpolationStep++;
                            if (interpolationStep == maxInterpolation) {
                                updateFrame(frameNumber + 1, false);
                                interpolationStep = 0;
                            }
                        } else {
                            updateFrame(frameNumber + 1, false);
                        }
                    }

                    // Wait for the _rest_ of the timeframe
                    stopTime = System.currentTimeMillis();
                    if (((startTime - stopTime) < settings.getWaitTimeMovie()) && (currentState != states.MOVIEMAKING)) {
                        Thread.sleep(settings.getWaitTimeMovie() - (startTime - stopTime));
                    }
                } catch (final InterruptedException e) {
                    System.err.println("Interrupted while playing.");
                }
            } else if (currentState == states.STOPPED) {
                try {
                    Thread.sleep(settings.getWaitTimeRetry());
                } catch (final InterruptedException e) {
                    System.err.println("Interrupted while stopped.");
                }
            } else if (currentState == states.REDRAWING) {
                currentState = states.STOPPED;
            } else if (currentState == states.WAITINGONFRAME) {
                try {
                    Thread.sleep(settings.getWaitTimeRetry());
                    currentState = states.PLAYING;
                } catch (final InterruptedException e) {
                    System.err.println("Interrupted while waiting.");
                }
            }
        }
    }

    public void setFrame(int value, boolean overrideUpdate) {
        // System.out.println("setValue?");
        stop();
        interpolationStep = 0;

        if (overrideUpdate) {
            frameManager.resetModels();
        }

        updateFrame(value, overrideUpdate);
    }

    public synchronized void start() {
        currentState = states.PLAYING;
    }

    public synchronized void stop() {
        currentState = states.STOPPED;
    }

    private synchronized void updateFrame(int frameNumber, boolean overrideUpdate) {
        Hdf5Frame newFrame = currentFrame;

        if (currentFrame == null || currentFrame.getNumber() != frameNumber || overrideUpdate) {
            Hdf5Frame frame = frameManager.getFrame(frameNumber);

            if (!frame.isError()) {
                newFrame = frame;
                this.frameNumber = frameNumber;
                this.timeBar.setValue(frameNumber);
                this.frameCounter.setValue(frameNumber);
            }
        }

        currentFrame = newFrame;
    }
}
