/*
 * BaseRule.java
 * 
 *  Copyright (C) 2017  Manfred Paula, http://www.docmenta.org
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

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.xml.bind.DatatypeConverter;

import org.docma.plugin.Content;
import org.docma.plugin.FileContent;
import org.docma.plugin.Folder;
import org.docma.plugin.FolderType;
import org.docma.plugin.Group;
import org.docma.plugin.ImageFile;

import org.docma.plugin.LogLevel;
import org.docma.plugin.Node;
import org.docma.plugin.PluginUtil;
import org.docma.plugin.PubContent;
import org.docma.plugin.PubSection;
import org.docma.plugin.Reference;
import org.docma.plugin.StoreConnection;
import org.docma.plugin.Style;
import org.docma.plugin.UserSession;
import org.docma.plugin.implementation.StoreHelper;
import org.docma.plugin.rules.HTMLRule;
import org.docma.plugin.rules.HTMLRuleConfig;
import org.docma.plugin.rules.HTMLRuleContext;
import org.docma.util.CSSParser;
import org.docma.util.Log;
import org.docma.util.XMLElementContext;
import org.docma.util.XMLElementHandler;
import org.docma.util.XMLParser;
import org.docma.util.XMLProcessor;
import org.docma.util.XMLProcessorFactory;


/**
 *
 * @author MP
 */
public class BaseRule implements HTMLRule, XMLElementHandler
{
    public static final String CHECK_ID_ATTRIBUTE_REQUIRED = "attribute_required";
    public static final String CHECK_ID_CONTENT_REQUIRED = "content_required";
    public static final String CHECK_ID_INVALID_STYLE = "invalid_style";
    public static final String CHECK_ID_NORMALIZE_TEXTALIGN = "normalize_textalign";
    public static final String CHECK_ID_TRIM_EMPTY_PARAS = "trim_empty_paras";
    public static final String CHECK_ID_TRIM_FIGURE_SPACES = "trim_figure_spaces";
    // Link checks
    public static final String CHECK_ID_BROKEN_INLINE_INCLUSION = "broken_inline_inclusion";
    public static final String CHECK_ID_BROKEN_LINK = "broken_link";
    public static final String CHECK_ID_EMBEDDED_IMAGE = "embedded_image";
    public static final String CHECK_ID_INVALID_TARGET_TYPE = "invalid_target_type";
    public static final String CHECK_ID_INVALID_IMAGE_SRC = "invalid_image_src";

    // Link prefixes
    private final String IMAGE_PREFIX = "image/";
    private final String FILE_PREFIX = "file/";

    private boolean initialized = false;
    private String lang = "en";
    private String msgEmptyParaExists = "";
    private String msgEmptyParaRemoved = "";
    private String msgFigureSpacesExist = "";
    private String msgFigureSpacesRemoved = "";

    // Rule configuration
    private final SortedSet<String> configElems = new TreeSet<String>();
    private final SortedSet<String> elemsContentRequired = new TreeSet<String>();
    private final Map<String, List<List<AttribConf>>> elemsAttribRequired = 
            new HashMap<String, List<List<AttribConf>>>();
    private final SortedSet<String> elemsNormalizeTextalign = new TreeSet<String>();
            
    // Cached style info
    private final SortedMap<String, StyleInfo> styleInfos = new TreeMap<String, StyleInfo>();
    
    private HTMLRuleContext ruleCtx;     // used in methods apply and processElement
    private boolean corrected = false;   // used in methods apply and processElement

    public BaseRule()
    {
        debug("BaseRule constructor()");
    }
    
    /* --------------  Interface HTMLRule  ---------------------- */
    
    public String getShortInfo(String languageCode) 
    {
        debug("BaseRule.getShortInfo()");
        return PluginUtil.getResourceString(this.getClass(), languageCode, "shortInfo");
    }

    public String getLongInfo(String languageCode) 
    {
        debug("BaseRule.getLongInfo()");
        return PluginUtil.getResourceString(this.getClass(), languageCode, "longInfo");
    }

    public String[] getCheckIds() 
    {
        debug("BaseRule.getCheckIds()");
        return new String[] { 
          CHECK_ID_ATTRIBUTE_REQUIRED, 
          CHECK_ID_BROKEN_INLINE_INCLUSION,
          CHECK_ID_BROKEN_LINK,
          CHECK_ID_CONTENT_REQUIRED, 
          CHECK_ID_EMBEDDED_IMAGE,
          CHECK_ID_INVALID_IMAGE_SRC,
          CHECK_ID_INVALID_STYLE,
          CHECK_ID_INVALID_TARGET_TYPE,
          CHECK_ID_NORMALIZE_TEXTALIGN,
          CHECK_ID_TRIM_EMPTY_PARAS, 
          CHECK_ID_TRIM_FIGURE_SPACES
        };
    }

    public String getCheckTitle(String checkId, String languageCode) 
    {
        debug("BaseRule.getCheckTitle()");
        return PluginUtil.getResourceString(this.getClass(), languageCode, checkId + ".title");
    }

    public boolean supportsAutoCorrection(String checkId) 
    {
        debug("BaseRule.supportsAutoCorrection()");
        return checkId.equals(CHECK_ID_ATTRIBUTE_REQUIRED) ||
               checkId.equals(CHECK_ID_CONTENT_REQUIRED) ||
               checkId.equals(CHECK_ID_EMBEDDED_IMAGE) ||
               checkId.equals(CHECK_ID_NORMALIZE_TEXTALIGN) ||
               checkId.equals(CHECK_ID_TRIM_EMPTY_PARAS) || 
               checkId.equals(CHECK_ID_TRIM_FIGURE_SPACES);
    }

