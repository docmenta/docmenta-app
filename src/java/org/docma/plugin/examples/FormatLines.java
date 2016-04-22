/*
 * FormatLines.java
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

package org.docma.plugin.examples;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import org.docma.util.*;
import org.docma.plugin.*;

/**
 *
 * @author MP
 */
public class FormatLines implements AutoFormat
{
    private static final Pattern KEEP_PATTERN = Pattern.compile("keep-together\\s*:\\s*(\\w+)");
    private static final Pattern MARK_ID_PATTERN = Pattern.compile("[A-Za-z_][0-9A-Za-z_-]{0,99}");
    private static final String[] INVALID_NESTED = 
      { "div", "p", "blockquote", "ol", "ul", "li", 
        "table", "tr", "td", "th", "tbody", "thead", "tfoot" };

    private static final String ARG_START = "start";
    private static final String ARG_LINE = "line";
    private static final String ARG_ODD = "odd";
    private static final String ARG_EVEN = "even";
    private static final String ARG_NUM = "num";
    private static final String ARG_ONUM = "onum";
    private static final String ARG_ENUM = "enum";
    private static final String ARG_CBOX = "cbox";
    private static final String ARG_NBOX = "nbox";
    private static final String ARG_BOX = "box";
    private static final String ARG_KEEP = "keep";
    private static final String ARG_NW = "nw";
    private static final String ARG_MAXCOLS = "maxcols";
    private static final String ARG_LBC = "lbc";
    private static final String ARG_PRE = "pre";
    private static final String ARG_TAB = "tab";
    private static final String ARG_M = "m";
    private static final String ARG_MSTYLE = "mstyle";
    private static final String ARG_MNUM = "mnum";

    private ExportContext exportCtx;
    private HashSet invalidNestedElements;


    public String getShortInfo(String languageCode)
    {
        return PluginUtil.getResourceString(this.getClass(), languageCode, "shortInfo");
    }

    public String getLongInfo(String languageCode)
    {
        return PluginUtil.getResourceString(this.getClass(), languageCode, "longInfo");
    }

    public void initialize(ExportContext ctx)
    {
        exportCtx = ctx;
        invalidNestedElements = new HashSet();
        invalidNestedElements.addAll(Arrays.asList(INVALID_NESTED));
    }

    public void finished()
    {
        exportCtx = null;
    }

