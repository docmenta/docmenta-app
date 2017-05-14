/*
 * RulesManager.java
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
package org.docma.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.docma.coreapi.DocException;
import org.docma.util.Log;

/**
 *
 * @author MP
 */
public class RulesManager 
{
    private static final String RULE_FILE_EXTENSION = ".properties";
    
    public static final String BASE_RULE_ID = "base";
    public static final String QUICK_LINKS_ID = "quicklinks";
    
    private static final List<String> DEFAULT_RULES = 
                                        Arrays.asList(new String[] { BASE_RULE_ID, QUICK_LINKS_ID, });
    
    private final DocmaApplication docmaApp;
    private final File rulesDir;
    private final SortedMap<String, RuleConfig> rules = new TreeMap();

    public RulesManager(DocmaApplication docmaApp, File dir) 
    {
        this.docmaApp = docmaApp;
        this.rulesDir = dir;
        if (! rulesDir.exists()) {
            rulesDir.mkdirs();
        }
        readRulesFromDir();
        createDefaultRules();
    }
    
    public RuleConfig createTransientRule()
    {
        return new RuleConfig(this);
    }
    
    public synchronized void saveRule(RuleConfig rule) throws DocException
    {
        saveRuleProps(rule.getId(), rule.getProperties());
        addRuleToList(rule);
    }
    
    public synchronized void deleteRule(String ruleId) throws DocException
    {
        File f = getRuleFile(ruleId);
        if (! f.delete()) {
            throw new DocException("Could not delete rule file: " + f);
        }
        removeRuleFromList(ruleId);
    }
    
    public synchronized boolean loadRule(String ruleId) throws DocException
    {
        File f = getRuleFile(ruleId);
        if (f.exists()) {
            Properties p;
            try {
                p = loadRuleProps(f);
            } catch (Exception ex) {
                throw new DocException(ex);
            }
            RuleConfig rc = getRule(ruleId);
            if (rc == null) {
                rc = new RuleConfig(this, ruleId, p);
                addRuleToList(rc);
            } else {
                rc.setProperties(p);
            }
            return true;
        } else {
            return false;
        }
    }
    
    public synchronized RuleConfig getRule(String ruleId) 
    {
        return rules.get(ruleId.toLowerCase());
    }
    
    public synchronized RuleConfig[] getAllRules()
    {
        Collection<RuleConfig> res = rules.values();
        return res.toArray(new RuleConfig[res.size()]);
    }

    public synchronized RuleConfig[] getActiveRules(String storeId)
    {
        List<RuleConfig> res = new ArrayList();
        for (RuleConfig rc : rules.values()) {
            if (rc.isRuleEnabled() && 
                rc.isApplicableForStore(storeId) && 
                (rc.getRuleClass() != null)) {
                res.add(rc);
            }
        }
        return res.toArray(new RuleConfig[res.size()]);
    }
    
    public boolean isDefaultRule(String ruleId)
    {
        return DEFAULT_RULES.contains(ruleId);
    }

    public DocmaApplication getDocmaApplication()
    {
        return docmaApp;
    }
    
    /* ------------ Private methods ---------------- */

    private void addRuleToList(RuleConfig rule) 
    {
        rules.put(rule.getId().toLowerCase(), rule);
    }
    
    private void removeRuleFromList(String ruleId)
    {
        rules.remove(ruleId.toLowerCase());
    }
    
    private synchronized void readRulesFromDir()
    {
        File[] farr = rulesDir.listFiles();
        rules.clear();
        if (farr == null) {  // rulesDir does not exist
            return;
        }
        for (File f : farr) {
            String fn = f.getName();
            if (fn.endsWith(RULE_FILE_EXTENSION)) {
                try {
                    Properties props = loadRuleProps(f);
                    String ruleId = fn.substring(0, fn.lastIndexOf('.'));
                    addRuleToList(new RuleConfig(this, ruleId, props));
                } catch (Exception ex) {
                    Log.error("Could not load rule properties " + f.getName() + ": " + ex.getMessage());
                }
            }
        }
    }
    
