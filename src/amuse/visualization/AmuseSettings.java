package amuse.visualization;

import openglCommon.util.Settings;
import openglCommon.util.TypedProperties;

public class AmuseSettings extends Settings {
    private static class SingletonHolder {
        public final static AmuseSettings instance = new AmuseSettings();
    }

    // Minimum and maximum values for the brightness sliders
    private static float   POSTPROCESSING_OVERALL_BRIGHTNESS_MIN   = 0f;
    private static float   POSTPROCESSING_OVERALL_BRIGHTNESS_MAX   = 10f;
    private static float   POSTPROCESSING_AXES_BRIGHTNESS_MIN      = 0f;
    private static float   POSTPROCESSING_AXES_BRIGHTNESS_MAX      = 4f;
    private static float   POSTPROCESSING_GAS_BRIGHTNESS_MIN       = 0f;
    private static float   POSTPROCESSING_GAS_BRIGHTNESS_MAX       = 4f;
    private static float   POSTPROCESSING_STAR_HALO_BRIGHTNESS_MIN = 0f;
    private static float   POSTPROCESSING_STAR_HALO_BRIGHTNESS_MAX = 4f;
    private static float   POSTPROCESSING_STAR_BRIGHTNESS_MIN      = 0f;
    private static float   POSTPROCESSING_STAR_BRIGHTNESS_MAX      = 4f;
    private static float   POSTPROCESSING_HUD_BRIGHTNESS_MIN       = 0f;

    private static float   POSTPROCESSING_HUD_BRIGHTNESS_MAX       = 4f;
    // Settings for the postprocessing shader
    private static float   POSTPROCESSING_OVERALL_BRIGHTNESS_DEF   = 4f;
    private static float   POSTPROCESSING_AXES_BRIGHTNESS_DEF      = 1f;
    private static float   POSTPROCESSING_GAS_BRIGHTNESS_DEF       = 3f;
    private static float   POSTPROCESSING_STAR_HALO_BRIGHTNESS_DEF = 3f;
    private static float   POSTPROCESSING_STAR_BRIGHTNESS_DEF      = 4f;

    private static float   POSTPROCESSING_HUD_BRIGHTNESS_DEF       = 1f;
    // Settings for the star-shape blur method (the + shape of stars)
    private static int     STAR_SHAPE_BLUR_SIZE                    = 1;
    private static float   STAR_SHAPE_BLURFILTER_SIZE              = 8f;
    private static float   STAR_SHAPE_SIGMA                        = 100f;
    private static float   STAR_SHAPE_ALPHA                        = 0.5f;

    private static int     STAR_SHAPE_BLUR_TYPE                    = 0;
    // Settings for the detail levels.
    private static int     LEVEL_OF_DETAIL                         = 0;
    private static int     LOW_GAS_BLUR_PASSES                     = 0;
    private static float   LOW_GAS_BLUR_SIZE                       = 2;

    private static int     LOW_GAS_BLUR_TYPE                       = 8;
    private static int     LOW_STAR_HALO_BLUR_PASSES               = 1;
    private static float   LOW_STAR_HALO_BLUR_SIZE                 = 1;

    private static int     LOW_STAR_HALO_BLUR_TYPE                 = 6;
    private static int     LOW_GAS_SUBDIVISION                     = 0;
    private static int     LOW_STAR_SUBDIVISION                    = 1;

    private static int     LOW_GAS_PARTICLES_PER_OCTREE_NODE       = 100;
    private static int     MEDIUM_GAS_BLUR_PASSES                  = 1;
    private static float   MEDIUM_GAS_BLUR_SIZE                    = 2;

    private static int     MEDIUM_GAS_BLUR_TYPE                    = 8;
    private static int     MEDIUM_STAR_HALO_BLUR_PASSES            = 1;
    private static float   MEDIUM_STAR_HALO_BLUR_SIZE              = 1;

