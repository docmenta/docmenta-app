/*
 * DocmaConstants.java
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

import org.docma.coreapi.fsimplementation.FilesystemStoreProperties;
import org.docma.coreapi.DocVersionState;
import org.docma.coreapi.DocConstants;
import org.docma.plugin.web.DefaultContentAppHandler;

/**
 *
 * @author MP
 */
public class DocmaConstants
{
    public static final boolean DEBUG = DocConstants.DEBUG;
    public static final String DEBUG_DIR = DocConstants.DEBUG_DIR;
    public static final String DISPLAY_APP_SHORTVERSION = "1.8";
    public static final String DISPLAY_APP_VERSION = "1.8.2";
    public static final String DISPLAY_APP_SHORTNAME = "Docmenta";
    public static final String DISPLAY_APP_NAME_BASEEDITION = "Docmenta Publishing System";
    public static final String DISPLAY_APP_COPYRIGHT = "\u00A9 2016";
    public static final String DISPLAY_APP_HOMEPAGE = "http://www.docmenta.org";
    public static final String DISPLAY_APP_EMAIL = "info@docmenta.org";
    public static final String DISPLAY_APP_CREDIT = "Produced with " + 
                                                    DISPLAY_APP_NAME_BASEEDITION +
                                                    " V" + DISPLAY_APP_SHORTVERSION + 
                                                    " &#169; 2016 (www.docmenta.org)";

    //
    // Application properties
    //
    public static final String PROP_BASE_PATH = "base.path";
    public static final String PROP_STORES_PATH = "docstores.path";
    public static final String PROP_STORE_DTD_PATH = "docstore.dtd.path";
    public static final String PROP_TEMP_PATH = "temp.path";
    public static final String PROP_DOCBOOK_XSL_PATH = "docbook.xsl.path";
    public static final String PROP_DOCMA_XSL_PATH = "docma.xsl.path";
    public static final String PROP_DOCMA_GENTEXT_PATH = "docma.gentext.path";
    public static final String PROP_DOCMA_RESOURCES_PATH = "docma.resources.path";
    public static final String PROP_PUBLICATION_ONLINE_RELATIVE_PATH = "publication.online.relative.path";
    public static final String PROP_PUBLICATION_ONLINE_PATH = "publication.online.path";
    public static final String PROP_PUBLICATION_ONLINE_URL = "publication.online.url";
    public static final String PROP_CHAR_ENTITIES = "char.entities";
    public static final String PROP_MAX_REVISIONS_PER_USER = "revisions.max.per.user";
    public static final String PROP_TEXT_FILE_EXTENSIONS = "text.file.extensions";
    public static final String PROP_FOP_CUSTOM_CONFIG_FILE = "fop.custom.config.file";
    public static final String PROP_AUTOFORMAT_CLASSES = "autoformat.classes";
    public static final String PROP_DISABLED_STORES = "docstores.disabled";
    public static final String PROP_PREVIEW_MAX_NODES = "preview.max.nodes";
    public static final String PROP_PREVIEW_MAX_NODES_PRINT = "preview.max.nodes.print";

    //
    // Store properties
    //
    public static final String PROP_STORE_BASEPATH = FilesystemStoreProperties.PROP_STORE_BASEPATH;
    public static final String PROP_STORE_PATH = FilesystemStoreProperties.PROP_STORE_PATH;
    public static final String PROP_STORE_TYPE = FilesystemStoreProperties.PROP_STORE_TYPE;
    public static final String PROP_STORE_ARCHIVE_TYPE = "docstore.archive.type";
    public static final String PROP_STORE_DISPLAYNAME = "docstore.displayname";
    public static final String PROP_STORE_DECLARED_APPLICS = "docstore.declaredapplics";
    public static final String PROP_STORE_FILTER_NAMES = "docstore.filternames";
    public static final String PROP_STORE_FILTER_SETTING = "docstore.filtersetting";
    public static final String PROP_STORE_ORIG_LANGUAGE = "docstore.origlang";
    public static final String PROP_STORE_TRANSLATION_LANGUAGES = "docstore.translationlangs";
    // External DB connection property names
    public static final String PROP_STORE_DB_DIALECT = FilesystemStoreProperties.PROP_DB_DIALECT;
    public static final String PROP_STORE_DB_DRIVER_CLASS = FilesystemStoreProperties.PROP_DB_DRIVER_CLASS;
    public static final String PROP_STORE_DB_CONNECTION_URL = FilesystemStoreProperties.PROP_DB_CONNECTION_URL;
    public static final String PROP_STORE_DB_CONNECTION_USER = FilesystemStoreProperties.PROP_DB_CONNECTION_USER;
    public static final String PROP_STORE_DB_CONNECTION_PWD = FilesystemStoreProperties.PROP_DB_CONNECTION_PWD;

