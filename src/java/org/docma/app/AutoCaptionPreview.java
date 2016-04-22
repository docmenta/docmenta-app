/*
 * AutoCaptionPreview.java
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

import org.docma.plugin.*;
import java.util.*;
import java.io.*;

/**
 * Autoformat transformation for preview of table captions.
 * Inserts the label-prefix in preview-mode.
 * 
 * Note: Currently this class is not used during autoformat transformations,
 * because the label-prefix for table captions is inserted by using the CSS  
 * rule <code>caption:before</code>. See DocmaAppUtil.writeContentCSS().
 * 
 * @author MP
 */
public class AutoCaptionPreview implements AutoFormat
{
    private DocmaExportContext exportCtx;
    // private boolean isTitleAfter;
    private String titlePattern;


    public void initialize(ExportContext ctx)
    {
        exportCtx = (DocmaExportContext) ctx;
        // DocmaOutputConfig outConf = exportCtx.getOutputConfig();
        // isTitleAfter = outConf.getTitlePlacement().equalsIgnoreCase("after");
        titlePattern = exportCtx.getGenTextProperty("title|table");
        if ((titlePattern == null) || titlePattern.trim().equals("")) {
            titlePattern = getDefaultTitlePattern(ctx.getExportLanguage());
        }
    }

    public void finished()
    {
        exportCtx = null;
    }

    public void transform(TransformationContext ctx) throws Exception
    {
        Writer out = ctx.getWriter();
        if (! ctx.getTagName().equals("caption")) {
            exportCtx.logError("Cannot apply auto-format " + this.getClass().getName() +
                               " to element '" + ctx.getTagName() + "'.");
            out.write(ctx.getOuterString());  // do not change block
            return;
        }
        Map<String, String> atts = ctx.getTagAttributes();

        out.write("<caption");
        if (! atts.isEmpty()) {
            Iterator<String> it = atts.keySet().iterator();
            while (it.hasNext()) {
                String aname = it.next();
                out.write(" " + aname + "=\"" +  atts.get(aname).replace('"', ' ') + "\"");
            }
        }
        out.write(">");
        out.write(titlePattern.replace("%t", ctx.getInnerString()));
        out.write("</caption>");
    }

    public String getShortInfo(String languageCode)
    {
        return "Add caption line to images with title attribute.";
    }

    public String getLongInfo(String languageCode)
    {
        return "";
    }

