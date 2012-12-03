package nl.esciencecenter.visualization.amuse.planetformation.glExt;


import java.nio.ByteBuffer;

public class ByteBufferTexture extends Texture2D {
    public ByteBufferTexture(int glMultitexUnit, ByteBuffer pixelBuffer, int width,
            int height) {
        super(glMultitexUnit);

        this.pixelBuffer = pixelBuffer;
        this.width = width;
        this.height = height;
    }
}
