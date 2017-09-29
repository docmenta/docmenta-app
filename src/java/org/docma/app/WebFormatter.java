/*
 * WebFormatter.java
 * 
 *  Copyright (C) 2015  Manfred Paula, http://www.docmenta.org
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

import java.io.*;
import java.util.*;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import javax.xml.namespace.QName;

import org.docma.coreapi.ExportLog;
import org.docma.util.CSSParser;
import org.docma.util.DocmaUtil;
import org.docma.util.XMLParser;
import org.docma.util.Log;


/**
 *
 * @author MP
 */
public class WebFormatter 
{
    public final static String DOCTYPE_INTRO_DEFAULT = 
        "<?xml version=\"1.0\" encoding=\"###encoding###\" standalone=\"no\"?>\n" + 
        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">";

    private final static String COMMON_RELATIVE_URL = "common";  // relative path from content to common
    
    private final static String ROOT_CHUNK_FILENAME = "index.html";
    private final static String TOC_CHUNK_FILENAME = "toc-01.html";
    private final static String INDEX_CHUNK_FILENAME = "ix-01.html";
    private final static String FORMAL_PREFIX = "_formal_";
    private final static String COMPONENT_TOC_PLACEHOLDER = "__component_toc__";
    private final static String FOOT_ID_PREFIX = "ftn.";
    
    private final File l10nDir;
    private final File tempDir;
    
    private final XMLInputFactory xinFactory;
    private final XMLOutputFactory xoutFactory;
    private final XMLEventFactory eventFactory;
    
    private final Map<String, Properties> defaultGenTextProps = new HashMap<String, Properties>();
    
    private TemplateReader template = null;
    private Map<String, TemplateReader> templateVariants = null;


    public WebFormatter(File configDir, File docbookXSLDir, File tempDir) 
    throws Exception
    {
        this.l10nDir = new File(docbookXSLDir, "common");
        this.tempDir = tempDir;
        xinFactory = XMLInputFactory.newInstance();
        xoutFactory = XMLOutputFactory.newInstance();
        eventFactory = XMLEventFactory.newInstance();
        if (DocmaConstants.DEBUG) {
            Log.info("STAX FACTORY IN:  " + ((xinFactory == null) ? "null" : xinFactory.getClass().getName()));
            Log.info("STAX FACTORY OUT: " + ((xoutFactory == null) ? "null" : xoutFactory.getClass().getName()));
        }
        try {
            xinFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
        } catch (Exception ex) {  // illegal argument exception
            Log.error("Property not supported: " + XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES);
        }
        
        readTemplates(configDir);
    }
    
