package amuse.visualization;

import java.io.File;
import java.util.ArrayList;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;

import openglCommon.CommonWindow;
import openglCommon.datastructures.FBO;
import openglCommon.datastructures.Material;
import openglCommon.datastructures.Picture;
import openglCommon.exceptions.UninitializedException;
import openglCommon.math.Color4;
import openglCommon.math.MatF3;
import openglCommon.math.MatF4;
import openglCommon.math.MatrixFMath;
import openglCommon.math.Point4;
import openglCommon.math.VecF3;
import openglCommon.math.VecF4;
import openglCommon.models.Axis;
import openglCommon.models.Model;
import openglCommon.models.MultiColorText;
import openglCommon.models.Text;
import openglCommon.models.base.Quad;
import openglCommon.shaders.Program;
import openglCommon.textures.Perlin3D;
import openglCommon.util.InputHandler;
import amuse.visualization.amuseAdaptor.AmuseGasOctreeNode;
import amuse.visualization.amuseAdaptor.Astrophysics;
import amuse.visualization.amuseAdaptor.Hdf5TimedPlayer;
import amuse.visualization.amuseAdaptor.Star2;

public class AmuseWindow extends CommonWindow {
    private ArrayList<Star2>    stars;

    private AmuseGasOctreeNode  octreeRoot;
    private Program             animatedTurbulenceShader, pplShader, axesShader, gasShader, postprocessShader,
            gaussianBlurShader, textShader;
    private FBO                 starHaloFBO, starHaloFBO4k;
    private FBO                 gasFBO, gasFBO4k;
    private FBO                 starFBO, starFBO4k;

    private FBO                 axesFBO, axesFBO4k;
    private FBO                 hudFBO, hudFBO4k;

    private Quad                FSQ_postprocess, FSQ_blur;
    private Model               xAxis, yAxis, zAxis;

    private final int           fontSize     = 30;

    private MultiColorText      myText;
    private Perlin3D            noiseTex;

    private final VecF3         lightPos     = new VecF3(2f, 2f, 2f);

    private final float         shininess    = 50f;

    private float               offset       = 0;

    private boolean             snapshotting = false;

    private final AmuseSettings settings     = AmuseSettings.getInstance();

    public AmuseWindow(InputHandler inputHandler, boolean post_process) {
        super(inputHandler, post_process);
    }

