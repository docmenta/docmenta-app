/*
 * UploadHandler.java
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

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.util.*;

import org.docma.app.*;
import org.docma.coreapi.*;
import org.docma.util.DocmaUtil;
import org.docma.util.ZipUtil;
import org.docma.util.Log;

import org.zkoss.zul.*;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.event.*;

/**
 *
 * @author MP
 */
public class UploadHandler implements UploadCallback
{
    private MainWindow mainWin;
    private DropUploadDialog uploadDialog;
    private FileOverwriteDialog overwriteDialog;
    private DocI18n i18n;
    private File tempDir;
    private DocmaSession docmaSess;
    // private DocmaNode folder;
    
    // Flags to indicate that user has clicked "Same for all"
    private int resolve_all_same_folder = -1;     // conflict in same folder
    private int resolve_all_other_folder = -1;    // alias conflict in other folder
    private int resolve_all_invalid_alias = -1;   // invalid alias conflict
    private int resolve_all_folder_conflict = -1; // folder with same name exists

    private boolean cancelUpload = false;
    private String transMode;
    private String language_suffix;

    public UploadHandler(MainWindow mainWin)
    {
        this.mainWin = mainWin;
        this.uploadDialog = (DropUploadDialog) mainWin.getPage().getFellow("DropUploadDialog");
        this.overwriteDialog = (FileOverwriteDialog) mainWin.getPage().getFellow("FileOverwriteDialog");
        DocmaWebApplication webApp = GUIUtil.getDocmaWebApplication(mainWin);
        this.i18n = webApp.getI18n();
        this.tempDir = webApp.getTempDirectory();
    }

    public boolean doUpload(DocmaNode folder, DocmaSession docmaSess) throws Exception
    {
        // this.folder = folder;
        this.docmaSess = docmaSess;
        if (! (folder.isImageFolder() || folder.isSystemFolder())) {
            throw new DocRuntimeException("Cannot upload file: invalid node type!");
        }
        resolve_all_same_folder = -1;
        resolve_all_other_folder = -1;
        resolve_all_invalid_alias = -1;
        resolve_all_folder_conflict = -1;
        transMode = docmaSess.getTranslationMode();
        language_suffix = null;
        if (transMode != null) {
            language_suffix = "[" + transMode.toUpperCase() + "]";
        }
        
        int cnt = uploadDialog.showUploadDialog(mainWin, this, folder);
        if (cnt > 0) {
            Clients.evalJavaScript("window.frames['viewcontentfrm'].location.reload();");
            return true;
        } else {
            return false;
        }
        // String message = i18("label.upload.selectfile") + ": ";
        // String title = i18("label.upload.dialog.title");
        // Media[] medias = Fileupload.get(message, title, 1000, true);
        // if ((medias != null) && (medias.length > 0)) {
        //     storeMedia(folder, medias);
        //     Clients.evalJavaScript("window.frames['viewcontentfrm'].location.reload();");
        //     return true;
        // } else {
        //     return false;
        // }
    }

    //    public EventListener createUploadListener(DocmaNode folder, DocmaSession doc_sess)
    //    {
    //        final DocmaSession upload_sess = doc_sess;
    //        final DocmaNode upload_folder = folder;
    //        return new EventListener() {
    //            public void onEvent(Event evt) throws Exception 
    //            {
    //                docmaSess = upload_sess;
    //                Log.info("Upload Handler event: " + evt.getName() + 
    //                         ", " + upload_folder.getId() + 
    //                         ", " + docmaSess.getSessionId());
    //                if (evt instanceof UploadEvent) {
    //                    Media[] meds = ((UploadEvent) evt).getMedias();
    //                    storeMedia(upload_folder, meds);
    //                    Clients.evalJavaScript("parent.previewRefresh();");
    //                } else {
    //                    Log.warning("Unexpected event type: " + evt.getClass().getName());
    //                }
    //            }
    //        };
    //    }

