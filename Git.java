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
    public static void main (String [] args)
    {
        initializesGitRepo();
        System.out.println("Git Repository Initialized");
        Git git = new Git();
        hash = null;
        try 
        {
            hash = git.putFileToObjectsFolder();
            System.out.println("File added to objects folder " + hash);
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
        if (hash != null)
        {
            File objectFile = new File ("git/objects/" + hash);
            if (objectFile.exists())
            {
                System.out.println("File added to the  objects folder");
            }
            else
            {
                System.out.println("File not found in the objects folder");
            }
        }
        try {
            git.putFileToIndex(hash, "plainTextFile.txt");
            System.out.println("File added to the index");
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
        File index = new File ("git/index");
        try 
        {
            String insideIndex = new String (Files.readAllBytes(index.toPath()));
            if (insideIndex.contains (hash + " plainTextFile.txt"))
            {
                System.out.println("File added to the index");
            }
            else
            {
                System.out.println("File not found in the index");
            }
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
        checkForAndDelete(gitDirectory);
        System.out.println ("Deleted");
    }
    private static File gitDirectory = new File("git");
    private static String hash;
    private static boolean compression = true;
    public static void initializesGitRepo ()
    {
        //create gitDirectory
        
        if (!gitDirectory.exists()) 
        {
            gitDirectory.mkdir();
        }
        else
        {
            System.out.println ("Git Repository already exists");
        }
        //create objects inside gitDirectory
        File objects = new File("git/objects");
        if (!objects.exists())
        {
            objects.mkdir();
        }
        else
        {
            System.out.println ("Git Repository already exists");
        }
        //creating index file
        File index = new File("git/index");
        if (!index.exists())
        {
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
            System.out.println ("Git Repository already exists");
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
    
    public String putFileToObjectsFolder()throws IOException
    {
        File regularFile = new File ("plainTextFile.txt");
        try (FileWriter writer2 = new FileWriter (regularFile))
        {
            writer2.write("top secret data");
        }
        byte[] fileContentInBytes = Files.readAllBytes(regularFile.toPath());
        byte [] hashData = null;
        if (compression)
        {
            hashData = zipBytes(regularFile.getName(), fileContentInBytes);
        }
        else
        {
            hashData = fileContentInBytes;
        }
        //reads the contents of the file into the byte array and then  we can generate the hash by using our encryptString method
        String fileContent = new String (Files.readAllBytes(regularFile.toPath()));
        hash = encryptThisString(new String (hashData));
        File objects = new File ("git/objects");
        if (!objects.exists())
        {
            objects.mkdir();
        }
        File objectFile = new File ("git/objects/" + hash);
        //Writes out bytes better
        try (FileOutputStream writer = new FileOutputStream (objectFile))
        {
            writer.write(hashData);

        }
        return hash;
    }

    public void putFileToIndex(String hash, String filename)throws IOException
    {
        File index = new File ("git/index");
        //this append mode is used so that we can add to an existing file without altering the og data
        try (FileWriter writer = new FileWriter (index, true))
        {
            writer.write(hash + " " + filename + "\n");
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