    // PROP_STORE_TYPE property values
    public static final String STORE_TYPE_FS = FilesystemStoreProperties.STORE_TYPE_FS;
    public static final String STORE_TYPE_DB_EMBEDDED = FilesystemStoreProperties.STORE_TYPE_DB_EMBEDDED;
    public static final String STORE_TYPE_DB_EXTERNAL = FilesystemStoreProperties.STORE_TYPE_DB_EXTERNAL;
    // PROP_STORE_ARCHIVE_TYPE property values
    public static final String ARCHIVE_TYPE_FS = "fs_archive";
    public static final String ARCHIVE_TYPE_DB = "db_archive";

    //
    // Store Version properties
    //
    public static final String PROP_VERSION_LAST_MODIFIED_DATE = "docversion.lastmodifieddate";
    public static final String PROP_VERSION_COMMENT = "docversion.comment";
    public static final String PROP_VERSION_STYLE_IDS = "docversion.style.ids";
    public static final String PROP_VERSION_STYLE_TYPE = "docversion.style.type";
    public static final String PROP_VERSION_STYLE_NAME = "docversion.style.name";
    public static final String PROP_VERSION_STYLE_CSS = "docversion.style.css";
    public static final String PROP_VERSION_STYLE_AUTOFORMAT = "docversion.style.autoformat";
    public static final String PROP_VERSION_STYLE_HIDDEN = "docversion.style.hidden";

    //
    // User properties
    //
    public static final String PROP_USER_LAST_NAME = "lastname";
    public static final String PROP_USER_FIRST_NAME = "firstname";
    public static final String PROP_USER_EMAIL = "email";
    public static final String PROP_USER_LAST_LOGIN = "lastlogin";
    public static final String PROP_USER_GUI_LANGUAGE = "guilanguage";
    public static final String PROP_USER_DATE_FORMAT = "dateformat";

    // User group properties
    public static final String PROP_USERGROUP_RIGHTS = "accessrights";

    //
    // Version constants
    //
    public static final String DEFAULT_LATEST_VERSION_ID = "LATEST";
    public static final String VERSION_STATE_DRAFT = DocVersionState.DRAFT;
    public static final String VERSION_STATE_RELEASED = DocVersionState.RELEASED;
    public static final String VERSION_STATE_TRANSLATION_PENDING = DocVersionState.TRANSLATION_PENDING;

    //
    // Other constants
    //
    public static final String SYS_ADMIN_LOGIN_NAME = "admin";
    public static final String SYS_ADMIN_GROUP_NAME = "Administrators";
    public static final String DEFAULT_DATE_FORMAT = "dd.MM.yyyy HH:mm";
    public static final int    ALIAS_MAX_LENGTH = 40;
    public static final int    GENTEXT_KEY_MAX_LENGTH = 100;
    public static final char   LINKALIAS_SEPARATOR = '!';
    public static final int    MAX_INDENT_LEVELS = 9;
    public static final String GENTEXT_ALIAS_NAME = "gentext";
    public static final String HTML_CONFIG_FOLDER_ALIAS_NAME = "htmlconf_folder";
    public static final String HTML_CSS_FILENAME = "styles.css";
    public static final String DEFAULT_HTML_CONFIG_ID = "html";
    public static final String DEFAULT_WEBHELP_CONFIG_ID = "webhelp";
    public static final String DEFAULT_EPUB_CONFIG_ID = "epub";
    public static final String DEFAULT_PDF_CONFIG_ID = "pdf";
    public static final String DEFAULT_DOCBOOK_CONFIG_ID = "docbook";
    public static final int    DEFAULT_MAX_REVISIONS_PER_USER = 10;
    public static final String DEFAULT_URL_TARGET_WINDOW = "_top";

    // Regular expressions
    public static final String REGEXP_ID = "[A-Za-z][0-9A-Za-z_-]*";
    public static final String REGEXP_NAME = "[\\p{L}0-9/!#()*+,.:<=>?@\\[\\]_ -]*";
    public static final String REGEXP_ALIAS = "[A-Za-z_][0-9A-Za-z_!?-]{1,39}";
    public static final String REGEXP_ALIAS_LINK = "[A-Za-z_][0-9A-Za-z_-]{1,39}";
    public static final String REGEXP_GENTEXT_KEY = "[^\\s\"'<>]+";  // sequence of non-whitespace, quotes and < > not allowed
    public static final String REGEXP_STYLE_ID = "[A-Za-z][0-9A-Za-z_]*";
    public static final String REGEXP_STYLE_NAME = "[0-9A-Za-z_,;. -]+";
    public static final String REGEXP_PRODUCT_ID = "[0-9A-Za-z_-]+";

