package ir.sharif.ce.commonsequencefinder.utils;

import ir.sharif.ce.commonsequencefinder.models.HashedSourceInfo;
import ir.sharif.ce.commonsequencefinder.models.SequenceInfo;

import java.util.List;

/**
 * Created by khesoem on 9/9/2018.
 */
public class SourceComparatorHelper {
    private static final int MAX_ARRAY_SIZE = 10000000;
    private static SourceComparatorHelper _instance;
    public static SourceComparatorHelper getInstance(){
        if(_instance == null)
            _instance = new SourceComparatorHelper();
        return _instance;
    }

    public List<SequenceInfo> getLongestCommonSequences
            (
                    HashedSourceInfo hashedSourceInfo1,
                    HashedSourceInfo hashedSourceInfo2
            ){
        if(hashedSourceInfo1.getTokenIds().size() * hashedSourceInfo2.getTokenIds().size() < MAX_ARRAY_SIZE){
            return getLongestCommonSequencesStrategy1(hashedSourceInfo1, hashedSourceInfo2);
        }else{
            return getLongestCommonSequencesStrategy2(hashedSourceInfo1, hashedSourceInfo2);
        }
    }

    /* this strategy uses dynamic programming to find the longest common sequences */
    private List<SequenceInfo> getLongestCommonSequencesStrategy1
            (
                    HashedSourceInfo hashedSourceInfo1,
                    HashedSourceInfo hashedSourceInfo2
            ) {
        return null;
    }

    /* this strategy uses binary-search to find the longest common sequences */
    private List<SequenceInfo> getLongestCommonSequencesStrategy2
            (
                    HashedSourceInfo hashedSourceInfo1,
                    HashedSourceInfo hashedSourceInfo2
            ) {
        return null;
    }
}
