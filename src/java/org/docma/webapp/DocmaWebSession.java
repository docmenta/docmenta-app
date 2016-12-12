/*
 * DocmaWebSession.java
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

import java.util.*;
import javax.servlet.http.HttpSession;
import org.docma.coreapi.*;
import org.docma.app.*;
import org.docma.plugin.web.WebUserSession;
import org.docma.plugin.web.ContentAppHandler;
import org.docma.plugin.web.DefaultContentAppHandler;
import org.docma.plugin.implementation.WebUserSessionImpl;
import org.zkoss.util.Locales;
import org.zkoss.web.Attributes;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.Menupopup;

/**
 *
 * @author MP
 */
public class DocmaWebSession
{
    private final DocmaWebApplication docmaApp;
    private DocmaSession docmaSess;
    private WebUserSessionImpl pluginWebSess = null;
    private MainWindow mainWin = null;
    private DocmaSearchResult searchResult = null;
    private DocmaExportLog previewLog = null;
    // private int editorPosX = GUIConstants.EDIT_WIN_DEFAULT_POSITION_X;
    // private int editorPosY = GUIConstants.EDIT_WIN_DEFAULT_POSITION_Y;
    private int screenWidth = -1;
    private int screenHeight = -1;
    private int desktopWidth = -1;
    private int desktopHeight = -1;
    private Map sessionObjects = null;

    
    public DocmaWebSession(DocmaWebApplication docmaApp, DocmaSession docmaSess)
    {
        this.docmaApp = docmaApp;
        this.docmaSess = docmaSess;
        this.pluginWebSess = new WebUserSessionImpl(docmaSess, this, docmaApp.getPluginManager());
    }

    public DocmaWebApplication getDocmaWebApplication()
    {
        return docmaApp;
    }
    
    public DocmaSession getDocmaSession()
    {
        return docmaSess;
    }
    
    public WebUserSession getPluginInterface()
    {
        return pluginWebSess;
    }
    
    public void unloadPluginComponents(String pluginId)
    {
        pluginWebSess.unloadPluginComponents(pluginId);
    }
    
    public MainWindow getMainWindow()
    {
        return mainWin;
    }

    void setMainWindow(MainWindow mainWin)
    {
        this.mainWin = mainWin;
    }

    public DocmaExportLog getPreviewLog()
    {
        return previewLog;
    }

    public void setPreviewLog(DocmaExportLog preview_log)
    {
        this.previewLog = preview_log;
    }

    Locale getCurrentLocale()
    {
        Locale loc = null;
        Component comp = getMainWindow();
        if (comp != null) {
            Object obj = comp.getDesktop().getSession().getAttribute(Attributes.PREFERRED_LOCALE);
            if (obj instanceof Locale) {
                loc = (Locale) obj;
            }
        }
        if (loc == null) {
            loc = Locales.getCurrent();
        }
        return loc;
    }
    
    public HttpSession getHttpSession()
    {
        Component comp = getMainWindow();
        if (comp != null) {
            Object obj = comp.getDesktop().getSession().getNativeSession();
            if (obj instanceof HttpSession) {
                return (HttpSession) obj;
            }
        }
        return null;
    }

    public int getEditorPositionX()
    {
        Object posx = getHttpSession().getAttribute(DefaultContentAppHandler.ATTRIBUTE_EDITOR_POS_X);
        if (posx instanceof Integer) {
            return (Integer) posx;
        } else {
            return DefaultContentAppHandler.WINDOW_DEFAULT_POSITION_X;
        }
    }

//    public void setEditorPositionX(int x)
//    {
//        this.editorPosX = (x < 0) ? 0 : x;
//    }
//    
//    public void setEditorPositionX(String posX)
//    {
//        try {
//            int x = Integer.parseInt(posX);
//            setEditorPositionX(x);
//        } catch (Exception ex) {}
//    }

    public int getEditorPositionY()
    {
        Object posy = getHttpSession().getAttribute(DefaultContentAppHandler.ATTRIBUTE_EDITOR_POS_Y);
        if (posy instanceof Integer) {
            return (Integer) posy;
        } else {
            return DefaultContentAppHandler.WINDOW_DEFAULT_POSITION_Y;
        }
    }

//    public void setEditorPositionY(int y)
//    {
//        this.editorPosY = (y < 0) ? 0 : y;
//    }
//    
//    public void setEditorPositionY(String posY)
//    {
//        try {
//            int y = Integer.parseInt(posY);
//            setEditorPositionY(y);
//        } catch (Exception ex) {}
//    }
    
    public int getPdfPreviewPositionX()
    {
        return 0;
    }

    public int getPdfPreviewPositionY()
    {
        return getEditorPositionY();   // same y position as editor window
    }

    public int getPdfPreviewWidth()
    {
        int max_width = getScreenWidth();
        if (max_width >= 900) return 900;
        else if (max_width > 0) return max_width;
        else return 800; // docmaSess.getUserProperty(GUIConstants.PROP_USER_EDIT_WIN_WIDTH);
    }

    public int getPdfPreviewHeight()
    {
        int h = GUIConstants.EDIT_WIN_DEFAULT_HEIGHT;
        int max_height = getScreenHeight();
        if (max_height <= 0) {
            if (docmaSess != null) {
                String hstr = docmaSess.getUserProperty(GUIConstants.PROP_USER_EDIT_WIN_HEIGHT);
                if ((hstr != null) && !hstr.equals("")) {
                    try { 
                        h = Integer.parseInt(hstr);
                    } catch (Exception ex) {}  // ignore, use default height
                }
            }
        } else {
            int availHeight = max_height - getPdfPreviewPositionY();
            if (DocmaConstants.DEBUG) {
                System.out.println("Available height: " + availHeight);
            }
            h = (availHeight <= 200) ?  100 : (availHeight - 100);
            if (h >= 1000) h = 1000;
        }
        return h;
    }