    private static int     MEDIUM_STAR_HALO_BLUR_TYPE              = 6;
    private static int     MEDIUM_GAS_SUBDIVISION                  = 1;
    private static int     MEDIUM_STAR_SUBDIVISION                 = 2;

    private static int     MEDIUM_GAS_PARTICLES_PER_OCTREE_NODE    = 25;
    private static int     HIGH_GAS_BLUR_PASSES                    = 2;
    private static float   HIGH_GAS_BLUR_SIZE                      = 2;

    private static int     HIGH_GAS_BLUR_TYPE                      = 8;
    private static int     HIGH_STAR_HALO_BLUR_PASSES              = 2;
    private static float   HIGH_STAR_HALO_BLUR_SIZE                = 1;

    private static int     HIGH_STAR_HALO_BLUR_TYPE                = 6;
    private static int     HIGH_GAS_SUBDIVISION                    = 1;
    private static int     HIGH_STAR_SUBDIVISION                   = 3;

    private static int     HIGH_GAS_PARTICLES_PER_OCTREE_NODE      = 2;
    // Snaphots have different settings, since they are rendered at extremely
    // high resolutions pixels
    private static int     SNAPSHOT_GAS_BLUR_PASSES                = 2;      // 2
    private static float   SNAPSHOT_GAS_BLUR_SIZE                  = 2;      // 6

    private static int     SNAPSHOT_GAS_BLUR_TYPE                  = 8;      // 10
    private static int     SNAPSHOT_STAR_HALO_BLUR_PASSES          = 2;      // 2
    private static float   SNAPSHOT_STAR_HALO_BLUR_SIZE            = 1;      // 1

    private static int     SNAPSHOT_STAR_HALO_BLUR_TYPE            = 6;      // 6
    private static boolean GAS_COLOR_INVERTED                      = false;
    private static boolean GAS_COLOR_BACKGROUND_INVERTED           = false;
    private static boolean GAS_COLOR_FROM_STARS                    = false;

    private static boolean STAR_COLORS_EXAGGERATED                 = true;

    private static long    WAITTIME_FOR_RETRY                      = 10000;
    private static long    WAITTIME_FOR_MOVIE                      = 100;
    private static float   EPSILON                                 = 1.0E-7f;

    private static float   GAS_OPACITY_FACTOR_MIN                  = 0f;
    private static float   GAS_OPACITY_FACTOR_DEF                  = 0.05f;
    private static float   GAS_OPACITY_FACTOR_MAX                  = 0.5f;

    private static boolean BEZIER_INTERPOLATION                    = false;
    private static int     BEZIER_INTERPOLATION_STEPS              = 10;

    private static int     PREPROCESSING_AMOUNT                    = 5;

    public static AmuseSettings getInstance() {
        return SingletonHolder.instance;
    }

