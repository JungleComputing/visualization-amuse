package amuse.visualization.amuseAdaptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.media.opengl.GL3;
import javax.swing.JFormattedTextField;
import javax.swing.JSlider;

import openglCommon.exceptions.FileOpeningException;
import openglCommon.math.VecF3;
import openglCommon.models.Model;
import openglCommon.util.CustomJSlider;
import openglCommon.util.InputHandler;
import amuse.visualization.AmuseSettings;
import amuse.visualization.AmuseTest;
import amuse.visualization.AmuseWindow;

public class Hdf5TimedPlayer implements Runnable {
    public static enum states {
        UNOPENED, UNINITIALIZED, INITIALIZED, STOPPED, REDRAWING, SNAPSHOTTING, MOVIEMAKING, CLEANUP, WAITINGONFRAME, PLAYING
    }

    private final AmuseSettings       settings        = AmuseSettings.getInstance(); ;

    private states                    currentState    = states.UNOPENED;
    private int                       currentFrame;

    private ArrayList<Star>           stars;
    private AmuseGasOctreeNode        octreeRoot;

    private boolean                   running         = true;

    private String                    path            = null;
    private String                    namePrefix      = null;
    private final String              gravNamePostfix = ".grav";

    private long                      startTime, stopTime;

    private final JSlider             timeBar;
    private final JFormattedTextField frameCounter;

    private HashMap<Integer, Model>   starModels;
    private HashMap<Integer, Model>   cloudModels;

    private boolean                   initialized     = false;
    private InputHandler              inputHandler;

    private Hdf5Snapshotter           snappy;
    private AmuseWindow               amuseWindow;

    public Hdf5TimedPlayer(AmuseTest amuseTest, CustomJSlider timeBar, JFormattedTextField frameCounter) {
        // this.amuseWindow = glw;
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
        for (final Entry<Integer, Model> e : starModels.entrySet()) {
            final Model m = e.getValue();
            m.delete(gl);
        }
        for (final Entry<Integer, Model> e : cloudModels.entrySet()) {
            final Model m = e.getValue();
            m.delete(gl);
        }
    }

    public int getFrame() {
        return currentFrame;
    }

    public synchronized AmuseGasOctreeNode getOctreeRoot() {
        return octreeRoot;
    }

    public synchronized ArrayList<Star> getStars() {
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

        snappy = new Hdf5Snapshotter();

        // The star and gas models can be re-used for efficiency, we therefore
        // store them in these central databases
        starModels = new HashMap<Integer, Model>();
        cloudModels = new HashMap<Integer, Model>();

        final int initialMaxBar = Hdf5StarReader.getNumFiles(path, gravNamePostfix);

        timeBar.setMaximum(initialMaxBar);

        try {
            updateFrame(false);
        } catch (final FileOpeningException e) {
            System.err.println("Failed to open file.");
            System.exit(1);
        }

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

        try {
            updateFrame(false);
        } catch (final FileOpeningException e) {
            System.err.println("Initial file not found");
            System.exit(1);
        }

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
                    } catch (final FileOpeningException e) {
                        setFrame(currentFrame - 1, false);
                        currentState = states.WAITINGONFRAME;
                        System.err.println("File not found, retrying from frame " + currentFrame + ".");
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
        } catch (final FileOpeningException e) {
            System.err.println("File not found, retrying from frame " + currentFrame + ".");

            setFrame(value - 1, overrideUpdate);
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

    private synchronized void updateFrame(boolean overrideUpdate) throws FileOpeningException {
        snappy.open(namePrefix, currentFrame, settings.getLOD(), cloudModels, overrideUpdate);
        final ArrayList<Star> newStars = snappy.getStars();
        final AmuseGasOctreeNode newOctreeRoot = snappy.getOctreeRoot();

        synchronized (this) {
            stars = newStars;
            octreeRoot = newOctreeRoot;
        }
    }
}
