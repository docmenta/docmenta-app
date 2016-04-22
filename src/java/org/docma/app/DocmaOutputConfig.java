/*
 * DocmaOutputConfig.java
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

import org.docma.coreapi.*;
import org.docma.util.*;
import java.util.*;

/**
 *
 * @author MP
 */
public class DocmaOutputConfig
{
    // Properties to store general output options
    private static final String PROP_VERSION_OUTCONFIG_IDS = "docversion.outconfig.ids";
    private static final String PROP_VERSION_OUTCONFIG_FILTER = "docversion.outconfig.filter";
    private static final String PROP_VERSION_OUTCONFIG_TOC = "docversion.outconfig.toc";
    private static final String PROP_VERSION_OUTCONFIG_INDEX = "docversion.outconfig.index";
    private static final String PROP_VERSION_OUTCONFIG_MAXTOCDEPTH = "docversion.outconfig.maxtocdepth";
    private static final String PROP_VERSION_OUTCONFIG_TOCINDENT = "docversion.outconfig.tocindent";
    private static final String PROP_VERSION_OUTCONFIG_TOCNAMEDLABELS = "docversion.outconfig.tocnamedlabels";
    private static final String PROP_VERSION_OUTCONFIG_PARTTOC = "docversion.outconfig.parttoc";
    private static final String PROP_VERSION_OUTCONFIG_CHAPTERTOC = "docversion.outconfig.chaptertoc";
    private static final String PROP_VERSION_OUTCONFIG_SECTIONTOC = "docversion.outconfig.sectiontoc";
    private static final String PROP_VERSION_OUTCONFIG_SECTIONTOCLEVELS = "docversion.outconfig.sectiontoclevels";
    private static final String PROP_VERSION_OUTCONFIG_SECTIONTOCDEPTH = "docversion.outconfig.sectiontocdepth";
    private static final String PROP_VERSION_OUTCONFIG_STYLEVARIANT = "docversion.outconfig.stylevariant";
    private static final String PROP_VERSION_OUTCONFIG_FORMAT = "docversion.outconfig.format";
    private static final String PROP_VERSION_OUTCONFIG_SUBFORMAT = "docversion.outconfig.subformat";
    private static final String PROP_VERSION_OUTCONFIG_PARASPACE = "docversion.outconfig.paraspace";
    private static final String PROP_VERSION_OUTCONFIG_PARAINDENT = "docversion.outconfig.paraindent";
    private static final String PROP_VERSION_OUTCONFIG_ITEMSPACE = "docversion.outconfig.itemspace";
    private static final String PROP_VERSION_OUTCONFIG_LISTINDENT = "docversion.outconfig.listindent";
    private static final String PROP_VERSION_OUTCONFIG_ORDEREDLISTLABELWIDTH = "docversion.outconfig.orderedlistlabelwidth";
    private static final String PROP_VERSION_OUTCONFIG_TITLEPLACEMENT = "docversion.outconfig.titleplacement";
    
    // Numbering options
    private static final String PROP_VERSION_OUTCONFIG_RENDER1STLEVEL = "docversion.outconfig.render1stlevel";
    private static final String PROP_VERSION_OUTCONFIG_PARTNUMBER = "docversion.outconfig.partnumber";
    private static final String PROP_VERSION_OUTCONFIG_CHAPTERNUMBER = "docversion.outconfig.chapternumber";
    private static final String PROP_VERSION_OUTCONFIG_SECTIONNUMBER = "docversion.outconfig.sectionnumber";
    private static final String PROP_VERSION_OUTCONFIG_APPENDIXNUMBER = "docversion.outconfig.appendixnumber";
    private static final String PROP_VERSION_OUTCONFIG_FOOTNOTENUMBER = "docversion.outconfig.footnotenumber";
    private static final String PROP_VERSION_OUTCONFIG_NUMBERINGDEPTH = "docversion.outconfig.numberingdepth";
    private static final String PROP_VERSION_OUTCONFIG_OMITSINGLETITLE = "docversion.outconfig.omitsingletitle";
    private static final String PROP_VERSION_OUTCONFIG_EXCLUDE1STLEVELNUMBER = "docversion.outconfig.exclude1stlevelnumber";
    private static final String PROP_VERSION_OUTCONFIG_RESTARTPART = "docversion.outconfig.restartpart";
    
    // Properties to store HTML options
    private static final String PROP_VERSION_HTMLCONFIG_SINGLEFILE = "docversion.htmlconfig.singlefile";
    private static final String PROP_VERSION_HTMLCONFIG_FILEPREFIX = "docversion.htmlconfig.fileprefix";
    private static final String PROP_VERSION_HTMLCONFIG_SEPARATEFILELEVEL = "docversion.htmlconfig.separatefilelevel";
    private static final String PROP_VERSION_HTMLCONFIG_DIRLEVEL = "docversion.htmlconfig.dirlevel";
    private static final String PROP_VERSION_HTMLCONFIG_ALIASFILENAME = "docversion.htmlconfig.aliasfilename";
    private static final String PROP_VERSION_HTMLCONFIG_INCLUDE1STSECTION = "docversion.htmlconfig.include1stsection";
    private static final String PROP_VERSION_HTMLCONFIG_SEPARATETOC = "docversion.htmlconfig.separatetoc";
    private static final String PROP_VERSION_HTMLCONFIG_SEPARATEEACHTABLE = "docversion.htmlconfig.separateeachtable";
    private static final String PROP_VERSION_HTMLCONFIG_NAVIGATIONICONS = "docversion.htmlconfig.navigationicons";
    private static final String PROP_VERSION_HTMLCONFIG_NAVIGATIONTITLES = "docversion.htmlconfig.navigationtitles";
    private static final String PROP_VERSION_HTMLCONFIG_BREADCRUMBS = "docversion.htmlconfig.breadcrumbs";
    private static final String PROP_VERSION_HTMLCONFIG_BREADCRUMBSTART = "docversion.htmlconfig.breadcrumbstart";
    private static final String PROP_VERSION_HTMLCONFIG_BREADCRUMBSEPARATOR = "docversion.htmlconfig.breadcrumbseparator";
    private static final String PROP_VERSION_HTMLCONFIG_ROOTFOLDER = "docversion.htmlconfig.rootfolder";
    private static final String PROP_VERSION_HTMLCONFIG_URLTARGETWINDOW = "docversion.htmlconfig.urltargetwindow";
    private static final String PROP_VERSION_HTMLCONFIG_CUSTOMHEADER = "docversion.htmlconfig.customheader";
    private static final String PROP_VERSION_HTMLCONFIG_CUSTOMFOOTER = "docversion.htmlconfig.customfooter";
    private static final String PROP_VERSION_HTMLCONFIG_CUSTOMCSS = "docversion.htmlconfig.customcss";
    private static final String PROP_VERSION_HTMLCONFIG_CUSTOMJS = "docversion.htmlconfig.customjs";
    private static final String PROP_VERSION_HTMLCONFIG_CUSTOMMETA = "docversion.htmlconfig.custommeta";
    private static final String PROP_VERSION_HTMLCONFIG_CUSTOMFILES = "docversion.htmlconfig.customfiles";
    private static final String PROP_VERSION_HTMLCONFIG_OUTPUTENCODING = "docversion.htmlconfig.outputencoding";
    // private static final String PROP_VERSION_HTMLCONFIG_COVERIMAGE = "docversion.htmlconfig.coverimage";
    private static final String PROP_VERSION_HTMLCONFIG_WEBHELPCONFIG = "docversion.htmlconfig.webhelpconfig";
    private static final String PROP_VERSION_HTMLCONFIG_WEBHELPHEADER1 = "docversion.htmlconfig.webhelpheader1";
    private static final String PROP_VERSION_HTMLCONFIG_WEBHELPHEADER2 = "docversion.htmlconfig.webhelpheader2";
    
