/*
 * AutoImagePreview.java
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
 *
 * @author MP
 */
public class AutoImagePreview implements AutoFormat
{
    private DocmaExportContext exportCtx;
    private boolean isTitleAfter;
    private String titlePattern;


    public void initialize(ExportContext ctx)
    {
        exportCtx = (DocmaExportContext) ctx;
        DocmaOutputConfig outConf = exportCtx.getOutputConfig();
        isTitleAfter = outConf.getTitlePlacement().equalsIgnoreCase("after");
        titlePattern = exportCtx.getGenTextProperty("title|figure");
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
        if (! ctx.getTagName().equals("img")) {
            exportCtx.logError("Cannot apply auto-format " + this.getClass().getName() +
                               " to element '" + ctx.getTagName() + "'.");
            out.write(ctx.getOuterString());  // do not change block
            return;
        }
        Map<String, String> atts = ctx.getTagAttributes();
        String title = atts.get("title");
        if (title == null) {
            exportCtx.logError("Cannot apply auto-format " + this.getClass().getName() +
                               ": element 'img' has no 'title' attribute.");
            out.write(ctx.getOuterString());  // do not change block
            return;
        }

        String cls_val = atts.get("class");
        String style_val = atts.get("style");
        boolean is_float = (style_val != null) && style_val.contains("float");
        if (is_float) {
            boolean is_left = style_val.contains("left");
            boolean is_right = is_left ? false : style_val.contains("right");
            String float_cls = is_left ? "float_left " : (is_right ? "float_right " : "");
            out.write("<div class=\"" + float_cls + "figure-float\" style=\"" + style_val.replace('"', ' ') + "\">");
        }
        if (! isTitleAfter) {
            writeCaption(out, title, cls_val);
            // out.write("<br />");
        }
        if (is_float) {
            writeOuterExcludeAttribute(ctx, out, atts, "style");  // Write image tag but exclude the style attribute
        } else {
            out.write(ctx.getOuterString());  // Write image tag unchanged
        }
        if (isTitleAfter) {
            // out.write("<br />");
            writeCaption(out, title, cls_val);
        }
        if (is_float) {
            out.write("</div>");
        }
    }

    private void writeCaption(Writer out, String title, String img_cls) throws Exception
    {
        String align_cls = null;
        boolean is_left = false;
        if (img_cls != null) {
            final String PATT_ALIGN = "align-";
            final String PATT_CENTER = "center";
            final String PATT_RIGHT = "right";
            final String PATT_LEFT = "left";
            int pos = img_cls.indexOf(PATT_ALIGN);
            if (pos >= 0) {
                int idx = pos + PATT_ALIGN.length();
                if (img_cls.regionMatches(idx, PATT_CENTER, 0, PATT_CENTER.length())) {
                    align_cls = "align-center";
                } else 
                if (img_cls.regionMatches(idx, PATT_RIGHT, 0, PATT_RIGHT.length())) {
                    align_cls = "align-right";
                } else 
                if (img_cls.regionMatches(idx, PATT_LEFT, 0, PATT_LEFT.length())) {
                    align_cls = "align-left";
                    is_left = true;
                }
            }
        }
        // Note: Do not place the title inside a p element, to avoid nested p elements.
        if ((align_cls == null) || is_left) {  // left-aligned image
            // The title has to be written as inline element (<span>), to keep
            // the indention of the enclosing <p> element.
            // The class title-preview is a inline-table.
            out.write("<br /><span class=\"title-preview caption\">");
            out.write(titlePattern.replace("%t", title));
            out.write("</span><br />");
        } else {  // centered or right-aligned image
            out.write("<div class=\"caption " + align_cls + "\">");
            out.write(titlePattern.replace("%t", title));
            out.write("</div>");
        }
    }

//    private void writeCaption(Writer out, String title, String img_cls) throws Exception
//    {
//        String align_cls = null;
//        if (img_cls != null) {
//            final String PATT_ALIGN = "align-";
//            final String PATT_CENTER = "center";
//            final String PATT_RIGHT = "right";
//            final String PATT_LEFT = "left";
//            int pos = img_cls.indexOf(PATT_ALIGN);
//            if (pos >= 0) {
//                int idx = pos + PATT_ALIGN.length();
//                if (img_cls.regionMatches(idx, PATT_CENTER, 0, PATT_CENTER.length())) {
//                    align_cls = "align-center";
//                } else 
//                if (img_cls.regionMatches(idx, PATT_RIGHT, 0, PATT_RIGHT.length())) {
//                    align_cls = "align-right";
//                } else 
//                if (img_cls.regionMatches(idx, PATT_LEFT, 0, PATT_LEFT.length())) {
//                    align_cls = "align-left";
//                }
//            }
//        }
//        if (align_cls == null) {
//            out.write("<p class=\"title\">");
//        } else {
//            out.write("<p class=\"title " + align_cls + "\">");
//        }
//        out.write(titlePattern.replace("%t", title));
//        out.write("</p>");
//    }
    
