package amuse.visualization.amuseAdaptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class StarMap extends HashMap<Long, Star> {
    private static final long serialVersionUID = 2310517864008913234L;

    public ArrayList<Star> process() {
        ArrayList<Star> result = new ArrayList<Star>();

        for (Map.Entry<Long, Star> entry : entrySet()) {
            Star s = entry.getValue();
            result.add(s);
        }

        return result;
    }
}
