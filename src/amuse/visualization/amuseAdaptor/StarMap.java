package amuse.visualization.amuseAdaptor;

import java.util.HashMap;
import java.util.Map;

import javax.media.opengl.GL3;

import openglCommon.exceptions.UninitializedException;
import openglCommon.math.MatF4;
import openglCommon.shaders.Program;

public class StarMap extends HashMap<Long, Star> {
    private static final long serialVersionUID = 2310517864008913234L;

    public void process() {
        for (Map.Entry<Long, Star> entry : entrySet()) {
            Star s = entry.getValue();

            s.init();
        }
    }

    public void process(StarMap otherStars) {
        for (Map.Entry<Long, Star> entry : entrySet()) {
            Long key = entry.getKey();
            Star s = entry.getValue();
            Star o = otherStars.get(key);

            s.init(o);
        }
    }

    public void draw(GL3 gl, Program program, MatF4 MVMatrix) throws UninitializedException {
        for (Map.Entry<Long, Star> entry : entrySet()) {
            Star s = entry.getValue();

            s.draw(gl, program, MVMatrix);
        }
    }

    public void draw(GL3 gl, Program program, MatF4 MVMatrix, int step) throws UninitializedException {
        for (Map.Entry<Long, Star> entry : entrySet()) {
            Star s = entry.getValue();

            s.draw(gl, program, MVMatrix, step);
        }
    }
}
