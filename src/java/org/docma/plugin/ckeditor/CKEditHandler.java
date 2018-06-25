/*
 * CKEditHandler.java
 * 
 *  Copyright (C) 2018  Manfred Paula, http://www.docmenta.org
 *   
 *  This file is part of Docmenta. Docmenta is free software: you can 
 *  redistribute it and/or modify it under the terms of the GNU Lesser 
 *  General Public License as published by the Free Software Foundation, 
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Docmenta.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.docma.plugin.ckeditor;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.docma.plugin.CharEntity;
import org.docma.plugin.internals.ContentEditHandler;
import org.docma.plugin.web.WebUserSession;
import org.docma.util.DocmaUtil;

/**
 *
 * @author MP
 */
public class CKEditHandler extends ContentEditHandler
{
    public static final String FALLBACK_FIGURE_CSS_CLS = "figure";

    private static final Map<String, CKEditPlugin> PLUGIN_REGISTRY = new HashMap<String, CKEditPlugin>();

    // Character entities as required by CKEditor entities_additional configuration    
    private String entitiesNumeric = ""; 
    private String entitiesSymbolic = ""; 
    
    // JavaScript array as required by CKEditor specialChars configuration.
    private String specialChars = "[]";  
    
    private String ckEditorInitTemplate = null;
    private String ckEditorToolbarTemplate = null;
    
    /* -------------  Interface ContentAppHandler ----------------- */
    
    @Override
    public void setCharEntities(CharEntity[] entities) 
    {
        entitiesNumeric = createEntitiesAdditional(entities, true);
        entitiesSymbolic = createEntitiesAdditional(entities, false);
        specialChars = createSpecialCharsJSArray(entities);
    }
    
    /* -------------  Other public methods  ----------------- */
    
    public String getEntitiesAdditionalNumeric()
    {
        return entitiesNumeric;
    }
    
    public String getEntitiesAdditionalSymbolic()
    {
        return entitiesSymbolic;
    }
    
    public String getSpecialCharsJSArray()
    {
        // return "[ '&quot;', '&rsquo;', [ '&custom;', 'Custom label' ] ]";
        return specialChars;
    }
    
    @Override
    public String prepareContentForEdit(String content, WebUserSession webSess)
    {
        return CKEditUtils.prepareContentForEdit(content, getApplicationId(), webSess);
    }

    @Override
    public String prepareContentForSave(String content, WebUserSession webSess)
    {
        return CKEditUtils.prepareContentForSave(content, getApplicationId(), webSess);
    }

    public String getCKEditorInitTemplate()
    {
        if (ckEditorInitTemplate == null) {
            try {
                File ckAppDir = new File(webBaseDirectory, relativeAppPath);
                File tmplFile = new File(ckAppDir, "ckedit_init_default.tmpl");
                ckEditorInitTemplate = DocmaUtil.readFileToString(tmplFile);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return ckEditorInitTemplate;
    }
    
    public String getCKEditorToolbarTemplate()
    {
        if (ckEditorToolbarTemplate == null) {
            try {
                File ckAppDir = new File(webBaseDirectory, relativeAppPath);
                File tmplFile = new File(ckAppDir, "ckedit_toolbar_default.tmpl");
                ckEditorToolbarTemplate = DocmaUtil.readFileToString(tmplFile);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return ckEditorToolbarTemplate;
    }
    
    public String replacePlaceholders(String template, Map<String, String> placeholders)
    {
        StringBuilder buf = new StringBuilder();
        
        // Replace placeholders in template
        int copypos = 0;
        int startpos = 0;
        while (startpos < template.length()) {
            int pstart = template.indexOf("###", startpos);
            if (pstart < 0) {
                break;
            }
            int nameStart = pstart + "###".length();
            int nameEnd = template.indexOf("###", nameStart);
            if (nameEnd < 0) {
                break;
            }
            int pend = nameEnd + "###".length();  // position after placeholder
            String pholder = template.substring(pstart, pend);
            String replacement = placeholders.get(pholder);
            if (replacement == null) {  // no placeholder
                startpos = pstart + 1;
            } else {
                // Replace placeholder
                buf.append(template, copypos, pstart);  // copy template up to start of placeholder
                buf.append(replacement);  // output replacement string
                copypos = pend;  // position after placeholder
                // Continue search after the placeholder
                startpos = pend;
            }
        }
        
        // Copy remaining
        if (copypos < template.length()) {
            buf.append(template, copypos, template.length());
        }
        return buf.toString();
    }
    
    public CKEditPlugin getPlugin()
    {
        return PLUGIN_REGISTRY.get(getApplicationId());
    }
    
    /* -------------  Public static methods  ----------------- */
    
    public static void registerPlugin(String app_id, CKEditPlugin plugin)
    {
        PLUGIN_REGISTRY.put(app_id, plugin);
    }

    public static void unregisterPlugin(String app_id, CKEditPlugin plugin)
    {
        CKEditPlugin current = PLUGIN_REGISTRY.get(app_id);
        if (current == plugin) {
            PLUGIN_REGISTRY.remove(app_id);
        }
    }
    
    public static CKEditPlugin getRegisteredPlugin(String app_id)
    {
        return PLUGIN_REGISTRY.get(app_id);
    }
    
    /* -------------  Private methods  ----------------- */
    
    private String createEntitiesAdditional(CharEntity[] entities, boolean numeric)
    {
        StringBuilder sb = new StringBuilder();
        if (entities != null) {
            boolean notFirst = false;
            for (CharEntity ce : entities) {
                // Get the preferred entity representation (symbolic / numeric).
                String ent = numeric ? ce.getNumeric() : ce.getSymbolic();
                if ((ent == null) || ent.equals("")) {  // Preferred representation does not exist.
                    // Fall back to the other representation.
                    ent = numeric ? ce.getSymbolic() : ce.getNumeric();
                }
                if ((ent == null) || ent.equals("")) {
                    continue;
                }
                if (notFirst) {
                    sb.append(",");
                }
                sb.append(getEntityNameOrNumber(ent));
                notFirst = true;
            }
        }
        return sb.toString();
    }
    
    private String createSpecialCharsJSArray(CharEntity[] entities)
    {
        StringBuilder sb = new StringBuilder("[ ");
        if (entities != null) {
            boolean notFirst = false;
            for (CharEntity ce : entities) {
                if (! ce.isSelectable()) {
                    continue;
                }
                String ent = ce.getSymbolic();
                if ((ent == null) || ent.equals("")) {
                    ent = ce.getNumeric();
                }
                if ((ent == null) || ent.equals("")) {
                    continue;
                }
                if (notFirst) {
                    sb.append(", ");
                }
                String desc = ce.getDescription();
                desc = (desc != null) ? desc.replace('\'', ' ').replace('\\', ' ') : "";
                if (desc.equals("")) {
                    sb.append("'").append(ent).append("'");
                } else {
                    sb.append("[ '").append(ent).append("', '").append(desc).append("' ]");
                }
                notFirst = true;
            }
        }
        sb.append(" ]");
        return sb.toString();
    }
    
    private String getEntityNameOrNumber(String ent)
    {
        if (ent.startsWith("&") && ent.endsWith(";")) {
            return ent.substring(1, ent.length() - 1);
        } else {
            return ent;
        }
    }
}
