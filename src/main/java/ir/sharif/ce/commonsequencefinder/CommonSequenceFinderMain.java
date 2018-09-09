package ir.sharif.ce.commonsequencefinder;

import ir.sharif.ce.commonsequencefinder.models.AllSourcesInfo;
import ir.sharif.ce.commonsequencefinder.models.CommonSequenceInfo;
import ir.sharif.ce.commonsequencefinder.models.SequenceInfo;

import java.io.*;
import java.util.List;

/**
 * Created by khesoem on 9/9/2018.
 */
public class CommonSequenceFinderMain {
    private static final String GENERATED_SET_PATH = "generated-set";

    /*  args[0] = inputPath, args[1] = outputPath,
        args[2] = to use input generation (0 or 1), args[3] = inputFolder (if args[2]=1) */
    public static void main(String[] args) throws IOException {
        args = new String[]{"input.txt", "output.txt", "1", "D:\\Daneshgah\\thesis\\paper\\line-frequency-coverage\\gitproj\\project-analysis\\math\\src\\org\\apache\\commons\\math3\\geometry"};
        if (args[2].equals("1")) {
            String inputPath = args[0];
            File inputFile = new File(inputPath);
            inputFile.createNewFile();
            PrintWriter inputPw = new PrintWriter(inputFile);
            writeJavaFilePaths(inputPw, args[3]);
            inputPw.close();
        }
        String inputPath = args[0], outputPath = args[1];
        File outputFile = new File(outputPath);
        outputFile.createNewFile();
        PrintWriter outputPw = new PrintWriter(outputFile);

        List<CommonSequenceInfo> sortedDistinctLongestCommonSequences =
                AllSourcesInfo.getInstance(inputPath).getSortedDistinctLongestCommonSequences();
        for (int i = 0; i < sortedDistinctLongestCommonSequences.size(); i++) {
            CommonSequenceInfo sequenceInfo = sortedDistinctLongestCommonSequences.get(i);
            String sourceCode = AllSourcesInfo.getInstance(inputPath).getSourceCode(sequenceInfo);
            outputPw.println(sequenceInfo.getScore() + ", " + sequenceInfo.getHashedSequence().size() + ", "
                    + sequenceInfo.getCount() + ", " + sourceCode);
        }
        outputPw.flush();
        outputPw.close();
    }

    private static void writeJavaFilePaths(PrintWriter inputPw, String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.isDirectory()) {
            addJavaFileToGeneratedSet(inputPw, file);
        } else {
            File[] containedFiles = file.listFiles();
            for (File containedFile : containedFiles) {
                if (containedFile.isDirectory())
                    writeJavaFilePaths(inputPw, containedFile.getPath());
                else {
                    addJavaFileToGeneratedSet(inputPw, containedFile);
                }
            }
        }
    }

    private static void addJavaFileToGeneratedSet(PrintWriter inputPw, File file) throws IOException {
        if (file.getName().endsWith("java")) {
            File newFile = new File(GENERATED_SET_PATH + File.separator + file.getName());
            copyFileUsingStream(file, newFile);
            inputPw.println(newFile.getPath());
            inputPw.flush();
        }
    }

    private static void copyFileUsingStream(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }

}