    public LogLevel getDefaultLogLevel(String checkId) 
    {
        debug("BaseRule.getDefaultLogLevel()");
        if (checkId.equals(CHECK_ID_BROKEN_INLINE_INCLUSION) ||
            checkId.equals(CHECK_ID_BROKEN_LINK) || 
            checkId.equals(CHECK_ID_EMBEDDED_IMAGE) || 
            checkId.equals(CHECK_ID_INVALID_IMAGE_SRC) || 
            checkId.equals(CHECK_ID_INVALID_STYLE) ||
            checkId.equals(CHECK_ID_INVALID_TARGET_TYPE) ||
            checkId.equals(CHECK_ID_NORMALIZE_TEXTALIGN)) {
            return LogLevel.WARNING;
        } else {
            return LogLevel.INFO;
        }
    }
    
    public void configure(HTMLRuleConfig conf) 
    {
        debug("BaseRule.configure()");
        configElems.clear();
        elemsContentRequired.clear();
        elemsAttribRequired.clear();
        elemsNormalizeTextalign.clear();
        // elemsNotAllowed.clear();
        for (String arg : conf.getArguments()) {
            arg = arg.toLowerCase();
            if (arg.startsWith(CHECK_ID_CONTENT_REQUIRED + "=")) {
                String val = arg.substring(CHECK_ID_CONTENT_REQUIRED.length() + 1);
                readContentRequiredConfig(val);
            } else if (arg.startsWith(CHECK_ID_ATTRIBUTE_REQUIRED + "=")) {
                String val = arg.substring(CHECK_ID_ATTRIBUTE_REQUIRED.length() + 1);
                readAttribRequiredConfig(val);
            } else if (arg.startsWith(CHECK_ID_NORMALIZE_TEXTALIGN + "=")) { 
                String val = arg.substring(CHECK_ID_NORMALIZE_TEXTALIGN.length() + 1).toLowerCase();
                readNormalizeTextAlignConfig(val);
            }
            // } else if (arg.startsWith(SUFFIX_NOT_ALLOWED + "=")) {
            //     StringTokenizer st = new StringTokenizer(arg.substring(SUFFIX_NOT_ALLOWED.length() + 1), ",");
            //     while (st.hasMoreTokens()) {
            //         String nm = st.nextToken();
            //         configElems.add(nm);
            //         checksNotAllowed.add(checkIdNotAllowed(nm));
            //     }
            // } 
        }  // for-loop
        
        if (elemsNormalizeTextalign.isEmpty()) {  // argument normalize_textalign does not exist
            elemsNormalizeTextalign.add("*");  // wildcard (apply check to all elements)
        }
    }
    
    public void startBatch(HTMLRuleContext ctx) 
    {
        debug("BaseRule.startBatch()");
        initialized = false;
        styleInfos.clear();
    }

    public void finishBatch(HTMLRuleContext ctx) 
    {
        debug("BaseRule.finishBatch()");

        //
        // Write style statistics        
        //
        if ((ctx != null) && ctx.isEnabled(CHECK_ID_INVALID_STYLE)) {
            ctx.logHeader(1, label("headStyleStatistics"));

            final String dots  = "........................................";
            final String space = "                                        ";
            // String countLabel = "Count";
            // String countLine = space.substring(countLabel.length()) + countLabel + "\n";
            StringBuilder statsValid = new StringBuilder();
            StringBuilder statsInvalid = new StringBuilder();
            int totalValid = 0;
            int totalInvalid = 0;
            for (StyleInfo sinfo : styleInfos.values()) {
                StringBuilder stats; 
                if (sinfo.styleExists() || sinfo.isPredefined()) {
                    stats = statsValid;
                    totalValid += sinfo.getCount();
                } else {
                    stats = statsInvalid;
                    totalInvalid += sinfo.getCount();
                }
                String cssName = sinfo.getCSSName();
                String cntStr =  " " + sinfo.getCount();
                int len = cssName.length() + cntStr.length() + 1;
                stats.append(cssName).append(" ");
                if (len < dots.length() - 3) {
                    stats.append(dots.substring(len));
                } else {
                    stats.append("...");
                }
                stats.append(cntStr).append("\n");
            }
            String totalLabel = label("stylesTotal");
            String sumValid = totalLabel + ": " + totalValid;
            statsValid.append(space.substring(sumValid.length()))
                      .append(sumValid).append("\n");
            String sumInvalid = totalLabel + ": " + totalInvalid;
            statsInvalid.append(space.substring(sumInvalid.length()))
                        .append(sumInvalid).append("\n");
            
            // ctx.logHeader(1, label("headValidStyles"));
            ctx.logText(label("headValidStyles"), statsValid.toString());
            // ctx.logHeader(1, label("headInvalidStyles"));
            ctx.logText(label("headInvalidStyles"), statsInvalid.toString());
            // ctx.logHeader(2, label("msgTotalInvalidStyleCount", totalInvalid));
        }
        styleInfos.clear();
    }

