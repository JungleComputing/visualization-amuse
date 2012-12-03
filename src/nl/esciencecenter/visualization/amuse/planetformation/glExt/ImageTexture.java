package nl.esciencecenter.visualization.amuse.planetformation.glExt;

import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import openglCommon.textures.Texture2D;

public class ImageTexture extends Texture2D {
    public ImageTexture(String filename, int w_offSet, int h_offSet,
            int glMultiTexUnit) {
        super(glMultiTexUnit);

        BufferedImage bi = null;
        try {
            bi = ImageIO.read(new FileInputStream(filename));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        int x = 0, y = 0, width = bi.getWidth(), height = bi.getHeight();

        this.width = width;
        this.height = height;

        int[] pixels = new int[width * height];
        PixelGrabber pg = new PixelGrabber(bi, x, y, width, height, pixels, 0,
                width);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
            System.err.println("interrupted waiting for pixels!");
            return;
        }

        ByteBuffer tempBuffer = ByteBuffer.allocate(width * height * 4);

        for (int row = (height + h_offSet) - 1; row >= h_offSet; row--) {
            int i = row;
            if (row >= height) {
                i = row - height;
            }

            for (int col = w_offSet; col < (width + w_offSet); col++) {
                int j = col;
                if (col >= width) {
                    j = col - width;
                }

                tempBuffer.put((byte) ((pixels[i * width + j]) & 0xff)); // blue
                tempBuffer.put((byte) ((pixels[i * width + j] >> 8) & 0xff)); // green
                tempBuffer.put((byte) ((pixels[i * width + j] >> 16) & 0xff)); // red
                tempBuffer.put((byte) ((pixels[i * width + j] >> 24) & 0xff)); // alpha
            }
        }

        tempBuffer.rewind();

        pixelBuffer = tempBuffer;
    }
}