    public void transform(TransformationContext ctx) throws Exception
    {
        Writer out = ctx.getWriter();
        ctx.setStyleRecursion(false);
        if (ctx.getArgumentCount() == 0) {
            out.write(ctx.getOuterString());  // do nothing; return original content
            return;
        }

        // Arguments:
        // start=[<INTEGER>|off]     default = off
        // line=STYLE_ID             default = no style
        // odd=STYLE_ID              default = line style
        // even=STYLE_ID             default = line style
        // num=STYLE_ID              default = line style
        // onum=STYLE_ID             default = odd style
        // enum=STYLE_ID             default = even style
        // cbox=STYLE_ID             default = keep original div style
        // nbox=STYLE_ID             default = no style
        // box=STYLE_ID              default = no style
        // keep=[auto|always]        default = always
        // nw=<SIZE>                 default = 32pt
        // maxcols=<INTEGER>         default = 0 (unlimited)
        // lbc=<CHAR-ENTITY>         default = "" (no character)
        // pre=[true|false]          default = false
        // tab=<INTEGER>             default = 0 (do not replace)
        // m=PATTERN                 default = ""
        // mstyle=STYLE_ID           default = no style
        // mnum=STYLE_ID             default = no style
        Map<String, String> args = new HashMap<String, String>(43);
        for (int i=0; i < ctx.getArgumentCount(); i++) {
            readArg(ctx.getArgument(i, ""), args);
        }

        String startStr = args.get(ARG_START);
        boolean numberingEnabled = (startStr != null) && !startStr.equalsIgnoreCase("off");
        int startNum = numberingEnabled ? Integer.parseInt(startStr) : 1;  // if no numbering, start with 1!

        if (! args.containsKey(ARG_M)) args.put(ARG_M, "");
        if (! args.containsKey(ARG_MAXCOLS)) args.put(ARG_MAXCOLS, "0");
        if (! args.containsKey(ARG_LBC)) args.put(ARG_LBC, "");
        if (! args.containsKey(ARG_PRE)) args.put(ARG_PRE, "false");
        if (! args.containsKey(ARG_TAB)) args.put(ARG_TAB, "0");
        boolean pre = args.get(ARG_PRE).equalsIgnoreCase("true");
        int maxCols = Integer.parseInt(args.get(ARG_MAXCOLS));
        int tabsize = Integer.parseInt(args.get(ARG_TAB));

        Map<String, String> tagAttribs = ctx.getTagAttributes();

        // Check if keep-together was set as style value. If yes, this value takes precedence
        String styleVal = tagAttribs.get("style");
        if (styleVal != null) {
            Matcher keepMatcher = KEEP_PATTERN.matcher(styleVal);
            if (keepMatcher.find()) {  // keep-together value exists
                String keepValue = keepMatcher.group(1);
                args.put(ARG_KEEP, keepValue);
            }
        }

        ArrayList<String> lines = new ArrayList<String>(200);
        ArrayList<String> nums = null;
        if (numberingEnabled) nums = new ArrayList<String>(200);
        ArrayList<String> attNames = new ArrayList<String>();
        ArrayList<String> attValues = new ArrayList<String>();
        ArrayList<Integer> autoBreakPositions = new ArrayList<Integer>();
        ArrayList<String> openInlineTags = new ArrayList<String>();
        ArrayList<String> openInlineTagNames = new ArrayList<String>();
        String input = ctx.getInnerString();
        XMLParser parser = new XMLParser(input, false, false, false);
        boolean checkFirstLine = true;   // check if first line contains arguments
        int count_chars = 0;
        int copy_pos = 0;
        StringBuilder lineBuf = new StringBuilder(250);
        int eventType;
        do {
            eventType = parser.next();
            if (eventType != XMLParser.FINISHED) {  // opening/closing tag, comment, PI, CDATA
                int linecount_before = lines.size();
                int tag_start = parser.getStartOffset();
                // Copy all characters up to the '<' of the opening/closing tag, comment, PI, CDATA
                count_chars =
                    processInlineChars(input, copy_pos, tag_start, count_chars,
                                       lineBuf, pre, tabsize, maxCols, startNum, args,
                                       autoBreakPositions, lines, nums,
                                       openInlineTagNames, openInlineTags);
                startNum += lines.size() - linecount_before;
                copy_pos = tag_start; // continue at start position of the tag/comment/PI/CDATA
            }
            if (eventType == XMLParser.START_ELEMENT) {
                String elemName = parser.getElementName();

                // Skip nested block tags
                if (! invalidNestedElements.contains(elemName.toLowerCase())) {
                    if (elemName.equalsIgnoreCase("br")) {
                        // Start new line
                        addLine(lines, nums, lineBuf, startNum, args, autoBreakPositions,
                                openInlineTagNames, openInlineTags);
                        ++startNum;
                        count_chars = 0;
                    } else {
                        // Opening tag of an inline element
                        // Re-write tag to avoid line-break characters between attributes!
                        parser.getAttributes(attNames, attValues);
                        // Add line-number as title-attribute
                        int id_pos = attNames.indexOf("id");
                        if (id_pos >= 0) {  // has id
                            String elem_title = String.valueOf(startNum);
                            int tpos = attNames.indexOf("title");
                            if (tpos >= 0) {
                                if (attValues.get(tpos).equals("")) {
                                    attValues.set(tpos, elem_title);
                                }
                            } else {
                                attNames.add("title");
                                attValues.add(elem_title);
                            }
                        }
                        // Write tag
                        int itag_start = lineBuf.length();
                        boolean isEmptyElem = parser.isEmptyElement();
                        writeTag(lineBuf, elemName, attNames, attValues, isEmptyElem);
                        if (! isEmptyElem) {
                            String inline_tag;
                            if (id_pos >= 0) {
                                attNames.remove(id_pos);
                                attValues.remove(id_pos);
                                inline_tag = writeTag(null, elemName, attNames, attValues, isEmptyElem);
                            } else {
                                inline_tag = lineBuf.substring(itag_start);
                            }
                            openInlineTags.add(inline_tag);
                            openInlineTagNames.add(elemName);
                        }
                    }
                }
                copy_pos = parser.getEndOffset(); // continue after the opening tag
            } else
            if (eventType == XMLParser.END_ELEMENT) {
                String elemName = parser.getElementName();

                // Skip nested block tags
                if (! invalidNestedElements.contains(elemName.toLowerCase())) {
                    // Closing tag of an inline element. Re-write closing tag to assure
                    // there are no line-break characters within the closing tag!
                    lineBuf.append("</").append(elemName).append(">");
                    int open_pos = openInlineTagNames.lastIndexOf(elemName);
                    if (open_pos >= 0) {
                        openInlineTagNames.remove(open_pos);
                        openInlineTags.remove(open_pos);
                    } else {
                        exportCtx.logWarning("Closing tag has no matching opening tag: " + elemName);
                    }
                }
                copy_pos = parser.getEndOffset(); // continue after the closing tag
            } else
            if (eventType != XMLParser.FINISHED) {  // comment, PI, CDATA
                // Just copy any comments, processing instructions, CDATA without modification
                int end_offset = parser.getEndOffset();
                lineBuf.append(input, copy_pos, end_offset);
                copy_pos = end_offset; // continue after the comment/PI/CDATA
            }

            // Read arguments from first line if first line matches arguments pattern
            if (checkFirstLine && lines.size() > 0) {
                checkFirstLine = false;
                if (readArgsFromLine(lines.get(0), args)) {
                    // do not display arguments line
                    lines.remove(0);
                    if (nums != null) nums.remove(0);
                    // re-read start value
                    startStr = args.get(ARG_START);
                    numberingEnabled = (startStr != null) && !startStr.equalsIgnoreCase("off");
                    if (numberingEnabled) {
                        if (nums == null) nums = new ArrayList<String>(200);
                    } else {
                        nums = null;
                    }
                    try {
                        startNum = numberingEnabled ? Integer.parseInt(startStr) : 1;  // if no numbering, start with 1!
                    } catch (Exception ex) {
                        exportCtx.logError("AutoFormat FormatLines: argument '" +
                                           ARG_START + "' has invalid value: " + startStr);
                    }
                    // re-read preformatted
                    pre = args.get(ARG_PRE).equalsIgnoreCase("true");
                    // re-read maxcols
                    maxCols = Integer.parseInt(args.get(ARG_MAXCOLS));
                    // re-read tabsize
                    tabsize = Integer.parseInt(args.get(ARG_TAB));
                }
            }
        } while (eventType != XMLParser.FINISHED);

        // Copy remaining characters
        if (copy_pos < input.length()) {
            count_chars =
                processInlineChars(input, copy_pos, input.length(), count_chars,
                                   lineBuf, pre, tabsize, maxCols, startNum, args,
                                   autoBreakPositions, lines, nums,
                                   openInlineTagNames, openInlineTags);
        }
        if (lineBuf.length() > 0) {
            addLine(lines, nums, lineBuf, startNum, args, autoBreakPositions,
                    openInlineTagNames, openInlineTags);
            ++startNum;
            // count_chars = 0;
        }

        // Write outer box that contains lines and optionally numbers
        String cboxStyle = args.get(ARG_CBOX);
        String nboxStyle = args.get(ARG_NBOX);
        String boxStyle = args.get(ARG_BOX);
        if (cboxStyle == null) {
            cboxStyle = tagAttribs.get("class");
        }
        String cboxCls = (cboxStyle == null) ? "" : " class=\"" + cboxStyle + "\"";
        String keepValue = args.get(ARG_KEEP);
        if ((keepValue == null) || keepValue.equals("")) {
            keepValue = "always";  // default
        }
        boolean isKeepAuto = keepValue.equalsIgnoreCase("auto");
        if (numberingEnabled) {
            String outerCls = (boxStyle == null) ? " border=\"0\"" : " class=\"" + boxStyle + "\"";
            String nboxCls = (nboxStyle == null) ? "" : " class=\"" + nboxStyle + "\"";
            String keepStyle = isKeepAuto ? " style=\"keep-together:auto;\"" : "";
            String numWidth = args.get(ARG_NW);
            if (numWidth == null) numWidth = "32pt";
            out.append("<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\"")
               .append(outerCls).append(keepStyle).append(">")
               .append("<tr><td valign=\"top\"").append(nboxCls)
               .append(" style=\"width:").append(numWidth).append(";\">");
            for (int i=0; i < nums.size(); i++) {
                out.append(nums.get(i));
            }
            out.append("</td><td valign=\"top\"").append(cboxCls).append(">");
        } else {
            out.append("<div").append(cboxCls)
                              .append(" style=\"keep-together:").append(keepValue).append(";\">");
        }
        for (int i=0; i < lines.size(); i++) {
            out.append(lines.get(i));
        }
        if (numberingEnabled) {
            out.append("</td></tr></table>");
        } else {
            out.append("</div>");
        }
    }

