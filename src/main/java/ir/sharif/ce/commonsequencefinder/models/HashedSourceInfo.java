package ir.sharif.ce.commonsequencefinder.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by khesoem on 9/9/2018.
 */
public class HashedSourceInfo {
    private ArrayList<Integer> tokenIds;
    private String hashedFilePath;

    /*  every time information is read from the file
        since keeping it in memory may cause the program to exceed the heap size */
    public HashedSourceInfo(String hashedFilePath) {
        this.hashedFilePath = hashedFilePath;

        Scanner sc = new Scanner(hashedFilePath);
        while (sc.hasNextLine()) {
            tokenIds.add(Integer.parseInt(sc.next()));
        }
        sc.close();
    }

    public ArrayList<Integer> getTokenIds() {
        return tokenIds;
    }

    public void setTokenIds(ArrayList<Integer> tokenIds) {
        this.tokenIds = tokenIds;
    }

    public String getHashedFilePath() {
        return hashedFilePath;
    }

    public void setHashedFilePath(String hashedFilePath) {
        this.hashedFilePath = hashedFilePath;
    }

    public boolean contains(List<Integer> hashedSequenceLst) {
        if (hashedSequenceLst.size() > tokenIds.size())
            return false;
        int seqInd = 0;
        for (int i = 0; i < tokenIds.size(); i++) {
            if (hashedSequenceLst.get(seqInd).equals(tokenIds.get(i))) {
                seqInd++;
            } else if (hashedSequenceLst.get(0).equals(tokenIds.get(i))) {
                seqInd = 1;
            } else
                seqInd = 0;
            if(seqInd == hashedSequenceLst.size())
                return true;
        }
        return false;
    }
}
