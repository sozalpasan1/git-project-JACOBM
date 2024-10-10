import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
//added a sigma comment
public class Git implements GitInterface{
    public static void main (String [] args) throws IOException
    {
        Git repo = new Git();
        //final boolean COMPRESS = false;
        initializesGitRepo();

        File testFile = new File("test.txt");
        checkForAndDelete(testFile);
        testFile.createNewFile();
        File testDir = new File("testDir");
        checkForAndDelete(testDir);
        testDir.mkdir();
        File dirInsideDir = new File("testDir/dirInsideDir");
        checkForAndDelete(dirInsideDir);
        dirInsideDir.mkdir();
        File testFileInDir = new File("testDir/test2.txt");
        checkForAndDelete(testFileInDir);
        testFileInDir.createNewFile();
        File lastFile = new File("testDir/dirInsideDir/theLastFile.txt");
        checkForAndDelete(lastFile);
        lastFile.createNewFile();

        FileWriter writer = new FileWriter("test.txt");
        writer.write("this is the first test");
        writer.close();
        FileWriter writerInDir = new FileWriter("testDir/test2.txt");
        writerInDir.write("this is the second test");
        writerInDir.close();
        FileWriter lastWriter = new FileWriter("testDir/dirInsideDir/theLastFile.txt");
        lastWriter.write("hopefully this works");
        lastWriter.close();
        
        // createBlob("testDir", COMPRESS);
        // createBlob("test.txt", COMPRESS);
        repo.stage("testDir");
        //add scanner for author and message before doing commit
        
       
        // System.out.println(formattedDate);


    }

    private static File gitDirectory = new File("git");
    private static String hash;

