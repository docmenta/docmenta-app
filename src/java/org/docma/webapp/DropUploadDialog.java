/*
 * DropUploadDialog.java
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

import java.util.Date;

import org.docma.app.*;
import org.docma.util.Log;
import org.docma.util.DocmaUtil;

import org.zkoss.zul.*;
import org.zkoss.zk.ui.event.*;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Execution;
import org.zkoss.util.media.Media;
import org.zkoss.zkmax.zul.Dropupload;

/**
 *
 * @author MP
 */
public class DropUploadDialog extends Window
{
    private static final String DROP_MSG_START = 
        "<div style=\"width:100%;height:100%;font-size:1.3em;color:#484848;font-family:Arial,Verdana,sans-serif;\" align=\"center\"><b>";
    private static final String DROP_MSG_END = 
        "</b></div>";
    
    // private int modalResult = -1;
    private MainWindow mainWin;
    private Dropupload dropBox;
    private UploadCallback uploadHandler;
    private DocmaNode uploadFolder;
    private boolean isImageFolder = false;
    private ThumbDimensions thumbDim = null;
    private int uploadCounter = 0;
    private StringBuilder uploadMessage = new StringBuilder();

    public int showUploadDialog(MainWindow main_win, UploadCallback upHandler, DocmaNode folder)
    {
        mainWin = main_win;
        uploadHandler = upHandler;
        uploadFolder = folder;
        isImageFolder = folder.isImageFolder();
        uploadCounter = 0;
        uploadMessage.setLength(0);  // clear
        init();

        DocmaSession docmaSess = mainWin.getDocmaSession();
        String viewmode = docmaSess.getUserProperty(GUIConstants.PROP_USER_IMAGE_PREVIEW_MODE);
        thumbDim = ThumbDimensions.getDimensionsForViewMode(viewmode);
        
        String msg = DROP_MSG_START + mainWin.i18n("label.upload.ordropfiles") + DROP_MSG_END;
        dropBox.setContent(msg);  // initial message in drop box
        dropBox.invalidate();
        
        // modalResult = -1;
        doModal();
        
        return uploadCounter;
    }
    
    public void onUploadFile(UploadEvent evt) throws Exception
    {
        if (DocmaConstants.DEBUG) {
            Log.info("onUploadFile. NodeId: " + uploadFolder.getId());
        }
        String box_css = "float: left; height:" + thumbDim.getBoxHeight() + 
                         "px; width:" + thumbDim.getBoxWidth() + 
                         "px; padding: 10px; margin: 5px 3px; background: white; " +
                         "border: 1px solid; border-color: #AAA #444 #444 #AAA;";
        String viewport_css = "width:" + thumbDim.getSize() + 
                              "px; height:" + thumbDim.getSize() + "px; margin:0; padding:0;";
        String img_css = "border: 1px solid; border-color: #444 #AAA #AAA #444; margin:6px 0 0 0;";
        String file_css = "border-width:0px; margin:6px 0 0 0;";
        String title_css = "margin:0; padding:0 0 2px 0;";
        
        Media[] medias = evt.getMedias();
        // StringBuilder sb = new StringBuilder();
        // for (Media m : medias) {
        //     sb.append(m.getName() + "<br />\n");
        // }
        // Log.info(sb.toString());
        
        DocmaNode[] upNodes = uploadHandler.storeMedia(uploadFolder, medias);
        uploadCounter += upNodes.length;
        
        Desktop desk = mainWin.getDesktop();
        Execution exec = desk.getExecution();
        String deskid = desk.getId();
        for (int i=0; i < upNodes.length; i++) {
            DocmaNode nd = upNodes[i];
            if (nd == null) {
                continue;
            }
            String size_str = DocmaUtil.formatByteSize(nd.getContentLength());
            if (isImageFolder) {
                String thumb_url;
                boolean is_img = nd.isImageContent();
                if (is_img) {
                    Date lastmoddate = nd.getLastModifiedDate();
                    long lastmod = (lastmoddate != null) ? lastmoddate.getTime() : System.currentTimeMillis();
                    thumb_url = exec.encodeURL("thumb.jsp?desk=" + deskid +
                                               "&nodeid=" + nd.getId() +
                                               "&lastmod=" + lastmod + 
                                               "&size=" + thumbDim.getSize());
                } else
                if (nd.isFolder()) {
                    thumb_url = "img/folder.gif";
                } else {
                    thumb_url = "img/file_upload_icon.png";
                }
                uploadMessage.append("<div style=\"").append(box_css)
                             .append("\"><div style=\"").append(viewport_css)
                             .append("\" align=\"center\"><img src=\"").append(thumb_url)
                             .append("\" style=\"").append(is_img ? img_css : file_css)
                             .append("\" /></div><div style=\"").append(title_css)
                             .append("\" align=\"center\">").append(nd.getDefaultFileName())
                             .append("<br />(").append(size_str)
                             .append(")</div></div>");
            } else {
                uploadMessage.append("<div style=\"margin:1px;\"><img src=\"img/checked.gif\" align=\"bottom\"/>")
                             .append("&nbsp;").append(nd.getDefaultFileName())
                             .append("&nbsp;(").append(size_str)
                             .append(")</div>");
            }
        }
        
        if (isImageFolder) {
            dropBox.setContent("<div style=\"margin-bottom:5px;\"><b>Uploaded:</b></div>" + uploadMessage);
        } else {
            dropBox.setContent("<div style=\"margin-bottom:5px;\"><b>Uploaded files:</b></div>" + uploadMessage);
        }
        dropBox.invalidate();
    }
    
    public void closeDialog()
    {
        cancelUpload();
        setVisible(false);
    }
    
    private void cancelUpload()
    {
        // to do
    }

    private void init()
    {
        dropBox = (Dropupload) getFellow("dropUploadBox");
    }
}
