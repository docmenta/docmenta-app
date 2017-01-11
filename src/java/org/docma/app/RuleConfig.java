/*
 * RuleConfig.java
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.StringTokenizer;
import org.docma.coreapi.DocException;
import org.docma.plugin.LogLevel;
import org.docma.plugin.rules.HTMLRule;
import org.docma.util.DocmaUtil;
import org.docma.util.Log;

/**
 *
 * @author MP
 */
public class RuleConfig implements Comparable
{
    private static final String PROP_TITLE = "title";
    private static final String PROP_RULE_CLASS = "rule_class";
    private static final String PROP_RULE_ARGS = "rule_args";
    private static final String PROP_LOG_LEVEL = "log_level";
    private static final String PROP_RULE_ENABLED = "rule_enabled";
    private static final String PROP_DEFAULT_STATE = "default_state";
    private static final String PROP_EXECUTE_ON_CHECK = "execute_on_check";
    private static final String PROP_EXECUTE_ON_SAVE = "execute_on_save";
    private static final String PROP_CORRECT_ON_CHECK = "correct_on_check";
    private static final String PROP_CORRECT_ON_SAVE = "correct_on_save";
    private static final String PROP_SCOPE = "scope";

    private static final String SCOPE_ALL_VALUE = "[ALL]";

    private String ruleId = "";
    private Properties props = null;
    private Class ruleClass = null;
    private Object ruleInstance = null;
    private String[] args = null;
    private String[] scopeArr = null;

    public RuleConfig()
    {
        props = new Properties();
        setScopeAll();   // default scope for newly created rules
    }
    
    RuleConfig(String ruleId, Properties props)
    {
        this.ruleId = ruleId;
        this.props = props;
    }
    
    public String getId() 
    {
        return ruleId;
    }

    public void setId(String ruleId)
    {
        this.ruleId = ruleId;
    }

    public String getTitle() 
    {
        return props.getProperty(PROP_TITLE, "");
    }

    public void setTitle(String title) 
    {
        props.setProperty(PROP_TITLE, title);
    }

    public String getRuleClassName() 
    {
        return props.getProperty(PROP_RULE_CLASS, "");
    }

    public void setRuleClassName(String cls_name) 
    {
        props.setProperty(PROP_RULE_CLASS, cls_name);
        ruleClass = null;
        ruleInstance = null;
    }

    public Class getRuleClass() 
    {
        if (ruleClass == null) {
            initRuleInstance();
        }
        return ruleClass;
    }

    public Object getRuleInstance() 
    {
        if (ruleInstance == null) {
            initRuleInstance();
        }
        return ruleInstance;
    }

    public String getArgsLine() 
    {
        return props.getProperty(PROP_RULE_ARGS, "");
    }

    public void setArgsLine(String argsLine) throws DocException
    {
        try {
            args = translateCommandline(argsLine);
        } catch (Exception ex) {
            throw new DocException(ex);
        }
        props.setProperty(PROP_RULE_ARGS, argsLine);
    }

    public String[] getArgs() 
    {
        if (args == null) {
            try {
                args = translateCommandline(getArgsLine());
            } catch (Exception ex) {
                return new String[0];
            }
        }
        return args;
    }

    public String getShortInfo(String languageCode)
    {
        Object obj = getRuleInstance();
        if (obj instanceof HTMLRule) {
            return ((HTMLRule) obj).getShortInfo(languageCode);
        } else {
            return "";
        }
    }
    
    public String getLongInfo(String languageCode)
    {
        Object obj = getRuleInstance();
        if (obj instanceof HTMLRule) {
            return ((HTMLRule) obj).getLongInfo(languageCode);
        } else {
            return "";
        }
    }
    
    public String[] getCheckIds()
    {
        Object obj = getRuleInstance();
        if (obj instanceof HTMLRule) {
            return ((HTMLRule) obj).getCheckIds();
        } else {
            return null;
        }
    }
    
