/*
 * ImageRenditions.java
 * 
 *  Copyright (C) 2016  Manfred Paula, http://www.docmenta.org
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

import org.docma.coreapi.DocImageRendition;

/**
 * Pre-defined image renditions.
 *
 * @author MP
 */
public class ImageRenditions 
{
    public static final String NAME_THUMB_DEFAULT = "thumb";
    public static final String NAME_THUMB_BIG = "thumb_big";
    
    // private static DocImageRendition thumb_small = null;
    private static DocImageRendition thumb_default = null;
    private static DocImageRendition thumb_big = null;

    static {
        try {
            thumb_default = new DocImageRendition(NAME_THUMB_DEFAULT, 
                                                  DocImageRendition.FORMAT_PNG,
                                                  ThumbDimensions.SIZE_NORMAL, 
                                                  ThumbDimensions.SIZE_NORMAL);
            thumb_big = new DocImageRendition(NAME_THUMB_BIG, 
                                              DocImageRendition.FORMAT_PNG,
                                              ThumbDimensions.SIZE_BIG, 
                                              ThumbDimensions.SIZE_BIG);
        } catch (Exception ex) {  // should never occur
            ex.printStackTrace();
        }
    }

    /**
     * Returns information about the rendition identified by the given
     * rendition name.
     * 
     * @param renditionName  the rendition name
     * @return  the rendition data for the given name
     */    
    public static DocImageRendition getImageRenditionInfo(String renditionName)
    {
        if (renditionName.equalsIgnoreCase(NAME_THUMB_DEFAULT)) {
            return thumb_default;
        } else if (renditionName.equalsIgnoreCase(NAME_THUMB_BIG)) {
            return thumb_big;
        } else {
            return null;
        }
    }
 
    /**
     * Returns the list of pre-defined thumb rendition names.
     * The list is sorted by thumb size.
     * 
     * @return  names of pre-defined thumb renditions
     */
    public static String[] listThumbRenditionNames()
    {
        return new String[] { NAME_THUMB_DEFAULT, NAME_THUMB_BIG };
    }
}
