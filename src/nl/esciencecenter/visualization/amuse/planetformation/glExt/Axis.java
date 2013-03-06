package nl.esciencecenter.visualization.amuse.planetformation.glExt;

import nl.esciencecenter.visualization.openglCommon.datastructures.Material;
import nl.esciencecenter.visualization.openglCommon.math.VecF3;
import nl.esciencecenter.visualization.openglCommon.math.VecF4;
import nl.esciencecenter.visualization.openglCommon.math.VectorFMath;
import nl.esciencecenter.visualization.openglCommon.models.Model;

public class Axis extends Model {
    public Axis(Material mat, VecF3 start, VecF3 end, float majorInterval,
            float minorInterval) {
        super(mat, vertex_format.LINES);

        float length = VectorFMath.length(end.sub(start));
        int numMajorIntervals = (int) Math.floor(length / majorInterval);
        int numMinorIntervals = (int) Math.floor(length / minorInterval);

        int numVertices = 2 + (numMajorIntervals * 2) - 4
                + (numMinorIntervals * 2) - 4;

        VecF4[] points = new VecF4[numVertices];
        VecF3[] normals = new VecF3[numVertices];
        VecF3[] tCoords = new VecF3[numVertices];

        int arrayindex = 0;
        points[0] = new VecF4(start, 1f);
        points[1] = new VecF4(end, 1f);

        normals[0] = VectorFMath.normalize(start).neg();
        normals[1] = VectorFMath.normalize(end).neg();

        tCoords[0] = new VecF3(0, 0, 0);
        tCoords[1] = new VecF3(1, 1, 1);

        arrayindex += 2;

        VecF3 perpendicular;
        VecF3 vec = VectorFMath.normalize((end.sub(start)));
        if (vec.get(0) > 0.5f) {
            perpendicular = new VecF3(0f, 0f, 1f);
        } else if (vec.get(1) > 0.5f) {
            perpendicular = new VecF3(1f, 0f, 0f);
        } else {
            perpendicular = new VecF3(1f, 0f, 0f);
        }

        VecF3 nil = new VecF3();
        float majorTickLength = length / 80;
        float minorTickLength = length / 240;

        for (int i = 1; i < numMajorIntervals / 2; i++) {
            arrayindex = addInterval(points, normals, tCoords, arrayindex,
                    nil.add(vec.mul(majorInterval * i)), perpendicular, majorTickLength);
            arrayindex = addInterval(points, normals, tCoords, arrayindex,
                    nil.sub(vec.mul(majorInterval * i)), perpendicular, majorTickLength);
        }

        for (int i = 1; i < numMinorIntervals / 2; i++) {
            arrayindex = addInterval(points, normals, tCoords, arrayindex,
                    nil.add(vec.mul(minorInterval * i)), perpendicular, minorTickLength);
            arrayindex = addInterval(points, normals, tCoords, arrayindex,
                    nil.sub(vec.mul(minorInterval * i)), perpendicular, minorTickLength);

        }

        this.numVertices = numVertices;
        this.vertices = VectorFMath.toBuffer(points);
        this.normals = VectorFMath.toBuffer(normals);
        this.texCoords = VectorFMath.toBuffer(tCoords);
    }

    private int addInterval(VecF4[] points, VecF3[] normals, VecF3[] tCoords,
            int arrayindex, VecF3 center, VecF3 alignment, float size) {
        points[arrayindex] = new VecF4(center.add(alignment.mul(size)), 1f);
        normals[arrayindex] = VectorFMath.normalize(alignment);
        tCoords[arrayindex] = new VecF3(0, 0, 0);
        arrayindex++;
        points[arrayindex] = new VecF4(center.sub(alignment.mul(size)), 1f);
        normals[arrayindex] = VectorFMath.normalize(alignment).neg();
        tCoords[arrayindex] = new VecF3(1, 1, 1);
        arrayindex++;
        return arrayindex;
    }
}
