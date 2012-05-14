package amuse.visualization.amuseAdaptor;

import java.util.HashMap;
import java.util.Map;

import openglCommon.exceptions.UninitializedException;
import openglCommon.math.VecF3;
import openglCommon.models.Model;
import amuse.visualization.AmuseSettings;

public class GasMap extends HashMap<Long, Gas> {
    private static final AmuseSettings settings = AmuseSettings.getInstance();
    private static final long serialVersionUID = -8418660952368158592L;

    public AmuseGasOctreeNode process(Model baseModel) {
        AmuseGasOctreeNode root = processUninterpolated(baseModel);

        root.finalizeAdding();

        return root;
    }

    public AmuseGasOctreeNode process(Model baseModel, StarMap stars) {
        AmuseGasOctreeNode root = processUninterpolated(baseModel);

        root.finalizeAdding(stars);

        return root;
    }

    public AmuseGasOctreeNode[] process(Model baseModel, GasMap otherGasMap) {
        AmuseGasOctreeNode[] result = processInterpolated(baseModel, otherGasMap);

        for (int i = 0; i < result.length; i++) {
            result[i].finalizeAdding();
        }

        return result;
    }

    public AmuseGasOctreeNode[] process(Model baseModel, GasMap otherGasMap, StarMap stars) {
        AmuseGasOctreeNode[] result = processInterpolated(baseModel, otherGasMap);

        for (int i = 0; i < result.length; i++) {
            result[i].finalizeAdding(stars);
        }

        return result;
    }

    private AmuseGasOctreeNode processUninterpolated(Model baseModel) {
        final int gasSubdivision = settings.getGasSubdivision();
        final int gasParticlesPerOctreeNode = settings.getGasParticlesPerOctreeNode();

        final float edge = settings.getOctreeEdges();

        AmuseGasOctreeNode root = new AmuseGasOctreeNode(baseModel, gasParticlesPerOctreeNode, 0, gasSubdivision,
                new VecF3(-edge, -edge, -edge), edge);

        try {
            for (Map.Entry<Long, Gas> entry : entrySet()) {
                Gas g = entry.getValue();
                g.init();

                VecF3 location = g.getLocation();
                float energy = g.getEnergy();

                root.addElement(new AmuseGasOctreeElement(location, energy));
            }
        } catch (UninitializedException e) {
            e.printStackTrace();
        }
        return root;
    }

    private AmuseGasOctreeNode[] processInterpolated(Model baseModel, GasMap otherGasMap) {
        int steps = settings.getBezierInterpolationSteps();
        AmuseGasOctreeNode[] result = new AmuseGasOctreeNode[steps];

        final int gasSubdivision = settings.getGasSubdivision();
        final int gasParticlesPerOctreeNode = settings.getGasParticlesPerOctreeNode();

        final float edge = settings.getOctreeEdges();

        for (int i = 0; i < steps; i++) {
            try {
                result[i] = new AmuseGasOctreeNode(baseModel, gasParticlesPerOctreeNode, 0, gasSubdivision, new VecF3(
                        -edge, -edge, -edge), edge);

                for (Map.Entry<Long, Gas> entry : entrySet()) {
                    Long key = entry.getKey();
                    Gas g = entry.getValue();
                    g.init(otherGasMap.get(key));

                    VecF3 location = g.getLocation(i);
                    float energy = g.getEnergy(i);

                    result[i].addElement(new AmuseGasOctreeElement(location, energy));
                }
            } catch (UninitializedException e) {
                e.printStackTrace();
            }
        }

        return result;
    }
}