    public String apply(String content, HTMLRuleContext ctx) 
    {
        debug("BaseRule.apply()");
        ruleCtx = ctx;
        initOnStart(ctx);

        String result = null;
        
        //
        // Check broken inline inclusions
        //
        if (ctx.isEnabled(CHECK_ID_BROKEN_INLINE_INCLUSION)) {
            checkInlineInclusions(content);
        }

        //
        // Checks realized by usage of XMLProcessor.
        //
        boolean chk_cont_required = ctx.isEnabled(CHECK_ID_CONTENT_REQUIRED);
        boolean chk_att_required = ctx.isEnabled(CHECK_ID_ATTRIBUTE_REQUIRED);
        boolean chk_broken_link = ctx.isEnabled(CHECK_ID_BROKEN_LINK);
        boolean chk_embedded_image = ctx.isEnabled(CHECK_ID_EMBEDDED_IMAGE);
        boolean chk_image_src = ctx.isEnabled(CHECK_ID_INVALID_IMAGE_SRC);
        boolean chk_invalid_style = ctx.isEnabled(CHECK_ID_INVALID_STYLE);
        boolean chk_normalize_align = ctx.isEnabled(CHECK_ID_NORMALIZE_TEXTALIGN);
        boolean chk_target_type = ctx.isEnabled(CHECK_ID_INVALID_TARGET_TYPE);
        
        boolean has_enabled = chk_cont_required || chk_att_required || 
                              chk_broken_link || chk_embedded_image || 
                              chk_image_src || chk_invalid_style || 
                              chk_normalize_align || chk_target_type;  
        
        if (has_enabled) {   // if one or more checks are enabled
            boolean has_correct = ctx.isAutoCorrect(CHECK_ID_CONTENT_REQUIRED) || 
                                  ctx.isAutoCorrect(CHECK_ID_ATTRIBUTE_REQUIRED) || 
                                  ctx.isAutoCorrect(CHECK_ID_EMBEDDED_IMAGE) || 
                                  ctx.isAutoCorrect(CHECK_ID_NORMALIZE_TEXTALIGN);
            XMLProcessor xmlproc = XMLProcessorFactory.newInstance();
            xmlproc.setCheckWellformed(true);
            xmlproc.setIgnoreElementCase(true);
            if (chk_invalid_style || elemsNormalizeTextalign.contains("*")) {
                xmlproc.setElementHandler(this);  // process all elements
            } else {
                if (chk_broken_link || chk_target_type) {
                    xmlproc.setElementHandler("a", this);
                }
                if (chk_embedded_image || chk_image_src || chk_target_type) {
                    xmlproc.setElementHandler("img", this);
                }
                // Register all elements configured for 
                // CHECK_ID_CONTENT_REQUIRED, CHECK_ID_ATTRIBUTE_REQUIRED and
                // CHECK_ID_NORMALIZE_TEXTALIGN
                for (String elem : configElems) {
                    xmlproc.setElementHandler(elem, this);
                }
            }

            try {
                corrected = false;
                String fixcontent = (result != null) ? result : content;
                if (has_correct) {
                    StringBuilder out = new StringBuilder();
                    xmlproc.process(fixcontent, out);
                    if (corrected) {
                        result = out.toString();
                    }
                } else {  // checks create only log entries
                    xmlproc.process(fixcontent);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                ctx.log(LogLevel.ERROR, ex.getMessage());
            }
        }

        //
        // Some more quick and dirty checks that are implemented with string 
        // pattern matching. 
        // Should be replaced by real XML implementation.
        //
        boolean chk_empty_p = ctx.isEnabled(CHECK_ID_TRIM_EMPTY_PARAS);
        boolean chk_fig_spaces = ctx.isEnabled(CHECK_ID_TRIM_FIGURE_SPACES);
        boolean correct_empty_p = ctx.isAutoCorrect(CHECK_ID_TRIM_EMPTY_PARAS);
        boolean correct_fig_spaces = ctx.isAutoCorrect(CHECK_ID_TRIM_FIGURE_SPACES);
        
        if (chk_empty_p || chk_fig_spaces) {
            String fixcontent = (result != null) ? result : content;
            if (chk_empty_p) {
                fixcontent = removeEmptyParaFromEnd(fixcontent, correct_empty_p, ctx);
            }
            if (chk_fig_spaces) {
                fixcontent = removeSpacesBeforeAfterFigure(fixcontent, correct_fig_spaces, ctx);
            }
            if (correct_empty_p || correct_fig_spaces) {
                result = fixcontent;  // Replace old content by fixed content
            }
        }
        
        return result;
    }

    /* --------------  Interface XMLElementHandler  ---------------------- */
    
    public void processElement(XMLElementContext elemCtx) 
    {
        String ename = elemCtx.getElementName().toLowerCase();
        int pos = elemCtx.getCharacterOffset();

        boolean chkTargetType = ruleCtx.isEnabled(CHECK_ID_INVALID_TARGET_TYPE);
        
        //
        // Check broken links: CHECK_ID_BROKEN_LINK, CHECK_ID_TARGET_TYPE
        //
        if (ename.equals("a")) {
            if (ruleCtx.isEnabled(CHECK_ID_BROKEN_LINK) || chkTargetType) {
                checkBrokenLink(elemCtx, pos);
            }
        }
        
        //
        // Check image references: CHECK_ID_IMAGE_SRC, CHECK_ID_TARGET_TYPE
        //
        if (ename.equals("img")) {
            if (ruleCtx.isEnabled(CHECK_ID_EMBEDDED_IMAGE) ||
                ruleCtx.isEnabled(CHECK_ID_INVALID_IMAGE_SRC) || chkTargetType) {
                checkImageSrc(elemCtx, pos);
            }
        }

        //
        // Check styles
        //
        if (ruleCtx.isEnabled(CHECK_ID_INVALID_STYLE)) {
            checkElementClassNames(elemCtx, pos);
        }
        
        //
        // Check for empty elements: CHECK_ID_CONTENT_REQUIRED
        // If auto-correction is enabled, this removes the tag.
        //
        if (elemsContentRequired.contains(ename) && ruleCtx.isEnabled(CHECK_ID_CONTENT_REQUIRED)) {
            if (elemCtx.isEmptyElement() || elemCtx.getElementContent().equals("")) {
                if (ruleCtx.isAutoCorrect(CHECK_ID_CONTENT_REQUIRED)) {
                    elemCtx.replaceElement("");  // remove the empty element
                    corrected = true;
                    ruleCtx.logInfo(CHECK_ID_CONTENT_REQUIRED, pos, label("msgEmptyElementRemoved", ename));
                    return;
                } else {
                    ruleCtx.log(CHECK_ID_CONTENT_REQUIRED, pos, label("msgEmptyElementExists", ename));
                }
            }
        }
        
        //
        // Check for required attributes: CHECK_ID_ATTRIBUTE_REQUIRED
        // If auto-correction is enabled, this may add attributes or remove the tag.
        //
        List<List<AttribConf>> orList = elemsAttribRequired.get(ename);
        if ((orList != null) && ruleCtx.isEnabled(CHECK_ID_ATTRIBUTE_REQUIRED)) {
            boolean correctAttRequired = ruleCtx.isAutoCorrect(CHECK_ID_ATTRIBUTE_REQUIRED);
            boolean remove;
            String missingName = null;
            String missingDefault = null;
            String forbidden = null;
            if (orList.isEmpty()) {
                // If no attributes defined in the configuration, then remove
                // element if it has no attributes.
                remove = (elemCtx.getAttributeCount() == 0);
            } else {
                remove = true;
                for (List<AttribConf> andList : orList) {
                    boolean is_valid = true;
                    boolean add_default = false;
                    boolean remove_forbidden = false;
                    for (AttribConf ac : andList) {
                        String aName = ac.getName();
                        boolean attMissing = elemCtx.getAttributeIndex(aName) < 0;
                        boolean attForbidden = ac.isForbidden();
                        if (attMissing && !attForbidden) {
                            // Attribute is missing, but is defined as required 
                            if (correctAttRequired && (ac.getDefaultValue() != null)) {
                                add_default = true;
                            } else {
                                missingName = aName;
                                missingDefault = ac.getDefaultValue();
                                forbidden = null;
                                is_valid = false;
                                break; // stop evaluating andList, continue with next andList
                            }
                        }
                        if (attForbidden && !attMissing) {
                            if (correctAttRequired) {
                                remove_forbidden = true;  // forbidden attribute exists
                            } else {
                                forbidden = aName;
                                missingName = null;
                                missingDefault = null;
                                is_valid = false;
                                break;
                            }
                        }
                    }
                    if (is_valid) {
                        remove = false;  // Do not remove tag if its valid.
                        
                        // Element is valid -> no change, except setting
                        // default attributes and removing forbidden attributes (if any).
                        if (add_default || remove_forbidden) {
                            for (AttribConf ac : andList) {
                                String aName = ac.getName();
                                boolean attMissing = elemCtx.getAttributeIndex(aName) < 0;
                                boolean attForbidden = ac.isForbidden();
                                if (attMissing && (ac.getDefaultValue() != null) && !attForbidden) {
                                    elemCtx.setAttribute(aName, ac.getDefaultValue());
                                    String msg = label("msgMissingAttribAddedDefault", ename, aName);
                                    ruleCtx.logInfo(CHECK_ID_ATTRIBUTE_REQUIRED, pos, msg);
                                } else if (attForbidden && !attMissing) {
                                    elemCtx.setAttribute(aName, null);  // remove attribute
                                    String msg = label("msgForbiddenAttribRemoved", ename, aName);
                                    ruleCtx.logInfo(CHECK_ID_ATTRIBUTE_REQUIRED, pos, msg);
                                }
                            }
                            corrected = true;
                        }
                        
                        break;  // stop at the first valid and-list 
                    }
                }
            }
            if (remove) {
                if (correctAttRequired) {
                    // remove the tag, but keep the content
                    elemCtx.replaceElement(elemCtx.getElementContent());
                    corrected = true;
                    String msg = (missingName == null) ? label("msgTagWithoutAttribRemoved", ename) 
                                                       : label("msgTagWithMissingAttribRemoved", ename, missingName);
                    ruleCtx.logInfo(CHECK_ID_ATTRIBUTE_REQUIRED, pos, msg);
                    return;  // Skip the other checks, because element has been removed
                } else {
                    String msg;
                    if (missingName == null) { 
                        msg = (forbidden == null) 
                                ? label("msgTagWithoutAttrib", ename)
                                : label("msgTagWithForbiddenAttrib", ename, forbidden); 
                    } else {
                        msg = (missingDefault == null) 
                                ? label("msgTagAttribMissing", ename, missingName)
                                : label("msgTagAttribMissingDefault", ename, missingName);
                    }
                    ruleCtx.log(CHECK_ID_ATTRIBUTE_REQUIRED, pos, msg);
                }
            }
        }

        //
        // Check for CSS property text-align: CHECK_ID_NORMALIZE_TEXTALIGN
        // If auto-correction is enabled, this replaces the CSS property 
        // text-align by one of the CSS classes align-left, align-center, 
        // align-right, align-full.
        //
        if (ruleCtx.isEnabled(CHECK_ID_NORMALIZE_TEXTALIGN)) {
            if (elemsNormalizeTextalign.contains("*") || elemsNormalizeTextalign.contains(ename)) {
                checkNormalizeTextalign(elemCtx, pos);
            }
        }

        // if (elemsNotAllowed.contains(ename) && ruleCtx.isEnabled(CHECK_ID_NOT_ALLOWED)) {
        //     if (ruleCtx.isAutoCorrect(CHECK_ID_NOT_ALLOWED)) {
        //         // remove the tag, but keep the content
        //         elemCtx.replaceElement(elemCtx.getElementContent());
        //         corrected = true;
        //         ruleCtx.logInfo(CHECK_ID_NOT_ALLOWED, pos, label("msgTagRemoved", ename));
        //         return;
        //     } else {
        //         ruleCtx.log(CHECK_ID_NOT_ALLOWED, pos, label("msgTagExists", ename));
        //     }
        // }
    }

    /* --------------  Private methods  ---------------------- */

    private void checkNormalizeTextalign(XMLElementContext elemCtx, int pos)
    {
        String style = elemCtx.getAttributeValue("style");
        if ((style != null) && style.contains("text-align")) {
            SortedMap<String, String> props = CSSParser.parseCSSProperties(style);
            String alignVal = props.remove("text-align");
            if (alignVal != null) {
                String elemName = elemCtx.getElementName();
                String alignCls = null;
                if (alignVal.equalsIgnoreCase("left")) {
                    alignCls = "align-left";
                } else if (alignVal.equalsIgnoreCase("center")) {
                    alignCls = "align-center";
                } else if (alignVal.equalsIgnoreCase("right")) {
                    alignCls = "align-right";
                } else if (alignVal.equalsIgnoreCase("justify")) {
                    alignCls = "align-full";
                }
                if (alignCls == null) {
                    String msg = label("msgInvalidTextAlignStyle", elemName, alignVal);
                    ruleCtx.log(CHECK_ID_NORMALIZE_TEXTALIGN, pos, msg);
                } else {
                    if (ruleCtx.isAutoCorrect(CHECK_ID_NORMALIZE_TEXTALIGN)) {
                        // Convert to CSS class
                        String cssNew = CSSParser.propertiesToString(props);
                        if ("".equals(cssNew)) {
                            cssNew = null;  // remove style attribute
                        }
                        String clsNew = addCSSClass(elemCtx.getAttributeValue("class"), alignCls);
                        elemCtx.setAttribute("style", cssNew);
                        elemCtx.setAttribute("class", clsNew);
                        corrected = true;
                        String msg = label("msgTextAlignNormalized", elemName, alignVal, alignCls);
                        ruleCtx.logInfo(CHECK_ID_NORMALIZE_TEXTALIGN, pos, msg);
                    } else {
                        // Just add log message
                        String msg = label("msgFoundTextAlignStyle", elemName, alignVal, alignCls);
                        ruleCtx.log(CHECK_ID_NORMALIZE_TEXTALIGN, pos, msg);
                    }
                }
            }
        }
    }

    private void checkElementClassNames(XMLElementContext elemCtx, int pos)
    {
        String cls_val = elemCtx.getAttributeValue("class");
        if (cls_val == null) {
            return;
        }
        StringTokenizer st = new StringTokenizer(cls_val);
        while (st.hasMoreTokens()) {
            String css_cls = st.nextToken();
            StyleInfo sinfo = styleInfos.get(css_cls);
            if (sinfo == null) {
                StoreConnection conn = ruleCtx.getStoreConnection();
                Style s = conn.getStyleVariant(css_cls, null);
                sinfo = new StyleInfo(css_cls, s);  // s may be null
                styleInfos.put(css_cls, sinfo);
            }
            sinfo.increaseCount();
            if (! (sinfo.styleExists() || sinfo.isPredefined())) {
                ruleCtx.log(CHECK_ID_INVALID_STYLE, pos, label("msgStyleNotFound", css_cls));
            }
        }
    }
    
    private void checkInlineInclusions(String content)
    {
        // Find inclusions
        int len = content.length();
        int startpos = 0;
        while (startpos < len) {
            int p1 = content.indexOf("[#", startpos);
            if (p1 < 0) {  // no inclusion found
                break;
            }
            p1 += 2;                // p1 is position after [#
            if (p1 >= len) break;   // end of content string reached
            startpos = p1;          // in next loop continue search at p1
            int p2 = content.indexOf("]", startpos);
            if (p2 < 0) {
                continue;
            }
            boolean isContentInclusion = content.charAt(p1) == '#';  // content inclusion: [##
            if (isContentInclusion) {
                p1++;
            }
            if ((p2 - p1) > DocmaConstants.ALIAS_MAX_LENGTH) {
                // ruleCtx.log(CHECK_ID_BROKEN_INLINE_INCLUSION, p1, label("msgReferencedAliasTooLong", DocmaConstants.ALIAS_MAX_LENGTH));
                continue;
            } else {
                String ref_alias = content.substring(p1, p2);
                if (ref_alias.equals("") || !ref_alias.matches(DocmaConstants.REGEXP_ALIAS_LINK)) {
                    continue;
                }
                StoreConnection conn = ruleCtx.getStoreConnection();
                Node nd = conn.getNodeByAlias(ref_alias);
                if (nd == null) {
                    ruleCtx.log(CHECK_ID_BROKEN_INLINE_INCLUSION, p1, label("msgInlineInclusionAliasNotFound", ref_alias));
                } else if (isContentInclusion && !(nd instanceof Content)) {
                    ruleCtx.log(CHECK_ID_BROKEN_INLINE_INCLUSION, p1, label("msgContentInclusionToNonContent", ref_alias));
                }
            }
        }
    }
    
    private void checkBrokenLink(XMLElementContext elemCtx, int pos)
    {
        String href = elemCtx.getAttributeValue("href");
        if (href != null) {  // href attribute exists
            boolean isContentLink = href.startsWith("#");
            boolean isImageLink = href.startsWith(IMAGE_PREFIX);
            boolean isFileLink = href.startsWith(FILE_PREFIX);

            String target_alias = null;
            if (isContentLink) target_alias = href.substring(1).trim();
            else if (isImageLink) target_alias = href.substring(IMAGE_PREFIX.length()).trim();
            else if (isFileLink) target_alias = href.substring(FILE_PREFIX.length()).trim();

            if (target_alias != null) {  // Do not check external links
                if (target_alias.equals("")) {
                    ruleCtx.log(CHECK_ID_BROKEN_LINK, pos, label("msgMissingAliasInHref", href));
                } else {
                    StoreConnection conn = ruleCtx.getStoreConnection();
                    Node nd = conn.getNodeByAlias(target_alias);
                    if (nd == null) {
                        ruleCtx.log(CHECK_ID_BROKEN_LINK, pos, label("msgAliasInHrefNotFound", target_alias));
                    } else {
                        if (isContentLink) {
                            boolean is_cont_nd;
                            if (nd instanceof Reference) {
                                Reference ref = (Reference) nd;
                                is_cont_nd = ref.isContentInclusion() || ref.isSectionInclusion();
                            } else {
                                is_cont_nd = (nd instanceof PubContent) || (nd instanceof PubSection);
                            }
                            if (! is_cont_nd) {
                                ruleCtx.log(CHECK_ID_INVALID_TARGET_TYPE, pos, label("msgContentLinkToNonContent", target_alias));
                            }
                        } else if (isImageLink) { 
                            if (! (nd instanceof ImageFile)) {
                                ruleCtx.log(CHECK_ID_INVALID_TARGET_TYPE, pos, label("msgImageLinkToNonSupportedImage", target_alias));
                            }
                        } else {  // file link
                            if (! (nd instanceof FileContent)) {
                                ruleCtx.log(CHECK_ID_INVALID_TARGET_TYPE, pos, label("msgFileLinkToNonFile", target_alias));
                            }
                        }
                    }
                }
            }
        }
    }
    
    private void checkImageSrc(XMLElementContext elemCtx, int pos)
    {
        final String DATA_PREFIX = "data:";
        final String DATA_IMAGE_PREFIX = "data:image/";
        String src = elemCtx.getAttributeValue("src");
        if (src == null) {
            ruleCtx.log(CHECK_ID_INVALID_IMAGE_SRC, pos, label("msgMissingImageSrcAttribute"));
        } else {
            boolean isImageLink = src.startsWith(IMAGE_PREFIX);
            if (isImageLink) {
                String  target_alias = src.substring(IMAGE_PREFIX.length()).trim();
                if (target_alias.equals("")) {
                    ruleCtx.log(CHECK_ID_INVALID_IMAGE_SRC, pos, label("msgMissingAliasInImageSrc", src));
                } else {
                    StoreConnection conn = ruleCtx.getStoreConnection();
                    Node nd = conn.getNodeByAlias(target_alias);
                    if (nd == null) {
                        ruleCtx.log(CHECK_ID_INVALID_IMAGE_SRC, pos, label("msgAliasInImageSrcNotFound", target_alias));
                    } else if (! (nd instanceof ImageFile)) {
                        ruleCtx.log(CHECK_ID_INVALID_TARGET_TYPE, pos, label("msgImageSrcToNonSupportedImage", target_alias));
                    }
                }
            } else if (src.startsWith(DATA_IMAGE_PREFIX)) {
                String imgAlias = null;
                String extract = (src.length() < 40) ? src : src.substring(0, 37) + "...";
                if (ruleCtx.isAutoCorrect(CHECK_ID_EMBEDDED_IMAGE)) {
                    int p = src.indexOf(';');
                    if (p > 0) {
                        String mime = src.substring(DATA_PREFIX.length(), p).toLowerCase();
                        String ext = ImageUtil.guessExtByMIMEType(mime);
                        int p2 = src.indexOf(',', p);
                        if ((ext != null) && (p2 > p)) {
                            String encoding = src.substring(p + 1, p2).trim().toLowerCase();
                            if (encoding.equals("base64")) {
                                String data = src.substring(p2 + 1).trim();
                                imgAlias = storeBase64Image(ext, data, pos);
                                if (imgAlias != null) {
                                    elemCtx.setAttribute("src", IMAGE_PREFIX + imgAlias);
                                    corrected = true;
                                }
                            }
                        }
                    }
                }
                if (imgAlias != null) {
                    ruleCtx.logInfo(CHECK_ID_EMBEDDED_IMAGE, pos, label("msgEmbeddedImageCorrected", extract, imgAlias));
                } else {
                    ruleCtx.log(CHECK_ID_EMBEDDED_IMAGE, pos, label("msgEmbeddedImage", extract));
                }
            } else {
                ruleCtx.log(CHECK_ID_INVALID_IMAGE_SRC, pos, label("msgInvalidImageSrc", src));
            }
        }
    }
    
    private String storeBase64Image(String ext, String data, int elemPos)
    {
        try {
            String nodeId = ruleCtx.getNodeId();
            if (nodeId != null) {
                StoreConnection con = ruleCtx.getStoreConnection();
                Folder folder = StoreHelper.getNearestImageFolder(con, nodeId);
                byte[] imgData = parseBase64(data);
                String imgAlias = createUniqueImageAlias(con, imgData);
                if (con.getNodeIdByAlias(imgAlias) == null) {
                    String fname = imgAlias + "." + ext;
                    FileContent imgNode = con.createFileContent(fname, true);
                    imgNode.setContent(imgData);
                    folder.addChildren(imgNode);
                } else {
                    ruleCtx.logInfo(CHECK_ID_EMBEDDED_IMAGE, label("msgEmbeddedImageExists", imgAlias));
                }
                return imgAlias;
            } else {
                return null;
            }
        } catch (Throwable ex) {
            ruleCtx.log(LogLevel.ERROR, elemPos, "Failed to store Base64 image: " + ex.getMessage());
            return null;
        }
    }
    
    private String createUniqueImageAlias(StoreConnection con, byte[] image) throws Exception
    {
        String md5 = getMD5Hash(image);
        if (md5.length() > 32) {
            md5 = md5.substring(0, 32);
        }
        return "image_" + md5;
        
        // long stamp = System.currentTimeMillis();
        // for (int i = 0; i < 20; i++) {
        //     String alias = "image" + stamp;
        //     if (con.getNodeIdByAlias(alias) == null) {
        //         return alias;
        //     }
        //     stamp++;
        // }
        // throw new Exception("Failed to store Base64 image: Could not create random image alias!");
    }

    private String getMD5Hash(byte[] data) throws Exception
    {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] enc = md.digest(data);
        BigInteger bigint = new BigInteger(1, enc);
        return bigint.toString(16);
    }
    