    private AmuseSettings() {
        super();

        try {
            final TypedProperties props = new TypedProperties();
            props.loadFromFile("settings.properties");

            // Minimum and maximum values for the brightness sliders
            AmuseSettings.POSTPROCESSING_OVERALL_BRIGHTNESS_MIN = props
                    .getFloatProperty("POSTPROCESSING_OVERALL_BRIGHTNESS_MIN");
            AmuseSettings.POSTPROCESSING_OVERALL_BRIGHTNESS_MAX = props
                    .getFloatProperty("POSTPROCESSING_OVERALL_BRIGHTNESS_MAX");
            AmuseSettings.POSTPROCESSING_AXES_BRIGHTNESS_MIN = props
                    .getFloatProperty("POSTPROCESSING_AXES_BRIGHTNESS_MIN");
            AmuseSettings.POSTPROCESSING_AXES_BRIGHTNESS_MAX = props
                    .getFloatProperty("POSTPROCESSING_AXES_BRIGHTNESS_MAX");
            AmuseSettings.POSTPROCESSING_GAS_BRIGHTNESS_MIN = props
                    .getFloatProperty("POSTPROCESSING_GAS_BRIGHTNESS_MIN");
            AmuseSettings.POSTPROCESSING_GAS_BRIGHTNESS_MAX = props
                    .getFloatProperty("POSTPROCESSING_GAS_BRIGHTNESS_MAX");
            AmuseSettings.POSTPROCESSING_STAR_HALO_BRIGHTNESS_MIN = props
                    .getFloatProperty("POSTPROCESSING_STAR_HALO_BRIGHTNESS_MIN");
            AmuseSettings.POSTPROCESSING_STAR_HALO_BRIGHTNESS_MAX = props
                    .getFloatProperty("POSTPROCESSING_STAR_HALO_BRIGHTNESS_MAX");
            AmuseSettings.POSTPROCESSING_STAR_BRIGHTNESS_MIN = props
                    .getFloatProperty("POSTPROCESSING_STAR_BRIGHTNESS_MIN");
            AmuseSettings.POSTPROCESSING_STAR_BRIGHTNESS_MAX = props
                    .getFloatProperty("POSTPROCESSING_STAR_BRIGHTNESS_MAX");

            // Settings for the postprocessing shader
            AmuseSettings.POSTPROCESSING_OVERALL_BRIGHTNESS_DEF = props
                    .getFloatProperty("POSTPROCESSING_OVERALL_BRIGHTNESS_DEF");
            AmuseSettings.POSTPROCESSING_AXES_BRIGHTNESS_DEF = props
                    .getFloatProperty("POSTPROCESSING_AXES_BRIGHTNESS_DEF");
            AmuseSettings.POSTPROCESSING_GAS_BRIGHTNESS_DEF = props
                    .getFloatProperty("POSTPROCESSING_GAS_BRIGHTNESS_DEF");
            AmuseSettings.POSTPROCESSING_STAR_HALO_BRIGHTNESS_DEF = props
                    .getFloatProperty("POSTPROCESSING_STAR_HALO_BRIGHTNESS_DEF");
            AmuseSettings.POSTPROCESSING_STAR_BRIGHTNESS_DEF = props
                    .getFloatProperty("POSTPROCESSING_STAR_BRIGHTNESS_DEF");

            // Settings for the star-shape blur method (the + shape of stars)
            AmuseSettings.STAR_SHAPE_BLUR_SIZE = props.getIntProperty("STAR_SHAPE_BLUR_SIZE");
            AmuseSettings.STAR_SHAPE_BLURFILTER_SIZE = props.getFloatProperty("STAR_SHAPE_BLURFILTER_SIZE");
            AmuseSettings.STAR_SHAPE_SIGMA = props.getFloatProperty("STAR_SHAPE_SIGMA");
            AmuseSettings.STAR_SHAPE_ALPHA = props.getFloatProperty("STAR_SHAPE_ALPHA");
            AmuseSettings.STAR_SHAPE_BLUR_TYPE = props.getIntProperty("STAR_SHAPE_BLUR_TYPE");

            // Settings for the detail levels.
            AmuseSettings.LEVEL_OF_DETAIL = props.getIntProperty("LEVEL_OF_DETAIL");

            AmuseSettings.LOW_GAS_BLUR_PASSES = props.getIntProperty("LOW_GAS_BLUR_PASSES");
            AmuseSettings.LOW_GAS_BLUR_SIZE = props.getFloatProperty("LOW_GAS_BLUR_SIZE");
            AmuseSettings.LOW_GAS_BLUR_TYPE = props.getIntProperty("LOW_GAS_BLUR_TYPE");

            AmuseSettings.LOW_STAR_HALO_BLUR_PASSES = props.getIntProperty("LOW_STAR_HALO_BLUR_PASSES");
            AmuseSettings.LOW_STAR_HALO_BLUR_SIZE = props.getFloatProperty("LOW_STAR_HALO_BLUR_SIZE");
            AmuseSettings.LOW_STAR_HALO_BLUR_TYPE = props.getIntProperty("LOW_STAR_HALO_BLUR_TYPE");

            AmuseSettings.LOW_GAS_SUBDIVISION = props.getIntProperty("LOW_GAS_SUBDIVISION");
            AmuseSettings.LOW_STAR_SUBDIVISION = props.getIntProperty("LOW_STAR_SUBDIVISION");
            AmuseSettings.LOW_GAS_PARTICLES_PER_OCTREE_NODE = props.getIntProperty("LOW_GAS_PARTICLES_PER_OCTREE_NODE");

            AmuseSettings.MEDIUM_GAS_BLUR_PASSES = props.getIntProperty("MEDIUM_GAS_BLUR_PASSES");
            AmuseSettings.MEDIUM_GAS_BLUR_SIZE = props.getFloatProperty("MEDIUM_GAS_BLUR_SIZE");
            AmuseSettings.MEDIUM_GAS_BLUR_TYPE = props.getIntProperty("MEDIUM_GAS_BLUR_TYPE");

            AmuseSettings.MEDIUM_STAR_HALO_BLUR_PASSES = props.getIntProperty("MEDIUM_STAR_HALO_BLUR_PASSES");
            AmuseSettings.MEDIUM_STAR_HALO_BLUR_SIZE = props.getFloatProperty("MEDIUM_STAR_HALO_BLUR_SIZE");
            AmuseSettings.MEDIUM_STAR_HALO_BLUR_TYPE = props.getIntProperty("MEDIUM_STAR_HALO_BLUR_TYPE");

            AmuseSettings.MEDIUM_GAS_SUBDIVISION = props.getIntProperty("MEDIUM_GAS_SUBDIVISION");
            AmuseSettings.MEDIUM_STAR_SUBDIVISION = props.getIntProperty("MEDIUM_STAR_SUBDIVISION");
            AmuseSettings.MEDIUM_GAS_PARTICLES_PER_OCTREE_NODE = props
                    .getIntProperty("MEDIUM_GAS_PARTICLES_PER_OCTREE_NODE");

            AmuseSettings.HIGH_GAS_BLUR_PASSES = props.getIntProperty("HIGH_GAS_BLUR_PASSES");
            AmuseSettings.HIGH_GAS_BLUR_SIZE = props.getFloatProperty("HIGH_GAS_BLUR_SIZE");
            AmuseSettings.HIGH_GAS_BLUR_TYPE = props.getIntProperty("HIGH_GAS_BLUR_TYPE");

            AmuseSettings.HIGH_STAR_HALO_BLUR_PASSES = props.getIntProperty("HIGH_STAR_HALO_BLUR_PASSES");
            AmuseSettings.HIGH_STAR_HALO_BLUR_SIZE = props.getFloatProperty("HIGH_STAR_HALO_BLUR_SIZE");
            AmuseSettings.HIGH_STAR_HALO_BLUR_TYPE = props.getIntProperty("HIGH_STAR_HALO_BLUR_TYPE");

            AmuseSettings.HIGH_GAS_SUBDIVISION = props.getIntProperty("HIGH_GAS_SUBDIVISION");
            AmuseSettings.HIGH_STAR_SUBDIVISION = props.getIntProperty("HIGH_STAR_SUBDIVISION");
            AmuseSettings.HIGH_GAS_PARTICLES_PER_OCTREE_NODE = props
                    .getIntProperty("HIGH_GAS_PARTICLES_PER_OCTREE_NODE");

            // Snaphots have different settings, since they are rendered at
            // extremely
            // high resolutions pixels
            AmuseSettings.SNAPSHOT_GAS_BLUR_PASSES = props.getIntProperty("SNAPSHOT_GAS_BLUR_PASSES");
            AmuseSettings.SNAPSHOT_GAS_BLUR_SIZE = props.getFloatProperty("SNAPSHOT_GAS_BLUR_SIZE");
            AmuseSettings.SNAPSHOT_GAS_BLUR_TYPE = props.getIntProperty("SNAPSHOT_GAS_BLUR_TYPE");

            AmuseSettings.SNAPSHOT_STAR_HALO_BLUR_PASSES = props.getIntProperty("SNAPSHOT_STAR_HALO_BLUR_PASSES");
            AmuseSettings.SNAPSHOT_STAR_HALO_BLUR_SIZE = props.getFloatProperty("SNAPSHOT_STAR_HALO_BLUR_SIZE");
            AmuseSettings.SNAPSHOT_STAR_HALO_BLUR_TYPE = props.getIntProperty("SNAPSHOT_STAR_HALO_BLUR_TYPE");

            AmuseSettings.GAS_COLOR_INVERTED = props.getBooleanProperty("GAS_COLOR_INVERTED");
            AmuseSettings.GAS_COLOR_BACKGROUND_INVERTED = props.getBooleanProperty("GAS_COLOR_BACKGROUND_INVERTED");
            AmuseSettings.GAS_COLOR_FROM_STARS = props.getBooleanProperty("GAS_COLOR_FROM_STARS");
            AmuseSettings.STAR_COLORS_EXAGGERATED = props.getBooleanProperty("STAR_COLORS_EXAGGERATED");

            AmuseSettings.WAITTIME_FOR_RETRY = props.getLongProperty("WAITTIME_FOR_RETRY");
            AmuseSettings.WAITTIME_FOR_MOVIE = props.getLongProperty("WAITTIME_FOR_MOVIE");
            AmuseSettings.EPSILON = props.getFloatProperty("EPSILON");

            AmuseSettings.GAS_OPACITY_FACTOR_MIN = props.getFloatProperty("GAS_OPACITY_FACTOR_MIN");
            AmuseSettings.GAS_OPACITY_FACTOR_DEF = props.getFloatProperty("GAS_OPACITY_FACTOR_DEF");
            AmuseSettings.GAS_OPACITY_FACTOR_MAX = props.getFloatProperty("GAS_OPACITY_FACTOR_MAX");

            AmuseSettings.BEZIER_INTERPOLATION = props.getBooleanProperty("BEZIER_INTERPOLATION");
            AmuseSettings.BEZIER_INTERPOLATION_STEPS = props.getIntProperty("BEZIER_INTERPOLATION_STEPS");

            AmuseSettings.PREPROCESSING_AMOUNT = props.getIntProperty("PREPROCESSING_AMOUNT");
        } catch (NumberFormatException e) {
            System.out.println(e.getMessage());
        }
    }