    // Default styles
    public static final String STYLE_DEFAULT_CSS = "color:#000000; font-family:Verdana, Arial, Helvetica, sans-serif; font-size:1em; border-color:#000000;";
    public static final String STYLE_CAPTION_CSS = "font-style:italic; font-size:1em; margin-top:2pt; margin-bottom:2pt;";
    public static final String STYLE_FLOAT_LEFT_CSS = "margin-right:8pt; margin-bottom:2pt;";  // margin-top is not set, to not overwrite the paragraph spacing
    public static final String STYLE_FLOAT_RIGHT_CSS = "margin-left:8pt; margin-bottom:2pt;";  // margin-top is not set, to not overwrite the paragraph spacing
    public static final String STYLE_H1_CSS = "font-size:34pt; font-weight:bold; text-align:center; margin-top:20pt;";  // title on title-page
    public static final String STYLE_H2_CSS = "font-size:28pt; font-weight:bold; margin-top:22pt; margin-bottom:16pt;";
    public static final String STYLE_H3_CSS = "font-size:1.8em; font-weight:bold; font-style:italic; margin-top:1.4em; margin-bottom:0.6em;";
    public static final String STYLE_H4_CSS = "font-size:1.6em; font-weight:bold; margin-top:1.2em; margin-bottom:0.5em;";
    public static final String STYLE_H5_CSS = "font-size:1.3em; font-weight:bold; font-style:italic; margin-top:1.2em; margin-bottom:0.5em;";
    public static final String STYLE_H6_CSS = "font-size:1.1em; font-weight:bold; margin-top:1.1em; margin-bottom:0.5em;";
    public static final String STYLE_HEADER_PARA_CSS = "font-weight:bold; margin-top:1em; margin-bottom:0.5em;";
    public static final String STYLE_TABLE_CELL_CSS = "";
    public static final String STYLE_TABLE_HEADER_CSS = "font-weight:bold; text-align:center;";
    public static final String STYLE_PAGE_HEADER_CSS = "border-bottom-width:0.5pt; border-bottom-style:solid; border-bottom-color:#000000; white-space:nowrap;";
    public static final String STYLE_PAGE_FOOTER_CSS = "border-top-width:0.5pt; border-top-style:solid; border-top-color:#000000;";
    public static final String STYLE_BREADCRUMBS_CSS = "color:#0000A8; background-color:#FFFFFF; padding:6px 0px 4px 12px;";
    public static final String STYLE_STRONG_CSS = "font-weight:bold;";
    public static final String STYLE_EMPHASIS_CSS = "font-style:italic;";
    public static final String STYLE_UNDERLINE_CSS = "text-decoration:underline;";
    public static final String STYLE_STRIKE_CSS = "text-decoration:line-through;";
    public static final String STYLE_LINK_CSS = "color:#0000FF;";
    public static final String STYLE_FOOTNOTE_CSS = "font-style:italic;";
    public static final String STYLE_BREADCRUMB_NODE_CSS = "white-space:nowrap;";
    // Default title-page styles
    public static final String STYLE_PARTHEADER_CSS = "font-size:24pt; font-weight:bold; text-align:center; margin-top:18pt";
    public static final String STYLE_SUBTITLE_CSS = "font-size:22pt; text-align:center; margin-top:16pt";
    public static final String STYLE_CORPAUTHOR_CSS = "font-size:16pt; margin-top:24pt";
    public static final String STYLE_AUTHORGROUP_CSS = "margin-top:24pt";
    public static final String STYLE_AUTHOR_CSS = "font-size:12pt; font-weight:bold; text-align:center; margin:1pt";
    public static final String STYLE_TITLEBACK_CSS = "font-size:15pt; font-weight:bold;";
    public static final String STYLE_RELEASEINFO_CSS = "margin-top:15pt";
    public static final String STYLE_PUBDATE_CSS = "margin-top:12pt";
    public static final String STYLE_ABSTRACT_CSS = "margin-top:12pt";
    public static final String STYLE_LEGALNOTICE_CSS = "font-size:8pt; margin-top:12pt";
    public static final String STYLE_OTHERCREDIT_CSS = "font-size:10pt; margin-top:16pt";

    // Default output configuration values
    public static final String DEFAULT_PARA_SPACE = "8pt";
    public static final String DEFAULT_PARA_INDENT = DefaultContentAppHandler.EDITOR_DEFAULT_PARA_INDENT;
    public static final String DEFAULT_ITEM_SPACE = "4pt";
    public static final String DEFAULT_LIST_INDENT = "11pt";