    public String getQualifiedCheckId(String checkId) 
    {
        if (checkId == null) {
            return null;
        } else {
            return getId() + "." + checkId;
        }
    }

    public boolean supportsAutoCorrection(String checkId)
    {
        Object obj = getRuleInstance();
        if (obj instanceof HTMLRule) {
            return ((HTMLRule) obj).supportsAutoCorrection(checkId);
        } else {
            return false;
        }
    }
    
    private LogLevel getDefaultLogLevel(String checkId) 
    {
        Object obj = getRuleInstance();
        if (obj instanceof HTMLRule) {
            return ((HTMLRule) obj).getDefaultLogLevel(checkId);
        } else {
            return null;
        }
    }
    
    public LogLevel getLogLevel(String checkId) 
    {
        String level_name = props.getProperty(PROP_LOG_LEVEL + "." + checkId, "").trim().toUpperCase();
        if (level_name.equals("")) {
            LogLevel defLevel = getDefaultLogLevel(checkId);
            if (defLevel != null) {
                return defLevel;
            } else {
                return LogLevel.ERROR;
            }
        }
        try {
            return Enum.valueOf(LogLevel.class, level_name);
        } catch (Exception ex) {
            Log.error("Log level with name '" + level_name + 
                      "' does not exist! Falling back to level 'ERROR'.");
            return LogLevel.ERROR;
        }
    }

    public void setLogLevel(String checkId, LogLevel lev) 
    {
        props.setProperty(PROP_LOG_LEVEL + "." + checkId, lev.name());
    }

    public boolean isRuleEnabled()
    {
        return props.getProperty(PROP_RULE_ENABLED, "true").trim().equalsIgnoreCase("true");
    }
    
    public void setRuleEnabled(boolean enabled)
    {
        props.setProperty(PROP_RULE_ENABLED, enabled ? "true" : "false");
    }

    public boolean isDefaultOn()
    {
        return props.getProperty(PROP_DEFAULT_STATE, "on").trim().equalsIgnoreCase("on");
    }
    
    public void setDefaultOn(boolean isOn)
    {
        props.setProperty(PROP_DEFAULT_STATE, isOn ? "on" : "off");
    }

    public boolean isExecuteOnSave(String checkId)
    {
        return props.getProperty(PROP_EXECUTE_ON_SAVE + "." + checkId, "true").trim().equalsIgnoreCase("true");
    }
    
    public void setExecuteOnSave(String checkId, boolean enabled)
    {
        props.setProperty(PROP_EXECUTE_ON_SAVE + "." + checkId, enabled ? "true" : "false");
    }
    
    public boolean isExecuteOnCheck(String checkId)
    {
        return props.getProperty(PROP_EXECUTE_ON_CHECK + "." + checkId, "true").trim().equalsIgnoreCase("true");
    }
    
    public void setExecuteOnCheck(String checkId, boolean enabled)
    {
        props.setProperty(PROP_EXECUTE_ON_CHECK + "." + checkId, enabled ? "true" : "false");
    }
    
    public boolean isCorrectOnCheck(String checkId)
    {
        return props.getProperty(PROP_CORRECT_ON_CHECK + "." + checkId, "true").trim().equalsIgnoreCase("true");
    }
    
    public void setCorrectOnCheck(String checkId, boolean enabled)
    {
        props.setProperty(PROP_CORRECT_ON_CHECK + "." + checkId, enabled ? "true" : "false");
    }
    
    public boolean isCorrectOnSave(String checkId)
    {
        return props.getProperty(PROP_CORRECT_ON_SAVE + "." + checkId, "true").trim().equalsIgnoreCase("true");
    }
    
    public void setCorrectOnSave(String checkId, boolean enabled)
    {
        props.setProperty(PROP_CORRECT_ON_SAVE + "." + checkId, enabled ? "true" : "false");
    }
    
