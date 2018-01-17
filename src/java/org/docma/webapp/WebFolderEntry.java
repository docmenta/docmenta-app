/*
 * WebFolderEntry.java
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
package org.docma.webapp;

import java.util.Date;
import org.docma.app.DocmaNode;
import org.docma.app.ui.FolderEntry;

/**
 *
 * @author MP
 */
public class WebFolderEntry extends FolderEntry
{
    private final String deskid;
    private final String servMediaBasePath;
    private long url_lastmod = -1;
    
    public WebFolderEntry(DocmaNode node, String deskid, String servMediaBasePath) 
    {
        super(node);
        this.deskid = deskid;
        this.servMediaBasePath = servMediaBasePath;
    }

    public String getThumbURL(ThumbDimensions thumbDim)
    {
        return "thumb.jsp?desk=" + deskid +
               "&nodeid=" + getNodeId() +
               "&lastmod=" + getURLLastMod() + 
               "&size=" + thumbDim.getSize();
    }
    
    public String getImgURL()
    {
        return "image.jsp?desk=" + deskid +
               "&nodeid=" + getNodeId() +
               "&lastmod=" + getURLLastMod();
    }
    
    public String getDownloadURL()
    {
        return servMediaBasePath + "/download/" + getNodeId() + "/" +
               getURLLastMod() + "/" + getFileName();
    }
    
    private long getURLLastMod()
    {
        if (url_lastmod < 0) {
            Date lastdate = getLastModifiedDate();
            url_lastmod = (lastdate != null) ? lastdate.getTime() : System.currentTimeMillis();
        }
        return url_lastmod;
    }

}
