/*
 * DownloadHandler.java
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

package org.docma.webapp;

import org.docma.app.*;
import org.docma.util.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.zkoss.zul.*;

/**
 *
 * @author MP
 */
public class DownloadHandler
{
    MainWindow mainwin;
    File tempDir;

    public DownloadHandler(MainWindow mainwin)
    {
        this.mainwin = mainwin;
        DocmaApplication docmaApp = GUIUtil.getDocmaWebApplication(mainwin);
        this.tempDir = docmaApp.getTempDirectory();
    }

    public void downloadSelectedTreeNodes(Tree docTree) throws Exception
    {
        List nodes = getSelectedDocmaNodes(docTree);
        if (nodes == null) {
            Messagebox.show("Please select a content or folder node!");
            return;
        }
        DocmaNode nd = (DocmaNode) nodes.get(0);

        InputStream in;
        String conttype;
        String filenm;
        File zipfile = null;
        boolean is_zip = !((nodes.size() == 1) && nd.isContent());
        if (is_zip) {
            zipfile = createZipFile(nodes);
            conttype = "application/zip";
            filenm = "download.zip";
            in = new FileInputStream(zipfile);
        } else {
            conttype = "application/octet-stream";
            filenm = nd.getDefaultFileName();
            in = nd.getContentStream();
        }

        Filedownload.save(in, conttype, filenm);
        // in.close();
        if (zipfile != null) {
            // zipfile.delete();
        }
    }

    private File createZipFile(List nodes) throws Exception
    {
        try {
            File f = getTempZipFilename(tempDir);
            FileOutputStream fout = new FileOutputStream(f);
            ZipOutputStream zipout = new ZipOutputStream(fout);

            SortedSet addedFiles = new TreeSet();
            for (int i=0; i < nodes.size(); i++) {
                DocmaNode node = (DocmaNode) nodes.get(i);
                addZipEntry(zipout, node, "", addedFiles);
            }

            zipout.close();
            fout.close();
            return f;
        } catch(IOException ex) {
            Messagebox.show("Error writing Zip Stream: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }

    private List getSelectedDocmaNodes(Tree docTree)
    {
        int cnt = docTree.getSelectedCount();
        if (cnt == 0) return null;

        List list = new ArrayList(cnt);
        Iterator it = docTree.getSelectedItems().iterator();
        while (it.hasNext()) {
            Treeitem item = (Treeitem) it.next();
            Object obj = item.getValue();
            if ((obj != null) && (obj instanceof DocmaNode)) {
                DocmaNode node = (DocmaNode) obj;
                if (node.isReference()) {
                    node = node.getReferencedNode();
                    if (node == null) continue;
                }
                if (node.isContent() || node.isImageFolder() || node.isSystemFolder()) {
                    list.add(node);
                }
            }
        }
        if (list.isEmpty()) return null;
        else return list;
    }


    private void addZipEntry(ZipOutputStream zipout, DocmaNode node, String path, Set addedFiles)
    throws IOException
    {
        if (node.isReference()) {
            node = node.getReferencedNode();
            if ((node == null) || !node.isContent()) return;
        }

        if (node.isImageFolder() || node.isSystemFolder()) {
            String foldname = node.getTitle();
            if (foldname == null) foldname = "";  // should not occur
            foldname = foldname.replace('/', '_').replace('\\', '_').trim();
            String newpath = path + foldname + "/";
            for (int i=0; i < node.getChildCount(); i++) {
                DocmaNode ch = node.getChild(i);
                addZipEntry(zipout, ch, newpath, addedFiles);
            }
        } else
        if (node.isContent()) {
            String nodefn = node.getDefaultFileName();
            // File f = new File(path, nodefn);
            String filename = path + nodefn; // f.getPath();
            if (! addedFiles.contains(filename)) {
                ZipEntry ze = new ZipEntry(filename);
                zipout.putNextEntry(ze);
                // Daten an zipout senden
                InputStream in = node.getContentStream();
                if (in != null) {
                    try {
                        DocmaUtil.copyStream(in, zipout);
                    } finally {
                        in.close();
                    }
                }
                zipout.closeEntry();
                addedFiles.add(filename);
            }
        }
    }

    private static synchronized File getTempZipFilename(File tmpDir)
    {
        File f;
        long now = System.currentTimeMillis();
        cleanTempDir(tmpDir, now);
        do {
            f = new File(tmpDir, "download" + (now++) + ".zip");
        } while (f.exists());
        return f;
    }

    private static synchronized void cleanTempDir(File tmpDir, long now)
    {
        final String PREFIX = "download";
        String[] fnames = tmpDir.list();
        for (int i=0; i < fnames.length; i++) {
            String fn = fnames[i];
            if (fn.startsWith(PREFIX)) {
                int pos = fn.indexOf('.', PREFIX.length());
                if (pos > 0) {
                    String numstr = fn.substring(PREFIX.length(), pos);
                    try {
                        long num = Long.parseLong(numstr);
                        if ((now - num) > 48*60*60*1000) {  // delete files which are older than 48 hours
                            File delfile = new File(tmpDir, fn);
                            delfile.delete();
                        }
                    } catch (Exception ex) {}
                }
            }
        }
    }

}
