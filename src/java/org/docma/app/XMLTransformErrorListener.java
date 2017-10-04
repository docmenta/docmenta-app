/*
 * XMLTransformErrorListener.java
 * 
 *  Copyright (C) 2017  Manfred Paula, http://www.docmenta.org
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

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import org.docma.coreapi.ExportLog;

/**
 *
 * @author MP
 */
public class XMLTransformErrorListener implements ErrorListener
{
    private final ExportLog exportLog;
    
    XMLTransformErrorListener(ExportLog exportLog)
    {
        // exportLog may be null (if errors shall be ignored)
        this.exportLog = exportLog;
    }

    public void warning(TransformerException exception) throws TransformerException 
    {
        if (exportLog != null) {
            exportLog.warningMsg(exception.getLocalizedMessage());
        }
    }

    public void error(TransformerException exception) throws TransformerException 
    {
        if (exportLog != null) {
            exportLog.errorMsg(exception.getLocalizedMessage());
        }
    }

    public void fatalError(TransformerException exception) throws TransformerException 
    {
        if (exportLog != null) {
            exportLog.errorMsg("FATAL: " + exception.getLocalizedMessage());
        }
    }
}
