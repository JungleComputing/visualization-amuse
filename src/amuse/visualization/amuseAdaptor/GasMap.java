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

            VecF3 location = g.location;

            if (!((location.get(0) < -edge) && (location.get(0) > edge) && (location.get(1) < -edge)
                    && (location.get(1) > edge) && (location.get(2) < -edge) && (location.get(2) > edge))) {
                root.addElement(new AmuseGasOctreeElement(Astrophysics.locationToScreenCoord(location), g.energy));
            }
        }

        root.finalizeAdding();

        return root;
    }
}
