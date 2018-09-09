package ir.sharif.ce.commonsequencefinder;

import ir.sharif.ce.commonsequencefinder.models.AllSourcesInfo;
import ir.sharif.ce.commonsequencefinder.models.CommonSequenceInfo;
import ir.sharif.ce.commonsequencefinder.models.SequenceInfo;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Created by khesoem on 9/9/2018.
 */
public class CommonSequenceFinderMain {

    /* args[0] = inputPath, args[1] = outputPath*/
    public static void main(String[] args) throws IOException {
        String inputPath = args[0], outputPath = args[1];
        File outputFile = new File(outputPath);
        outputFile.createNewFile();
        PrintWriter outputPw = new PrintWriter(outputFile);

        List<CommonSequenceInfo> sortedDistinctLongestCommonSequences =
                AllSourcesInfo.getInstance(inputPath).getSortedDistinctLongestCommonSequences();
        for(int i = 0; i < sortedDistinctLongestCommonSequences.size(); i++){
            CommonSequenceInfo sequenceInfo = sortedDistinctLongestCommonSequences.get(i);
            outputPw.println(sequenceInfo.getScore() + ", " + sequenceInfo.getHashedSequence().size() + ", "
                    + sequenceInfo.getCount() + ", " +
                    AllSourcesInfo.getInstance(inputPath).getSourceCode(sequenceInfo));
        }
        outputPw.close();
    }
}
