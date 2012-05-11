package amuse.visualization.amuseAdaptor;

import java.util.HashMap;
import java.util.Map;

import openglCommon.math.VecF3;
import openglCommon.models.Model;
import amuse.visualization.AmuseSettings;

public class GasMap extends HashMap<Long, Gas> {
    private static final AmuseSettings settings = AmuseSettings.getInstance();
    private static final long serialVersionUID = -8418660952368158592L;

    public AmuseGasOctreeNode process(Model baseModel) {
        final int gasSubdivision = settings.getGasSubdivision();
        final int gasParticlesPerOctreeNode = settings.getGasParticlesPerOctreeNode();

        final float edge = settings.getOctreeEdges();

        AmuseGasOctreeNode root = new AmuseGasOctreeNode(baseModel, gasParticlesPerOctreeNode, 0, gasSubdivision,
                new VecF3(-edge, -edge, -edge), edge);

        for (Map.Entry<Long, Gas> entry : entrySet()) {
            Gas g = entry.getValue();
            g.init();

            VecF3 location = g.processedLocation;
            float energy = g.energy;

            root.addElement(new AmuseGasOctreeElement(location, energy));
        }

        root.finalizeAdding();

        return root;
    }

    public AmuseGasOctreeNode[] process(Model baseModel, GasMap otherGasMap) {
        final int gasSubdivision = settings.getGasSubdivision();
        final int gasParticlesPerOctreeNode = settings.getGasParticlesPerOctreeNode();

        final float edge = settings.getOctreeEdges();

        int steps = settings.getBezierInterpolationSteps();
        AmuseGasOctreeNode[] result = new AmuseGasOctreeNode[steps];
        for (int i = 0; i < steps; i++) {
            result[i] = new AmuseGasOctreeNode(baseModel, gasParticlesPerOctreeNode, 0, gasSubdivision, new VecF3(
                    -edge, -edge, -edge), edge);

            for (Map.Entry<Long, Gas> entry : entrySet()) {
                Long key = entry.getKey();
                Gas g = entry.getValue();
                g.init(otherGasMap.get(key));

                VecF3 location = g.bezierPoints[i];
                float energy = g.interpolatedEnergy[i];

                result[i].addElement(new AmuseGasOctreeElement(location, energy));
            }

            result[i].finalizeAdding();
        }

        return result;
    }
}
