/*
 * UIEventImpl.java
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

import org.docma.app.DocmaConstants;
import org.docma.plugin.web.ButtonType;
import org.docma.plugin.web.UIEvent;
import org.docma.plugin.web.WebUserSession;
import org.docma.util.Log;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Button;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.Menupopup;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Tab;
import org.zkoss.zul.impl.MessageboxDlg;

/**
 *
 * @author MP
 */
public class UIEventImpl implements UIEvent
{
    private String name;
    private Component target;
    private final WebUserSession session;
    
    private boolean isMessageboxClick = false;
    private ButtonType messageboxBtnType = null;


    // ***************** Constructors *********************
    
    UIEventImpl(String eventName, Component target, WebUserSession sess)
    {
        this.name = eventName;
        this.target = target;
        this.session = sess;
        if (DocmaConstants.DEBUG) {
            Log.info("Creating UIEvent with event name: '" + name + "'. Target class: " + 
                    ((target == null) ? target : target.getClass().getName()));
        }
    }
    
    public UIEventImpl(Event zkEvent, WebUserSession sess)
    {
        initFieldsFromZkEvent(zkEvent);
        this.session = sess;
        if (DocmaConstants.DEBUG) {
            Log.info("Creating UIEvent from zk event. Event name: '" + name + 
                     "'. Target class: " +
                    ((target == null) ? target : target.getClass().getName()));
        }
    }

    // ***************** Interface UIEvent *********************
    
    public String getName() 
    {
        return name;
    }

    public String getTargetId() 
    {
        return (target != null) ? target.getId() : null;
    }
    
    public WebUserSession getSession()
    {
        return session;
    }

    public boolean isClick() 
    {
        return isMessageboxClick || "onClick".equalsIgnoreCase(name);
    }
    
    public boolean isOpen() 
    {
        return "onOpen".equalsIgnoreCase(name);
    }

    public boolean isSelect() 
    {
        return "onSelect".equalsIgnoreCase(name);
    }

    public boolean isButtonTarget() 
    {
        return isMessageboxClick || (target instanceof Button);
    }

    public boolean isTabTarget() 
    {
        return (target instanceof Tab);
    }

    public boolean isMenuItemTarget() 
    {
        return (target instanceof Menuitem);
    }

    public boolean isMenuTarget() 
    {
        return (target instanceof Menupopup);
    }

//    public boolean isTabSelect() 
//    {
//        return (target instanceof Tab) && "onSelect".equalsIgnoreCase(name);
//    }
//    
//    public boolean isButtonClick() 
//    {
//        return isMessageboxClick || 
//               ((target instanceof Button) && "onClick".equalsIgnoreCase(name));
//    }
//
//    public boolean isMenuItemClick() 
//    {
//        return (target instanceof Menuitem) && "onClick".equalsIgnoreCase(name);
//    }
//    
//    public boolean isMenuOpen() 
//    {
//        return (target instanceof Menupopup) && "onOpen".equalsIgnoreCase(name);
//    }
    
    public ButtonType getButtonType() 
    {
        if (isMessageboxClick && (messageboxBtnType != null)) {
            return messageboxBtnType;
        } else if (target instanceof Button) {
            return ButtonType.USER_DEFINED;
        } else {
            return null;
        }
    }
    
    // ***************** Private methods *********************
    
    private ButtonType zkMsgboxEventToButtonType(String evt)
    {
        if (evt == null) {
            return null;
        }
        
        if (evt.equals(Messagebox.ON_ABORT)) {
            return ButtonType.ABORT;
        }
        if (evt.equals(Messagebox.ON_CANCEL)) {
            return ButtonType.CANCEL;
        }
        if (evt.equals(Messagebox.ON_IGNORE)) {
            return ButtonType.IGNORE;
        }
        if (evt.equals(Messagebox.ON_NO)) {
            return ButtonType.NO;
        }
        if (evt.equals(Messagebox.ON_OK)) {
            return ButtonType.OK;
        }
        if (evt.equals(Messagebox.ON_RETRY)) {
            return ButtonType.RETRY;
        }
        if (evt.equals(Messagebox.ON_YES)) {
            return ButtonType.YES;
        }
        
        return null;
    }

    
    private void initFieldsFromZkEvent(Event zkEvent)
    {
        this.name = zkEvent.getName();
        this.target = zkEvent.getTarget();
        this.isMessageboxClick = target instanceof MessageboxDlg;
        if (this.isMessageboxClick) {
            messageboxBtnType = zkMsgboxEventToButtonType(name);
        }
    }
}
