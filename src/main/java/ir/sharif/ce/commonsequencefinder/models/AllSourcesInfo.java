package ir.sharif.ce.commonsequencefinder.models;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by khesoem on 9/9/2018.
 */
public class AllSourcesInfo {
    public static final String HASHED_KEYWORD = "HASHED";
    public static final int MAX_NUMBER_OF_FILES = 200;
    private static AllSourcesInfo _instance;

    public static AllSourcesInfo getInstance(String sourcesListPath) throws IOException {
        if (_instance == null)
            return new AllSourcesInfo(sourcesListPath);
        return _instance;
    }

    private List<String> hashedSourcePaths;
    private Map<String, Integer> tokenToInt;
    private ArrayList<String> allTokens;

    public AllSourcesInfo(String sourceListPath) throws IOException {
        init(sourceListPath);
    }

    private void init(String sourceListPath) throws IOException {
        hashedSourcePaths = new ArrayList<>();
        tokenToInt = new HashMap<>();
        allTokens = new ArrayList<>();

        Scanner sc = new Scanner(sourceListPath);
        while (sc.hasNextLine()) {
            String path = sc.nextLine();
            addFile(path);
        }
        sc.close();
    }

    private void addFile(String path) throws IOException {
        File file = new File(path);

        File hashedFile = new File(path + HASHED_KEYWORD);
        hashedFile.createNewFile();
        hashedSourcePaths.add(path + HASHED_KEYWORD);
        PrintWriter hashedPw = new PrintWriter(hashedFile);

        List<String> tokens = new ArrayList<>();
        CompilationUnit compilationUnit = JavaParser.parse(file);
        compilationUnit.getTokenRange().get()
                .forEach(t -> tokens.add(t.getText()));
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            int tokenId;
            if(tokenToInt.containsKey(token)){
                tokenId = tokenToInt.get(token);
            }else{
                tokenId = allTokens.size();
                allTokens.add(token);
                tokenToInt.put(token, tokenId);
            }
            hashedPw.println(tokenId);
        }

        hashedPw.close();
    }

    public ArrayList<SequenceInfo> getLongestCommonSequences(){

    }
}
