import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.ByteArrayOutputStream;
public class Git
{
    public static void main (String [] args) throws IOException
    {
        final boolean COMPRESS = false;
        initializesGitRepo();

        File testFile = new File("test.txt");
        checkForAndDelete(testFile);
        testFile.createNewFile();
        File testFile2 = new File("redundantTest.txt");
        checkForAndDelete(testFile2);
        testFile2.createNewFile();
        File testDir = new File("testDir");
        checkForAndDelete(testDir);
        testDir.mkdir();
        File testFileInDir = new File("testDir/test2.txt");
        checkForAndDelete(testFileInDir);
        testFileInDir.createNewFile();

        FileWriter writer = new FileWriter("test.txt");
        writer.write("this is the first test");
        writer.close();
        FileWriter writerInDir = new FileWriter("testDir/test2.txt");
        writerInDir.write("this is the second test");
        writerInDir.close();
        FileWriter redundantWriter = new FileWriter("redundantTest.txt");
        redundantWriter.write("this is a redundant test");
        redundantWriter.close();
        
        createTree("testDir", COMPRESS);
        createBlob("test.txt", COMPRESS);
        createBlob("redundantTest.txt", COMPRESS);
    }


    private static File gitDirectory = new File("git");
    private static String hash;
    
    public static void initializesGitRepo ()
    {
        //create gitDirectory
        
        if (!gitDirectory.exists()) 
        {
            gitDirectory.mkdir();
            File objects = new File("git/objects");
            File index = new File("git/index");
            objects.mkdir();
            try 
            {
                index.createNewFile();
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
    
    private static void createBlob(String pathName, boolean compressed) throws IOException
    {
        putFileToObjectsFolder(pathName, compressed);
        putFileToIndex(pathName, compressed);

    }

    private static String createTree(String pathName, boolean compressed) throws IOException
    {
        File dir = new File(pathName);
        if(dir.exists())
        {
            File[] contents = dir.listFiles();
            System.out.println(contents.toString());
            if(contents.length != 0)
                {
                    for(File f : contents)
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
            putFileToIndex(pathName, compressed);
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
            String contentsString = "";
            if(contents.length != 0)
            {
                for(File f : contents)
                {
                    contentsString += f.getName() + " ";
                }
                contentsString = contentsString.substring(0, contentsString.length() - 1);
            }
            fileContentInBytes = contentsString.getBytes();
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
    
    public static String putFileToObjectsFolder(String pathName, boolean compressed) throws IOException
    {
        File regularFile = new File (pathName);
        byte[] fileContentInBytes = {};
        if(regularFile.isFile())
        {
            fileContentInBytes = Files.readAllBytes(regularFile.toPath());
        }
        else if(regularFile.isDirectory()){
            File[] contents = regularFile.listFiles();
            String contentsString = "";
            if(contents.length != 0)
            {
                for(File f : contents)
                {
                    contentsString += f.getName() + " ";
                    if(f.isFile())
                    {
                        tree += "blob " + getHash(f.getPath(), compressed) + " " + f.getName() + "\n";
                    }
                    else{
                        tree += "tree " + getHash(f.getPath(), compressed) + " " + f.getName() + "\n";
                    }
                }
                contentsString = contentsString.substring(0, contentsString.length() - 1);
            }
            fileContentInBytes = contentsString.getBytes();
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
        return hash;
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