    // public void onUploadFile(ForwardEvent fe) throws Exception
    // {
    //     UploadEvent ue = (UploadEvent) fe.getOrigin();
    //     Media[] meds = ue.getMedias();
    //     storeMedia(meds);
    // }

    
    public DocmaNode[] storeMedia(DocmaNode folder, Media[] meds) throws Exception
    {
        cancelUpload = false;
        List<DocmaNode> res = new ArrayList<DocmaNode>(meds.length);
        for (int i=0; i < meds.length; i++) {
            Media med = meds[i];
            String ctype = med.getContentType();
            String format = med.getFormat();
            String filename = med.getName();
            if ((filename != null) && filename.trim().equals("")) filename = null;
            if (filename == null) {
                Messagebox.show("Invalid filename!");
                continue;
            }
            Object obj = storeNode(folder, med, filename, format, ctype, true);
            if (obj != null) {
                if (obj instanceof DocmaNode) {
                    res.add((DocmaNode) obj);
                } else 
                if (obj instanceof List) {
                    res.addAll((List) obj);
                }
            }
            if (cancelUpload) break;
        }
        return res.toArray(new DocmaNode[res.size()]);
    }
   
    private Object storeNode(DocmaNode folder, Object data, String filename, String format, String ctype, boolean allow_extract) 
    throws Exception
    {
        String name = filename;
        String ext = null;
        int pos = name.lastIndexOf('.');
        if (pos >= 0) {
            ext = name.substring(pos + 1);
            name = name.substring(0, pos);
        }
        if ((ext != null) && ext.trim().equals("")) ext = null;
        if (ctype != null) ctype = ctype.toLowerCase();
        if ((ctype != null) && (ext == null)) {
            ext = ImageUtil.guessExtByMIMEType(ctype);
        }
        boolean isImage = ImageUtil.isSupportedImageExtension(ext);

        if (isImage) {
            if (ext != null) ext = ext.toLowerCase();
            if (ext == null) {
                ext = format;
            }
            if ((ctype == null) && (ext != null)) {
                ctype = ImageUtil.guessMIMETypeByExt(ext);
            }
            if (ctype == null) {
                // throw new DocException("Uploaded file has unknown MIME type.");
                Messagebox.show("Uploaded file '" + name + "' has unknown MIME type.");
                return null;
            }
            String img_alias = extractAliasFromName(name);
            if (! img_alias.matches(DocmaConstants.REGEXP_ALIAS)) {
                String new_alias = resolveInvalidImageAlias(img_alias);
                if (new_alias == null) {  // skip image
                    return null;
                } else {
                    // img_alias = new_alias;
                    if (transMode == null) {
                        name = new_alias;
                    } else {
                        name = new_alias + language_suffix;
                    }
                    filename = name + ((ext != null) ? ("." + ext) : "");
                }
            }
        }

        boolean is_zip = ext.equalsIgnoreCase("zip");
        boolean extract_zip = false;
        boolean extract_into_folder = false;
        if (allow_extract && is_zip) {
            // if zip is added, ask user if zip file shall be extracted
            String extract_msg = i18("label.upload.extract.prompt", filename );
            String extract_title = i18("label.upload.extract.title");
            int extract_btn = Messagebox.show(extract_msg, extract_title, 
                                              Messagebox.YES | Messagebox.NO, 
                                              Messagebox.QUESTION);
            extract_zip = (extract_btn == Messagebox.YES);
            if (extract_zip) {
                String extrsub_msg = i18("label.upload.extractsub.prompt", name ) +
                                     "\n \n" + i18("label.upload.extractsub.comment");
                String extrsub_title = i18("label.upload.extractsub.title");
                int extrsub_btn = Messagebox.show(extrsub_msg, extrsub_title, 
                                                  Messagebox.YES | Messagebox.NO, 
                                                  Messagebox.QUESTION);
                extract_into_folder = (extrsub_btn == Messagebox.YES);
            }
        }
        if (extract_zip) {
            return extractZip(folder, getInputStream(data), extract_into_folder ? name : null);
        }

        DocmaNode node = hasConflict(folder, name, ext, isImage);
        if (node != null) { // conflict: file with this name/alias already exists
            String newname = resolveNameConflict(folder, name, ext, isImage, node);
            if (newname == null) {
                node = null;
                return null;  // do not overwrite; skip file
            } else
            if (! newname.equals(name)) {  // conflict resolved by renaming
                name = newname;
                node = null;  // do not overwrite, but create new file with new name
            } else {
                // overwrite file (node != null)
            }
        }

        boolean add_file = (node == null);
        if (add_file && (transMode != null)) {  // adding a file in translation mode is not allowed
            showTransModeError(name);
            cancelUpload = true;
            return null;
        }

        boolean local_transact = !docmaSess.runningTransaction();
        try {
            if (local_transact) docmaSess.startTransaction();

            // create node
            if (add_file) {
                if (isImage) {
                    node = docmaSess.createImageContent();
                    node.setProgress(100);
                    // node.setWorkflowStatus("wip");
                } else {
                    node = docmaSess.createFileContent();
                }
            }

            // write content
            if (isImage) {
                String alias = extractAliasFromName(name);
                node.setImageContentStream(getInputStream(data), alias, ctype, ext);
            } else {
                String fname = extractTitleFromName(name);
                if (transMode != null) {
                    fname = fname + language_suffix;
                }
                if ((ext != null) && (ext.length() > 0)) fname += "." + ext;
                node.setFileContentStream(getInputStream(data), fname);
            }

            // insert node
            if (add_file) {
                int ins_pos = folder.getDefaultInsertPos(node);
                folder.insertChild(ins_pos, node);
            }

            if (local_transact) docmaSess.commitTransaction();
            
            return node;
        } catch (Exception ex) {
            if (local_transact) docmaSess.rollbackTransaction();
            ex.printStackTrace();
            throw ex;
        }
    }
    
