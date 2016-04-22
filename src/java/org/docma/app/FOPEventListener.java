/*
 * FOPEventListener.java
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

package org.docma.app;

import java.util.HashSet;
import org.apache.fop.events.Event;
import org.apache.fop.events.EventFormatter;
import org.apache.fop.events.EventListener;
import org.apache.fop.events.model.EventSeverity;

import org.docma.coreapi.ExportLog;

/**
 *
 * @author MP
 */
public class FOPEventListener implements EventListener
{
    private static final boolean SHOW_SAME_WARNINGS_ON_PAGE = false;
    
    private ExportLog docmaLog;
    private HashSet<String> warningCache = new HashSet<String>();  // used to avoid showing the same warning twice

    public FOPEventListener(ExportLog docmaLog)
    {
        this.docmaLog = docmaLog;
    }

    public void processEvent(Event event)
    {
        String msg = EventFormatter.format(event);
        if (msg == null) msg = "-";  // Avoid nullpointer exception; should not occur
        EventSeverity severity = event.getSeverity();
        if (severity == EventSeverity.INFO) {
            docmaLog.infoMsg(msg);
            warningCache.clear();  // Note: start of a new page is notified by an info message 
        } else if (severity == EventSeverity.WARN) {
            // Show the same warning only once on the same page
            if (warningCache.contains(msg)) { 
                // Either show duplicated warnings as info messages or suppress duplicated warnings 
                if (SHOW_SAME_WARNINGS_ON_PAGE) {
                    docmaLog.infoMsg(msg);  // Show same warnings as info messages (to reduce number of warnings)
                }
            } else { 
                // Show all FOP warnings as info messages
                // if (msg.startsWith("Font") && msg.contains("not found. Substituting")) {
                    docmaLog.infoMsg(msg);
                // } else {
                //     docmaLog.warningMsg(msg);
                // }
                warningCache.add(msg);
            }
        } else if (severity == EventSeverity.ERROR) {
            docmaLog.errorMsg(msg);
        } else if (severity == EventSeverity.FATAL) {
            docmaLog.errorMsg(msg);
        } else {
            docmaLog.infoMsg(msg);
        }
    }

}
