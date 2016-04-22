/*
 * HTMLCopyStrategy.java
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

import java.util.*;
import java.util.regex.*;
import org.docma.coreapi.*;
import org.docma.util.Log;

/**
 *
 * @author MP
 */
public class HTMLCopyStrategy extends DefaultContentCopyStrategy
{
    private static Pattern id_pattern = Pattern.compile("\\s+id\\s*=\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
    private Map aliasMap;

    public HTMLCopyStrategy(Map aliasMap)
    {
        this.aliasMap = aliasMap;
    }

    public void copyContent(DocContent sourceContent,
                            DocStoreSession sourceSession,
                            DocContent targetContent,
                            DocStoreSession targetSession) throws DocException
    {
        if ((sourceContent instanceof DocXML) && (targetContent instanceof DocXML)) {

            // get content anchors and replace anchor names with new names if mapping exists
            String content = sourceContent.getContentString();
            if ((aliasMap != null) && !aliasMap.isEmpty()) {
                DocmaAnchor[] anchors = ContentUtil.getContentAnchors(content);
                String[] new_names = new String[anchors.length];
                boolean has_mapping = false;
                for (int i=0; i < anchors.length; i++) {
                    // see if anchor name has to be mapped
                    new_names[i] = (String) aliasMap.get(anchors[i].getAlias());
                    if (new_names[i] != null) has_mapping = true;
                }
                if (has_mapping) {
                    content = renameContentAnchors(content, anchors, new_names);
                }
            }
            targetContent.setContentString(content);
        } else {
            super.copyContent(sourceContent, sourceSession, targetContent, targetSession);
        }
    }

    private String renameContentAnchors(String content, DocmaAnchor[] anchors, String[] newNames)
    {
        StringBuilder sb = new StringBuilder(content.length() + 100);
        Matcher matcher = id_pattern.matcher(content);
        int copy_start = 0;
        for (int i=0; i < newNames.length; i++) {
            String new_alias = newNames[i];
            if (new_alias != null) {
                int tag_pos = anchors[i].getTagPosition();
                // It is assumed that anchors array is sorted by tag-position of the anchors!
                if (tag_pos < copy_start) {
                    Log.error("renameContentAnchors(): Anchor positions are not sorted! Skipping anchor.");
                    continue;
                }
                if (matcher.find(tag_pos)) {  // search id pattern starting from anchor tag
                    if (anchors[i].getAlias().equals(matcher.group(1))) {
                        int alias_start = matcher.start(1);
                        int alias_end = matcher.end(1);
                        sb.append(content, copy_start, alias_start);
                        sb.append(new_alias);
                        copy_start = alias_end;
                    }
                }
            }
        }
        if (copy_start < content.length()) {
            sb.append(content, copy_start, content.length());
        }
        return sb.toString();
    }

}
