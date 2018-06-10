/*
 * OldTinymceHandler.java
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
package org.docma.plugin.tinymce;

import java.io.File;
import java.net.URLEncoder;
import java.util.Properties;
import javax.servlet.http.HttpSession;
import org.docma.plugin.CharEntity;

import org.docma.plugin.Node;
import org.docma.plugin.Lock;
import org.docma.plugin.User;
import org.docma.plugin.internals.EmbeddedContentEditor;
import org.docma.plugin.internals.WindowPositionStorage;
import org.docma.plugin.internals.WindowSizeStorage;
import org.docma.plugin.web.ButtonType;
import org.docma.plugin.web.ContentAppHandler;
import org.docma.plugin.web.DefaultContentAppHandler;
import org.docma.plugin.web.MessageType;
import org.docma.plugin.web.UIEvent;
import org.docma.plugin.web.UIListener;
import org.docma.plugin.web.WebUserSession;

/**
 *
 * @author MP
 */
public class OldTinymceHandler implements ContentAppHandler, EmbeddedContentEditor, 
                                          WindowSizeStorage, WindowPositionStorage
{
    // HTTP session attributes used to store the window position. 
    public static final String ATTRIBUTE_EDITOR_POS_X = WebUserSession.class.getName() + ".editor_pos_x";
    public static final String ATTRIBUTE_EDITOR_POS_Y = WebUserSession.class.getName() + ".editor_pos_y";
    
    // User properties used to store the window size.
    public static final String USER_PROPERTY_EDIT_WIN_WIDTH = "editwin.width";
    public static final String USER_PROPERTY_EDIT_WIN_HEIGHT = "editwin.height";
    
    private File webBasePath;
    private File contentAppPath;
    private String contentAppRelativePath;
    
    private String applicationId;
    private String applicationName;
    
    private TinyMCECharEntities tinyEntities;

    /* -------------   Interface ContentAppHandler --------------- */
    
    public void initialize(File webBasePath, String relativeAppPath, Properties props) throws Exception 
    {
        this.webBasePath = webBasePath;
        contentAppPath = new File(webBasePath, relativeAppPath);
        contentAppRelativePath = relativeAppPath;
        
        applicationId = contentAppPath.getName();
        applicationName = applicationId.replace("tinymce_", "Tinymce ").replace('_', '.');
        
        tinyEntities = new TinyMCECharEntities(contentAppPath);
    }

    public String getApplicationId() 
    {
        return applicationId;
    }

    public String getApplicationName(String languageCode) 
    {
        return applicationName;
    }

    public String[] getSupportedEditExtensions() 
    {
        return new String[] { "content" };
    }

    public String[] getSupportedViewExtensions() 
    {
        return new String[0];  // view is not supported
    }

    public String getPreviewURL(WebUserSession webSess, String nodeId) 
    {
        return null;   // view is not supported
    }

    public void openEditor(final WebUserSession webSess, final String nodeId) throws Exception 
    {
        final Node node = webSess.getOpenedStore().getNodeById(nodeId);
        final User sessUser = webSess.getUser();
        
        // Check if lock exists
        Lock lock = node.getLock();
        if (lock != null) {
            String usr_id = lock.getUserId();
            if ((usr_id != null) && usr_id.equals(sessUser.getId())) {
                webSess.showMessage(
                    webSess.getLabel("text.confirm_self_locked"), 
                    webSess.getLabel("question.continue"), MessageType.QUESTION, 
                    new ButtonType[] { ButtonType.OK, ButtonType.CANCEL }, 
                    ButtonType.OK, 
                    new UIListener() {
                        public void onEvent(UIEvent evt) 
                        {
                            // If user clicks okay, refresh lock and open editor window
                            if (evt.isClick() && evt.isButtonTarget() &&
                                ButtonType.OK.equals(evt.getButtonType())) {
                                node.refreshLock();
                                openEditWindow(webSess, sessUser, nodeId);
                            }
                        }
                    }
                );
                return;
            } else {  // Node is locked by another user 
                User usr = webSess.getApplicationContext().getUser(usr_id);
                String usr_name = (usr != null) ? usr.getName() : null;
                if ((usr_name == null) || usr_name.equals("")) {
                    usr_name = "'" + usr_id + "'";
                } else {
                    usr_name = "'" + usr_name + "' [" + usr_id + "]";
                }
                webSess.showMessage(webSess.getLabel("text.locked_by_user", usr_name));
                return;
            }
        }
        
        // Set lock
        if (! node.setLock()) {
            webSess.showMessage(webSess.getLabel("text.could_not_set_lock"));
            return;
        }
        openEditWindow(webSess, sessUser, nodeId);
    }

    public void openViewer(WebUserSession webSess, String nodeId) throws Exception 
    {
        throw new UnsupportedOperationException("View is not supported.");
    }

    public void setCharEntities(CharEntity[] entities) 
    {
        tinyEntities.setCharEntities(entities);
    }
 
    /* -------------   Interface EmbeddedContentEditor --------------- */

    public String getIFrameURL(WebUserSession webSess, String nodeId)
    {
        return "tinymce_editor/iframe_content.jsp?docsess=" + webSess.getSessionId() + 
               "&nodeid=" + nodeId + "&appid=" + urlEncode(getApplicationId());
    }
    
    /* -------------   Interface WindowSizeStorage --------------- */
    
    public String getWindowWidth(WebUserSession webSess)
    {
        User sessUser = webSess.getUser();
        String win_width = sessUser.getProperty(USER_PROPERTY_EDIT_WIN_WIDTH);
        if ((win_width == null) || win_width.equals("")) {
            win_width = "" + DefaultContentAppHandler.WINDOW_DEFAULT_WIDTH;
        }
        return win_width;
    }

    public String getWindowHeight(WebUserSession webSess)
    {
        User sessUser = webSess.getUser();
        String win_height = sessUser.getProperty(USER_PROPERTY_EDIT_WIN_HEIGHT);
        if ((win_height == null) || win_height.equals("")) {
            win_height = "" + DefaultContentAppHandler.WINDOW_DEFAULT_HEIGHT;
        }
        return win_height;
    }

    public void setWindowSize(WebUserSession webSess, String win_width, String win_height)
    {
        if (win_width == null) win_width = "";
        if (win_height == null) win_height = "";
        try {
            // Save window size as user property:
            User usr = webSess.getUser();
            String old_width = usr.getProperty(USER_PROPERTY_EDIT_WIN_WIDTH);
            String old_height = usr.getProperty(USER_PROPERTY_EDIT_WIN_HEIGHT);
            if (! (win_width.equals(old_width) && win_height.equals(old_height))) {
                String[] p_names = { USER_PROPERTY_EDIT_WIN_WIDTH, USER_PROPERTY_EDIT_WIN_HEIGHT };
                String[] p_vals = { win_width, win_height };
                usr.setProperties(p_names, p_vals);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    /* -------------   Interface WindowPositionStorage --------------- */

    public String getWindowPosLeft(WebUserSession webSess)
    {
        HttpSession hsess = webSess.getHttpSession();
        Object posx = hsess.getAttribute(ATTRIBUTE_EDITOR_POS_X);
        return (posx instanceof Integer) ? posx.toString() : "" + DefaultContentAppHandler.WINDOW_DEFAULT_POSITION_X;
    }

    public String getWindowPosTop(WebUserSession webSess)
    {
        HttpSession hsess = webSess.getHttpSession();
        Object posy = hsess.getAttribute(ATTRIBUTE_EDITOR_POS_Y);
        return (posy instanceof Integer) ? posy.toString() : "" + DefaultContentAppHandler.WINDOW_DEFAULT_POSITION_Y;
    }
    
    public void setWindowPos(WebUserSession webSess, String win_xpos, String win_ypos)
    {
        if (win_xpos == null) win_xpos = "";
        if (win_ypos == null) win_ypos = "";
        try {
            // Save window position in session object
            if (! (win_xpos.equals("") || win_ypos.equals(""))) {
                HttpSession hsess = webSess.getHttpSession();
                hsess.setAttribute(ATTRIBUTE_EDITOR_POS_X, new Integer(win_xpos));
                hsess.setAttribute(ATTRIBUTE_EDITOR_POS_Y, new Integer(win_ypos));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /* -------------   Other public methods   --------------- */
    
    public String getCharEntitiesConfigString()
    {
        return tinyEntities.getCharEntitiesConfigString();
    }
    
    public String prepareContentForEdit(String content, String paraIndent)
    {
        return TinyEditorUtil.prepareContentForEdit(content, getApplicationId(), paraIndent);
    }
    
    public String prepareContentForSave(String content, String paraIndent)
    {
        return TinyEditorUtil.prepareContentForSave(content, getApplicationId(), paraIndent);
    }
    
    /* -------------   Other methods   --------------- */
    
    private String urlEncode(String s) 
    {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception ex) {
            return s;
        }
    }
    
    private void openEditWindow(WebUserSession webSess, User sessUser, String nodeId)
    {
        // Get editor position 
        HttpSession hsess = webSess.getHttpSession();
        Object posx = hsess.getAttribute(ATTRIBUTE_EDITOR_POS_X);
        Object posy = hsess.getAttribute(ATTRIBUTE_EDITOR_POS_Y);
        int win_left = (posx instanceof Integer) ? (Integer) posx : DefaultContentAppHandler.WINDOW_DEFAULT_POSITION_X;
        int win_top = (posy instanceof Integer) ? (Integer) posy : DefaultContentAppHandler.WINDOW_DEFAULT_POSITION_Y;
        
        // Get editor width and height
        String win_width = sessUser.getProperty(USER_PROPERTY_EDIT_WIN_WIDTH);
        String win_height = sessUser.getProperty(USER_PROPERTY_EDIT_WIN_HEIGHT);
        if ((win_width == null) || win_width.equals("")) {
            win_width = "" + DefaultContentAppHandler.WINDOW_DEFAULT_WIDTH;
        }
        if ((win_height == null) || win_height.equals("")) {
            win_height = "" + DefaultContentAppHandler.WINDOW_DEFAULT_HEIGHT;
        }
        
        // Desktop desk = getDesktop();
        String sessId = webSess.getSessionId();
        String edit_url = webSess.encodeURL("edit.zul?docsess=" + sessId +
                          "&nodeid=" + nodeId + "&appid=" + getApplicationId());
        // String win_name = "docmawin_" + docmaSess.getStoreId() + "_" + docmaSess.getVersionId() +
        //                   "_" + node.getId();
        // String client_action = "if (window." + win_name + ") { " +
        //   win_name + ".focus(); } else { window." + win_name + " = window.open('" + edit_url +
        //   "', '" + win_name +
        //   "', 'width=560,height=450,left=100,top=100,resizable=yes,location=no,menubar=no') };";
        String client_action = "window.open('" + edit_url +
          "', '_blank', 'width=" + win_width + ",height=" + win_height +
          ",left=" + win_left + ",top=" + win_top +
          ",resizable=yes,location=no,menubar=no');";
        webSess.evalJavaScript(client_action);
    }

    CharEntity[] getCharEntities()
    {
        return tinyEntities.getCharEntities();
    }

}