    public float getEpsilon() {
        return AmuseSettings.EPSILON;
    }

    public int getGasBlurPasses() {
        switch (AmuseSettings.LEVEL_OF_DETAIL) {
            case 0:
                return AmuseSettings.LOW_GAS_BLUR_PASSES;
            case 1:
                return AmuseSettings.MEDIUM_GAS_BLUR_PASSES;
            case 2:
                return AmuseSettings.HIGH_GAS_BLUR_PASSES;
        }
        return 0;
    }

    public float getGasBlurSize() {
        switch (AmuseSettings.LEVEL_OF_DETAIL) {
            case 0:
                return AmuseSettings.LOW_GAS_BLUR_SIZE;
            case 1:
                return AmuseSettings.MEDIUM_GAS_BLUR_SIZE;
            case 2:
                return AmuseSettings.HIGH_GAS_BLUR_SIZE;
        }
        return 0;
    }

    public int getGasBlurType() {
        switch (AmuseSettings.LEVEL_OF_DETAIL) {
            case 0:
                return AmuseSettings.LOW_GAS_BLUR_TYPE;
            case 1:
                return AmuseSettings.MEDIUM_GAS_BLUR_TYPE;
            case 2:
                return AmuseSettings.HIGH_GAS_BLUR_TYPE;
        }
        return 0;
    }