    private byte[] parseBase64(String data)
    {
        try {
            return Base64.getDecoder().decode(data);
        } catch (Throwable ex) {
            // If the java.util.Base64 class does not exist (JRE 1.7 and below),
            // then use javax.xml.bind.DatatypeConverter.
            return DatatypeConverter.parseBase64Binary(data);
        }
    }

    private String addCSSClass(String classVal, String clsAdd) 
    {
        if ((clsAdd == null) || clsAdd.equals("")) {  // nothing to add
            return classVal;
        }
        if ((classVal == null) || classVal.equals("")) {
            return clsAdd;
        }
        classVal = classVal.trim();
        if ((" " + classVal + " ").contains(" " + clsAdd + " ")) {
            return classVal;
        }
        return classVal + " " + clsAdd;
    }

    private void debug(String msg)
    {
        if (DocmaConstants.DEBUG) {
            Log.info(msg);
        }
    }
    
    private void initOnStart(HTMLRuleContext context)
    {
        // Initialize log messages
        if (! initialized) {
            StoreConnection conn = context.getStoreConnection();
            UserSession     sess = (conn == null) ? null : conn.getUserSession();
            Locale          loc  = (sess == null) ? null : sess.getCurrentLocale();
            lang = (loc == null)  ? "en" : loc.getLanguage();
            
            msgEmptyParaExists = label("msgEmptyParaExists");
            msgEmptyParaRemoved = label("msgEmptyParaRemoved");
            msgFigureSpacesExist = label("msgFigureSpacesExist");
            msgFigureSpacesRemoved = label("msgFigureSpacesRemoved");
            
            initialized = true;
        }
    }
    
