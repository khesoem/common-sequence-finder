
public class SourceComparatorHelper {
    private static final int MAX_COMPUTATION_STEpS = 10000000;
    private static SourceComparatorHelper _instance;

    public static SourceComparatorHelper getInstance() {
        if (_instance == null)
            _instance = new SourceComparatorHelper();
        return _instance;
    }

    public List<SequenceInfo> getLongestCommonSequences
            (
                    HashedSourceInfo hashedSourceInfo1,
                    HashedSourceInfo hashedSourceInfo2
            ) {
        return getLongestCommonSequencesStrategy1(hashedSourceInfo1, hashedSourceInfo2);

//        if (hashedSourceInfo1.getTokenIds().size() * hashedSourceInfo2.getTokenIds().size() < MAX_COMPUTATION_STEpS) {
//            return getLongestCommonSequencesStrategy1(hashedSourceInfo1, hashedSourceInfo2);
//        } else {
//            return getLongestCommonSequencesStrategy2(hashedSourceInfo1, hashedSourceInfo2);
//        }
    }

    /* this strategy uses dynamic programming to find the longest common sequences */
    private List<SequenceInfo> getLongestCommonSequencesStrategy1
    (
            HashedSourceInfo hsi1,
            HashedSourceInfo hsi2
    ) {
        List<SequenceInfo> result = new ArrayList<>();

        ArrayList<Integer> tokens1 = hsi1.getTokenIds(), tokens2 = hsi2.getTokenIds();
        int lengthOfLongest = 0, m = tokens1.size(), n = tokens2.size(), currRow = 0;
        int[][] lcs = // longestCommonSequenceEndingHere
                new int[2][n + 1];
        // init
        for (int i = 0; i < n; i++)
            lcs[currRow][i] = 0;

        currRow = 1 - currRow; // changing row

        for (int i = 0; i <= m; i++) {
            int oldRow = 1 - currRow;
            for (int j = 0; j <= n; j++) {
                if (i == 0 || j == 0) {
                    lcs[currRow][j] = 0;
                } else if (tokens1.get(i - 1).equals(tokens2.get(j - 1))) {
                    lcs[currRow][j] = lcs[oldRow][j - 1] + 1;
                    if (lcs[currRow][j] > lengthOfLongest) {
                        lengthOfLongest = lcs[currRow][j];
                        result = new ArrayList<>();
                        result.add(new SequenceInfo(i - lengthOfLongest, lengthOfLongest, hsi1.getHashedFilePath()));
                    } else if (lcs[currRow][j] == lengthOfLongest) {
                        result.add(new SequenceInfo(i - lengthOfLongest, lengthOfLongest, hsi1.getHashedFilePath()));
                    }
                } else {
                    lcs[currRow][j] = 0;
                }
            }
            currRow = 1 - currRow;
        }

        return result;
    }

    /* this strategy uses Generalized Suffix Tree to find the longest common sequences */
    private List<SequenceInfo> getLongestCommonSequencesStrategy2
    (
            HashedSourceInfo hashedSourceInfo1,
            HashedSourceInfo hashedSourceInfo2
    ) {
        return null;
    }
}