    public boolean getGasInvertedBackgroundColor() {
        return AmuseSettings.GAS_COLOR_BACKGROUND_INVERTED;
    }

    public boolean getGasInvertedColor() {
        return AmuseSettings.GAS_COLOR_INVERTED;
    }

    public int getGasParticlesPerOctreeNode() {
        switch (AmuseSettings.LEVEL_OF_DETAIL) {
            case 0:
                return AmuseSettings.LOW_GAS_PARTICLES_PER_OCTREE_NODE;
            case 1:
                return AmuseSettings.MEDIUM_GAS_PARTICLES_PER_OCTREE_NODE;
            case 2:
                return AmuseSettings.HIGH_GAS_PARTICLES_PER_OCTREE_NODE;
        }
        return 0;
    }

    public boolean getGasStarInfluencedColor() {
        return AmuseSettings.GAS_COLOR_FROM_STARS;
    }

    public int getGasSubdivision() {
        switch (AmuseSettings.LEVEL_OF_DETAIL) {
            case 0:
                return AmuseSettings.LOW_GAS_SUBDIVISION;
            case 1:
                return AmuseSettings.MEDIUM_GAS_SUBDIVISION;
            case 2:
                return AmuseSettings.HIGH_GAS_SUBDIVISION;
        }
        return 0;
    }

