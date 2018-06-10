/*
 * StoreHelper.java
 * 
 *  Copyright (C) 2018  Manfred Paula, http://www.docmenta.org
 *   
 *  This file is part of Docmenta. Docmenta is free software: you can 
 *  redistribute it and/or modify it under the terms of the GNU Lesser 
 *  General Public License as published by the Free Software Foundation, 
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Docmenta.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.docma.plugin.implementation;

import java.io.*;
import org.docma.app.DocmaAppUtil;
import org.docma.app.DocmaConstants;
import org.docma.app.DocmaSession;
import org.docma.app.ImageUtil;
import org.docma.plugin.FileContent;
import org.docma.plugin.Folder;
import org.docma.plugin.FolderType;
import org.docma.plugin.Group;
import org.docma.plugin.Node;

import org.docma.plugin.StoreConnection;

/**
 *
 * @author MP
 */
public class StoreHelper 
{
    /**
     * Private constructor to avoid the creation of instances. 
     * This class only provides static utility methods.
     */
    private StoreHelper() 
    {
    }

    /**
     * Adds a new image to the store. The image is stored in the image folder
     * identified by <code>contextNodeId</code>. If <code>contextNodeId</code>
     * is no image folder, then the image folder next to  
     * <code>contextNodeId</code> is used.
     * If no such image folder exists, then an image folder is created (next to 
     * <code>contextNodeId</code>).
     * 
     * @param filename   the original image filename
     * @param imageStream   the image data
     * @param storeConn  the store connection
     * @param contextNodeId  the image folder location
     * @return  the assigned image alias
     * @throws Exception 
     */
    public static String saveImageFile(String filename, 
                                      InputStream imageStream, 
                                      StoreConnection storeConn, 
                                      String contextNodeId) throws Exception
    {
        String name = filename;
        String ext = null;
        int pos = name.lastIndexOf('.');
        if (pos >= 0) {
            ext = name.substring(pos + 1);
            name = name.substring(0, pos);
        }
        if ((ext == null) || ext.trim().equals("")) {
            throw new Exception("Upload failed: Missing filename extension.");
        }
        boolean isImage = ImageUtil.isSupportedImageExtension(ext);
        if (! isImage) {
            throw new Exception("Unsupported image extension: " + ext);
        }
        
        String transMode = storeConn.getTranslationMode();
        // String languageSuffix = null;
        if (transMode != null) {
            // language_suffix = "[" + transMode.toUpperCase() + "]";
            throw new Exception("Upload of new images is not allowed in translation mode.");
        }
                
        String imgAlias = name;  // extractAliasFromName(name, languageSuffix);
        boolean isValid = imgAlias.matches(DocmaConstants.REGEXP_ALIAS);
        if (isValid) {
            if (storeConn.getNodeIdByAlias(imgAlias) != null) {
                throw new Exception("Could not upload image, because a node with same alias already exists: " + imgAlias);
            }
        } else {
            // If name is no valid alias, then create a valid new alias.
            // Note that it has to be taken care, that the generated alias 
            // is not already used by any other node.
            imgAlias = validNewAlias(imgAlias, storeConn);
        }
        filename = imgAlias + "." + ext;
        
        Folder fold = getNearestImageFolder(storeConn, contextNodeId);
        FileContent imgNode = storeConn.createFileContent(filename, true);
        imgNode.setContentStream(imageStream);
        fold.addChildren(imgNode);
        
        return imgAlias;
    }
    
    /**
     * Returns an unused alias that is equal or similar to 
     * <code>proposedAlias</code>. If <code>proposedAlias</code> is 
     * syntactically invalid or a node with this alias already exists, then 
     * this method returns a generated alias that is similar to 
     * <code>proposedAlias</code> (but which is syntactically valid and 
     * not already used by any other node). 
     * Otherwise <code>proposedAlias</code> is returned.
     * <p>
     * Implementation note: In case <code>proposedAlias</code> is already used 
     * as alias, then a sequence number is appended to generate a new alias.
     * Furthermore, invalid characters are replaced by underscore.
     * However, the implementation may be subject of change. Therefore, the
     * caller should not assume any convention for the returned alias, 
     * except that the alias is syntactically valid and unused in the 
     * context of the given store connection (<code>storeConn</code>).
     * </p>
     */
    public static String validNewAlias(String proposedAlias, StoreConnection storeConn)
    {
        String valid_alias = DocmaAppUtil.toValidAlias(proposedAlias);
        int seq = 2;
        String new_alias = valid_alias;
        while (storeConn.getNodeIdByAlias(new_alias) != null) {
            String suffix = "_" + (seq++);
            if ((valid_alias.length() + suffix.length()) <= DocmaConstants.ALIAS_MAX_LENGTH) {
                new_alias = valid_alias + suffix;
            } else {
                new_alias = valid_alias.substring(0, DocmaConstants.ALIAS_MAX_LENGTH - suffix.length()) + suffix;
            }
        }
        return new_alias;
    }

    public static Folder getNearestImageFolder(StoreConnection con, String nodeId) throws Exception
    {
        Node node = con.getNodeById(nodeId);
        if (node != null) {
            Folder res = null;
            Node parent = node.getParent();
            if (parent instanceof Group) {
                res = getOrCreateImageSubFolder(con, (Group) parent);
            } else {
                res = getMediaRoot(con);
            }
            if (res == null) {
                throw new Exception("Could not get image folder!");
            }
            return res;
        } else {
            throw new Exception("Non-existing node ID '" +  nodeId + "'!");
        }
    }
    
    public static Folder getOrCreateImageSubFolder(StoreConnection con, Group parent)
    {
        // Return first image folder (if any)
        for (Node nd : parent.getChildren()) {
            if (nd instanceof Folder) {
                Folder fold = (Folder) nd;
                if (FolderType.IMAGE.equals(fold.getFolderType())) {
                    return fold;
                }
            }
        }
        
        // No image folder exists. Create image folder.
        String fname = con.getUserSession().getLabel("label.imagefolder.title");
        Folder fold = con.createFolder(fname, FolderType.IMAGE);
        parent.insertChildren(0, fold);
        return fold;
    }
    
    public static Folder getMediaRoot(StoreConnection con)
    {
        return (Folder) con.getNodeByAlias(DocmaSession.MEDIA_ROOT_ALIAS);
    }

}
