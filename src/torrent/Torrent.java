/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package torrent;

import java.awt.EventQueue;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.apache.commons.io.IOUtils;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.StringTokenizer;
import java.util.zip.ZipInputStream;

public class Torrent {

    public Torrent() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                doit();
            }
        });
    }

    private static void encodeObject(Object o, OutputStream out) throws IOException {
        if (o instanceof String) {
            encodeString((String) o, out);
        } else if (o instanceof Map) {
            encodeMap((Map) o, out);
        } else if (o instanceof byte[]) {
            encodeBytes((byte[]) o, out);
        } else if (o instanceof Number) {
            encodeLong(((Number) o).longValue(), out);
        } else {
            throw new Error("Unencodable type");
        }
    }

    private static void encodeLong(long value, OutputStream out) throws IOException {
        out.write('i');
        out.write(Long.toString(value).getBytes("US-ASCII"));
        out.write('e');

    }

    private static void encodeBytes(byte[] bytes, OutputStream out) throws IOException {
        out.write(Integer.toString(bytes.length).getBytes("US-ASCII"));
        out.write(':');
        out.write(bytes);

    }

    private static void encodeString(String str, OutputStream out) throws IOException {
        encodeBytes(str.getBytes("UTF-8"), out);
    }

    private static void encodeMap(Map<String, Object> map, OutputStream out) throws IOException {
        // Sort the map. A generic encoder should sort by key bytes
        SortedMap<String, Object> sortedMap = new TreeMap<String, Object>(map);
        out.write('d');
        for (Entry<String, Object> e : sortedMap.entrySet()) {
            encodeString(e.getKey(), out);
            encodeObject(e.getValue(), out);
        }
        out.write('e');

    }

    private static byte[] hashPieces(File file, int pieceLength) throws IOException {
        MessageDigest sha1;
        int totalcount = 0;
        int piececount = 0;
        int newcount = 0;
        int totalbytes = 0;
        try {
            sha1 = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            throw new Error("SHA1 not supported");
        }
        InputStream in = new FileInputStream(file);
        ByteArrayOutputStream pieces = new ByteArrayOutputStream();
        byte[] bytes = new byte[pieceLength];
        int pieceByteCount = 0, readCount = in.read(bytes, 0, pieceLength);

        OutputStream out = null;
        if (firstrun) {
            out = new FileOutputStream(new File(file.getPath() + "_ncz/source/" + file.getName() + ".1scount"));
        }

        while (readCount != -1) {
            pieceByteCount += readCount;
            sha1.update(bytes, 0, readCount);
            if (pieceByteCount == pieceLength) {
                pieceByteCount = 0;
                pieces.write(sha1.digest());

                if (firstrun) {
                    for (int i = 0; i < pieceLength; i++) {
                        newcount = newcount + countBits(bytes[i]);

                        totalbytes++;
                        // System.out.println("1s in byte: "+newcount);
                    }
                    piececount++;
                    totalcount = totalcount + newcount;
                    out.write(newcount);
                    newcount = 0;
                }

            } else {
                newcount = 0;
                if (firstrun) {
                    for (int i = 0; i < pieceByteCount; i++) {
                        newcount = newcount + countBits(bytes[i]);
                        totalbytes++;
                        // System.out.println(countBits(bytes[i])+" 1s in byte: "+newcount);
                    }
                    piececount++;

                    totalcount = totalcount + newcount;
                    //piececount++;
                    out.write(newcount);//store count of 1s per block
                    out.write(pieceByteCount);//store remainder blocks size
                }
                System.out.println(pieceLength + " byte piecelength has a remainder of " + pieceByteCount + " bytes left");
            }
            readCount = in.read(bytes, 0, pieceLength - pieceByteCount);
        }
        if (firstrun) {
            out.write(totalcount);//store total number of 1s
            out.write(pieceLength);//store block size in byte size pieces
            out.close();
            System.out.println("total blocks counted for 1s: " + piececount + " - Total 1s:" + totalcount + " - " + totalbytes + " total bytes in file");

        }
        System.out.println("");
        in.close();
        if (pieceByteCount > 0) {
            pieces.write(sha1.digest());
        }

        return pieces.toByteArray();
    }

    public static void createTorrent(int size, File file, File sharedFile, String announceURL) throws IOException {
        //final int pieceLength = 1*128;
        final int pieceLength = size;
        Map<String, Object> info = new HashMap<String, Object>();
        info.put("name", sharedFile.getName());
        System.out.println(sharedFile.getName() + " - " + sharedFile.length() + " - " + pieceLength + " - ");
        info.put("length", sharedFile.length());
        info.put("piece length", pieceLength);
        info.put("pieces", hashPieces(sharedFile, pieceLength));
        Map<String, Object> metainfo = new HashMap<String, Object>();
        metainfo.put("announce", announceURL);
        metainfo.put("info", info);
        OutputStream out = new FileOutputStream(file);
        encodeMap(metainfo, out);
        out.close();
    }

    public static int countBits(byte number) {
        if (number == 0) {
            return number;
        }
        int count = 0;
        while (number != 0) {
            number &= (number - 1);
            count++;
        }
        return count;
    }

    public static int bitsInByteArray(byte[] b) {
        return b.length * 8;
    }

    public static void delete(File index) {
        String[] entries = index.list();
        boolean deleted = false;
        for (String s : entries) {

            File currentFile = new File(index.getPath(), s);
            deleted = true;
            currentFile.delete();
        }
        if (deleted) {
            System.out.println("delete");
        }
    }

    static public void zipFolder(String srcFolder, String destZipFile) throws Exception {
        ZipOutputStream zip = null;
        FileOutputStream fileWriter = null;
        fileWriter = new FileOutputStream(destZipFile);
        zip = new ZipOutputStream(fileWriter);
        addFolderToZip("", srcFolder, zip);
        zip.flush();
        zip.close();

    }

    static public void zipFolders(File[] srcFolder, String destZipFile) throws Exception {
        ZipOutputStream zip = null;
        FileOutputStream fileWriter = null;
        fileWriter = new FileOutputStream(destZipFile);
        zip = new ZipOutputStream(fileWriter);
        for (int i = 0; i < srcFolder.length; i++) {
            if (srcFolder[i].isDirectory()) {
                addFolderToZip("", srcFolder[i].toString(), zip);
            } else {
                addFileToZip("", srcFolder[i].toString(), zip);
            }
        }
        zip.flush();
        zip.close();

    }

    static private void addFileToZip(String path, String srcFile, ZipOutputStream zip)
            throws Exception {
        File folder = new File(srcFile);
        if (folder.isDirectory()) {
            addFolderToZip(path, srcFile, zip);
        } else {

            byte[] buf = new byte[1024];
            int len;
            FileInputStream in = new FileInputStream(srcFile);
            zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
            while ((len = in.read(buf)) > 0) {
                zip.write(buf, 0, len);
            }
            in.close();

        }
    }

    static private void addFolderToZip(String path, String srcFolder, ZipOutputStream zip)
            throws Exception {
        File folder = new File(srcFolder);

        for (String fileName : folder.list()) {

            if (path.equals("")) {
                addFileToZip(folder.getName(), srcFolder + "/" + fileName, zip);

            } else {

                addFileToZip(path + "/" + folder.getName(), srcFolder + "/" + fileName, zip);
            }

        }

    }

    public static byte[] createChecksum(String filename) throws Exception {
        InputStream fis = new FileInputStream(filename);

        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;

        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);

        fis.close();
        return complete.digest();
    }

    public static String getMD5Checksum(String filename) throws Exception {
        byte[] b = createChecksum(filename);
        String result = "";

        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    public static void rename(String md5, String path) {
        // File (or directory) with old name
        File file = new File(path + "\\.md5");

// File (or directory) with new name
        File file2 = new File(path + "\\" + md5 + ".md5");

// Rename file (or directory)
        boolean success = file.renameTo(file2);

        if (!success) {
            // File was not successfully renamed
        }
    }
    static boolean firstrun = true;
    static boolean delete = true;

    public static void main(String[] args) throws Exception {
        int bytes = 1024;
         int multiplier = 1;
        String value = (String) JOptionPane.showInputDialog(null, "Please choose a block size:", "Quantum Compression/Encryption level",
                JOptionPane.QUESTION_MESSAGE, null, new Object[]{"1 Byte (8-bit) - Encryption",
                    "2 Byte (16-bit) - Encryption", "4 Byte (32-bit) - Encryption", "8 Byte (64-bit) - Encryption", "16 Byte (128-bit) - Encryption", "32 Byte (256-bit) - Encryption", "64 Byte (512-bit) - Compression (1.5:1)",
                    "128 Byte (1024-bit) - Compression (3:1)", "256 Byte (2048-bit) - Compression (6:1)", "512 Byte (4096-bit) - Compression (12:1)", "1024 Byte (8192-bit) 1 Kilobyte - Compression (25:1)",
                     "2048 Byte - 2 Kilobyte - Time Capsule (49:1)", "4096 Byte - 4 Kilobyte - Time Capsule (99:1)", "8192 Byte - 8 Kilobyte - Time Capsule (200:1)", "16384 Byte - 16 Kilobyte - Time Capsule (400:1)"
                , "32768 Byte - 32 Kilobyte - Time Capsule (800:1)", "65536 Byte - 64 Kilobyte - Time Capsule (1600:1)", "131072 Byte - 128 Kilobyte - Time Capsule (3200:1)"
                , "262144 Byte - 256 Kilobyte - Time Capsule (6400:1)", "524288 Byte - 512 Kilobyte - Time Capsule (12800:1)", "1048576 Byte - 1024 Kb (1 Meg) - Time Capsule (25600:1)"}, "1024 Byte (8192-bit) 1 Kilobyte - Compression (25:1)");
        System.out.println(value);

        StringTokenizer st = new StringTokenizer(value, " ");
        // System.out.println(st.nextToken());
        String token = st.nextToken();
        if (token.equals("1")) {
            System.out.println("8 Bit");
            bytes = 1;
        } else if (token.equals("2")) {
            System.out.println("16 Bit");
            bytes = 2;
        } else if (token.equals("4")) {
            System.out.println("32 Bit");
            bytes = 4;
        } else if (token.equals("8")) {
            System.out.println("64 Bit");
            bytes = 8;
        } else if (token.equals("16")) {
            System.out.println("128 Bit");
            bytes = 16;
        } else if (token.equals("32")) {
            System.out.println("256 Bit");
            bytes = 32;
        } else if (token.equals("64")) {
            System.out.println("512 Bit");
            bytes = 64;
        } else if (token.equals("128")) {
            System.out.println("1024 Bit");
            bytes = 128;
        } else if (token.equals("256")) {
            System.out.println("2048 Bit");
            bytes = 256;
        } else if (token.equals("512")) {
            System.out.println("4096 Bit");
            bytes = 512;
        } else if (token.equals("1024")) {
            System.out.println("1 Kilobyte (8192 bit - 1024 byte)");
        } else if (token.equals("2048")) {
            System.out.println("2 Kilobyte (2048 byte)");
            multiplier = 2;
        }else if (token.equals("4096")) {
            System.out.println("4 Kilobyte (4096 byte)");
            multiplier = 4;
        }else if (token.equals("8192")) {
            System.out.println("8 Kilobyte (8192 byte)");
            multiplier = 8;
        }else if (token.equals("16384")) {
            System.out.println("16 Kilobyte (16384 byte)");
            multiplier = 16;
        }else if (token.equals("32768")) {
            System.out.println("32 Kilobyte (32768 byte)");
            multiplier = 32;
        }else if (token.equals("65536")) {
            System.out.println("64 Kilobyte (65536 byte)");
            multiplier = 64;
        }else if (token.equals("131072")) {
            System.out.println("128 Kilobyte (131072 byte)");
            multiplier = 128;
        }else if (token.equals("262144")) {
            System.out.println("256 Kilobyte (262144 byte - quarter megabyte)");
            multiplier = 256;
        }else if (token.equals("524288")) {
            System.out.println("512 Kilobyte (524288 - half megabyte)");
            multiplier = 512;
        }else if (token.equals("1048576")) {
            System.out.println("1024 Kilobyte (1048576 - 1 megabyte)");
            multiplier = 1024;
        }else {
            System.out.println("Nothing chosen, defaulting to 1 Kilobyte (8192 bit)");

        }

        String directory = "C:/torrent/";
        String thefile = "mario";
        String filetype = "nes";

       
        final JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);//multi file select not enabled yet needs code
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int returnVal = fc.showOpenDialog(null);
        thefile = thefile + "." + filetype;

        if (returnVal == 0) {
            File[] file = fc.getSelectedFiles();

            if (file.length == 1) {
                if (!file[0].isDirectory()) {
                    //ask if they want to prezip (good idea to do so...)
                    directory = file[0].getParent() + "/";
                    thefile = file[0].getName();
                } else {
                    System.out.println("A folder was selected - creating zip file containing selected content.");

                    zipFolder(file[0].getPath(), file[0].getPath() + "_nano.zip");
                    directory = file[0].getParent() + "/";
                    thefile = file[0].getName() + "_nano.zip";
                }
            } else {
                System.out.println("multiple files and/or folders selected - creating zip file containing selected content.");
                zipFolders(file, file[0].getPath() + "_nano.zip");
                directory = file[0].getParent() + "/";
                thefile = file[0].getName() + "_nano.zip";
            }
        }

        new File(directory + thefile + "_ncz\\source").mkdirs();
        String odir = directory;
        directory = directory + thefile + "_ncz/source/";
        File index = new File(directory);
        delete(index);
        System.out.println(thefile + "\n" + directory + "\n");

        while (multiplier <= 2048) {
            createTorrent(multiplier * bytes, new File(directory + thefile + "." + multiplier + "x" + bytes + ".torrent"), new File(odir + thefile), "http://nanocheeze.com");
            if (bytes == 1024) {
                multiplier = multiplier * 2;
            } else {
                bytes = bytes * 2;
            }
            firstrun = false;
        }
        System.out.println("\n\n--------------\nCreating Checksums for inner zip");
        try {

            String[] entries = index.list();

            try {
                PrintWriter writer = new PrintWriter(directory + ".md5", "UTF-8");

                for (String s : entries) {

                    File currentFile = new File(index.getPath(), s);
                    String md5 = getMD5Checksum(currentFile.toString());
                    writer.println(currentFile.getName() + " - " + md5);
                    System.out.println(currentFile.toString() + " - " + md5);

                }
                String md5 = getMD5Checksum(odir + thefile);
                System.out.println("\nOriginal file md5: " + md5 + " - original filepath:" + odir + thefile + "");
                writer.println(thefile + " - " + md5);
                writer.close();
            } catch (IOException e) {

            }

            String md5 = getMD5Checksum(directory + "\\.md5");
            System.out.println("\n.md5: " + md5 + "");
            rename(md5, directory);
            String newmd5 = getMD5Checksum(directory + "\\" + md5 + ".md5");
            System.out.println("" + md5 + ".md5: " + newmd5 + "\n");
            if (md5.equals(newmd5)) {
                System.out.println("MD5 Creation succesful!\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(directory);
        zipFolder(directory, odir + thefile + "_ncz/" + thefile + ".ncz");

        if (!index.exists()) {
            //index.mkdir();
        } else if (delete) {
            delete(index);
            index.delete();

        }

        //success ask to verify
        JDialog.setDefaultLookAndFeelDecorated(true);
        int response = JOptionPane.showConfirmDialog(null, "Do you want to verify your file?", "Verify File?",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (response == JOptionPane.NO_OPTION) {
            System.out.println("You chose not to verify the file(s).");
        } else if (response == JOptionPane.YES_OPTION) {
            System.out.println("Verifying file(s).");
            verify = true;
            //verify 1s file..
            //later
            //extract torrentcheck.exe
            new Torrent();
            String directory2 = directory.substring(0, directory.length() - 7);
            directory2 = directory2.replace("/", "\\");
            String zipFilePath = directory2 + thefile + ".ncz";

            String destDir = directory2 + "verify\\";

            unzip(zipFilePath, destDir);

            System.out.println(directory2);
            System.out.println(directory2 + thefile + ".zip");
            System.out.println(odir);

            System.out.println(odir);

            runbat(directory, odir.substring(0, odir.length() - 1) + "\\" + thefile);

            //end
        } else if (response == JOptionPane.CLOSED_OPTION) {
            System.out.println("You chose not to verify the file(s).");
        }
    }

    public static String thedatFile;
    public static File[] opener = new File[100];

    public static void runbat(String directory, String ofile) {

        directory = directory.replace("/", "\\");
        String directory2 = directory.substring(0, directory.length() - 7);
        //create bat file to update system
        String FILENAME = directory2 + "\\verify.bat";
        BufferedWriter bw = null;
        FileWriter fw = null;

        try {
            File folder = new File(directory2 + "verify\\source\\");
            System.out.println(directory2 + "verify\\source\\");
            File[] listOfFiles = folder.listFiles();
            String checks = "";
            System.out.println("waiting for files to finish writing...one moment please...");

            while (verifyerloc.equals("")) {

            }
            int i = 0, l = 0;
            for (File file : listOfFiles) {
                l++;
            }
            for (File file : listOfFiles) {

                if (file.toString().substring(file.toString().length() - 8, file.toString().length()).equals(".torrent")) {
                    i++;
                    System.out.println("in use " + file);

                    String temp = directory2 + "verify\\source\\";
                    String temp2 = directory2 + "verify\\source2\\";
                    opener[i - 1] = file;

                    System.out.println(file);
                    checks = checks + verifyerloc + " -t \"" + file + "\" -p \"" + ofile + "\" && echo \"Verification of file " + i + " of " + (l - 2) + " was successful\" || set \"verified=fail\"\necho[\n";
                }
            }
            String checker = "";

            for (int j = 0; j < i; j++) {
                checker = checker + "( (call ) >>\"" + opener[j] + "\" ) 2>nul && (\n"
                        + "  echo The file is unlocked and ready for transfer\n"
                        + ") || (\n"
                        + "  echo The file is still being created\n"
                        + ")\n";
            }

            String content = "@echo off\n"
                    + checker
                    + "ping -n 11 -w 1000 127.0.0.1 > nul\necho[\n"
                    + "set \"verified=good\"\ncd \"" + directory.substring(0, directory.length() - 7) + "verify\\source\"\n"
                    + checks
                    + "if \"%verified%\"==\"fail\" (echo \"VERIFICATION FAILED!\") else (echo \"Verification passed! You have a properly compressed file!\")"
                    + "\ncd ..\nrmdir /Q /S source\ncd ..\nrmdir /Q /S verify\nDel verify.bat >nul 2>&1&pause";
            //                  + "\ncd ..\ncd ..";

            fw = new FileWriter(FILENAME);
            bw = new BufferedWriter(fw);
            bw.write(content);

            System.out.println("\n\nSUCCESS!!! Your file(s) have been successfully compressed/hashed/encrypted...");

        } catch (IOException e) {

            e.printStackTrace();

        } finally {

            try {

                if (bw != null) {
                    bw.close();
                }

                if (fw != null) {
                    fw.close();
                }

            } catch (IOException ex) {

                ex.printStackTrace();

            }

        }

        //launch bat 
        try {
            /*
            int i = 0;
            while (i < 10000) {
                i++;
               // System.out.print("waiting");
            }
             */

            File it = new File(directory2 + "verify.bat");
            String itter = it.toString();
            System.out.println(itter);//waiting

            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", "verify.bat");
            pb.directory(new File(directory2));
            pb.redirectError();
            Process process = pb.start();

            //Runtime.getRuntime().exec("cmd /c start " + itter);
            //  System.exit(0);
            if (verify) {
                System.out.println("\nWaiting for file Verification to finish...");//waiting

                while (new File(directory2 + "verify.bat").exists()) {
                    //Thread.sleep(5000);

                }

                System.out.println("\n\nVerification Finished!");//waiting

            }
        } catch (Exception e) {
            //

            // }
        }
        System.exit(0);

    }
    public static boolean verify = false;
    public static String verifyerloc = "";

    public void doit() {

        File datFile = null;
        //File tempFile2=null;

        try {
            datFile = File.createTempFile("torrentcheck", ".exe");
            datFile.deleteOnExit();
            FileOutputStream out = new FileOutputStream(datFile);
            IOUtils.copy(indat, out);
            thedatFile = datFile.getPath();     // For example
            System.out.println(thedatFile + "  extracted");
            indat.close();
            verifyerloc = thedatFile;
        } catch (IOException e) {

        }

    }
    public InputStream indat = getClass().getResourceAsStream("/torrent/torrentcheck.exe");

    private static void unzip(String zipFilePath, String destDir) {
        File dir = new File(destDir);
        // create output directory if it doesn't exist
        if (!dir.exists()) {
            dir.mkdirs();
        }
        FileInputStream fis;
        //buffer for read and write data to file
        byte[] buffer = new byte[1024];
        try {
            fis = new FileInputStream(zipFilePath);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                String fileName = ze.getName();
                File newFile = new File(destDir + File.separator + fileName);
                System.out.println("Unzipping to " + newFile.getAbsolutePath());
                //create directories for sub directories in zip
                new File(newFile.getParent()).mkdirs();
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                //close this ZipEntry
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
            //close last ZipEntry
            zis.closeEntry();
            zis.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
