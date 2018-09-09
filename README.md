# common-sequence-finder
You can use this project to find the longest common subsequences among a set of files.
## How to use it?
To use this project, you should run "Common.Sequence.Finder-1.0-SNAPSHOT-jar-with-dependencies.jar" file (in the root folder of this project). This jar file takes 4 input arguments and uses Java 8.
```
java -jar Common.Sequence.Finder-1.0-SNAPSHOT-jar-with-dependencies.jar input.txt output.txt 1 directory
```
The first argument (input.txt) is the path of a input file. Each line of the input file is the path of a java file. For example:
```
test-folder\JavaClass1.java
test-folder\JavaClass2.java
```
The second argument (output.txt) is the path of the output file. For example it can be "output.txt".

The third argument should be 0 or 1. If you set this arguemt to 0, the program uses input file and generates the output file.
On the other hand if you set this argument to 1, the program will find all the java files in the directory the path of which is passed as the
fourth argument.

If the third arguemnt is 0, the fourth arguemnt can be any string (you can simply pass "tmp"). If the third argument is 1,
the program will find all the java files in the directory whose path is equal to fourth argument, copy all of them to a new directory 
(generated-set), generate input file, and print the result in the output file.
## How to change and compile it?
To compile this project, you need maven on your computer. You can change source files in the "src" folder and once your new versoin is ready
you should run "mvn package" in the root folder of the project (where the pom.xml is placed).

A new "Common.Sequence.Finder-1.0-SNAPSHOT-jar-with-dependencies.jar" will be generated and can be accessed in the target folder.
## Sample test-sets
This project has been tested on three test sets that are available in "test-set1", "test-set2", and "test-set3" folders. In each folder,
java files are the source files and the output-set(i).txt file is the result file.

You can run jar file on any of these test-sets. For example, if you are on the root folder of the project, you can use the following commands
to run the jar file on "test-set1" and see the results:
```
mkdir test
cd test
cp ../Common.Sequence.Finder-1.0-SNAPSHOT-jar-with-dependencies.jar ./
java -jar Common.Sequence.Finder-1.0-SNAPSHOT-jar-with-dependencies.jar input.txt output.txt 1 ../test-set1
vim output.txt
```
## Clarifications
1- This project uses https://github.com/javaparser/javaparser to parse source files and find tokens.
 Therefore, your source-files must be parseable 
(should not have compile errors).

2- Comments, imports, and package names are considered as tokens. Whitespaces and endline characters are ignored.

3- It is assumed that the COUNT of a sequence of tokens is the number of source files that contain that sequence.

4- A common sequence is a sequence whose COUNT is more than one.

5- Each row of output file has the format of: "score, length, count, sequence".

6- All the common sequences that have the maximum number of tokens are reported in the output file, so the second column 
is the same for all of the rows in the output file.

7- Rows of output file are sorted due to their first column which indicates the score of the related sequence.

8- Tokens of the sequence in each row of the output file are separated by ":::". Note that some sequences contain "," character, so
if you open output file with Microsoft Excel, it may show more than 4 columns for each row.

9- It is assumed that we can keep the content of all the source files in the memory.

10- This programs runs on the test-set2 (which contains 69 java files) in less than 30 seconds. It is assumed that 
dynamic programming is quick enough for this project. However, we can use Generalized Suffix Tree instead of dynamic algorithms
 to make it quicker.