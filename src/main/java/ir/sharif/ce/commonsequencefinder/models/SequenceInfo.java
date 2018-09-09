package ir.sharif.ce.commonsequencefinder.models;

/**
 * Created by khesoem on 9/9/2018.
 */
public class SequenceInfo {
    private int startInd;
    private int length;
    private String hashedFilePath;
    private int count;
    private double score;

    public SequenceInfo(int startInd, int length, String hashedFilePath, int count){
        this.startInd = startInd;
        this.length = length;
        this.hashedFilePath = hashedFilePath;
        this.count = count;
        this.score = (Math.log(length) / Math.log(2)) * (Math.log(count) / Math.log(2));
    }

    public String getSourceCode(){
        return null;
    }

    public int getStartInd() {
        return startInd;
    }

    public void setStartInd(int startInd) {
        this.startInd = startInd;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getHashedFilePath() {
        return hashedFilePath;
    }

    public void setHashedFilePath(String hashedFilePath) {
        this.hashedFilePath = hashedFilePath;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