    private String label(String msgKey, Object... args) 
    {
        return PluginUtil.getResourceString(this.getClass(), lang, msgKey, args);
    }
    
    private String resolveConfigEscapes(String str)
    {
        int pos = 0;
        while (pos < str.length()) {
            int uni_start = str.indexOf("\\u", pos);
            if (uni_start < 0) {
                return str;
            } else {
                pos = uni_start + 1;  // continue after this position
                int uni_end = uni_start + 6;
                if (str.length() >= uni_end) {
                    try {
                        char unicode = (char) Integer.parseInt(str.substring(uni_start + 2, uni_end), 16);
                        str = str.substring(0, uni_start) + unicode + str.substring(uni_end);
                    } catch (Exception ex) {}  // no valid escape; continue search at pos
                }
            }
        }
        return str;
    }
    
    private String removeSpacesBeforeAfterFigure(String content, boolean correct, HTMLRuleContext context)
    {
        final String IMG_START = "<img ";
        int tag_start = content.indexOf(IMG_START);
        if (tag_start < 0) {
            return content;  // content does not contain any image tag
        }
        int len = content.length();
        List attnames = new ArrayList();
        List attvalues = new ArrayList();
        StringBuilder buf = null;
        int copypos = 0;
        do {
            int att_start = tag_start + IMG_START.length();  // start of img attributes
            int startpos = att_start;  // continue search for next img tag after this tag
            attnames.clear(); 
            attvalues.clear();
            // int tag_end = content.indexOf("/>", tag_start);
            int tag_end = XMLParser.parseTagAttributes(content, att_start, attnames, attvalues);
            if (tag_end > 0) {   // position of '>', tag_end < 0 means syntax error 
                int pos_after_tag = tag_end + 1;
                startpos = pos_after_tag;  // continue search for next tag after this tag
                if (attnames.contains("title")) { // img is a figure, i.e. has a title
                    
                    // Skip all whitespace before the img tag
                    boolean skipped = false;
                    int pos = skipWhiteSpaceBefore(content, tag_start); 
                    // if only whitespace exists between figure and previous element
                    if ((pos < (tag_start - 1)) && (pos > 0) && (content.charAt(pos) == '>')) { 
                        if (buf == null) { buf = new StringBuilder(len); }
                        buf.append(content, copypos, pos + 1);
                        copypos = tag_start;  // continue copying after the whitespace
                        skipped = true;
                    }
                    
                    // Skip all whitespace after the img tag
                    pos = skipWhiteSpaceAfter(content, pos_after_tag);
                    // if only whitespace exists between figure and following element
                    if ((pos > pos_after_tag) && (pos < len) && (content.charAt(pos) == '<')) {  
                        if (buf == null) { buf = new StringBuilder(len); }
                        buf.append(content, copypos, pos_after_tag);
                        copypos = pos;  // continue copying after the whitespace
                        skipped = true;
                    }
                    
                    // Write log message
                    if (skipped) {
                        if (correct) {
                            context.logInfo(CHECK_ID_TRIM_FIGURE_SPACES, tag_start, msgFigureSpacesRemoved);
                        } else {
                            context.log(CHECK_ID_TRIM_FIGURE_SPACES, tag_start, msgFigureSpacesExist);
                        }
                    }
                }
            }
            if (startpos >= len) break;
            tag_start = content.indexOf(IMG_START, startpos);  // search next img tag
        } while (tag_start >= 0);

        if (buf == null) {   // no whitespace removed
            return content;
        } else {
            if (correct) {
                if (copypos < len) {
                    buf.append(content, copypos, len); // copy remaining content
                }
                return buf.toString();
            } else {
                return content;  // content is unchanged
            }
        }
    }
    
