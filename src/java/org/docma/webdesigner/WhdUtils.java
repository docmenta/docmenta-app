/*
 * WhdUtils.java
 */
package org.docma.webdesigner;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;

/**
 *
 * @author manfred
 */
public class WhdUtils 
{
    public static boolean selectListItem(Listbox listbox, String itemvalue)
    {
        for (int i=0; i < listbox.getItemCount(); i++) {
            Listitem item = listbox.getItemAtIndex(i);
            Object val = item.getValue();
            if ((val != null) && val.toString().equalsIgnoreCase(itemvalue)) {
                item.setSelected(true);
                return true;
            }
        }
        return false;
    }
    
    public static boolean removeListItem(Listbox listbox, String itemvalue) 
    {
        for (int i=0; i < listbox.getItemCount(); i++) {
            Listitem item = listbox.getItemAtIndex(i);
            Object val = item.getValue();
            if ((val != null) && val.toString().equalsIgnoreCase(itemvalue)) {
                listbox.removeItemAt(i);
                return true;
            }
        }
        return false;        
    }

    public static String getSelectedListValue(Listbox listbox, String default_value)
    {
        Listitem sel = listbox.getSelectedItem();
        if (sel == null) {
            return default_value;
        }
        Object obj = sel.getValue();
        String res = (obj == null) ? sel.getLabel() : obj.toString();
        return (res == null) ? default_value : res;
    }

    public static String readStreamToString(InputStream in, String encoding) throws IOException
    {
        InputStreamReader reader = new InputStreamReader(in, encoding);
        StringBuilder outbuf = new StringBuilder();
        char[] buf = new char[16 * 1024];
        int cnt;
        while ((cnt = reader.read(buf)) >= 0) {
            outbuf.append(buf, 0, cnt);
        }
        return outbuf.toString();
    }


    public static byte[] readFile(File f) throws IOException
    {
        FileInputStream fin = new FileInputStream(f);
        ByteArrayOutputStream outbuf = new ByteArrayOutputStream();
        byte[] buf = new byte[16 * 1024];
        int cnt;
        while ((cnt = fin.read(buf)) >= 0) {
            outbuf.write(buf, 0, cnt);
        }
        try { fin.close(); } catch (Exception ex) {}
        return outbuf.toByteArray();
    }
    
    public static String readFileToString(File f) throws IOException
    {
        FileInputStream fin = new FileInputStream(f);
        String s = readStreamToString(fin, "UTF-8");
        try { fin.close(); } catch (Exception ex) {}
        return s;
        // StringBuilder buf = new StringBuilder();
        // BufferedReader in = new BufferedReader(new FileReader(f));
        // String line;
        // while ((line = in.readLine()) != null) {
        //     buf.append(line);
        // }
        // return buf.toString();
    }
    
    public static void writeBytesToFile(byte[] data, File fileout) throws IOException
    {
        FileOutputStream fout = new FileOutputStream(fileout);
        try {
            fout.write(data);
        } finally {
            try { fout.close(); } catch (Exception ex) {}
        }
    }

    public static void writeStringToFile(String str, File fileout, String charsetName)
    throws IOException
    {
        FileOutputStream fout = new FileOutputStream(fileout);
        try {
            Writer w = new OutputStreamWriter(fout, charsetName);
            w.write(str);
            w.close();
        } finally {
            try { fout.close(); } catch (Exception ex) {}
        }
    }

    public static boolean recursiveFileCopy(File sourceDir, File destDir, boolean overwrite)
    {
        if (sourceDir.isDirectory()) {
            if (! destDir.exists()) {
                destDir.mkdirs();
            }
            String[] fnames = sourceDir.list();
            for (int i=0; i < fnames.length; i++) {
                File sourceChild = new File(sourceDir, fnames[i]);
                File destChild = new File(destDir, fnames[i]);
                if (! recursiveFileCopy(sourceChild, destChild, overwrite)) {
                    return false;
                }
            }
            return true;
        } else {
            return fileCopy(sourceDir, destDir, overwrite);
        }
    }

    public static boolean fileCopy(File sourceFile, File destFile, boolean overwrite)
    {
        if (destFile.exists()) {
            if (overwrite) {
                if (! destFile.delete()) return false;
            } else {
                return false;
            }
        }
        try {
            FileInputStream fin = new FileInputStream(sourceFile);
            FileOutputStream fout = new FileOutputStream(destFile);
            byte[] buf = new byte[4096];
            int cnt;
            while((cnt = fin.read(buf)) >= 0) {
                fout.write(buf, 0, cnt);
            }
            fin.close();
            fout.close();
            return true;
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    
    public static void clearDirectory(File dir)
    {
        File[] sub = dir.listFiles();
        if (sub != null) {
            for (File f : sub) {
                recursiveFileDelete(f);
            }
        }
    }

    public static void clearDirectory(File dir, String prefix)
    {
        File[] sub = dir.listFiles();
        if (sub != null) {
            for (File f : sub) {
                if (f.getName().startsWith(prefix)) {
                    recursiveFileDelete(f);
                }
            }
        }
    }

    public static boolean recursiveFileDelete(File f)
    {
        if (! f.isAbsolute()) {
            throw new RuntimeException("Recursive file deletion is not allowed for relative path names: " + f);
        }
        boolean del_okay = true;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            for (int i=0; i < children.length; i++) {
                if (! recursiveFileDelete(children[i])) del_okay = false;
            }
        }
        if (! f.delete()) del_okay = false;

        return del_okay;
    }

    
    public static Properties loadPropertiesFile(File propfile) throws IOException
    {
        Properties props = new Properties();
        // ClassLoader cl = PropertiesLoader.class.getClassLoader();
        // InputStream fin = cl.getResourceAsStream(inifilename);
        InputStream fin = new FileInputStream(propfile);
        try {
            props.load(fin);
            return props;
        } finally {
            fin.close();
        }
    }

    public static String getFilenameExtension(String filename) 
    {
        int p = filename.lastIndexOf('.');
        if (p < 0) {
            return null;
        }
        return filename.substring(p + 1);
    }

    public static void extractZipStream(InputStream in, File extractDir) throws Exception
    {
        ZipInputStream zip_in;
        boolean wrapped = false;
        if (in instanceof ZipInputStream) {
            zip_in = (ZipInputStream) in;
        } else {
            zip_in = new ZipInputStream(in);
            wrapped = true;
        }

        ZipEntry entry;
        while ((entry = zip_in.getNextEntry()) != null) {
            String entry_name = entry.getName();
            File out_file = new File(extractDir, entry_name);
            if (entry.isDirectory()) {
                if (! out_file.exists()) out_file.mkdirs();
            } else {
                // BufferedInputStream buf_in = new BufferedInputStream(zip_in);
                File parentDir = out_file.getParentFile();
                if (! parentDir.exists()) parentDir.mkdirs();
                FileOutputStream out = new FileOutputStream(out_file);
                copyStream(zip_in, out);
                try { out.close(); } catch (Exception ex) {}  // ignore close exception
            }
            zip_in.closeEntry();
        }

        if (wrapped) {
            zip_in.close();
        }
    }

    public static void copyStream(InputStream in, OutputStream out) throws IOException
    {
        byte[] buf = new byte[64*1024];
        int cnt;
        while ((cnt = in.read(buf)) >= 0) {
            if (cnt > 0) out.write(buf, 0, cnt);
        }
        // in.close();
    }

}
