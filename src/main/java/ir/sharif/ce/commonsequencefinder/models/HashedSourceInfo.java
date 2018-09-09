package ir.sharif.ce.commonsequencefinder.models;

import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by khesoem on 9/9/2018.
 */
public class HashedSourceInfo {
    private ArrayList<Integer> tokenIds;
    private String hashedFilePath;

    public HashedSourceInfo(String hashedFilePath){
        this.hashedFilePath = hashedFilePath;

        Scanner sc = new Scanner(hashedFilePath);
        while(sc.hasNextLine()){
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
}