    private static int skipWhiteSpaceBefore(String content, int startpos) 
    {
        int pos = startpos;
        while (pos > 0) {
            char ch = content.charAt(--pos);
            if (Character.isWhitespace(ch)) continue;
            if ((ch == ';') && (pos >= 5) && 
                (content.regionMatches(pos - 5, "&#160;", 0, 6) ||
                 content.regionMatches(pos - 5, "&nbsp;", 0, 6))) {
                pos -= 5;
                continue;
            }
            break;
        }
        // return 0 or the position of the first non-whitespace character.
        return pos;
    }
    
    private static int skipWhiteSpaceAfter(String content, int startpos) 
    {
        int pos = startpos;
        int len = content.length();
        while (pos < len) {
            char ch = content.charAt(pos);
            if (Character.isWhitespace(ch)) {
                pos++;
                continue;
            }
            if ((ch == '&') && ((pos + 5) < len) && 
                (content.regionMatches(pos, "&#160;", 0, 6) ||
                 content.regionMatches(pos, "&nbsp;", 0, 6))) {
                pos += 6;
                continue;
            }
            break;
        }
        // pos is either the position of the first non-whitespace character 
        // or equal to content.length() 
        return pos;
    }

    private String removeEmptyParaFromEnd(String content, boolean correct, HTMLRuleContext context)
    {
        final String PATT_1 = "<p>&#160;</p>";
        final String PATT_2 = "<p>&nbsp;</p>";
        final String PATT_3 = "<p></p>";

        String str = content.trim();
        int strlen = str.length();
        int endpos = -1;
        
        if (str.endsWith(PATT_1)) {
            endpos = strlen - PATT_1.length();
        } else if (str.endsWith(PATT_2)) { 
            endpos = strlen - PATT_2.length();
        } else if (str.endsWith(PATT_3)) {
            endpos = strlen - PATT_3.length();
        }
        
        if (endpos >= 0) {  // if empty para exists
            // Write log message
            if (correct) {
                context.logInfo(CHECK_ID_TRIM_EMPTY_PARAS, endpos, msgEmptyParaRemoved);
                return str.substring(0, endpos);  // trim empty para
            } else {
                context.log(CHECK_ID_TRIM_EMPTY_PARAS, endpos, msgEmptyParaExists);
                return content;
            }
        } else {
            return content;
        }
    }