    // Properties to store PDF options
    private static final String PROP_VERSION_PDFCONFIG_PAPERSIZE = "docversion.pdfconfig.papersize";
    private static final String PROP_VERSION_PDFCONFIG_PAGEWIDTH = "docversion.pdfconfig.pagewidth";
    private static final String PROP_VERSION_PDFCONFIG_PAGEHEIGHT = "docversion.pdfconfig.pageheight";
    private static final String PROP_VERSION_PDFCONFIG_PAGEORIENTATION = "docversion.pdfconfig.pageorientation";
    private static final String PROP_VERSION_PDFCONFIG_PAGETOP = "docversion.pdfconfig.pagetop";
    private static final String PROP_VERSION_PDFCONFIG_PAGEBOTTOM = "docversion.pdfconfig.pagebottom";
    private static final String PROP_VERSION_PDFCONFIG_PAGEINNER = "docversion.pdfconfig.pageinner";
    private static final String PROP_VERSION_PDFCONFIG_PAGEOUTER = "docversion.pdfconfig.pageouter";
    private static final String PROP_VERSION_PDFCONFIG_BODYTOP = "docversion.pdfconfig.bodytop";
    private static final String PROP_VERSION_PDFCONFIG_BODYBOTTOM = "docversion.pdfconfig.bodybottom";
    private static final String PROP_VERSION_PDFCONFIG_BODYSTART = "docversion.pdfconfig.bodystart";
    private static final String PROP_VERSION_PDFCONFIG_BODYEND = "docversion.pdfconfig.bodyend";
    private static final String PROP_VERSION_PDFCONFIG_HEADERHEIGHT = "docversion.pdfconfig.headerheight";
    private static final String PROP_VERSION_PDFCONFIG_FOOTERHEIGHT = "docversion.pdfconfig.footerheight";
    private static final String PROP_VERSION_PDFCONFIG_DOUBLESIDED = "docversion.pdfconfig.doublesided";
    private static final String PROP_VERSION_PDFCONFIG_BOOKMARKS = "docversion.pdfconfig.bookmarks";
    private static final String PROP_VERSION_PDFCONFIG_NUMBERSINREFS = "docversion.pdfconfig.numbersinrefs";
    private static final String PROP_VERSION_PDFCONFIG_EXPORTFILES = "docversion.pdfconfig.exportfiles";
    private static final String PROP_VERSION_PDFCONFIG_FITIMAGES = "docversion.pdfconfig.fitimages";
    private static final String PROP_VERSION_PDFCONFIG_SHOWEXTERNALHREF = "docversion.pdfconfig.showexternalhref";
    private static final String PROP_VERSION_PDFCONFIG_SOURCERESOLUTION = "docversion.pdfconfig.sourceresolution";
    private static final String PROP_VERSION_PDFCONFIG_TARGETRESOLUTION = "docversion.pdfconfig.targetresolution";
    private static final String PROP_VERSION_PDFCONFIG_COVERMODE = "docversion.pdfconfig.covermode";
    private static final String PROP_VERSION_PDFCONFIG_COVERHORIZONTAL = "docversion.pdfconfig.coverhorizontal";
    private static final String PROP_VERSION_PDFCONFIG_COVERVERTICAL = "docversion.pdfconfig.coververtical";
    private static final String PROP_VERSION_PDFCONFIG_INDEXCOLUMNCOUNT = "docversion.pdfconfig.indexcolumncount";
    private static final String PROP_VERSION_PDFCONFIG_INDEXCOLUMNGAP = "docversion.pdfconfig.indexcolumngap";
    private static final String PROP_VERSION_PDFCONFIG_CUSTOMHEADERFOOTER = "docversion.pdfconfig.customheaderfooter";
    private static final String PROP_VERSION_PDFCONFIG_CUSTOMPAGETYPES = "docversion.pdfconfig.custompagetypes";
    private static final String PROP_VERSION_PDFCONFIG_HEADERWIDTHS = "docversion.pdfconfig.headerwidths";
    private static final String PROP_VERSION_PDFCONFIG_FOOTERWIDTHS = "docversion.pdfconfig.footerwidths";
    private static final String PROP_VERSION_PDFCONFIG_REGION = "docversion.pdfconfig.region";
    private static final String PROP_VERSION_PDFCONFIG_MARKERSECTIONLEVEL = "docversion.pdfconfig.markersectionlevel";

    // General output options
    private String  id;
    private String  filterSetting = "";
    private boolean toc = true;
    private boolean index = false;
    private int     maxTocDepth = 5;
    private String  tocIndent = "";  // use default
    private String  tocNamedLabels = "";   // comma separated list of values: "part", "chapter", "appendix"
    private boolean partToc = false;
    private boolean chapterToc = false;
    private boolean sectionToc = false;
    private int     sectionTocLevels = 2;
    private int     sectionTocDepth = 2;
    private String  styleVariant = "";
    private String  format = "html";
    private String  subformat = "";
    private String  paraSpace = DocmaConstants.DEFAULT_PARA_SPACE;   // "0.8em";
    private String  paraIndent = DocmaConstants.DEFAULT_PARA_INDENT; // "1.5em";
    private String  itemSpace = DocmaConstants.DEFAULT_ITEM_SPACE;   // "0.4em";
    private String  listIndent = DocmaConstants.DEFAULT_LIST_INDENT; // "2em";
    private String  orderedListLabelWidth = "";                    // undefined; use output dependent default
    private String  titlePlacement = "after";
    // Numbering options
    private String  render1stLevel = "chapter";
    private String  partNumbering = "I";
    private String  chapterNumbering = "1";
    private String  sectionNumbering = "1";
    private String  appendixNumbering = "A";
    private String  footnoteNumbering = "1";
    private int     numberingDepth = 3;
    private boolean omitSingleTitle = false;
    private boolean exclude1stLevelNumber = false;
    private boolean restartPart = false;
    // HTML options
    private boolean htmlSingleFile = true;
    private String  htmlFilePrefix = "";
    private int     htmlSeparateFileLevel = 0;
    private int     htmlDirLevel = 0;
    private boolean htmlAliasFilename = false;
    private boolean htmlInclude1stSection = false;
    private boolean htmlSeparateTOC = false;
    private boolean htmlSeparateEachTable = false;
    private boolean htmlNavigationIcons = false;
    private boolean htmlNavigationTitles = false;
    private boolean htmlBreadcrumbs = false;
    private String  htmlBreadcrumbStart = "publication";
    private String  htmlBreadcrumbSeparator = "&gt;";
    private String  htmlRootFolder = "";
    private String  htmlURLTargetWindow = DocmaConstants.DEFAULT_URL_TARGET_WINDOW;
    private String  htmlCustomHeaderAlias = null;
    private String  htmlCustomFooterAlias = null;
    private String  htmlCustomCSSFilename = null;
    private String  htmlCustomJSFilename = null;
    private String  htmlCustomMetaFilename = null;
    private String  htmlCustomFiles = null;
    private String  htmlOutputEncoding = null;
    // private String  htmlCoverImage = null;
    private String  htmlWebhelpConfig = null;
    private String  htmlWebhelpHeader1 = "upper";
    private String  htmlWebhelpHeader2 = "current";
    // PDF options
    private String  pdfPaperSize = "A4";
    private String  pdfPageWidth = "cm";
    private String  pdfPageHeight = "cm";
    private String  pdfPageOrientation = "portrait";
    private String  pdfPageTop = "cm";
    private String  pdfPageBottom = "cm";
    private String  pdfPageInner = "cm";
    private String  pdfPageOuter = "cm";
    private String  pdfBodyTop = "cm";
    private String  pdfBodyBottom = "cm";
    private String  pdfBodyStart = "0.0cm";
    private String  pdfBodyEnd = "0.0cm";
    private String  pdfHeaderHeight = "cm";
    private String  pdfFooterHeight = "cm";
    private boolean pdfDoubleSided = false;
    private boolean pdfBookmarks = true;
    private boolean pdfNumbersInRefs = false;
    private boolean pdfExportFiles = false;
    private boolean pdfFitImages = true;
    private String  pdfShowExternalHRef = "bracket";  // allowed values: no, bracket, footnote
    private int     pdfSourceResolution = 96;  // dpi
    private int     pdfTargetResolution = 300;  // dpi
    private String  pdfCoverMode = "";       // allowed values: extrapage, extrapage_blank, titlepage
    private String  pdfCoverHorizontal = "";   // e.g. "0pt", empty string means centered
    private String  pdfCoverVertical = "";     // e.g. "0pt", empty string means centered
    private int     pdfIndexColumnCount = 2;
    private String  pdfIndexColumnGap = "12pt";
    private boolean pdfCustomHeaderFooter = false;
    private String  pdfHeaderWidths = null;
    private String  pdfFooterWidths = null;
    private int     pdfMarkerSectionLevel = 2;
    private Set<String> pdfCustomPageTypes = null;
    private Map<HeaderFooterKey, String> pdfHeaderFooterContent = null;

    // transient package local options (used to control the formatter)
    private String languageCode = "en";   // English is default
    private String draftImagePath = "";
    private String coverImagePath = "";
    private Properties gentextProps = null;
    private boolean showIndexTerms = false;
    private String[] effectiveFilterApplics = null;
    private ApplicEvaluator applicEvaluator = null;
    private String htmlCustomHeaderContent = null;
    private String htmlCustomFooterContent = null;
    private String htmlCustomHeadTags = null;
    private String htmlNavigationIconsPath = null;
    private String htmlNavigationIconsExt = null;
    private String pdfCustomHeaderFooterXsl = null;
    private String pdfCustomFOPConfig = null;


    /* -----------  Public constructor  ------------------ */

    public DocmaOutputConfig(String outConfigId)
    {
        id = outConfigId;
    }

    /* -----------  Package local  ------------------ */

    static String[] getIds(DocmaSession docmaSess)
    {
        String sid = docmaSess.getStoreId();
        DocVersionId vid = docmaSess.getVersionId();
        String pub_ids = docmaSess.getVersionProperty(sid, vid, PROP_VERSION_OUTCONFIG_IDS);
        String[] id_arr;
        if ((pub_ids == null) || (pub_ids.trim().length() == 0)) {
            id_arr = new String[0];
        } else {
            id_arr = pub_ids.split(",");
        }
        return id_arr;
    }

    static boolean createId(DocmaSession docmaSess, String outConfId) throws DocException
    {
        String sid = docmaSess.getStoreId();
        DocVersionId vid = docmaSess.getVersionId();
        List id_list = Arrays.asList(getIds(docmaSess));
        if (! id_list.contains(outConfId)) {
            // add new output configuration to list
            List ids_new = new ArrayList(id_list);
            ids_new.add(outConfId);
            Collections.sort(ids_new);
            String out_ids = DocmaUtil.concatStrings(ids_new, ",");
            docmaSess.setVersionProperty(sid, vid, PROP_VERSION_OUTCONFIG_IDS, out_ids);
            return true;
        } else {
            return false;
        }
    }

