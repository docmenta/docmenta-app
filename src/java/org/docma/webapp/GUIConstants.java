/*
 * GUIConstants.java
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

import org.docma.plugin.web.DefaultContentAppHandler;

/**
 *
 * @author MP
 */
public class GUIConstants
{
    public static final int MODAL_CANCEL = 0;
    public static final int MODAL_OKAY = 1;

    public static final long LOCK_TIMEOUT = 10*60*1000;  // 10 minutes

    // Web-Application property names
    public static final String PROP_APP_THEME = "app.theme";
    public static final String PROP_APP_THEME_VERSION_RELATION = "app.theme.versionrelation";
    public static final String PROP_APP_ASSIGNMENT_EDITORS = "app.assignments.editors";
    public static final String PROP_APP_ASSIGNMENT_VIEWERS = "app.assignments.viewers";
    
    // Web-Application property values
    public static final String APP_THEME_DEFAULT_ID = "BlackUI";
    
    // User property names
    public static final String PROP_USER_CONTENT_WIN_LAYOUT = "contentwinlayout";
    public static final String PROP_USER_CONTENT_TREE_SIZE_HORIZONTAL = "contenttreesize.horizontal";
    public static final String PROP_USER_CONTENT_TREE_SIZE_VERTICAL = "contenttreesize.vertical";
    public static final String PROP_USER_CONTENT_TREE_COL_WIDTH_HORIZONTAL = "contenttreecolwidth.horizontal";
    public static final String PROP_USER_CONTENT_TREE_COL_WIDTH_VERTICAL = "contenttreecolwidth.vertical";
    public static final String PROP_USER_PREVIEW_FORMAT = "previewformat";
    public static final String PROP_USER_PREVIEW_PUBCONF = "previewpubconf";
    public static final String PROP_USER_PREVIEW_OUTCONF_HTML = "previewoutconfhtml";
    public static final String PROP_USER_PREVIEW_OUTCONF_PDF = "previewoutconfpdf";
    public static final String PROP_USER_PREVIEW_PATH_OPENED = "previewpath.opened";
    public static final String PROP_USER_EDIT_WIN_WIDTH = DefaultContentAppHandler.USER_PROPERTY_EDIT_WIN_WIDTH;
    public static final String PROP_USER_EDIT_WIN_HEIGHT = DefaultContentAppHandler.USER_PROPERTY_EDIT_WIN_HEIGHT;
    public static final String PROP_USER_IMAGE_PREVIEW_MODE = "imagepreviewmode";
    public static final String PROP_USER_EDITOR_ID = "editor.id";
    public static final String PROP_USER_QUICKLINKS_ENABLED = "quicklinks.enabled";

    // User property values
    public static final String CONTENT_WIN_LAYOUT_HORIZONTAL = "horizontal";
    public static final String CONTENT_WIN_LAYOUT_VERTICAL = "vertical";
    public static final String CONTENT_TREE_DEFAULT_SIZE = "50%";
    public static final int EDIT_WIN_DEFAULT_WIDTH = DefaultContentAppHandler.EDITOR_DEFAULT_WIDTH;
    public static final int EDIT_WIN_DEFAULT_HEIGHT = DefaultContentAppHandler.EDITOR_DEFAULT_HEIGHT;
    public static final String IMAGE_PREVIEW_MODE_GALLERY = "gallery";
    public static final String IMAGE_PREVIEW_MODE_GALLERY_BIG = "gallery_big";
    public static final String IMAGE_PREVIEW_MODE_LIST = "list";
    public static final String IMAGE_PREVIEW_MODE_LIST_BIG = "list_big";
    public static final String CONTENT_EDITOR_DEFAULT_ID = "tinymce_3_5_11_b";
    public static final String TEXT_EDITOR_DEFAULT_ID = "internal_text_editor_1_0";

}