    public int getLOD() {
        return AmuseSettings.LEVEL_OF_DETAIL;
    }

    public float getPostprocessingAxesBrightness() {
        return AmuseSettings.POSTPROCESSING_AXES_BRIGHTNESS_DEF;
    }

    public float getPostprocessingAxesBrightnessMax() {
        return AmuseSettings.POSTPROCESSING_AXES_BRIGHTNESS_MAX;
    }

    public float getPostprocessingAxesBrightnessMin() {
        return AmuseSettings.POSTPROCESSING_AXES_BRIGHTNESS_MIN;
    }

    public float getPostprocessingGasBrightness() {
        return AmuseSettings.POSTPROCESSING_GAS_BRIGHTNESS_DEF;
    }

    public float getPostprocessingGasBrightnessMax() {
        return AmuseSettings.POSTPROCESSING_GAS_BRIGHTNESS_MAX;
    }

    public float getPostprocessingGasBrightnessMin() {
        return AmuseSettings.POSTPROCESSING_GAS_BRIGHTNESS_MIN;
    }

    public float getPostprocessingHudBrightness() {
        return AmuseSettings.POSTPROCESSING_HUD_BRIGHTNESS_DEF;
    }

    public float getPostprocessingHudBrightnessMax() {
        return AmuseSettings.POSTPROCESSING_HUD_BRIGHTNESS_MAX;
    }

    public float getPostprocessingHudBrightnessMin() {
        return AmuseSettings.POSTPROCESSING_HUD_BRIGHTNESS_MIN;
    }

    public float getPostprocessingOverallBrightness() {
        return AmuseSettings.POSTPROCESSING_OVERALL_BRIGHTNESS_DEF;
    }

    public float getPostprocessingOverallBrightnessMax() {
        return AmuseSettings.POSTPROCESSING_OVERALL_BRIGHTNESS_MAX;
    }

    public float getPostprocessingOverallBrightnessMin() {
        return AmuseSettings.POSTPROCESSING_OVERALL_BRIGHTNESS_MIN;
    }

    public float getPostprocessingStarBrightness() {
        return AmuseSettings.POSTPROCESSING_STAR_BRIGHTNESS_DEF;
    }

    public float getPostprocessingStarBrightnessMax() {
        return AmuseSettings.POSTPROCESSING_STAR_BRIGHTNESS_MAX;
    }

    public float getPostprocessingStarBrightnessMin() {
        return AmuseSettings.POSTPROCESSING_STAR_BRIGHTNESS_MIN;
    }

    public float getPostprocessingStarHaloBrightness() {
        return AmuseSettings.POSTPROCESSING_STAR_HALO_BRIGHTNESS_DEF;
    }