    public int getScreenWidth()
    {
        return screenWidth;
    }

    public void setScreenWidth(int width)
    {
        screenWidth = width;
    }

    public int getScreenHeight()
    {
        return screenHeight;
    }

    public void setScreenHeight(int height)
    {
        screenHeight = height;
    }

    public int getDesktopWidth()
    {
        return desktopWidth;
    }

    public void setDesktopWidth(int width)
    {
        desktopWidth = width;
    }

    public int getDesktopHeight()
    {
        return desktopHeight;
    }

    public void setDesktopHeight(int height)
    {
        desktopHeight = height;
    }

    public DocmaSearchResult getSearchResult()
    {
        return searchResult;
    }

    public void setSearchResult(DocmaSearchResult searchResult)
    {
        this.searchResult = searchResult;
    }

    public void closeSession()
    {
        if (docmaSess != null) {
            try {
                pluginWebSess.onSessionClose();  // e.g. close temporary store connections opened by plugins
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            docmaApp.releaseSession(docmaSess.getSessionId());
            docmaSess.closeSession();
            docmaSess = null;
            pluginWebSess = null;
        }
    }

    public String[] getEditorIds(String... exts)
    {
        return docmaApp.getEditorIds(exts);
    }

    public String[] getViewerIds(String... exts)
    {
        return docmaApp.getViewerIds(exts);
    }

    public String getFileEditorId(String ext)
    {
        // To do: consider file extension assignments defined by the user
        return docmaApp.getFileEditorId(ext);
    }
    
    public String getFileViewerId(String ext)
    {
        // To do: consider file extension assignments defined by the user
        return docmaApp.getFileViewerId(ext);
    }
    
    public String getPreviewURL(String viewer_id, String node_id)
    {
        ContentAppHandler handler = docmaApp.getContentAppHandler(viewer_id);
        if (handler == null) {
            return null;   // no preview available
        }
        return handler.getPreviewURL(getPluginInterface(), node_id);
    }
    
    public void openEditor(String editor_id, String node_id) throws Exception
    {
        ContentAppHandler handler = docmaApp.getContentAppHandler(editor_id);
        if (handler == null) {
            throw new Exception("Editor with id '" + editor_id + "' not found.");
        }
        handler.openEditor(getPluginInterface(), node_id);
    }
    
    public void openViewer(String viewer_id, String node_id) throws Exception
    {
        ContentAppHandler handler = docmaApp.getContentAppHandler(viewer_id);
        if (handler == null) {
            throw new Exception("Viewer with id '" + viewer_id + "' not found.");
        }
        handler.openViewer(getPluginInterface(), node_id);
    }
    
    public void openContentEditor(String node_id) throws Exception
    {
        ContentAppHandler handler = getContentEditorHandler();
        if (handler == null) {
            throw new Exception("Could not open content editor: no content editor loaded.");
        }
        handler.openEditor(getPluginInterface(), node_id);
    }

    public String encodeEntitiesNumeric(String str)
    {
        return docmaSess.encodeCharEntities(str, false);
    }

    public String decodeEntities(String str)
    {
        return docmaSess.decodeCharEntities(str);
    }
    
    public void setSessionObject(String key, Object obj) 
    {
        if (this.sessionObjects == null) {
            this.sessionObjects = new HashMap();
        }
        if (obj == null) {
            this.sessionObjects.remove(key);
        } else {
            this.sessionObjects.put(key, obj);
        }
    }
    
    public Object getSessionObject(String key) 
    {
        if (this.sessionObjects != null) {
            return this.sessionObjects.get(key);
        } else {
            return null;
        }
    }

    public String getUserTheme()
    {
        // Currently user cannot select his own theme. This is a dummy implementation:
        return docmaApp.getDefaultTheme();
    }

    public void setUserTheme(String themeId)
    {
        throw new UnsupportedOperationException("This method is not implemented yet!");
        // Implement this method to allow each user to select his preferrred theme.
    }
    
    public String getThemeProperty(String propName)
    {
        return docmaApp.getThemeProperty(getUserTheme(), propName);
    }

    /* --------------  Package local methods  ---------------------- */
    
    void sendMenuClickEventToPlugin(Menuitem item)
    {
        if (pluginWebSess != null) {
            pluginWebSess.sendMenuClickEventToPlugin(item);
        } 
    }

    void sendMenuOpenEventToPlugins(Menupopup menu)
    {
        if (pluginWebSess != null) {
            pluginWebSess.sendMenuOpenEventToPlugins(menu);
        }
    }
    
    
    /* --------------  Private methods  ---------------------- */

    private ContentAppHandler getContentEditorHandler()
    {
        ContentAppHandler handler = null;
        
        // Try to get user assigned content editor
        String editor_id = docmaSess.getUserProperty(GUIConstants.PROP_USER_EDITOR_ID);
        if ((editor_id != null) && !editor_id.equals("")) {
            handler = docmaApp.getContentAppHandler(editor_id);
        }
        
        // If user assigned editor does not exist, then use default content editor. 
        if (handler == null) {
            editor_id = docmaApp.getContentEditorId();
            if ((editor_id != null) && !editor_id.equals("")) {
                handler = docmaApp.getContentAppHandler(editor_id);
            }
        }

        // If the default content editor does not exist (should not occur),
        // then use existing content editor with highest ID (use highest ID to 
        // get the latest version of the editor in case multiple versions of the 
        // same editor are installed).
        if (handler == null) {
            String[] ids = docmaApp.getContentEditorIds();
            if (ids.length > 0) {
                handler = docmaApp.getContentAppHandler(ids[ids.length - 1]);
            }
        }
        return handler;
    }

}
