/*
 * CssUtils.java
 */
package org.docma.webdesigner;

/**
 *
 * @author manfred
 */
public class CssUtils 
{
    public static String extractPropertyValue(String css, String name)
    {
        int startpos = 0;
        while (startpos < css.length()) {
            int name_end = css.indexOf(':', startpos);
            if (name_end < 0) {
                return null;
            }
            String prop_nm = css.substring(startpos, name_end).trim();
            int val_start = name_end + 1;
            int val_end = css.indexOf(';', val_start);
            if (val_end < 0) {
                val_end = css.length();
            }
            startpos = val_end + 1;
            if (prop_nm.equalsIgnoreCase(name)) {
                return css.substring(val_start, val_end).trim();
            }
        }
        return null;
    }
}
