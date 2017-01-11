/*
 * LogEntriesImpl.java
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

import org.docma.coreapi.implementation.DefaultLog;
import org.docma.plugin.LogEntry;
import org.docma.plugin.LogEntries;

/**
 *
 * @author MP
 */
public class LogEntriesImpl implements LogEntries
{
    private final DefaultLog docmaLog;

    public LogEntriesImpl(DefaultLog docmaLog) 
    {
        this.docmaLog = docmaLog;
    }

    public int getInfoCount() 
    {
        return docmaLog.getInfoCount();
    }

    public int getWarningCount() 
    {
        return docmaLog.getWarningCount();
    }

    public int getErrorCount() 
    {
        return docmaLog.getErrorCount();
    }

    public LogEntry[] getLog() 
    {
        return docmaLog.getLog(true, true, true);
    }

    public LogEntry[] getInfos() 
    {
        return docmaLog.getLog(true, false, false);
    }

    public LogEntry[] getWarnings() 
    {
        return docmaLog.getLog(false, true, false);
    }

    public LogEntry[] getErrors() 
    {
        return docmaLog.getLog(false, false, true);
    }
    
}