    private void readTemplates(File configDir) throws Exception
    {
        final String templateName = "web_v2.tmpl";
        final String templatePrefix = "web_v2-";
        
        // Read default template
        template = new TemplateReader(new File(configDir, templateName), "UTF-8");
        
        // Read variant templates
        File[] arr = configDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name)
            {
                return name.startsWith(templatePrefix) && name.endsWith(".tmpl");
            }
        });
        if (arr != null) {
            for (File f : arr) {
                String fn = f.getName();
                String variant = fn.substring(templatePrefix.length(), fn.lastIndexOf('.'));
                if (templateVariants == null) {
                    templateVariants = new HashMap<String, TemplateReader>();
                }
                templateVariants.put(variant, new TemplateReader(f, "UTF-8"));
            }
        }
    }
    
    private TemplateReader getTemplateReader(String variant) 
    {
        if ((variant == null) || variant.equals("") || (templateVariants == null)) {
            return template;
        } else {
            TemplateReader variantTemplate = templateVariants.get(variant);
            return (variantTemplate != null) ? variantTemplate : template;
        }
    }

    public void format(String htmlPrintInstance,
                       File outputDir,
                       DocmaPublicationConfig pub_config,
                       DocmaOutputConfig out_config,
                       ExportLog export_log)
    throws Exception
    {
        if (out_config == null) {
            out_config = new DocmaOutputConfig("_default_html_output_");
            out_config.setFormat("html");
            out_config.setHtmlSeparateFileLevel(1);
            out_config.setPartToc(true);
            out_config.setChapterToc(true);
            out_config.setIndex(true);
        }
        ParserTocEntry tocRoot = new ParserTocEntry();
        List<WebChunk> chunks = new ArrayList<WebChunk>();
        Map<String, String> infoElems = new HashMap<String, String>();
        
        parse_1st(htmlPrintInstance, out_config, tocRoot, chunks, infoElems, export_log);
        // String infoDiv = infoDivWriter.toString().trim();

        // Transform <br></br> to <br />, because otherwise browsers display two line-breaks
        for (WebChunk chunk : chunks) {
            fixEmptyElements(chunk.getContent());
        }

        // Create index
        WebChunk indexChunk = null;
        if (out_config.isIndex()) {
            indexChunk = new WebChunk(INDEX_CHUNK_FILENAME);
            chunks.add(indexChunk);
            
            ParserTocEntry indexEntry = new ParserTocEntry(ParserTocEntry.TYPE_INDEX);
            indexEntry.setTitle(getGenText("Index", out_config, "Index"));
            indexEntry.setChunk(indexChunk);
            tocRoot.addSubEntry(indexEntry);
            writeIndex(indexChunk.getWriter(), chunks, out_config, export_log);
        }
        
        // try {
        String pubTitle = infoElems.get("doc-title"); // extractPublicationTitle(infoDiv);
        if (pubTitle == null) {
            pubTitle = "";
        }
        // } catch (Exception ex) {
        //     pubTitle = "";
        //     export_log.warningMsg("Failed to read publication title from info element: " + ex.getMessage());
        //     ex.printStackTrace();
        // }
        tocRoot.setTitleIfNotExists(pubTitle);
        
        if (DocmaConstants.DEBUG) {
            System.out.println("------ Info: ------");
            System.out.println("Publication Title: " + pubTitle);
            System.out.println("------ ToC: ------");
            printToc(tocRoot, "");
            System.out.println("-----------------");
        }
        
        // Transform links and insert component ToCs
        Map<String, ParserTocEntry> mapAliasToEntry = new HashMap<String, ParserTocEntry>();
        createAliasToTocEntryMap(tocRoot, mapAliasToEntry); 
        Map<String, WebChunk> mapFootnoteToChunk = new HashMap<String, WebChunk>();
        createFootnoteToChunkMap(chunks, mapFootnoteToChunk);
        for (WebChunk chunk : chunks) {
            StringBuffer cont = chunk.getContent();
            transformLinks(cont, mapAliasToEntry, out_config, export_log);
            insertComponentToCs(cont, mapAliasToEntry, out_config);
            appendFootnotes(cont, chunk, out_config, mapFootnoteToChunk, export_log);
        }
        
        // Prepare custom header and footer
        String header = out_config.getHtmlCustomHeaderContent();
        String footer = out_config.getHtmlCustomFooterContent();
        if (header != null) {
            StringBuffer head_buf = new StringBuffer(header);
            transformLinks(head_buf, mapAliasToEntry, out_config, export_log);
            header = head_buf.toString();
        }
        if (footer != null) {
            StringBuffer foot_buf = new StringBuffer(footer);
            transformLinks(foot_buf, mapAliasToEntry, out_config, export_log);
            footer = foot_buf.toString();
        }
        if (! outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        // Insert title page and table of contents
        updateRootChunk(chunks.get(0), tocRoot, infoElems, out_config, mapAliasToEntry, export_log);
        WebChunk tocChunk = null;
        if (out_config.isHtmlSeparateTOC()) {
            tocChunk = new WebChunk(TOC_CHUNK_FILENAME);
            tocChunk.setTocEntry(tocRoot);
            writeSeparateToc(tocChunk.getWriter(), tocRoot, pubTitle, out_config);
            chunks.add(tocChunk);
        }
        
        // Writes chunks to filesystem
        for (int i=0; i < chunks.size(); i++) {
            WebChunk chunk = chunks.get(i);
            WebChunk prev = (i > 0) ? chunks.get(i - 1) : null;
            WebChunk next = (i < (chunks.size() - 1)) ? chunks.get(i + 1) : null;
            writeChunk(outputDir, out_config, chunk, 
                       prev, next, chunks.get(0), tocChunk, indexChunk,
                       header, footer, tocRoot, infoElems);
        }
    }
    
    public static void main(String[] args) throws Exception
    {
        File dir = new File("c:\\TEMP");
        WebFormatter formatter = new WebFormatter(dir, dir, dir);
        String in = DocmaUtil.readFileToString(new File("c:\\TEMP\\printinstance.html"));
        File out_dir = new File(dir, "web_output_" + System.currentTimeMillis());
        formatter.format(in, out_dir, null, null, null);
    }
    
    private void fixEmptyElements(StringBuffer xhtml) 
    {
        // Transform <br ...></br>  to <br .../>
        fix_EmptyElement(xhtml, "br");
        fix_EmptyElement(xhtml, "BR");
        fix_EmptyElement(xhtml, "img");
        fix_EmptyElement(xhtml, "IMG");
    }
    
    private void fix_EmptyElement(StringBuffer xhtml, String element_name) 
    {
        int startpos = 0;  // continue search for next link tag at startpos
        while (startpos < xhtml.length()) {
            final String END_PATTERN = "></" + element_name + ">";
            int br_end = xhtml.indexOf(END_PATTERN, startpos);
            if (br_end < 0) break;

            int after_br = br_end + END_PATTERN.length();
            startpos = after_br;
            int br_start = xhtml.lastIndexOf("<", br_end);
            if (br_start < 0) continue;
            
            if (element_name.equals(xhtml.substring(br_start + 1, br_start + 1 + element_name.length()))) {
                int last_char = after_br - 2;
                for (int i = br_end; i < last_char; i++) { 
                    xhtml.setCharAt(i, ' ');
                }
                xhtml.setCharAt(last_char, '/');
            }
        }
    }

//    private String extractPublicationTitle(String infoDiv) throws Exception
//    {
//        // infoDiv = infoDiv.trim();
//        if (infoDiv.length() == 0) {
//            return "";
//        }
//        final QName ATT_NAME_CLASS = new QName("class");
//        XMLEventReader xr = xinFactory.createXMLEventReader(new StringReader(infoDiv));
//        String title = null;
//        while ((title == null) && xr.hasNext()) {
//            XMLEvent e = xr.nextEvent();
//            switch (e.getEventType()) {
//                case XMLEvent.START_ELEMENT: 
//                    StartElement se = e.asStartElement();
//                    String ename = se.getName().getLocalPart().toLowerCase();
//                    if (ename.equals("div")) {
//                        Attribute att = se.getAttributeByName(ATT_NAME_CLASS);
//                        if ((att != null) && "doc-title".equals(att.getValue())) {
//                            title = readText(se, xr);
//                        }
//                    }
//                    break;
//            }
//        }
//        return (title == null) ? "" : title;
//    }
    
    private void writeSeparateToc(Appendable out, 
                                  ParserTocEntry tocRoot, 
                                  String pubTitle, 
                                  DocmaOutputConfig outConfig) throws Exception
    {
        if ((pubTitle != null) && !pubTitle.equals("")) {
            out.append("<h1>").append(pubTitle).append("</h1>");
        }
        writeToc(out, tocRoot, outConfig);
    }
    
    private void updateRootChunk(WebChunk rootChunk, 
                                 ParserTocEntry tocRoot, 
                                 Map<String, String> infoElements,
                                 DocmaOutputConfig outConfig,
                                 Map<String, ParserTocEntry> mapAliasToEntry,
                                 ExportLog exportLog) throws Exception
    {
        boolean toc_in_titlepage = outConfig.isToc() && (! outConfig.isHtmlSeparateTOC());
        StringBuilder intro = new StringBuilder();
        intro.append("<div class=\"titlepage\">");
        writeTitlePage1(intro, infoElements, outConfig, mapAliasToEntry, exportLog);
        if (toc_in_titlepage) {
            intro.append("<hr/>");
        }
        intro.append("</div>");
        if (toc_in_titlepage) {
            writeToc(intro, tocRoot, outConfig);
        }
        writeTitlePage2(intro, infoElements, outConfig, mapAliasToEntry, exportLog);
        
        StringBuffer buf = rootChunk.getContent();
        int offset = 0;
        int end_offset = buf.length();
        while (offset < buf.length() && Character.isWhitespace(buf.charAt(offset))) {
            offset++;
        }
        final String XML_PROLOG = "<?xml";
        if (XML_PROLOG.equals(buf.substring(offset, offset + XML_PROLOG.length()))) {
            int pend = buf.indexOf("?>");
            if (pend > 0) offset = pend + 2;
        }

        // extract content of body element
        int p1 = buf.indexOf("<body", offset);
        if (p1 >= 0) {
            int p2 = buf.indexOf(">", p1);
            if (p2 >= 0) {
                int p3 = buf.lastIndexOf("</body>");
                if (p3 >= 0) {  
                    offset = p2 + 1;   // start after body
                    end_offset = p3;   // up to end tag
                }
            }
        }
        
        if (offset > 0) {
            if (end_offset < buf.length()) {
                buf.setLength(end_offset); // buf.delete(end_offset, buf.length());
            }
            buf.replace(0, offset, intro.toString());
        } else {
            buf.insert(0, intro);
        }
    }
    
    private void writeTitlePage1(Appendable out,  
                                 Map<String, String> infoElements, 
                                 DocmaOutputConfig outConfig,
                                 Map<String, ParserTocEntry> mapAliasToEntry, 
                                 ExportLog exportLog) throws Exception
    {
        out.append("<div>");
        String page = infoElements.get("doc-titlepage1");
        if ((page != null) && (page.length() > 0)) {
            StringBuffer page_buf = new StringBuffer(page);
            fixEmptyElements(page_buf);
            transformLinks(page_buf, mapAliasToEntry, outConfig, exportLog);
            out.append(page_buf);
        } else {
            //
            // Default title page (show publication title as h1 header)
            //
            String pubTitle = infoElements.get("doc-title"); // extractPublicationTitle(infoDiv);
            String subTitle = infoElements.get("doc-subtitle");
            String corporate = infoElements.get("doc-corpauthor");
            String releaseInfo = infoElements.get("doc-releaseinfo");
            String pubdate = infoElements.get("doc-pubdate");
            String publisher = infoElements.get("doc-publisher");
            String biblioId = infoElements.get("doc-biblioid");
            String authorgrp = infoElements.get("doc-authorgroup");
            String copyright = infoElements.get("doc-copyright");
            String credits = infoElements.get("doc-credit");
            String pubAbstract = infoElements.get("doc-abstract");
            String legal = infoElements.get("doc-legalnotice");
            if (pubTitle == null) {
                pubTitle = "";
            }
            out.append("<h1 class=\"title\">").append(pubTitle).append("</h1>");
            if ((subTitle != null) && (subTitle.length() > 0)) {
                out.append("<h2 class=\"subtitle\">").append(subTitle).append("</h2>");
            }
            if ((corporate != null) && (corporate.length() > 0)) {
                out.append("<div class=\"corpauthor\">").append(corporate).append("</div>");
            }
            if ((authorgrp != null) && (authorgrp.length() > 0)) {
                List<String> authors = extractDivContents(authorgrp);
                if (authors.size() > 0) {
                    out.append("<div class=\"authorgroup\">");
                    for (String a : authors) {
                        out.append("<div class=\"author\">").append(a).append("</div>");
                    }
                    out.append("</div>");
                }
            }
            if ((credits != null) && (credits.length() > 0)) {
                List<String> cred_list = extractDivContents(credits);
                if (cred_list.size() > 0) {
                    out.append("<div class=\"othercredit\">");
                    for (String cred : cred_list) {
                        out.append("<div>").append(cred).append("</div>");
                    }
                    out.append("</div>");
                }
            }
            if ((releaseInfo != null) && (releaseInfo.length() > 0)) {
                out.append("<div class=\"releaseinfo\">").append(releaseInfo).append("</div>");
            }
            if ((biblioId != null) && (biblioId.length() > 0)) {
                out.append("<div class=\"biblioid\">").append(biblioId).append("</div>");
            }
            if ((copyright != null) && (copyright.length() > 0)) {
                // Extract year and copyright-holder. This is a quick and dirty implementation.
                int start = 0;
                String year = "";
                String holder = "";
                while (start < copyright.length()) {
                    int p1 = copyright.indexOf("<div", start);
                    if (p1 < 0) break;
                    int p2 = copyright.indexOf(">", p1);
                    if (p2 < 0) break;
                    int p3 = copyright.indexOf("</div>", p2);
                    if (p3 < 0) break;
                    String attribs = copyright.substring(p1, p2);
                    String value = copyright.substring(p2 + 1, p3).trim();
                    if (attribs.contains("doc-year")) {
                        year = value;
                    } else if (attribs.contains("doc-holder")) {
                        holder = value;
                    }
                    start = p3;
                }
                boolean has_year = year.length() > 0;
                boolean has_holder = holder.length() > 0;
                if (has_year || has_holder) {
                    out.append("<div class=\"copyright\">");
                    out.append(getGenText("Copyright", outConfig, "Copyright"));
                    out.append(" &copy; ");
                    if (has_year) {
                        out.append(year);
                    }
                    if (has_holder) {
                        if (has_year) out.append(" ");
                        out.append(holder);
                    }
                    out.append("</div>");
                }
            }
            if ((legal != null) && (legal.length() > 0)) {
                StringBuffer legal_buf = new StringBuffer(legal);
                transformLinks(legal_buf, mapAliasToEntry, outConfig, exportLog);
                String legal_title = getGenText("LegalNotice", outConfig, "");
                legal_title = legal_title.replace('"', '\'');
                out.append("<div class=\"legalnotice\""); 
                if (legal_title.length() > 0) {
                    out.append(" title=\"").append(legal_title).append("\"");
                }
                out.append(">").append(legal_buf).append("</div>");
            }
            if ((publisher != null) && (publisher.length() > 0)) {
                out.append("<div class=\"publisher\">").append(publisher).append("</div>");
            }
            if ((pubdate != null) && (pubdate.length() > 0)) {
                out.append("<div class=\"pubdate\">").append(pubdate).append("</div>");
            }
            if ((pubAbstract != null) && (pubAbstract.length() > 0)) {
                StringBuffer abstr_buf = new StringBuffer(pubAbstract);
                transformLinks(abstr_buf, mapAliasToEntry, outConfig, exportLog);
                String abstr_title = getGenText("Abstract", outConfig, "");
                if (abstr_title.length() > 0) {
                    out.append("<div class=\"abstract\" title=\""); 
                    out.append(abstr_title.replace('"', '\'')).append("\">");
                    out.append("<p class=\"title\">").append(abstr_title).append("</p>");
                } else {
                    out.append("<div class=\"abstract\">");
                }
                out.append(abstr_buf).append("</div>");
            }
        }
        out.append("</div>");
    }

    private void writeTitlePage2(Appendable out,  
                                 Map<String, String> infoElements, 
                                 DocmaOutputConfig outConfig,
                                 Map<String, ParserTocEntry> mapAliasToEntry, 
                                 ExportLog exportLog) throws Exception
    {
        String page = infoElements.get("doc-titlepage2");
        if ((page != null) && (page.length() > 0)) {
            out.append("<div class=\"titlepage2\">");
            boolean toc_in_titlepage = outConfig.isToc() && (! outConfig.isHtmlSeparateTOC());
            if (toc_in_titlepage) {
                out.append("<hr/>");
            }
            StringBuffer page_buf = new StringBuffer(page);
            fixEmptyElements(page_buf);
            transformLinks(page_buf, mapAliasToEntry, outConfig, exportLog);
            out.append("<div>").append(page_buf).append("</div>");
            out.append("</div>");
        }
    }

    private List<String> extractDivContents(String html)
    {
        // Note that this implementation can only be used if div elements 
        // are not nested!
        List<String> res = new ArrayList<String>();
        int start = 0;
        while (start < html.length()) {
            int p1 = html.indexOf("<div", start);
            if (p1 < 0) break;
            int p2 = html.indexOf(">", p1);
            if (p2 < 0) break;
            int p3 = html.indexOf("</div>", p2);
            if (p3 < 0) break;
            res.add(html.substring(p2 + 1, p3));
            start = p3;
        }
        return res;
    }

    private void writeToc(Appendable out, ParserTocEntry tocRoot, DocmaOutputConfig outConfig) throws Exception
    {
        if (tocRoot.getSubEntryCount() > 0) {
            String toc_title = getGenText("TableofContents", outConfig);
            if ((toc_title == null) || toc_title.equals("")) {
                toc_title = "Table of Contents";
            }
            out.append("<div class=\"toc\">");
            out.append("<p><strong>").append(toc_title).append("</strong></p>");
            writeTocRecursive(out, tocRoot, outConfig, 1, (tocRoot.isSection() ? 1 : 0));
            out.append("</div>\n");
        }
    }

    private void writeTocRecursive(Appendable out, 
                                   ParserTocEntry parentEntry, 
                                   DocmaOutputConfig outConfig, 
                                   final int depth, 
                                   final int sect_depth) throws Exception
    {
        int cnt = parentEntry.getSubEntryCount();
        if (cnt > 0) {
            int max_sect_depth = outConfig.getSectionTocDepth();
            boolean component_toc = outConfig.isPartToc() || outConfig.isChapterToc() || outConfig.isSectionToc();
            boolean restrict_sect = component_toc && (max_sect_depth > 0);
            String toc_named = outConfig.getTocNamedLabels();
            toc_named = (toc_named == null) ? "" : toc_named.toLowerCase();
            out.append("<dl>");
            for (int i = 0; i < cnt; i++) {
                ParserTocEntry entry = parentEntry.getSubEntry(i);
                String etype = entry.getType();
                out.append("<dt>");
                if (etype.length() > 0) {
                    out.append("<span>");
                } else {
                    out.append("<span class=\"").append(etype).append("\">");
                }
                out.append("<a href=\"").append(entry.getLinkURL()).append("\">");
                if (toc_named.contains(etype)) {
                    String lab_name = getLabelName(etype, outConfig);
                    if (! lab_name.equals("")) {
                        out.append(lab_name).append(" ");
                    }
                }
                out.append(entry.getNumberedTitle());
                out.append("</a></span></dt>");
                if (entry.getSubEntryCount() > 0) {
                    int s_depth = entry.isSection() ? (sect_depth + 1) : 0;
                    if ((depth < outConfig.getMaxTocDepth()) && 
                        ((! restrict_sect) || (s_depth < max_sect_depth))) {
                        out.append("<dd>");
                        writeTocRecursive(out, entry, outConfig, depth + 1, s_depth);
                        out.append("</dd>");
                    }
                }
            }
            out.append("</dl>");
        }
    }

    private void createAliasToTocEntryMap(ParserTocEntry parentEntry, 
                                          Map<String, ParserTocEntry> map)
    {
        String alias = parentEntry.getAlias();
        if (alias != null) {
            if (! map.containsKey(alias)) {
                map.put(alias, parentEntry);
            }
        }
        for (int i=0; i< parentEntry.getSubEntryCount(); i++) {
            createAliasToTocEntryMap(parentEntry.getSubEntry(i), map);
        }
        for (int i=0; i< parentEntry.getTargetElementCount(); i++) {
            createAliasToTocEntryMap(parentEntry.getTargetElement(i), map);
        }
    }
    
    private void createFootnoteToChunkMap(List<WebChunk> chunks, 
                                          Map<String, WebChunk> map)
    {
        for (WebChunk chunk : chunks) {
            int cnt = chunk.getFootnoteCount();
            for (int i=0; i < cnt; i++) {
                map.put(chunk.getFootnote(i).getAlias(), chunk);
            }
        }
    }
    
    private void writeIndex(Appendable out, List<WebChunk> chunks, DocmaOutputConfig outConfig, ExportLog exportLog)
    throws Exception
    {
        int total_cnt = 0; 
        for (WebChunk ch : chunks) {
            total_cnt += ch.getIndexEntryCount();
        }
        List<ParserIndexEntry> entries = new ArrayList<ParserIndexEntry>(total_cnt);
        for (WebChunk ch : chunks) {
            for (int i = 0; i < ch.getIndexEntryCount(); i++) {
                entries.add(ch.getIndexEntry(i));
            }
        }
        String index_title = getGenText("Index", outConfig, "Index");
        String txt_see = getGenText("see", outConfig, "see");
        String txt_seealso = getGenText("seealso", outConfig, "see also");
        out.append("<div class=\"doc-index\" title=\"").append(index_title).append("\">");
        out.append("<h2>").append(index_title).append("</h2>");
        WebIndexWriter.writeIndex(out, entries, outConfig.getLanguageCode(), txt_see, txt_seealso, exportLog);
        out.append("</div>");
    }

    private void transformLinks(StringBuffer html_buf, 
                                Map<String, ParserTocEntry> mapAliasToUrl, 
                                DocmaOutputConfig outConfig,
                                ExportLog exportLog) throws Exception
    {
        // boolean has_log = (export_log != null);
        List<String> attNames = new ArrayList<String>();
        List<String> attValues = new ArrayList<String>();
        StringBuilder link_new = new StringBuilder();
        int startpos = 0;  // continue search for next link tag at startpos
        while (startpos < html_buf.length()) {
            final String A_PATTERN = "<a";
            final String END_PATTERN = "</a>";
            int tag_start = html_buf.indexOf(A_PATTERN, startpos);
            if (tag_start < 0) break;

            startpos = tag_start + 1;  // continue after this position
            int att_start = tag_start + A_PATTERN.length();
            if (! Character.isWhitespace(html_buf.charAt(att_start))) continue;

            int tag_end = XMLParser.parseTagAttributes(html_buf, att_start, attNames, attValues);
            if (tag_end < 0) continue;

            startpos = tag_end + 1;  // continue search after opening tag

            int href_idx = attNames.indexOf("href");
            if (href_idx < 0) continue;
            
            int a_end = html_buf.indexOf(END_PATTERN, tag_end);
            if (a_end < 0) break;
            
            String link_text = html_buf.substring(tag_end + 1, a_end);
            int after_link = a_end + END_PATTERN.length();
            startpos = after_link;  // continue search after closing tag

            String href_val = attValues.get(href_idx).trim();
            boolean isContentLink = href_val.startsWith("#");
            
            if (isContentLink) { 

                String target_alias = href_val.substring(1).trim();

                if (target_alias.equals("")) {
                    // if (has_log) {
                    //     export_log.warningMsg("Invalid link '" + href_val + "'. Missing target alias.");
                    // }
                    continue;
                }
                ParserTocEntry target = mapAliasToUrl.get(target_alias);
                if (target == null) {
                    continue;
                }

                int title_idx = attNames.indexOf("title");
                String title_val = (title_idx < 0) ? null : attValues.get(title_idx).trim();
                final String REPLACE_PATT = "%target%";
                boolean replace = (title_val != null) && title_val.startsWith(REPLACE_PATT);
                boolean replace_print = (title_val != null) && title_val.startsWith("%target_print%");

                attValues.set(href_idx, target.getLinkURL());  // replace href value

                if (replace || replace_print) {
                    attValues.set(title_idx, target.getNumberedTitle());
                }
                if (replace) {
                    String select = title_val.substring(REPLACE_PATT.length()).trim();
                    String label = null;
                    String title = null;
                    if (select.contains("labelname")) {
                        label = getLabelName(target.getType(), outConfig);
                    } else if (select.contains("labelnumber")) {
                        label = target.getLabelNumber();
                    } else if (select.contains("label")) {
                        label = (getLabelName(target.getType(), outConfig) + " " +
                                target.getLabelNumber()).trim();
                    }
                    if (select.contains("quotedtitle")) {
                        String startquote = getGenText("startquote", outConfig);
                        String endquote = getGenText("endquote", outConfig);
                        if ((startquote == null) || (endquote == null) || 
                             startquote.equals("") || endquote.equals("")) {
                            startquote = "\"";
                            endquote = "\"";
                        }
                        title = startquote + target.getTitle() + endquote;
                    } else if (select.contains("title")) {
                        title = target.getTitle();
                    }
                    String sep = getGenText("xref|label-title-separator", outConfig);
                    if ((sep == null) || sep.equals("")) {
                        sep = ": ";
                    }
                    if ((label == null) && (title == null)) {
                        label = target.getLabelNumber();
                        title = target.getTitle();
                        String gen_key = (label.equals("") ? "xref|" : "xref-number-and-title|") + target.getType();
                        String pattern = getGenText(gen_key, outConfig);
                        if ((pattern != null) && (pattern.contains("%t") || pattern.contains("%n"))) {
                            link_text = pattern.replace("%n", label).replace("%t", title);
                        } else {
                            link_text = label.equals("") ? title : (label + sep + title);
                        }
                    } else {
                        StringBuilder txt = new StringBuilder();
                        if ((label != null) && (label.length() > 0)) {
                            txt.append(label);
                        }
                        if ((title != null) && (title.length() > 0)) {
                            if (txt.length() > 0) txt.append(sep);
                            txt.append(title);
                        }
                        if (txt.length() > 0) {
                            link_text = txt.toString();
                        }
                    }
                }
            } else {   // external link or file/image link
                // Set target attribte for external/file/image links
                int target_idx = attNames.indexOf("target");
                if (target_idx >= 0) {
                    continue;   // target attribute has been set by user; do not modify link;
                }
                // Add target attribute to link
                String window_name = outConfig.getHtmlURLTargetWindow();
                if ((window_name == null) || window_name.equals("")) {
                    window_name = DocmaConstants.DEFAULT_URL_TARGET_WINDOW;
                }
                attNames.add("target");
                attValues.add(window_name);
            }
            
            // Replace old <a ..>...</a> with new tag
            link_new.setLength(0);  // clear buffer
            link_new.append("<a");
            for (int i = 0; i < attNames.size(); i++) {
                link_new.append(" ");
                link_new.append(attNames.get(i)).append("=\"");
                link_new.append(attValues.get(i).replace('"', '\''));
                link_new.append("\"");
            }
            link_new.append(">").append(link_text).append("</a>");

            html_buf.replace(tag_start, after_link, link_new.toString());
            startpos = tag_start + link_new.length();
        }  // end of while loop
    }
    
    private void insertComponentToCs(StringBuffer html_buf, 
                                     Map<String, ParserTocEntry> mapAliasToEntry, 
                                     DocmaOutputConfig outConfig) throws Exception
    {
        final String COMMENT_START = "<!--";
        final String COMMENT_END = "-->";
        int startpos = 0;
        while (startpos < html_buf.length()) {
            final int p1 = html_buf.indexOf(COMPONENT_TOC_PLACEHOLDER, startpos);
            if (p1 < 0) break;
            
            startpos = p1 + 1;
            
            // skip whitespace
            int c_start = p1 - 1;
            while (c_start >= 0) {
                if (! Character.isWhitespace(html_buf.charAt(c_start))) {
                    break;
                }
                c_start--;
            }
             
            c_start = c_start - COMMENT_START.length() + 1; // Start of comment
            if ((c_start >= 0) && COMMENT_START.equals(html_buf.substring(c_start, c_start + COMMENT_START.length()))) {
                int c_end = html_buf.indexOf(COMMENT_END, p1); // End of comment
                if (c_end > 0) {  // found valid component ToC placeholder 
                    String root_alias = html_buf.substring(p1 + COMPONENT_TOC_PLACEHOLDER.length(), c_end).trim();
                    ParserTocEntry entry = mapAliasToEntry.get(root_alias);
                    if (entry != null) {
                        // Create component ToC
                        StringBuilder toc_buf = new StringBuilder();
                        writeToc(toc_buf, entry, outConfig);
                        // Replace placeholder by component ToC
                        html_buf.replace(c_start, c_end + COMMENT_END.length(), toc_buf.toString());
                        startpos = c_start + toc_buf.length();  // continue after inserted ToC
                        break;   // only insert one ToC per chunk
                    }
                }
            }
        }
    }
    
    private void writeChunk(File outputDir,
                            final DocmaOutputConfig outConfig,
                            WebChunk chunk, 
                            WebChunk prev,
                            WebChunk next,
                            WebChunk home, 
                            WebChunk separateToc,
                            WebChunk index,
                            String header, 
                            String footer, 
                            ParserTocEntry tocRoot, 
                            Map<String, String> infoElems) throws Exception
    {
        String encoding = outConfig.getHtmlOutputEncoding();
        if ((encoding == null) || (encoding.length() == 0)) {
            encoding = "UTF-8";
        }
        String docType = outConfig.getHtmlCustomDocType();
        if ((docType == null) || docType.equals("")) {
            docType = DOCTYPE_INTRO_DEFAULT;
        }
        docType = docType.replace("###encoding###", encoding);
        ParserTocEntry chunkEntry = chunk.getTocEntry();
        ParserTocEntry prevEntry = (prev == null) ? null : prev.getTocEntry();
        ParserTocEntry nextEntry = (next == null) ? null : next.getTocEntry();
        ParserTocEntry homeEntry = (home == null) ? null : home.getTocEntry();
        ParserTocEntry upEntry = chunkEntry.getParentEntry();
        while ((upEntry != null) && (upEntry.getChunk() == null)) {
            upEntry = upEntry.getParentEntry();
        }
        boolean show_up = (upEntry != null) && (upEntry != homeEntry);
        
        String custom_head_tags = outConfig.getHtmlCustomHeadTags();
        custom_head_tags = (custom_head_tags != null) ? custom_head_tags.trim() : "";
        
        String langCode = outConfig.getLanguageCode();
        if ((langCode == null) || langCode.equals("")) {
            langCode = "en";
        }
        
        Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("current_time_millis", Long.toString(System.currentTimeMillis()));
        placeholders.put("lang_code", langCode);
        placeholders.put("encoding", encoding);
        placeholders.put("doctype", docType);
        placeholders.put("custom_head_tags", custom_head_tags);
        placeholders.put("app_shortname", DocmaConstants.DISPLAY_APP_SHORTNAME);
        placeholders.put("app_version", DocmaConstants.DISPLAY_APP_VERSION);
        placeholders.put("chunk_title", chunkEntry.getNumberedTitle());
        if ((homeEntry != null) && (home != chunk)) {
            String url = homeEntry.getLinkURL();
            String ti = homeEntry.getNumberedTitle();
            placeholders.put("link_home", "<link rel=\"home\" href=\"" + url + "\" title=\"" + ti + "\"/>");
        }
        if (show_up) {
            String url = upEntry.getLinkURL();
            String ti = upEntry.getNumberedTitle();
            placeholders.put("link_up", "<link rel=\"up\" href=\"" + url + "\" title=\"" + ti + "\"/>");
        }
        if (prevEntry != null) {
            String url = prevEntry.getLinkURL();
            String ti = prevEntry.getNumberedTitle();
            placeholders.put("link_prev", "<link rel=\"prev\" href=\"" + url + "\" title=\"" + ti + "\"/>");
        }
        if (nextEntry != null) {
            String url = nextEntry.getLinkURL();
            String ti = nextEntry.getNumberedTitle();
            placeholders.put("link_next", "<link rel=\"next\" href=\"" + url + "\" title=\"" + ti + "\"/>");
        }
        placeholders.put("link_custom_css", "<link rel=\"stylesheet\" type=\"text/css\" href=\"" + DocmaConstants.HTML_CSS_FILENAME + "\"/>");
        placeholders.put("common_url", COMMON_RELATIVE_URL);

        // Header titles
        String head1_mode = outConfig.getHtmlWebhelpHeader1();
        String head2_mode = outConfig.getHtmlWebhelpHeader2();
        if ((head1_mode == null) || head1_mode.equals("")) {
            head1_mode = "upper";    // Show title of upper chunk by default
        }
        if ((head2_mode == null) || head2_mode.equals("")) {
            head2_mode = "current";  // Show title of current chunk by default
        }
        String head1 = null;
        if ((! head1_mode.equals("upper")) || show_up) {
            head1 = getChunkHeaderText(head1_mode, chunkEntry, upEntry, tocRoot, infoElems, outConfig);
            if (head1.length() > 0) {
                placeholders.put("header_title1", "<div class=\"webhelptitle1\">" + head1 + "</div>");
            }
        }
        if ((! head2_mode.equals("upper")) || show_up) {
            String head2 = getChunkHeaderText(head2_mode, chunkEntry, upEntry, tocRoot, infoElems, outConfig);
            // Output second header if existent and not the same as the first header
            if ((head2.length() > 0) && !((head1 != null) && head1.equals(head2))) {
                placeholders.put("header_title2", "<div class=\"webhelptitle2\">" + head2 + "</div>");
            }
        }
        
        // Prev and Next links in header
        placeholders.put("navigation_links", getHeaderNavigation(prevEntry, nextEntry, upEntry, homeEntry, outConfig));
        
        placeholders.put("navigation_tree", getLeftNavigation(chunkEntry, outConfig));
        placeholders.put("parts_navigation", getPartsNavigation(chunkEntry, tocRoot, outConfig));
            
        if (outConfig.isHtmlBreadcrumbs()) {
            placeholders.put("breadcrumbs_navigation", getBreadcrumbsNavigation(chunkEntry, outConfig, infoElems));
        }
        if (header != null) {
            placeholders.put("custom_header", header);
        }
        placeholders.put("content", chunk.getContent().toString());
        placeholders.put("footer_navigation", getFooterNavigation(chunkEntry, prevEntry, nextEntry, upEntry, homeEntry, separateToc, outConfig));
        if (footer != null) {
            placeholders.put("custom_footer", footer);
        }
            
        GentextRetriever gen = new GentextRetriever() {
            public String getGenText(String key) throws Exception
            {
                return WebFormatter.this.getGenText(key, outConfig);
            }
        };
        TemplateReader tr = getTemplateReader(outConfig.getStyleVariant());
        
        File fn = new File(outputDir, chunk.getRelativeFilePath());
        FileOutputStream fout = new FileOutputStream(fn);
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(fout, encoding));
        try {
            tr.writeTemplate(out, placeholders, gen);
        } finally {
            out.close();
            try {
                fout.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private String getPartsNavigation(ParserTocEntry chunkEntry,
                                      ParserTocEntry rootEntry,
                                      DocmaOutputConfig outConfig) throws Exception
    {
        StringBuilder buf = new StringBuilder();
        ParserTocEntry currentPartEntry = chunkEntry;
        while ((currentPartEntry != null) && !currentPartEntry.isPart()) {
            currentPartEntry = currentPartEntry.getParentEntry();
        }
        ParserTocEntry[] parts = rootEntry.getSubEntries(ParserTocEntry.TYPE_PART);
        if ((currentPartEntry == null) && (chunkEntry == rootEntry) && (parts.length > 0)) {
            currentPartEntry = parts[0];
        }
        for (ParserTocEntry entry : parts) {
            if (entry == currentPartEntry) {
                buf.append("<li class=\"book-currentpart\">");
            } else {
                buf.append("<li>");
            }
            buf.append("<a href=\"").append(entry.getLinkURL()).append("\">");
            buf.append(entry.getTitle());
            buf.append("</a></li>");
        }
        if (buf.length() > 0) {
            return "<div id=\"bookpartsnavigation\"><ul>" + buf + "</ul></div>";
        } else {
            return "";
        }
    }
    
    private String getBreadcrumbsNavigation(ParserTocEntry chunkEntry,
                                            DocmaOutputConfig outConfig, 
                                            Map<String, String> infoElems) throws Exception
    {
        String crumb_sep = outConfig.getHtmlBreadcrumbSeparator();
        if ((crumb_sep == null) || crumb_sep.equals("")) {
            crumb_sep = "&gt;";
        }
        String start_level = outConfig.getHtmlBreadcrumbStart();
        
        StringBuilder link_buf = new StringBuilder();
        ParserTocEntry parent = chunkEntry.getParentEntry();
        while (parent != null) {
            ParserTocEntry pp = parent.getParentEntry();
            if (parent.getChunk() != null) {
                boolean is_root = (pp == null);
                String title;
                if (is_root) {
                    String pubTitle = infoElems.get("doc-title");
                    title = (pubTitle != null) ? pubTitle : parent.getTitle();
                    if (title.equals("")) {
                        title = getGenText("nav-home", outConfig);
                    }
                } else {
                    title = parent.getTitle();
                }
                link_buf.insert(0, "<span class=\"breadcrumb-link\"><a class=\"breadcrumb_node\" href=\"" + 
                                parent.getLinkURL() + "\">" + title + "</a></span> <span class=\"breadcrumb_separator\">" + 
                                crumb_sep + "</span> ");
                if (start_level != null) {
                    if (start_level.equals("part") && parent.isPart()) {
                        break;
                    } else if (start_level.equals("chapter") && !parent.isSection()) {
                        break;
                    }
                }
            }
            parent = pp;
        }
        if (link_buf.length() == 0) {
            return "";  // do not show breadcrumbs if it consists of the last node only (i.e. the root)
        }
        
        // Output breadcrumbs
        return "<div id=\"breadcrumbsnavigation\" class=\"breadcrumbs\">" + link_buf +
               "<span class=\"breadcrumb_node breadcrumb_lastnode\">" + chunkEntry.getTitle() + 
               "</span></div>";
    }
    
    private void appendFootnotes(Appendable out,
                                 WebChunk chunk,
                                 DocmaOutputConfig outConfig, 
                                 Map<String, WebChunk> mapFootnoteToChunk, 
                                 ExportLog export_log) throws Exception
    {
        int fcount = chunk.getFootnoteCount();
        if (fcount == 0) {
            return;
        }
        out.append("<div class=\"footnotes\"><br/><hr width=\"100\" align=\"left\"/>");
        for (int i=0; i < fcount; i++) {
            ParserTocEntry note = chunk.getFootnote(i);
            String alias = note.getAlias();
            String source_id = ((alias != null) && alias.startsWith(FOOT_ID_PREFIX)) ? alias.substring(FOOT_ID_PREFIX.length()) : "";
            String txt = note.getTitle();
            String source_link = "#" + source_id;
            if (txt.equals("")) {   // The footnote may be defined in another chunk
                WebChunk note_chunk = mapFootnoteToChunk.get(alias);
                if (note_chunk != null) {
                    ParserTocEntry external_note = note_chunk.getFootnoteByAlias(alias);
                    if (external_note != null) {  // should always be true
                        txt = external_note.getTitle();
                        source_link = note_chunk.getRelativeURL() + "#" + source_id;
                    }
                } else {
                    export_log.errorMsg(chunk.getRelativeFilePath() + ": Referenced footnote not found: " + alias);
                }
            }
            out.append("<div class=\"footnote\"><sup>[<a id=\"");
            out.append(alias);
            out.append("\" href=\"").append(source_link).append("\">");
            out.append(note.getLabelNumber()).append("</a>] </sup>");
            out.append(txt);
            out.append("</div>");
        }
        out.append("</div>\n");
    }

    private String getHeaderNavigation(ParserTocEntry prevEntry,
                                       ParserTocEntry nextEntry,
                                       ParserTocEntry upEntry,
                                       ParserTocEntry homeEntry,
                                       DocmaOutputConfig outConfig) throws Exception
    {
        boolean show_up = (upEntry != null) && (upEntry != homeEntry);
        
        StringBuilder out = new StringBuilder();
        // final String NAVHEAD_SEP = outConfig.isHtmlNavigationalIcons() ? "&nbsp;" : "&nbsp;<span class=\"navhead-separator\">|</span>&nbsp;";
        final String NAVHEAD_SEP = "<span class=\"navhead-separator\">&nbsp;|&nbsp;</span>";
        if (prevEntry != null) {
            out.append("<a id=\"navhead-prev\" accesskey=\"p\" href=\"").append(prevEntry.getLinkURL()).append("\">");
            writeNavLinkContent(out, "prev", outConfig);
            out.append("</a>");
        }
        if (show_up) {
            if (prevEntry != null) {
                out.append(NAVHEAD_SEP);
            }
            out.append("<a id=\"navhead-up\" accesskey=\"u\" href=\"").append(upEntry.getLinkURL()).append("\">");
            writeNavLinkContent(out, "up", outConfig);
            out.append("</a>");
        }
        if (nextEntry != null) {
            if ((prevEntry != null) || show_up) {
                out.append(NAVHEAD_SEP);
            }
            out.append("<a id=\"navhead-next\" accesskey=\"n\" href=\"").append(nextEntry.getLinkURL()).append("\">");
            writeNavLinkContent(out, "next", outConfig);
            out.append("</a>");
        }
        return out.toString();
    }

    private String getFooterNavigation(ParserTocEntry chunkEntry,
                                       ParserTocEntry prevEntry,
                                       ParserTocEntry nextEntry,
                                       ParserTocEntry upEntry,
                                       ParserTocEntry homeEntry,
                                       WebChunk separateToc,
                                       DocmaOutputConfig outConfig) throws Exception
    {
        StringBuilder rows = new StringBuilder();
        String self_url = chunkEntry.getChunk().getRelativeURL();
        boolean show_up = (upEntry != null) && (upEntry != homeEntry);
        boolean row1 = (prevEntry != null) || (nextEntry != null) || (upEntry != null);
        
        if (row1) {
            rows.append("<tr>");
            rows.append("<td width=\"40%\" align=\"left\">");
            if ((prevEntry != null)) {
                rows.append("<a accesskey=\"p\" href=\"").append(prevEntry.getLinkURL()).append("\">");
                writeNavLinkContent(rows, "prev", outConfig);
                rows.append("</a>");
            }
            rows.append("&#160;");
            rows.append("</td>");
            rows.append("<td width=\"20%\" align=\"center\">");
            if (show_up) {
                rows.append("<a accesskey=\"u\" href=\"").append(upEntry.getLinkURL()).append("\">");
                writeNavLinkContent(rows, "up", outConfig);
                rows.append("</a>");
            } else {
                rows.append("&#160;");
            }
            rows.append("</td>");
            rows.append("<td width=\"40%\" align=\"right\">&#160;");
            if ((nextEntry != null)) {
                rows.append("<a accesskey=\"n\" href=\"").append(nextEntry.getLinkURL()).append("\">");
                writeNavLinkContent(rows, "next", outConfig);
                rows.append("</a>");
            }
            rows.append("</td>");
            rows.append("</tr>");
        }
        boolean show_titles = outConfig.isHtmlNavigationalTitles();
        boolean toc_link = (separateToc != null) && !separateToc.getRelativeURL().equals(self_url);
        boolean row2 = (show_titles && (prevEntry != null)) || 
                       (show_titles && (nextEntry != null)) || 
                       toc_link || (homeEntry != chunkEntry);
        if (row2) {
            rows.append("<tr>");
            rows.append("<td width=\"40%\" align=\"left\" valign=\"top\">");
            if (show_titles && (prevEntry != null)) {
                rows.append(getLabeledTitle(prevEntry, outConfig));
            }
            rows.append("&#160;");
            rows.append("</td>");
            
            rows.append("<td width=\"20%\" align=\"center\">");
            if (homeEntry != chunkEntry) {
                rows.append("<a accesskey=\"h\" href=\"").append(homeEntry.getLinkURL()).append("\">");
                writeNavLinkContent(rows, "home", outConfig);
                rows.append("</a>");
                if (toc_link) {
                    rows.append("&#160;<span class=\"navfoot-separator\">|</span>&#160;");
                }
            } else {
                rows.append("&#160;");
            }
            if (toc_link) {
                rows.append("<a accesskey=\"t\" href=\"").append(separateToc.getRelativeURL()).append("\">");
                writeNavLinkContent(rows, "toc", outConfig);
                rows.append("</a>");
            }
            rows.append("</td>");
            rows.append("<td width=\"40%\" align=\"right\" valign=\"top\">&#160;");
            if (show_titles && (nextEntry != null)) {
                rows.append(getLabeledTitle(nextEntry, outConfig));
            }
            rows.append("</td>");
            rows.append("</tr>");
        }

        if (rows.length() > 0) {
            return "<table width=\"100%\" summary=\"Navigation footer\">" + rows + "</table>";
        } else {
            return "";
        }
    }
    
    private void writeNavLinkContent(Appendable out, String direction, DocmaOutputConfig outConfig) throws Exception
    {
        String txt = jsGenTxt("nav-" + direction, outConfig);
        String spantxt = "<span class=\"navlink-text\">" + txt + "</span>";
        if (outConfig.isHtmlNavigationalIcons()) {
            boolean is_next = direction.equals("next");
            String path = COMMON_RELATIVE_URL + "/images/"; // outConfig.getHtmlNavigationIconsPath();
            String ext = ".png";   // outConfig.getHtmlNavigationIconsExt()
            // if ((path.length() > 0) && !path.endsWith("/")) {
            //     path += "/";
            // }
            if (is_next) {
                out.append(spantxt);
                // out.append("&nbsp;");
            }
            out.append("<img src=\"")
               .append(path).append(direction).append(ext)
               .append("\" class=\"navlink-image\" title=\"").append(txt).append("\"/>");
            if (! is_next) {
                // out.append("&nbsp;");
                out.append(spantxt);
            }
        } else {
            out.append(spantxt);
        }
    }
    
    private String getLabeledTitle(ParserTocEntry entry, DocmaOutputConfig outConfig) throws Exception
    {
        if (entry.isSection()) {
            return entry.getNumberedTitle();
        } else {
            String etype = entry.getType();
            String toc_named = outConfig.getTocNamedLabels();
            toc_named = (toc_named == null) ? "" : toc_named.toLowerCase();
            String lab_name = toc_named.contains(etype) ? getLabelName(etype, outConfig) : "";
            if (lab_name.equals("")) {
                return entry.getNumberedTitle();
            } else {
                return lab_name + " " + entry.getNumberedTitle();
            }
        }
    }
            
    private String getLeftNavigation(ParserTocEntry chunkEntry, DocmaOutputConfig outConfig) throws Exception
    {
        ParserTocEntry rootEntry = null;
        if (chunkEntry.getParentEntry() == null) {
            // If this is the root chunk (index.html), show sub-tree of first part
            ParserTocEntry[] parts = chunkEntry.getSubEntries(ParserTocEntry.TYPE_PART);
            if (parts.length > 0) {
                rootEntry = parts[0];
            }
        }
        if (rootEntry == null) {
            ParserTocEntry p = chunkEntry;
            do {
                rootEntry = p;
                p = rootEntry.getParentEntry();
            } while ((p != null) && !rootEntry.isPart());
        }
        
        StringBuilder out = new StringBuilder();
        writeNavigationTree(out, rootEntry, chunkEntry);
        return out.toString();
    }
    
    private void writeNavigationTree(Appendable out, 
                                     ParserTocEntry parentEntry,
                                     ParserTocEntry chunkEntry) throws IOException
    {
        int cnt = parentEntry.getSubEntryCount();
        for (int i = 0; i < cnt; i++) {
            ParserTocEntry entry = parentEntry.getSubEntry(i);
            if (entry == chunkEntry) {
                out.append("<li id=\"webhelp-currentid\">");
            } else {
                out.append("<li>");
            }
            out.append("<span class=\"file\">");
            out.append("<a href=\"").append(entry.getLinkURL()).append("\">");
            out.append(entry.getTitle());
            out.append("</a></span>");
            if (entry.getSubEntryCount() > 0) {
                out.append("<ul>");
                writeNavigationTree(out, entry, chunkEntry);
                out.append("</ul>");
            }
            out.append("</li>");
        }
    }
    
    private void writeCSSLink(Appendable out, String cssUrl) throws IOException
    {
        out.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"").append(cssUrl).append("\"/>\n");
    }
    
    private void writeJSLink(Appendable out, String jsUrl) throws IOException
    {
        out.append("<script type=\"text/javascript\" src=\"").append(jsUrl).append("\"></script>\n");
    }
    
    private String jsGenTxt(String genKey, DocmaOutputConfig outConfig) throws Exception
    {
        String txt = getGenText(genKey, outConfig);
        return ((txt == null) || txt.equals("")) ? genKey : txt.replace('"', '\'');
    }
    
    private void parse_1st(String source, 
                           DocmaOutputConfig outConfig,
                           ParserTocEntry tocRoot, 
                           List<WebChunk> chunks, 
                           Map<String, String> infoElements, 
                           ExportLog exportLog) throws Exception
    {
        chunks.clear();
        WebChunk rootChunk = new WebChunk(ROOT_CHUNK_FILENAME);
        chunks.add(rootChunk);
        
        XMLEventReader xr = xinFactory.createXMLEventReader(new StringReader(source));
        XMLEventWriter xw = xoutFactory.createXMLEventWriter(rootChunk.getWriter());
        tocRoot.setXMLChunk(rootChunk, xw);
        
        int divLevel = 0;
        Stack<Integer> tocLevels = new Stack<Integer>();
        ParserTocEntry currentTocEntry = tocRoot;  // the current ToC entry
        Stack<ParserTocEntry> currentTables = new Stack<ParserTocEntry>();
        boolean isInfoDiv = false;
        int infoDivLevel = 0;
        
        int partCounter = 0;
        int chapCounter = 0;
        int appendixCounter = 0;
        Map<String, Integer> sectCounters = new HashMap<String, Integer>();   // maps prefix to counter
        Map<String, Integer> figureCounters = new HashMap<String, Integer>(); // maps prefix to counter
        Map<String, Integer> tableCounters = new HashMap<String, Integer>();  // maps prefix to counter
        Map<String, Integer> formalCounters = new HashMap<String, Integer>(); // maps prefix to counter
        int fallbackCounter = 0;  // fallback to generate component ids 
        int filenameCounter = 0;  // fallback to generate unique chunk filenames (should not be required)
        
        // Get all relevant configuration
        String langCode = outConfig.getLanguageCode();
        Properties genTextProps = outConfig.getGentextProps();
        boolean singleFile = false; // outConfig.isHtmlSingleFile();
        int chunkLevel = singleFile ? 
            0 : // put all content in a single file (currently not supported for WebHelp)
            outConfig.getHtmlSeparateFileLevel() + 1;  // 1 means for each part a separate file or
                                                       // if no parts exist, for each chapter a separate file
        boolean aliasFileName = outConfig.isHtmlAliasFilename();
        String partFormat = numFormat(outConfig.getPartNumbering());
        String chapFormat = numFormat(outConfig.getChapterNumbering());
        String appendixFormat = numFormat(outConfig.getAppendixNumbering());
        String sectFormat = numFormat(outConfig.getSectionNumbering());
        int sectMaxDepth = outConfig.getNumberingDepth();
        boolean exclude1stLevNum = outConfig.isExclude1stLevelNumber();
        boolean restartFromPart = outConfig.isRestartPart();
        boolean isTitleAfter = "after".equalsIgnoreCase(outConfig.getTitlePlacement());
        
        while (xr.hasNext()) {
            XMLEvent e = xr.nextEvent();
            int etype = e.getEventType();
            // System.out.println(e.toString());
            switch (etype) {
                case XMLEvent.START_ELEMENT: 
                    StartElement se = e.asStartElement();
                    String ename = se.getName().getLocalPart().toLowerCase();
                    String aid = null;
                    String aclass = null;
                    String atitle = null;
                    String astyle = null;
                    String alang = null;
                    Iterator it = se.getAttributes();
                    while (it.hasNext()) {
                        Attribute att = (Attribute) it.next();
                        String attname = att.getName().getLocalPart();
                        if (attname.equalsIgnoreCase("id")) {
                            aid = att.getValue();
                        } else if (attname.equalsIgnoreCase("class")) {
                            aclass = att.getValue();
                        } else if (attname.equalsIgnoreCase("title")) {
                            atitle = att.getValue();
                        } else if (attname.equalsIgnoreCase("style")) {
                            astyle = att.getValue();
                        } else if (attname.equalsIgnoreCase("lang")) {
                            alang = att.getValue();
                        }
                    }
                    
                    if (ename.equals("div")) {
                        divLevel++;
                        if (aclass != null) {
                            final String DOC_PREFIX = "doc-";
                            if (aclass.startsWith(DOC_PREFIX)) {
                                boolean is_sect = aclass.equals("doc-section");
                                boolean is_chap = aclass.equals("doc-chapter");
                                boolean is_preface = aclass.equals("doc-preface");
                                boolean is_appendix = aclass.equals("doc-appendix");
                                boolean is_part = aclass.equals("doc-part");
                                boolean is_partintro = aclass.equals("doc-partintro");
                                boolean is_index = aclass.equals("doc-index");
                                boolean start_info = aclass.equals("doc-info") && (divLevel == 1);
                                boolean first_section = false;

                                if (start_info) {
                                    isInfoDiv = true;
                                    infoDivLevel = divLevel;
                                    break;  // do not write div tag to root chunk
                                }
                                
                                if (isInfoDiv) {  // a div which is child of <div class="doc-info">
                                    infoElements.put(aclass, readElementToString(se, xr, false, currentTocEntry, outConfig));
                                    divLevel--;  // child-div has been consumed by readElementToString(...) 
                                    break;
                                }
                                
                                if (is_index) {
                                    // do not write index-div to output; just ignore it
                                    readText(se, xr);  // read up to the closing tag
                                    break;
                                }

                                boolean is_toclevel = is_sect || is_chap || is_preface || is_appendix || is_part;
                                
                                ParserTocEntry entry = new ParserTocEntry();
                                if (aid != null) {
                                    entry.setAlias(aid);
                                }
                                if ((atitle != null) && !is_toclevel) {
                                    entry.setTitle(atitle);
                                    // Note: For toc-level elements (preface, chapter...)
                                    // the title is given in the first h1,h2,h3... element.
                                }
                                if (is_sect) {
                                    entry.setType(ParserTocEntry.TYPE_SECTION);
                                    int sectDepth = calcSectionDepth(currentTocEntry) + 1;
                                    if ((sectDepth <= sectMaxDepth) && ! sectFormat.equals("0")) {
                                        String parentLabel = currentTocEntry.getLabelNumber();
                                        Integer prevCount = sectCounters.get(parentLabel);
                                        if (prevCount == null) {
                                            prevCount = 0;
                                        }
                                        int sectNum = prevCount + 1;
                                        first_section = (sectNum == 1);
                                        sectCounters.put(parentLabel, sectNum);
                                        String sectLab = formatNumber(sectNum, sectFormat);
                                        if (currentTocEntry.isSection() || !exclude1stLevNum) {
                                            if (parentLabel.length() > 0) {
                                                sectLab = parentLabel + labelSeparator() + sectLab;
                                            }
                                        }
                                        entry.setLabelNumber(sectLab);
                                    }
                                } else if (is_chap) {
                                    sectCounters.clear();   // restart section numbering in each chapter
                                    entry.setType(ParserTocEntry.TYPE_CHAPTER);
                                    chapCounter++;
                                    if (! chapFormat.equals("0")) {
                                        String chapLab = formatNumber(chapCounter, chapFormat);
                                        if (restartFromPart && ! partFormat.equals("0")) {
                                            String partLab = currentTocEntry.getLabelNumber();
                                            if (partLab.length() > 0) {
                                                chapLab = partLab + labelSeparator() + chapLab;
                                            }
                                        }
                                        entry.setLabelNumber(chapLab);
                                    }
                                } else if (is_preface) {
                                    sectCounters.clear();   // restart section numbering in each preface
                                    entry.setType(ParserTocEntry.TYPE_PREFACE);
                                } else if (is_appendix) {
                                    sectCounters.clear();   // restart section numbering in each appendix
                                    entry.setType(ParserTocEntry.TYPE_APPENDIX);
                                    appendixCounter++;
                                    if (! appendixFormat.equals("0")) {
                                        entry.setLabelNumber(formatNumber(appendixCounter, appendixFormat));
                                    }
                                } else if (is_part) {
                                    entry.setType(ParserTocEntry.TYPE_PART);
                                    partCounter++;
                                    if (! partFormat.equals("0")) {
                                        entry.setLabelNumber(formatNumber(partCounter, partFormat));
                                    }
                                    if (restartFromPart) {
                                        chapCounter = 0;
                                    }
                                } else if (is_partintro) {
                                    sectCounters.clear();   // restart section numbering in each partintro
                                    entry.setType(ParserTocEntry.TYPE_PARTINTRO);
                                } else {
                                    entry.setType(ParserTocEntry.TYPE_BLOCK);
                                }

                                if (is_toclevel) {
                                    tocLevels.push(divLevel);
                                    currentTocEntry.addSubEntry(entry);
                                    currentTocEntry = entry;
                                    
                                    // Write parts/chapters/sections/... to 
                                    // separate files up to the configured chunk
                                    // level. If isHtmlInclude1stSection() is 
                                    // true, then include the first section in
                                    // the parent chunk.
                                    int depth = calcDepth(currentTocEntry);
                                    if ((depth <= chunkLevel) && !(first_section && outConfig.isHtmlInclude1stSection())) {
                                        String chunkfn = chunkFilename(currentTocEntry, aliasFileName, ++filenameCounter);
                                        // Place content of this part/chapter/section/... in a separate file
                                        WebChunk chunk = new WebChunk(chunkfn);
                                        chunks.add(chunk);
                                        // Replace XML writer; 
                                        // Note that the div tag will be written to the new writer 
                                        // See xw.add(e) below.
                                        xw = xoutFactory.createXMLEventWriter(chunk.getWriter());
                                        currentTocEntry.setXMLChunk(chunk, xw);
                                    }
                                    if (aid == null) {  // id is required to be able to reference the section from the ToC
                                        // Add id attribute to the output element; see createStartTag(...) below.
                                        aid = formatComponentId(++fallbackCounter); // getCurrentChunk(currentTocEntry).generateAnchorId(); 
                                        currentTocEntry.setAlias(aid);
                                        // e = createStartTag(ename, se.getAttributes(), "id", aid);
                                    }
                                } else {
                                    if (aid != null) {
                                        currentTocEntry.addTargetElement(entry);
                                    }
                                }
                                
                                if (is_toclevel || is_partintro) {
                                    // Remove "doc-" from class name (e.g. change "doc-chapter" to "chapter")
                                    aclass = aclass.substring(DOC_PREFIX.length());
                                    e = createStartTag(ename, "id", aid, "class", aclass, "title", atitle);
                                    // e is written to output. See xw.add(e) below.
                                }
                            } else {  // div with custom class attribute
                                boolean is_formal = aclass.startsWith(FORMAL_PREFIX);
                                if (is_formal) {
                                    ParserTocEntry entry = createFormalTocEntry(currentTocEntry, aclass, aid, atitle, formalCounters);
                                    // Write (title before or after)
                                    //
                                    //   <div class="formal-block"><a id="d0e8422"></a>
                                    //     <div class="formal-block-contents">
                                    //       <div class="user_style">...</div>
                                    //     </div>
                                    //     <p class="title">...</p>
                                    //   </div>
                                    // String anch_id = (aid != null) ? aid : formatAnchorId(++fallbackCounter);
                                    writeFormalBlock(se, xr, xw, entry, isTitleAfter, langCode, genTextProps, outConfig);
                                    divLevel--;  // div has been completely consumed and written to xw
                                    break;  // break because div tag has already been written to xw
                                } else if (aid != null) {
                                    ParserTocEntry entry = new ParserTocEntry(ParserTocEntry.TYPE_BLOCK, aid, atitle);
                                    entry.setBlockName(aclass);
                                    currentTocEntry.addTargetElement(entry);
                                }
                            }
                        } else {  // div without class attribute
                            if (aid != null) {
                                ParserTocEntry entry = new ParserTocEntry(ParserTocEntry.TYPE_BLOCK, aid, atitle);
                                entry.setBlockName("");
                                currentTocEntry.addTargetElement(entry);
                            }
                        }
                        xw.add(e);  // write div tag
                        
                    } else if (ename.equals("p")) {
                        boolean is_formal = (aclass != null) && aclass.startsWith(FORMAL_PREFIX);
                        if (is_formal) {
                            ParserTocEntry entry = createFormalTocEntry(currentTocEntry, aclass, aid, atitle, formalCounters);
                            // Write (title before or after)
                            //
                            //   <div class="formal-block"><a id="d0e8422"></a>
                            //     <div class="formal-block-contents">
                            //       <div class="normal-para user_style">...</div>
                            //     </div>
                            //     <p class="title">...</p>
                            //   </div>
                            // String anch_id = (aid != null) ? aid : formatAnchorId(++fallbackCounter);
                            writeFormalBlock(se, xr, xw, entry, isTitleAfter, langCode, genTextProps, outConfig);
                            break;  // break because element has already been written to xw

                        } else if (aid != null) {
                            ParserTocEntry entry = new ParserTocEntry(ParserTocEntry.TYPE_BLOCK, aid, atitle);
                            entry.setBlockName("");
                            currentTocEntry.addTargetElement(entry);
                        }
                        
                        // Transform p tag to div tag. See also end-tag transformation below.
                        xw.add(createStartTag("div", toNormalParaAttributes(se.getAttributes())));
                        
                    } else if ((ename.length() == 2) && ename.startsWith("h") && Character.isDigit(ename.charAt(1))) {
                        // Title is given as child element h1, h2, h3,...,h6 of a div element.
                        // Title is first occurence of h1, h2, h3,...h6.
                        if (currentTocEntry.getTitle().equals("")) {  // first occurence of h1, h2, h3,...
                            String title = readText(se, xr);  // readElementToString(se, xr, false)
                            currentTocEntry.setTitle(title);

                            // Write h1, h2,...,h6 to output
                            boolean is_sect = currentTocEntry.isSection();
                            int sect_depth = 0;
                            String hout;
                            // In exported HTML: h1 is used for part headers.
                            // h2 is used for chapter, appendix, prefix headers. 
                            // h3 is used for 1st level section headers and so on.
                            if (is_sect) {
                                sect_depth = calcSectionDepth(currentTocEntry);
                                hout = "h" + (sect_depth + 2);
                            } else if (currentTocEntry.isPart()) {
                                hout = "h1";
                            } else {
                                hout = "h2";   // chapter, appendix, prefix
                            }

                            // e = createStartTag(hout, "class", aclass, "id", aid);
                            // xw.add(e);  // write opening header tag (h1, h2, h3, ...)
                            // writeHeader(xw, currentTocEntry.getLabelNumber(), title, currentTocEntry.getType(), langCode, genTextProps);
                            // xw.add(createEndTag(hout));  // write closing header tag
                            writeHeader(xw, hout, aclass, aid,
                                        currentTocEntry.getLabelNumber(), 
                                        title, currentTocEntry.getType(), 
                                        langCode, genTextProps);
                            
                            // Write placeholder for Table of Contents
                            boolean sect_toc = is_sect && outConfig.isSectionToc() && 
                                               (sect_depth <= outConfig.getSectionTocLevels());
                            boolean is_chap_level = currentTocEntry.isChapter() || 
                                                    currentTocEntry.isAppendix() || 
                                                    currentTocEntry.isPreface();
                            if (sect_toc || 
                                (outConfig.isChapterToc() && is_chap_level) ||
                                (outConfig.isPartToc() && currentTocEntry.isPart())) {
                                xw.add(createComment(COMPONENT_TOC_PLACEHOLDER + currentTocEntry.getAlias()));
                            }
                        } else {
                            xw.add(e);  // write opening header tag (h1, h2, h3, ...)
                        }
                    } else if (ename.equals("span")) {  
                        // special handling of span (footnotes, index entries, ...)
                        processSpan(se, xr, xw, aid, aclass, atitle, currentTocEntry, outConfig);
                    } else if (ename.equals("img")) {
                        if ((atitle != null) && (atitle.length() > 0)) {  // figure (labeled image)
                            ParserTocEntry entry = createFigureTocEntry(currentTocEntry, aid, atitle, figureCounters);
                            
                            boolean is_float = false; //(astyle != null) && astyle.contains("float");
                            if (astyle != null) {
                                SortedMap<String, String> cssprops = CSSParser.parseCSSProperties(astyle);
                                String float_val = cssprops.get("float");
                                is_float = (float_val != null);
                                if (is_float) {
                                    boolean is_left = float_val.contains("left");
                                    boolean is_right = is_left ? false : float_val.contains("right");
                                    String float_cls = (is_left ? "float_left " : (is_right ? "float_right " : "")) + 
                                                       "figure-float";
                                    xw.add(createStartTag("div", "class", float_cls, "style", astyle));
                                }
                            }
                            // String anch_id = (aid != null) ? aid : formatAnchorId(++fallbackCounter);
                            xw.add(createStartTag("div", "class", "figure", "title", atitle));
                            // xw.add(createStartTag("a", "id", anch_id));
                            // xw.add(createEndTag("a"));
                            if (! isTitleAfter) {
                                writeFigureTitle(xw, entry.getLabelNumber(), atitle, langCode, genTextProps);
                            }
                            xw.add(createStartTag("div", "class", "mediaobject"));
                            if (is_float) {
                                // Write original img tag, but exclude the style attribute
                                List<Attribute> copied_atts = copyAttributesExclude(se.getAttributes(), "style");
                                xw.add(createStartTag("img", copied_atts.iterator()));
                                readElement(se, xr, xw, false, currentTocEntry, outConfig);  // read closing img tag
                                xw.add(createEndTag("img"));  // write closing img tag
                            } else {
                                readElement(se, xr, xw, true, currentTocEntry, outConfig);  // write img element unchanged
                            }
                            xw.add(createEndTag("div"));
                            if (isTitleAfter) {
                                writeFigureTitle(xw, entry.getLabelNumber(), atitle, langCode, genTextProps);
                            }
                            xw.add(createEndTag("div"));
                            if (is_float) {
                                xw.add(createEndTag("div"));
                            }
                        } else {
                            xw.add(e);  // write opening img tag unchanged
                        }
                        
                    } else if (ename.equals("table")) {
                        ParserTocEntry tableEntry = new ParserTocEntry(ParserTocEntry.TYPE_TABLE);
                        tableEntry.setAlias(aid);
                        currentTables.push(tableEntry);
                        currentTocEntry.addTargetElement(tableEntry);
                        xw.add(e);  // write opening table tag
                        
                    } else if (ename.equals("caption")) {
                        if (! currentTables.isEmpty()) {
                            ParserTocEntry currTable = currentTables.lastElement();
                            xw.add(e);  // write opening caption tag (unchanged)
                            // Add label number to table caption
                            String title = readText(se, xr);
                            String labelNum = createTableLabel(currentTocEntry, tableCounters);
                            currTable.setTitle(title);
                            currTable.setLabelNumber(labelNum);
                            writeTableTitle(xw, labelNum, title, langCode, genTextProps);
                            xw.add(createEndTag(ename));  // write closing caption tag
                        } else {
                            xw.add(e);  // write opening caption tag (unchanged)
                        }
                    } else if (ename.equals("body")) {
                        // Set language code given in print-instance, if no 
                        // language code has been set in the output configuration.
                        if ((alang != null) && (alang.length() > 0)) {
                            if ((langCode == null) || langCode.equals("")) {
                                outConfig.setLanguageCode(alang);
                                langCode = alang;
                            }
                        }
                        xw.add(e);  // write body tag
                        
                    } else {
                        // Any other start element
                        xw.add(e);  // write all other start elements
                    }
                    break;
                    
                case XMLEvent.END_ELEMENT: 
                    
                    EndElement ee = e.asEndElement();
                    String tagname = ee.getName().getLocalPart().toLowerCase();
                    boolean restore_writer = false;
                    if ("div".equals(tagname)) {
                        if (isInfoDiv && (divLevel == infoDivLevel)) {
                            isInfoDiv = false;
                            divLevel--;
                            break;   // do not write closing div-tag
                        } else {
                            if (! tocLevels.isEmpty()) {
                                if (divLevel == tocLevels.lastElement().intValue()) {
                                    tocLevels.pop();
                                    currentTocEntry = currentTocEntry.getParentEntry();
                                    restore_writer = true;
                                }
                            }
                            divLevel--;
                        }
                    } else if ("p".equals(tagname)) {  
                        // Note: <p>...</p> are replaced by <div class="normal-para...">...</div> 
                        xw.add(createEndTag("div"));
                        break;  // do not write p end-tag (already replaced by div end-tag)
                    } else if ("table".equals(tagname)) {
                        if (! currentTables.isEmpty()) {
                            currentTables.pop();
                        }
                    }
                    
                    try {
                        xw.add(e);  // write end-tag
                        xw.flush();
                    } catch (Exception ex) {
                        if (exportLog != null) { 
                            exportLog.errorMsg("Failed to write end-tag '" + tagname + "': " + ex.getMessage());
                            ex.printStackTrace();
                        } else {
                            throw ex;
                        }
                    }
                    
                    // replace xw with writer of upper chunk
                    if (restore_writer) {  
                        // If parent level is written to a separate chunk,
                        // then restore chunk writer.
                        // Note: If first section is written to parent chunk
                        // then the restored writer is the current writer .
                        ParserTocEntry pe = currentTocEntry; // the parent level (after end-tag) 
                        while (pe != null) {
                            XMLEventWriter chunk_writer = pe.getXMLWriter();
                            // If the content of the current toc-level is included
                            // in the parent chunk, then chunk_writer is null. In 
                            // this case, the writer of the upper (parent) level
                            // has to be used.
                            if (chunk_writer != null) {
                                xw = chunk_writer;
                                break;
                            }
                            pe = pe.getParentEntry();
                        }
                        if ((pe == null) && (exportLog != null)) {  // debug message
                            exportLog.warningMsg("Could not retrieve upper chunk. Current ToC level: " + 
                                                 ((currentTocEntry == null) ? "null" : currentTocEntry.getTitle()));
                        }
                    }
                    break;
                    
                default:
                    xw.add(e);  // write all other XML tags
                    break;
            }  // switch
        }  // while (xr.hasNext())
        xw.flush();
        closeAllXMLWriter(tocRoot);
    }

    private boolean processSpan(StartElement se,
                                XMLEventReader xr,
                                XMLEventWriter xw, 
                                String aid,
                                String aclass,
                                String atitle,
                                ParserTocEntry currentTocEntry, 
                                DocmaOutputConfig outConfig) throws Exception
    {
        if (containsCSSClass(aclass, "footnote")) {
            String footFormat = numFormat(outConfig.getFootnoteNumbering());
            ParserTocEntry chunkTocEntry = getChunkEntry(currentTocEntry);
            WebChunk chunk = chunkTocEntry.getChunk();
            String ftext = readElementToString(se, xr, false, currentTocEntry, outConfig).trim();
            String txt_only = extractTextFromXML(ftext).trim();
            String see_alias = extractFootnoteSeeAlias(txt_only);
            ParserTocEntry fentry = null;
            boolean is_see = (see_alias != null);
            if (is_see) {
                fentry = chunk.getFootnoteByAlias(FOOT_ID_PREFIX + see_alias);
            } else if (aid != null) {  
                // If an id is assigned to the footnote, then the footnote entry 
                // may have already been created (in case of a forward-reference).
                fentry = chunk.getFootnoteByAlias(FOOT_ID_PREFIX + aid);
                if (fentry != null) {  
                    // The entry has already been created by a see reference.
                    // Now, set the footnote text.
                    fentry.setTitle(ftext);
                }
            }
            if (fentry == null) {  // New footnote or forward see-reference.
                // In case of a see-reference, the entry for the referenced
                // footnote must already be created here, because the label must 
                // be assigned and written to the output. Note however, that
                // the footnote text cannot be set here. This is done when
                // the referenced footnote is processed.
                int fnum = chunk.getFootnoteCount() + 1;
                String flabel = formatNumber(fnum, footFormat);
                String footid;
                if (is_see) {
                    footid = FOOT_ID_PREFIX + see_alias;
                } else {
                    if ((aid == null) || aid.trim().equals("")) {
                        String cn = defaultEntryName(chunkTocEntry);
                        aid = cn + ((fnum < 10) ? "foot-0" : "foot-") + fnum;
                    }
                    footid = FOOT_ID_PREFIX + aid;
                }
                fentry = new ParserTocEntry(ParserTocEntry.TYPE_FOOTNOTE);
                fentry.setLabelNumber(flabel);
                fentry.setTitle(is_see ? "" : ftext);
                fentry.setAlias(footid);
                chunk.addFootnote(fentry);
            }
            xw.add(createStartTag("sup"));
            xw.add(eventFactory.createCharacters("["));
            xw.add(createStartTag("a", "class" , "footnote", "href", "#" + fentry.getAlias(), "id", aid, "title", atitle));
            xw.add(eventFactory.createCharacters(fentry.getLabelNumber()));
            xw.add(createEndTag("a"));
            xw.add(eventFactory.createCharacters("]"));
            xw.add(createEndTag("sup"));
            
            return true;  // span tag has already been processed (removed)
            
        } else if (containsCSSClass(aclass, "indexterm")) {
            String txt = readText(se, xr).trim();
            if (txt.length() > 0) {
                String[] terms = txt.split("\\|");
                for (String t : terms) {
                    t = t.trim();
                    int p = t.indexOf("{start:");
                    if (p >= 0) {
                        int p2 = t.indexOf('}', p);
                        if (p2 > 0) {
                            t = (t.substring(0, p) + t.substring(p2 + 1)).trim();
                        }
                    }
                    p = t.indexOf("{end:");
                    if (p >= 0) {
                        int p2 = t.indexOf('}', p);
                        if (p2 > 0) {
                            t = (t.substring(0, p) + t.substring(p2 + 1)).trim();
                        }
                    }
                    String see_term = null;
                    List<String> seealso_terms = null;
                    final String SEE_PATTERN = "{see:";
                    p = t.indexOf(SEE_PATTERN);
                    if (p > 0) {
                        int p2 = t.indexOf('}', p);
                        if (p2 > 0) {
                            see_term = t.substring(p + SEE_PATTERN.length(), p2).trim();
                            t = (t.substring(0, p) + t.substring(p2 + 1)).trim();
                        }
                    }
                    final String SEEALSO_PATTERN = "{seealso:";
                    do {
                        p = t.lastIndexOf(SEEALSO_PATTERN);
                        if (p > 0) {
                            int p2 = t.lastIndexOf('}');
                            if (p2 > p) {
                                String ref = t.substring(p + SEEALSO_PATTERN.length(), p2).trim();
                                if (ref.length() > 0) {
                                    if (seealso_terms == null) {
                                        seealso_terms = new ArrayList<String>();
                                    }
                                    seealso_terms.add(ref);
                                }
                            }
                            t = t.substring(0, p).trim();
                        }
                    } while (p > 0);
                    
                    final String PREF_PATTERN = "{!}";
                    boolean preferred = t.endsWith(PREF_PATTERN);
                    if (preferred) {
                        t = t.substring(0, t.length() - PREF_PATTERN.length()).trim();
                    }

                    if (t.length() > 0) {
                        ParserTocEntry chunkTocEntry = getChunkEntry(currentTocEntry);
                        WebChunk chunk = chunkTocEntry.getChunk();
                        String link_title = getLabeledTitle(currentTocEntry, outConfig);
                        if ((see_term == null) && (seealso_terms == null)) {
                            ParserIndexEntry entry = new ParserIndexEntry(ParserIndexEntry.TYPE_NORMAL, t);
                            String anch_id = chunk.generateAnchorId();
                            entry.setPreferred(preferred);
                            entry.setLinkURL(chunk.getRelativeURL() + "#" + anch_id);
                            entry.setLinkTitle(link_title);
                            chunk.addIndexEntry(entry);
                            // Replace index term by anchor
                            xw.add(createStartTag("a", "id", anch_id));
                            xw.add(createEndTag("a"));
                        } else {
                            if (see_term != null) {
                                ParserIndexEntry entry = new ParserIndexEntry(ParserIndexEntry.TYPE_SEE, t);
                                entry.setReferencedEntry(see_term);
                                chunk.addIndexEntry(entry);
                            }
                            if (seealso_terms != null) {
                                for (String seealso : seealso_terms) {
                                    ParserIndexEntry entry = new ParserIndexEntry(ParserIndexEntry.TYPE_SEE_ALSO, t);
                                    entry.setReferencedEntry(seealso);
                                    chunk.addIndexEntry(entry);
                                }
                            }
                        }
                    }
                }
            }
            return true;  // span tag has already been processed (removed / replaced by anchor)
            
        } else if (aid != null) {   // other span with id -> just register it as inline target
            ParserTocEntry entry = new ParserTocEntry(ParserTocEntry.TYPE_INLINE, aid, atitle);
            currentTocEntry.addTargetElement(entry);
        }
        xw.add(se);  // write span tag
        return false;  // process the content of the span in the parse_1st loop
    }

    private String extractFootnoteSeeAlias(String txt)
    {
        final String SEE_PREFIX = "{see:";
        final String SHORT_PREFIX = "{#";
        if (txt.startsWith(SEE_PREFIX)) {
            String str = txt.substring(SEE_PREFIX.length()).trim();
            if (str.startsWith("#")) {
                str = str.substring(1).trim();
                return (str.length() > 0) ? str : null;
            }
        }
        if (txt.startsWith(SHORT_PREFIX)) {
            String str = txt.substring(SHORT_PREFIX.length()).trim();
            return (str.length() > 0) ? str : null;
        }
        return null;
    }
    
    private String extractTextFromXML(String xml) 
    {
        int tstart = xml.indexOf("<");
        if (tstart < 0) {  // no tags found
            return xml;
        }
        StringBuilder buf = new StringBuilder(xml);
        do {
            // find end of tag
            int tend = buf.indexOf(">", tstart + 1);
            if (tend < 0) {
                return buf.toString();
            }
            buf.delete(tstart, tend + 1);  // delete tag
            tstart = buf.indexOf("<", tstart);  // find start of next tag
        } while (tstart >= 0);
        return buf.toString();
    }
    
    private ParserTocEntry getChunkEntry(ParserTocEntry currentEntry) 
    {
        ParserTocEntry e = currentEntry;
        while (e != null) { 
            if (e.getChunk() != null) {
                return e;
            } else {
                e = e.getParentEntry();
            }
        }
        return null;
    }
    
    private WebChunk getCurrentChunk(ParserTocEntry currentEntry) 
    {
        ParserTocEntry e = getChunkEntry(currentEntry);
        return (e == null) ? null : e.getChunk();
    }
    
    private void closeAllXMLWriter(ParserTocEntry entry)
    {
        XMLEventWriter xw = entry.getXMLWriter();
        if (xw != null) {
            try {
                xw.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        for (int i=0; i < entry.getSubEntryCount(); i++) {
            closeAllXMLWriter(entry.getSubEntry(i));
        }
    }

    private String defaultEntryName(ParserTocEntry entry)
    {
        String fn = "";
        ParserTocEntry e = entry;
        while (e != null) {
            ParserTocEntry parent = e.getParentEntry();
            if (parent == null) {
                break;  // stop when root entry has been reached
            }
            String prefix = null; 
            if (e.isSection()) prefix = "s";
            else if (e.isPart()) prefix = "pt";
            else if (e.isChapter()) prefix = "ch";
            else if (e.isPreface()) prefix = "pr";
            else if (e.isAppendix()) prefix = "ap";

            if (prefix == null) {  // should not occur
                fn = "";
                break;
            } else {
                String seq = Integer.toString(parent.getSubEntryIndexSameType(e) + 1);
                if (seq.length() <= 1) {
                    seq = "0" + seq;
                }
                fn = prefix + seq + fn;
            }
            e = parent;
        }
        return fn;
    }
    
    private String chunkFilename(ParserTocEntry entry, boolean alias_filename, int id_num) 
    {
        String alias = entry.getAlias();
        if (alias_filename && (alias != null) && (alias.length() > 0)) {
            return alias + ".html";
        } else {
            // Generate default filename
            String fn = defaultEntryName(entry);
            if (fn.equals("")) {  // generate filename from id_num
                final String ZEROS = "00000000";
                String id_str = Integer.toString(id_num);
                int id_len = id_str.length();
                fn = "doc" + ((id_len >= ZEROS.length()) ? id_str : (ZEROS.substring(id_len) + id_str));
            }
            return fn + ".html";
        }
    }

    private String createTableLabel(ParserTocEntry parentEntry, 
                                    Map<String, Integer> tableCounters)
    {
        String prefix = getFormalPrefix(parentEntry);
        // Separate counters are used for different prefixes.
        Integer prev_count = tableCounters.get(prefix);
        if (prev_count == null) {
            prev_count = 0;
        }
        int label_num = prev_count + 1;
        tableCounters.put(prefix, label_num);
        String label_str = formatNumber(label_num, "1");
        if (prefix.length() > 0) {
            label_str = prefix + labelSeparator() + label_str;
        }
        return label_str;
    }
    
    private ParserTocEntry createFigureTocEntry(ParserTocEntry parentEntry, 
                                                String refId, 
                                                String title, 
                                                Map<String, Integer> figureCounters)
    {
        ParserTocEntry entry = new ParserTocEntry(ParserTocEntry.TYPE_FIGURE, refId, title);
        String prefix = getFormalPrefix(parentEntry);
        // Separate counters are used for different prefixes.
        Integer prev_count = figureCounters.get(prefix);
        if (prev_count == null) {
            prev_count = 0;
        }
        int label_num = prev_count + 1;
        figureCounters.put(prefix, label_num);
        String label_str = formatNumber(label_num, "1");
        if (prefix.length() > 0) {
            label_str = prefix + labelSeparator() + label_str;
        }
        entry.setLabelNumber(label_str);
        parentEntry.addTargetElement(entry);
        return entry;
    }
    
    private ParserTocEntry createFormalTocEntry(ParserTocEntry parentEntry, 
                                                String aclass, 
                                                String refId, 
                                                String title, 
                                                Map<String, Integer> formalCounters)
    {
        String formalId = aclass.substring(FORMAL_PREFIX.length()).trim();
        ParserTocEntry entry = new ParserTocEntry(ParserTocEntry.TYPE_FORMAL, refId, title);
        entry.setFormalId(formalId);
        String formal_prefix = getFormalPrefix(parentEntry);
        String counter_key = formalId + "#" + formal_prefix;
        // Separate counters are used for different prefixes.
        Integer prev_count = formalCounters.get(counter_key);
        if (prev_count == null) {
            prev_count = 0;
        }
        int label_num = prev_count + 1;
        formalCounters.put(counter_key, label_num);
        String label_str = formatNumber(label_num, "1");
        if (formal_prefix.length() > 0) {
            label_str = formal_prefix + labelSeparator() + label_str;
        }
        entry.setLabelNumber(label_str);
        parentEntry.addTargetElement(entry);
        return entry;
    }
    
    private void writeFormalBlock(StartElement se,
                                  XMLEventReader xr,
                                  XMLEventWriter xw, 
                                  ParserTocEntry entry,
                                  boolean isTitleAfter,
                                  // String anchorId, 
                                  String langCode,
                                  Properties genTextProps, 
                                  DocmaOutputConfig outConfig) throws Exception
    {
        boolean is_para = se.getName().getLocalPart().equalsIgnoreCase("p");

        String title = entry.getTitle();
        xw.add(createStartTag("div", "class", "formal-block", "title", title));
        // xw.add(createStartTag("a", "id", anchorId));
        // xw.add(createEndTag("a"));
        if (! isTitleAfter) {
            writeFormalTitle(xw, entry.getLabelNumber(), title, entry.getFormalId(), "p", langCode, genTextProps);
        }
        xw.add(createStartTag("div", "class", "formal-contents"));
        if (is_para) {
            // Change <p class="..."> element to <div class="normal-para ...">  
            xw.add(createStartTag("div", toNormalParaAttributes(se.getAttributes())));
            readElement(se, xr, xw, false, entry, outConfig);  // copy content of p element unchanged
            xw.add(createEndTag("div"));
        } else {
            // Write user-defined div unchanged
            readElement(se, xr, xw, true, entry, outConfig);
        }
        xw.add(createEndTag("div"));
        if (isTitleAfter) {
            writeFormalTitle(xw, entry.getLabelNumber(), title, entry.getFormalId(), "p", langCode, genTextProps);
        }
        xw.add(createEndTag("div"));
    }

    private Iterator<Attribute> toNormalParaAttributes(Iterator source_attributes)
    {
            List<Attribute> atts = new ArrayList<Attribute>();
            boolean has_class = false;
            if (source_attributes != null) {
                while (source_attributes.hasNext()) {
                    Attribute a = (Attribute) source_attributes.next();
                    String attname = a.getName().getLocalPart();
                    if (attname.equalsIgnoreCase("class")) {  // add CSS class "normal-para" 
                        has_class = true;
                        String val = "normal-para " + a.getValue();
                        atts.add(eventFactory.createAttribute("class", val.trim()));
                    } else {
                        atts.add(a);  // add attribute unchanged
                    }
                }
            }
            if (! has_class) {
                atts.add(eventFactory.createAttribute("class", "normal-para"));
            }
            return atts.iterator();
    }
    
    private void writeFigureTitle(XMLEventWriter xw, 
                                  String labelNum, 
                                  String title, 
                                  String langCode, 
                                  Properties genTextProps) throws Exception
    {
        writeFormalTitle(xw, labelNum, title, "figure", "p", langCode, genTextProps);
    }
    
    private void writeTableTitle(XMLEventWriter xw, 
                                 String labelNum, 
                                 String title, 
                                 String langCode, 
                                 Properties genTextProps) throws Exception
    {
        writeFormalTitle(xw, labelNum, title, "table", null, langCode, genTextProps);
    }
    
    private void writeFormalTitle(XMLEventWriter xw, 
                                  String labelNum, 
                                  String title, 
                                  String formalId,
                                  String tagName,
                                  String langCode, 
                                  Properties genTextProps) throws Exception
    {
        if (labelNum == null) {
            labelNum = "";
        }
        if (title == null) {
            title = "";
        }
        String gen_key = "title|" + formalId;
        String pattern = getGenText(gen_key, genTextProps, langCode);
        String title_line;
        if ((pattern != null) && (pattern.length() > 0)) {
            title_line = pattern.replace("%n", labelNum).replace("%t", title);
        } else {
            title_line = labelNum.equals("") ? title : (labelNum + ". " + title);
        }
        if (title_line.length() > 0) {
            if (tagName != null) {
                writeXMLElement(xw, "<" + tagName + " class=\"title\">" + xmlEncode(title_line) + 
                                    "</" + tagName + ">");
            } else {
                writeText(xw, title_line);
            }
        }
    }

    private String getLabelName(String headerType, DocmaOutputConfig outConfig) throws Exception
    {
        if (headerType.equals("")) {
            return "";
        }
        String key;
        if (ParserTocEntry.TYPE_SECTION.equals(headerType)) {
            key = "Section";
        } else if (ParserTocEntry.TYPE_CHAPTER.equals(headerType)) {
            key = "Chapter";
        } else if (ParserTocEntry.TYPE_PREFACE.equals(headerType)) {
            key = "Preface";
        } else if (ParserTocEntry.TYPE_APPENDIX.equals(headerType)) {
            key = "Appendix";
        } else if (ParserTocEntry.TYPE_PART.equals(headerType)) {
            key = "Part";
        } else {
            key = headerType.substring(0, 1).toUpperCase() + headerType.substring(1);
        }
        
        String name = getGenText(key, outConfig);
        if ((name == null) || name.equals("")) {
            name = getGenText(headerType, outConfig);
            if ((name != null) && (name.length() > 0)) {
                name = name.substring(0, 1).toUpperCase() + name.substring(1);
            }
        }
        return (name == null) ? "" : name.trim();
    }
            
    private String getChunkHeaderText(String titleMode,
                                      ParserTocEntry currentEntry,
                                      ParserTocEntry upperEntry,
                                      ParserTocEntry tocRoot,
                                      Map<String, String> infoElems,
                                      DocmaOutputConfig outConfig) throws Exception
    {
        if ("publication".equals(titleMode)) {
            String pubTitle = infoElems.get("doc-title");
            if (pubTitle == null) { 
                pubTitle = tocRoot.getTitle();
            }
            return (pubTitle == null) ? "" : pubTitle;
        } else if ("firstlevel".equals(titleMode)) {
            ParserTocEntry first = currentEntry;
            while (first != null) {
                ParserTocEntry p = first.getParentEntry();
                if (p != null) {  // should always be true (if currentEntry is not tocRoot)
                    // first level is reached if p is the root element
                    if ((p == tocRoot) || (p.getParentEntry() == null)) {
                        break;
                    }
                }
                first = p;
            }
            return (first != null) ? getHeaderText(first, outConfig) : "";
        } else if ("upper".equals(titleMode)) {
            return getHeaderText(upperEntry, outConfig);
        } else if ("current".equals(titleMode)) {
            return getHeaderText(currentEntry, outConfig);
        } else if ("chapter".equals(titleMode)) {
            ParserTocEntry chap = currentEntry;
            while ((chap != null) && !chap.isChapter()) {
                chap = chap.getParentEntry();
            }
            return (chap != null) ? getHeaderText(chap, outConfig) : "";
        } else if ("part".equals(titleMode)) {
            ParserTocEntry prt = currentEntry;
            while ((prt != null) && !prt.isPart()) {
                prt = prt.getParentEntry();
            }
            return (prt != null) ? getHeaderText(prt, outConfig) : "";
        } else {  // none
            return "";
        }
    }

    private String getHeaderText(ParserTocEntry entry, DocmaOutputConfig outConfig) throws Exception
    {
        if (entry == null) {
            return "";
        }
        return getHeaderText(entry.getLabelNumber(), entry.getTitle(), entry.getType(), 
                             outConfig.getLanguageCode(), outConfig.getGentextProps());
    }
    
    private String getHeaderText(String labelNum,
                                 String title, 
                                 String headerType,
                                 String langCode, 
                                 Properties genTextProps) throws Exception
    {
        if (labelNum == null) {
            labelNum = "";
        }
        if (title == null) {
            title = "";
        }
        String gen_key = (labelNum.equals("") ? "title-unnumbered|" : "title-numbered|") + headerType;
        String pattern = getGenText(gen_key, genTextProps, langCode);
        String title_line;
        if ((pattern != null) && (pattern.contains("%t") || pattern.contains("%n"))) {
            title_line = pattern.replace("%n", labelNum).replace("%t", title);
        } else {
            title_line = labelNum.equals("") ? title : (labelNum + ". " + title);
        }
        return title_line;
    }
    
    private void writeHeader(XMLEventWriter xw, 
                             String labelNum, 
                             String title, 
                             String headerType,
                             String langCode, 
                             Properties genTextProps) throws Exception
    {
        String title_line = getHeaderText(labelNum, title, headerType, langCode, genTextProps);
        if (title_line.length() > 0) {
            xw.add(eventFactory.createCharacters(title_line));  // problem: & of entities is encoded as &amp; 
        }
    }
    
    private void writeHeader(XMLEventWriter xw, 
                             String tagname,
                             String aclass,
                             String aid,
                             String labelNum, 
                             String title, 
                             String headerType,
                             String langCode, 
                             Properties genTextProps) throws Exception
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(tagname);
        if ((aid != null) && (aid.length() > 0)) {
            sb.append(" id=\"").append(aid).append("\"");
        }
        if ((aclass != null) && (aclass.length() > 0)) {
            sb.append(" class=\"").append(aclass).append("\"");
        }
        sb.append(">"); 
        sb.append(xmlEncode(getHeaderText(labelNum, title, headerType, langCode, genTextProps))); 
        sb.append("</").append(tagname).append(">");
        writeXMLElement(xw, sb.toString());
    }
    
    private String xmlEncode(String txt) 
    {
        // Encode ampersand
        int pos = txt.indexOf('&');
        int len = txt.length();
        if (pos >= 0) {
            StringBuilder sb = new StringBuilder(len + 16);
            int copypos = 0;
            do {
                pos++;   // position after &
                sb.append(txt.substring(copypos, pos));
                copypos = pos;
                boolean is_entity = false;
                while (pos < len) {
                    char ch = txt.charAt(pos);
                    if (ch == ';') {
                        is_entity = (pos > copypos);
                        break;
                    }
                    if (Character.isWhitespace(ch)) {
                        break;
                    }
                    pos++;
                }
                
                // If & is not start of an entity then replace "&" by "&amp;".
                if (! is_entity) {  
                    sb.append("amp;");
                }
                
                // Search next occurence of &
                pos = txt.indexOf('&', copypos);
            } while (pos >= 0);  // skip if no more & exist
            
            // copy remaining characters after the last &
            if (copypos < len) {
                sb.append(txt.substring(copypos));
            }
            txt = sb.toString();
        }
        
        return txt.replace("<", "&lt;").replace(">", "&gt;");
    }
    
    private String xmlEncodeQuotedAtt(String value)
    {
        return xmlEncode(value).replace("\"", "&quot;");
    }
    
    private void writeXMLElement(XMLEventWriter xw, String xml) throws Exception
    {
        writeXML(xw, xml, true);
    }
    
    private void writeText(XMLEventWriter xw, String txt) throws Exception
    {
        // The writeXML() method requires a valid XML document. 
        // Therefore enclose txt with <span> element to get a valid XML document.
        // By passing argument false, the <span> will not be written to the output.
        writeXML(xw, "<span>" + xmlEncode(txt) + "</span>", false);
    }
    
    private void writeXML(XMLEventWriter xw, String xml, boolean write_tags) throws Exception
    {
        if ((xml != null) && (xml.length() > 0)) {
            XMLEventReader xr = xinFactory.createXMLEventReader(new StringReader(xml));
            int level = 0;
            while(xr.hasNext()) {
                XMLEvent e = xr.nextEvent();
                int etype = e.getEventType();
                switch (etype) {
                    case XMLEvent.START_ELEMENT:
                        level++;
                        if (write_tags) {
                            xw.add(e);
                        }
                        break;
                    case XMLEvent.END_ELEMENT:
                        level--;
                        if (write_tags) {
                            xw.add(e);
                        }
                        break;
                    default:
                        if (level > 0) {  // skip DOCUMENT_START, DOCUMENT_END and prolog events
                            xw.add(e);
                        }
                        break;
                }
            }
            xr.close();
        }
    }

    private String getFormalPrefix(ParserTocEntry entry)
    {
        // The sequential numbering of formal blocks shall restart in each chapter/appendix/... 
        // Therefore the prefix of the sequential number has to be the chapter/appendix... number.
        while ((entry != null) && entry.isSection()) {  // go back to chapter/appendix/... level
            entry = entry.getParentEntry();
        }
        // Note: If the formal block is outside of a chapter/appendix/... then
        // the prefix is the label of the part or an empty string (label of root entry).
        // In any case: the same sequence is used for the same prefix.
        return (entry == null) ? "" : entry.getLabelNumber();
    }
    
    private int calcDepth(ParserTocEntry entry) 
    {
        int depth = 0;
        entry = entry.getParentEntry();
        while (entry != null) {
            depth++;
            entry = entry.getParentEntry();
        }
        return depth;
    }
    
    private int calcSectionDepth(ParserTocEntry entry) 
    {
        int depth = 0;
        while ((entry != null) && entry.isSection()) {
            depth++;
            entry = entry.getParentEntry();
        }
        return depth;
    }
    
    private String readElementToString(StartElement start_elem, 
                                       XMLEventReader xr, 
                                       boolean enclosed, 
                                       ParserTocEntry tocEntry, 
                                       DocmaOutputConfig outConfig) throws Exception
    {
        StringWriter out = new StringWriter();
        readElement(start_elem, xr, out, enclosed, tocEntry, outConfig);
        return out.toString();
    }
    
    private void readElement(StartElement start_elem, 
                             XMLEventReader xr, 
                             Writer out, 
                             boolean enclosed, 
                             ParserTocEntry tocEntry, 
                             DocmaOutputConfig outConfig) throws Exception
    {
        XMLEventWriter xw = xoutFactory.createXMLEventWriter(out);
        readElement(start_elem, xr, xw, enclosed, tocEntry, outConfig);
    }
    
    private void readElement(StartElement start_elem, 
                             XMLEventReader xr, 
                             XMLEventWriter xw, 
                             boolean enclosed, 
                             ParserTocEntry tocEntry, 
                             DocmaOutputConfig outConfig) throws Exception
    {
        final String start_name = start_elem.getName().getLocalPart();
        if (enclosed) {
            xw.add(start_elem);
        }
        int level = 0;

        boolean finished = false;
        while ((! finished) && xr.hasNext()) {
            XMLEvent e = xr.nextEvent();
            int etype = e.getEventType();
            switch (etype) {
                case XMLEvent.START_ELEMENT: 
                    StartElement se = e.asStartElement();
                    String ename = se.getName().getLocalPart().toLowerCase();
                    String aid = null;
                    String aclass = null;
                    String atitle = null;
                    Iterator it = se.getAttributes();
                    while (it.hasNext()) {
                        Attribute att = (Attribute) it.next();
                        String attname = att.getName().getLocalPart();
                        if (attname.equalsIgnoreCase("id")) {
                            aid = att.getValue();
                        } else if (attname.equalsIgnoreCase("class")) {
                            aclass = att.getValue();
                        } else if (attname.equalsIgnoreCase("title")) {
                            atitle = att.getValue();
                        }
                    }
                    boolean is_nested_start_tag = ename.equalsIgnoreCase(start_name);
                    if (is_nested_start_tag) {
                        level++;
                    }
                    if (ename.equals("span")) {  // special handling for span (e.g. footnotes, index entries, ...)
                        if (processSpan(se, xr, xw, aid, aclass, atitle, tocEntry, outConfig)) {  
                            // If span element has been completely replaced and
                            // spans are nested, then undo increase of nesting level.
                            if (is_nested_start_tag) level--; 
                        }
                    } else {  // all other start tags are just copied to the output
                        xw.add(e); 
                    }
                    break;
                    
                case XMLEvent.END_ELEMENT: 
                    EndElement ee = e.asEndElement();
                    boolean close_start_tag = start_name.equalsIgnoreCase(ee.getName().getLocalPart());
                    if (close_start_tag) {
                        if (level == 0) {
                            finished = true;
                        } else {
                            level--;
                        }
                    }
                    if ((! finished) || enclosed) {
                        xw.add(e);
                    }
                    break;
                    
                default:
                    xw.add(e);  // write all other XML tags
                    break;
            }
        }
        xw.flush();
    }
    
    private String readText(StartElement start_elem, XMLEventReader xr) throws Exception
    {
        final String elem_name = start_elem.getName().getLocalPart();
        
        // StringBuilder buf = new StringBuilder();
        StringWriter out = new StringWriter();
        XMLEventWriter xw = xoutFactory.createXMLEventWriter(out);
        
        int level = 0;
        boolean finished = false;
        while ((! finished) && xr.hasNext()) {
            XMLEvent e = xr.nextEvent();
            int etype = e.getEventType();
            switch (etype) {
                case XMLEvent.START_ELEMENT: 
                    StartElement se = e.asStartElement();
                    if (elem_name.equalsIgnoreCase(se.getName().getLocalPart())) {
                        level++;
                    }
                    break;
                    
                case XMLEvent.END_ELEMENT: 
                    EndElement ee = e.asEndElement();
                    if (elem_name.equalsIgnoreCase(ee.getName().getLocalPart())) {
                        if (level == 0) {
                            finished = true;
                        } else {
                            level--;
                        }
                    }
                    break;
                    
                case XMLEvent.CHARACTERS:
                    xw.add(e);  // buf.append(e.asCharacters().getData());
                    break;

                case XMLEvent.ENTITY_REFERENCE:
                    xw.add(e);
                    break;
            }
        }
        xw.close();
        return out.toString();  // return buf.toString();
    }
    

    /* -------------  Utility methods  ------------------- */

    private String labelSeparator()
    {
        return ".";
    }
    
    private boolean containsCSSClass(String att_class, String class_name) 
    {
        if (att_class == null) {
            return false;
        }
        int pos = att_class.indexOf(class_name);
        if (pos < 0) {
            return false;
        }
        if (att_class.equals(class_name)) {
            return true;
        }
        return att_class.startsWith(class_name + " ") || 
               att_class.endsWith(" " + class_name) ||
               att_class.contains(" " + class_name + " ");
        // return ((pos == 0) || Character.isWhitespace(att_class.charAt(pos-1))) &&
        //        ((att_class.length() == pos + class_name.length()) || 
        //         Character.isWhitespace(att_class.charAt(pos + class_name.length())));
    }
    
    private String numFormat(String format) 
    {
        if ((format == null) || format.equals("")) return "1";
        return format;
    }

    private String formatComponentId(int seqnum) 
    {
        final String PATT = "00000000";
        String s = Integer.toString(seqnum);
        if (s.length() < PATT.length()) {
            s = PATT.substring(s.length()) + s;
        }
        return "comp" + s;
    }
    
    private StartElement createStartTag(String tagName, Iterator atts) 
    {
        return eventFactory.createStartElement(new QName(tagName), atts, null);
    }
    
    private StartElement createStartTag(String tagName, String... atts) 
    {
        return createStartTag(tagName, null, atts);
    }
    
    private StartElement createStartTag(String tagName, Iterator attsIterator, String... atts) 
    {
        List<Attribute> attList = new ArrayList<Attribute>();
        if (attsIterator != null) {
            while (attsIterator.hasNext()) {
                attList.add((Attribute) attsIterator.next());
            }
        }
        if ((atts != null) && (atts.length > 0)) {
            int idx = 0;
            while (idx < (atts.length - 1)) {
                String val = atts[idx + 1];
                if (val != null) {
                    attList.add(eventFactory.createAttribute(atts[idx], val));
                }
                idx += 2;
            }
        }
        return eventFactory.createStartElement(new QName(tagName), attList.iterator(), null);
    }
    
    private List<Attribute> copyAttributesExclude(Iterator<Attribute> atts, String excludeAtt)
    {
        List<Attribute> copied_atts = new ArrayList<Attribute>();
        while (atts.hasNext()) {
            Attribute a = atts.next();
            if (! a.getName().getLocalPart().equalsIgnoreCase(excludeAtt)) { 
               copied_atts.add(a);  // add all attributes except excludeAtt
           }
        }
        return copied_atts;
    }

    private EndElement createEndTag(String tagName) 
    {
        return eventFactory.createEndElement(new QName(tagName), null);
    }
    
    private Comment createComment(String content) 
    {
        return eventFactory.createComment(content);
    }
    
    private String getGenText(String key, DocmaOutputConfig outConfig, String default_value) throws Exception
    {
        String val = getGenText(key, outConfig);
        return ((val == null) || val.equals("")) ? default_value : val;
    }
    
    private String getGenText(String key, DocmaOutputConfig outConfig) throws Exception
    {
        return getGenText(key, outConfig.getGentextProps(), outConfig.getLanguageCode());
    }
    
    private String getGenText(String key, Properties props, String langCode) throws Exception
    {
        String val = (props == null) ? null : props.getProperty(key);
        if (val == null) {
            // If it's not a user-defined GenText property then get default values. 
            if ((langCode == null) || langCode.equals("")) {
                langCode = "en";  // fall back to English as default
            }
            Properties props_default = getGenTextProps(langCode);
            val = (props_default == null) ? null : props_default.getProperty(key);
            if ((val == null) && !langCode.equalsIgnoreCase("en")) {
                props_default = getGenTextProps("en");
                val = (props_default == null) ? null : props_default.getProperty(key);
            }
        }
        return val;
    }
    
    private synchronized Properties getGenTextProps(String langCode) throws Exception
    {
        langCode = ((langCode == null) || langCode.equals("")) ? "en" : langCode.toLowerCase();
        Properties props = defaultGenTextProps.get(langCode);
        if (props == null) {
            props = loadL10nAsProperties(langCode);
            defaultGenTextProps.put(langCode, props);
        }
        return props;
    }
    
    private Properties loadL10nAsProperties(String langCode) throws Exception
    {
        String fn = langCode + ".xml";
        File f = new File(this.l10nDir, fn);
        Properties props = null;
        if (f.exists()) {
            props = L10nConverter.getL10nAsProperties(f);
        }
        return props;
    }

    private void printToc(ParserTocEntry entry, String indent)
    {
        System.out.print(indent);
        System.out.println(entry.toString());
        for (int i=0; i < entry.getTargetElementCount(); i++) {
            printToc(entry.getTargetElement(i), indent + "  ");
        }
        for (int i=0; i < entry.getSubEntryCount(); i++) {
            printToc(entry.getSubEntry(i), indent + "  ");
        }
    }

    private String formatNumber(int number, String format)
    {
        if (format.equals("1")) {
            return Integer.toString(number);
        } else if (format.equals("A")) {
            return formatAlphaNumber(number);
        } else if (format.equals("a")) {
            return formatAlphaNumber(number).toLowerCase();
        } else if (format.equals("I")) {
            return formatRomanNumber(number);
        } else if (format.equals("i")) {
            return formatRomanNumber(number).toLowerCase();
        } else {
            return Integer.toString(number);
        }
    }
    
    private String formatRomanNumber(int val) 
    {
        final String[] huns = {"", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM"};
        final String[] tens = {"", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"};
        final String[] ones = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};

        StringBuilder res = new StringBuilder();
        //  Add 'M' until we drop below 1000.
        while (val >= 1000) {
            res.append('M');
            val -= 1000;
        }

        // Add each of the correct elements, adjusting as we go.
        res.append(huns[val/100]); 
        val = val % 100;
        res.append(tens[val/10]);
        val = val % 10;
        res.append(ones[val]);
        
        return res.toString();
    }
    
    private String formatAlphaNumber(int val)
    {
        final char[] table = { 'Z',  // z for zero
                               'A', 'B', 'C', 'D', 'E',
                               'F', 'G', 'H', 'I', 'J',
                               'K', 'L', 'M', 'N', 'O',
                               'P', 'Q', 'R', 'S', 'T',
                               'U', 'V', 'W', 'X', 'Y' }; 
        int radix = table.length;

        // Create a buffer to hold the result
        // TODO:  size of the table can be detereined by computing
        // logs of the radix.  For now, we fake it.
        char buf[] = new char[100];
    
        // next character to set in the buffer
        int charPos = buf.length - 1;  // work backward through buf[]
    
        // index in table of the last character that we stored
        int lookupIndex = 1;  // start off with anything other than zero to make correction work
    
        //                                          Correction number
        //
        //  Correction can take on exactly two values:
        //
        //          0       if the next character is to be emitted is usual
        //
        //      radix - 1
        //                  if the next char to be emitted should be one less than
        //                  you would expect
        //
        // For example, consider radix 10, where 1="A" and 10="J"
        //
        // In this scheme, we count: A, B, C ...   H, I, J (not A0 and certainly
        // not AJ), A1
        //
        // So, how do we keep from emitting AJ for 10?  After correctly emitting the
        // J, lookupIndex is zero.  We now compute a correction number of 9 (radix-1).
        // In the following line, we'll compute (val+correction) % radix, which is,
        // (val+9)/10.  By this time, val is 1, so we compute (1+9) % 10, which
        // is 10 % 10 or zero.  So, we'll prepare to emit "JJ", but then we'll
        // later suppress the leading J as representing zero (in the mod system,
        // it can represent either 10 or zero).  In summary, the correction value of
        // "radix-1" acts like "-1" when run through the mod operator, but with the
        // desireable characteristic that it never produces a negative number.
        int correction = 0;
    
        // TODO:  throw error on out of range input
        do
        {
    
            // most of the correction calculation is explained above,  the reason for the
            // term after the "|| " is that it correctly propagates carries across
            // multiple columns.
            correction =
              ((lookupIndex == 0) || (correction != 0 && lookupIndex == radix - 1))
              ? (radix - 1) : 0;
    
            // index in "table" of the next char to emit
            lookupIndex = (val + correction) % radix;
    
            // shift input by one "column"
            val = (val / radix);
    
            // if the next value we'd put out would be a leading zero, we're done.
            if (lookupIndex == 0 && val == 0) break;
    
            // put out the next character of output
            buf[charPos--] = table[lookupIndex];
        }
        while (val > 0);

        return new String(buf, charPos + 1, (buf.length - charPos - 1));
    }
    
 
}
