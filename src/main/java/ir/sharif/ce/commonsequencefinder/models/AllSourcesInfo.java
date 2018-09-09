package ir.sharif.ce.commonsequencefinder.models;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import ir.sharif.ce.commonsequencefinder.utils.SourceComparatorHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by khesoem on 9/9/2018.
 */
public class AllSourcesInfo {
    public static final String HASHED_KEYWORD = "HASHED";
    private static AllSourcesInfo _instance;

    public static AllSourcesInfo getInstance(String sourcesListPath) throws IOException {
        if (_instance == null)
            _instance = new AllSourcesInfo(sourcesListPath);
        return _instance;
    }

    private List<String> hashedSourcePaths;
    private List<HashedSourceInfo> hashedSourceInfoList;
    private Map<String, Integer> tokenToInt;
    private ArrayList<String> allTokens;

    public AllSourcesInfo(String sourceListPath) throws IOException {
        init(sourceListPath);
    }

    private void init(String sourceListPath) throws IOException {
        hashedSourcePaths = new ArrayList<>();
        hashedSourceInfoList = new ArrayList<>();
        tokenToInt = new HashMap<>();
        allTokens = new ArrayList<>();

        Scanner sc = new Scanner(new File(sourceListPath));
        while (sc.hasNextLine()) {
            String path = sc.nextLine();
            addFile(path);
        }
        sc.close();
    }

    private void addFile(String path) throws IOException {
        File file = new File(path);
        String hashedSourcePath = path + HASHED_KEYWORD;

        File hashedFile = new File(hashedSourcePath);
        hashedFile.createNewFile();
        hashedSourcePaths.add(hashedSourcePath);
        PrintWriter hashedPw = new PrintWriter(hashedFile);

        List<String> tokens = new ArrayList<>();
        CompilationUnit compilationUnit = JavaParser.parse(file);
        compilationUnit.getTokenRange().get()
                .forEach(t -> tokens.add(t.getText()));
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            token = token.replace("\n", "");
            token = token.replace("\r", "");
            if(isStringWhiteSpace(token))
                continue;
            int tokenId;
            if (tokenToInt.containsKey(token)) {
                tokenId = tokenToInt.get(token);
            } else {
                tokenId = allTokens.size();
                allTokens.add(token);
                tokenToInt.put(token, tokenId);
            }
            hashedPw.println(tokenId);
        }

        hashedPw.close();

        hashedSourceInfoList.add(new HashedSourceInfo(hashedSourcePath));
    }

    public static boolean isStringWhiteSpace(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public String getSourceCode(CommonSequenceInfo sequenceInfo) {
        String sourceCode = "";

        for (int i = 0; i < sequenceInfo.getHashedSequence().size(); i++) {
            Integer hashedToken = sequenceInfo.getHashedSequence().get(i);
            String token = allTokens.get(hashedToken);
            sourceCode += (sourceCode.length() == 0 ? "" : ":::") + token;
        }
        return sourceCode;
    }

    public List<CommonSequenceInfo> getSortedDistinctLongestCommonSequences() throws FileNotFoundException {
        List<SequenceInfo> allLongestCommonSequences =
                new ArrayList<>();

        /*  finding longest common sequences among all files
            by checking longest common sequences between each pair of files */
        for (int i = 0; i < hashedSourcePaths.size(); i++) {
            HashedSourceInfo hashedSourceInfo1 = hashedSourceInfoList.get(i);
            for (int j = i + 1; j < hashedSourcePaths.size(); j++) {
                HashedSourceInfo hashedSourceInfo2 = hashedSourceInfoList.get(j);
                List<SequenceInfo> longestCommonSequences =
                        SourceComparatorHelper.getInstance()
                                .getLongestCommonSequences
                                        (
                                                hashedSourceInfo1,
                                                hashedSourceInfo2
                                        );
                if (longestCommonSequences != null && longestCommonSequences.size() != 0) {
                    if (allLongestCommonSequences == null
                            || allLongestCommonSequences.size() == 0
                            || allLongestCommonSequences.get(0).getLength() < longestCommonSequences.get(0).getLength()) {
                        allLongestCommonSequences = longestCommonSequences;
                    } else if (allLongestCommonSequences.get(0).getLength() == longestCommonSequences.get(0).getLength()) {
                        allLongestCommonSequences.addAll(longestCommonSequences);
                    }
                }
            }
        }

        List<CommonSequenceInfo> commonSequenceInfoList = new ArrayList<>();

        // finding distinct longest common sequences and their "count"
        Set<Integer> checkedSequences = new HashSet<>();
        for (SequenceInfo sequenceInfo : allLongestCommonSequences) {
            List<Integer> hashedSequenceLst = sequenceInfo.getHashedSequence();
            if (checkedSequences.contains(hashedSequenceLst.hashCode()))
                continue;
            int count = 0;
            for (int i = 0; i < hashedSourcePaths.size(); i++) {
                if(hashedSourceInfoList.get(i).contains(hashedSequenceLst)){
                    count++;
                }
            }
            commonSequenceInfoList.add(new CommonSequenceInfo(hashedSequenceLst, count));
            checkedSequences.add(hashedSequenceLst.hashCode());
        }

        // sorting common-sequence-lst due to sequence scores (a simple bubble sort!)
        for(int i = 0; i < commonSequenceInfoList.size(); i++){
            for(int j = 0; j < commonSequenceInfoList.size() - i - 1; j++){
                if(commonSequenceInfoList.get(j).getScore() < commonSequenceInfoList.get(j + 1).getScore()){
                    Collections.swap(commonSequenceInfoList, j, j + 1);
                }
            }
        }

        return commonSequenceInfoList;
    }

}
