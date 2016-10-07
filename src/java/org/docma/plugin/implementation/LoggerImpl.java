/*
 * LoggerImpl.java
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

import org.docma.plugin.Logger;

/**
 *
 * @author MP
 */
public class LoggerImpl implements Logger
{

    public void info(String msg) 
    {
        org.docma.util.Log.info(msg);
    }

    public void warning(String msg) 
    {
        org.docma.util.Log.warning(msg);
    }

    public void error(String msg) 
    {
        org.docma.util.Log.error(msg);
    }
    
}