    private int processInlineChars(String input,
                                   int start_pos,
                                   int end_pos,
                                   int count_chars,
                                   StringBuilder lineBuf,
                                   boolean pre,
                                   int tabsize,
                                   int maxCols,
                                   int startNum,
                                   Map<String, String> args,
                                   List<Integer> autoBreakPositions,
                                   List<String> lines,
                                   List<String> nums,
                                   List<String> openedTagNames, 
                                   List<String> openedTags) throws Exception
    {
        // if (! (pre || maxCols > 0)) {
        //     // No line breaks have to be inserted -> just copy chars to lineBuf
        //     lineBuf.append(input, start_pos, end_pos);
        //     count_chars += end_pos - start_pos;
        //     return count_chars;
        // }

        // Parse line and insert line breaks if pre or maxCols > 0
        // Furthermore replace tab by spaces if tabsize > 0
        String lineBreakChar = (maxCols > 0) ? args.get(ARG_LBC) : "";
        int max = (lineBreakChar.length() == 0) ? maxCols : (maxCols - 1);
        int pos = start_pos;
        while (pos < end_pos) { // read from last end of tag to beginning of next tag
            char ch = input.charAt(pos);
            int nextpos = pos + 1;
            if (pre && ((ch == '\n') || (ch == '\r') || (ch == '\u0085') ||
                        (ch == '\u2028') || (ch == '\u2029'))) {
                if ((ch == '\r') && (nextpos < end_pos) && (input.charAt(nextpos) == '\n')) {
                    nextpos++;   // found a two character line terminator: \r\n
                }
                addLine(lines, nums, lineBuf, startNum, args, autoBreakPositions,
                        openedTagNames, openedTags);
                count_chars = 0;
                pos = nextpos;
                continue;
            }

            // Add automatic line break if character count exceeds maxCols
            if ((maxCols > 0) && (count_chars >= max)) {  // automatic line break
                lineBuf.append(lineBreakChar);
                autoBreakPositions.add(lineBuf.length());
                lineBuf.append("<br />");
                count_chars = 0;
            }

            // Add next inline character to lineBuf
            if ((ch == '\t') && (tabsize > 0)) {  // tab
                for (int i=0; i < tabsize; i++) {
                    lineBuf.append(" ");
                }
                count_chars += tabsize;
            } else
            if (ch == '&') {
                int entity_end = input.indexOf(';', pos + 1);
                if ((entity_end > 0) && (entity_end < end_pos)) {
                    nextpos = entity_end + 1;
                }
                lineBuf.append(input, pos, nextpos);
                ++count_chars;
            } else {
                lineBuf.append(ch);
                ++count_chars;
            }

            pos = nextpos;
        }
        return count_chars;
    }

