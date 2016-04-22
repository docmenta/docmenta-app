/*
 * ApplyTemplate.java
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

package org.docma.plugin.examples;

import org.docma.plugin.*;
import java.util.*;
import java.io.*;

/**
 *
 * @author MP
 */
public class ApplyTemplate implements AutoFormat
{
    private ExportContext exportCtx;
    private Map<String, String> templateMap;

    public void initialize(ExportContext ctx)
    {
        exportCtx = ctx;
        templateMap = new HashMap<String, String>();
    }

    public void finished()
    {
        exportCtx = null;
        templateMap = null;
    }

    public void transform(TransformationContext ctx) throws Exception
    {
        String alias = ctx.getArgument(0);
        String template = templateMap.get(alias);
        if (template == null) {
            template = exportCtx.getContentStringByAlias(alias, true);
            if (template == null) {
                throw new Exception("Could not find template with alias " + alias);
            }
            templateMap.put(alias, template);
        }
        String title = ctx.getTagAttributes().get("title");
        if (title == null) title = "";
        Writer out = ctx.getWriter();
        out.write(template.replace("$content", ctx.getInnerString()).replace("$title", title));
    }

    public String getShortInfo(String languageCode)
    {
        return PluginUtil.getResourceString(this.getClass(), languageCode, "shortInfo");
    }

    public String getLongInfo(String languageCode)
    {
        return PluginUtil.getResourceString(this.getClass(), languageCode, "longInfo");
    }

}
