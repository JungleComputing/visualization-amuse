package nl.esciencecenter.visualization.amuse.planetformation.data;

import java.nio.ByteBuffer;

import nl.esciencecenter.visualization.amuse.planetformation.AmuseSettings;
import nl.esciencecenter.visualization.amuse.planetformation.util.ColormapInterpreter;
import nl.esciencecenter.visualization.amuse.planetformation.util.ColormapInterpreter.Color;
import nl.esciencecenter.visualization.amuse.planetformation.util.ColormapInterpreter.Dimensions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegendTextureBuilder implements Runnable {
    private final static Logger         logger   = LoggerFactory
                                                         .getLogger(LegendTextureBuilder.class);
    private final AmuseSettings         settings = AmuseSettings.getInstance();

    private final AmuseSceneDescription description;
    private final AmuseDataArray        inputArray;
    private final AmuseSceneStorage     sceneStore;

    private boolean                     initialized;

    public LegendTextureBuilder(AmuseSceneStorage sceneStore,
            AmuseDataArray inputArray) {
        this.sceneStore = sceneStore;
        this.inputArray = inputArray;
        this.description = inputArray.getDescription();
    }

    @Override
    public void run() {
        if (!initialized) {
            Dimensions dims = getDimensions(inputArray.getDescription());

            int height = 500;
            int width = 1;
            ByteBuffer outBuf = ByteBuffer.allocate(height * width * 4);

            for (int row = height - 1; row >= 0; row--) {
                float index = row / (float) height;
                float var = (index * dims.getDiff()) + dims.min;

                Color c = ColormapInterpreter.getColor(
                        description.getColorMap(), dims, var);

                for (int col = 0; col < width; col++) {
                    outBuf.put((byte) (255 * c.red));
                    outBuf.put((byte) (255 * c.green));
                    outBuf.put((byte) (255 * c.blue));
                    outBuf.put((byte) 1);
                }
            }

            outBuf.flip();

            sceneStore.setLegendImage(description, outBuf);
        }
    }

    public Dimensions getDimensions(AmuseSceneDescription desc) {
        float min = desc.getLowerBound();
        float max = desc.getUpperBound();

        return new Dimensions(min, max);
    }
}