    // Static example templates
    public static final String TEMPLATE_TABLE_HEADER1_CSS =
        "text-align: center; background-color: #ffffa0; font-weight: bold;";
    public static final String TEMPLATE_TABLE1_HTML =
        "<table style=\"width: 100%;\" border=\"1\" cellspacing=\"0\">\n<tbody>\n" +
        "<tr class=\"table_header1\"><th>Header 1</th><th>Header 2</th><th>Header 3</th></tr>\n" +
        "<tr>\n<td>&#160;</td>\n<td>&#160;</td>\n<td>&#160;</td>\n</tr>\n" +
        "<tr>\n<td>&#160;</td>\n<td>&#160;</td>\n<td>&#160;</td>\n</tr>\n" +
        "</tbody>\n</table>\n";
    public static final String TEMPLATE_TABLE2_HTML =
        "<table style=\"width: 100%;\" border=\"1\" cellspacing=\"0\">\n<tbody>\n" +
        "<tr><th class=\"table_header1\" style=\"width: 100pt;\">Header 1</th>\n<td>&#160;</td>\n<td>&#160;</td>\n</tr>\n" +
        "<tr><th class=\"table_header1\">Header 2</th>\n<td>&#160;</td>\n<td>&#160;</td>\n</tr>\n" +
        "</tbody>\n</table>\n";

    // Auto-Format example templates
    public static final String TEMPLATE_NOTE_HEADER_CSS =
        "font-weight: bold; color: #f0f0f0; background-color: #0a88d4; padding: 0pt 0pt 0pt 2pt; margin: 8pt 0pt 0pt 0pt; border: 2pt solid #0a88d4;";
    public static final String TEMPLATE_NOTE_BOX1_CSS =
        "border-bottom: #0a88d4 2pt solid; border-left: #0a88d4 2pt solid; border-top: #0a88d4 0pt solid; border-right: #0a88d4 2pt solid; padding: 2pt; margin: 0pt; background-color: #f0f0f0;";
    public static final String TEMPLATE_NOTE_BOX2_CSS =
        "border-bottom: #bcc7d5 1pt solid; border-left: #bcc7d5 1pt solid; border-top: #bcc7d5 1pt solid; border-right: #bcc7d5 1pt solid; background-color: #e0eefe;";
    public static final String TEMPLATE_NOTE_CSS = 
        "background-color: #e0eefe; margin-top: 8pt; border-bottom-style: none; border-left: #bcc7d5 2pt solid; border-top-style: none; border-right-style: none;";
    public static final String TEMPLATE_NOTE_AUTOFORMAT =
        "org.docma.plugin.examples.ApplyTemplate(notebox_template2)";
//    public static final String TEMPLATE_NOTE_LAYOUT1_HTML =
//        "<table style=\"width: 100%;\" border=\"0\" cellspacing=\"0\">\n" +
//        "<tbody>\n" +
//        "<tr>\n" +
//        "<td class=\"header_note\" valign=\"center\">[#title_note]: $title</td>\n" +
//        "</tr>\n" +
//        "<tr>\n" +
//        "<td class=\"note_box\">$content</td>\n" +
//        "</tr>\n" +
//        "</tbody>\n" +
//        "</table>\n";
    public static final String TEMPLATE_NOTE_LAYOUT1_HTML =
        "<table style=\"width: 100%;\" border=\"0\" cellspacing=\"0\">\n" +
        "<tbody>\n" +
        "<tr>\n" +
        "<td class=\"header_note\">" +
        "<sub><img style=\"print-height: 13pt;\" src=\"image/info_icon\" alt=\"\" width=\"16\" height=\"16\" /></sub>&#160;$title</td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        "<td class=\"note_box\">$content</td>\n" +
        "</tr>\n" +
        "</tbody>\n" +
        "</table>\n";
    public static final String TEMPLATE_NOTE_LAYOUT2_HTML =
        "<table style=\"width: 100%;\" border=\"0\" cellspacing=\"0\" cellpadding=\"3\">\n" +
        "<tbody>\n" +
        "<tr class=\"note_box2\">\n" +
        "<td style=\"width: 50px;\" valign=\"top\">\n" +
        "<p><img class=\"align-center\" src=\"image/info_icon\" alt=\"\" /></p>\n" +
        "</td>\n" +
        "<td valign=\"top\">\n" +
        "<p><span class=\"underline\"><strong>$title</strong></span> $content</p>\n" +
        "</td>\n" +
        "</tr>\n" +
        "</tbody>\n" +
        "</table>\n";

    /* --------  package local constants  --------- */

    // DocNode attribute names
    static final String ATTRIBUTE_DOCNODE_WORKFLOWSTATE = "wfstate";
    static final String ATTRIBUTE_DOCNODE_PROGRESS = "progress";
    static final String ATTRIBUTE_DOCNODE_APPLIC = "applic";
    static final String ATTRIBUTE_DOCNODE_LASTMOD_DATE = "lastmod_date";
    static final String ATTRIBUTE_DOCNODE_LASTMOD_BY = "lastmod_by";
    static final String ATTRIBUTE_DOCNODE_COMMENT = "comment";
    static final String ATTRIBUTE_DOCNODE_PRIORITY = "priority";
    static final String ATTRIBUTE_DOCNODE_CHARSET = "charset";

}
