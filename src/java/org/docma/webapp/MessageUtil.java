/*
 * MessageUtil.java
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

import org.docma.coreapi.DocI18n;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Messagebox;

/**
 *
 * @author MP
 */
public class MessageUtil 
{
    public static int YES = Messagebox.YES;
    public static int NO  = Messagebox.NO;
    public static int OK  = Messagebox.OK;
    public static int CANCEL  = Messagebox.CANCEL;
    
    
    public static void showError(Component comp, String msg_key, Object... args) 
    {
        String msg = GUIUtil.getI18n(comp).getLabel(msg_key, args);
        Messagebox.show(msg, null, Messagebox.OK, Messagebox.ERROR);
    }
    
    public static void showError(DocI18n i18n, String msg_key, Object... args) 
    {
        String msg = i18n.getLabel(msg_key, args);
        Messagebox.show(msg, null, Messagebox.OK, Messagebox.ERROR);
    }
    
    public static void showWarning(Component comp, String msg_key, Object... args) 
    {
        String msg = GUIUtil.getI18n(comp).getLabel(msg_key, args);
        Messagebox.show(msg, null, Messagebox.OK, Messagebox.EXCLAMATION);
    }
    
    public static void showWarning(DocI18n i18n, String msg_key, Object... args) 
    {
        String msg = i18n.getLabel(msg_key, args);
        Messagebox.show(msg, null, Messagebox.OK, Messagebox.EXCLAMATION);
    }
    
    public static int showYesNoQuestion(Component comp, String title_key, String msg_key, Object... args) 
    {
        return showYesNoQuestion(GUIUtil.getI18n(comp), title_key, msg_key, args);
    }
    
    public static int showYesNoQuestion(DocI18n i18n, String title_key, String msg_key, Object... args) 
    {
        String title = i18n.getLabel(title_key);
        String msg = i18n.getLabel(msg_key, args);
        return Messagebox.show(msg, title, Messagebox.YES | Messagebox.NO, Messagebox.QUESTION);
    }
    
    public static int showOkCancelExclamation(Component comp, String title_key, String msg_key, Object... args) 
    {
        return showOkCancelExclamation(GUIUtil.getI18n(comp), title_key, msg_key, args);
    }
    
    public static int showOkCancelExclamation(DocI18n i18n, String title_key, String msg_key, Object... args) 
    {
        String title = i18n.getLabel(title_key);
        String msg = i18n.getLabel(msg_key, args);
        return Messagebox.show(msg, title, Messagebox.OK | Messagebox.CANCEL, Messagebox.EXCLAMATION);
    }
    
}
