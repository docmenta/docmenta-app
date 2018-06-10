/*
 * CKEditPlugin.java
 * 
 *  Copyright (C) 2018  Manfred Paula, http://www.docmenta.org
 *   
 *  This file is part of Docmenta. Docmenta is free software: you can 
 *  redistribute it and/or modify it under the terms of the GNU Lesser 
 *  General Public License as published by the Free Software Foundation, 
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Docmenta.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.docma.plugin.ckeditor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import org.docma.coreapi.DocConstants;
import org.docma.plugin.PluginContext;
import org.docma.plugin.web.DefaultWebPlugin;
import org.docma.plugin.web.WebPluginContext;
import org.docma.plugin.web.WebUserSession;
import org.docma.util.DocmaUtil;
import org.docma.util.Log;
import org.zkoss.zul.Window;

/**
 *
 * @author MP
 */
public class CKEditPlugin extends DefaultWebPlugin
{
    private static final String CKEDIT_PROPS_FILENAME = "ckedit.properties";
    
    static final String PROP_DIALOG_BG_OPACITY = "dialog_backgroundCoverOpacity";
    static final String PROP_DIALOG_NO_CONFIRM_CANCEL = "dialog_noConfirmCancel";
    static final String PROP_DISABLE_NATIVE_SPELLCHECKER = "disableNativeSpellChecker";
    static final String PROP_DISABLE_NATIVE_TABLEHANDLES = "disableNativeTableHandles";
    static final String PROP_ENTITIES_HTML = "entities";
    static final String PROP_ENTITIES_USER = "entities_user";
    static final String PROP_ENTITIES_USER_NUMERICAL = "entities_userNumerical";
    static final String PROP_ENTITIES_GREEK = "entities_greek";
    static final String PROP_ENTITIES_LATIN = "entities_latin";
    static final String PROP_ENTITIES_NUMERICAL = "entities_processNumerical";
    static final String PROP_FIGURE_TAG_SUPPORT = "figure_tag_support";
    static final String PROP_PASTE_AS_PLAINTEXT = "forcePasteAsPlainText";
    static final String PROP_IGNORE_EMPTY_PARA = "ignoreEmptyParagraph";
    static final String PROP_IMG_ALT_REQUIRED = "image2_altRequired";
    static final String PROP_IMG_CAPTIONED_CLASS = "image2_captionedClass";
    static final String PROP_IMG_DISABLE_RESIZER = "image2_disableResizer";
    static final String PROP_REMOVE_BUTTONS = "removeButtons";
    static final String PROP_SKIN = "skin";
    static final String PROP_TOOLBAR_LOCATION = "toolbarLocation";
    static final String PROP_UI_COLOR = "uiColor";
    
    private Properties ckEditProps = null;
    private String[] availableSkins = null;
    private String configDialogId = null;

