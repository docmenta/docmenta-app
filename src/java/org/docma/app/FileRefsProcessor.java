/*
 * FileRefsProcessor.java
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

import java.util.HashSet;
import java.util.Set;
import org.docma.coreapi.ExportLog;
import org.docma.util.XMLElementContext;
import org.docma.util.XMLElementHandler;
import org.docma.util.XMLProcessor;
import org.docma.util.XMLProcessorFactory;

/**
 *
 * @author MP
 */
public class FileRefsProcessor implements XMLElementHandler
{
    public final static String IMAGE_PREFIX = "image/";
    public final static String FILE_PREFIX = "file/";
    
    private final String html;
    private final FileURLTransformer fileTransformer;
    private final ImageURLTransformer imgTransformer;
    private final ExportLog exportLog;

    // Following fields are only used for processing of complete publication.
    private final DocmaExportContext exportCtx;
    private final Set aliasSet;   // all aliases in complete publication
    private final boolean removeFileLinks;
    private final String linkTarget;  // target attribute to be added to links


    private FileRefsProcessor(String html,
                              FileURLTransformer fileTransformer,
                              ImageURLTransformer imgTransformer,
                              ExportLog log)
    {
        this.html = html;
        this.fileTransformer = fileTransformer;
        this.imgTransformer = imgTransformer;
        this.exportLog = log;
        this.exportCtx = null;
        this.aliasSet = null;
        this.removeFileLinks = false;
        this.linkTarget = null;
    }


    private FileRefsProcessor(String html,
                              FileURLTransformer fileTransformer,
                              ImageURLTransformer imgTransformer,
                              DocmaExportContext expCtx, 
                              Set aliasSet, 
                              boolean removeFileLinks, 
                              String linkTarget)
    {
        this.html = html;
        this.fileTransformer = fileTransformer;
        this.imgTransformer = imgTransformer;
        this.exportLog = (expCtx != null) ? expCtx.getExportLog() : null;
        this.exportCtx = expCtx;
        this.aliasSet = aliasSet;
        this.removeFileLinks = removeFileLinks;
        this.linkTarget = linkTarget;
    }
    
    public static void process(String html,
                               StringBuilder out, 
                               FileURLTransformer fileTransformer,
                               ImageURLTransformer imgTransformer,
                               ExportLog log)
    {
        try {
            XMLProcessor xmlproc = XMLProcessorFactory.newInstance();
            xmlproc.setCheckWellformed(false);
            xmlproc.setIgnoreElementCase(true);
            xmlproc.setIgnoreAttributeCase(true);
            
            // Process all elements (this includes a, img, video, audio, source, track)
            xmlproc.setElementHandler(new FileRefsProcessor(html, fileTransformer, imgTransformer, log));
            xmlproc.process(html, out);
        } catch (Exception ex) {
            if (log != null) {
                log.errorMsg(ex.getMessage());
            }
        }
    }

    public static void processPublication(String html,
                                          StringBuilder out, 
                                          FileURLTransformer fileTransformer,
                                          ImageURLTransformer imgTransformer,
                                          DocmaExportContext expCtx, 
                                          boolean removeFileLinks)
    {
        try {
            HashSet aliases = new HashSet();  // new HashSet((3 * id_set.size()) + 50);
            ContentUtil.getIdValues(html, aliases);
            if (DocmaConstants.DEBUG) {
                System.out.println("ID value count: " + aliases.size());
            }
            
            String aTarget = null;  // currently not used!

            XMLProcessor xmlproc = XMLProcessorFactory.newInstance();
            xmlproc.setCheckWellformed(false);
            xmlproc.setIgnoreElementCase(true);
            xmlproc.setIgnoreAttributeCase(true);
            
            // Process all elements (this includes a, img, video, audio, source, track)
            xmlproc.setElementHandler(new FileRefsProcessor(html, 
                                                            fileTransformer, 
                                                            imgTransformer, 
                                                            expCtx, 
                                                            aliases, 
                                                            removeFileLinks, 
                                                            aTarget));
            xmlproc.process(html, out);
        } catch (Exception ex) {
            if (expCtx != null) {
                ExportLog log = expCtx.getExportLog();
                if (log != null) {
                    log.errorMsg(ex.getMessage());
                }
            }
        }
    }


    public void processElement(XMLElementContext ctx) 
    {
        String ename = ctx.getElementName().toLowerCase();
        
        if (ename.equals("a")) {  // && export_ctx != null
            prepareLink(ctx);  // process links with href attribute
        }
        
        // Process the src attribute of img, video, audio, source, track and 
        // any other element that has a src attribute.
        processFileRefAttribute("src", ctx);
        
        // Process the poster attribute of the video element
        if (ename.equals("video")) {
            processFileRefAttribute("poster", ctx);
        }
    }