    private void writeOuterExcludeAttribute(TransformationContext ctx,
                                            Writer out,
                                            Map<String, String> atts, 
                                            String excludeAtt) throws IOException
    {
        out.write("<");
        out.write(ctx.getTagName());
        Iterator<String> it = atts.keySet().iterator();
        while (it.hasNext()) {
            String aname = it.next();
            if (! aname.equals(excludeAtt)) {
                out.write(" " + aname + "=\"" +  atts.get(aname).replace('"', ' ') + "\"");
            }
        }
        out.write("/>");
        // Note: It is assumed that inner string is empty (the img element cannot contain other elements)!
    }

    public String getShortInfo(String languageCode)
    {
        return "Add caption line to images with title attribute.";
    }

    public String getLongInfo(String languageCode)
    {
        return "";
    }

    private String getDefaultTitlePattern(String lang)
    {
        if (lang == null) lang = "en";
        else lang = lang.toLowerCase();

        String pattern;
        // Find pattern
        if (lang.equals("af")) pattern = "Figuur %n. %t";
        else if (lang.equals("am")) pattern = "\u121D\u1235\u120D\u00A0%n.\u00A0%t";
        else if (lang.equals("ar")) pattern = "\u0634\u0643\u0644\u00A0%n.\u00A0%t";
        else if (lang.equals("as")) pattern = "\u099B\u09AC\u09BF\u00A0%n.\u00A0%t";
        else if (lang.equals("ast")) pattern = "Figura\u00A0%n.\u00A0%t";
        else if (lang.equals("az")) pattern = "Fiqur\u00A0%n.\u00A0%t";
        else if (lang.equals("bg")) pattern = "\u0424\u0438\u0433\u0443\u0440\u0430\u00A0%n.\u00A0%t";
        else if (lang.equals("bn")) pattern = "\u099A\u09BF\u09A4\u09CD\u09B0\u00A0%n.\u00A0%t";
        else if (lang.equals("bs")) pattern = "Slika\u00A0%n.\u00A0%t";
        else if (lang.equals("ca")) pattern = "Figura %n. %t";
        else if (lang.equals("cs")) pattern = "Obr\u00E1zek %n. %t";
        else if (lang.equals("cy")) pattern = "Ffigur\u00A0%n.\u00A0%t";
        else if (lang.equals("da")) pattern = "Figur %n. %t";
        else if (lang.equals("de")) pattern = "Abbildung %n. %t";
        else if (lang.equals("el")) pattern = "\u03A3\u03C7\u03AE\u03BC\u03B1 %n. %t";
        else if (lang.equals("en")) pattern = "Figure\u00A0%n.\u00A0%t";
        else if (lang.equals("eo")) pattern = "Figuro\u00A0%n.\u00A0%t";
        else if (lang.equals("es")) pattern = "Figura %n. %t";
        else if (lang.equals("et")) pattern = "Joonis %n. %t";
        else if (lang.equals("eu")) pattern = "Irudia %n. %t";
        else if (lang.equals("fa")) pattern = "\n      \u0634\u0643\u0644\u00A0%n.\u00A0%t";
        else if (lang.equals("fi")) pattern = "Kuva %n. %t";
        else if (lang.equals("fr")) pattern = "Figure\u00A0%n.\u00A0%t";
        else if (lang.equals("ga")) pattern = "L\u00E9ar\u00E1id\u00A0%n.\u00A0%t";
        else if (lang.equals("gl")) pattern = "Figura\u00A0%n.\u00A0%t";
        else if (lang.equals("gu")) pattern = "\u0A86\u0A95\u0AC3\u0AA4\u0ABF\u00A0%n.\u00A0%t";
        else if (lang.equals("he")) pattern = "\u05D0\u05D9\u05D5\u05E8 %n. %t";
        else if (lang.equals("hi")) pattern = "\u091A\u093F\u0924\u094D\u0930\u00A0%n.\u00A0%t";
        else if (lang.equals("hr")) pattern = "Slika\u00A0%n.\u00A0%t";
        else if (lang.equals("hu")) pattern = "%n. \u00E1bra - %t";
        else if (lang.equals("id")) pattern = "Gambar %n. %t";
        else if (lang.equals("is")) pattern = "Sk\u00FDringamynd\u00A0%n.\u00A0%t";
        else if (lang.equals("it")) pattern = "Figura\u00A0%n.\u00A0%t";
        else if (lang.equals("ja")) pattern = "\u56F3%n %t";
        else if (lang.equals("ka")) pattern = "\u10E1\u10E3\u10E0\u10D0\u10D7\u10D8\u00A0%n.\u00A0%t";
        else if (lang.equals("kn")) pattern = "\u0C9A\u0CBF\u0CA4\u0CCD\u0CB0\u00A0%n.\u00A0%t";
        else if (lang.equals("ko")) pattern = "\uADF8\uB9BC %n. %t";
        else if (lang.equals("ky")) pattern = "\u0421\u04AF\u0440\u04E9\u0442 %n. %t";
        else if (lang.equals("la")) pattern = "Descriptio%n.%t";
        else if (lang.equals("lt")) pattern = "Pav.\u00A0%n.\u00A0%t";
        else if (lang.equals("lv")) pattern = "Ilustr\u0101cija\u00A0%n.\u00A0%t";
        else if (lang.equals("ml")) pattern = "\u0D1A\u0D3F\u0D24\u0D4D\u0D30\u0D02\u00A0%n.\u00A0%t";
        else if (lang.equals("mn")) pattern = "\u0417\u0443\u0440\u0430\u0433 %n. %t";
        else if (lang.equals("mr")) pattern = "\u0906\u0915\u0943\u0924\u0940\u00A0%n.\u00A0%t";
        else if (lang.equals("nb")) pattern = "Figur %n. %t";
        else if (lang.equals("nds")) pattern = "Avbillen\u00A0%n.\u00A0%t";
        else if (lang.equals("nl")) pattern = "Afbeelding %n. %t";
        else if (lang.equals("nn")) pattern = "Figur %n. %t";
        else if (lang.equals("or")) pattern = "\u0B1A\u0B3F\u0B24\u0B4D\u0B30\u00A0%n.\u00A0%t";
        else if (lang.equals("pa")) pattern = "\u0A1A\u0A3F\u0A71\u0A24\u0A30\u00A0%n.\u00A0%t";
        else if (lang.equals("pl")) pattern = "Rysunek %n. %t";
        else if (lang.equals("pt")) pattern = "Figura %n. %t";
        else if (lang.equals("pt_br")) pattern = "Figura %n. %t";
        else if (lang.equals("ro")) pattern = "Fig. %n. %t";
        else if (lang.equals("ru")) pattern = "\u0420\u0438\u0441\u0443\u043D\u043E\u043A %n. %t";
        else if (lang.equals("sk")) pattern = "Obr\u00E1zok %n. %t";
        else if (lang.equals("sl")) pattern = "Slika %n. %t";
        else if (lang.equals("sq")) pattern = "Figura\u00A0%n.\u00A0%t";
        else if (lang.equals("sr")) pattern = "\u0421\u043B\u0438\u043A\u0430\u00A0%n.\u00A0%t";
        else if (lang.equals("sr_latn")) pattern = "Slika\u00A0%n.\u00A0%t";
        else if (lang.equals("sv")) pattern = "Figur %n. %t";
        else if (lang.equals("ta")) pattern = "\u0BAA\u0B9F\u0BAE\u0BCD\u00A0%n.\u00A0%t";
        else if (lang.equals("te")) pattern = "\u0C2E\u0C42\u0C30\u0C4D\u0C24\u0C3F\u00A0%n.\u00A0%t";
        else if (lang.equals("th")) pattern = "\u0E23\u0E39\u0E1B %n. %t";
        else if (lang.equals("tl")) pattern = "Pigyur\u00A0%n.\u00A0%t";
        else if (lang.equals("tr")) pattern = "\u015Eekil %n. %t";
        else if (lang.equals("uk")) pattern = "\u0420\u0438\u0441\u0443\u043D\u043E\u043A %n. %t";
        else if (lang.equals("vi")) pattern = "H\u00ECnh\u00A0%n.\u00A0%t";
        else if (lang.equals("xh")) pattern = "Ulungu %n. %t";
        else if (lang.equals("zh")) pattern = "\u56FE\u00A0%n.\u00A0%t";
        else if (lang.equals("zh_cn")) pattern = "\u56FE\u00A0%n.\u00A0%t";
        else if (lang.equals("zh_tw")) pattern = "\u5716\u5F62 %n. %t";
        else pattern = "%t";

        return pattern;
    }

}