    public void stage(String filePath){
        try{
            createBlob(filePath, false);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public String commit(String author, String message){
        StringBuilder commitFileContents = new StringBuilder();
        

        
        
        try (BufferedReader reader = new BufferedReader(new FileReader("git/HEAD"))) {
            String line;
            if((line = reader.readLine()) != null) {
                commitFileContents.append("parent: " + line);
                commitFileContents.append("\n");
                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //add author
        commitFileContents.append("author: " + author);
        commitFileContents.append("\n");

        //add date
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
        String formattedDate = currentDate.format(formatter);
        commitFileContents.append("date: " + formattedDate);
        commitFileContents.append("\n");

        //add message
        commitFileContents.append("message: " + message);

        //hash everything we just did, make the commitFile, and write in the commitFile
        String hashOfCommitFile = encryptThisString(commitFileContents.toString());
        File commitFile = new File("git/objects/" + hashOfCommitFile);
        try (FileWriter writer = new FileWriter(commitFile, false)){
            writer.write(commitFileContents.toString());
            writer.close();
        } catch (IOException e){
            e.printStackTrace();
        }

        //update head to have the correct thing
        try (FileWriter writer = new FileWriter("git/HEAD", false)){
            writer.write(hashOfCommitFile);
            writer.close();
        } catch (IOException e){
            e.printStackTrace();
        }


        return hashOfCommitFile;
    }

    public void checkout(String commitHash){
        
    }
    
    public static void initializesGitRepo ()
    {
        //create gitDirectory
        
        if (!gitDirectory.exists()) 
        {
            gitDirectory.mkdir();
            File objects = new File("git/objects");
            File index = new File("git/index");
            objects.mkdir();
            final File HEAD = new File("git/HEAD");
            try 
            {
                index.createNewFile();
                HEAD.createNewFile();
            } catch (IOException e) 
            {
                e.printStackTrace();
            }
        }
        else
        {
            //create objects inside gitDirectory
            File objects = new File("git/objects");
            boolean objectsExists = objects.exists();
            if (!objectsExists)
            {
                objects.mkdir();
            }
            File index = new File("git/index"); //create index inside gitDirectory
            if(!index.exists())
            {
                try 
                {
                    index.createNewFile();
                } catch (IOException e) 
                {
                    e.printStackTrace();
                }
            }
            else if (objectsExists)
            {
                System.out.println ("Git Repository already exists");
            }
        }
    }
    
    private static void checkForAndDelete(File file)
    {
        if (file.isDirectory())
        {
            File[] files = file.listFiles();
            for (File f : files)
            {
                checkForAndDelete(f);
                f.delete();
            }
        }
        else{
            file.delete();
        }
    }
    
    private void createBlob(String pathName, boolean compressed) throws IOException
    {
        File file = new File(pathName);
        if(file.isDirectory()){
            createTree(pathName, compressed);
        } else {
            putFileToObjectsFolder(pathName, compressed);
            putFileToIndex(pathName, compressed);
        }
        

    }

    private String createTree(String pathName, boolean compressed) throws IOException
    {
        File dir = new File(pathName);
        if(dir.exists())
        {
            File[] contents = dir.listFiles();
            putFileToObjectsFolder(pathName, compressed);
            if(contents.length != 0)
            {
                    for(File f : contents)
                    {
                        if(f.getName().charAt(0) != '.')
                        {
                            if(f.isFile())
                            {
                                createBlob(pathName + "/" + f.getName(), compressed);
                            }
                            else if(f.isDirectory())
                            {
                                createTree(pathName + "/" + f.getName(), compressed);
                            }
                        }
                    }
            }
            putFileToIndex(pathName, compressed);
        }
        else{
            throw new FileNotFoundException("Directory named " + pathName + " not found.");
        }
        return getHash(pathName, compressed);
    }

    private static String getHash(String pathName, boolean compressed) throws IOException
    {
        File regularFile = new File (pathName);
        byte[] fileContentInBytes = {};
        if(regularFile.isFile())
        {
            fileContentInBytes = Files.readAllBytes(regularFile.toPath());
        }
        else if(regularFile.isDirectory()){
            File[] contents = regularFile.listFiles();
            String tree = "";
            if(contents.length != 0)
            {
                for(File f : contents)
                {
                    if(f.getName().charAt(0) != '.')
                    {
                        tree += getIndexText(getHash(f.getPath(), compressed), f.getPath());
                    }
                }
                tree = tree.substring(0, tree.length() - 1);
            }
            fileContentInBytes = tree.getBytes();
        }
        byte [] hashData = null;
        if (compressed)
        {
            hashData = zipBytes(regularFile.getName(), fileContentInBytes);
        }
        else
        {
            hashData = fileContentInBytes;
        }
        //reads the contents of the file into the byte array and then  we can generate the hash by using our encryptString method
        hash = encryptThisString(new String (hashData));
        return hash;
    }

    public static String encryptThisString(String input) {
        try {
            // getInstance() method is called with algorithm SHA-1
            MessageDigest md = MessageDigest.getInstance("SHA-1");

            // digest() method is called
            // to calculate message digest of the input string
            // returned as array of byte
            byte[] messageDigest = md.digest(input.getBytes());

            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, messageDigest);

            // Convert message digest into hex value
            String hashtext = no.toString(16);

            // Add preceding 0s to make it 40 digits long
            while (hashtext.length() < 40) {
                hashtext = "0" + hashtext;
            }

            // return the HashText
            return hashtext;
        }

        // For specifying wrong message digest algorithms
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void putFileToObjectsFolder(String pathName, boolean compressed) throws IOException
    {
        File regularFile = new File (pathName);
        byte[] fileContentInBytes = {};
        if(regularFile.isFile())
        {
            fileContentInBytes = Files.readAllBytes(regularFile.toPath());
        }
        else if(regularFile.isDirectory()){
            File[] contents = regularFile.listFiles();
            String tree = "";
            if(contents.length != 0)
            {
                for(File f : contents)
                {
                    if(f.getName().charAt(0) != '.')
                    {
                        tree += getIndexText(getHash(f.getPath(), compressed), f.getPath());
                    }
                }
                tree = tree.substring(0, tree.length() - 1);
            }
            fileContentInBytes = tree.getBytes();
        }
        byte [] hashData = null;
        if (compressed)
        {
            hashData = zipBytes(regularFile.getName(), fileContentInBytes);
        }
        else
        {
            hashData = fileContentInBytes;
        }
        //reads the contents of the file into the byte array and then  we can generate the hash by using our encryptString method
        hash = encryptThisString(new String (hashData));
        File objectFile = new File ("git/objects/" + hash);
        //Writes out bytes better
        try (FileOutputStream writer = new FileOutputStream (objectFile))
        {
            writer.write(hashData);
        }
        catch (IOException e){
            e.printStackTrace();
        }
        // return hash;
    }

    public static void putFileToIndex(String pathName, boolean compressed) throws IOException
    {
        hash = getHash(pathName, compressed);
        File index = new File ("git/index");
        File file = new File(pathName);
        if(file.isFile()){
        //this append mode is used so that we can add to an existing file without altering the og data
            try (FileWriter writer = new FileWriter (index, true))
            {
            writer.write("blob " + hash + " " + pathName + "\n");
            } catch (IOException e){
                e.printStackTrace();
            }  
        }
        else{ //if its a directory
            try (FileWriter writer = new FileWriter (index, true))
            {
            writer.write("tree " + hash + " " + pathName + "\n");
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public static String getIndexText(String hash, String pathName)
    {
        File file = new File(pathName);
        String indexText = "";
        if(file.isFile()){
            indexText += "blob " + hash + " " + file.getPath() + "\n";
        }
        else{ //if its a directory
            indexText += "tree " + hash + " " + file.getPath() + "\n";
        }
        return indexText;
    }

    public static byte[] zipBytes(String filename, byte[] input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        ZipEntry entry = new ZipEntry(filename);
        entry.setSize(input.length);
        zos.putNextEntry(entry);
        zos.write(input);
        zos.closeEntry();
        zos.close();
        return baos.toByteArray();
    }

}