    public boolean isScopeAll()
    {
        String s = props.getProperty(PROP_SCOPE, "").trim();
        return s.equals(SCOPE_ALL_VALUE);
    }
    
    public void setScopeAll()
    {
        props.setProperty(PROP_SCOPE, SCOPE_ALL_VALUE);
        scopeArr = null;
    }
    
    public String[] getScope()
    {
        if (scopeArr == null) {
            String s = props.getProperty(PROP_SCOPE, "").trim();
            if (s.equals("")) {
                scopeArr = new String[0];
            } else {
                scopeArr = s.split("[, ]");
            }
        }
        return scopeArr;
    }
    
    public void setScope(String[] storeIds)
    {
        props.setProperty(PROP_SCOPE, DocmaUtil.concatStrings(storeIds, ","));
        scopeArr = storeIds.clone();
    }
    
    public boolean isApplicableForStore(String storeId)
    {
        return isScopeAll() || Arrays.asList(getScope()).contains(storeId);
    }
    
    public int compareTo(Object other) 
    {
        String otherId = ((RuleConfig) other).getId();
        return ruleId.compareToIgnoreCase(otherId);
    }
    
    /* ------------ Package local methods ---------------- */
    
    Properties getProperties()
    {
        return props;
    }
    
    /* ------------ Private methods ---------------- */
    

    synchronized void initRuleInstance()
    {
        String cls_name = getRuleClassName();
        if (cls_name.equals("")) {
            ruleClass = null;
        } else {
            try {
                ruleClass = Class.forName(cls_name);
            } catch (Exception ex) {
                ruleClass = null;
            }
        }
        if (ruleClass == null) {
            ruleInstance = null;
        } else {
            try {
                ruleInstance = ruleClass.newInstance();
            } catch (Exception ex) {
                Log.error("Instantiation of rule class '" + cls_name + "' failed: " + ex.getMessage());
                ruleInstance = null;
            }
        }
    }

    /**
     * Crack a command line.
     *
     * @param toProcess
     *            the command line to process
     * @return the command line broken into strings. An empty or null toProcess
     *         parameter results in a zero sized array
     */
    private static String[] translateCommandline(final String toProcess) 
    {
        if (toProcess == null || toProcess.length() == 0) {
            // no command? no string
            return new String[0];
        }

        // parse with a simple finite state machine

        final int normal = 0;
        final int inQuote = 1;
        final int inDoubleQuote = 2;
        int state = normal;
        final StringTokenizer tok = new StringTokenizer(toProcess, "\"\' ", true);
        final ArrayList<String> list = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean lastTokenHasBeenQuoted = false;

        while (tok.hasMoreTokens()) {
            final String nextTok = tok.nextToken();
            switch (state) {
            case inQuote:
                if ("\'".equals(nextTok)) {
                    lastTokenHasBeenQuoted = true;
                    state = normal;
                } else {
                    current.append(nextTok);
                }
                break;
            case inDoubleQuote:
                if ("\"".equals(nextTok)) {
                    lastTokenHasBeenQuoted = true;
                    state = normal;
                } else {
                    current.append(nextTok);
                }
                break;
            default:
                if ("\'".equals(nextTok)) {
                    state = inQuote;
                } else if ("\"".equals(nextTok)) {
                    state = inDoubleQuote;
                } else if (" ".equals(nextTok)) {
                    if (lastTokenHasBeenQuoted || current.length() != 0) {
                        list.add(current.toString());
                        current = new StringBuilder();
                    }
                } else {
                    current.append(nextTok);
                }
                lastTokenHasBeenQuoted = false;
                break;
            }
        }

        if (lastTokenHasBeenQuoted || current.length() != 0) {
            list.add(current.toString());
        }

        if (state == inQuote || state == inDoubleQuote) {
            throw new IllegalArgumentException("Unbalanced quotes in "
                    + toProcess);
        }

        final String[] args = new String[list.size()];
        return list.toArray(args);
    }

    
}
