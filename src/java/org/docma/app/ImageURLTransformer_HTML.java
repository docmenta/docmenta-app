/*
 * ImageURLTransformer_HTML.java
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
public class ImageURLTransformer_HTML implements ImageURLTransformer
{
    private DocmaSession docmaSess;
    private String[] filterApplics;
    private ExportLog exportLog;
    private SortedMap transformed;

    public ImageURLTransformer_HTML(DocmaSession docmaSess,
                                    String[] filterApplics,
                                    ExportLog exportLog,
                                    SortedMap transformed)
    {
        this.docmaSess = docmaSess;
        this.filterApplics = filterApplics;
        this.exportLog = exportLog;
        this.transformed = transformed;
    }

    public String getImageURLByAlias(String img_alias)
    {
        DocmaNode img_node = docmaSess.getApplicableNodeByLinkAlias(img_alias, filterApplics);
        if (img_node != null) {
            String img_url = "images/" + img_node.getDefaultFileName();
            if (transformed != null) {
                transformed.put(img_url, img_node);
            }
            return img_url;
        } else {
            exportLog.error("Referenced image does not exist or is not applicable: " + img_alias);
            return null;
        }
    }

}
