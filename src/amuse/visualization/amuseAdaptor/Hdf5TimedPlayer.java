package amuse.visualization.amuseAdaptor;

import java.util.ArrayList;

import javax.media.opengl.GL3;
import javax.swing.JFormattedTextField;
import javax.swing.JSlider;

import openglCommon.math.VecF3;
import openglCommon.util.CustomJSlider;
import openglCommon.util.InputHandler;
import amuse.visualization.AmuseSettings;
import amuse.visualization.AmuseWindow;
import amuse.visualization.amuseAdaptor.exceptions.KeyframeUnavailableException;

public class Hdf5TimedPlayer implements Runnable {
    public static enum states {
        UNOPENED, UNINITIALIZED, INITIALIZED, STOPPED, REDRAWING, SNAPSHOTTING, MOVIEMAKING, CLEANUP, WAITINGONFRAME, PLAYING
    }

    private final AmuseSettings       settings        = AmuseSettings.getInstance(); ;

    private states                    currentState    = states.UNOPENED;
    private int                       currentFrame;

    private Hdf5GasCloudUpdater       cloudUpdater;
    private Hdf5StarUpdater           starUpdater;

    private ArrayList<Star2>          stars;
    private AmuseGasOctreeNode        octreeRoot;

    private boolean                   running         = true;

    private String                    path            = null;
    private String                    namePrefix      = null;
    private final String              gravNamePostfix = ".grav";

    private long                      startTime, stopTime;

    private final JSlider             timeBar;
    private final JFormattedTextField frameCounter;

    private boolean                   initialized     = false;
    private InputHandler              inputHandler;

    private AmuseWindow               amuseWindow;

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
        currentFrame = 0;
        timeBar.setValue(0);
        frameCounter.setValue(0);
        timeBar.setMaximum(0);
    }

    public void delete(GL3 gl) {
        // TODO
    }

    public int getFrame() {
        return currentFrame;
    }

    public synchronized AmuseGasOctreeNode getOctreeRoot() {
        return octreeRoot;
    }

    public synchronized ArrayList<Star2> getStars() {
        return stars;
    }

    public states getState() {
        return currentState;
    }

    public void init() {
        if (path == null) {
            System.err.println("HDFTimer initialized with no open file.");
            System.exit(1);
        }

        starUpdater = new Hdf5StarUpdater(namePrefix);
        cloudUpdater = new Hdf5GasCloudUpdater(namePrefix);

        new Thread(starUpdater).start();
        new Thread(cloudUpdater).start();

        while (starUpdater.getCurrentFrame() == 0 || cloudUpdater.getCurrentFrame() == 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        try {
            updateFrame(false);
        } catch (KeyframeUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        final int initialMaxBar = starUpdater.getLastFrame();
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
        setFrame(currentFrame - 1, false);
    }

    public void oneForward() {
        stop();
        setFrame(currentFrame + 1, false);
    }

    public void open(String path, String namePrefix) {
        this.path = path;
        this.namePrefix = namePrefix;
    }

    public void redraw() {
        if (initialized) {
            setFrame(currentFrame, true);
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

        currentFrame = settings.getInitialSimulationFrame();

        inputHandler.setRotation(new VecF3(settings.getInitialRotationX(), settings.getInitialRotationY(), 0f));
        inputHandler.setViewDist(settings.getInitialZoom());

        timeBar.setValue(currentFrame);
        frameCounter.setValue(currentFrame);

        currentState = states.STOPPED;

        while (running) {
            if ((currentState == states.PLAYING) || (currentState == states.REDRAWING)
                    || (currentState == states.MOVIEMAKING)) {
                try {
                    startTime = System.currentTimeMillis();

                    try {
                        updateFrame(false);
                    } catch (final KeyframeUnavailableException e) {
                        setFrame(currentFrame - 1, false);
                        currentState = states.WAITINGONFRAME;
                        System.err.println(e);
                        System.err.println(" run File not found, retrying from frame " + currentFrame + ".");
                        continue;
                    }

                    if (currentState == states.MOVIEMAKING) {
                        if (settings.getMovieRotate()) {
                            final VecF3 rotation = inputHandler.getRotation();
                            System.out.println("Simulation frame: " + currentFrame + ", Rotation x: " + rotation.get(0)
                                    + " y: " + rotation.get(1));
                            amuseWindow.makeSnapshot(String.format("%05d", (currentFrame)));

                            rotation.set(1, rotation.get(1) + settings.getMovieRotationSpeedDef());
                            inputHandler.setRotation(rotation);
                        } else {
                            amuseWindow.makeSnapshot(String.format("%05d", currentFrame));
                        }
                    }

                    if (currentState != states.REDRAWING) {
                        currentFrame++;
                    }

                    timeBar.setValue(currentFrame);
                    frameCounter.setValue(currentFrame);

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
        currentFrame = value;

        timeBar.setValue(currentFrame);
        frameCounter.setValue(currentFrame);

        try {
            updateFrame(overrideUpdate);
        } catch (final KeyframeUnavailableException e) {
            System.err.println(e);
            System.err.println("setFrame File not found, retrying from frame " + currentFrame + ".");

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

    private synchronized void updateFrame(boolean overrideUpdate) throws KeyframeUnavailableException {
        final ArrayList<Star2> newStars = starUpdater.getStarsAt(currentFrame, 0);
        final AmuseGasOctreeNode newOctreeRoot = cloudUpdater.getOctreeAt(currentFrame, 0);

        synchronized (this) {
            stars = newStars;
            octreeRoot = newOctreeRoot;
        }
    }
}
