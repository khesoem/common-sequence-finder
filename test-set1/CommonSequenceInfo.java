
public class CommonSequenceInfo {
    private int count;
    private double score;
    private List<Integer> hashedSequence;

    public CommonSequenceInfo(List<Integer> hashedSequence, int count){
        this.hashedSequence = hashedSequence;
        this.count = count;
        this.score = (Math.log(hashedSequence.size()) / Math.log(2)) * (Math.log(count) / Math.log(2));
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

    public List<Integer> getHashedSequence() {
        return hashedSequence;
    }

    public void setHashedSequence(List<Integer> hashedSequence) {
        this.hashedSequence = hashedSequence;
    }
}
