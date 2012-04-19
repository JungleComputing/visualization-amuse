package amuse.visualization;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import openglCommon.GLOffscreenContext;
import openglCommon.datastructures.FBO;
import openglCommon.datastructures.Material;
import openglCommon.math.Color4;
import openglCommon.math.VecF3;
import openglCommon.models.Axis;
import openglCommon.models.Model;
import openglCommon.models.Text;
import openglCommon.models.base.Quad;
import openglCommon.shaders.Program;
import openglCommon.shaders.ProgramLoader;
import openglCommon.textures.Perlin3D;
import openglCommon.util.CustomJSlider;
import openglCommon.util.GLProfileSelector;
import openglCommon.util.InputHandler;
import amuse.visualization.amuseAdaptor.Astrophysics;
import amuse.visualization.amuseAdaptor.Hdf5TimedPlayer;

import com.jogamp.opengl.util.FPSAnimator;

public class AmuseTest extends JPanel implements GLEventListener {
    private static final long          serialVersionUID = 1L;
    private final static AmuseSettings settings         = AmuseSettings.getInstance();

    public static void main(String[] arguments) {
        String cmdlnfileName = null;
        String path = "";

        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i].equals("-o")) {
                i++;
                cmdlnfileName = arguments[i];
                final File cmdlnfile = new File(cmdlnfileName);
                path = cmdlnfile.getPath().substring(0, cmdlnfile.getPath().length() - cmdlnfile.getName().length());
            } else if (arguments[i].equals("-resume")) {
                i++;
                AmuseTest.settings.setInitial_simulation_frame(Integer.parseInt(arguments[i]));
                i++;
                AmuseTest.settings.setInitial_rotation_x(Float.parseFloat(arguments[i]));
                i++;
                AmuseTest.settings.setInitial_rotation_y(Float.parseFloat(arguments[i]));
            } else {
                cmdlnfileName = null;
                path = System.getProperty("user.dir");
            }
        }

        final JFrame frame = new JFrame("Amuse Visualization");
        frame.setPreferredSize(new Dimension(AmuseTest.settings.getDefaultScreenWidth(), AmuseTest.settings
                .getDefaultScreenHeight()));

        final AmuseTest amuseTest = new AmuseTest();

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    frame.getContentPane().add(amuseTest);

                    frame.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent we) {
                            amuseTest.close();
                            System.exit(0);
                        }
                    });

                } catch (final Exception e) {
                    e.printStackTrace(System.err);
                    System.exit(1);
                }
            }
        });

        // Display the window.
        frame.pack();

        // center on screen
        frame.setLocationRelativeTo(null);

        frame.setVisible(true);
    }

    private final ProgramLoader   loader;
    private final InputHandler    inputHandler;
    private final GLCanvas        glCanvas;
    private final CustomJSlider   timeBar;
    private final Hdf5TimedPlayer timer;
    private int                   canvasWidth;
    private int                   canvasHeight;

    private final GLContext       offScreenContext;
    private Program               animatedTurbulenceShader;
    private Program               pplShader;
    private Program               gasShader;
    private Program               axesShader;
    private Program               postprocessShader;
    private Program               gaussianBlurShader;
    private boolean               post_process;

    private FBO                   starHaloFBO, starHaloFBO4k;
    private FBO                   gasFBO, gasFBO4k;
    private FBO                   starFBO, starFBO4k;
    private FBO                   axesFBO, axesFBO4k;
    private FBO                   hudFBO, hudFBO4k;

    private Model                 FSQ_postprocess, FSQ_blur;
    private Model                 xAxis, yAxis, zAxis;
    private Text                  myText;
    private Perlin3D              noiseTex;

    public AmuseTest() {
        loader = new ProgramLoader();
        inputHandler = InputHandler.getInstance();

        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        setLayout(new BorderLayout(0, 0));

        GLProfileSelector.printAvailable();
        final GLProfile glp = GLProfile.get(GLProfile.GL2ES2);

        // Standard GL3 capabilities
        final GLCapabilities glCapabilities = new GLCapabilities(glp);

        glCapabilities.setHardwareAccelerated(true);
        glCapabilities.setDoubleBuffered(true);

        // Anti-Aliasing
        glCapabilities.setSampleBuffers(true);
        glCapabilities.setAlphaBits(4);
        glCapabilities.setNumSamples(4);

        // Create the canvas
        offScreenContext = new GLOffscreenContext(glp).getContext();
        glCanvas = new GLCanvas(glCapabilities, offScreenContext);

        // Add Mouse event listener
        final InputHandler inputHandler = InputHandler.getInstance();
        glCanvas.addMouseListener(inputHandler);
        glCanvas.addMouseMotionListener(inputHandler);
        glCanvas.addMouseWheelListener(inputHandler);

        // Add key event listener
        glCanvas.addKeyListener(inputHandler);

        // Make the GLEventListener
        glCanvas.addGLEventListener(this);

        // Set up animator
        final FPSAnimator animator = new FPSAnimator(glCanvas, 60);
        animator.start();

        add(glCanvas, BorderLayout.CENTER);

        setVisible(true);
        glCanvas.setFocusable(true);
        glCanvas.requestFocusInWindow();

        timeBar = new openglCommon.util.CustomJSlider();
        timeBar.setValue(0);
        timeBar.setMajorTickSpacing(5);
        timeBar.setMinorTickSpacing(1);
        timeBar.setMaximum(0);
        timeBar.setMinimum(0);
        timeBar.setPaintTicks(true);
        timeBar.setSnapToTicks(true);

        timer = new Hdf5TimedPlayer(this, timeBar, new JFormattedTextField());
    }

    void close() {
        dispose(glCanvas);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        drawable.getContext().makeCurrent();
        // TODO Auto-generated method stub

    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        drawable.getContext().makeCurrent();
        // TODO Auto-generated method stub

    }

    @Override
    public void init(GLAutoDrawable drawable) {
        // First, init the 'normal' context
        drawable.getContext().makeCurrent();
        GL3 gl = drawable.getGL().getGL3();

        canvasWidth = drawable.getWidth();
        canvasHeight = drawable.getHeight();

        // Anti-Aliasing
        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
        gl.glEnable(GL2GL3.GL_POLYGON_SMOOTH);
        gl.glHint(GL2GL3.GL_POLYGON_SMOOTH_HINT, GL.GL_NICEST);

        // Depth testing
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthFunc(GL.GL_LEQUAL);
        gl.glClearDepth(1.0f);

        // Culling
        gl.glEnable(GL.GL_CULL_FACE);
        gl.glCullFace(GL.GL_BACK);

        // Enable Blending (needed for both Transparency and
        // Anti-Aliasing
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL.GL_BLEND);

        // Enable Vertical Sync
        gl.setSwapInterval(1);

        // Set black background
        gl.glClearColor(0f, 0f, 0f, 0f);

        // Now, init the offscreen context as well
        drawable.getContext().release();
        if (offScreenContext != null) {
            try {
                final int status = offScreenContext.makeCurrent();
                if ((status != GLContext.CONTEXT_CURRENT) && (status != GLContext.CONTEXT_CURRENT_NEW)) {
                    System.err.println("Error swapping context to offscreen.");
                }
            } catch (final GLException e) {
                System.err.println("Exception while swapping context to offscreen.");
                e.printStackTrace();
            }

            gl = offScreenContext.getGL().getGL3();

            // Anti-Aliasing
            gl.glEnable(GL.GL_LINE_SMOOTH);
            gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
            gl.glEnable(GL2GL3.GL_POLYGON_SMOOTH);
            gl.glHint(GL2GL3.GL_POLYGON_SMOOTH_HINT, GL.GL_NICEST);

            // Depth testing
            gl.glEnable(GL.GL_DEPTH_TEST);
            gl.glDepthFunc(GL.GL_LEQUAL);
            gl.glClearDepth(1.0f);

            // Culling
            gl.glEnable(GL.GL_CULL_FACE);
            gl.glCullFace(GL.GL_BACK);

            // Enable Blending (needed for both Transparency and
            // Anti-Aliasing
            gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
            gl.glEnable(GL.GL_BLEND);

            // Enable Vertical Sync
            gl.setSwapInterval(1);

            // Set black background
            gl.glClearColor(0f, 0f, 0f, 0f);

            try {
                offScreenContext.release();
            } catch (final GLException e) {
                System.err.println("Exception while releasing offscreen context.");
                e.printStackTrace();
            }
        }

        drawable.getContext().makeCurrent();
        gl = drawable.getGL().getGL3();

        // Load and compile shaders, then use program.
        try {
            animatedTurbulenceShader = loader.createProgram(gl, "shaders/vs_sunsurface.vp",
                    "shaders/fs_animatedTurbulence.fp");
            pplShader = loader.createProgram(gl, "shaders/vs_ppl.vp", "shaders/fs_ppl.fp");
            axesShader = loader.createProgram(gl, "shaders/vs_axes.vp", "shaders/fs_axes.fp");
            gasShader = loader.createProgram(gl, "shaders/vs_gas.vp", "shaders/fs_gas.fp");

            if (post_process) {
                postprocessShader = loader.createProgram(gl, "shaders/vs_postprocess.vp", "shaders/fs_postprocess.fp");
            }
            if (post_process) {
                gaussianBlurShader = loader.createProgram(gl, "shaders/vs_postprocess.vp",
                        "shaders/fs_gaussian_blur.fp");
            }
        } catch (final Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // AXES
        final Color4 axisColor = new Color4(0f, 1f, 0f, 1f);
        final Material axisMaterial = new Material(axisColor, axisColor, axisColor);
        xAxis = new Axis(axisMaterial, new VecF3(-800f, 0f, 0f), new VecF3(800f, 0f, 0f),
                Astrophysics.toScreenCoord(1), Astrophysics.toScreenCoord(.2));
        xAxis.init(gl);
        yAxis = new Axis(axisMaterial, new VecF3(0f, -800f, 0f), new VecF3(0f, 800f, 0f),
                Astrophysics.toScreenCoord(1), Astrophysics.toScreenCoord(.2));
        yAxis.init(gl);
        zAxis = new Axis(axisMaterial, new VecF3(0f, 0f, -800f), new VecF3(0f, 0f, 800f),
                Astrophysics.toScreenCoord(1), Astrophysics.toScreenCoord(.2));
        zAxis.init(gl);

        // TEXT
        myText = new Text(axisMaterial);
        myText.init(gl);

        // FULL SCREEN QUADS
        FSQ_postprocess = new Quad(Material.random(), 2, 2, new VecF3(0, 0, 0.1f));
        FSQ_postprocess.init(gl);

        FSQ_blur = new Quad(Material.random(), 2, 2, new VecF3(0, 0, 0.1f));
        FSQ_blur.init(gl);

        // TEXTURES
        noiseTex = new Perlin3D(128, GL.GL_TEXTURE0);
        noiseTex.init(gl);

        // Full screen textures (for post processing) done with FBO's
        starFBO = new FBO(canvasWidth, canvasHeight, GL.GL_TEXTURE1);
        starHaloFBO = new FBO(canvasWidth, canvasHeight, GL.GL_TEXTURE2);
        gasFBO = new FBO(canvasWidth, canvasHeight, GL.GL_TEXTURE3);
        axesFBO = new FBO(canvasWidth, canvasHeight, GL.GL_TEXTURE4);
        hudFBO = new FBO(canvasWidth, canvasHeight, GL.GL_TEXTURE5);

        starFBO.init(gl);
        starHaloFBO.init(gl);
        gasFBO.init(gl);
        axesFBO.init(gl);
        hudFBO.init(gl);

        final int ssWidth = AmuseTest.settings.getScreenshotScreenWidth();
        final int ssHeight = AmuseTest.settings.getScreenshotScreenHeight();

        starFBO4k = new FBO(ssWidth, ssHeight, GL.GL_TEXTURE1);
        starHaloFBO4k = new FBO(ssWidth, ssHeight, GL.GL_TEXTURE2);
        gasFBO4k = new FBO(ssWidth, ssHeight, GL.GL_TEXTURE3);
        axesFBO4k = new FBO(ssWidth, ssHeight, GL.GL_TEXTURE4);
        hudFBO4k = new FBO(ssWidth, ssHeight, GL.GL_TEXTURE5);

        starFBO4k.init(gl);
        starHaloFBO4k.init(gl);
        gasFBO4k.init(gl);
        axesFBO4k.init(gl);
        hudFBO4k.init(gl);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
        drawable.getContext().makeCurrent();
        final GL3 gl = drawable.getGL().getGL3();

        starFBO = new FBO(canvasWidth, canvasHeight, GL.GL_TEXTURE1);
        starHaloFBO = new FBO(canvasWidth, canvasHeight, GL.GL_TEXTURE2);
        gasFBO = new FBO(canvasWidth, canvasHeight, GL.GL_TEXTURE3);
        axesFBO = new FBO(canvasWidth, canvasHeight, GL.GL_TEXTURE4);
        hudFBO = new FBO(canvasWidth, canvasHeight, GL.GL_TEXTURE5);

        starFBO.init(gl);
        starHaloFBO.init(gl);
        gasFBO.init(gl);
        axesFBO.init(gl);
        hudFBO.init(gl);
    }

}
