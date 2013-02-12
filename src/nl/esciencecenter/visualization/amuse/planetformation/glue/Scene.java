package nl.esciencecenter.visualization.amuse.planetformation.glue;

public class Scene {
    private final double     time;
    private final Sphere[]   spheres;

    private final Star[]     stars;
    private final Planet[]   planets;

    private final SPHGas[]   sphGas;
    private final PointGas[] pointGas;

    public Scene(double time, Sphere[] spheres, Star[] stars, Planet[] planets,
            SPHGas[] sphGas, PointGas[] pointGas) {
        this.time = time;
        this.spheres = spheres;
        this.stars = stars;
        this.planets = planets;
        this.sphGas = sphGas;
        this.pointGas = pointGas;
    }

    public double getTime() {
        return time;
    }

    public Sphere[] getSpheres() {
        return spheres;
    }

    public Star[] getStars() {
        return stars;
    }

    public Planet[] getPlanets() {
        return planets;
    }

    public SPHGas[] getSphGas() {
        return sphGas;
    }

    public PointGas[] getPointGas() {
        return pointGas;
    }

}
