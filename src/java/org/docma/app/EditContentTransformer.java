/*
 * EditContentTransformer.java
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

import org.docma.plugin.DocmaException;
import org.docma.plugin.LogEntries;
import org.docma.plugin.LogLevel;
import org.docma.plugin.implementation.HTMLRuleContextImpl;
import org.docma.plugin.implementation.LogEntriesImpl;
import org.docma.plugin.implementation.StoreConnectionImpl;
import org.docma.plugin.rules.HTMLRule;


/**
 *
 * @author MP
 */
public class EditContentTransformer
{
    // private static final String PADDING_PATTERN = "padding-left:";

    public static final String PROP_TRANSFORM_TRIM_EMPTY_PARAS = "trim_empty_paras";
    public static final String PROP_TRANSFORM_TRIM_FIGURE_SPACES = "trim_figure_spaces";

    public static LogEntries prepareHTMLForSave(StringBuilder content,
                                                String nodeId,
                                                Map<Object, Object> props,
                                                DocmaSession docmaSess)
    {
        return applyHTMLRules(content, nodeId, props, true, true, docmaSess);
    }

    public static LogEntries checkHTML(StringBuilder content,
                                       String nodeId,
                                       Map<Object, Object> props,
                                       boolean allowAutoCorrect,
                                       DocmaSession docmaSess)
    {
        return applyHTMLRules(content, nodeId, props, allowAutoCorrect, false, docmaSess);
    }

    private static LogEntries applyHTMLRules(StringBuilder content,
                                             String nodeId, 
                                             Map<Object, Object> props, 
                                             boolean allowAutoCorrect,
                                             boolean isModeSave,
                                             DocmaSession docmaSess) throws DocmaException
    {
        RulesManager rm = docmaSess.getRulesManager();
        RuleConfig[] rules = rm.getAllRules();
        StoreConnectionImpl storeConn = (StoreConnectionImpl) docmaSess.getPluginStoreConnection();
        HTMLRuleContextImpl ctx = new HTMLRuleContextImpl(storeConn, content);
        if (props != null) {
            ctx.setProperties(props);
        }
        if (isModeSave) {
            ctx.setModeSave();
        } else {
            ctx.setModeCheck();
        }
        ctx.setAllowAutoCorrect(allowAutoCorrect);
        ctx.setNodeId(nodeId);

        String contentStr = null;
        for (RuleConfig rc : rules) {
            if (rc.isRuleEnabled() && rc.isApplicableForStore(docmaSess.getStoreId())) {
                String ruleId = rc.getId();
                Object pobj = (props == null) ? null : props.get(ruleId);
                String v = (pobj == null) ? "" : pobj.toString();
                
                // SPECIAL CASE: If this is the quick-links rule and no property 
                // has been set, then set property based on user's profile setting.
                if (v.equals("") && RulesManager.QUICK_LINKS_ID.equals(ruleId)) {
                    v = docmaSess.getUserProperty(DocmaConstants.PROP_USER_QUICKLINKS_ENABLED);
                    if ((v == null) || v.equals("")) {
                        v = "false";  // by default quick-links are disabled if no user property has been set
                    }
                }
                
                // Skip rule if it is turned off by supplied properties
                if (v.equalsIgnoreCase("false") || v.equalsIgnoreCase("off")) {
                    continue;   // skip this rule
                }
                
                // Skip rule if it is turned off by default, and not turned on by property
                if (! rc.isDefaultOn()) {   // rule is turned off by default
                    if (! (v.equalsIgnoreCase("true") || v.equalsIgnoreCase("on"))) {
                        continue;   // skip this rule
                    }
                }

                // Apply the rule to the content
                Object obj = rc.acquireRuleInstance();
                try {
                    if (obj instanceof HTMLRule) {
                        HTMLRule hr = (HTMLRule) obj;
                        ctx.setActiveRule(rc);
                        try {
                            hr.startBatch();
                        } catch (Exception ex) {
                            ctx.log(LogLevel.ERROR, "Exception in startBatch() of rule " + rc.getId() + ": " + ex.getMessage());
                        }
                        try {
                            if (contentStr == null) {  // initialize on first rule to be applied
                                contentStr = content.toString();
                            }
                            String res = hr.apply(contentStr, ctx);
                            if (allowAutoCorrect && (res != null)) {
                                contentStr = res;
                                content.replace(0, content.length(), res);
                            }
                        } catch (Exception ex) {
                            ctx.log(LogLevel.ERROR, "Exception in applying rule " + rc.getId() + ": " + ex.getMessage());
                        }
                        try {
                            hr.finishBatch();
                        } catch (Exception ex) {
                            ctx.log(LogLevel.INFO, "Exception in finishBatch() of rule " + rc.getId() + ": " + ex.getMessage());
                        }
                    }
                } finally {
                    if (obj != null) rc.releaseRuleInstance(obj);
                }
            }
        }
        
        return new LogEntriesImpl(ctx.getLog());
    }

}