    private String writeTag(StringBuilder buf,
                            String elemName,
                            List<String> attNames,
                            List<String> attValues,
                            boolean isEmpty)
    {
        boolean nobuf = (buf == null);
        if (nobuf) {
            buf = new StringBuilder();
        }
        buf.append("<").append(elemName);
        for (int i=0; i < attNames.size(); i++) {
            buf.append(" ").append(attNames.get(i)).append("=\"")
               .append(attValues.get(i).replace("\"", "&#34;")).append("\"");
        }
        if (isEmpty) buf.append("/>");
        else buf.append(">");
        return nobuf ? buf.toString() : null;
    }

    private void closeOpenedInlineTags(StringBuilder buf, List<String> tagNames)
    {
        int last_idx = tagNames.size() - 1;
        for (int i = last_idx; i >= 0; i--) {
            buf.append("</").append(tagNames.get(i)).append(">");
        }
    }

    private void openOpenedInlineTags(StringBuilder buf, List<String> openedTags)
    {
        for (int i=0; i < openedTags.size(); i++) {
            buf.append(openedTags.get(i));
        }
    }

    private boolean readArgsFromLine(String line, Map<String, String> args)
    {
        if (line.startsWith("<div ")) {
            line = line.substring(line.indexOf('>') + 1);
        }
        line = line.replace("&#160;", " ").replace("&nbsp;", " ");
        line = line.trim();
        final String ARGS_PATTERN = "[args:";
        if (line.regionMatches(true, 0, ARGS_PATTERN, 0, ARGS_PATTERN.length())) {
            int endPos = line.lastIndexOf(']');
            if (endPos > 0) {  // line is a valid arguments line
                // extract arguments from line
                String argline = line.substring(ARGS_PATTERN.length(), endPos).trim();
                // To do: decode entities
                String[] arr = argline.split("\\s+");
                for (int i=0; i < arr.length; i++) readArg(arr[i], args);
                return true;
            }
        }
        return false;
    }


    private String getMarkerId(String line, int marker_end)
    {
        if ((marker_end < line.length()) && (line.charAt(marker_end) == '[')) {
            int id_end = line.indexOf(']', marker_end);
            if (id_end > 0) {
                String m_id = line.substring(marker_end + 1, id_end);
                return MARK_ID_PATTERN.matcher(m_id).matches() ? m_id : null;
            }
        }
        return null;
    }