    private void readContentRequiredConfig(String config)
    {
        StringTokenizer st = new StringTokenizer(config, ",");
        while (st.hasMoreTokens()) {
            String nm = st.nextToken();
            configElems.add(nm);
            elemsContentRequired.add(nm);
        }
    }

    private void readAttribRequiredConfig(String config)
    {
        StringTokenizer st = new StringTokenizer(config, ",");
        while (st.hasMoreTokens()) {
            String elem = st.nextToken();
            List<List<AttribConf>> orList = new ArrayList<List<AttribConf>>();
            int p1 = elem.indexOf('[');
            if (p1 > 0) {
                int p2 = elem.lastIndexOf(']');
                if (p2 > p1) {
                    String expr = elem.substring(p1 + 1, p2);
                    elem =  elem.substring(0, p1);
                    StringTokenizer st2 = new StringTokenizer(expr, "|");
                    while (st2.hasMoreTokens()) {
                        String atts = st2.nextToken();
                        List<AttribConf> andList = new ArrayList<AttribConf>();
                        StringTokenizer st3 = new StringTokenizer(atts, "+");
                        while (st3.hasMoreTokens()) {
                            String att = st3.nextToken();
                            String defValue = null;
                            int p3 = att.indexOf('=');
                            if (p3 > 0) {
                                defValue = att.substring(p3 + 1);
                                att = att.substring(0, p3);
                                if (defValue.startsWith("\"") && defValue.endsWith("\"")) { 
                                    defValue = defValue.substring(1, defValue.length() - 1);
                                }
                                defValue = resolveConfigEscapes(defValue);
                            }
                            boolean isForbidden = att.startsWith("!");
                            if (isForbidden) {
                                att = att.substring(1);
                            }
                            andList.add(new AttribConf(att, defValue, isForbidden));
                        }
                        orList.add(andList);
                    }
                }
            }
            // elem contains one element name or several element names 
            // separated by slash character (/).
            elem = elem.toLowerCase();
            if (elem.contains("/")) {
                // Element names separated by slash character (/) share the 
                // same attribute conditions.   
                StringTokenizer elemNames = new StringTokenizer(elem, "/");
                while (elemNames.hasMoreTokens()) {
                    String ename = elemNames.nextToken().trim();
                    configElems.add(ename);
                    elemsAttribRequired.put(ename, orList);
                }
            } else {
                // Only one element name.
                configElems.add(elem);
                elemsAttribRequired.put(elem, orList);
            }
        }
    }
    
