/*
 * HTMLRuleConfigImpl.java
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
package org.docma.plugin.implementation;

import org.docma.app.RuleConfig;
import org.docma.plugin.LogLevel;
import org.docma.plugin.rules.HTMLRuleConfig;

/**
 *
 * @author MP
 */
public class HTMLRuleConfigImpl implements HTMLRuleConfig
{
    private final RuleConfig ruleConf;

    public HTMLRuleConfigImpl(RuleConfig ruleConf)
    {
        this.ruleConf = ruleConf;
    }
        
    public String getRuleId() 
    {
        return ruleConf.getId();
    }

    public String[] getArguments() 
    {
        return ruleConf.getArgs();
    }

    public LogLevel getLogLevel(String checkId) 
    {
        return ruleConf.getLogLevel(checkId);
    }
    
}
