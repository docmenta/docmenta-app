/*
 * ImageURLTransformer_PDF.java
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
public class ImageURLTransformer_PDF implements ImageURLTransformer
{
    private DocmaSession docmaSess;
    private String[] filterApplics;
    private ExportLog exportLog;
    private SortedMap transformed;
    private boolean directAccess;

    public ImageURLTransformer_PDF(DocmaSession docmaSess,
                                   String[] filterApplics,
                                   ExportLog exportLog,
                                   SortedMap transformed, 
                                   boolean directAccess)
    {
        this.docmaSess = docmaSess;
        this.filterApplics = filterApplics;
        this.exportLog = exportLog;
        this.transformed = transformed;
        this.directAccess = directAccess;
    }

    public String getImageURLByAlias(String img_alias)
    {
        DocmaNode img_node = docmaSess.getApplicableNodeByLinkAlias(img_alias, filterApplics);
        if (img_node != null) {
            String img_url;
            if (directAccess) {
                String trans_mode = img_node.getTranslationMode();
                if ((trans_mode != null) && img_node.isContentTranslated()) {
                    img_url = img_node.getContentRelativeURL(trans_mode);
                    // "translations/" + trans_mode + "/images/" + img_node.getId() + "." + img_node.getFileExtension();
                } else {
                    img_url = img_node.getContentRelativeURL(null);
                    // "content/images/" + img_node.getId() + "." + img_node.getFileExtension();
                }
            } else {
                img_url = img_node.getId() + "." + img_node.getFileExtension();
            }
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
