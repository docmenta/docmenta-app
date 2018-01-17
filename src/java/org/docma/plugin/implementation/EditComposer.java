/*
 * EditComposer.java
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

package org.docma.plugin.implementation;

import org.docma.coreapi.DocConstants;
import org.docma.plugin.FileContent;
import org.docma.plugin.Node;
import org.docma.plugin.PubContent;
import org.docma.plugin.StoreConnection;
import org.docma.plugin.VersionId;
import org.docma.plugin.internals.EmbeddedContentEditor;
import org.docma.plugin.internals.WindowPositionStorage;
import org.docma.plugin.internals.WindowSizeStorage;
import org.docma.plugin.web.ContentAppHandler;
import org.docma.plugin.web.WebPluginUtil;
import org.docma.plugin.web.WebUserSession;
import org.docma.plugin.web.DefaultContentAppHandler;

import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.Composer;
import org.zkoss.zul.Label;
import org.zkoss.zul.Slider;
import org.zkoss.zul.Iframe;
import org.zkoss.zk.ui.util.Clients;

/**
 *
 * @author MP
 */
public class EditComposer implements Composer
{

    public void doAfterCompose(Component comp) throws Exception
    {
        EditWindow win = (EditWindow) comp;
        Desktop desk = win.getDesktop();
        Execution exec = desk.getExecution();
        String docsess = exec.getParameter("docsess"); // String deskid = exec.getParameter("desk");
        String nodeid = exec.getParameter("nodeid");
        String editorid = exec.getParameter("appid");
        // String langid = exec.getParameter("langid");

        WebUserSession webSess = WebPluginUtil.getUserSession(desk.getWebApp().getServletContext(), docsess);
        // DocmaWebSession webSess = GUIUtil.getDocmaWebSession(desk.getSession(), deskid);
        StoreConnection store = webSess.getOpenedStore();  // DocmaSession docmaSess = webSess.getDocmaSession();
        String storeId =  (store != null) ? store.getStoreId() : null;
        VersionId verId = (store != null) ? store.getVersionId() : null;
        Node node =       (store != null) ? store.getNodeById(nodeid) : null;
        String langid =   (store != null) ? store.getTranslationMode() : null;

        win.webSess = webSess;
        // win.desktopId = deskid;
        // win.mainWin = webSess.getMainWindow();
        win.storeId = storeId;
        win.versionId = verId;
        win.nodeId = nodeid;
        win.transMode = langid;
        
        // Set Theme Styles
        win.setContentStyle(webSess.getThemeProperty("popupwin.contentstyle.css"));
        Iframe result_frm = (Iframe) win.getFellow("docmaresultfrm");  // hidden result frame
        // hide frame by setting frame-background to background-color of window:
        result_frm.setStyle(webSess.getThemeProperty("popupwin.bgcolor.css"));

        ContentAppHandler appHandler = (editorid == null) ? null : webSess.getContentAppHandler(editorid);
        String url = null;
        if (appHandler instanceof EmbeddedContentEditor) {
            url = ((EmbeddedContentEditor) appHandler).getIFrameURL(webSess, nodeid);
        }
        if (url == null) {
            url = ""; // "tinymce_editor/iframe_content.jsp?docsess=" + docsess + 
                      // "&nodeid=" + nodeid + "&appid=" + editorid;
        }
        // url = exec.encodeURL(url);
        Iframe frm = (Iframe) win.getFellow("contentfrm");
        frm.setSrc(url);

        Label contTitle = (Label) win.getFellow("contentTitle");
        Label prodId = (Label) win.getFellow("productId");
        Label prodVer = (Label) win.getFellow("productVersion");
        Label prodLang = (Label) win.getFellow("productLanguage");
        Slider contProgress = (Slider) win.getFellow("editProgressSlider");

        String ntitle; 
        if (node == null) { 
            ntitle = "";
        } else if (node instanceof PubContent) {
            ntitle = ((PubContent) node).getTitle();
        } else if (node instanceof FileContent) {
            ntitle = ((FileContent) node).getFileName();
        } else {
            ntitle = node.getId();
        }
        contTitle.setValue(ntitle);
        prodId.setValue((storeId != null) ? storeId : "");
        prodVer.setValue((verId != null) ? verId.toString() : "");
        String lang = langid;
        if ((lang == null) || lang.equals("")) {
            lang = "original";
        }
        prodLang.setValue(lang);
        contProgress.setCurpos((node != null) ? node.getProgress() : 0);

        String win_x = null;
        String win_y = null;
        String win_width = null;
        String win_height = null;
        if (appHandler instanceof WindowPositionStorage) {
            WindowPositionStorage wps = (WindowPositionStorage) appHandler;
            win_x = wps.getWindowPosLeft(webSess);
            win_y = wps.getWindowPosTop(webSess);
        }
        if (appHandler instanceof WindowSizeStorage) {
            WindowSizeStorage wss = (WindowSizeStorage) appHandler;
            win_width = wss.getWindowWidth(webSess);
            win_height = wss.getWindowHeight(webSess);
        }
        
        if ((win_x == null) || win_x.equals("")) {
            win_x = "" + DefaultContentAppHandler.WINDOW_DEFAULT_POSITION_X;
        }
        if ((win_y == null) || win_y.equals("")) { 
            win_y = "" + DefaultContentAppHandler.WINDOW_DEFAULT_POSITION_Y;
        }
        String fix_pos_js = "fixWinPos('" + win_x + "', '" + win_y + "');";
        if (DocConstants.DEBUG) {
            System.out.println(fix_pos_js);
        }
        
        if (win_width == null) win_width = "";
        if (win_height == null) win_height = "";
        String fix_size_js = "";
        if ((win_width.length() > 0) && (win_height.length() > 0)) {
            fix_size_js = "fixWinSize('" + win_width + "', '" + win_height + "');";
            if (DocConstants.DEBUG) {
                System.out.println(fix_size_js);
            }
        }

        String unload_js = "setUnloadMsg('" + 
                           webSess.getLabel("confirm.discard_unsaved").replace("'", " ") +
                           "');";
        Clients.evalJavaScript(unload_js + fix_size_js + fix_pos_js);
    }

}