    public float getPostprocessingStarHaloBrightnessMax() {
        return AmuseSettings.POSTPROCESSING_STAR_HALO_BRIGHTNESS_MAX;
    }

    public float getPostprocessingStarHaloBrightnessMin() {
        return AmuseSettings.POSTPROCESSING_STAR_HALO_BRIGHTNESS_MIN;
    }

    public int getSnapshotGasBlurPasses() {
        return AmuseSettings.SNAPSHOT_GAS_BLUR_PASSES;
    }

    public float getSnapshotGasBlurSize() {
        return AmuseSettings.SNAPSHOT_GAS_BLUR_SIZE;
    }

    public int getSnapshotGasBlurType() {
        return AmuseSettings.SNAPSHOT_GAS_BLUR_TYPE;
    }

    public int getSnapshotStarHaloBlurPasses() {
        return AmuseSettings.SNAPSHOT_STAR_HALO_BLUR_PASSES;
    }

    public float getSnapshotStarHaloBlurSize() {
        return AmuseSettings.SNAPSHOT_STAR_HALO_BLUR_SIZE;
    }

    public int getSnapshotStarHaloBlurType() {
        return AmuseSettings.SNAPSHOT_STAR_HALO_BLUR_TYPE;
    }

    public boolean getStarColorsExaggerated() {
        return AmuseSettings.STAR_COLORS_EXAGGERATED;
    }

    public int getStarHaloBlurPasses() {
        switch (AmuseSettings.LEVEL_OF_DETAIL) {
            case 0:
                return AmuseSettings.LOW_STAR_HALO_BLUR_PASSES;
            case 1:
                return AmuseSettings.MEDIUM_STAR_HALO_BLUR_PASSES;
            case 2:
                return AmuseSettings.HIGH_STAR_HALO_BLUR_PASSES;
        }
        return 0;
    }

    public float getStarHaloBlurSize() {
        switch (AmuseSettings.LEVEL_OF_DETAIL) {
            case 0:
                return AmuseSettings.LOW_STAR_HALO_BLUR_SIZE;
            case 1:
                return AmuseSettings.MEDIUM_STAR_HALO_BLUR_SIZE;
            case 2:
                return AmuseSettings.HIGH_STAR_HALO_BLUR_SIZE;
        }
        return 0;
    }

    public int getStarHaloBlurType() {
        switch (AmuseSettings.LEVEL_OF_DETAIL) {
            case 0:
                return AmuseSettings.LOW_STAR_HALO_BLUR_TYPE;
            case 1:
                return AmuseSettings.MEDIUM_STAR_HALO_BLUR_TYPE;
            case 2:
                return AmuseSettings.HIGH_STAR_HALO_BLUR_TYPE;
        }
        return 0;
    }

    public float getStarShapeAlpha() {
        return AmuseSettings.STAR_SHAPE_ALPHA;
    }

    public float getStarShapeBlurfilterSize() {
        return AmuseSettings.STAR_SHAPE_BLURFILTER_SIZE;
    }

    public int getStarShapeBlurSize() {
        return AmuseSettings.STAR_SHAPE_BLUR_SIZE;
    }

    public int getStarShapeBlurType() {
        return AmuseSettings.STAR_SHAPE_BLUR_TYPE;
    }

    public float getStarShapeSigma() {
        return AmuseSettings.STAR_SHAPE_SIGMA;
    }

    public int getStarSubdivision() {
        switch (AmuseSettings.LEVEL_OF_DETAIL) {
            case 0:
                return AmuseSettings.LOW_STAR_SUBDIVISION;
            case 1:
                return AmuseSettings.MEDIUM_STAR_SUBDIVISION;
            case 2:
                return AmuseSettings.HIGH_STAR_SUBDIVISION;
        }
        return 0;
    }

    public long getWaitTimeMovie() {
        return AmuseSettings.WAITTIME_FOR_MOVIE;
    }

