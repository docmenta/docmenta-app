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
import java.util.Properties;
import javax.servlet.http.HttpSession;
import org.docma.plugin.CharEntity;

import org.docma.plugin.Node;
import org.docma.plugin.Lock;
import org.docma.plugin.User;
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
public class OldTinymceHandler implements ContentAppHandler
{
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
                    "This content is already locked by you! Continue?", 
                    "Continue?", MessageType.QUESTION, 
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
                webSess.showMessage("This content is locked by user " + usr_name + ".");
                return;
            }
        }
        
        // Set lock
        if (! node.setLock()) {
            webSess.showMessage("Could not set lock! Content may be locked by another user.");
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
 
    /* -------------   Other methods   --------------- */
    
    private void openEditWindow(WebUserSession webSess, User sessUser, String nodeId)
    {
        // Get editor position 
        HttpSession hsess = webSess.getHttpSession();
        Object posx = hsess.getAttribute(DefaultContentAppHandler.ATTRIBUTE_EDITOR_POS_X);
        Object posy = hsess.getAttribute(DefaultContentAppHandler.ATTRIBUTE_EDITOR_POS_Y);
        int win_left = (posx instanceof Integer) ? (Integer) posx : DefaultContentAppHandler.WINDOW_DEFAULT_POSITION_X;
        int win_top = (posy instanceof Integer) ? (Integer) posy : DefaultContentAppHandler.WINDOW_DEFAULT_POSITION_Y;
        
        // Get editor width and height
        String win_width = sessUser.getProperty(DefaultContentAppHandler.USER_PROPERTY_EDIT_WIN_WIDTH);
        String win_height = sessUser.getProperty(DefaultContentAppHandler.USER_PROPERTY_EDIT_WIN_HEIGHT);
        if ((win_width == null) || win_width.equals("")) {
            win_width = "" + DefaultContentAppHandler.WINDOW_DEFAULT_WIDTH;
        }
        if ((win_height == null) || win_height.equals("")) {
            win_height = "" + DefaultContentAppHandler.WINDOW_DEFAULT_HEIGHT;
        }
        
        // Desktop desk = getDesktop();
        String sessId = webSess.getSessionId();
        String edit_url = webSess.encodeURL("tinyedit.zul?docsess=" + sessId +
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

    public String getCharEntitiesConfigString()
    {
        return tinyEntities.getCharEntitiesConfigString();
    }

}