    @Override
    public void onLoad(PluginContext ctx) throws Exception 
    {
        loadCKEditProps(ctx);
        CKEditHandler.registerPlugin(getAppHandlerId(ctx), this);
        try {
            availableSkins = getCKSkins((WebPluginContext) ctx);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    
    @Override
    public void onUnload(PluginContext ctx) throws Exception
    {
        CKEditHandler.unregisterPlugin(getAppHandlerId(ctx), this);
    }
    
    @Override
    public void onShowConfigDialog(WebPluginContext ctx, WebUserSession webSess) 
    {
        if (ckEditProps == null) {  // should not be true (see onLoad)
            loadCKEditProps(ctx);
        }
        Window dialog = getConfigDialog(ctx, webSess);
        CKEditConfComposer composer = (CKEditConfComposer) dialog.getAttribute("$composer");
        composer.editProps(this, ctx, webSess);
    }
    
    public String getFirstAvailableCKSkin()
    {
        return ((availableSkins != null) && (availableSkins.length > 0)) ? availableSkins[0] : null;
    }
    
    public void saveConfiguration(PluginContext ctx)
    {
        saveCKEditProps(ctx);
    }

    public String getDialogBgOpacity()
    {
        return getCKProperty(PROP_DIALOG_BG_OPACITY, "0.5");
    }
    
    public void setDialogBgOpacity(String opacity)
    {
        setCKProperty(PROP_DIALOG_BG_OPACITY, opacity);
    }
    
    public boolean isDialogNoConfirmCancel()
    {
        return isCKProperty(PROP_DIALOG_NO_CONFIRM_CANCEL, true);
    }
    
    public void setDialogNoConfirmCancel(boolean noConfirm)
    {
        setCKProperty(PROP_DIALOG_NO_CONFIRM_CANCEL, noConfirm ? "true" : "false");
    }
    
    public boolean isDisableNativeSpellChecker()
    {
        return isCKProperty(PROP_DISABLE_NATIVE_SPELLCHECKER, true);
    }
    
    public void setDisableNativeSpellChecker(boolean disabled)
    {
        setCKProperty(PROP_DISABLE_NATIVE_SPELLCHECKER, disabled ? "true" : "false");
    }
    
    public boolean isDisableNativeTableHandles()
    {
        return isCKProperty(PROP_DISABLE_NATIVE_TABLEHANDLES, true);
    }
    
    public void setDisableNativeTableHandles(boolean disabled)
    {
        setCKProperty(PROP_DISABLE_NATIVE_TABLEHANDLES, disabled ? "true" : "false");
    }
    
    public boolean isEntitiesHTML()
    {
        return isCKProperty(PROP_ENTITIES_HTML, true);
    }
    
    public void setEntitiesHTML(boolean enabled)
    {
        setCKProperty(PROP_ENTITIES_HTML, enabled ? "true" : "false");
    }
    
    public boolean isEntitiesUser()
    {
        return isCKProperty(PROP_ENTITIES_USER, true);
    }
    
    public void setEntitiesUser(boolean enabled)
    {
        setCKProperty(PROP_ENTITIES_USER, enabled ? "true" : "false");
    }
    
    public boolean isEntitiesUserNumerical()
    {
        return isCKProperty(PROP_ENTITIES_USER_NUMERICAL, false);
    }
    
    public void setEntitiesUserNumerical(boolean enabled)
    {
        setCKProperty(PROP_ENTITIES_USER_NUMERICAL, enabled ? "true" : "false");
    }
    
    public boolean isEntitiesGreek()
    {
        return isCKProperty(PROP_ENTITIES_GREEK, true);
    }
    
    public void setEntitiesGreek(boolean enabled)
    {
        setCKProperty(PROP_ENTITIES_GREEK, enabled ? "true" : "false");
    }
    
    public boolean isEntitiesLatin()
    {
        return isCKProperty(PROP_ENTITIES_LATIN, true);
    }
    
    public void setEntitiesLatin(boolean enabled)
    {
        setCKProperty(PROP_ENTITIES_LATIN, enabled ? "true" : "false");
    }
    
    public String getEntitiesProcessNumerical()
    {
        return getCKProperty(PROP_ENTITIES_NUMERICAL, "false").toLowerCase();
    }
    
    public void setEntitiesProcessNumerical(String value)
    {
        setCKProperty(PROP_ENTITIES_NUMERICAL, value);
    }
    
    public boolean isPastePlainText()
    {
        return isCKProperty(PROP_PASTE_AS_PLAINTEXT, false);
    }
    
    public void setPastePlainText(boolean enabled)
    {
        setCKProperty(PROP_PASTE_AS_PLAINTEXT, enabled ? "true" : "false");
    }
    
    public boolean isFigureTagEnabled(String productId)
    {
        boolean isModeEnabled = getFigureTagMode();
        String[] prodArr = getFigureTagProductList();
        if (prodArr == null) {
            return isModeEnabled;
        } else {
            boolean isInList = Arrays.asList(prodArr).contains(productId);
            return isModeEnabled ? isInList : !isInList;
        }
    }
    
    public boolean getFigureTagMode()
    {
        String v = getCKProperty(PROP_FIGURE_TAG_SUPPORT, "disabled");
        int p = v.indexOf(':');
        String val = (p < 0) ? v : v.substring(0, p);
        return val.equalsIgnoreCase("enabled");
    }
    
    public String[] getFigureTagProductList()
    {
        String v = getFigureTagProducts();
        if (v == null) {
            return null;
        } else {
            return v.equals("") ? new String[0] : v.split("[,\\s]");
        }
    }
    
    public String getFigureTagProducts()
    {
        String v = getCKProperty(PROP_FIGURE_TAG_SUPPORT, "disabled");
        int p = v.indexOf(':');
        if (p < 0) {
            return null;
        } else {
            return v.substring(p + 1).trim();
        }
    }
    
    public void setFigureTag(boolean enabled, String products)
    {
        String val = enabled ? "enabled" : "disabled";
        if (products != null) {
            val += ":" + products;
        }
        setCKProperty(PROP_FIGURE_TAG_SUPPORT, val);
    }
    
    public void setFigureTagProductList(boolean enabled, String[] products)
    {
        if (products == null) {
            setFigureTag(enabled, null);
        } else {
            setFigureTag(enabled, DocmaUtil.concatStrings(products, ","));
        }
    }
    
    public boolean isIgnoreEmptyPara()
    {
        return isCKProperty(PROP_IGNORE_EMPTY_PARA, true);
    }
    
    public void setIgnoreEmptyPara(boolean ignore)
    {
        setCKProperty(PROP_IGNORE_EMPTY_PARA, ignore ? "true" : "false");
    }
    
    public boolean isImgAltRequired()
    {
        return isCKProperty(PROP_IMG_ALT_REQUIRED, false);
    }
    
    public void setImgAltRequired(boolean required)
    {
        setCKProperty(PROP_IMG_ALT_REQUIRED, required ? "true" : "false");
    }
    
    public String getImgCaptionedClass()
    {
        return getCKProperty(PROP_IMG_CAPTIONED_CLASS, "");
    }
    
    public void setImgCaptionedClass(String value)
    {
        setCKProperty(PROP_IMG_CAPTIONED_CLASS, value);
    }
    
    public boolean isImgDisableResizer()
    {
        return isCKProperty(PROP_IMG_DISABLE_RESIZER, false);
    }
    
    public void setImgDisableResizer(boolean disabled)
    {
        setCKProperty(PROP_IMG_DISABLE_RESIZER, disabled ? "true" : "false");
    }
    
    public String getRemoveButtons()
    {
        return getCKProperty(PROP_REMOVE_BUTTONS, "");
    }
    
    public void setRemoveButtons(String buttonList)
    {
        setCKProperty(PROP_REMOVE_BUTTONS, buttonList);
    }
    
    public String getSkin()
    {
        return getCKProperty(PROP_SKIN, "");
    }
    
    public void setSkin(String skinName)
    {
        setCKProperty(PROP_SKIN, skinName);
    }
    
    public String getToolbarLocation()
    {
        return getCKProperty(PROP_TOOLBAR_LOCATION, "top");
    }
    
    public void setToolbarLocation(String location)
    {
        setCKProperty(PROP_TOOLBAR_LOCATION, location);
    }
    
    public String getUIColor()
    {
        return getCKProperty(PROP_UI_COLOR, "#9AB8F3");
    }
    
    public void setUIColor(String color)
    {
        setCKProperty(PROP_UI_COLOR, color);
    }
    
    // ************ Package local methods ***************
    
    String getRelativeCKAppURI(WebPluginContext ctx)
    {
        return "apps/" + getAppHandlerId(ctx);
    }

    File getCKAppDir(WebPluginContext ctx)
    {
        return new File(ctx.getWebAppDirectory(), "apps" + File.separator + getAppHandlerId(ctx));
    }
    
    String[] getCKSkins(WebPluginContext ctx)
    {
        File appDir = getCKAppDir(ctx);
        File skinDir = new File(appDir, "ckeditor" + File.separator + "skins");
        String[] skins = skinDir.list();
        return (skins == null) ? new String[0] : skins;
    }
    
    // ************ Private methods ***************

    private boolean isCKProperty(String propName, boolean defaultValue)
    {
        return getCKProperty(propName, defaultValue ? "true" : "false").equalsIgnoreCase("true");
    }
            
    private String getCKProperty(String propName, String defaultValue)
    {
        String v = ckEditProps.getProperty(propName, defaultValue);
        return v.equals("") ? defaultValue : v;
    }
    
    private void setCKProperty(String propName, String value)
    {
        ckEditProps.setProperty(propName, value);
    }
    
    private String getAppHandlerId(PluginContext ctx)
    {
        return ctx.getPluginProperty("ckeditor_apphandler_id");
    }
    
    private Window getConfigDialog(WebPluginContext ctx, WebUserSession webSess)
    {
        Window dialog = null;
        if (configDialogId != null) {
            try {
                dialog = (Window) webSess.getDialog(configDialogId);
            } catch (Exception ex) {}  // dialog with given id does not yet exist
        }
        if (dialog == null) {
            String zul_uri = getRelativeCKAppURI(ctx) + "/ckedit_config_dialog.zul";
            configDialogId = webSess.addDialog(zul_uri);
            dialog = (Window) webSess.getDialog(configDialogId);
        }
        return dialog;
    }
    
    
    private File getCKEditPropsFile(PluginContext ctx)
    {
        File f = ctx.getPluginDirectory();
        return new File(f, CKEDIT_PROPS_FILENAME);
    }
    
    private synchronized void loadCKEditProps(PluginContext ctx) 
    {
        File pfile = getCKEditPropsFile(ctx);
        try {
            ckEditProps = new Properties();
            // ClassLoader cl = PropertiesLoader.class.getClassLoader();
            // InputStream fin = cl.getResourceAsStream(inifilename);
            if (pfile.exists()) {
                InputStream fin = new FileInputStream(pfile);
                try {
                    ckEditProps.load(fin);
                } finally {
                    fin.close();
                }
            }
        } catch (Exception ex) {
            ckEditProps = null;
            Log.error("Could not load CKEditor properties file: " + pfile);
            throw new RuntimeException(ex);
        }
    }

    private synchronized void saveCKEditProps(PluginContext ctx)
    {
        if (ckEditProps == null) return; 

        File pfile = getCKEditPropsFile(ctx);
        if (DocConstants.DEBUG) {
            Log.info("Saving CKEditor properties file: " + pfile);
        }
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(pfile);
            ckEditProps.store(fout, "CKEditor configuration properties");
            fout.close();
        } catch (Exception ex) {
            Log.error("Could not save CKEditor properties file: " + pfile);
            if (fout != null) try { fout.close(); } catch (Exception ex2) {}
            throw new RuntimeException(ex);
        }
    }
    
}
