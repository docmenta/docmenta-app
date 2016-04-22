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

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Messagebox.ClickEvent;
import org.zkoss.zul.Messagebox.Button;

/**
 *
 * @author MP
 */
public class UIEventImpl implements UIEvent
{
    private final Event zkEvent;

    public UIEventImpl(Event zkEvent)
    {
        this.zkEvent = zkEvent;
    }

    public String getName() 
    {
        return zkEvent.getName();
    }

    public boolean isButtonClick() 
    {
        return (zkEvent instanceof ClickEvent);
    }

    public ButtonType getButtonType() 
    {
        if (isButtonClick()) {
            return zkButtonToButtonType(((ClickEvent) zkEvent).getButton());
        } else {
            return null;
        }
    }
    
    private ButtonType zkButtonToButtonType(Button zkBtn)
    {
        if (zkBtn == null) {
            return ButtonType.CLOSE;
        }
        
        if (zkBtn.equals(Button.ABORT)) {
            return ButtonType.ABORT;
        }
        if (zkBtn.equals(Button.CANCEL)) {
            return ButtonType.CANCEL;
        }
        if (zkBtn.equals(Button.IGNORE)) {
            return ButtonType.IGNORE;
        }
        if (zkBtn.equals(Button.NO)) {
            return ButtonType.NO;
        }
        if (zkBtn.equals(Button.OK)) {
            return ButtonType.OK;
        }
        if (zkBtn.equals(Button.RETRY)) {
            return ButtonType.RETRY;
        }
        if (zkBtn.equals(Button.YES)) {
            return ButtonType.YES;
        }
        
        // Map any unknown zk button to the button type CLOSE.
        return ButtonType.CLOSE;
    }
}
