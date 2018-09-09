package ir.sharif.ce.commonsequencefinder.utils;

import ir.sharif.ce.commonsequencefinder.models.HashedSourceInfo;
import ir.sharif.ce.commonsequencefinder.models.SequenceInfo;

import java.util.List;

/**
 * Created by khesoem on 9/9/2018.
 */
public class SourceComparatorHelper {
    private static SourceComparatorHelper _instance;
    public SourceComparatorHelper getInstance(){
        if(_instance == null)
            _instance = new SourceComparatorHelper();
        return _instance;
    }

    public List<SequenceInfo> getLongestCommonSequences
            (
                    HashedSourceInfo hashedSourceInfo1,
                    HashedSourceInfo hashedSourceInfo2
            ){
        
    }
}
