package nl.esciencecenter.visualization.amuse.planetformation.interfaces;

import java.nio.ByteBuffer;

import javax.media.opengl.GL3;

public interface SceneStorage {

    public void init(GL3 gl);

    public void requestNewConfiguration(SceneDescription currentDescription);

    public VisualScene getScene();

    public ByteBuffer getLegendImage();

}
