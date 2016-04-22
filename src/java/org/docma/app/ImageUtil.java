/*
 * ImageUtil.java
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

/**
 *
 * @author MP
 */
public class ImageUtil
{
    private static final String[][] IMG_FILE_TYPES = {
        {"bmp", "image/bmp"},
        {"cgm", "image/cgm"},
        {"gif", "image/gif"},
        {"ief", "image/ief"},
        {"jpeg", "image/jpeg"},
        {"jpg", "image/jpeg"},
        {"jpe", "image/jpeg"},
        {"png", "image/png"},
        {"svg", "image/svg+xml"},
        {"tiff", "image/tiff"},
        {"tif", "image/tiff"}
    };

    public static String guessMIMETypeByExt(String ext)
    {
        for (int i=0; i < IMG_FILE_TYPES.length; i++) {
            if (ext.equalsIgnoreCase(IMG_FILE_TYPES[i][0])) return IMG_FILE_TYPES[i][1];
        }
        return null;
    }

    public static String guessExtByMIMEType(String mime_type)
    {
        for (int i=0; i < IMG_FILE_TYPES.length; i++) {
            if (mime_type.equals(IMG_FILE_TYPES[i][1])) return IMG_FILE_TYPES[i][0];
        }
        return null;
    }

    public static boolean isSupportedImageExtension(String ext)
    {
        if ((ext == null) || ext.equals("")) return false;
        else return guessMIMETypeByExt(ext) != null;
    }

}
