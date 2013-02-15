package nl.esciencecenter.visualization.amuse.planetformation.data;

import nl.esciencecenter.visualization.amuse.planetformation.interfaces.SceneDescription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmuseSceneDescription implements SceneDescription {
    private final static Logger logger = LoggerFactory
                                               .getLogger(AmuseSceneDescription.class);

    private int                 frameNumber;
    private String              colorMap;
    private float               lowerBound;
    private float               upperBound;
    private int                 levelOfDetail;

    public AmuseSceneDescription(int frameNumber, int levelOfDetail,
            String colorMap, float lowerBound, float upperBound) {
        this.frameNumber = frameNumber;
        this.levelOfDetail = levelOfDetail;
        this.colorMap = colorMap;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    @Override
    public int hashCode() {
        int frameNumberPrime = (frameNumber + 131) * 1543;
        int lodPrime = (frameNumber + 439) * 1847;
        int colorMapPrime = (colorMap.hashCode() + 919) * 7883;
        int lowerBoundPrime = (int) ((lowerBound + 41) * 1543);
        int upperBoundPrime = (int) ((upperBound + 67) * 2957);

        int hashCode = frameNumberPrime + lodPrime + colorMapPrime
                + lowerBoundPrime + upperBoundPrime;

        return hashCode;
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject)
            return true;
        if (!(thatObject instanceof AmuseSceneDescription))
            return false;

        // cast to native object is now safe
        AmuseSceneDescription that = (AmuseSceneDescription) thatObject;

        // now a proper field-by-field evaluation can be made
        return (frameNumber == that.frameNumber
                && levelOfDetail == that.levelOfDetail
                && lowerBound == that.lowerBound
                && upperBound == that.upperBound && colorMap
                    .compareTo(that.colorMap) == 0);
    }

    public static Logger getLogger() {
        return logger;
    }

    public int getFrameNumber() {
        return frameNumber;
    }

    public String getColorMap() {
        return colorMap;
    }

    @Override
    public float getLowerBound() {
        return lowerBound;
    }

    @Override
    public float getUpperBound() {
        return upperBound;
    }

    @Override
    public AmuseSceneDescription clone() {
        return new AmuseSceneDescription(frameNumber, levelOfDetail, colorMap,
                lowerBound, upperBound);
    }

    public void setFrameNumber(int frameNumber) {
        this.frameNumber = frameNumber;
    }

    public void setColorMap(String colorMap) {
        this.colorMap = colorMap;
    }

    public void setLowerBound(float lowerBound) {
        this.lowerBound = lowerBound;
    }

    public void setUpperBound(float upperBound) {
        this.upperBound = upperBound;
    }

    public int getLevelOfDetail() {
        return levelOfDetail;
    }

    public void setLevelOfDetail(int value) {
        levelOfDetail = value;
    }
}
