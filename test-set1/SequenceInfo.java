
public class SequenceInfo {
    private int startInd;
    private int length;
    private String hashedFilePath;

    public SequenceInfo(int startInd, int length, String hashedFilePath){
        this.startInd = startInd;
        this.length = length;
        this.hashedFilePath = hashedFilePath;
    }

    public List<Integer> getHashedSequence() throws FileNotFoundException {
        List<Integer> hashedSequence = new ArrayList<>();

        int ind = 0;
        Scanner sc = new Scanner(new File(hashedFilePath));
        while(sc.hasNextLine()){
            String hashedToken = sc.nextLine();

            if(ind < getStartInd()) {
                ind++;
                continue;
            }
            if(ind >= getStartInd() + getLength())
                break;

            hashedSequence.add(Integer.parseInt(hashedToken));

            ind++;
        }
        return hashedSequence;
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

}
