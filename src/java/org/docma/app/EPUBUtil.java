/*
 * EPUBUtil.java
 * 
 *  Copyright (C) 2013  Manfred Paula, http://www.docmenta.org
 *   
 *  This file is part of Docmenta. Docmenta is free software: you can 
 *  redistribute it and/or modify it under the terms of the GNU Lesser 
 *  General Public License as published by the Free Software Foundation, 
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Docmenta.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.docma.app;

import java.util.zip.*;
import java.io.*;
import org.docma.coreapi.*;
import org.docma.util.*;

/**
 *
 * @author MP
 */
public class EPUBUtil
{

    public static void addMIMETypeFile(ZipOutputStream zipout) throws Exception
    {
        final String FILE_CONTENT = "application/epub+zip";

        int len = FILE_CONTENT.length();
        ByteArrayOutputStream bout = new ByteArrayOutputStream(len);
        OutputStreamWriter writer = new OutputStreamWriter(bout, "UTF-8");
        writer.write(FILE_CONTENT);
        writer.flush();
        bout.close();
        byte[] arr = bout.toByteArray();
        if (arr.length != len) {
            Log.warning("EPUBUtil.addMIMETypeFile(): unexpected array length!");
        }

        CRC32 crc_calculator = new CRC32();
        crc_calculator.update(arr);

        ZipEntry ze = new ZipEntry("mimetype");
        ze.setMethod(ZipEntry.STORED);  // uncompressed
        ze.setExtra(new byte[0]);
        ze.setSize(arr.length);
        ze.setCrc(crc_calculator.getValue());
        zipout.putNextEntry(ze);
        zipout.write(arr);
        zipout.closeEntry();
    }

    public static void validateEPUBPublication(DocmaSession docmaSess,
                                               String publicationId,
                                               ExportLog export_log)
                                               throws Exception
    {
        DocmaPublication pub = docmaSess.getPublication(publicationId);
        validateEPUBPublication(docmaSess, pub, export_log);
    }

    public static void validateEPUBPublication(DocmaSession docmaSess,
                                               DocmaPublication pub,
                                               ExportLog export_log)
                                               throws Exception
    {
        String tempPath = docmaSess.getApplicationProperty(DocmaConstants.PROP_TEMP_PATH);
        String xslPath = docmaSess.getApplicationProperty(DocmaConstants.PROP_DOCBOOK_XSL_PATH);

        File tempDir = new File(tempPath);
        if (! tempDir.exists()) {
            export_log.errorMsg("Temporaray directory does not exist: " + tempDir.getAbsolutePath());
            return;
        }

        File tempFile = null;
        long millis = System.currentTimeMillis();
        for (int i=0; i < 10; i++) {
            tempFile = new File(tempDir, "epubval" + (millis + i) + ".epub");
            if (! tempFile.exists()) break;
        }
        if (tempFile.exists()) {
            export_log.errorMsg("Could not create temporary file. File already exists.");
            return;
        }

        InputStream in = pub.getContentStream();
        try {
            if (in == null) {
                export_log.errorMsg("Could not read publication. Content is null.");
                return;
            }
            DocmaUtil.writeStreamToFile(in, tempFile);
            validateEPUBFile(tempFile, new File(xslPath), export_log);
        } finally {
            if (in != null) in.close();
            tempFile.delete();
        }
    }

    private static void validateEPUBFile(File epub_file,
                                         File docbookXSLDir,
                                         ExportLog export_log)
    {
        export_log.infoMsg("Starting EPUB file validation.");
        String javahome = System.getProperty("java.home");
        if (DocmaConstants.DEBUG) System.out.println("Java Home: " + javahome);
        // export_log.infoMsg("Using Java Home: " + javahome);

        File javabin = new File(javahome, "bin");
        String javacommand = new File(javabin, "java").getPath();

        File workdir = new File(docbookXSLDir, "epubcheck");
        if (! workdir.exists()) {
            export_log.errorMsg("Could not find epubcheck folder: " + workdir.getAbsolutePath());
            return;
        }
        File jar_file = getEPUBCheckJar(workdir);
        if (jar_file == null) {
            export_log.errorMsg("Could not find epubcheck .jar library.");
            return;
        }

        String[] cmd = new String[] {
            javacommand,
            "-jar", jar_file.getName(),
            epub_file.getAbsolutePath()
        };

        Runtime rt = Runtime.getRuntime();
        try {
            if (DocmaConstants.DEBUG) System.out.println("Starting validation process... ");
            Process proc = rt.exec(cmd, new String[] {}, workdir);
            InputStream in_std = proc.getInputStream();
            InputStream in_err = proc.getErrorStream();
            OutputStream out = proc.getOutputStream();
            out.close();
            if (DocmaConstants.DEBUG) System.out.println("Reading std_err... ");
            String msg_err = readStringFromInputStream(in_err).trim();
            in_err.close();
            if (DocmaConstants.DEBUG) System.out.println("Reading std_in... ");
            String msg = readStringFromInputStream(in_std).trim();
            in_std.close();
            if (DocmaConstants.DEBUG) {
                System.out.println("Waiting for validation process ...");
            }
            int exitcode = proc.waitFor();
            if (DocmaConstants.DEBUG) {
                System.out.println("EPUB validation process finished with exit code " + exitcode);
            }
            boolean has_error = (msg_err.length() > 0);
            if (has_error) {
                msg += ((msg.length() > 0) ? "\n" : "") + msg_err;
                export_log.warningMsg(msg);  // Show validation errors as warning
            } else
            if (msg.length() > 0) {
                export_log.infoMsg(msg);
            } else {
                if (exitcode != 0) {
                    export_log.warningMsg("EPUB validation finished with exit code: " + exitcode);
                } else {
                    export_log.infoMsg("EPUB validation finished. No errors found.");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            export_log.errorMsg(ex.getMessage());
        }

    }


    private static File getEPUBCheckJar(File workdir)
    {
        File jar_file = null;
        String[] fnarr = workdir.list();
        for (int i=0; i < fnarr.length; i++) {
            String fn = fnarr[i];
            if (fn.startsWith("epubcheck") && fn.endsWith(".jar")) {
                jar_file = new File(workdir, fn);
                break;
            }
        }
        return jar_file;
    }

    private static String readStringFromInputStream(InputStream in) throws Exception
    {
        BufferedReader rd = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line).append("\n");
            // if (DocmaConstants.DEBUG) System.out.println("'" + line + "'");
        }
        return sb.toString();
    }

}