    private void readNormalizeTextAlignConfig(String config)
    {
        if (config.equals(""))  {
            config = "*";
        }
        StringTokenizer st = new StringTokenizer(config, ",");
        while (st.hasMoreTokens()) {
            String nm = st.nextToken();
            if (! nm.equals("*")) {
                configElems.add(nm);
            }
            elemsNormalizeTextalign.add(nm);
        }
    }
    
    private static class AttribConf
    {
        private final String name;
        private final String defaultValue;
        private final boolean forbidden;
        
        AttribConf(String attName, String defVal, boolean forbid) 
        {
            this.name = attName;
            this.defaultValue = defVal;
            this.forbidden = forbid;
        }
        
        String getName()
        {
            return name;
        }
        
        String getDefaultValue()
        {
            return defaultValue;
        }
        
        boolean isForbidden()
        {
            return forbidden;
        }
    }
    
    private static class StyleInfo
    {
        private final String cssName;
        private final Style style;
        private final boolean predefined;
        private int count = 0;
        
        StyleInfo(String cssName, Style style)
        {
            this.cssName = cssName;
            this.style = style;
            this.predefined = DocmaStyleUtil.isPredefinedStyle(cssName) || 
                              cssName.startsWith("print_width_") || 
                              cssName.startsWith("print_height_");
        }
        
        String getCSSName()
        {
            return cssName;
        }
        
        Style getStyle()
        {
            return style;
        }
        
        boolean styleExists()
        {
            return (style != null);
        }
        
        boolean isPredefined()
        {
            return predefined;
        }
        
        int getCount() 
        {
            return count;
        }
        
        void increaseCount()
        {
            count++;
        }
    }
}
