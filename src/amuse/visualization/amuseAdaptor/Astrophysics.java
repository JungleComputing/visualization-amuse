package amuse.visualization.amuseAdaptor;

import java.util.HashMap;

import openglCommon.datastructures.Material;
import openglCommon.math.VecF3;
import openglCommon.math.VecF4;
import openglCommon.math.VectorFMath;
import amuse.visualization.AmuseSettings;

public class Astrophysics {
    private final static AmuseSettings settings                        = AmuseSettings.getInstance();

    public final static double         SIGMA                           = 5.67037321E-8;
    public final static double         WIEN                            = 2.8977685E-3;

    public final static double         SOLAR_LUMINOSITY                = 3.839E26;
    public final static double         SOLAR_RADIUS                    = 6.955E8;
    public final static double         EARTH_RADIUS                    = 6371;

    public final static double         PARSEC                          = 3.08568025E16;
    public final static double         DISTANCE_FACTOR                 = 25.0;
    public final static double         STAR_RADIUS_FACTOR_SMALL        = 0.25;
    public final static double         STAR_RADIUS_AT_1000_SOLAR_RADII = 8.0;

    public final static double         STAR_FORMULAE_INTERSECTION      = Astrophysics.find_intersection();

    private final static VecF4         INITIAL_GAS_COLOR               = new VecF4(.6f, .3f, .3f, 0f);
    private final static VecF4         transparent                     = new VecF4(0, 0, 0, 0);
    private final static Material      gasMaterial                     = new Material(Astrophysics.INITIAL_GAS_COLOR,
                                                                               Astrophysics.transparent,
                                                                               Astrophysics.transparent);

    private static double colorIntensity(double max, double min, double current) {
        return (current - min) / (max - min);
    }

    private static double find_intersection() {
        final double radius_factor = 1000.0 / Math.pow(Astrophysics.STAR_RADIUS_AT_1000_SOLAR_RADII, 2);

        for (double i = 0.1; i < 10000.0; i += 0.01) {
            final double diff = (i * Astrophysics.STAR_RADIUS_FACTOR_SMALL) - (Math.sqrt(i / radius_factor));
            if (diff > 0.0) {
                return i;
            }
        }
        return 0;
    }

    public static VecF4 gasColor(float density, float total_u_inNode, int membersOfnode) {
        float u = (float) (Math.sqrt(total_u_inNode / membersOfnode) / 5000.0);
        if (Float.isNaN(u)) {
            u = 0f;
        }
        if (Astrophysics.settings.getGasInvertedColor()) {
            return new VecF4(0f + u, 0f + u, 1f - u, density);
        } else {
            return new VecF4(1f - u, 0f + u, 0f + u, density);
        }
    }

    public static Material getGasMaterial() {
        return Astrophysics.gasMaterial;
    }

    public static int indexOfStarRadius(double size) {
        final double radius_in_solar = size / Astrophysics.SOLAR_RADIUS;

        return (int) Math.round(radius_in_solar * 10);
    }

    public static VecF3 locationToScreenCoord(double x, double y, double z) {
        final float fx = (float) (Astrophysics.DISTANCE_FACTOR * (x / Astrophysics.PARSEC));
        final float fy = (float) (Astrophysics.DISTANCE_FACTOR * (y / Astrophysics.PARSEC));
        final float fz = (float) (Astrophysics.DISTANCE_FACTOR * (z / Astrophysics.PARSEC));

        return new VecF3(fx, fy, fz);
    }

