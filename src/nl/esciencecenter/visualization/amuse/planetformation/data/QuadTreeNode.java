package nl.esciencecenter.visualization.amuse.planetformation.data;

public class QuadTreeNode {
    protected enum Side {
        LEFT, RIGHT, TOP, BOTTOM
    };

    protected enum Quadrant {
        NN, PN, NP, PP
    };

    Quadrant quadrant;

    protected QuadTreeNode nn, pn, np, pp;
    protected QuadTreeNode parent;
    protected int          depth;
    protected boolean      subdivided;

    public QuadTreeNode(QuadTreeNode parent, Quadrant quadrant, int depth) {
        this.parent = parent;
        this.quadrant = quadrant;
        this.depth = depth;
    }

    public Quadrant quadrantAt(Side s) {
        switch (s) {
        case LEFT:
        case RIGHT:
            switch (quadrant) {
            case NN:
                return Quadrant.PN;
            case PN:
                return Quadrant.NN;
            case NP:
                return Quadrant.PP;
            case PP:
                return Quadrant.NP;
            default:
                return null;
            }
        case BOTTOM:
        case TOP:
            switch (quadrant) {
            case NN:
                return Quadrant.NP;
            case PN:
                return Quadrant.PP;
            case NP:
                return Quadrant.NN;
            case PP:
                return Quadrant.PN;
            default:
                return null;
            }
        default:
            return null;
        }
    }

    public QuadTreeNode getNeighbourAt(Side s) {
        switch (s) {
        case LEFT:
            switch (quadrant) {
            case NN:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(s).getChild(quadrantAt(s));
            case NP:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(s).getChild(quadrantAt(s));
            case PN:
                return nn;
            case PP:
                return np;
            default:
                return null;
            }
        case RIGHT:
            switch (quadrant) {
            case NN:
                return pn;
            case NP:
                return pp;
            case PN:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(s).getChild(quadrantAt(s));
            case PP:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(s).getChild(quadrantAt(s));
            default:
                return null;
            }
        case BOTTOM:
            switch (quadrant) {
            case NN:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(s).getChild(quadrantAt(s));
            case NP:
                return nn;
            case PN:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(s).getChild(quadrantAt(s));
            case PP:
                return pn;
            default:
                return null;
            }
        case TOP:
            switch (quadrant) {
            case NN:
                return np;
            case NP:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(s).getChild(quadrantAt(s));
            case PN:
                return pp;
            case PP:
                if (parent == null)
                    return null;
                return parent.getNeighbourAt(s).getChild(quadrantAt(s));
            default:
                return null;
            }
        default:
            return null;
        }
    }

    private QuadTreeNode getChild(Quadrant quadrant) {
        if (subdivided) {
            switch (quadrant) {
            case NN:
                return nn;
            case NP:
                return np;
            case PN:
                return pn;
            case PP:
                return pp;
            default:
                return null;
            }
        } else {
            return this;
        }
    }
}
