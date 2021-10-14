import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.io.IOException;
import java.io.File;


public class Outer {

    public static boolean isValidPath(String path) {
        try {
            Paths.get(path);
        } catch (InvalidPathException | NullPointerException ex) {
            return false;
        }
        return true;
    }


    public static String stringDateTimeAdd(String baseString){
        /*
         * the function returns the String in the format: baseString_yyyyMMdd_HHmm
         */
        SimpleDateFormat formatter= new SimpleDateFormat("yyyyMMdd'_'HHmm");
        Date date = new Date(System.currentTimeMillis());
        return (baseString + "_" + formatter.format(date));
    }


    public static void createFile(File file){
        try {
            // Create the empty file with default permissions, etc.
            Path dir = Paths.get(IndexDataCollector.path);
            if (!Files.isDirectory(dir)){
                Files.createDirectories(file.toPath().getParent());
            }
            Files.createFile(file.toPath());
        } catch (FileAlreadyExistsException x) {
            System.err.format("file %s" +
                    " already exists%n", file);
        } catch (IOException x) {
            // Some other sort of failure, such as permissions.
            System.err.format("createFile error: %s%n", x);
        }
    }


    public static ArrayList<String[]> getConfigFromFile (File fileName){
        ArrayList<String[]> configurations = new ArrayList<>();
        try{
            BufferedReader inFile = new BufferedReader(new FileReader(fileName));
            while (inFile.ready()){
                String[] line = inFile.readLine().split("=");
                if (line[0].startsWith("#") || line.length != 2){
                    continue;
                }
                    configurations.add(line);
            }
            inFile.close();
        }
        catch (FileNotFoundException e){
            System.out.printf("File Not Found:The file [%s] not found\n", fileName);
        }
        catch (IOException e){
            System.out.println("An exception occurred while reading the configuration file.");
            e.printStackTrace();
        }
        return configurations;
    }


    public static void appendLineToFile(File fileName, String str){
        try {
            // Open given file in append mode.
            BufferedWriter out = new BufferedWriter(
                    new FileWriter(fileName, true));
            out.write(str);
            out.newLine();
            out.close();
        }
        catch (IOException e) {
            System.out.println("exception occurred while append data to the file: " + e);
        }
    }


    public static void copyFile(File sourceFile, File destinationFile) throws IOException {

            Files.copy(sourceFile.toPath(), destinationFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
    }
}
