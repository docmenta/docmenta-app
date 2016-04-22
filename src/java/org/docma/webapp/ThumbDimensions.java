/*
 * ThumbDimensions.java
 * 
 *  Copyright (C) 2014  Manfred Paula, http://www.docmenta.org
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

/**
 *
 * @author MP
 */
public class ThumbDimensions 
{
    public static final int SIZE_NORMAL = 128;
    public static final int SIZE_BIG = 256;
    
    private static final ThumbDimensions THUMB_NORMAL = new ThumbDimensions(SIZE_NORMAL, 130, 158);
    private static final ThumbDimensions THUMB_BIG = new ThumbDimensions(SIZE_BIG, 260, 286);
    
    private int thumbSize;
    private int thumbBoxWidth;
    private int thumbBoxHeight;

    public ThumbDimensions(int thumb_size, int box_width, int box_height) 
    {
        this.thumbSize = thumb_size;
        this.thumbBoxWidth = box_width;
        this.thumbBoxHeight = box_height;
    }

    public static ThumbDimensions getDimensionsForViewMode(String mode)
    {
        if (mode == null) {
            mode = "";  // use normal size as default
        }
        boolean isGalleryBig = mode.equalsIgnoreCase(GUIConstants.IMAGE_PREVIEW_MODE_GALLERY_BIG);
        boolean isListBig    = mode.equalsIgnoreCase(GUIConstants.IMAGE_PREVIEW_MODE_LIST_BIG);
        if (isGalleryBig || isListBig) {
            return THUMB_BIG;
        } else {
            return THUMB_NORMAL;
        }
    }

    public int getSize() 
    {
        return thumbSize;
    }

    public int getBoxWidth() 
    {
        return thumbBoxWidth;
    }

    public int getBoxHeight() 
    {
        return thumbBoxHeight;
    }
    
    
}
