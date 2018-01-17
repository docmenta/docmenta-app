/*
 * VideoUtil.java
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
package org.docma.app;

/**
 *
 * @author MP
 */
public class VideoUtil 
{
    private static final String[][] VIDEO_FILE_TYPES = {
        {"mp4",  "video/mp4"},
        {"webm", "video/webm"},
        {"ogg",  "video/ogg"}
    };
    
    public static String guessMIMETypeByExt(String ext)
    {
        for (int i=0; i < VIDEO_FILE_TYPES.length; i++) {
            if (ext.equalsIgnoreCase(VIDEO_FILE_TYPES[i][0])) return VIDEO_FILE_TYPES[i][1];
        }
        return null;
    }

    public static boolean isSupportedVideoExtension(String ext)
    {
        if ((ext == null) || ext.equals("")) return false;
        else return guessMIMETypeByExt(ext) != null;
    }
}
