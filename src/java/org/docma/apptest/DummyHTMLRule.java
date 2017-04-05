/*
 * DummyHTMLRule.java
 */
package org.docma.apptest;

import org.docma.plugin.LogEntries;
import org.docma.plugin.LogLevel;
import org.docma.plugin.rules.*;

/**
 *
 * @author MP
 */
public class DummyHTMLRule implements HTMLRule
{

    public String getShortInfo(String languageCode) 
    {
        return "This is a dummy HTML rule.";
    }

    public String getLongInfo(String languageCode) 
    {
        return "This is the online help for the dummy HTML rule.";
    }

    public void configure(HTMLRuleConfig conf) 
    {
    }

    public void startBatch(HTMLRuleContext context) 
    {
    }

    public void finishBatch(HTMLRuleContext context) 
    {
    }
    
    public String apply(String content, HTMLRuleContext context) 
    {
        if (context.isEnabled("checkLength")) {
            if (content.length() > 40) {
                context.log("checkLength", "The content exceeds maximum length of 40 characters: " + content.length());
                if (context.isAutoCorrect("checkLength")) {
                    context.log("checkLength", "Content has been stripped to 40 characters.");
                    return content.substring(0, 40);
                }
            }
        }
        return null;  // content is unchanged
    }

    public String[] getCheckIds() 
    {
        return new String[] { "checkLength", "dummyCheck2" };
    }

    public String getCheckTitle(String checkId, String languageCode) 
    {
        if (checkId.equals("checkLength")) {
            return "Checks the length of the string.";
        } else if (checkId.equals("dummyCheck2")) {
            return "The second dummy check.";
        }
        return null;
    }

    public boolean supportsAutoCorrection(String checkId) 
    {
        if (checkId.equals("checkLength")) {
            return true;
        } else if (checkId.equals("dummyCheck2")) {
            return false;
        }
        return false;
    }

    public LogLevel getDefaultLogLevel(String checkId) 
    {
        if (checkId.equals("checkLength")) {
            return LogLevel.ERROR;
        } else if (checkId.equals("dummyCheck2")) {
            return LogLevel.WARNING;
        }
        return LogLevel.ERROR;
    }

}