    void init(DocmaSession docmaSess, String outputId)
    {
        id = outputId;
        // String sid = docmaSess.getStoreId();
        // DocVersionId vid = docmaSess.getVersionId();
        
        // General options
        filterSetting         = getStringProp(docmaSess, PROP_VERSION_OUTCONFIG_FILTER);
        toc                   = getBoolProp(docmaSess, PROP_VERSION_OUTCONFIG_TOC);
        index                 = getBoolProp(docmaSess, PROP_VERSION_OUTCONFIG_INDEX);
        maxTocDepth           = getIntProp(docmaSess, PROP_VERSION_OUTCONFIG_MAXTOCDEPTH);
        tocIndent             = getStringProp(docmaSess, PROP_VERSION_OUTCONFIG_TOCINDENT);
        tocNamedLabels        = getStringProp(docmaSess, PROP_VERSION_OUTCONFIG_TOCNAMEDLABELS);
        partToc               = getBoolProp(docmaSess, PROP_VERSION_OUTCONFIG_PARTTOC);
        chapterToc            = getBoolProp(docmaSess, PROP_VERSION_OUTCONFIG_CHAPTERTOC);
        sectionToc            = getBoolProp(docmaSess, PROP_VERSION_OUTCONFIG_SECTIONTOC);
        sectionTocLevels      = getIntProp(docmaSess, PROP_VERSION_OUTCONFIG_SECTIONTOCLEVELS);
        sectionTocDepth       = getIntProp(docmaSess, PROP_VERSION_OUTCONFIG_SECTIONTOCDEPTH);
        styleVariant          = getStringProp(docmaSess, PROP_VERSION_OUTCONFIG_STYLEVARIANT);
        format                = getStringProp(docmaSess, PROP_VERSION_OUTCONFIG_FORMAT);
        subformat             = getStringProp(docmaSess, PROP_VERSION_OUTCONFIG_SUBFORMAT);
        paraSpace             = getStringProp(docmaSess, PROP_VERSION_OUTCONFIG_PARASPACE);
        paraIndent            = getStringProp(docmaSess, PROP_VERSION_OUTCONFIG_PARAINDENT);
        itemSpace             = getStringProp(docmaSess, PROP_VERSION_OUTCONFIG_ITEMSPACE);
        listIndent            = getStringProp(docmaSess, PROP_VERSION_OUTCONFIG_LISTINDENT);
        orderedListLabelWidth = getStringProp(docmaSess, PROP_VERSION_OUTCONFIG_ORDEREDLISTLABELWIDTH);
        titlePlacement        = getStringProp(docmaSess, PROP_VERSION_OUTCONFIG_TITLEPLACEMENT);
        // Numbering options
        render1stLevel        = getStringProp(docmaSess, PROP_VERSION_OUTCONFIG_RENDER1STLEVEL);
        partNumbering         = getStringProp(docmaSess, PROP_VERSION_OUTCONFIG_PARTNUMBER);
        chapterNumbering      = getStringProp(docmaSess, PROP_VERSION_OUTCONFIG_CHAPTERNUMBER);
        sectionNumbering      = getStringProp(docmaSess, PROP_VERSION_OUTCONFIG_SECTIONNUMBER);
        appendixNumbering     = getStringProp(docmaSess, PROP_VERSION_OUTCONFIG_APPENDIXNUMBER);
        footnoteNumbering     = getStringProp(docmaSess, PROP_VERSION_OUTCONFIG_FOOTNOTENUMBER);
        numberingDepth        = getIntProp(docmaSess, PROP_VERSION_OUTCONFIG_NUMBERINGDEPTH);
        omitSingleTitle       = getBoolProp(docmaSess, PROP_VERSION_OUTCONFIG_OMITSINGLETITLE);
        exclude1stLevelNumber = getBoolProp(docmaSess, PROP_VERSION_OUTCONFIG_EXCLUDE1STLEVELNUMBER);
        restartPart           = getBoolProp(docmaSess, PROP_VERSION_OUTCONFIG_RESTARTPART);
        // HTML options
        if (format.equals("html")) {
            htmlSingleFile        = getBoolProp(docmaSess, PROP_VERSION_HTMLCONFIG_SINGLEFILE);
            htmlFilePrefix        = getStringProp(docmaSess, PROP_VERSION_HTMLCONFIG_FILEPREFIX);
            htmlSeparateFileLevel = getIntProp(docmaSess, PROP_VERSION_HTMLCONFIG_SEPARATEFILELEVEL);
            htmlDirLevel          = getIntProp(docmaSess, PROP_VERSION_HTMLCONFIG_DIRLEVEL);
            htmlAliasFilename     = getBoolProp(docmaSess, PROP_VERSION_HTMLCONFIG_ALIASFILENAME);
            htmlInclude1stSection = getBoolProp(docmaSess, PROP_VERSION_HTMLCONFIG_INCLUDE1STSECTION);
            htmlSeparateTOC       = getBoolProp(docmaSess, PROP_VERSION_HTMLCONFIG_SEPARATETOC);
            htmlSeparateEachTable = getBoolProp(docmaSess, PROP_VERSION_HTMLCONFIG_SEPARATEEACHTABLE);
            htmlNavigationIcons   = getBoolProp(docmaSess, PROP_VERSION_HTMLCONFIG_NAVIGATIONICONS);
            htmlNavigationTitles  = getBoolProp(docmaSess, PROP_VERSION_HTMLCONFIG_NAVIGATIONTITLES);
            htmlBreadcrumbs       = getBoolProp(docmaSess, PROP_VERSION_HTMLCONFIG_BREADCRUMBS);
            htmlBreadcrumbStart   = getStringProp(docmaSess, PROP_VERSION_HTMLCONFIG_BREADCRUMBSTART);
            htmlBreadcrumbSeparator = getStringProp(docmaSess, PROP_VERSION_HTMLCONFIG_BREADCRUMBSEPARATOR);
            htmlRootFolder        = getStringProp(docmaSess, PROP_VERSION_HTMLCONFIG_ROOTFOLDER);
            htmlURLTargetWindow   = getStringProp(docmaSess, PROP_VERSION_HTMLCONFIG_URLTARGETWINDOW);
            htmlCustomHeaderAlias = getStringProp(docmaSess, PROP_VERSION_HTMLCONFIG_CUSTOMHEADER);
            htmlCustomFooterAlias = getStringProp(docmaSess, PROP_VERSION_HTMLCONFIG_CUSTOMFOOTER);
            htmlCustomCSSFilename = getStringProp(docmaSess, PROP_VERSION_HTMLCONFIG_CUSTOMCSS);
            htmlCustomJSFilename  = getStringProp(docmaSess, PROP_VERSION_HTMLCONFIG_CUSTOMJS);
            htmlCustomMetaFilename= getStringProp(docmaSess, PROP_VERSION_HTMLCONFIG_CUSTOMMETA);
            htmlCustomFiles       = getStringProp(docmaSess, PROP_VERSION_HTMLCONFIG_CUSTOMFILES);
            htmlOutputEncoding    = getStringProp(docmaSess, PROP_VERSION_HTMLCONFIG_OUTPUTENCODING);
            // htmlCoverImage        = getStringProp(docmaSess, PROP_VERSION_HTMLCONFIG_COVERIMAGE);
            htmlWebhelpConfig     = getStringProp(docmaSess, PROP_VERSION_HTMLCONFIG_WEBHELPCONFIG);
            htmlWebhelpHeader1    = getStringProp(docmaSess, PROP_VERSION_HTMLCONFIG_WEBHELPHEADER1);
            htmlWebhelpHeader2    = getStringProp(docmaSess, PROP_VERSION_HTMLCONFIG_WEBHELPHEADER2);
        } else
        // PDF options
        if (format.equals("pdf")) {
            pdfPaperSize          = getStringProp(docmaSess, PROP_VERSION_PDFCONFIG_PAPERSIZE);
            pdfPageWidth          = getStringProp(docmaSess, PROP_VERSION_PDFCONFIG_PAGEWIDTH);
            pdfPageHeight         = getStringProp(docmaSess, PROP_VERSION_PDFCONFIG_PAGEHEIGHT);
            pdfPageOrientation    = getStringProp(docmaSess, PROP_VERSION_PDFCONFIG_PAGEORIENTATION);
            pdfPageTop            = getStringProp(docmaSess, PROP_VERSION_PDFCONFIG_PAGETOP);
            pdfPageBottom         = getStringProp(docmaSess, PROP_VERSION_PDFCONFIG_PAGEBOTTOM);
            pdfPageInner          = getStringProp(docmaSess, PROP_VERSION_PDFCONFIG_PAGEINNER);
            pdfPageOuter          = getStringProp(docmaSess, PROP_VERSION_PDFCONFIG_PAGEOUTER);
            pdfBodyTop            = getStringProp(docmaSess, PROP_VERSION_PDFCONFIG_BODYTOP);
            pdfBodyBottom         = getStringProp(docmaSess, PROP_VERSION_PDFCONFIG_BODYBOTTOM);
            pdfBodyStart          = getStringProp(docmaSess, PROP_VERSION_PDFCONFIG_BODYSTART);
            pdfBodyEnd            = getStringProp(docmaSess, PROP_VERSION_PDFCONFIG_BODYEND);
            pdfHeaderHeight       = getStringProp(docmaSess, PROP_VERSION_PDFCONFIG_HEADERHEIGHT);
            pdfFooterHeight       = getStringProp(docmaSess, PROP_VERSION_PDFCONFIG_FOOTERHEIGHT);
            pdfDoubleSided        = getBoolProp(docmaSess, PROP_VERSION_PDFCONFIG_DOUBLESIDED);
            pdfBookmarks          = getBoolProp(docmaSess, PROP_VERSION_PDFCONFIG_BOOKMARKS);
            pdfNumbersInRefs      = getBoolProp(docmaSess, PROP_VERSION_PDFCONFIG_NUMBERSINREFS);
            pdfExportFiles        = getBoolProp(docmaSess, PROP_VERSION_PDFCONFIG_EXPORTFILES);
            pdfFitImages          = getBoolProp(docmaSess, PROP_VERSION_PDFCONFIG_FITIMAGES);
            pdfShowExternalHRef   = getStringProp(docmaSess, PROP_VERSION_PDFCONFIG_SHOWEXTERNALHREF);
            pdfSourceResolution   = getIntProp(docmaSess, PROP_VERSION_PDFCONFIG_SOURCERESOLUTION);
            pdfTargetResolution   = getIntProp(docmaSess, PROP_VERSION_PDFCONFIG_TARGETRESOLUTION);
            pdfCoverMode          = getStringProp(docmaSess, PROP_VERSION_PDFCONFIG_COVERMODE);
            pdfCoverHorizontal    = getStringProp(docmaSess, PROP_VERSION_PDFCONFIG_COVERHORIZONTAL);
            pdfCoverVertical      = getStringProp(docmaSess, PROP_VERSION_PDFCONFIG_COVERVERTICAL);
            pdfIndexColumnCount   = getIntProp(docmaSess, PROP_VERSION_PDFCONFIG_INDEXCOLUMNCOUNT);
            pdfIndexColumnGap     = getStringProp(docmaSess, PROP_VERSION_PDFCONFIG_INDEXCOLUMNGAP);
            pdfCustomHeaderFooter = getBoolProp(docmaSess, PROP_VERSION_PDFCONFIG_CUSTOMHEADERFOOTER);
            pdfHeaderWidths       = getStringProp(docmaSess, PROP_VERSION_PDFCONFIG_HEADERWIDTHS);
            pdfFooterWidths       = getStringProp(docmaSess, PROP_VERSION_PDFCONFIG_FOOTERWIDTHS);
            pdfMarkerSectionLevel = getIntProp(docmaSess, PROP_VERSION_PDFCONFIG_MARKERSECTIONLEVEL, 2);
            decodePdfCustomHeaderFooterPageTypes(getStringProp(docmaSess, PROP_VERSION_PDFCONFIG_CUSTOMPAGETYPES));
            readPdfHeaderFooterConfig(docmaSess, getPdfCustomHeaderFooterPageTypes());
        }
    }