    private void addLine(List<String> lines,
                         List<String> nums,
                         StringBuilder lineBuf,
                         int startNum,
                         Map<String, String> args,
                         List<Integer> autoBreakPositions,
                         List<String> openedTagNames,
                         List<String> openedTags) throws Exception
    {
        closeOpenedInlineTags(lineBuf, openedTagNames);
        String line = lineBuf.toString();
        if (line.length() == 0) {  // empty line
            line = "&#160;";  // add space as workaround to avoid div with 0px height
        }
        boolean highlight = false;
        String marker_id = null;
        String marker = args.get(ARG_M);
        if (! marker.equals("")) {
            // final String SPACES = "                                         ";
            int mark_len = marker.length();
            int mark_start = line.indexOf(marker);
            if (mark_start >= 0) {
                highlight = true;
                marker_id = getMarkerId(line, mark_start + mark_len);
                if (marker_id != null) {
                    mark_len += marker_id.length() + "[]".length();
                }
                // Remove marker
                line = line.substring(0, mark_start) + // SPACES.substring(0, marker.length()) +
                       line.substring(mark_start + mark_len);
            }
        }

        // Calc line style
        boolean isEven = (startNum % 2) == 0;
        String line_style = isEven ? args.get(ARG_EVEN) : args.get(ARG_ODD);
        if (highlight) {
            String markLineStyle = args.get(ARG_MSTYLE);
            if (markLineStyle != null) line_style = markLineStyle;
        }
        if (line_style == null) line_style = args.get(ARG_LINE);
        if (line_style == null) line_style = "__line__";
        // Note: Dummy class name "__line__" is needed for print output, because
        // div without class is omitted in print output.
        String cls = " class=\"" + line_style + "\"";

        // Wrap line with style
        if (marker_id != null) {
            line = "<span class=\"__marker_id__\" id=\"" + marker_id + "\" title=\"" + startNum + "\">" +
                    line + "</span>";
        }
        line = "<div" + cls + ">" + line + "\n</div>";
        // Note: Insert \n before </div>, otherwise if user copy-pastes the
        //       generated content, all lines would be merged into one line.
        lines.add(line);

        // Create line numbering
        if (nums != null) {
            String num_style = isEven ? args.get(ARG_ENUM) : args.get(ARG_ONUM);
            if (highlight) {
                String markNumStyle = args.get(ARG_MNUM);
                if (markNumStyle != null) num_style = markNumStyle;
            }
            if (num_style == null) num_style = args.get(ARG_NUM);
            if (num_style == null) num_style = line_style;
            cls = (num_style == null) ? "" : " class=\"" + num_style + "\"";

            // Wrap number with style
            // Add &#160; after number to force space between line number and content
            StringBuilder numDiv = new StringBuilder("<div" + cls + ">" + startNum + "&#160;");
            int countBreaks = autoBreakPositions.size();
            for (int i=0; i < countBreaks; i++) numDiv.append("<br />&#160;");
            numDiv.append("\n</div>");  // insert \n before </div>; see note above
            nums.add(numDiv.toString());
        }

        autoBreakPositions.clear();
        lineBuf.setLength(0);  // clear buffer for next line
        openOpenedInlineTags(lineBuf, openedTags);  // open inline tags for next line
    }


    private void readArg(String arg, Map<String, String> argMap)
    {
        if ((arg == null) || (arg.length() == 0)) {
            return;
        }
        int p = arg.indexOf('=');
        if (p < 0) argMap.put(arg, "true");
        else {
            argMap.put(arg.substring(0, p), arg.substring(p + 1));
        }
    }


    private int getContentStart(String line)
    {
        int p = 0;
        int len = line.length();
        // Skip whitespace
        while (p < len) {
            char ch = line.charAt(p);
            if (Character.isWhitespace(ch)) {
                ++p;  // skip whitespace
            } else
            if (ch == '<') {
                // Skip tags
                int tag_end = line.indexOf('>', p);
                if (tag_end < 0) break;
                p = tag_end + 1;
            } else {
                break;
            }
        }
        return p;
    }

    private int getContentEnd(String line)
    {
        int p = line.length();
        // Skip whitespace
        while (p > 0) {
            char ch = line.charAt(p - 1);
            if (Character.isWhitespace(ch)) {
                --p;  // skip whitespace
            } else
            if (ch == '>') {
                // Skip tags
                int tag_start = line.lastIndexOf('<', p - 1);
                if (tag_start < 0) break;
                p = tag_start;
            } else {
                break;
            }
        }
        return p;
    }

}