    private InputStream getInputStream(Object data)
    {
        if (data instanceof Media) {
            return ((Media) data).getStreamData();
        } else
        if (data instanceof InputStream) {
            return (InputStream) data;
        }
        throw new DocRuntimeException("Unknown data class '" + data.getClass().getName() + "'.");
    }

    private List<DocmaNode> extractZip(DocmaNode folder, InputStream in_stream, String subfolder_name) 
    throws Exception
    {
        long now = System.currentTimeMillis();
        File extractDir;
        do {
            extractDir = new File(tempDir, "extract" + now++);
        } while (extractDir.exists());

        try {
            File extDir = (subfolder_name == null) ? extractDir : new File(extractDir, subfolder_name);
            extDir.mkdirs();
            ZipUtil.extractZipStream(in_stream, extDir);
            docmaSess.startTransaction();
            List<DocmaNode> res = storeFilesFromDirectory(folder, extractDir, true);
            docmaSess.commitTransaction();
            return res;
        } catch (Exception ex) {
            docmaSess.rollbackTransaction();
            ex.printStackTrace();
            throw ex;
        } finally {
            try {
                DocmaUtil.recursiveFileDelete(extractDir);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
        }
    }
    
    private List<DocmaNode> storeFilesFromDirectory(DocmaNode folder, File dir, boolean checkFolderExists)
    throws Exception
    {
        File[] files = dir.listFiles();
        List<DocmaNode> res = new ArrayList<DocmaNode>(files.length);
        ArrayList<File> nodirs = new ArrayList<File>();
        for (File f : files) {
            if (f.isDirectory()) {
                String child_name = f.getName();
                boolean create_folder = true;
                DocmaNode child_folder = null;
                if (checkFolderExists) {
                    DocmaNode nd = folder.getChildByFilename(child_name);
                    if ((nd != null) && nd.isFolder()) {
                        String new_foldername = resolveSameFolderName(folder, child_name);
                        if (new_foldername == null) {  // skip folder
                            continue;
                        } else 
                        if (new_foldername.equals("")) {  // overwrite
                            child_folder = nd;  // use existing folder
                            create_folder = false;
                        } else
                        if (new_foldername.equals(child_name)) {  // replace
                            nd.deleteRecursive();
                        } else {  // rename
                            child_name = new_foldername;
                        }
                    }
                }
                if (create_folder) {
                    if (imageFilenamesOnly(f.list())) {
                        child_folder = docmaSess.createImageFolder();
                    } else {
                        child_folder = docmaSess.createSystemFolder();
                    }
                    child_folder.setTitle(child_name);
                    folder.addChild(child_folder);
                }
                storeFilesFromDirectory(child_folder, f, !create_folder);  // recursive call
                // Note: if new folder was created, then it is not necessary
                // within this folder to check whether a sub-folder to be 
                // created already exists. 
                res.add(child_folder);
            } else {
                nodirs.add(f);
            }
        }
        for (File f : nodirs) {
            InputStream fstream = new FileInputStream(f);
            try {
                Object obj = storeNode(folder, fstream, f.getName(), null, null, false);
                if ((obj != null) && (obj instanceof DocmaNode)) {
                    res.add((DocmaNode) obj);
                }
            } finally {
                try { fstream.close(); } catch (Exception ex) {}
            }
        }
        return res;
    }
    
    private boolean imageFilenamesOnly(String[] filenames)
    {
        for (String fn : filenames) {
            int pos = fn.lastIndexOf('.');
            if (pos < 0) return false;   // no file extension
            
            String ext = fn.substring(pos + 1);
            if (! ImageUtil.isSupportedImageExtension(ext)) return false;
        }
        return true;
    }
    
    private String resolveSameFolderName(DocmaNode parentFolder, String foldername) throws Exception
    {
        int btn;
        if (resolve_all_folder_conflict < 0) { // as long as "same for all" checkbox not checked, ask user
            String msg = "A folder with name '" + foldername + " already exists!";
            btn = overwriteDialog.resolveFolderConflict(msg);
            if (overwriteDialog.isAll()) {
                resolve_all_folder_conflict = btn;
            }
        } else {
            btn = resolve_all_folder_conflict;
        }
        
        if (btn == FileOverwriteDialog.OVERWRITE_BUTTON) {
            return "";  // returning empty string indicates overwrite
        } else
        if (btn == FileOverwriteDialog.REPLACE_BUTTON) {
            return foldername;  // returning same name indicates replace
        } else
        if (btn == FileOverwriteDialog.RENAME_BUTTON) {
            int seq = 2;
            String new_name;
            do {
                new_name = foldername + "_" + seq++;
            } while (parentFolder.getChildByTitle(new_name) != null);
            return new_name;
        } else
        if (btn == FileOverwriteDialog.CANCEL_BUTTON) {
            this.cancelUpload = true;
            return null;  // skip file
        }
        return null;  // skip button
    }

    private String resolveInvalidImageAlias(String invalid_alias) throws Exception
    {
        int btn;
        if (resolve_all_invalid_alias < 0) { // as long as "same for all" checkbox not checked, ask user
            String msg = "Image filename '" + invalid_alias + "' is no valid alias name.";
            final boolean ALLOW_OVERWRITE = false;
            btn = overwriteDialog.resolveFileConflict(msg, ALLOW_OVERWRITE);
            if (overwriteDialog.isAll()) {
                resolve_all_invalid_alias = btn;
            }
        } else {
            btn = resolve_all_invalid_alias;
        }
        
        if (btn == FileOverwriteDialog.RENAME_BUTTON) {
            String valid_alias = DocmaAppUtil.toValidAlias(invalid_alias);
            int seq = 2;
            String new_alias = valid_alias;
            while (docmaSess.getNodeByAlias(new_alias) != null) {
                String suffix = "_" + (seq++);
                if ((valid_alias.length() + suffix.length()) <= DocmaConstants.ALIAS_MAX_LENGTH) {
                    new_alias = valid_alias + suffix;
                } else {
                    new_alias = valid_alias.substring(0, DocmaConstants.ALIAS_MAX_LENGTH - suffix.length()) + suffix;
                }
            }
            return new_alias;
        } else 
        if (btn == FileOverwriteDialog.CANCEL_BUTTON) {
            this.cancelUpload = true;
            return null;  // skip file
        }
        return null;  // skip button
    }

    private String resolveNameConflict(DocmaNode folder, String name, String ext, boolean isImage, DocmaNode node) throws Exception
    {
        DocmaNode par = node.getParent();
        boolean conflict_same_folder = (par != null) && folder.getId().equals(par.getId());
        boolean conflict_other_folder = !conflict_same_folder;

        if (transMode != null) {
            if (conflict_other_folder) {
                showTransModeError(name);
                cancelUpload = true;
                return null;  // skip file; in translation mode only overwrite is allowed
            }
            resolve_all_same_folder = FileOverwriteDialog.OVERWRITE_BUTTON;
        }

        int btn;
        boolean allow_overwrite = conflict_same_folder;
        if (conflict_same_folder) {
            if (resolve_all_same_folder < 0) {  // as long as "same for all" checkbox not checked, ask user
                String msg;
                if (isImage) {
                    msg = "An image with alias '" + name + "' already exists in this folder.";
                } else {
                    msg = "A file with name '" + name + "' already exists in this folder.";
                }
                btn = overwriteDialog.resolveFileConflict(msg, allow_overwrite);
                if (overwriteDialog.isAll()) {
                    resolve_all_same_folder = btn;
                }
            } else {  // "same for all" checkbox checked or translation mode
                btn = resolve_all_same_folder;
            }
        } else {  // conflict_other_folder
            if (resolve_all_other_folder < 0) {
                String msg;
                if (isImage) {
                    msg = "An image with alias '" + name + "' already exists in another folder.";
                } else {
                    msg = "A file with name '" + name + "' already exists in another folder.";
                }
                btn = overwriteDialog.resolveFileConflict(msg, allow_overwrite);
                if (btn == FileOverwriteDialog.OVERWRITE_BUTTON) {  // should not occur as overwrite button is disabled 
                    Messagebox.show("Overwrite not allowed!");
                    btn = FileOverwriteDialog.SKIP_BUTTON;
                }
                if (overwriteDialog.isAll()) {
                    resolve_all_other_folder = btn;
                }
            } else {
                btn = resolve_all_other_folder;
            }
        }

        if (btn == FileOverwriteDialog.OVERWRITE_BUTTON) {
            return name;  // return name unchanged -> overwrite node
        } else
        if (btn == FileOverwriteDialog.RENAME_BUTTON) {
            int seq = 2;
            String new_name = name;
            do {
                new_name = name + "_" + (seq++);
            } while (hasConflict(folder, new_name, ext, isImage) != null);
            return new_name;   // return new name
        } else 
        if (btn == FileOverwriteDialog.CANCEL_BUTTON) {
            this.cancelUpload = true;
            return null;  // skip file
        }
        return null;   // skip button
    }

    private DocmaNode hasConflict(DocmaNode folder, String name, String ext, boolean isImage)
    {
        DocmaNode node = null;
        if (isImage) {
            String alias = extractAliasFromName(name);
            node = folder.getChildByAlias(alias);
            if (node == null) {
                node = docmaSess.getNodeByAlias(alias);
            }
            return node;
        } else {
            node = folder.getChildByTitleAndExtension(name, ext);
            return node;
        }
    }

    private void showTransModeError(String name) throws Exception
    {
        Messagebox.show("Adding a new file is not allowed in translation mode: " + name);
    }

    private String extractAliasFromName(String name)
    {
        String alias = name;
        if (transMode != null) {
            if (name.toUpperCase().endsWith(language_suffix)) {
                alias = name.substring(0, name.length() - language_suffix.length());
            }
        }
        return alias;
    }

    private String extractTitleFromName(String name)
    {
        String tit = name;
        if (transMode != null) {
            if (name.toUpperCase().endsWith(language_suffix)) {
                tit = name.substring(0, name.length() - language_suffix.length());
            }
        }
        return tit;
    }

    private String i18(String key) 
    {
        return i18n.getLabel(key);
    }
    
    String i18(String key, Object... args)
    {
        return i18n.getLabel(key, args);
    }

}
