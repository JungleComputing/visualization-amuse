package amuse.visualization.amuseAdaptor;

import javax.media.opengl.GL3;
import javax.swing.JFormattedTextField;
import javax.swing.JSlider;

import openglCommon.datastructures.Material;
import openglCommon.exceptions.FileOpeningException;
import openglCommon.exceptions.UninitializedException;
import openglCommon.math.VecF3;
import openglCommon.models.Model;
import openglCommon.models.base.Sphere;
import openglCommon.util.CustomJSlider;
import openglCommon.util.InputHandler;
import amuse.visualization.AmuseSettings;
import amuse.visualization.AmuseWindow;

public class Hdf5TimedPlayer implements Runnable {
    public static enum states {
        UNOPENED, UNINITIALIZED, INITIALIZED, STOPPED, REDRAWING, SNAPSHOTTING, MOVIEMAKING, CLEANUP, WAITINGONFRAME, PLAYING
    }

    private final AmuseSettings settings = AmuseSettings.getInstance();

    private states currentState = states.UNOPENED;
    private int frameNumber, interpolationStep;

    private final int maxInterpolation = settings.getBezierInterpolationSteps();

    private Hdf5Frame currentFrame;

    private boolean running = true;

    private String path = null;
    private String namePrefix = null;

    private long startTime, stopTime;

    private final JSlider timeBar;
    private final JFormattedTextField frameCounter;

    private boolean initialized = false;
    private InputHandler inputHandler;

    private AmuseWindow amuseWindow;

    private Model starModelBase = new Sphere(Material.random(), settings.getStarSubdivision(), 1f, new VecF3(0, 0, 0));
    private Model gasModelBase = new Sphere(Material.random(), settings.getGasSubdivision(), 1f, new VecF3(0, 0, 0));

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

    public Hdf5Frame getFrame() {
        return currentFrame;
    }

    public states getState() {
        return currentState;
    }

    public void init() {
        if (path == null) {
            System.err.println("HDFTimer initialized with no open file.");
            System.exit(1);
        }

        frameNumber = settings.getInitialSimulationFrame();
        interpolationStep = 0;

        try {
            currentFrame = updateFrame(true);
        } catch (FileOpeningException e) {
            System.err.println("Initial simulation frame (settings) not found. Trying again from frame 0.");
            frameNumber = 0;
            try {
                currentFrame = updateFrame(true);
            } catch (FileOpeningException e1) {
                System.err.println("Frame 0 also not found. Exiting.");
                System.exit(1);
            }
        }

        final int initialMaxBar = Hdf5Util.getNumFiles(path);
        timeBar.setMaximum(initialMaxBar);

        initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isPlaying() {
        if ((currentState == states.PLAYING) || (currentState == states.MOVIEMAKING)) {
            return true;
        }

        return false;
    }

    public void movieMode() {
        currentState = states.MOVIEMAKING;
    }

    public void oneBack() {
        stop();
        setFrame(frameNumber - 1, false);
    }

    public void oneForward() {
        stop();
        setFrame(frameNumber + 1, false);
    }

    public void open(String path, String namePrefix) {
        this.path = path;
        this.namePrefix = namePrefix;
    }

    public void redraw() {
        if (initialized) {
            setFrame(frameNumber, true);
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

        frameNumber = settings.getInitialSimulationFrame();

        inputHandler.setRotation(new VecF3(settings.getInitialRotationX(), settings.getInitialRotationY(), 0f));
        inputHandler.setViewDist(settings.getInitialZoom());

        timeBar.setValue(frameNumber);
        frameCounter.setValue(frameNumber);

        currentState = states.STOPPED;

        while (running) {
            if ((currentState == states.PLAYING) || (currentState == states.REDRAWING)
                    || (currentState == states.MOVIEMAKING)) {
                try {
                    startTime = System.currentTimeMillis();

                    if (currentState != states.REDRAWING) {
                        interpolationStep++;
                        if (interpolationStep == maxInterpolation) {
                            frameNumber++;
                            interpolationStep = 0;
                        }
                    }

                    try {
                        currentFrame = updateFrame(false);
                    } catch (final FileOpeningException e) {
                        setFrame(frameNumber - 1, false);
                        currentState = states.WAITINGONFRAME;
                        System.err.println(e);
                        System.err.println(" run File not found, retrying from frame " + frameNumber + ".");
                        continue;
                    }

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

                    timeBar.setValue(frameNumber);
                    frameCounter.setValue(frameNumber);

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
        currentState = states.STOPPED;
        frameNumber = value;
        interpolationStep = 0;

        timeBar.setValue(frameNumber);
        frameCounter.setValue(frameNumber);

        if (overrideUpdate) {
            starModelBase = new Sphere(Material.random(), settings.getStarSubdivision(), 1f, new VecF3(0, 0, 0));
            gasModelBase = new Sphere(Material.random(), settings.getGasSubdivision(), 1f, new VecF3(0, 0, 0));
        }

        try {
            currentFrame = updateFrame(overrideUpdate);
        } catch (final FileOpeningException e) {
            System.err.println(e);
            System.err.println("setFrame File not found, retrying from frame " + frameNumber + ".");

            if (value - 1 < 0) {
                setFrame(0, overrideUpdate);
            } else {
                setFrame(value - 1, overrideUpdate);
            }
            currentState = states.WAITINGONFRAME;
        } catch (final Throwable t) {
            System.err.println("Got error in Hdf5TimedPlayer.setFrame!");
            t.printStackTrace(System.err);
        }
    }

    public void start() {
        currentState = states.PLAYING;
    }

    public void stop() {
        currentState = states.STOPPED;
    }

    private synchronized Hdf5Frame updateFrame(boolean overrideUpdate) throws FileOpeningException {
        Hdf5Frame newFrame = currentFrame;

        if (currentFrame == null || currentFrame.getNumber() != frameNumber || overrideUpdate) {
            if (settings.getBezierInterpolation()) {
                Hdf5Frame frame = new Hdf5Frame(starModelBase, gasModelBase, namePrefix, frameNumber);
                Hdf5Frame nextFrame = new Hdf5Frame(starModelBase, gasModelBase, namePrefix, frameNumber + 1);

                frame.init();
                nextFrame.init();
                try {
                    frame.process(nextFrame);
                } catch (UninitializedException e) {
                    e.printStackTrace();
                }

                newFrame = frame;
            } else {
                Hdf5Frame frame = new Hdf5Frame(starModelBase, gasModelBase, namePrefix, frameNumber);

                frame.init();
                try {
                    frame.process();
                } catch (UninitializedException e) {
                    e.printStackTrace();
                }

                newFrame = frame;
            }
        }

        return newFrame;
    }
}