    private void prepareLink(XMLElementContext ctx)
    {
        String href_val = ctx.getAttributeValue("href");
        if (href_val == null) {
            return;
        }
        boolean isContentLink = href_val.startsWith("#");
        boolean isImageLink = href_val.startsWith(IMAGE_PREFIX);
        boolean isFileLink = href_val.startsWith(FILE_PREFIX);

        if (! (isContentLink || isImageLink || isFileLink)) { 
            return;  // do not modify external links and other unknown links
        }
        
        String target_alias = null;
        if (isContentLink) target_alias = href_val.substring(1).trim();
        else if (isImageLink) target_alias = href_val.substring(IMAGE_PREFIX.length()).trim();
        else if (isFileLink) target_alias = href_val.substring(FILE_PREFIX.length()).trim();
        
        int tag_start = ctx.getCharacterOffset();
        String elem = ctx.getElement();
        int after_link = tag_start + elem.length();
        
        if (target_alias.equals("")) {
            LogUtil.addWarning(exportLog, html, tag_start, after_link, 
                "publication.export.missing_target_alias", href_val);
            return;
        }
        
        String link_text = ctx.getElementContent();
        if (isContentLink) {
            // 1) target alias exists as ID in html_in: do not modify link
            // 2) target alias does not exist: do not modify link, write warning
            // 3) target alias exists but is not included in publiation: 
            // 3.1) target alias is in referenced publication: remove link, replace link text
            // 3.2) target alias is not(!) in referenced publication: remove link, write warning
            if ((exportCtx != null) &&
                (aliasSet != null) &&
                ! aliasSet.contains(target_alias)) {
                DocmaSession docmaSess = exportCtx.getDocmaSession();
                if (docmaSess.getNodeIdByAlias(target_alias) == null) {
                    LogUtil.addWarning(exportLog, html, tag_start, after_link, 
                        "publication.export.cannot_resolve_target_alias", target_alias);
                } else {
                    String ref_title = exportCtx.getNodeTitleInReferencedPub(target_alias);
                    if (ref_title != null) {
                        link_text = ref_title;
                    } else {
                        LogUtil.addWarning(exportLog, html, tag_start, after_link, 
                            "publication.export.target_not_included_replacing_txt",
                            target_alias, escapeTags(link_text));
                    }
                    // Remove the opening tag <a ... > and the closing tag </a>
                    ctx.replaceElement(link_text);
                }
            }
        } else {  // isImageLink or isFileLink
            // 1) if removeFileLinks is true: remove link (e.g. for print output)
            // 2) otherwise if target exists transform link
            // 3) otherwise do not modify link, write warning
            String url = null;
            if (isImageLink  && (imgTransformer != null)) { 
                url = imgTransformer.getImageURLByAlias(target_alias);
            }
            if (isFileLink  && (fileTransformer != null)) { 
                url = fileTransformer.getFileURLByAlias(target_alias);
            }

            if (removeFileLinks) {  // Currently: is only true for print-output
                LogUtil.addInfo(exportLog, html, tag_start, after_link, 
                                "publication.export.removing_file_link", href_val);
                String title_val = ctx.getAttributeValue("title");
                // If link has title, then replace link text with link title
                if ((title_val != null) && (title_val.trim().length() > 0)) {
                    if ((url != null) && 
                        (title_val.contains("%target%") || 
                         title_val.contains("%target_print%"))) {
                        // If "Use target title" option is set, then use filename as title
                        int fn_start = url.lastIndexOf('/');
                        String fn = (fn_start < 0) ? url : url.substring(fn_start + 1);
                        link_text = (fn.trim().length() > 0) ? fn : url;
                    } else {
                        link_text = title_val;
                    }
                }
                ctx.replaceElement(link_text);
            } else {
                if (url != null) {  // target_alias exists
                    if ((linkTarget != null) && (ctx.getAttributeValue("target") == null)) {
                        ctx.setAttribute("target", linkTarget);
                    }
                    ctx.setAttribute("href", url);
                }
            }
        }  // end of isImageLink or isFileLink
    }

    
    private void processFileRefAttribute(String attName, XMLElementContext ctx)
    {
        String value = ctx.getAttributeValue(attName);
        if (value == null) {
            return;
        }
        value = value.trim();
        boolean isImage = value.startsWith(IMAGE_PREFIX);
        boolean isFile = value.startsWith(FILE_PREFIX);
        String alias = null;
        if (isImage) { 
            alias = value.substring(IMAGE_PREFIX.length()).trim();
        } else if (isFile) {
            alias = value.substring(FILE_PREFIX.length()).trim();
        }
        if (alias == null) {  // do not process attribute if it's not a valid file or image reference
            return;
        }
        if (alias.equals("")) {
            LogUtil.addWarning(exportLog, html, ctx.getCharacterOffset(), -1, 
                               "publication.export.missing_target_alias", value);
            return;
        }
        String url = null;
        if (isImage && (imgTransformer != null)) {
            url = imgTransformer.getImageURLByAlias(alias);
        }
        if (isFile && (fileTransformer != null)) { 
            url = fileTransformer.getFileURLByAlias(alias);
        }
        if (url != null) {  // alias exists
            ctx.setAttribute(attName, url);
            // Note: If url is null, then error message has already been written
            // to the log by imgTransformer/fileTransformer
        }
    }

    private static String escapeTags(String str)
    {
        return str.replace("<", "&lt;").replace(">", "&gt;");
    }

//    private int getElementEndPos(int startPos)
//    {
//        int endPos = startPos + 1;
//        int maxPos = Math.min(html.length(), startPos + 40);
//        while (endPos < maxPos) {
//            if (html.charAt(endPos) == '>') break;
//        }
//        return endPos;
//    }
            
}
