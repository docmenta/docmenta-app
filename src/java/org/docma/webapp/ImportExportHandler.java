/*
 * ImportExportHandler.java
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

import java.util.*;
import java.text.SimpleDateFormat;
import java.io.*;
import org.zkoss.zul.*;
import org.zkoss.util.media.Media;

import org.docma.app.*;
import org.docma.util.*;

/**
 *
 * @author MP
 */
public class ImportExportHandler
{
    private SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
    private static final String IMPORT_DIR_PREFIX = "import_";

    private MainWindow mainwin;

    public ImportExportHandler(MainWindow mainwin)
    {
        this.mainwin = mainwin;
    }

    public void exportNodes(Tree docTree) throws Exception
    {
        List nodes = getSelectedDocmaNodes(docTree);
        if (nodes == null) return;   // selection was not valid
        DocmaNode[] node_arr = (DocmaNode[]) nodes.toArray(new DocmaNode[nodes.size()]);

        DocmaSession docmaSess = mainwin.getDocmaSession();
        File zipfile = null;
        try {
            zipfile = docmaSess.exportNodesToFile(node_arr, null);
        } catch (Exception ex) {
            if (DocmaConstants.DEBUG) ex.printStackTrace();
            Messagebox.show("Export error: " + ex.getMessage());
            return;
        }

        String conttype = "application/zip";
        String timestr = dateformat.format(new Date());
        String filenm = "export_nodes_" + timestr + ".zip";
        InputStream in = new FileInputStream(zipfile);
        Filedownload.save(in, conttype, filenm);
        // in.close();
        if (zipfile != null) {
            if (!DocmaConstants.DEBUG) zipfile.delete();
        }
    }

    public boolean importSubNodes(Tree docTree) throws Exception
    {
        List nodes = getSelectedDocmaNodes(docTree);
        if (nodes == null) return false;   // selection was not valid
        if (nodes.size() > 1) {
            Messagebox.show("Please select a single section or folder!");
            return false;
        }

        DocmaNode parentNode = (DocmaNode) nodes.get(0);
        DocmaSession docmaSess = mainwin.getDocmaSession();
        if (docmaSess.getTranslationMode() != null) {
            Messagebox.show("Cannot import nodes in translation mode!");
            return false;
        }

        String message = mainwin.i18("label.upload.selectfile") + ": ";
        String title = mainwin.i18("label.upload.dialog.title");
        Media med = Fileupload.get(message, title, true);
        if (med != null) {
            File tempDir = GUIUtil.getDocmaWebApplication(mainwin).getTempDirectory();
            cleanTempDir(tempDir);

            File importDir = new File(tempDir, IMPORT_DIR_PREFIX + System.currentTimeMillis());
            if (! importDir.exists()) importDir.mkdirs();
            if (! importDir.exists()) {
                Messagebox.show("Import failed: Could not create temporary directory: " + importDir);
                return false;
            }
            try {
                ZipUtil.extractZipStream(med.getStreamData(), importDir);
            } catch (Exception ex) {
                Messagebox.show("Import failed: Could not extract file: " + ex.getMessage());
                if (DocmaConstants.DEBUG) ex.printStackTrace();
                return false;
            }

            try {
                // get translation languages of nodes to be imported
                DocmaLanguage[] importLangs_all = docmaSess.getImportTranslationLanguages(importDir);

                // show message if language to be imported is not configured
                DocmaLanguage[] langs = docmaSess.getTranslationLanguages();
                SortedSet diff_langs = new TreeSet(Arrays.asList(importLangs_all));
                diff_langs.removeAll(Arrays.asList(langs));
                if (diff_langs.size() > 0) {
                    Iterator it = diff_langs.iterator();
                    StringBuilder sb = new StringBuilder();
                    while (it.hasNext()) {
                        if (sb.length() > 0) sb.append("\n");
                        sb.append(((DocmaLanguage)it.next()).getDescription());
                    }
                    if (Messagebox.show(
                            "The import contains translated content of languages which are \n" +
                            "not configured for the current product.\n" +
                            "\nFollowing translations will not be imported:\n\n" + sb, "Continue?",
                        Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION) == Messagebox.CANCEL) {
                        return false;  // cancel import
                    }
                }

                // import only configured translation languages
                SortedSet importLangs = new TreeSet(Arrays.asList(importLangs_all));
                importLangs.removeAll(diff_langs);  // remove languages which are not configured
                DocmaLanguage[] langs_arr = new DocmaLanguage[importLangs.size()];
                langs_arr = (DocmaLanguage[]) importLangs.toArray(langs_arr);

                docmaSess.importNodes(parentNode, importDir, langs_arr);
                // Clients.evalJavaScript("window.frames['viewcontentfrm'].location.reload();");
                return true;
            } finally {
                if (!DocmaConstants.DEBUG) DocmaUtil.recursiveFileDelete(importDir);
            }
        } else {
            return false;
        }
    }

    /* ------  Private methods  ------ */

    private static List getSelectedDocmaNodes(Tree docTree) throws Exception
    {
        int cnt = docTree.getSelectedCount();
        if (DocmaConstants.DEBUG) {
            System.out.println("Selected count: " + cnt);
        }
        if (cnt == 0) return null;

        List list = new ArrayList(cnt);
        Set id_list = new HashSet(2*cnt);
        Iterator it = docTree.getSelectedItems().iterator();
        while (it.hasNext()) {
            Treeitem item = (Treeitem) it.next();
            Object obj = item.getValue();
            if ((obj != null) && (obj instanceof DocmaNode)) {
                DocmaNode node = (DocmaNode) obj;
                id_list.add(node.getId());
                list.add(node);
            }
        }

        List resultlist = new ArrayList(cnt);
        // String parentId = null;
        for (int i=0; i < list.size(); i++) {
            DocmaNode node = (DocmaNode) list.get(i);
            DocmaNode par = node.getParent();
            if (par == null) {
                Messagebox.show("Operation cannot be applied on root folder!");
                return null;
            }
            if (id_list.contains(par.getId())) {
                // If parent is exported, then child nodes will be exported
                // automatically. Therefore If parent is in selection list, then
                // ignore any selected child nodes.
                // This check is needed, because when a range is selected, then
                // all child nodes of a selected node are also included in
                // the list of selected nodes.
                continue;  // exclude this node, because parent is in selection list
            }
            // if (parentId == null) {
            //     parentId = par.getId();
            // } else {
            //     if (! parentId.equals(par.getId())) {
            //         Messagebox.show("Operation can only be applied on content within the same folder!");
            //         return null;
            //     }
            // }
            resultlist.add(node);
        }
        if (resultlist.isEmpty()) return null;
        else return resultlist;
    }

    private static void cleanTempDir(File tempDir)
    {
        if (! tempDir.exists()) return;

        long nowtime = System.currentTimeMillis();
        String[] filenames = tempDir.list();
        for (int i=0; i < filenames.length; i++) {
            String fn = filenames[i];
            String timestr = null;
            if (fn.startsWith(IMPORT_DIR_PREFIX)) {
                timestr = fn.substring(IMPORT_DIR_PREFIX.length());
                File del_file = new File(tempDir, fn);
                try {
                    long filetime = Long.parseLong(timestr);
                    if ((nowtime - filetime) > (48*60*60*1000)) {  // delete files older than 48h
                        DocmaUtil.recursiveFileDelete(del_file);
                    }
                } catch (Exception ex) {
                    Log.warning("Could not delete files in temporary directory: " + del_file);
                }
            }
        }
    }

}
