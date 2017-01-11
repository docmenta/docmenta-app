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
    
    private final File rulesDir;
    private final SortedMap<String, RuleConfig> rules = new TreeMap();

    public RulesManager(File dir) 
    {
        this.rulesDir = dir;
        if (! rulesDir.exists()) {
            rulesDir.mkdirs();
        }
        readRulesFromDir();
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
                (rc.getRuleInstance() != null)) {
                res.add(rc);
            }
        }
        return res.toArray(new RuleConfig[res.size()]);
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
                    addRuleToList(new RuleConfig(ruleId, props));
                } catch (Exception ex) {
                    Log.error("Could not load rule properties " + f.getName() + ": " + ex.getMessage());
                }
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