    private synchronized void createDefaultRules()
    {
        boolean create_quicklinks = (getRule(QUICK_LINKS_ID) == null);
        boolean create_baserule = (getRule(BASE_RULE_ID) == null);

        String quicklinks_cls = QuickLinkRule.class.getName();
        String baserule_cls = BaseRule.class.getName();
        
        if (create_quicklinks || create_baserule) {
            try {
                docmaApp.registerRuleClasses(baserule_cls, quicklinks_cls);
            } catch (Exception ex) {
                Log.error("Could not register default rule classes: " + ex.getMessage());
            }
        }
        
        // Create Quick-Link rule if not existent
        if (create_quicklinks) {
            try {
                RuleConfig rc = createTransientRule();
                rc.setId(QUICK_LINKS_ID);
                rc.setRuleClassName(quicklinks_cls);
                rc.setTitle(rc.getShortInfo("en"));
                rc.setRuleEnabled(true);
                rc.setDefaultOn(true);
                rc.setScopeAll();

                String chk = QuickLinkRule.CHECK_ID_TRANSFORM;
                rc.setExecuteOnCheck(chk, false);
                rc.setCorrectOnCheck(chk, false);
                rc.setExecuteOnSave(chk,  true);
                rc.setCorrectOnSave(chk,  true);
                
                saveRule(rc);
            } catch (Exception ex) {
                Log.error("Could not create rule " + QUICK_LINKS_ID + ": " + ex.getMessage());
            }
        }
        
        // Create base rule if not existent
        if (create_baserule) {
            try {
                RuleConfig rc = createTransientRule();
                rc.setId(BASE_RULE_ID);
                rc.setRuleClassName(baserule_cls);
                rc.setArgsLine("attribute_required=div,span content_required=span");
                rc.setTitle(rc.getShortInfo("en"));
                rc.setRuleEnabled(true);
                rc.setDefaultOn(true);
                rc.setScopeAll();

                // Attribute required setting
                String chk = BaseRule.CHECK_ID_ATTRIBUTE_REQUIRED;
                rc.setExecuteOnCheck(chk, true);
                rc.setCorrectOnCheck(chk, false);
                rc.setExecuteOnSave(chk,  true);
                rc.setCorrectOnSave(chk,  true);
                
                // Broken link setting
                chk = BaseRule.CHECK_ID_BROKEN_LINK;
                rc.setExecuteOnCheck(chk, true);
                rc.setExecuteOnSave(chk,  false);
                
                // Content required setting
                chk = BaseRule.CHECK_ID_CONTENT_REQUIRED;
                rc.setExecuteOnCheck(chk, true);
                rc.setCorrectOnCheck(chk, false);
                rc.setExecuteOnSave(chk,  true);
                rc.setCorrectOnSave(chk,  true);
                
                // Check image src setting 
                chk = BaseRule.CHECK_ID_INVALID_IMAGE_SRC;
                rc.setExecuteOnCheck(chk, true);
                rc.setExecuteOnSave(chk,  false);
                
                // Check target type setting
                chk = BaseRule.CHECK_ID_INVALID_TARGET_TYPE;
                rc.setExecuteOnCheck(chk, true);
                rc.setExecuteOnSave(chk,  false);
                
                // Trim empty paras setting
                chk = BaseRule.CHECK_ID_TRIM_EMPTY_PARAS;
                rc.setExecuteOnCheck(chk, false);
                rc.setCorrectOnCheck(chk, false);
                rc.setExecuteOnSave(chk,  true);
                rc.setCorrectOnSave(chk,  true);

                // Trim figure spaces
                chk = BaseRule.CHECK_ID_TRIM_FIGURE_SPACES;
                rc.setExecuteOnCheck(chk, false);
                rc.setCorrectOnCheck(chk, false);
                rc.setExecuteOnSave(chk,  true);
                rc.setCorrectOnSave(chk,  true);

                saveRule(rc);
            } catch (Exception ex) {
                Log.error("Could not create rule " + BASE_RULE_ID + ": " + ex.getMessage());
            }
        }
    }
    
    private File getRuleFile(String ruleId) 
    {
        return new File(rulesDir, ruleId + RULE_FILE_EXTENSION);
    }
    
    private Properties loadRuleProps(File propFile) throws Exception
    {
        if (DocmaConstants.DEBUG) {
            Log.info("Loading rule properties: " + propFile);
        }
        Properties props = new Properties();
        // ClassLoader cl = PropertiesLoader.class.getClassLoader();
        // InputStream fin = cl.getResourceAsStream(inifilename);
        if (propFile.exists()) {
            FileInputStream fin = new FileInputStream(propFile);
            try {
                props.load(fin);
            } finally {
                fin.close();
            }
        }
        return props;
    }

    private synchronized void saveRuleProps(String ruleId, Properties props) throws DocException
    {
        if (props == null) return;  // should not occur

        File propsFile = getRuleFile(ruleId);
        if (DocmaConstants.DEBUG) {
            Log.info("Saving rule properties: " + propsFile);
        }
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(propsFile);
            props.store(fout, "Rule properties");
            fout.close();
        } catch (Exception ex) {
            Log.error("Could not save rule properties: " + propsFile);
            if (fout != null) try { fout.close(); } catch (Exception ex2) {}
            throw new DocException(ex);
        }
    }

}