    private void blur(GL3 gl, FBO target, Quad fullScreenQuad, int passes, int blurType, float blurSize) {
        gaussianBlurShader.setUniform("Texture", target.getTexture().getMultitexNumber());

        gaussianBlurShader.setUniformMatrix("PMatrix", new MatF4());
        gaussianBlurShader.setUniformMatrix("MVMatrix", new MatF4());

        gaussianBlurShader.setUniform("blurType", blurType);
        gaussianBlurShader.setUniform("blurSize", blurSize);
        gaussianBlurShader.setUniform("scrWidth", target.getTexture().getWidth());
        gaussianBlurShader.setUniform("scrHeight", target.getTexture().getHeight());
        gaussianBlurShader.setUniform("Alpha", 1f);

        gaussianBlurShader.setUniform("blurDirection", 0);

        try {
            // gaussianBlurShader.use(gl);

            for (int i = 0; i < passes; i++) {
                target.bind(gl);

                gaussianBlurShader.setUniform("blurDirection", 0);
                fullScreenQuad.draw(gl, gaussianBlurShader, new MatF4());

                gaussianBlurShader.setUniform("blurDirection", 1);
                fullScreenQuad.draw(gl, gaussianBlurShader, new MatF4());
                target.unBind(gl);
            }
        } catch (final UninitializedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        final Hdf5TimedPlayer timer = AmusePanel.getTimer();
        if (timer.isInitialized()) {
            try {
                final int status = drawable.getContext().makeCurrent();
                if ((status != GLContext.CONTEXT_CURRENT) && (status != GLContext.CONTEXT_CURRENT_NEW)) {
                    System.err.println("Error swapping context to onscreen.");
                }
            } catch (final GLException e) {
                System.err.println("Exception while swapping context to onscreen.");
                e.printStackTrace();
            }

            final GL3 gl = drawable.getContext().getGL().getGL3();
            gl.glViewport(0, 0, canvasWidth, canvasHeight);

            stars = timer.getStars();
            octreeRoot = timer.getOctreeRoot();

            displayContext(stars, octreeRoot, starFBO, starHaloFBO, gasFBO, hudFBO, axesFBO);

            try {
                drawable.getContext().release();
            } catch (final GLException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void displayContext(ArrayList<Star2> stars2, AmuseGasOctreeNode octreeRoot, FBO starFBO,
            FBO starHaloFBO, FBO gasFBO, FBO hudFBO, FBO axesFBO) {
        final GL3 gl = GLContext.getCurrentGL().getGL3();

        stars2.get(0).init(gl);
        octreeRoot.init(gl);

        final int width = GLContext.getCurrent().getGLDrawable().getWidth();
        final int height = GLContext.getCurrent().getGLDrawable().getHeight();
        final float aspect = (float) width / (float) height;

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        final Point4 eye = new Point4((float) (radius * Math.sin(ftheta) * Math.cos(phi)), (float) (radius
                * Math.sin(ftheta) * Math.sin(phi)), (float) (radius * Math.cos(ftheta)), 1.0f);
        final Point4 at = new Point4(0.0f, 0.0f, 0.0f, 1.0f);
        final VecF4 up = new VecF4(0.0f, 1.0f, 0.0f, 0.0f);

        if (settings.getStereo()) {
            MatF4 mv = MatrixFMath.lookAt(eye, at, up);
            mv = mv.mul(MatrixFMath.translate(new VecF3(0f, 0f, inputHandler.getViewDist())));
            MatF4 mv2 = mv.clone();

            final MatF3 n = new MatF3();
            final MatF4 p = MatrixFMath.perspective(fovy, aspect, zNear, zFar);

            // Vertex shader variables
            loader.setUniformMatrix("NormalMatrix", n);
            loader.setUniformMatrix("PMatrix", p);
            loader.setUniformMatrix("SMatrix", MatrixFMath.scale(1));

            if (!settings.getStereoSwitched()) {
                gl.glDrawBuffer(GL2GL3.GL_BACK_LEFT);
            } else {
                gl.glDrawBuffer(GL2GL3.GL_BACK_RIGHT);
            }
            mv = mv.mul(MatrixFMath.translate(new VecF3(-.5f * settings.getStereoOcularDistance(), 0f, 0f)));
            mv = mv.mul(MatrixFMath.rotationX(inputHandler.getRotation().get(0)));
            mv = mv.mul(MatrixFMath.rotationY(inputHandler.getRotation().get(1)));

            renderScene(gl, mv, stars2, octreeRoot, starHaloFBO, starFBO, gasFBO, axesFBO);

            try {
                renderHUDText(gl, mv, hudFBO);
            } catch (final UninitializedException e) {
                e.printStackTrace();
            }

            if (post_process) {
                renderTexturesToScreen(gl, width, height, starHaloFBO, starFBO, gasFBO, hudFBO, axesFBO);
            }

            if (!settings.getStereoSwitched()) {
                gl.glDrawBuffer(GL2GL3.GL_BACK_RIGHT);
            } else {
                gl.glDrawBuffer(GL2GL3.GL_BACK_LEFT);
            }
            mv2 = mv2.mul(MatrixFMath.translate(new VecF3(.5f * settings.getStereoOcularDistance(), 0f, 0f)));
            mv2 = mv2.mul(MatrixFMath.rotationX(inputHandler.getRotation().get(0)));
            mv2 = mv2.mul(MatrixFMath.rotationY(inputHandler.getRotation().get(1)));

            renderScene(gl, mv2, stars2, octreeRoot, starHaloFBO, starFBO, gasFBO, axesFBO);

            try {
                renderHUDText(gl, mv2, hudFBO);
            } catch (final UninitializedException e) {
                e.printStackTrace();
            }

            if (post_process) {
                renderTexturesToScreen(gl, width, height, starHaloFBO, starFBO, gasFBO, hudFBO, axesFBO);
            }
        } else {
            MatF4 mv = MatrixFMath.lookAt(eye, at, up);
            mv = mv.mul(MatrixFMath.translate(new VecF3(0f, 0f, inputHandler.getViewDist())));
            mv = mv.mul(MatrixFMath.rotationX(inputHandler.getRotation().get(0)));
            mv = mv.mul(MatrixFMath.rotationY(inputHandler.getRotation().get(1)));

            final MatF3 n = new MatF3();
            final MatF4 p = MatrixFMath.perspective(fovy, aspect, zNear, zFar);

            // Vertex shader variables
            loader.setUniformMatrix("NormalMatrix", n);
            loader.setUniformMatrix("PMatrix", p);
            loader.setUniformMatrix("SMatrix", MatrixFMath.scale(1));

            renderScene(gl, mv, stars2, octreeRoot, starHaloFBO, starFBO, gasFBO, axesFBO);

            try {
                renderHUDText(gl, mv, hudFBO);
            } catch (final UninitializedException e) {
                e.printStackTrace();
            }

            if (post_process) {
                renderTexturesToScreen(gl, width, height, starHaloFBO, starFBO, gasFBO, hudFBO, axesFBO);
            }
        }

    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        super.dispose(drawable);
        final GL3 gl = drawable.getGL().getGL3();

        noiseTex.delete(gl);

        starFBO.delete(gl);
        starHaloFBO.delete(gl);
        gasFBO.delete(gl);
        hudFBO.delete(gl);
        axesFBO.delete(gl);

        starFBO4k.delete(gl);
        starHaloFBO4k.delete(gl);
        gasFBO4k.delete(gl);
        hudFBO4k.delete(gl);
        axesFBO4k.delete(gl);
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        super.init(drawable);

        drawable.getContext().makeCurrent();
        final GL3 gl = drawable.getGL().getGL3();

        // Load and compile shaders, then use program.
        try {
            animatedTurbulenceShader = loader.createProgram(gl, "animatedTurbulence", new File(
                    "shaders/vs_sunsurface.vp"), new File("shaders/fs_animatedTurbulence.fp"));
            pplShader = loader.createProgram(gl, "ppl", new File("shaders/vs_ppl.vp"), new File("shaders/fs_ppl.fp"));
            axesShader = loader.createProgram(gl, "axes", new File("shaders/vs_axes.vp"),
                    new File("shaders/fs_axes.fp"));
            gasShader = loader.createProgram(gl, "gas", new File("shaders/vs_gas.vp"), new File("shaders/fs_gas.fp"));
            textShader = loader.createProgram(gl, "text", new File("shaders/vs_multiColorTextShader.vp"), new File(
                    "shaders/fs_multiColorTextShader.fp"));

            if (post_process) {
                postprocessShader = loader.createProgram(gl, "postprocess", new File("shaders/vs_postprocess.vp"),
                        new File("shaders/fs_postprocess.fp"));
            }
            if (post_process) {
                gaussianBlurShader = loader.createProgram(gl, "gaussianBlur", new File("shaders/vs_postprocess.vp"),
                        new File("shaders/fs_gaussian_blur.fp"));
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
        myText = new MultiColorText(axisMaterial);
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

        final int ssWidth = settings.getScreenshotScreenWidth();
        final int ssHeight = settings.getScreenshotScreenHeight();

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
    public void makeSnapshot(String fileName) {
        snapshotting = true;

        try {
            final int status = offScreenContext.makeCurrent();
            if ((status != GLContext.CONTEXT_CURRENT) && (status != GLContext.CONTEXT_CURRENT_NEW)) {
                System.err.println("Error swapping context to offscreen.");
            }
        } catch (final GLException e) {
            System.err.println("Exception while swapping context to offscreen.");
            e.printStackTrace();
        }

        final int width = offScreenContext.getGLDrawable().getWidth();
        final int height = offScreenContext.getGLDrawable().getHeight();

        final GL3 gl = offScreenContext.getGL().getGL3();
        gl.glViewport(0, 0, width, height);

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

        gl.glClearColor(0f, 0f, 0f, 0f);

        starFBO4k.init(gl);
        starHaloFBO4k.init(gl);
        gasFBO4k.init(gl);
        hudFBO4k.init(gl);
        axesFBO4k.init(gl);

        displayContext(stars, octreeRoot, starFBO4k, starHaloFBO4k, gasFBO4k, hudFBO4k, axesFBO);

        final Picture p = new Picture(width, height);

        gl.glFinish();

        p.copyFrameBufferToFile(settings.getScreenshotPath(), fileName);

        try {
            offScreenContext.release();
        } catch (final GLException e) {
            e.printStackTrace();
        }
        snapshotting = false;
    }

    private void renderAxes(GL3 gl, MatF4 mv, FBO axesFBO) throws UninitializedException {
        axesFBO.bind(gl);
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);

        // axesShader.use(gl);
        xAxis.draw(gl, axesShader, mv);
        yAxis.draw(gl, axesShader, mv);
        zAxis.draw(gl, axesShader, mv);

        axesFBO.unBind(gl);
    }

    private void renderGas(GL3 gl, MatF4 mv, FBO gasFBO, AmuseGasOctreeNode octreeRoot) throws UninitializedException {
        gl.glDisable(GL.GL_DEPTH_TEST);

        gasFBO.bind(gl);
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);
        octreeRoot.draw(gl, gasShader, mv);
        gasFBO.unBind(gl);

        if (post_process) {
            if (snapshotting) {
                blur(gl, gasFBO, FSQ_blur, settings.getSnapshotGasBlurPasses(), settings.getSnapshotGasBlurType(),
                        settings.getSnapshotGasBlurSize());
            } else {
                blur(gl, gasFBO, FSQ_blur, settings.getGasBlurPasses(), settings.getGasBlurType(),
                        settings.getGasBlurSize());
            }
        }

        gl.glEnable(GL.GL_DEPTH_TEST);
    }

    private void renderHUDText(GL3 gl, MatF4 mv, FBO hudFBO) throws UninitializedException {
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        final String text = "Frame: " + AmusePanel.getTimer().getFrame();
        myText.setString(gl, textShader, font, text, Color4.green, fontSize);

        hudFBO.bind(gl);
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);

        myText.draw(gl, textShader, Text.getPMVForHUD(canvasWidth, canvasHeight, 30f, 30f));
        hudFBO.unBind(gl);
    }

    @Override
    public void renderScene(GL3 arg0, MatF4 arg1) {
        // TODO Auto-generated method stub

    }

    private void renderScene(GL3 gl, MatF4 mv, ArrayList<Star2> stars2, AmuseGasOctreeNode octreeRoot, FBO starHaloFBO,
            FBO starFBO, FBO gasFBO, FBO axesFBO) {
        if (settings.getGasInvertedBackgroundColor()) {
            gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        } else {
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        }
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        try {
            renderStarHalos(gl, mv, starHaloFBO, stars2);
            renderGas(gl, mv, gasFBO, octreeRoot);
            renderStars(gl, mv, starFBO, stars2);
            renderAxes(gl, mv, axesFBO);
        } catch (final UninitializedException e) {
            e.printStackTrace();
        }
    }

    private void renderStarHalos(GL3 gl, MatF4 mv, FBO starHaloFBO, ArrayList<Star2> stars2)
            throws UninitializedException {
        if (post_process) {
            starHaloFBO.bind(gl);
            gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);
            pplShader.setUniformVector("LightPos", lightPos);
            pplShader.setUniform("Shininess", shininess);

            pplShader.setUniformMatrix("SMatrix", MatrixFMath.scale(2));
            pplShader.setUniform("StarDrawMode", 1);

            for (final Star2 s : stars2) {
                s.draw(gl, pplShader, mv);
            }

            blur(gl, starHaloFBO, FSQ_blur, settings.getStarHaloBlurPasses(), settings.getStarHaloBlurType(),
                    settings.getStarHaloBlurSize());

            starHaloFBO.unBind(gl);
        }
    }

    private void renderStars(GL3 gl, MatF4 mv, FBO starsFBO, ArrayList<Star2> stars2) throws UninitializedException {
        starsFBO.bind(gl);
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);

        noiseTex.use(gl);
        animatedTurbulenceShader.setUniform("Noise", noiseTex.getMultitexNumber());

        animatedTurbulenceShader.setUniformMatrix("SMatrix", MatrixFMath.scale(1));
        animatedTurbulenceShader.setUniform("Offset", offset);

        offset += .001f;

        animatedTurbulenceShader.setUniform("StarDrawMode", 0);

        for (final Star2 s : stars2) {
            s.draw(gl, animatedTurbulenceShader, mv);
        }
        starsFBO.unBind(gl);
    }

    @Override
    public void renderTexturesToScreen(GL3 arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub

    }

    private void renderTexturesToScreen(GL3 gl, int width, int height, FBO starHaloFBO, FBO starFBO, FBO gasFBO,
            FBO hudFBO, FBO axesFBO) {
        postprocessShader.setUniform("axesTexture", axesFBO.getTexture().getMultitexNumber());
        postprocessShader.setUniform("gasTexture", gasFBO.getTexture().getMultitexNumber());
        postprocessShader.setUniform("starTexture", starFBO.getTexture().getMultitexNumber());
        postprocessShader.setUniform("starHaloTexture", starHaloFBO.getTexture().getMultitexNumber());
        postprocessShader.setUniform("hudTexture", hudFBO.getTexture().getMultitexNumber());

        postprocessShader.setUniform("starBrightness", settings.getPostprocessingStarBrightness());
        postprocessShader.setUniform("starHaloBrightness", settings.getPostprocessingStarHaloBrightness());
        postprocessShader.setUniform("gasBrightness", settings.getPostprocessingGasBrightness());
        postprocessShader.setUniform("axesBrightness", settings.getPostprocessingAxesBrightness());
        postprocessShader.setUniform("hudBrightness", settings.getPostprocessingHudBrightness());
        postprocessShader.setUniform("overallBrightness", settings.getPostprocessingOverallBrightness());

        postprocessShader.setUniformMatrix("MVMatrix", new MatF4());
        postprocessShader.setUniformMatrix("PMatrix", new MatF4());

        postprocessShader.setUniform("scrWidth", width);
        postprocessShader.setUniform("scrHeight", height);

        try {
            postprocessShader.use(gl);

            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
            FSQ_blur.draw(gl, postprocessShader, new MatF4());
        } catch (final UninitializedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
        super.reshape(drawable, x, y, w, h);
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
