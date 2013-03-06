package nl.esciencecenter.visualization.amuse.planetformation.glue.data;

import java.nio.ByteBuffer;

import nl.esciencecenter.visualization.amuse.planetformation.interfaces.SceneDescription;
import nl.esciencecenter.visualization.openglCommon.swing.ColormapInterpreter;
import nl.esciencecenter.visualization.openglCommon.swing.ColormapInterpreter.Color;
import nl.esciencecenter.visualization.openglCommon.swing.ColormapInterpreter.Dimensions;

public class LegendTextureBuilder implements Runnable {
    protected SceneDescription     description;
    private final GlueSceneStorage texStore;
    private boolean                initialized;

    public LegendTextureBuilder(GlueSceneStorage texStore,
            SceneDescription description) {
        this.texStore = texStore;
        this.description = description;
    }

    @Override
    public void run() {
        if (!initialized) {
            GlueSceneDescription desc = (GlueSceneDescription) description;
            Dimensions dims = new Dimensions(desc.getLowerBound(), desc.getUpperBound());

            int height = 500;
            int width = 1;
            ByteBuffer outBuf = ByteBuffer.allocate(height * width * 4);

            for (int row = height - 1; row >= 0; row--) {
                float index = row / (float) height;
                float var = (index * dims.getDiff()) + dims.min;

                Color c = ColormapInterpreter.getColor(
                        desc.getColorMap(), dims, var);

                for (int col = 0; col < width; col++) {
                    outBuf.put((byte) (255 * c.red));
                    outBuf.put((byte) (255 * c.green));
                    outBuf.put((byte) (255 * c.blue));
                    outBuf.put((byte) 1);
                }
            }

            outBuf.flip();

            texStore.setLegendImage(desc, outBuf);
        }
    }

    @Override
    public int hashCode() {
        return description.hashCode();
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject)
            return true;
        if (!(thatObject instanceof LegendTextureBuilder))
            return false;

        // cast to native object is now safe
        LegendTextureBuilder that = (LegendTextureBuilder) thatObject;

        // now a proper field-by-field evaluation can be made
        return (description.equals(that.description));
    }
}
