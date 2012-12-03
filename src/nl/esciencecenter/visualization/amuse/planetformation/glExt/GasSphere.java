package nl.esciencecenter.visualization.amuse.planetformation.glExt;

import java.util.ArrayList;
import java.util.List;

import openglCommon.math.VecF3;
import openglCommon.math.VecF4;
import openglCommon.math.VectorFMath;

public class GasSphere extends GasModel {
    private static float X        = 0.525731112119133606f;
    private static float Z        = 0.850650808352039932f;

    static VecF3[]       vdata    = { new VecF3(-X, 0f, Z),
            new VecF3(X, 0f, Z), new VecF3(-X, 0f, -Z), new VecF3(X, 0f, -Z),
            new VecF3(0f, Z, X), new VecF3(0f, Z, -X), new VecF3(0f, -Z, X),
            new VecF3(0f, -Z, -X), new VecF3(Z, X, 0f), new VecF3(-Z, X, 0f),
            new VecF3(Z, -X, 0f), new VecF3(-Z, -X, 0f) };

    static int[][]       tindices = { { 1, 4, 0 }, { 4, 9, 0 }, { 4, 5, 9 },
            { 8, 5, 4 }, { 1, 8, 4 }, { 1, 10, 8 }, { 10, 3, 8 }, { 8, 3, 5 },
            { 3, 2, 5 }, { 3, 7, 2 }, { 3, 10, 7 }, { 10, 6, 7 }, { 6, 11, 7 },
            { 6, 0, 11 }, { 6, 1, 0 }, { 10, 1, 6 }, { 11, 0, 9 },
            { 2, 11, 9 }, { 5, 2, 9 }, { 11, 2, 7 } };

    public GasSphere(int ndiv, float radius, VecF3 center) {
        super();

        List<VecF3> points3List = new ArrayList<VecF3>();

        for (int i = 0; i < 20; i++) {
            makeVertices(points3List, vdata[tindices[i][0]],
                    vdata[tindices[i][1]], vdata[tindices[i][2]], ndiv, radius);
        }

        List<VecF4> pointsList = new ArrayList<VecF4>();

        for (int i = 0; i < points3List.size(); i++) {
            pointsList.add(new VecF4(points3List.get(i).add(center), 1f));
        }

        numVertices = pointsList.size();

        vertices = VectorFMath.vec4ListToBuffer(pointsList);
    }

    private void makeVertices(List<VecF3> pointsList, VecF3 a, VecF3 b,
            VecF3 c, int div, float r) {
        if (div <= 0) {
            VecF3 na = new VecF3(a);
            VecF3 nb = new VecF3(b);
            VecF3 nc = new VecF3(c);

            VecF3 ra = na.mul(r);
            VecF3 rb = nb.mul(r);
            VecF3 rc = nc.mul(r);

            pointsList.add(ra);
            pointsList.add(rb);
            pointsList.add(rc);
        } else {
            VecF3 ab = new VecF3();
            VecF3 ac = new VecF3();
            VecF3 bc = new VecF3();

            for (int i = 0; i < 3; i++) {
                ab.set(i, (a.get(i) + b.get(i)));
                ac.set(i, (a.get(i) + c.get(i)));
                bc.set(i, (b.get(i) + c.get(i)));
            }

            ab = VectorFMath.normalize(ab);
            ac = VectorFMath.normalize(ac);
            bc = VectorFMath.normalize(bc);

            makeVertices(pointsList, a, ab, ac, div - 1, r);
            makeVertices(pointsList, b, bc, ab, div - 1, r);
            makeVertices(pointsList, c, ac, bc, div - 1, r);
            makeVertices(pointsList, ab, bc, ac, div - 1, r);
        }
    }
}