    public static VecF4 starColor(double luminosity, double radius) {
        luminosity *= Astrophysics.SOLAR_LUMINOSITY;

        final double temperature = Astrophysics.starTemperature(luminosity, radius);

        float intensity = 0f;

        final HashMap<Integer, Double> temperatureBands = new HashMap<Integer, Double>();
        temperatureBands.put(0, 2000.0);
        temperatureBands.put(1, 3500.0);
        temperatureBands.put(2, 5000.0);
        temperatureBands.put(3, 6000.0);
        temperatureBands.put(4, 7500.0);
        temperatureBands.put(5, 10000.0);
        temperatureBands.put(6, 30000.0);
        temperatureBands.put(7, 60000.0);

        float r = 0f, g = 0f, b = 0f;

        for (int i = 1; i < temperatureBands.size(); i++) {
            if (temperature <= temperatureBands.get(i)) {
                intensity = (float) (Astrophysics.colorIntensity(temperatureBands.get(i), temperatureBands.get(i - 1),
                        temperature));

                // Mix colors to create star color for x between 1 and 7.
                // Easy reference for function here:
                // http://fooplot.com/index.php?&type0=0&type1=0&type2=0&type3=0&type4=0&y0=&y1=%28sin%28%28.33*%28x-1%29%29%2B%28.5*pi%29%29%2B1%29/2&y2=%28sin%28%28.33*%28x-7%29%29%2B%28.5*pi%29%29%2B1%29/2&y3=%28sin%28%28.65*%28x-4%29%29%2B%28.5*pi%29%29%2B1%29/2&y4=&r0=&r1=&r2=&r3=&r4=&px0=&px1=&px2=&px3=&px4=&py0=&py1=&py2=&py3=&py4=&smin0=0&smin1=0&smin2=0&smin3=0&smin4=0&smax0=2pi&smax1=2pi&smax2=2pi&smax3=2pi&smax4=2pi&thetamin0=0&thetamin1=0&thetamin2=0&thetamin3=0&thetamin4=0&thetamax0=2pi&thetamax1=2pi&thetamax2=2pi&thetamax3=2pi&thetamax4=2pi&ipw=0&ixmin=-5&ixmax=5&iymin=-3&iymax=3&igx=1&igy=1&igl=1&igs=0&iax=1&ila=1&xmin=-1.58&xmax=8.42&ymin=-0.9199999999999998&ymax=5.080000000000001
                final float x = (i - 1) + intensity;
                r = (float) ((Math.sin((.33 * (x - 1.0)) + (0.5 * Math.PI)) + 1.0) / 2.0);
                g = (float) ((Math.sin((.66 * (x - 4.0)) + (0.5 * Math.PI)) + 1.0) / 2.5);
                b = (float) ((Math.sin((.33 * (x - 7.0)) + (0.5 * Math.PI)) + 1.0) / 2.0);

                // System.out.println(i + " " + r + " " + g + " " + b + " ");

                // Color components are a fraction of 1, so multiplying them
                // with themselves makes the prevalent one more distinct.
                if (Astrophysics.settings.getStarColorsExaggerated()) {
                    r *= r;
                    g *= g;
                    b *= b;
                }

                // We normalize to make sure that the stars have the same
                // brightness. This ensures that even almost completely white
                // stars retain their color.
                return VectorFMath.normalize(new VecF4(r, g, b, 1f));
            }
        }

        return new VecF4(r, g, b, 1f);
    }

    public static double starTemperature(double luminosity_in_solar_luminosities, double radius) {
        return Math.pow((luminosity_in_solar_luminosities / (4 * Math.PI * (radius * radius) * Astrophysics.SIGMA)),
                0.25);
    }

    public static float starToScreenRadius(double size) {
        final double radius_in_solar = size / Astrophysics.SOLAR_RADIUS;
        final double radius_factor = 1000.0 / Math.pow(Astrophysics.STAR_RADIUS_AT_1000_SOLAR_RADII, 2);

        float fs;
        if (radius_in_solar < Astrophysics.STAR_FORMULAE_INTERSECTION) {
            fs = (float) (radius_in_solar * Astrophysics.STAR_RADIUS_FACTOR_SMALL);
        } else {
            fs = (float) (Math.sqrt(radius_in_solar / radius_factor));
        }

        return fs;
    }

    public static float toScreenCoord(double parsecs) {
        final float fx = (float) (Astrophysics.DISTANCE_FACTOR * parsecs);

        return fx;
    }
}