    static String getDefaultTitlePattern(String lang)
    {
        if (lang == null) lang = "en";
        else lang = lang.toLowerCase();

        String pattern;
        // Find pattern
        if (lang.equals("af")) pattern = "Tabel %n. %t";
        else if (lang.equals("am")) pattern = "\u1220\u1295\u1320\u1228\u12E5\u00A0%n.\u00A0%t";
        else if (lang.equals("ar")) pattern = "\u062C\u062F\u0648\u0644\u00A0%n.\u00A0%t";
        else if (lang.equals("as")) pattern = "\u09A4\u09BE\u09B2\u09BF\u0995\u09BE\u00A0%n.\u00A0%t";
        else if (lang.equals("ast")) pattern = "Tabla\u00A0%n.\u00A0%t";
        else if (lang.equals("az")) pattern = "C\u0259dv\u0259l\u00A0%n.\u00A0%t";
        else if (lang.equals("bg")) pattern = "\u0422\u0430\u0431\u043B\u0438\u0446\u0430\u00A0%n.\u00A0%t";
        else if (lang.equals("bn")) pattern = "\u099B\u0995\u00A0%n.\u00A0%t";
        else if (lang.equals("bs")) pattern = "Tabela\u00A0%n.\u00A0%t";
        else if (lang.equals("ca")) pattern = "Taula %n. %t";
        else if (lang.equals("cs")) pattern = "Tabulka %n. %t";
        else if (lang.equals("cy")) pattern = "Tabl\u00A0%n.\u00A0%t";
        else if (lang.equals("da")) pattern = "Tabel %n. %t";
        else if (lang.equals("de")) pattern = "Tabelle %n. %t";
        else if (lang.equals("el")) pattern = "\u03A0\u03AF\u03BD\u03B1\u03BA\u03B1\u03C2 %n. %t";
        else if (lang.equals("en")) pattern = "Table\u00A0%n.\u00A0%t";
        else if (lang.equals("eo")) pattern = "Tabelo\u00A0%n.\u00A0%t";
        else if (lang.equals("es")) pattern = "Tabla %n. %t";
        else if (lang.equals("et")) pattern = "Tabel %n. %t";
        else if (lang.equals("eu")) pattern = "Taula %n. %t";
        else if (lang.equals("fa")) pattern = "\n      \u062C\u062F\u0648\u0644\u00A0%n.\u00A0%t";
        else if (lang.equals("fi")) pattern = "Taulu %n. %t";
        else if (lang.equals("fr")) pattern = "Tableau\u00A0%n.\u00A0%t";
        else if (lang.equals("ga")) pattern = "T\u00E1bla\u00A0%n.\u00A0%t";
        else if (lang.equals("gl")) pattern = "T\u00E1boaT\u00E1boa\u00A0%n.\u00A0%t";
        else if (lang.equals("gu")) pattern = "\u0A95\u0ACB\u0AB7\u0ACD\u0A9F\u0A95\u00A0%n.\u00A0%t";
        else if (lang.equals("he")) pattern = "\u05D8\u05D1\u05DC\u05D4 %n. %t";
        else if (lang.equals("hi")) pattern = "\u0924\u093E\u0932\u093F\u0915\u093E\u00A0%n.\u00A0%t";
        else if (lang.equals("hr")) pattern = "Tablica\u00A0%n.\u00A0%t";
        else if (lang.equals("hu")) pattern = "%n. t\u00E1bl\u00E1zat - %t";
        else if (lang.equals("id")) pattern = "Tabel %n. %t";
        else if (lang.equals("is")) pattern = "Tafla\u00A0%n.\u00A0%t";
        else if (lang.equals("it")) pattern = "Tabella\u00A0%n.\u00A0%t";
        else if (lang.equals("ja")) pattern = "\u8868%n %t";
        else if (lang.equals("ka")) pattern = "\u10EA\u10EE\u10E0\u10D8\u10DA\u10D8\u00A0%n.\u00A0%t";
        else if (lang.equals("kn")) pattern = "\u0C95\u0CCB\u0CB7\u0CCD\u0C9F\u0C95\u00A0%n.\u00A0%t";
        else if (lang.equals("ko")) pattern = "\uD45C %n. %t";
        else if (lang.equals("ky")) pattern = "\u0422\u0430\u0431\u043B\u0438\u0446\u0430 %n. %t";
        else if (lang.equals("la")) pattern = "Tabula%n.%t";
        else if (lang.equals("lt")) pattern = "Lentel\u0117\u00A0%n.\u00A0%t";
        else if (lang.equals("lv")) pattern = "Tabula\u00A0%n.\u00A0%t";
        else if (lang.equals("ml")) pattern = "\u0D2A\u0D1F\u0D4D\u0D1F\u0D3F\u0D15\u00A0%n.\u00A0%t";
        else if (lang.equals("mn")) pattern = "\u0425\u04AF\u0441\u043D\u044D\u0433\u0442 %n. %t";
        else if (lang.equals("mr")) pattern = "\u0924\u0915\u094D\u0924\u093E\u00A0%n.\u00A0%t";
        else if (lang.equals("nb")) pattern = "Tabell %n. %t";
        else if (lang.equals("nds")) pattern = "Tabell\u00A0%n.\u00A0%t";
        else if (lang.equals("nl")) pattern = "Tabel %n. %t";
        else if (lang.equals("nn")) pattern = "Tabell %n. %t";
        else if (lang.equals("or")) pattern = "\u0B38\u0B3E\u0B30\u0B23\u0B40\u00A0%n.\u00A0%t";
        else if (lang.equals("pa")) pattern = "\u0A38\u0A3E\u0A30\u0A23\u0A40\u00A0%n.\u00A0%t";
        else if (lang.equals("pl")) pattern = "Tabela %n. %t";
        else if (lang.equals("pt")) pattern = "Tabela %n. %t";
        else if (lang.equals("pt_br")) pattern = "Tabela %n. %t";
        else if (lang.equals("ro")) pattern = "Tabel %n. %t";
        else if (lang.equals("ru")) pattern = "\u0422\u0430\u0431\u043B\u0438\u0446\u0430 %n. %t";
        else if (lang.equals("sk")) pattern = "Tabu\u013Eka %n. %t";
        else if (lang.equals("sl")) pattern = "Tabela %n. %t";
        else if (lang.equals("sq")) pattern = "Tabela\u00A0%n.\u00A0%t";
        else if (lang.equals("sr")) pattern = "\u0422\u0430\u0431\u0435\u043B\u0430\u00A0%n.\u00A0%t";
        else if (lang.equals("sr_latn")) pattern = "Tabela\u00A0%n.\u00A0%t";
        else if (lang.equals("sv")) pattern = "Tabell %n. %t";
        else if (lang.equals("ta")) pattern = "\u0B85\u0B9F\u0BCD\u0B9F\u0BB5\u0BA3\u0BC8\u00A0%n.\u00A0%t";
        else if (lang.equals("te")) pattern = "\u0C2A\u0C1F\u0C4D\u0C1F\u0C3F\u0C15\u00A0%n.\u00A0%t";
        else if (lang.equals("th")) pattern = "\u0E15\u0E32\u0E23\u0E32\u0E07 %n. %t";
        else if (lang.equals("tl")) pattern = "Talaan\u00A0%n.\u00A0%t";
        else if (lang.equals("tr")) pattern = "Tablo %n. %t";
        else if (lang.equals("uk")) pattern = "\u0422\u0430\u0431\u043B\u0438\u0446\u044F %n. %t";
        else if (lang.equals("vi")) pattern = "B\u1EA3ng\u00A0%n.\u00A0%t";
        else if (lang.equals("xh")) pattern = "Indlela Yokwenza Imigca %n. %t";
        else if (lang.equals("zh")) pattern = "\u8868\u00A0%n.\u00A0%t";
        else if (lang.equals("zh_cn")) pattern = "\u8868\u00A0%n.\u00A0%t";
        else if (lang.equals("zh_tw")) pattern = "\u8868\u683C %n. %t";
        else pattern = "%t";

        return pattern;
    }

    
}
