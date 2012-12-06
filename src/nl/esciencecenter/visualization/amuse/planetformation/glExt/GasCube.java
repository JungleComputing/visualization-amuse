package nl.esciencecenter.visualization.amuse.planetformation.glExt;

import nl.esciencecenter.visualization.openglCommon.math.Point4;
import nl.esciencecenter.visualization.openglCommon.math.VecF3;
import nl.esciencecenter.visualization.openglCommon.math.VectorFMath;

public class GasCube extends GasModel {
    public GasCube(float height, VecF3 center) {
        super();
        Point4[] vertices = makeVertices(height, center);

        int numVertices = 36;

        Point4[] points = new Point4[numVertices];

        int arrayindex = 0;
        arrayindex = newQuad(points, arrayindex, vertices, 1, 0, 3, 2); // FRONT
        arrayindex = newQuad(points, arrayindex, vertices, 2, 3, 7, 6); // RIGHT
        arrayindex = newQuad(points, arrayindex, vertices, 3, 0, 4, 7); // BOTTOM
        arrayindex = newQuad(points, arrayindex, vertices, 6, 5, 1, 2); // TOP
        arrayindex = newQuad(points, arrayindex, vertices, 4, 5, 6, 7); // BACK
        arrayindex = newQuad(points, arrayindex, vertices, 5, 4, 0, 1); // LEFT

        this.numVertices = numVertices;
        this.vertices = VectorFMath.toBuffer(points);
    }

    private Point4[] makeVertices(float height, VecF3 center) {
        float x = center.get(0);
        float y = center.get(1);
        float z = center.get(2);

        float xpos = x + height / 2f;
        float xneg = x - height / 2f;
        float ypos = y + height / 2f;
        float yneg = y - height / 2f;
        float zpos = z + height / 2f;
        float zneg = z - height / 2f;

        Point4[] result = new Point4[] { new Point4(xneg, yneg, zpos, 1.0f),
                new Point4(xneg, ypos, zpos, 1.0f),
                new Point4(xpos, ypos, zpos, 1.0f),
                new Point4(xpos, yneg, zpos, 1.0f),
                new Point4(xneg, yneg, zneg, 1.0f),
                new Point4(xneg, ypos, zneg, 1.0f),
                new Point4(xpos, ypos, zneg, 1.0f),
                new Point4(xpos, yneg, zneg, 1.0f) };

        return result;
    }

    private int newQuad(Point4[] points, int arrayindex, Point4[] source,
            int a, int b, int c, int d) {
        points[arrayindex] = source[a];
        arrayindex++;
        points[arrayindex] = source[b];
        arrayindex++;
        points[arrayindex] = source[c];
        arrayindex++;
        points[arrayindex] = source[a];
        arrayindex++;
        points[arrayindex] = source[c];
        arrayindex++;
        points[arrayindex] = source[d];
        arrayindex++;

        return arrayindex;
    }
}
