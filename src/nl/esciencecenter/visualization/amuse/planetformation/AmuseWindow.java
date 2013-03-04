package nl.esciencecenter.visualization.amuse.planetformation;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;

import nl.esciencecenter.visualization.amuse.planetformation.glue.data.GlueSceneDescription;
import nl.esciencecenter.visualization.amuse.planetformation.interfaces.SceneDescription;
import nl.esciencecenter.visualization.amuse.planetformation.interfaces.SceneStorage;
import nl.esciencecenter.visualization.amuse.planetformation.interfaces.TimedPlayer;
import nl.esciencecenter.visualization.amuse.planetformation.interfaces.VisualScene;
import nl.esciencecenter.visualization.openglCommon.datastructures.FBO;
import nl.esciencecenter.visualization.openglCommon.datastructures.IntPBO;
import nl.esciencecenter.visualization.openglCommon.datastructures.Material;
import nl.esciencecenter.visualization.openglCommon.exceptions.UninitializedException;
import nl.esciencecenter.visualization.openglCommon.input.InputHandler;
import nl.esciencecenter.visualization.openglCommon.math.Color4;
import nl.esciencecenter.visualization.openglCommon.math.MatF4;
import nl.esciencecenter.visualization.openglCommon.math.MatrixFMath;
import nl.esciencecenter.visualization.openglCommon.math.Point4;
import nl.esciencecenter.visualization.openglCommon.math.VecF3;
import nl.esciencecenter.visualization.openglCommon.math.VecF4;
import nl.esciencecenter.visualization.openglCommon.models.Axis;
import nl.esciencecenter.visualization.openglCommon.models.Model;
import nl.esciencecenter.visualization.openglCommon.models.Quad;
import nl.esciencecenter.visualization.openglCommon.shaders.Program;
import nl.esciencecenter.visualization.openglCommon.shaders.ProgramLoader;
import nl.esciencecenter.visualization.openglCommon.text.FontFactory;
import nl.esciencecenter.visualization.openglCommon.text.MultiColorText;
import nl.esciencecenter.visualization.openglCommon.text.TypecastFont;
import nl.esciencecenter.visualization.openglCommon.textures.ByteBufferTexture;
import nl.esciencecenter.visualization.openglCommon.textures.Perlin3D;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmuseWindow implements GLEventListener {
    private final static Logger   logger         = LoggerFactory
                                                         .getLogger(AmuseWindow.class);

    private Model                 legendModel;

    private MultiColorText        legendTextmin, legendTextmax;

    private Program               animatedTurbulenceShader, pplShader,
                                  axesShader, gasShader, postprocessShader, gaussianBlurShader,
                                  textShader, legendProgram;

    private FBO                   starHaloFBO, gasFBO, starFBO, axesFBO,
                                  hudFBO, legendTextureFBO;

    private Quad                  FSQ_postprocess, FSQ_blur;
    private Model                 xAxis, yAxis, zAxis;

    private final int             fontSize       = 30;

    private MultiColorText        frameNumberText;
    private Perlin3D              noiseTex;

    private float                 offset         = 0;

    private final boolean         snapshotting   = false;

    private final AmuseSettings   settings       = AmuseSettings.getInstance();

    private GlueSceneDescription  requestedScene = null;

    private SceneStorage          sceneStore;

    private TimedPlayer           timer;

    private IntPBO                finalPBO;

    private VisualScene           oldScene;

    private final InputHandler    inputHandler;

    protected final ProgramLoader loader;
    protected int                 canvasWidth, canvasHeight;

    protected int                 fontSet        = FontFactory.UBUNTU;
    protected TypecastFont        font;
    protected final float         radius         = 1.0f;
    protected final float         ftheta         = 0.0f;
    protected final float         phi            = 0.0f;

    protected final float         fovy           = 45.0f;
    private float                 aspect;
    protected final float         zNear          = 0.1f;
    protected final float         zFar           = 3000.0f;

    public AmuseWindow(InputHandler inputHandler) {
        this.loader = new ProgramLoader();
        this.inputHandler = inputHandler;
        this.font = (TypecastFont) FontFactory.get(fontSet).getDefault();
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        TimedPlayer timer = AmusePanel.getTimer();

        if (timer.isInitialized()) {
            this.timer = timer;
            try {
                final int status = drawable.getContext().makeCurrent();
                if ((status != GLContext.CONTEXT_CURRENT)
                        && (status != GLContext.CONTEXT_CURRENT_NEW)) {
                    System.err.println("Error swapping context to onscreen.");
                }
            } catch (final GLException e) {
                System.err
                        .println("Exception while swapping context to onscreen.");
                e.printStackTrace();
            }

            final GL3 gl = drawable.getContext().getGL().getGL3();
            gl.glViewport(0, 0, canvasWidth, canvasHeight);

            GlueSceneDescription currentDescription = settings
                    .getCurrentDescription();

            sceneStore = timer.getSceneStorage();
            sceneStore.init(gl);

            if (currentDescription != requestedScene) {
                sceneStore.requestNewConfiguration(currentDescription);
                requestedScene = currentDescription;
            }

            VisualScene newScene = sceneStore.getScene();
            if (newScene != null) {
                displayContext(newScene);

                if (oldScene != null && oldScene != newScene) {
                    oldScene.dispose(gl);

                    oldScene = newScene;
                }
            } else {
                logger.debug("Scene is null");
            }

            if (timer.isScreenshotNeeded()) {
                try {
                    finalPBO.copyToPBO(gl);
                    ByteBuffer bb = finalPBO.getBuffer();
                    bb.rewind();

                    int pixels = canvasWidth * canvasHeight;
                    int[] array = new int[pixels];
                    IntBuffer ib = IntBuffer.wrap(array);

                    for (int i = 0; i < (pixels * 4); i += 4) {
                        int b = bb.get(i) & 0xFF;
                        int g = bb.get(i + 1) & 0xFF;
                        int r = bb.get(i + 2) & 0xFF;
                        // int a = bb.get(i + 3) & 0xFF;

                        int argb = (r << 16) | (g << 8) | b;
                        ib.put(argb);
                    }
                    ib.rewind();

                    int[] destArray = new int[pixels];
                    IntBuffer dest = IntBuffer.wrap(destArray);

                    int[] rowPix = new int[canvasWidth];
                    for (int row = 0; row < canvasHeight; row++) {
                        ib.get(rowPix);
                        dest.position((canvasHeight - row - 1) * canvasWidth);
                        dest.put(rowPix);
                    }

                    BufferedImage bufIm = new BufferedImage(canvasWidth,
                            canvasHeight, BufferedImage.TYPE_INT_RGB);
                    bufIm.setRGB(0, 0, canvasWidth, canvasHeight, dest.array(),
                            0, canvasWidth);
                    try {

                        ImageIO.write(bufIm, "png",
                                new File(timer.getScreenshotFileName()));
                    } catch (IOException e2) {
                        // TODO Auto-generated catch block
                        e2.printStackTrace();
                    }

                    finalPBO.unBind(gl);
                } catch (UninitializedException e) {
                    e.printStackTrace();
                }

                timer.setScreenshotNeeded(false);
            }

            try {
                drawable.getContext().release();
            } catch (final GLException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void displayContext(VisualScene newScene) {
        final GL3 gl = GLContext.getCurrentGL().getGL3();

        final int width = GLContext.getCurrent().getGLDrawable().getWidth();
        final int height = GLContext.getCurrent().getGLDrawable().getHeight();
        this.aspect = (float) width / (float) height;

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        final Point4 eye = new Point4(
                (float) (radius * Math.sin(ftheta) * Math.cos(phi)),
                (float) (radius * Math.sin(ftheta) * Math.sin(phi)),
                (float) (radius * Math.cos(ftheta)), 1.0f);
        final Point4 at = new Point4(0.0f, 0.0f, 0.0f, 1.0f);
        final VecF4 up = new VecF4(0.0f, 1.0f, 0.0f, 0.0f);

        if (settings.getStereo()) {
            MatF4 mv = MatrixFMath.lookAt(eye, at, up);
            mv = mv.mul(MatrixFMath.translate(new VecF3(0f, 0f, inputHandler
                    .getViewDist())));
            MatF4 mv2 = mv.clone();

            if (!settings.getStereoSwitched()) {
                gl.glDrawBuffer(GL2GL3.GL_BACK_LEFT);
            } else {
                gl.glDrawBuffer(GL2GL3.GL_BACK_RIGHT);
            }
            mv = mv.mul(MatrixFMath.translate(new VecF3(-.5f
                    * settings.getStereoOcularDistance(), 0f, 0f)));
            mv = mv.mul(MatrixFMath
                    .rotationX(inputHandler.getRotation().get(0)));
            mv = mv.mul(MatrixFMath
                    .rotationY(inputHandler.getRotation().get(1)));

            renderScene(gl, mv.clone(), newScene);

            try {
                renderHUDText(gl, newScene, mv.clone());
            } catch (final UninitializedException e) {
                e.printStackTrace();
            }
            renderTexturesToScreen(gl);

            if (!settings.getStereoSwitched()) {
                gl.glDrawBuffer(GL2GL3.GL_BACK_RIGHT);
            } else {
                gl.glDrawBuffer(GL2GL3.GL_BACK_LEFT);
            }
            mv2 = mv2.mul(MatrixFMath.translate(new VecF3(.5f * settings
                    .getStereoOcularDistance(), 0f, 0f)));
            mv2 = mv2.mul(MatrixFMath.rotationX(inputHandler.getRotation().get(
                    0)));
            mv2 = mv2.mul(MatrixFMath.rotationY(inputHandler.getRotation().get(
                    1)));

            renderScene(gl, mv2.clone(), newScene);

            try {
                renderHUDText(gl, newScene, mv2.clone());
            } catch (final UninitializedException e) {
                e.printStackTrace();
            }
            renderTexturesToScreen(gl);
        } else {
            MatF4 mv = MatrixFMath.lookAt(eye, at, up);
            mv = mv.mul(MatrixFMath.translate(new VecF3(0f, 0f, inputHandler
                    .getViewDist())));
            mv = mv.mul(MatrixFMath
                    .rotationX(inputHandler.getRotation().get(0)));
            mv = mv.mul(MatrixFMath
                    .rotationY(inputHandler.getRotation().get(1)));

            renderScene(gl, mv.clone(), newScene);

            try {
                renderHUDText(gl, newScene, mv.clone());
            } catch (final UninitializedException e) {
                e.printStackTrace();
            }
            renderTexturesToScreen(gl);
        }
    }

    private void renderScene(GL3 gl, MatF4 mv, VisualScene newScene) {
        if (settings.getGasInvertedBackgroundColor()) {
            gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        } else {
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        }
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        try {
            renderGas(gl, mv.clone(), newScene);
            renderStars(gl, mv.clone(), newScene);
            renderStarHalos(gl, mv.clone(), newScene);
            renderAxes(gl, mv.clone());
        } catch (final UninitializedException e) {
            e.printStackTrace();
        }
    }

    private void renderGas(GL3 gl, MatF4 mv, VisualScene newScene)
            throws UninitializedException {
        gasFBO.bind(gl);
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);

        gl.glDisable(GL.GL_DEPTH_TEST);

        final MatF4 p = MatrixFMath.perspective(fovy, aspect, zNear, zFar);
        gasShader.setUniformMatrix("PMatrix", p);

        newScene.drawGasPointCloud(gl, gasShader, mv);

        gl.glEnable(GL.GL_DEPTH_TEST);

        gasFBO.unBind(gl);

        if (snapshotting) {
            blur(gl, gasFBO, FSQ_blur, settings.getBlurPassSetting(),
                    settings.getBlurTypeSetting(),
                    settings.getBlurSizeSetting());
        } else {
            blur(gl, gasFBO, FSQ_blur, settings.getBlurPassSetting(),
                    settings.getBlurTypeSetting(),
                    settings.getBlurSizeSetting());
        }
    }

    private void renderStars(GL3 gl, MatF4 mv, VisualScene newScene)
            throws UninitializedException {
        starFBO.bind(gl);
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);

        noiseTex.use(gl);
        animatedTurbulenceShader.setUniform("Noise",
                noiseTex.getMultitexNumber());

        final MatF4 p = MatrixFMath.perspective(fovy, aspect, zNear, zFar);
        animatedTurbulenceShader.setUniformMatrix("PMatrix", p);
        animatedTurbulenceShader.setUniformMatrix("SMatrix",
                MatrixFMath.scale(1));
        animatedTurbulenceShader.setUniformMatrix("MVMatrix", mv);
        animatedTurbulenceShader.setUniform("Offset", offset);

        offset += .001f;

        animatedTurbulenceShader.setUniform("StarDrawMode", 0);

        newScene.drawStars(gl, animatedTurbulenceShader, mv);

        starFBO.unBind(gl);
    }

    private void renderStarHalos(GL3 gl, MatF4 mv, VisualScene newScene)
            throws UninitializedException {
        starHaloFBO.bind(gl);
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);

        final MatF4 p = MatrixFMath.perspective(fovy, aspect, zNear, zFar);
        pplShader.setUniformMatrix("PMatrix", p);
        pplShader.setUniformMatrix("SMatrix", MatrixFMath.scale(2f));

        newScene.drawStars(gl, pplShader, mv);

        starHaloFBO = blur(gl, starHaloFBO, FSQ_blur,
                settings.getStarHaloBlurPasses(),
                settings.getStarHaloBlurType(), settings.getStarHaloBlurSize());

        starHaloFBO.unBind(gl);
    }

    private void renderAxes(GL3 gl, MatF4 mv) throws UninitializedException {
        axesFBO.bind(gl);
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);

        final MatF4 p = MatrixFMath.perspective(fovy, aspect, zNear, zFar);
        axesShader.setUniformMatrix("PMatrix", p);
        axesShader.setUniformMatrix("MVMatrix", mv);

        axesShader.setUniformVector("Color", new VecF4(1f, 0f, 0f, 1f));
        axesShader.use(gl);
        xAxis.draw(gl, axesShader);

        axesShader.setUniformVector("Color", new VecF4(0f, 1f, 0f, 1f));
        axesShader.use(gl);
        yAxis.draw(gl, axesShader);

        axesShader.setUniformVector("Color", new VecF4(0f, 0f, 1f, 1f));
        axesShader.use(gl);
        zAxis.draw(gl, axesShader);

        axesFBO.unBind(gl);
    }

    private void renderHUDText(GL3 gl, VisualScene newScene, MatF4 mv)
            throws UninitializedException {

        final String text = "Frame: " + AmusePanel.getTimer().getFrameNumber();
        frameNumberText.setString(gl, text, Color4.white, fontSize);

        SceneDescription currentDesc = newScene.getDescription();

        String min = Float.toString(currentDesc.getLowerBound());
        String max = Float.toString(currentDesc.getUpperBound());
        legendTextmin.setString(gl, min, Color4.white, fontSize);
        legendTextmax.setString(gl, max, Color4.white, fontSize);

        hudFBO.bind(gl);
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);

        // Draw Legend
        ByteBufferTexture legendTexture = new ByteBufferTexture(
                GL3.GL_TEXTURE9, timer.getSceneStorage().getLegendImage(), 1,
                500);
        legendTexture.init(gl);

        legendProgram.setUniform("texture_map",
                legendTexture.getMultitexNumber());
        legendProgram.setUniformMatrix("PMatrix", new MatF4());
        legendProgram.setUniformMatrix("MVMatrix", new MatF4());

        legendProgram.use(gl);
        legendModel.draw(gl, legendProgram);

        legendTexture.delete(gl);

        // Draw legend text
        int textLength = legendTextmin.toString().length() * fontSize;
        legendTextmin.draw(gl, textShader, canvasWidth, canvasHeight, 2
                * canvasWidth - textLength - 100, .2f * canvasHeight);

        textLength = legendTextmax.toString().length() * fontSize;
        legendTextmax.draw(gl, textShader, canvasWidth, canvasHeight, 2
                * canvasWidth - textLength - 100, 1.75f * canvasHeight);

        frameNumberText.draw(gl, textShader, canvasWidth, canvasHeight, 30f,
                30f);

        hudFBO.unBind(gl);
    }

    private void renderTexturesToScreen(GL3 gl) {
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        postprocessShader.setUniform("axesTexture", axesFBO.getTexture()
                .getMultitexNumber());
        postprocessShader.setUniform("gasTexture", gasFBO.getTexture()
                .getMultitexNumber());
        postprocessShader.setUniform("starTexture", starFBO.getTexture()
                .getMultitexNumber());
        postprocessShader.setUniform("starHaloTexture", starHaloFBO
                .getTexture().getMultitexNumber());
        postprocessShader.setUniform("hudTexture", hudFBO.getTexture()
                .getMultitexNumber());

        postprocessShader.setUniform("starBrightness",
                settings.getPostprocessingStarBrightness());
        postprocessShader.setUniform("starHaloBrightness",
                settings.getPostprocessingStarHaloBrightness());
        postprocessShader.setUniform("gasBrightness",
                settings.getPostprocessingGasBrightness());
        postprocessShader.setUniform("axesBrightness",
                settings.getPostprocessingAxesBrightness());
        postprocessShader.setUniform("hudBrightness",
                settings.getPostprocessingHudBrightness());
        postprocessShader.setUniform("overallBrightness",
                settings.getPostprocessingOverallBrightness());

        postprocessShader.setUniformMatrix("MVMatrix", new MatF4());
        postprocessShader.setUniformMatrix("PMatrix", new MatF4());

        postprocessShader.setUniform("scrWidth", canvasWidth);
        postprocessShader.setUniform("scrHeight", canvasHeight);

        try {
            postprocessShader.use(gl);

            FSQ_blur.draw(gl, postprocessShader);
        } catch (final UninitializedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
        final GL3 gl = drawable.getGL().getGL3();

        canvasWidth = w;
        canvasHeight = h;

        starFBO.delete(gl);
        starHaloFBO.delete(gl);
        gasFBO.delete(gl);
        axesFBO.delete(gl);
        hudFBO.delete(gl);
        legendTextureFBO.delete(gl);

        starFBO = new FBO(canvasWidth, canvasHeight, GL.GL_TEXTURE1);
        starHaloFBO = new FBO(canvasWidth, canvasHeight, GL.GL_TEXTURE2);
        gasFBO = new FBO(canvasWidth, canvasHeight, GL.GL_TEXTURE3);
        axesFBO = new FBO(canvasWidth, canvasHeight, GL.GL_TEXTURE4);
        hudFBO = new FBO(canvasWidth, canvasHeight, GL.GL_TEXTURE5);
        legendTextureFBO = new FBO(canvasWidth, canvasHeight, GL.GL_TEXTURE6);

        finalPBO.delete(gl);
        finalPBO = new IntPBO(canvasWidth, canvasHeight);
        finalPBO.init(gl);

        starFBO.init(gl);
        starHaloFBO.init(gl);
        gasFBO.init(gl);
        axesFBO.init(gl);
        hudFBO.init(gl);
        legendTextureFBO.init(gl);
    }

    private FBO blur(GL3 gl, FBO target, Quad fullScreenQuad, int passes,
            int blurType, float blurSize) {
        gaussianBlurShader.setUniform("Texture", target.getTexture()
                .getMultitexNumber());

        gaussianBlurShader.setUniformMatrix("PMatrix", new MatF4());
        gaussianBlurShader.setUniformMatrix("MVMatrix", new MatF4());

        gaussianBlurShader.setUniform("blurType", blurType);
        gaussianBlurShader.setUniform("blurSize", blurSize);
        gaussianBlurShader.setUniform("scrWidth", target.getTexture()
                .getWidth());

        gaussianBlurShader.setUniform("scrHeight", target.getTexture()
                .getHeight());
        gaussianBlurShader.setUniform("Alpha", 1f);

        gaussianBlurShader.setUniform("blurDirection", 0);

        gaussianBlurShader.setUniform("NumPixelsPerSide", 2f);
        gaussianBlurShader.setUniform("Sigma", 2f);

        try {
            target.bind(gl);
            for (int i = 0; i < passes; i++) {
                gaussianBlurShader.setUniform("blurDirection", 0);
                gaussianBlurShader.use(gl);
                fullScreenQuad.draw(gl, gaussianBlurShader);

                gaussianBlurShader.setUniform("blurDirection", 1);
                gaussianBlurShader.use(gl);
                fullScreenQuad.draw(gl, gaussianBlurShader);
            }

            target.unBind(gl);
        } catch (final UninitializedException e) {
            e.printStackTrace();
        }

        return target;
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        final GL3 gl = drawable.getGL().getGL3();

        noiseTex.delete(gl);

        starFBO.delete(gl);
        starHaloFBO.delete(gl);
        gasFBO.delete(gl);
        hudFBO.delete(gl);
        axesFBO.delete(gl);
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        try {
            final int status = drawable.getContext().makeCurrent();
            if ((status != GLContext.CONTEXT_CURRENT)
                    && (status != GLContext.CONTEXT_CURRENT_NEW)) {
                System.err.println("Error swapping context to onscreen.");
            }
        } catch (final GLException e) {
            System.err.println("Exception while swapping context to onscreen.");
            e.printStackTrace();
        }

        canvasWidth = drawable.getWidth();
        canvasHeight = drawable.getHeight();

        final GL3 gl = drawable.getGL().getGL3();

        // Anti-Aliasing
        gl.glEnable(GL3.GL_LINE_SMOOTH);
        gl.glHint(GL3.GL_LINE_SMOOTH_HINT, GL3.GL_NICEST);
        gl.glEnable(GL3.GL_POLYGON_SMOOTH);
        gl.glHint(GL3.GL_POLYGON_SMOOTH_HINT, GL3.GL_NICEST);

        // Depth testing
        gl.glEnable(GL3.GL_DEPTH_TEST);
        gl.glDepthFunc(GL3.GL_LEQUAL);
        gl.glClearDepth(1.0f);

        // Culling
        gl.glEnable(GL3.GL_CULL_FACE);
        gl.glCullFace(GL3.GL_BACK);

        // Enable Blending (needed for both Transparency and
        // Anti-Aliasing
        gl.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL3.GL_BLEND);

        // Enable Vertical Sync
        gl.setSwapInterval(1);

        // Set black background
        gl.glClearColor(0f, 0f, 0f, 0f);

        // Load and compile shaders, then use program.
        try {
            animatedTurbulenceShader = loader.createProgram(gl,
                    "animatedTurbulence", new File("shaders/vs_sunsurface.vp"),
                    new File("shaders/fs_animatedTurbulence.fp"));
            pplShader = loader.createProgram(gl, "ppl", new File(
                    "shaders/vs_ppl.vp"), new File("shaders/fs_ppl.fp"));
            axesShader = loader.createProgram(gl, "axes", new File(
                    "shaders/vs_axes.vp"), new File("shaders/fs_axes.fp"));
            gasShader = loader.createProgram(gl, "gas", new File(
                    "shaders/vs_gas.vp"), new File("shaders/fs_gas.fp"));
            textShader = loader.createProgram(gl, "text", new File(
                    "shaders/vs_multiColorTextShader.vp"), new File(
                    "shaders/fs_multiColorTextShader.fp"));
            legendProgram = loader
                    .createProgram(gl, "legend", new File(
                            "shaders/vs_texture.vp"), new File(
                            "shaders/fs_texture.fp"));

            postprocessShader = loader.createProgram(gl, "postprocess",
                    new File("shaders/vs_postprocess.vp"), new File(
                            "shaders/fs_postprocess.fp"));
            gaussianBlurShader = loader.createProgram(gl, "gaussianBlur",
                    new File("shaders/vs_postprocess.vp"), new File(
                            "shaders/fs_gaussian_blur.fp"));
        } catch (final Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // AXES
        final Color4 axisColor = new Color4(0f, 1f, 0f, 1f);
        final Material axisMaterial = new Material(axisColor, axisColor,
                axisColor);
        xAxis = new Axis(axisMaterial, new VecF3(-1f, 0f, 0f), new VecF3(1f, 0f, 0f), 1f, .02f);
        xAxis.init(gl);
        yAxis = new Axis(axisMaterial, new VecF3(0f, -1f, 0f), new VecF3(0f, 1f, 0f), 1f, .02f);
        yAxis.init(gl);
        zAxis = new Axis(axisMaterial, new VecF3(0f, 0f, -1f), new VecF3(0f, 0f, 1f), 1f, .02f);
        zAxis.init(gl);

        // TEXT
        Material textMaterial = new Material(Color4.white, Color4.white,
                Color4.white);
        frameNumberText = new MultiColorText(textMaterial, font);
        legendTextmin = new MultiColorText(textMaterial, font);
        legendTextmax = new MultiColorText(textMaterial, font);

        frameNumberText.init(gl);
        legendTextmin.init(gl);
        legendTextmax.init(gl);

        // FULL SCREEN QUADS
        FSQ_postprocess = new Quad(Material.random(), 2, 2, new VecF3(0, 0,
                0.1f));
        FSQ_postprocess.init(gl);

        FSQ_blur = new Quad(Material.random(), 2, 2, new VecF3(0, 0, 0.1f));
        FSQ_blur.init(gl);

        // TEXTURES
        noiseTex = new Perlin3D(GL.GL_TEXTURE0, 128, 128, 128);
        noiseTex.init(gl);

        // Full screen textures (for post processing) done with FBO's
        starFBO = new FBO(canvasWidth, canvasHeight, GL.GL_TEXTURE1);
        starHaloFBO = new FBO(canvasWidth, canvasHeight, GL.GL_TEXTURE2);
        gasFBO = new FBO(canvasWidth, canvasHeight, GL.GL_TEXTURE3);
        axesFBO = new FBO(canvasWidth, canvasHeight, GL.GL_TEXTURE4);
        hudFBO = new FBO(canvasWidth, canvasHeight, GL.GL_TEXTURE5);
        legendTextureFBO = new FBO(canvasWidth, canvasHeight, GL.GL_TEXTURE6);

        starFBO.init(gl);
        starHaloFBO.init(gl);
        gasFBO.init(gl);
        axesFBO.init(gl);
        hudFBO.init(gl);
        legendTextureFBO.init(gl);

        legendModel = new Quad(Material.random(), 1.5f, .1f, new VecF3(1, 0,
                0.1f));
        legendModel.init(gl);

        finalPBO = new IntPBO(canvasWidth, canvasHeight);
        finalPBO.init(gl);
    }

    public InputHandler getInputHandler() {
        return inputHandler;
    }

    public TimedPlayer getTimer() {
        return timer;
    }
}