    void save(DocmaSession docmaSess) throws DocException
    {
        String sid = docmaSess.getStoreId();
        DocVersionId vid = docmaSess.getVersionId();
        createId(docmaSess, this.id);   // create new configuration if not already existent
        List nl = new ArrayList(100);
        List vl = new ArrayList(100);
        // General options
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_FILTER, filterSetting);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_TOC, "" + toc);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_INDEX, "" + index);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_MAXTOCDEPTH, "" + maxTocDepth);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_TOCINDENT, tocIndent);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_TOCNAMEDLABELS, tocNamedLabels);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_PARTTOC, "" + partToc);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_CHAPTERTOC, "" + chapterToc);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_SECTIONTOC, "" + sectionToc);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_SECTIONTOCLEVELS, "" + sectionTocLevels);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_SECTIONTOCDEPTH, "" + sectionTocDepth);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_STYLEVARIANT, styleVariant);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_FORMAT, format);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_SUBFORMAT, subformat);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_PARASPACE, paraSpace);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_PARAINDENT, paraIndent);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_ITEMSPACE, itemSpace);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_LISTINDENT, listIndent);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_ORDEREDLISTLABELWIDTH, orderedListLabelWidth);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_TITLEPLACEMENT, titlePlacement);
        // Numbering options
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_RENDER1STLEVEL, render1stLevel);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_PARTNUMBER, partNumbering);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_CHAPTERNUMBER, chapterNumbering);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_SECTIONNUMBER, sectionNumbering);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_APPENDIXNUMBER, appendixNumbering);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_FOOTNOTENUMBER, footnoteNumbering);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_NUMBERINGDEPTH, "" + numberingDepth);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_OMITSINGLETITLE, "" + omitSingleTitle);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_EXCLUDE1STLEVELNUMBER, "" + exclude1stLevelNumber);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_RESTARTPART, "" + restartPart);
        if (format.equals("html")) {  // HTML options
            addProp(nl, vl, PROP_VERSION_HTMLCONFIG_SINGLEFILE, "" + htmlSingleFile);
            addProp(nl, vl, PROP_VERSION_HTMLCONFIG_FILEPREFIX, htmlFilePrefix);
            addProp(nl, vl, PROP_VERSION_HTMLCONFIG_SEPARATEFILELEVEL, "" + htmlSeparateFileLevel);
            addProp(nl, vl, PROP_VERSION_HTMLCONFIG_DIRLEVEL, "" + htmlDirLevel);
            addProp(nl, vl, PROP_VERSION_HTMLCONFIG_ALIASFILENAME, "" + htmlAliasFilename);
            addProp(nl, vl, PROP_VERSION_HTMLCONFIG_INCLUDE1STSECTION, "" + htmlInclude1stSection);
            addProp(nl, vl, PROP_VERSION_HTMLCONFIG_SEPARATETOC, "" + htmlSeparateTOC);
            addProp(nl, vl, PROP_VERSION_HTMLCONFIG_SEPARATEEACHTABLE, "" + htmlSeparateEachTable);
            addProp(nl, vl, PROP_VERSION_HTMLCONFIG_NAVIGATIONICONS, "" + htmlNavigationIcons);
            addProp(nl, vl, PROP_VERSION_HTMLCONFIG_NAVIGATIONTITLES, "" + htmlNavigationTitles);
            addProp(nl, vl, PROP_VERSION_HTMLCONFIG_BREADCRUMBS, "" + htmlBreadcrumbs);
            addProp(nl, vl, PROP_VERSION_HTMLCONFIG_BREADCRUMBSTART, htmlBreadcrumbStart);
            addProp(nl, vl, PROP_VERSION_HTMLCONFIG_BREADCRUMBSEPARATOR, htmlBreadcrumbSeparator);
            addProp(nl, vl, PROP_VERSION_HTMLCONFIG_ROOTFOLDER, htmlRootFolder);
            addProp(nl, vl, PROP_VERSION_HTMLCONFIG_URLTARGETWINDOW, htmlURLTargetWindow);
            addProp(nl, vl, PROP_VERSION_HTMLCONFIG_CUSTOMHEADER, htmlCustomHeaderAlias);
            addProp(nl, vl, PROP_VERSION_HTMLCONFIG_CUSTOMFOOTER, htmlCustomFooterAlias);
            addProp(nl, vl, PROP_VERSION_HTMLCONFIG_CUSTOMCSS, htmlCustomCSSFilename);
            addProp(nl, vl, PROP_VERSION_HTMLCONFIG_CUSTOMJS, htmlCustomJSFilename);
            addProp(nl, vl, PROP_VERSION_HTMLCONFIG_CUSTOMMETA, htmlCustomMetaFilename);
            addProp(nl, vl, PROP_VERSION_HTMLCONFIG_CUSTOMFILES, htmlCustomFiles);
            addProp(nl, vl, PROP_VERSION_HTMLCONFIG_OUTPUTENCODING, htmlOutputEncoding);
            // addProp(nl, vl, PROP_VERSION_HTMLCONFIG_COVERIMAGE, htmlCoverImage);
            addProp(nl, vl, PROP_VERSION_HTMLCONFIG_WEBHELPCONFIG, htmlWebhelpConfig);
            addProp(nl, vl, PROP_VERSION_HTMLCONFIG_WEBHELPHEADER1, htmlWebhelpHeader1);
            addProp(nl, vl, PROP_VERSION_HTMLCONFIG_WEBHELPHEADER2, htmlWebhelpHeader2);
        } else
        if (format.equals("pdf")) {  // PDF options
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_PAPERSIZE, pdfPaperSize);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_PAGEWIDTH, pdfPageWidth);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_PAGEHEIGHT, pdfPageHeight);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_PAGEORIENTATION, pdfPageOrientation);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_PAGETOP, pdfPageTop);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_PAGEBOTTOM, pdfPageBottom);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_PAGEINNER, pdfPageInner);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_PAGEOUTER, pdfPageOuter);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_BODYTOP, pdfBodyTop);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_BODYBOTTOM, pdfBodyBottom);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_BODYSTART, pdfBodyStart);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_BODYEND, pdfBodyEnd);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_HEADERHEIGHT, pdfHeaderHeight);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_FOOTERHEIGHT, pdfFooterHeight);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_DOUBLESIDED, "" + pdfDoubleSided);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_BOOKMARKS, "" + pdfBookmarks);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_NUMBERSINREFS, "" + pdfNumbersInRefs);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_EXPORTFILES, "" + pdfExportFiles);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_FITIMAGES, "" + pdfFitImages);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_SHOWEXTERNALHREF, pdfShowExternalHRef);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_SOURCERESOLUTION, "" + pdfSourceResolution);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_TARGETRESOLUTION, "" + pdfTargetResolution);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_COVERMODE, pdfCoverMode);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_COVERHORIZONTAL, pdfCoverHorizontal);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_COVERVERTICAL, pdfCoverVertical);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_INDEXCOLUMNCOUNT, "" + pdfIndexColumnCount);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_INDEXCOLUMNGAP, pdfIndexColumnGap);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_CUSTOMHEADERFOOTER, "" + pdfCustomHeaderFooter);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_HEADERWIDTHS, (pdfHeaderWidths == null) ? "" : pdfHeaderWidths);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_FOOTERWIDTHS, (pdfFooterWidths == null) ? "" : pdfFooterWidths);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_MARKERSECTIONLEVEL, "" + pdfMarkerSectionLevel);
            addProp(nl, vl, PROP_VERSION_PDFCONFIG_CUSTOMPAGETYPES, encodePdfCustomHeaderFooterPageTypes());
            if (pdfHeaderFooterContent != null) {
                Iterator<HeaderFooterKey> it = pdfHeaderFooterContent.keySet().iterator();
                while (it.hasNext()) {
                    HeaderFooterKey key = it.next();
                    String val = pdfHeaderFooterContent.get(key);
                    addProp(nl, vl, PROP_VERSION_PDFCONFIG_REGION + "_" + key.getKeyString(), val);
                }
            }
        }
        String[] p_names = new String[nl.size()];
        String[] p_values = new String[vl.size()];
        p_names = (String[]) nl.toArray(p_names);
        p_values = (String[]) vl.toArray(p_values);
        docmaSess.setVersionProperties(sid, vid, p_names, p_values);
    }

    void delete(DocmaSession docmaSess) throws DocException
    {
        String sid = docmaSess.getStoreId();
        DocVersionId vid = docmaSess.getVersionId();
        String out_ids = docmaSess.getVersionProperty(sid, vid, PROP_VERSION_OUTCONFIG_IDS);
        if ((out_ids == null) || (out_ids.trim().length() == 0)) {
            return;
        }
        String[] id_arr = out_ids.split(",");
        List id_list = new ArrayList(Arrays.asList(id_arr));
        id_list.remove(this.id);
        out_ids = DocmaUtil.concatStrings(id_list, ",");
        List nl = new ArrayList(100);
        List vl = new ArrayList(100);
        nl.add(PROP_VERSION_OUTCONFIG_IDS);
        vl.add(out_ids);
        // General and numbering options
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_FILTER, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_TOC, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_INDEX, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_MAXTOCDEPTH, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_TOCINDENT, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_TOCNAMEDLABELS, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_PARTTOC, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_CHAPTERTOC, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_SECTIONTOC, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_SECTIONTOCLEVELS, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_SECTIONTOCDEPTH, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_STYLEVARIANT, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_FORMAT, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_SUBFORMAT, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_PARASPACE, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_PARAINDENT, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_ITEMSPACE, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_LISTINDENT, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_ORDEREDLISTLABELWIDTH, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_TITLEPLACEMENT, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_RENDER1STLEVEL, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_PARTNUMBER, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_CHAPTERNUMBER, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_SECTIONNUMBER, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_APPENDIXNUMBER, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_FOOTNOTENUMBER, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_NUMBERINGDEPTH, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_OMITSINGLETITLE, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_EXCLUDE1STLEVELNUMBER, null);
        addProp(nl, vl, PROP_VERSION_OUTCONFIG_RESTARTPART, null);
        // HTML options
        addProp(nl, vl, PROP_VERSION_HTMLCONFIG_SINGLEFILE, null);
        addProp(nl, vl, PROP_VERSION_HTMLCONFIG_FILEPREFIX, null);
        addProp(nl, vl, PROP_VERSION_HTMLCONFIG_SEPARATEFILELEVEL, null);
        addProp(nl, vl, PROP_VERSION_HTMLCONFIG_DIRLEVEL, null);
        addProp(nl, vl, PROP_VERSION_HTMLCONFIG_ALIASFILENAME, null);
        addProp(nl, vl, PROP_VERSION_HTMLCONFIG_INCLUDE1STSECTION, null);
        addProp(nl, vl, PROP_VERSION_HTMLCONFIG_SEPARATETOC, null);
        addProp(nl, vl, PROP_VERSION_HTMLCONFIG_SEPARATEEACHTABLE, null);
        addProp(nl, vl, PROP_VERSION_HTMLCONFIG_NAVIGATIONICONS, null);
        addProp(nl, vl, PROP_VERSION_HTMLCONFIG_NAVIGATIONTITLES, null);
        addProp(nl, vl, PROP_VERSION_HTMLCONFIG_BREADCRUMBS, null);
        addProp(nl, vl, PROP_VERSION_HTMLCONFIG_BREADCRUMBSTART, null);
        addProp(nl, vl, PROP_VERSION_HTMLCONFIG_BREADCRUMBSEPARATOR, null);
        addProp(nl, vl, PROP_VERSION_HTMLCONFIG_ROOTFOLDER, null);
        addProp(nl, vl, PROP_VERSION_HTMLCONFIG_URLTARGETWINDOW, null);
        addProp(nl, vl, PROP_VERSION_HTMLCONFIG_CUSTOMHEADER, null);
        addProp(nl, vl, PROP_VERSION_HTMLCONFIG_CUSTOMFOOTER, null);
        addProp(nl, vl, PROP_VERSION_HTMLCONFIG_CUSTOMCSS, null);
        addProp(nl, vl, PROP_VERSION_HTMLCONFIG_CUSTOMJS, null);
        addProp(nl, vl, PROP_VERSION_HTMLCONFIG_CUSTOMMETA, null);
        addProp(nl, vl, PROP_VERSION_HTMLCONFIG_CUSTOMFILES, null);
        addProp(nl, vl, PROP_VERSION_HTMLCONFIG_OUTPUTENCODING, null);
        // addProp(nl, vl, PROP_VERSION_HTMLCONFIG_COVERIMAGE, null);
        addProp(nl, vl, PROP_VERSION_HTMLCONFIG_WEBHELPCONFIG, null);
        addProp(nl, vl, PROP_VERSION_HTMLCONFIG_WEBHELPHEADER1, null);
        addProp(nl, vl, PROP_VERSION_HTMLCONFIG_WEBHELPHEADER2, null);
        // PDF options
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_PAPERSIZE, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_PAGEWIDTH, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_PAGEHEIGHT, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_PAGEORIENTATION, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_PAGETOP, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_PAGEBOTTOM, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_PAGEINNER, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_PAGEOUTER, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_BODYTOP, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_BODYBOTTOM, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_BODYSTART, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_BODYEND, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_HEADERHEIGHT, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_FOOTERHEIGHT, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_DOUBLESIDED, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_BOOKMARKS, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_NUMBERSINREFS, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_EXPORTFILES, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_FITIMAGES, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_SHOWEXTERNALHREF, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_SOURCERESOLUTION, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_TARGETRESOLUTION, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_COVERMODE, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_COVERHORIZONTAL, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_COVERVERTICAL, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_INDEXCOLUMNCOUNT, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_INDEXCOLUMNGAP, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_CUSTOMHEADERFOOTER, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_HEADERWIDTHS, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_FOOTERWIDTHS, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_MARKERSECTIONLEVEL, null);
        addProp(nl, vl, PROP_VERSION_PDFCONFIG_CUSTOMPAGETYPES, null);
        if (pdfHeaderFooterContent != null) {
            Iterator<HeaderFooterKey> it = pdfHeaderFooterContent.keySet().iterator();
            while (it.hasNext()) {
                HeaderFooterKey key = it.next();
                addProp(nl, vl, PROP_VERSION_PDFCONFIG_REGION + "_" + key.getKeyString(), null);
            }
        }
        
        String[] p_names = new String[nl.size()];
        String[] p_values = new String[vl.size()];
        p_names = (String[]) nl.toArray(p_names);
        p_values = (String[]) vl.toArray(p_values);
        docmaSess.setVersionProperties(sid, vid, p_names, p_values);
    }

    String getLanguageCode() {
        return languageCode;
    }

    void setLanguageCode(String lang_code) {
        this.languageCode = lang_code;
    }

    String getDraftImagePath() {
        return draftImagePath;
    }

    void setDraftImagePath(String image_path) {
        this.draftImagePath = image_path;
    }

    String getCoverImagePath() {
        return coverImagePath;
    }

    void setCoverImagePath(String image_path) {
        this.coverImagePath = image_path;
    }

    Properties getGentextProps() {
        return gentextProps;
    }

    void setGentextProps(Properties gentext_props) {
        this.gentextProps = gentext_props;
    }

    String[] getEffectiveFilterApplics() {
        if (effectiveFilterApplics == null) {
            effectiveFilterApplics = getFilterSettingApplics();
        }
        return effectiveFilterApplics;
    }

    void setEffectiveFilterApplics(String[] applics) {
        effectiveFilterApplics = applics;
        applicEvaluator = null;
    }

    void setEffectiveFilterApplics(Collection applics) {
        String[] arr = new String[applics.size()];
        effectiveFilterApplics = (String[]) applics.toArray(arr);
        applicEvaluator = null;
    }

    boolean evaluateApplicabilityTerm(String applicTerm) {
        String[] effectiveApplics = getEffectiveFilterApplics();
        if (effectiveApplics == null) {  // null means unfiltered (e.g. unfiltered preview)
            return true;  // all nodes are applicable
        }
        if (applicEvaluator == null) {
            applicEvaluator = new ApplicEvaluator();
            applicEvaluator.setApplicability(effectiveApplics);
        }
        if ((applicTerm == null) || applicTerm.trim().equals("")) {
            return true;  // no applicability assigned, therefore node is applicable
        } else {
            try {
                return applicEvaluator.evaluate(applicTerm);
            } catch (DocException dex) {
                return false;
            }
        }
    }

    String getHtmlCustomHeaderContent() {
        return htmlCustomHeaderContent;
    }

    void setHtmlCustomHeaderContent(String content) {
        this.htmlCustomHeaderContent = content;
    }

    String getHtmlCustomFooterContent() {
        return htmlCustomFooterContent;
    }

    void setHtmlCustomFooterContent(String content) {
        this.htmlCustomFooterContent = content;
    }

    String getHtmlCustomHeadTags() {
        return htmlCustomHeadTags;
    }

    void setHtmlCustomHeadTags(String tags) {
        this.htmlCustomHeadTags = tags;
    }

    String getHtmlNavigationIconsPath() {
        return htmlNavigationIconsPath;
    }

    void setHtmlNavigationIconsPath(String path) {
        htmlNavigationIconsPath = path;
    }

    String getHtmlNavigationIconsExt() {
        return htmlNavigationIconsExt;
    }

    void setHtmlNavigationIconsExt(String ext) {
        htmlNavigationIconsExt = ext;
    }

    String getPdfCustomHeaderFooterXsl() {
        return pdfCustomHeaderFooterXsl;
    }

    void setPdfCustomHeaderFooterXsl(String xsl) {
        this.pdfCustomHeaderFooterXsl = xsl;
    }

    String getPdfCustomFOPConfigPath() {
        return pdfCustomFOPConfig;
    }

    void setPdfCustomFOPConfigPath(String fop_config) {
        this.pdfCustomFOPConfig = fop_config;
    }

    /* -----------  Public methods  ------------------ */

    public boolean isShowIndexTerms() {
        return showIndexTerms;
    }

    public void setShowIndexTerms(boolean show_index_terms) {
        this.showIndexTerms = show_index_terms;
    }

    public String getFilterSetting() {
        return filterSetting;
    }

    public String[] getFilterSettingApplics() {
        if (filterSetting == null) {  // null means unfiltered
            return null;
        }
        if (filterSetting.trim().equals("")) {
            return new String[0];
        }
        String[] arr = filterSetting.split(",");
        for (int i=0; i < arr.length; i++) {
            arr[i] = arr[i].trim();
        }
        return arr;
    }

    public void setFilterSetting(String filterSetting) {
        this.filterSetting = filterSetting;
        this.applicEvaluator = null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAppendixNumbering() {
        return appendixNumbering;
    }

    public void setAppendixNumbering(String appendixNumbering) {
        this.appendixNumbering = appendixNumbering;
    }

    public String getFootnoteNumbering() {
        return footnoteNumbering;
    }

    public void setFootnoteNumbering(String footnoteNumbering) {
        this.footnoteNumbering = footnoteNumbering;
    }

    public String getChapterNumbering() {
        return chapterNumbering;
    }

    public void setChapterNumbering(String chapterNumbering) {
        this.chapterNumbering = chapterNumbering;
    }

    public boolean isExclude1stLevelNumber() {
        return exclude1stLevelNumber;
    }

    public void setExclude1stLevelNumber(boolean exclude1stLevelNumber) {
        this.exclude1stLevelNumber = exclude1stLevelNumber;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getSubformat() {
        if (subformat == null) {
            return "";
        }
        if (subformat.equalsIgnoreCase("webhelp")) {
            return "webhelp1";
        } else {
            return subformat.toLowerCase();
        }
    }

    public void setSubformat(String subformat) {
        this.subformat = subformat;
    }

    public int getHtmlDirLevel() {
        return htmlDirLevel;
    }

    public void setHtmlDirLevel(int htmlDirLevel) {
        this.htmlDirLevel = htmlDirLevel;
    }

    public String getHtmlFilePrefix() {
        return htmlFilePrefix;
    }

    public void setHtmlFilePrefix(String htmlFilePrefix) {
        this.htmlFilePrefix = htmlFilePrefix;
    }

    public boolean isHtmlAliasFilename() {
        return htmlAliasFilename;
    }

    public void setHtmlAliasFilename(boolean htmlAliasFilename) {
        this.htmlAliasFilename = htmlAliasFilename;
    }

    public boolean isHtmlInclude1stSection() {
        return htmlInclude1stSection;
    }

    public void setHtmlInclude1stSection(boolean htmlInclude1stSection) {
        this.htmlInclude1stSection = htmlInclude1stSection;
    }

    public boolean isHtmlSeparateEachTable() {
        return htmlSeparateEachTable;
    }

    public void setHtmlSeparateEachTable(boolean htmlSeparateEachTable) {
        this.htmlSeparateEachTable = htmlSeparateEachTable;
    }

    public int getHtmlSeparateFileLevel() {
        return htmlSeparateFileLevel;
    }

    public void setHtmlSeparateFileLevel(int htmlSeparateFileLevel) {
        this.htmlSeparateFileLevel = htmlSeparateFileLevel;
    }

    public boolean isHtmlSeparateTOC() {
        return htmlSeparateTOC;
    }

    public void setHtmlSeparateTOC(boolean htmlSeparateTOC) {
        this.htmlSeparateTOC = htmlSeparateTOC;
    }

    public boolean isHtmlSingleFile() {
        return htmlSingleFile;
    }

    public void setHtmlSingleFile(boolean htmlSingleFile) {
        this.htmlSingleFile = htmlSingleFile;
    }

    public boolean isHtmlNavigationalIcons() {
        return htmlNavigationIcons;
    }

    public void setHtmlNavigationalIcons(boolean nav_icons) {
        this.htmlNavigationIcons = nav_icons;
    }

    public boolean isHtmlNavigationalTitles() {
        return htmlNavigationTitles;
    }

    public void setHtmlNavigationalTitles(boolean nav_titles) {
        this.htmlNavigationTitles = nav_titles;
    }

    public boolean isHtmlBreadcrumbs() {
        return htmlBreadcrumbs;
    }

    public void setHtmlBreadcrumbs(boolean breadcrumbs) {
        this.htmlBreadcrumbs = breadcrumbs;
    }

    public String getHtmlBreadcrumbStart() {
        return htmlBreadcrumbStart;
    }

    public void setHtmlBreadcrumbStart(String start_level) {
        this.htmlBreadcrumbStart = start_level;
    }

    public String getHtmlBreadcrumbSeparator() {
        return htmlBreadcrumbSeparator;
    }

    public void setHtmlBreadcrumbSeparator(String sep_char) {
        this.htmlBreadcrumbSeparator = sep_char;
    }

    public String getHtmlRootFolder() {
        return htmlRootFolder;
    }

    public void setHtmlRootFolder(String folder_path) {
        this.htmlRootFolder = folder_path;
    }

    public String getHtmlURLTargetWindow() {
        return htmlURLTargetWindow;
    }

    public void setHtmlURLTargetWindow(String window_name) {
        this.htmlURLTargetWindow = window_name;
    }

    public String getHtmlCustomHeaderAlias() {
        return htmlCustomHeaderAlias;
    }

    public void setHtmlCustomHeaderAlias(String alias) {
        this.htmlCustomHeaderAlias = alias;
    }

    public String getHtmlCustomFooterAlias() {
        return htmlCustomFooterAlias;
    }

    public void setHtmlCustomFooterAlias(String alias) {
        this.htmlCustomFooterAlias = alias;
    }

    public String getHtmlCustomCSSFilename() {
        return htmlCustomCSSFilename;
    }

    public void setHtmlCustomCSSFilename(String filename) {
        this.htmlCustomCSSFilename = filename;
    }

    public String getHtmlCustomJSFilename() {
        return htmlCustomJSFilename;
    }

    public void setHtmlCustomJSFilename(String filename) {
        this.htmlCustomJSFilename = filename;
    }

    public String getHtmlCustomMetaFilename() {
        return htmlCustomMetaFilename;
    }

    public void setHtmlCustomMetaFilename(String filename) {
        this.htmlCustomMetaFilename = filename;
    }

    public String getHtmlCustomFiles() {
        return htmlCustomFiles;
    }

    public void setHtmlCustomFiles(String files) {
        this.htmlCustomFiles = files;
    }

    public String getHtmlOutputEncoding() {
        return htmlOutputEncoding;
    }

    public void setHtmlOutputEncoding(String encoding) {
        this.htmlOutputEncoding = encoding;
    }

//    /**
//     * 
//     * @return Alias of cover image.
//     * @deprecated Replaced by DocmaPublicationConfig.getCoverImageAlias().
//     */
//    @Deprecated
//    public String getHtmlCoverImageAlias() {
//        return htmlCoverImage;
//    }
//
//    /**
//     * 
//     * @param coverimage Alias of cover image.
//     * @deprecated Replaced by DocmaPublicationConfig.setCoverImageAlias().
//     */
//    @Deprecated
//    public void setHtmlCoverImageAlias(String coverimage) {
//        this.htmlCoverImage = coverimage;
//    }

    public String getHtmlWebhelpConfigFolder() {
        return htmlWebhelpConfig;
    }

    public void setHtmlWebhelpConfigFolder(String foldername) {
        this.htmlWebhelpConfig = foldername;
    }

    public String getHtmlWebhelpHeader1() {
        return htmlWebhelpHeader1;
    }

    public void setHtmlWebhelpHeader1(String header) {
        this.htmlWebhelpHeader1 = header;
    }

    public String getHtmlWebhelpHeader2() {
        return htmlWebhelpHeader2;
    }

    public void setHtmlWebhelpHeader2(String header) {
        this.htmlWebhelpHeader2 = header;
    }

    public boolean isIndex() {
        return index;
    }

    public void setIndex(boolean index) {
        this.index = index;
    }

    public int getNumberingDepth() {
        return numberingDepth;
    }

    public void setNumberingDepth(int numberingDepth) {
        this.numberingDepth = numberingDepth;
    }

    public int getMaxTocDepth() {
        return maxTocDepth;
    }

    public void setMaxTocDepth(int maxTocDepth) {
        this.maxTocDepth = maxTocDepth;
    }

    public String getTocIndentWidth() {
        return tocIndent;
    }

    public void setTocIndentWidth(String width) {
        this.tocIndent = width;
    }

    public String getTocNamedLabels() {
        return tocNamedLabels;
    }

    public void setTocNamedLabels(String component_list) {
        this.tocNamedLabels = component_list;
    }

    public int getSectionTocLevels() {
        return sectionTocLevels;
    }

    public void setSectionTocLevels(int sectionTocLevels) {
        this.sectionTocLevels = sectionTocLevels;
    }

    public int getSectionTocDepth() {
        return sectionTocDepth;
    }

    public void setSectionTocDepth(int sectionTocDepth) {
        this.sectionTocDepth = sectionTocDepth;
    }

    public boolean isOmitSingleTitle() {
        return omitSingleTitle;
    }

    public void setOmitSingleTitle(boolean omitSingleTitle) {
        this.omitSingleTitle = omitSingleTitle;
    }

    public String getPartNumbering() {
        return partNumbering;
    }

    public void setPartNumbering(String partNumbering) {
        this.partNumbering = partNumbering;
    }

    public String getPdfBodyBottom() {
        return pdfBodyBottom;
    }

    public void setPdfBodyBottom(String pdfBodyBottom) {
        this.pdfBodyBottom = pdfBodyBottom;
    }

    public String getPdfBodyEnd() {
        return pdfBodyEnd;
    }

    public void setPdfBodyEnd(String pdfBodyEnd) {
        this.pdfBodyEnd = pdfBodyEnd;
    }

    public String getPdfBodyStart() {
        return pdfBodyStart;
    }

    public void setPdfBodyStart(String pdfBodyStart) {
        this.pdfBodyStart = pdfBodyStart;
    }

    public String getPdfBodyTop() {
        return pdfBodyTop;
    }

    public void setPdfBodyTop(String pdfBodyTop) {
        this.pdfBodyTop = pdfBodyTop;
    }

    public boolean isPdfBookmarks() {
        return pdfBookmarks;
    }

    public void setPdfBookmarks(boolean pdfBookmarks) {
        this.pdfBookmarks = pdfBookmarks;
    }

    public boolean isPdfDoubleSided() {
        return pdfDoubleSided;
    }

    public void setPdfDoubleSided(boolean pdfDoubleSided) {
        this.pdfDoubleSided = pdfDoubleSided;
    }

    public String getPdfFooterHeight() {
        return pdfFooterHeight;
    }

    public void setPdfFooterHeight(String pdfFooterHeight) {
        this.pdfFooterHeight = pdfFooterHeight;
    }

    public String getPdfHeaderHeight() {
        return pdfHeaderHeight;
    }

    public void setPdfHeaderHeight(String pdfHeaderHeight) {
        this.pdfHeaderHeight = pdfHeaderHeight;
    }

    public boolean isPdfNumbersInRefs() {
        return pdfNumbersInRefs;
    }

    public void setPdfNumbersInRefs(boolean pdfNumbersInRefs) {
        this.pdfNumbersInRefs = pdfNumbersInRefs;
    }

    public boolean isPdfExportReferencedFiles() {
        return pdfExportFiles;
    }

    public void setPdfExportReferencedFiles(boolean pdfExportFiles) {
        this.pdfExportFiles = pdfExportFiles;
    }

    public boolean isPdfFitImages() {
        return pdfFitImages;
    }

    public void setPdfFitImages(boolean pdfFitImages) {
        this.pdfFitImages = pdfFitImages;
    }

    public String getPdfCoverMode() {
        if (pdfCoverMode == null) pdfCoverMode = "";
        return pdfCoverMode;
    }

    public void setPdfCoverMode(String cover_mode) {
        this.pdfCoverMode = (cover_mode == null) ? "" : cover_mode;
    }

    public String getPdfCoverHorizontalPosition() {
        if (pdfCoverHorizontal == null) pdfCoverHorizontal = "";  // empty string means centered
        return pdfCoverHorizontal;
    }

    public void setPdfCoverHorizontalPosition(String hpos) {
        this.pdfCoverHorizontal = (hpos == null) ? "" : hpos;
    }

    public String getPdfCoverVerticalPosition() {
        if (pdfCoverVertical == null) pdfCoverVertical = "";  // empty string means centered
        return pdfCoverVertical;
    }

    public void setPdfCoverVerticalPosition(String vpos) {
        this.pdfCoverVertical = (vpos == null) ? "" : vpos;
    }

    public String getPdfShowExternalHRef() {
        if (pdfShowExternalHRef == null) pdfShowExternalHRef = "no";
        return pdfShowExternalHRef;
    }

    public void setPdfShowExternalHRef(String showExtHRef) {
        if ((showExtHRef == null) || showExtHRef.equals("")) showExtHRef = "no";
        this.pdfShowExternalHRef = showExtHRef;
    }

    public String getPdfPageBottom() {
        return pdfPageBottom;
    }

    public void setPdfPageBottom(String pdfPageBottom) {
        this.pdfPageBottom = pdfPageBottom;
    }

    public String getPdfPageHeight() {
        return pdfPageHeight;
    }

    public void setPdfPageHeight(String pdfPageHeight) {
        this.pdfPageHeight = pdfPageHeight;
    }

    public String getPdfPageInner() {
        return pdfPageInner;
    }

    public void setPdfPageInner(String pdfPageInner) {
        this.pdfPageInner = pdfPageInner;
    }

    public String getPdfPageOrientation() {
        return pdfPageOrientation;
    }

    public void setPdfPageOrientation(String pdfPageOrientation) {
        this.pdfPageOrientation = pdfPageOrientation;
    }

    public String getPdfPageOuter() {
        return pdfPageOuter;
    }

    public void setPdfPageOuter(String pdfPageOuter) {
        this.pdfPageOuter = pdfPageOuter;
    }

    public String getPdfPageTop() {
        return pdfPageTop;
    }

    public void setPdfPageTop(String pdfPageTop) {
        this.pdfPageTop = pdfPageTop;
    }

    public String getPdfPageWidth() {
        return pdfPageWidth;
    }

    public void setPdfPageWidth(String pdfPageWidth) {
        this.pdfPageWidth = pdfPageWidth;
    }

    public String getPdfPaperSize() {
        return pdfPaperSize;
    }

    public void setPdfPaperSize(String pdfPaperSize) {
        this.pdfPaperSize = pdfPaperSize;
    }

    public String getRender1stLevel() {
        return render1stLevel;
    }

    public void setRender1stLevel(String render1stLevel) {
        this.render1stLevel = render1stLevel;
    }

    public boolean isRestartPart() {
        return restartPart;
    }

    public void setRestartPart(boolean restartPart) {
        this.restartPart = restartPart;
    }

    public String getSectionNumbering() {
        return sectionNumbering;
    }

    public void setSectionNumbering(String sectionNumbering) {
        this.sectionNumbering = sectionNumbering;
    }

    public String getStyleVariant() {
        return styleVariant;
    }

    public void setStyleVariant(String styleVariant) {
        this.styleVariant = styleVariant;
    }

    public boolean isToc() {
        return toc;
    }

    public void setToc(boolean toc) {
        this.toc = toc;
    }

    public boolean isPartToc() {
        return partToc;
    }

    public void setPartToc(boolean partToc) {
        this.partToc = partToc;
    }

    public boolean isChapterToc() {
        return chapterToc;
    }

    public void setChapterToc(boolean chapterToc) {
        this.chapterToc = chapterToc;
    }

    public boolean isSectionToc() {
        return sectionToc;
    }

    public void setSectionToc(boolean sectionToc) {
        this.sectionToc = sectionToc;
    }

    public int getPdfSourceResolution() {
        return pdfSourceResolution;
    }

    public void setPdfSourceResolution(int dpi) {
        this.pdfSourceResolution = dpi;
    }

    public int getPdfTargetResolution() {
        return pdfTargetResolution;
    }

    public void setPdfTargetResolution(int dpi) {
        this.pdfTargetResolution = dpi;
    }

    public int getPdfIndexColumnCount() {
        return pdfIndexColumnCount;
    }

    public void setPdfIndexColumnCount(int cnt) {
        this.pdfIndexColumnCount = (cnt < 1) ? 2 : cnt;   // 2 is default
    }

    public String getPdfIndexColumnGap() {
        return pdfIndexColumnGap;
    }

    public void setPdfIndexColumnGap(String gap) {
        this.pdfIndexColumnGap = gap;
    }
    
    public String getItemSpace() {
        if ((itemSpace == null) || itemSpace.equals("")) {
            itemSpace = DocmaConstants.DEFAULT_ITEM_SPACE;
        }
        return itemSpace;
    }

    public void setItemSpace(String itemSpace) {
        this.itemSpace = itemSpace;
    }

    public String getListIndent() {
        if ((listIndent == null) || listIndent.equals("")) {
            listIndent = DocmaConstants.DEFAULT_LIST_INDENT;
        }
        return listIndent;
    }

    public void setListIndent(String listIndent) {
        this.listIndent = listIndent;
    }

    public String getOrderedListLabelWidth() {
        return orderedListLabelWidth;
    }

    public void setOrderedListLabelWidth(String labelWidth) {
        this.orderedListLabelWidth = labelWidth;
    }

    public String getParaSpace() {
        if ((paraSpace == null) || paraSpace.equals("")) {
            paraSpace = DocmaConstants.DEFAULT_PARA_SPACE;
        }
        return paraSpace;
    }

    public void setParaSpace(String paraSpace) {
        this.paraSpace = paraSpace;
    }

    public String getParaIndent() {
        if ((paraIndent == null) || paraIndent.equals("")) {
            paraIndent = DocmaConstants.DEFAULT_PARA_INDENT;
        }
        return paraIndent;
    }

    public void setParaIndent(String paraIndent) {
        this.paraIndent = paraIndent;
    }

    public String getTitlePlacement() {
        return titlePlacement;
    }

    public void setTitlePlacement(String titlePlacement) {
        this.titlePlacement = titlePlacement;
    }
    
    public boolean isPdfCustomHeaderFooter() 
    {
        return pdfCustomHeaderFooter;
    }
    
    public void setPdfCustomHeaderFooter(boolean pdfCustomHeaderFooter)
    {
        this.pdfCustomHeaderFooter = pdfCustomHeaderFooter;
    }
    
    public String getPdfHeaderWidths()
    {
        return pdfHeaderWidths;
    }
    
    public void setPdfHeaderWidths(String widths)
    {
        this.pdfHeaderWidths = widths;
    }

    public String getPdfFooterWidths()
    {
        return pdfFooterWidths;
    }
    
    public void setPdfFooterWidths(String widths)
    {
        this.pdfFooterWidths = widths;
    }
    
    public String[] getPdfCustomHeaderFooterPageTypes()
    {
        if (this.pdfCustomPageTypes == null) {
            return new String[0];
        } else {
            int len = this.pdfCustomPageTypes.size();
            return (String[]) this.pdfCustomPageTypes.toArray(new String[len]);
        }
    }
    
    public void setPdfCustomHeaderFooterPageTypes(String[] pageTypes)
    {
        if (this.pdfCustomPageTypes == null) {
            this.pdfCustomPageTypes = new TreeSet<String>();
        }
        this.pdfCustomPageTypes.clear();
        if (pageTypes != null) {
            this.pdfCustomPageTypes.addAll(Arrays.asList(pageTypes));
        }
        
        // Remove header/footer configuration of removed pageTypes 
        if (this.pdfHeaderFooterContent != null) {
            Iterator<HeaderFooterKey> it = this.pdfHeaderFooterContent.keySet().iterator();
            while (it.hasNext()) {
                if (! this.pdfCustomPageTypes.contains(it.next().pageType)) {
                    it.remove();
                }
            }
        }
    }
    
    public String getPdfHeaderFooterContent(String pageType,
                                            String region,
                                            int column,
                                            int row)
    {
        if (this.pdfHeaderFooterContent == null) {
            return null;
        }
        HeaderFooterKey key = new HeaderFooterKey(pageType, region, column, row);
        return this.pdfHeaderFooterContent.get(key);
    }

    public void setPdfHeaderFooterContent(String pageType, 
                                          String region,
                                          int column,
                                          int row, 
                                          String value)
    {
        if (this.pdfHeaderFooterContent == null) {
            this.pdfHeaderFooterContent = new HashMap<HeaderFooterKey, String>(211);
        }
        if (this.pdfCustomPageTypes == null) {
            this.pdfCustomPageTypes = new TreeSet<String>();
        }
        HeaderFooterKey key = new HeaderFooterKey(pageType, region, column, row);
        if (value == null) {
            this.pdfHeaderFooterContent.remove(key);
        } else {
            this.pdfHeaderFooterContent.put(key, value);
            this.pdfCustomPageTypes.add(pageType);
        }
    }

    public int getPdfMarkerSectionLevel() 
    {
        return pdfMarkerSectionLevel;
    }

    public void setPdfMarkerSectionLevel(int sect_level) 
    {
        this.pdfMarkerSectionLevel = sect_level;
    }



    /* -----------  Private methods  ------------------ */

    private void readPdfHeaderFooterConfig(DocmaSession docmaSess, String[] pageTypes)
    {
        if (pageTypes.length > 0) {
            this.pdfHeaderFooterContent = new HashMap<HeaderFooterKey, String>();
        }
        for (int i = 0; i < pageTypes.length; i++) {
            String ptype = pageTypes[i];
            for (int col = 1; col <= 3; col++) {
                for (int row = 1; row <= 2; row++) {
                    HeaderFooterKey hkey = new HeaderFooterKey(ptype, "header", col, row);
                    HeaderFooterKey fkey = new HeaderFooterKey(ptype, "footer", col, row);
                    String hprop = PROP_VERSION_PDFCONFIG_REGION + "_" + hkey.getKeyString();
                    String fprop = PROP_VERSION_PDFCONFIG_REGION + "_" + fkey.getKeyString();
                    this.pdfHeaderFooterContent.put(hkey, getStringProp(docmaSess, hprop));
                    this.pdfHeaderFooterContent.put(fkey, getStringProp(docmaSess, fprop));
                }
            }
        }
    }
        
    private String encodePdfCustomHeaderFooterPageTypes()
    {
        String[] ptypes = getPdfCustomHeaderFooterPageTypes();
        if (ptypes.length == 0) return null;
        StringBuilder buf = new StringBuilder(ptypes[0]);
        for (int i = 1; i < ptypes.length; i++) {
            buf.append(",").append(ptypes[i]);
        }
        return buf.toString();
    }
    
    private void decodePdfCustomHeaderFooterPageTypes(String encoded) 
    {
        if (this.pdfCustomPageTypes != null) {
            this.pdfCustomPageTypes.clear();
        }
        if ((encoded != null) && !encoded.trim().equals("")) {
            if (this.pdfCustomPageTypes == null) {
                this.pdfCustomPageTypes = new TreeSet<String>();
            }
            StringTokenizer st = new StringTokenizer(encoded, ",");
            while (st.hasMoreTokens()) {
                this.pdfCustomPageTypes.add(st.nextToken());
            }
        }
    }

    private boolean getBoolProp(DocmaSession sess, String prop_name)
    {
        String val = sess.getVersionProperty(sess.getStoreId(), sess.getVersionId(),
                                             prop_name + "." + this.id);
        if (val == null) return false;
        else return val.trim().equalsIgnoreCase("true");
    }

    private String getStringProp(DocmaSession sess, String prop_name)
    {
        String val = sess.getVersionProperty(sess.getStoreId(), sess.getVersionId(),
                                             prop_name + "." + this.id);
        if (val == null) return "";
        else return val;
    }

    private int getIntProp(DocmaSession sess, String prop_name)
    {
        return getIntProp(sess, prop_name, 0);
    }
    
    private int getIntProp(DocmaSession sess, String prop_name, int default_value)
    {
        String val = sess.getVersionProperty(sess.getStoreId(), sess.getVersionId(),
                                             prop_name + "." + this.id);
        if (val == null) return default_value;
        else {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException nfe) {
                return default_value;
            }
        }
    }

    private void addProp(List namelist, List valuelist, String name, String value)
    {
        namelist.add(name + "." + this.id);
        valuelist.add(value);
    }

    
    /* -----------  Helper classes  ------------------ */

    private static class HeaderFooterKey
    {
        String pageType;
        String region;
        int column;
        int row;
        
        HeaderFooterKey(String pageType, String region, int col, int row) 
        {
            this.pageType = pageType;
            this.region = region;
            this.column = col;
            this.row = row;
        }
        
        String getKeyString()
        {
            return pageType + "_" + region + "_" + column + "_" + row;
        }

        public int hashCode() 
        {
            return getKeyString().hashCode();
        }

        public boolean equals(Object obj) 
        {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final HeaderFooterKey other = (HeaderFooterKey) obj;
            if ((this.pageType == null) ? (other.pageType != null) : !this.pageType.equals(other.pageType)) {
                return false;
            }
            if ((this.region == null) ? (other.region != null) : !this.region.equals(other.region)) {
                return false;
            }
            if (this.column != other.column) {
                return false;
            }
            if (this.row != other.row) {
                return false;
            }
            return true;
        }
        
    }

}
