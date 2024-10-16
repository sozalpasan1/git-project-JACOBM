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
import java.util.ArrayList;
//added a sigma comment
public class Git implements GitInterface{
    public static String workingDirectoryName;
    public static void main (String [] args) throws IOException
    {
        File file = new File("doNotPayThisAnyAttention"); //added this so line saying not used goes away
        checkForAndDelete(file);
        
        /*
         * !!! SUPER IMPORTANT
         * to test:
         * 1) manually make a working directory, add whatever files or folders you want inside of it
         * 2) initialize git repo
         * 3) stage any file/folder you want. You can stage as many things as you want, then commit.
         * 3.5) when staging, the pathway is .stage(workingdirectoryName + "/__whateverfileorfolder__")
         * 4) commit --> committing twice will j make another commmit file thats tree is already existing
         * 
         * for testing checkout, i recommend making all the files you want, staging and commiting them then stopping there
         * then go to head, find the commit file and go as far back as you want to check out
         * then comment out all the file creation / staging / commit stuff, and only have 
         *                      repo.checkout(hash)
         * then you checkout and the working directory should be all good 
         * 
         */

        
        workingDirectoryName = "wordir"; //!!! create your working director and type its name here
        
        Git repo = new Git();

        repo.makeFiles();
        initializesGitRepo();

        repo.stage(workingDirectoryName + "/deez");
        repo.commit("sigma", "please");

        repo.stage(workingDirectoryName + "/testDir");
        repo.commit("no", "yes");

        repo.stage(workingDirectoryName + "/test.txt");
        repo.commit("sean", "test");


        //repo.checkout("");

        //repo.checkout("");
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
        
        //first get the tree line
        String treeHashLineForCommit = makeTreeHashLineForCommit();
        commitFileContents.append("tree: " + treeHashLineForCommit);
        commitFileContents.append("\n");

        //then get the parent line
        String parentLine = "";
        try(BufferedReader reader = new BufferedReader(new FileReader("git/HEAD"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                parentLine = line;
            }
            commitFileContents.append("parent: " + parentLine);
            commitFileContents.append("\n");
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

        //wipe index to get ready for next stage
        try (FileWriter writer = new FileWriter("git/index", false)){
            writer.close();
        } catch (IOException e){
            e.printStackTrace();
        }

        return hashOfCommitFile;
    }

    //this only works if ur checkingout backwards
    public void checkout(String commitHash) {
        //change head to be the parameter
        try (FileWriter writer = new FileWriter("git/HEAD", false)){
            writer.write(commitHash);
            writer.close();
        } catch (IOException e){
            e.printStackTrace();
        }
        //go into commitHash, get the tree hash
        String getTheTreeOfThatCommit = "";
        try(BufferedReader reader = new BufferedReader(new FileReader("git/objects/" + commitHash))) {
            getTheTreeOfThatCommit = reader.readLine();
            getTheTreeOfThatCommit = getTheTreeOfThatCommit.substring(6);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        //the chunk below goes into tree file and adds all file names to an arraylist
        ArrayList<String> filesInTree = new ArrayList<String>();
        try(BufferedReader reader = new BufferedReader(new FileReader("git/objects/" + getTheTreeOfThatCommit))) {
            String oneLineFromTreeFile;
            while((oneLineFromTreeFile = reader.readLine()) != null){
                //oneLineFromTreeFile = oneLineFromTreeFile.substring(46);
                System.out.println(oneLineFromTreeFile);
                filesInTree.add(oneLineFromTreeFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        File workDir = new File(workingDirectoryName);
        deleteEverything(workDir);

        //the below for loop checks if first thing in array is tree or file
        //if tree create the dir
        //if file, create file, copy paste contents from objects using the hash, then ur done
        //we utiilze the fact that tree files are always written after the blob files given a directory
        for(int i = filesInTree.size()-1; i>=0; i--){
            if(filesInTree.get(i).contains("tree")){
                String fileName =  filesInTree.get(i).substring(46);
                File tree = new File(fileName);
                tree.mkdirs();
            } else {
                String hashOfLine = filesInTree.get(i).substring(5,45);
                String nameOfFile = filesInTree.get(i).substring(46);
                File file = new File(nameOfFile);
                try (BufferedReader reader = new BufferedReader(new FileReader("git/objects/" + hashOfLine)); FileWriter writer = new FileWriter(file, false)){
                    file.createNewFile();
                    String line = "";
                    while((line = reader.readLine()) != null){
                        writer.write(line + "\n");
                    }
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    //deletes everything except parameter
    public void deleteEverything(File file){
        for(File childFile : file.listFiles()){
            if(childFile.isDirectory()){
                deleteEverything(childFile);
            }
            childFile.delete();
        }
    }

    //go to previous commit, get the tree hash, go to that file in objects, append everythign into treeHashLineForCommit, then append
    //whats currently in index, hash that, put all the junk into a file that has the name of the hash, then return the hash
    public String makeTreeHashLineForCommit(){
        //below gets us the hash of head file
        String parentLine = "";
        try(BufferedReader reader = new BufferedReader(new FileReader("git/HEAD"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                parentLine = line;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
       
        //BELOW
        //if statement --> if head NOT empty, get prev commits tree. if empty then j hash index
        StringBuilder treeHashLineForCommit = new StringBuilder();
        if(!parentLine.equals("")){
            //go parent commit, get contents of THAT tree, add the junk of index in, then make file with name of hashed everything.
            try(BufferedReader reader = new BufferedReader(new FileReader("git/objects/" + parentLine))) { //need to go into this ones tree
                String getTheTreeOfParent = "";
                getTheTreeOfParent = reader.readLine();
                getTheTreeOfParent = getTheTreeOfParent.substring(6);
                try(BufferedReader treeReader = new BufferedReader(new FileReader("git/objects/" + getTheTreeOfParent))){
                    String oneTreeOfParentLine;
                    while((oneTreeOfParentLine = treeReader.readLine()) != null) {
                        treeHashLineForCommit.append(oneTreeOfParentLine);
                        treeHashLineForCommit.append("\n");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try(BufferedReader reader = new BufferedReader(new FileReader("git/index"))) {
            String oneIndexLine;
            while((oneIndexLine = reader.readLine()) != null) {
                treeHashLineForCommit.append(oneIndexLine);
                treeHashLineForCommit.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //hashed the contents of index, put it in file with name of hash
        String thisIsTheTreeFileHash = encryptThisString(treeHashLineForCommit.toString());
        File treeFile = new File("git/objects/" + thisIsTheTreeFileHash);
        try (FileWriter writer = new FileWriter(treeFile, false)) {
            writer.write(treeHashLineForCommit.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return thisIsTheTreeFileHash;
    }
    
    public static void initializesGitRepo ()
    {
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

    private static void checkForAndDelete(File file){
        if(!file.exists()){
            return;
        }
        if (file.isDirectory()){
            File[] files = file.listFiles();
            for (File f : files){
                checkForAndDelete(f);
                f.delete();
            }
        } else {
            file.delete();
        }
    }
    
    private void createBlob(String pathName, boolean compressed) throws IOException
    {
        File file = new File(pathName);
        if(!file.exists()){
            throw new FileNotFoundException();
        }
        
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

    public void makeFiles(){
        try{
            File workDir = new File(workingDirectoryName);
            checkForAndDelete(workDir);
            workDir.mkdir();
            File testFile = new File(workingDirectoryName + "/test.txt");
            checkForAndDelete(testFile);
            testFile.createNewFile();
            File testDir = new File(workingDirectoryName + "/testDir");
            checkForAndDelete(testDir);
            testDir.mkdir();
            File dirInsideDir = new File(workingDirectoryName + "/testDir/dirInsideDir");
            checkForAndDelete(dirInsideDir);
            dirInsideDir.mkdir();
            File testFileInDir = new File(workingDirectoryName + "/testDir/test2.txt");
            checkForAndDelete(testFileInDir);
            testFileInDir.createNewFile();
            File lastFile = new File(workingDirectoryName + "/testDir/dirInsideDir/theLastFile.txt");
            checkForAndDelete(lastFile);
            lastFile.createNewFile();
            File deezDir = new File(workingDirectoryName + "/deez");
            checkForAndDelete(deezDir);
            deezDir.mkdir();
            File deezDirFile = new File(workingDirectoryName + "/deez/simga.txt");
            checkForAndDelete(deezDirFile);
            deezDir.createNewFile();
            File deezDirFile2 = new File(workingDirectoryName + "/deez/fortnite.txt");
            checkForAndDelete(deezDirFile2);
            deezDir.createNewFile();

            FileWriter writer = new FileWriter(workingDirectoryName + "/test.txt");
            writer.write("this is the first test");
            writer.close();
            FileWriter writerInDir = new FileWriter(workingDirectoryName + "/testDir/test2.txt");
            writerInDir.write("this is the second test");
            writerInDir.close();
            FileWriter lastWriter = new FileWriter(workingDirectoryName + "/testDir/dirInsideDir/theLastFile.txt");
            lastWriter.write("hopefully this works");
            lastWriter.close();

            FileWriter deezsimga = new FileWriter(workingDirectoryName + "/deez/simga.txt");
            deezsimga.write("deez sigma . fortnite");
            deezsimga.close();

            FileWriter hi = new FileWriter(workingDirectoryName + "/deez/fortnite.txt");
            hi.write("transformers is better than pirates of the carribiean");
            hi.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

}