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

import org.docma.plugin.web.ButtonType;
import org.docma.plugin.web.UIEvent;
import org.docma.plugin.web.WebUserSession;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Button;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.Menupopup;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Tab;

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
    }
    
    UIEventImpl(Event zkEvent, WebUserSession sess)
    {
        initFieldsFromZkEvent(zkEvent);
        this.session = sess;
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
        if (isMessageboxClick) {
            return messageboxBtnType;
        } else {
            return null;
        }
    }
    
    // ***************** Private methods *********************
    
    private ButtonType zkButtonToButtonType(Messagebox.Button zkBtn)
    {
        if (zkBtn == null) {
            return ButtonType.CLOSE;
        }
        
        if (zkBtn.equals(Messagebox.Button.ABORT)) {
            return ButtonType.ABORT;
        }
        if (zkBtn.equals(Messagebox.Button.CANCEL)) {
            return ButtonType.CANCEL;
        }
        if (zkBtn.equals(Messagebox.Button.IGNORE)) {
            return ButtonType.IGNORE;
        }
        if (zkBtn.equals(Messagebox.Button.NO)) {
            return ButtonType.NO;
        }
        if (zkBtn.equals(Messagebox.Button.OK)) {
            return ButtonType.OK;
        }
        if (zkBtn.equals(Messagebox.Button.RETRY)) {
            return ButtonType.RETRY;
        }
        if (zkBtn.equals(Messagebox.Button.YES)) {
            return ButtonType.YES;
        }
        
        // Map any unknown zk button to the button type CLOSE.
        return ButtonType.CLOSE;
    }

    
    private void initFieldsFromZkEvent(Event zkEvent)
    {
        this.name = zkEvent.getName();
        this.target = zkEvent.getTarget();
        this.isMessageboxClick = zkEvent instanceof Messagebox.ClickEvent;
        if (this.isMessageboxClick) {
            messageboxBtnType = zkButtonToButtonType(((Messagebox.ClickEvent) zkEvent).getButton());
        }
    }
}