    public long getWaitTimeRetry() {
        return AmuseSettings.WAITTIME_FOR_RETRY;
    }

    public void setGasInvertedBackgroundColor(int stateChange) {
        if (stateChange == 1) {
            AmuseSettings.GAS_COLOR_BACKGROUND_INVERTED = true;
        }
        if (stateChange == 2) {
            AmuseSettings.GAS_COLOR_BACKGROUND_INVERTED = false;
        }
    }

    public void setInvertGasColor(int stateChange) {
        if (stateChange == 1) {
            AmuseSettings.GAS_COLOR_INVERTED = true;
        }
        if (stateChange == 2) {
            AmuseSettings.GAS_COLOR_INVERTED = false;
        }
    }

    public void setLOD(int levelOfDetail) {
        AmuseSettings.LEVEL_OF_DETAIL = levelOfDetail;
    }

    public void setPostprocessingAxesBrightness(float value) {
        AmuseSettings.POSTPROCESSING_AXES_BRIGHTNESS_DEF = value;
    }

    public void setPostprocessingGasBrightness(float value) {
        AmuseSettings.POSTPROCESSING_GAS_BRIGHTNESS_DEF = value;
    }

    public void setPostprocessingHudBrightness(int value) {
        AmuseSettings.POSTPROCESSING_HUD_BRIGHTNESS_DEF = value;
    }

    public void setPostprocessingOverallBrightness(float value) {
        AmuseSettings.POSTPROCESSING_OVERALL_BRIGHTNESS_DEF = value;
    }

    public void setPostprocessingStarBrightness(float value) {
        AmuseSettings.POSTPROCESSING_STAR_BRIGHTNESS_DEF = value;
    }

    public void setPostprocessingStarHaloBrightness(float value) {
        AmuseSettings.POSTPROCESSING_STAR_HALO_BRIGHTNESS_DEF = value;
    }

    public void setStarColorsExaggerated(int stateChange) {
        if (stateChange == 1) {
            AmuseSettings.STAR_COLORS_EXAGGERATED = true;
        }
        if (stateChange == 2) {
            AmuseSettings.STAR_COLORS_EXAGGERATED = false;
        }
    }

    public void setStarInfluencedGasColor(int stateChange) {
        if (stateChange == 1) {
            AmuseSettings.GAS_COLOR_FROM_STARS = true;
        }
        if (stateChange == 2) {
            AmuseSettings.GAS_COLOR_FROM_STARS = false;
        }
    }

    public void setWaitTimeMovie(long value) {
        AmuseSettings.WAITTIME_FOR_MOVIE = value;
    }

    public void setGasOpacityFactor(float value) {
        AmuseSettings.GAS_OPACITY_FACTOR_DEF = value;
    }

    public float getGasOpacityFactorMin() {
        return AmuseSettings.GAS_OPACITY_FACTOR_MIN;
    }

    public float getGasOpacityFactor() {
        return AmuseSettings.GAS_OPACITY_FACTOR_DEF;
    }

    public float getGasOpacityFactorMax() {
        return AmuseSettings.GAS_OPACITY_FACTOR_MAX;
    }

    public void setBezierInterpolation(boolean value) {
        AmuseSettings.BEZIER_INTERPOLATION = value;
    }

    public boolean getBezierInterpolation() {
        return AmuseSettings.BEZIER_INTERPOLATION;
    }

    public void setBezierInterpolationSteps(int value) {
        AmuseSettings.BEZIER_INTERPOLATION_STEPS = value;
    }

    public int getBezierInterpolationSteps() {
        return AmuseSettings.BEZIER_INTERPOLATION_STEPS;
    }

    public int getPreprocessAmount() {
        return AmuseSettings.PREPROCESSING_AMOUNT;
    }

    public void setPreprocessAmount(int value) {
        AmuseSettings.PREPROCESSING_AMOUNT = value;
    }
}
