/*
 * FileURLTransformer_Default.java
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

import java.util.SortedMap;
import org.docma.coreapi.*;

/**
 *
 * @author MP
 */
public class FileURLTransformer_Default implements FileURLTransformer
{
    private DocmaSession docmaSess;
    private String[] filterApplics;
    private ExportLog exportLog;
    private SortedMap transformed;

    public FileURLTransformer_Default(DocmaSession docmaSess,
                                      String[] filterApplics,
                                      ExportLog exportLog,
                                      SortedMap transformed)
    {
        this.docmaSess = docmaSess;
        this.filterApplics = filterApplics;
        this.exportLog = exportLog;
        this.transformed = transformed;
    }

    public String getFileURLByAlias(String alias)
    {
        DocmaNode file_node = docmaSess.getApplicableNodeByLinkAlias(alias, filterApplics);
        if (file_node != null) {
            String ext = file_node.getFileExtension();
            if (ext == null) ext = "";
            else if (ext.length() > 0) ext = "." + ext;
            String file_url = "files/" + alias + ext;
            if (transformed != null) {
                transformed.put(file_url, file_node);
            }
            return file_url;
        } else {
            exportLog.error("Referenced file does not exist or is not applicable: " + alias);
            return null;
        }
    }

}
