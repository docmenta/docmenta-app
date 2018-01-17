/*
 * FolderEntry.java
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
package org.docma.app.ui;

import java.util.Date;

import org.docma.app.DocmaNode;
import org.docma.app.VideoUtil;
import org.docma.util.DocmaUtil;

/**
 *
 * @author MP
 */
public class FolderEntry 
{
    private final DocmaNode node;
    private final String nodeId;
    private final boolean is_file;
    private final boolean is_image;
    private String file_name = null;
    private String file_ext = null;
    private long file_size = -1;
    private Date lastmodified_date = null;
    
    public FolderEntry(DocmaNode node) 
    {
        this.node = node;
        this.nodeId = node.getId();
        is_file = node.isFileContent();
        is_image = node.isImageContent();
    }
    
    public String getNodeId()
    {
        return nodeId;
    }
    
    public boolean isFile()
    {
        return is_file;
    }
    
    public boolean isImage()
    {
        return is_image;
    }
    
    public boolean isVideo() 
    {
        return isFile() ? VideoUtil.isSupportedVideoExtension(getFileExtension()) : false;
    }
    
    public String getFileName()
    {
        if (file_name == null) {
            file_name = node.getDefaultFileName();
        }
        return file_name;
    }
    
    public String getFileExtension()
    {
        if (file_ext == null) {
            file_ext = node.getFileExtension();
            if (file_ext == null) {
                file_ext = "";
            }
            if (file_ext.startsWith(".")) { 
                file_ext = file_ext.substring(1);
            }
        }
        return file_ext;
    }
    
//    public String getFormat()
//    {
//        return getFileExtension().toUpperCase();
//    }
    
    public long getFileSize()
    {
        if (file_size < 0) {
            file_size = node.getContentLength();
        }
        return file_size;
    }
    
    public String getFileSizeFormatted()
    {
        return DocmaUtil.formatByteSize(getFileSize());
    }
    
    public Date getLastModifiedDate()
    {
        if (lastmodified_date == null) {
            lastmodified_date = node.getLastModifiedDate();
        }
        return lastmodified_date;
    }
    
    public String getAlias()
    {
        return node.getAlias();
    }
    
    public String getTitleEntityEncoded()
    {
        String title = node.getTitleEntityEncoded();
        return (title == null) ? "" : title; 
    }
    
    public boolean isTranslated()
    {
        return node.isTranslated();
    }
    
}
