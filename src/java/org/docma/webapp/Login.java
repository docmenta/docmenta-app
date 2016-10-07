/*
 * Login.java
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

import java.util.Locale;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;

import org.docma.app.*;
import org.docma.coreapi.*;
import org.docma.userapi.UserManager;
import org.docma.util.Log;
import org.zkoss.util.Locales;

import org.zkoss.web.Attributes;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.event.*;
import org.zkoss.zk.ui.util.Composer;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Window;
import org.zkoss.zul.Button;
import org.zkoss.zul.Textbox;

/**
 *
 * @author MP
 */
public class Login implements Composer, EventListener
{
    Window loginWin;
    Button loginBtn;
    Textbox username;
    Textbox userpwd;

    // private int desktopWidth = -1;
    // private int desktopHeight = -1;


    public void doAfterCompose(Component comp) throws Exception
    {
        loginWin = (Window) comp;
        // Window main = (Window) comp.getPage().getFellow("main");
        // main.addEventListener("onClientInfo", this);
        loginBtn = (Button) loginWin.getFellow("loginbtn");
        loginBtn.addEventListener("onClick", this);
        username = (Textbox) loginWin.getFellow("username");
        userpwd = (Textbox) loginWin.getFellow("userpwd");

        ServletContext servletCtx = loginWin.getDesktop().getWebApp().getServletContext();
        DocmaWebApplication app = null; 
        try {
            // Note: app is shared by all users (sessions) of this web application
            app = DocmaWebApplication.getInstance(servletCtx); // GUIUtil.getDocmaWebApplication(loginWin);
        } catch (DocException dex) {
            String err_msg = "Initialization of web application failed: " + dex.getMessage();
            Log.info(err_msg);
            if (DocmaConstants.DEBUG) {
                dex.printStackTrace();
            }
            Execution exec = loginWin.getDesktop().getExecution();
            String para_redir = exec.getParameter("redir");
            if (para_redir == null) {
                exec.sendRedirect("setup/index.zul");
            } else {
                if (DocmaConstants.DEBUG) {
                    Log.info("Login redirect paramter: " + para_redir);
                }
                Events.echoEvent("onShowErrorMsg", loginWin, err_msg);
            }
            return;
        }

        // Try to get username from stored cookie and set preferred UI language.
        String uName = GUIUtil.getUserNameFromCookie(loginWin);
        if ((uName != null) && (uName.length() > 0)) {
            username.setValue(uName);   // prefill username field
            userpwd.setFocus(true);     // switch focus to password field
            
            // Set preferred UI language of user
            try {
                UserManager um = app.getUserManager();
                String uid = um.getUserIdFromName(uName);
                if (uid != null) {
                    String lang_code = um.getUserProperty(uid, DocmaConstants.PROP_USER_GUI_LANGUAGE);
                    GUIUtil.setCurrentUILanguage(loginWin, lang_code, true);
                }
            } catch (Exception ex) {
                Log.error("Failed to set UI language: " + ex.getMessage());
                if (DocmaConstants.DEBUG) {
                    ex.printStackTrace();
                }
            }
        }
        
        if (app.hasPluginRestartNotification(true)) {
            Events.echoEvent("onShowPluginRestartMsg", loginWin, null);
        }

        DocmaI18 i18 = app.i18();
        String wintitle = DocmaConstants.DISPLAY_APP_SHORTNAME +
                          " V" + DocmaConstants.DISPLAY_APP_SHORTVERSION +
                          " " + i18.getLabel("label.login.windowtitle");
        loginWin.setTitle(wintitle);

        // redirect to main page, if already logged in
        // DocmaWebSession sess = GUIUtil.getDocmaWebSession(loginWin);
        // if (sess != null) {
        //     DocmaSession docmaSess = sess.getDocmaSession();
        //     if (docmaSess != null) {
        //         loginWin.getDesktop().getExecution().sendRedirect("main.zul");
        //     }
        // }
    }

    public void onEvent(Event evt) throws Exception 
    {
        Component t = evt.getTarget();
        // if (evt instanceof ClientInfoEvent) {
        //     ClientInfoEvent cinfo = (ClientInfoEvent) evt;
        //     if (desktopWidth < 0) desktopWidth = cinfo.getDesktopWidth();
        //     if (desktopHeight < 0) desktopHeight = cinfo.getDesktopHeight();
        //     System.out.println("Desktop width: " + cinfo.getDesktopWidth());
        //     System.out.println("Desktop height: " + cinfo.getDesktopHeight());
        // } else
        if (t == loginBtn) {
            doLogin();
        }
    }

    public void doLogin() throws Exception 
    {
            String uname = username.getValue();
            String pwd = userpwd.getValue();
            userpwd.setValue("");
            loginBtn.setDisabled(true);

            DocmaWebApplication app = GUIUtil.getDocmaWebApplication(loginWin);
            if (app == null) {
                loginWin.getDesktop().getExecution().sendRedirect("setup/index.zul");
            }
            Session uiSess = loginWin.getDesktop().getSession();
            DocmaWebSession webSess = GUIUtil.getDocmaWebSession_SessionContext(uiSess);
            if (webSess != null) {
                webSess.closeSession();  // Close existing Docma session
            }

            try {
                DocmaSession docmaSess = app.connect(uname, pwd);
                webSess = app.getWebSessionContext(docmaSess.getSessionId());
                // webSess.setDesktopWidth(desktopWidth);
                // webSess.setDesktopHeight(desktopHeight);
                GUIUtil.setDocmaWebSession_SessionContext(uiSess, webSess);

                docmaSess.setSessionListener(new DocmaSessionListenerImpl(loginWin.getDesktop()));

                // try to set username as cookie
                Object obj = loginWin.getDesktop().getExecution().getNativeResponse();
                if (obj instanceof HttpServletResponse) {
                    HttpServletResponse serv_resp = (HttpServletResponse) obj;
                    Cookie cook = new Cookie("org.docma.username", uname);
                    cook.setMaxAge(365*24*60*60);  // one year
                    serv_resp.addCookie(cook);
                }

                // login okay, thus redirect to main page (see Main.java):
                loginWin.getDesktop().getExecution().sendRedirect("main.zul");
            } catch (DocException dex) {
                Messagebox.show("Login failed: Invalid username or password.");
                loginBtn.setDisabled(false);
            } catch (Throwable ex) {  // also catch runtime exceptions (e.g. DocRuntimeException)
                if (DocmaConstants.DEBUG) {
                    ex.printStackTrace();
                }
                Messagebox.show("Login failed: " + ex.getMessage());
                loginBtn.setDisabled(false);
            }
            // Messagebox.show("Username: " + uname + " Pwd: " + pwd + " Root: " + app_root);

    }

}
