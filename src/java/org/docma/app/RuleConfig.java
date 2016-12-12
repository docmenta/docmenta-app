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

/**
 *
 * @author MP
 */
public class RuleConfig implements Comparable
{
    private String ruleId = "";

    public String getRuleId() 
    {
        return ruleId;
    }

    public void setRuleId(String ruleId) 
    {
        this.ruleId = ruleId;
    }

    public int compareTo(Object other) 
    {
        String otherId = ((RuleConfig) other).getRuleId();
        return ruleId.compareToIgnoreCase(otherId);
    }
    
}
