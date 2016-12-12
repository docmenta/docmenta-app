/*
 * PublicationManager.java
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

import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.xml.transform.stream.StreamSource;

import org.docma.coreapi.DocException;
import org.docma.coreapi.DocRuntimeException;
import org.docma.coreapi.DocVersionId;
import org.docma.coreapi.DocmaExportLog;
import org.docma.coreapi.ExportLog;
import org.docma.coreapi.PublicationArchive;
import org.docma.coreapi.PublicationArchivesSession;
import org.docma.util.*;

/**
 *
 * @author MP
 */
class PublicationManager
{
    private static final List globalExportQueue = Collections.synchronizedList(new ArrayList());

    private final DocmaSession docmaSess;
    private final PublicationArchivesSession allArchives;
    private final Map publicationMap = new HashMap();
    private PublicationArchive archive;   // the currently opened archive


    PublicationManager(DocmaSession docmaSess, PublicationArchivesSession pub_archives)
    {
        this.docmaSess = docmaSess;
        this.allArchives = pub_archives;
    }

    String[] getPublicationIds()
    {
        openPublicationArchive();
        return archive.listPublications();
    }

    DocmaPublication getPublication(String publicationId)
    {
        openPublicationArchive();
        DocmaPublication pub = get_Publication(publicationId);
        pub.refresh();
        return pub;
    }

    DocmaPublication[] listPublications(String language)
    {
        return listPublications(language, null);
    }
    
    DocmaPublication[] listPublications(String language, String versionState)
    {
        openPublicationArchive();
        String[] pub_ids = archive.listPublications();
        List list = new ArrayList(pub_ids.length);
        for (String pub_id : pub_ids) {
            DocmaPublication pub = get_Publication(pub_id);
            if (((language == null) || language.equalsIgnoreCase(pub.getLanguage())) &&
                ((versionState == null) || versionState.equalsIgnoreCase(pub.getPublicationState()))) {
                pub.refresh();
                list.add(pub);
            }
        }
        Collections.sort(list);
        DocmaPublication[] arr = new DocmaPublication[list.size()];
        return (DocmaPublication[]) list.toArray(arr);
    }

    String getDefaultPublicationFilename(String pubConfigId, String outConfigId)
    {
        DocmaOutputConfig outConf = docmaSess.getOutputConfig(outConfigId);
        return get_DefaultPublicationFilename(pubConfigId, outConf);
    }

    String createPublication(String pubConfigId, String outConfigId, String filename)
    {
        String transMode = docmaSess.getTranslationMode();
        String lang;
        if (transMode == null) {
            lang = docmaSess.getOriginalLanguage().getCode();
        } else {
            lang = transMode;
        }
        return createPublication(pubConfigId, outConfigId, lang, filename);
    }
    
    String createPublication(String pubConfigId, String outConfigId, String lang, String filename)
    {
        openPublicationArchive();

        if (filename == null) {
            filename = getDefaultPublicationFilename(pubConfigId, outConfigId);
        }

        String publicationId = archive.createPublication(lang, filename);
        DocmaPublication pub = get_Publication(publicationId);

        String storeId = docmaSess.getStoreId();
        DocVersionId verId = docmaSess.getVersionId();
        DocmaPublicationConfig pubConf = docmaSess.getPublicationConfig(pubConfigId);
        DocmaOutputConfig outConf = docmaSess.getOutputConfig(outConfigId);

        String title = pubConf.getTitle();
        String ver_state = docmaSess.getVersionState(storeId, verId);
        Date release_date = docmaSess.getVersionReleaseDate(storeId, verId);
        String userId = docmaSess.getUserId();
        pub.setPublicationMetadata(title, ver_state, pubConfigId, outConfigId,
                                   outConf.getFormat(), outConf.getSubformat(),
                                   release_date, userId, "");
        pub.setExportProgressMessage("Please wait...");

        addToGlobalExportQueue(pub);

        return publicationId;
    }

    void exportPublicationAsync(String publicationId)
    {
        DocmaSession tempSess = docmaSess.createNewSession();
        tempSess.openDocStore(docmaSess.getStoreId(), docmaSess.getVersionId());
        String transMode = docmaSess.getTranslationMode();
        if (transMode != null) {
            tempSess.enterTranslationMode(transMode);
        }
        PublicationExportThread exp_thread = new PublicationExportThread(tempSess, publicationId);
        exp_thread.start();
    }

    void exportPublication(String publicationId)
    {
        openPublicationArchive();
        DocmaPublication pub = get_Publication(publicationId);
        String pubConfigId = pub.getPublicationConfigId();
        String outConfigId = pub.getOutputConfigId();
        DocmaPublicationConfig pubConf = docmaSess.getPublicationConfig(pubConfigId);
        DocmaOutputConfig outConf = docmaSess.getOutputConfig(outConfigId);

        String ver_state = pub.getPublicationState();
        boolean is_draft = !DocmaConstants.VERSION_STATE_RELEASED.equals(ver_state);

        String format = outConf.getFormat();
        String subformat = outConf.getSubformat();
        boolean is_pdf = format.equalsIgnoreCase("pdf");
        boolean is_html = format.equalsIgnoreCase("html");
        boolean is_docbook = format.equalsIgnoreCase("docbook");
        boolean is_epub = subformat.startsWith("epub");

        pubConf.setDraft(is_draft && !is_epub);  // Do not create draft-watermark for EPUB
                                                 // because this causes problems in Abobe Reader!

        String lang = docmaSess.getLanguageCode();
        if (! lang.equalsIgnoreCase(pub.getLanguage())) {
            throw new DocRuntimeException("Cannot export publication. Language mismatch!");
        }

        pub.setExportProgressMessage("Exporting... please wait");
        DocmaExportContext export_ctx = new DocmaExportContext(docmaSess, pubConf, outConf, true);
        DocmaExportLog export_log = export_ctx.getDocmaExportLog();

        // FormattingEngine formatter = docmaSess.getFormatter();
        OutputStream pub_out = null;  // archive output stream
        try {
            if (is_pdf) {
                if (outConf.isPdfExportReferencedFiles()) {
                    File pdf_file = docmaSess.getFormatter().createTempFile("pdf");
                    FileOutputStream fout = new FileOutputStream(pdf_file);
                    try {
                        OutputStream buf_out = new BufferedOutputStream(fout, 256*1024);
                        SortedMap fileurl_map = new TreeMap();
                        try {
                            exportPDF(buf_out, null, export_ctx, fileurl_map);
                        } finally {
                            buf_out.close(); // this closes fout as well
                        }
                        if (fileurl_map.isEmpty()) {
                            // No files are referenced. Put PDF into archive.
                            pub_out = archive.openPublicationOutputStream(publicationId);
                            FileInputStream fin = new FileInputStream(pdf_file);
                            DocmaUtil.copyStream(fin, pub_out);
                            fin.close();
                        } else {
                            // Add PDF and referenced files to zip. Put zip into archive.
                            String pdf_filename = pub.getFilename();
                            int pos = pdf_filename.lastIndexOf('.');
                            String base_name = (pos < 0) ? pdf_filename 
                                                         : pdf_filename.substring(0, pos);
                            pub.setFilename(base_name + ".zip");
                            
                            pub_out = archive.openPublicationOutputStream(publicationId);
                            ZipOutputStream zipout = new ZipOutputStream(pub_out);
                            ZipUtil.addFileToZip(zipout, pdf_file, pdf_filename);
                            addReferencedFilesToZip(fileurl_map, zipout);
                            zipout.close();
                        }
                    } finally {
                        pdf_file.delete();
                    }
                } else {
                    pub_out = archive.openPublicationOutputStream(publicationId);
                    exportPDF(pub_out, null, export_ctx);  // Put PDF into archive
                }
            } else
            if (is_html) {
                pub_out = archive.openPublicationOutputStream(publicationId);
                exportHTML(pub_out, null, export_ctx);
            } else
            if (is_docbook) {
                pub_out = archive.openPublicationOutputStream(publicationId);
                exportDocBook(pub_out, null, export_ctx);
            } else {
                throw new DocRuntimeException("Cannot export publication. Unknown format: " + format);
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            export_log.error(ex.getMessage());
        } finally {
            try { 
                if (pub_out != null) archive.closePublicationOutputStream(publicationId);
            } catch (Exception ex2) {
                ex2.printStackTrace();
                export_log.error(ex2.getMessage());
            }
        }
        
        if (export_log.hasError()) {
            export_log.info("Export finished with errors!", null);
        } else {
            export_log.info("Export finished!", null);
        }

        try {
            if (is_epub) {
                pub.setExportProgressMessage("Validating... please wait");
                EPUBUtil.validateEPUBPublication(docmaSess, pub, export_log);
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            export_log.warningMsg("Abnormal termination of validation process caused by exception: " + ex.getMessage());
        }

        // Saving log file
        try {
            pub.setExportProgressMessage("Export finished. Writing log...");
            pub.setLogCounters(export_log.getErrorCount(), export_log.getWarningCount());
            archive.writeExportLog(publicationId, export_log);
            pub.setExportProgressMessage("Export finished.");
        } catch (Exception ex3) {
            ex3.printStackTrace();
        }
        export_ctx.finished();
        removeFromGlobalExportQueue(publicationId);
    }

    void deletePublication(String publicationId)
    {
        openPublicationArchive();
        publicationMap.remove(publicationId);
        archive.deletePublication(publicationId);
        removeFromGlobalExportQueue(publicationId);
    }

    void previewPDF(OutputStream outstream,
                    String node_id,
                    DocmaExportContext export_ctx)
    throws DocException
    {
        String max_str = export_ctx.getApplicationProperty(DocmaConstants.PROP_PREVIEW_MAX_NODES_PRINT);
        int max_node_cnt = 0;
        if ((max_str != null) && (max_str.length() > 0)) {
            try {
                max_node_cnt = Integer.parseInt(max_str);
            } catch (Exception nfe) {}
        }
        exportPDF(outstream, node_id, export_ctx, null, max_node_cnt);
    }

    void exportPDF(OutputStream outstream,
                   String node_id,
                   DocmaExportContext export_ctx) throws DocException
    {
        final int MAX_NODE_CNT = 0;  // allow unlimited number of nodes
        exportPDF(outstream, node_id, export_ctx, null, MAX_NODE_CNT);
    }

    void exportPDF(OutputStream outstream,
                   String node_id,
                   DocmaExportContext export_ctx, 
                   SortedMap fileurl_map) 
    throws DocException
    {
        final int MAX_NODE_CNT = 0;  // allow unlimited number of nodes
        exportPDF(outstream, node_id, export_ctx, fileurl_map, MAX_NODE_CNT);
    }
    
    private void exportPDF(OutputStream outstream,
                           String node_id,
                           DocmaExportContext export_ctx, 
                           SortedMap fileurl_map, 
                           int max_node_count)
    throws DocException
    {
        DocmaPublicationConfig pub_conf = export_ctx.getPublicationConfig();
        DocmaOutputConfig out_conf = export_ctx.getOutputConfig();
        ExportLog export_log = export_ctx.getExportLog();
        try {
            FormattingEngine formatter = docmaSess.getFormatter();
            String image_base_URL = docmaSess.getOpenedStoreMediaBaseURL();

            if (pub_conf == null) {
                pub_conf = formatter.getDefaultPublicationConfig();
                export_ctx.setPublicationConfig(pub_conf);
                if (export_log != null) export_log.infoMsg("Using default publication configuration.");
            }
            if (out_conf == null) {
                out_conf = formatter.getDefaultPDFOutputConfig();
                export_ctx.setOutputConfig(out_conf);
                if (export_log != null) export_log.infoMsg("Using default PDF output configuration.");
            }

            loadLanguageConfig(out_conf, export_log);

            String styleVariant = out_conf.getStyleVariant();
            DocmaStyle[] styles = docmaSess.getStyles(styleVariant);

            String[] applics = DocmaAppUtil.getFilterApplics(pub_conf, out_conf);
            out_conf.setEffectiveFilterApplics(applics);

            if (node_id == null) node_id = pub_conf.getContentRoot();
            else {
                if (! node_id.equals(pub_conf.getContentRoot())) {
                    out_conf.setPdfBookmarks(false);  // create bookmarks only for complete publication
                    out_conf.setPdfDoubleSided(false);  // double-sided output only for complete publication
                    // Note: Double-sided output requires setting of fop1.extensions
                    // parameter. But this also activates generation of PDF bookmarks.
                    // Therefore, to suppress bookmarks for partial previews both
                    // options have to be deactivated.
                }
            }
            DocmaNode node = docmaSess.getNodeById(node_id);
            StringBuilder html_buf;
            try {
                html_buf = getPrintInstance(node, export_ctx, max_node_count);
            } catch (DocmaLimitException dle) {
                String gui_lang = export_ctx.getGUILanguage();
                gui_lang = (gui_lang != null) ? gui_lang.toLowerCase() : "en"; 
                String msg = dle.getLocalizedMessage();
                msg = (msg != null) ? msg.replace("<", "&lt;").replace(">", "&gt;") : "";
                html_buf = new StringBuilder();
                html_buf.append("<html><body class=\"article\" lang=\"" + gui_lang + "\" ><p><b>");
                html_buf.append("Preview failed. ").append(msg);
                html_buf.append("</b></p></body></html>");
            }

            // Transform URLs
            boolean direct_access = (image_base_URL != null) && (image_base_URL.length() > 0);
            SortedMap imgurl_map = null;
            if (! direct_access) {  
                // image-url map is needed because images have to be exported in temporary folder
                imgurl_map = new TreeMap();
            }
            ImageURLTransformer imgTransformer =
                    new ImageURLTransformer_PDF(docmaSess, applics, export_log, imgurl_map, direct_access);
            FileURLTransformer fileTransformer =
                    new FileURLTransformer_Default(docmaSess, applics, export_log, fileurl_map);
            StringBuilder temp_buf = new StringBuilder(html_buf.length() + 10000);
            final boolean REMOVE_FILE_LINKS = true;
            transformLinks(html_buf, imgTransformer, fileTransformer, null, REMOVE_FILE_LINKS, temp_buf, export_ctx);
            html_buf.setLength(0);
            DocmaAppUtil.transformImageURLs(temp_buf.toString(), imgTransformer, html_buf);
            String html = html_buf.toString();

            // Write debugging output
            if (DocmaConstants.DEBUG) {
                FileWriter fw = new FileWriter(new File(DocmaConstants.DEBUG_DIR, "printinstance.html"));
                fw.write(html); fw.close();
                // outstream = new FileOutputStream("c:\\TEMP\\printinstance.pdf");
            }

            // Set path to draft watermark image
            if (pub_conf.isDraft()) {
                String draft_url = imgTransformer.getImageURLByAlias("draft_watermark");
                out_conf.setDraftImagePath(draft_url);
            }
            
            // Set cover image for output
            String cover_alias = getEffectiveCoverImageAlias(pub_conf, applics);
            out_conf.setCoverImagePath((cover_alias == null) ? null : imgTransformer.getImageURLByAlias(cover_alias));
            
            // Set custom header/footer
            if (out_conf.isPdfCustomHeaderFooter()) {
                String headfoot_xsl = PdfHeaderFooterUtil.getHeaderFooterXsl(export_ctx, imgTransformer, styles);
                out_conf.setPdfCustomHeaderFooterXsl(headfoot_xsl);
            }
            
            // Set custom FOP configuration file
            String fop_conf = docmaSess.getApplicationProperty(DocmaConstants.PROP_FOP_CUSTOM_CONFIG_FILE);
            if ((fop_conf != null) && fop_conf.trim().equals("")) {
                fop_conf = null;
            }
            if (fop_conf != null) {
                File fop_file = new File(fop_conf);
                if (! fop_file.isAbsolute()) {
                    String base_dir = docmaSess.getApplicationProperty(DocmaConstants.PROP_BASE_PATH);
                    fop_conf = (new File(base_dir, fop_conf)).getAbsolutePath();
                    fop_file = new File(fop_conf);
                }
                String fn = fop_file.getName();
                String lang = docmaSess.getLanguageCode().toLowerCase();
                int p = fn.lastIndexOf('.');
                String fn_lang;
                if (p < 0) {
                    fn_lang = fn + "_" + lang; 
                } else {
                    fn_lang = fn.substring(0, p) + "_" + lang + fn.substring(p);
                }
                File lang_file = new File(fop_file.getParent(), fn_lang);
                if (lang_file.exists()) {
                    fop_conf = lang_file.getAbsolutePath();
                }
            }
            out_conf.setPdfCustomFOPConfigPath(fop_conf);
            
            // If images cannot be directly accessed (e.g. in case of database storage),
            // then export images into temporary folder.
            File img_temp_dir = null;
            if (! direct_access) {
                img_temp_dir = docmaSess.getFormatter().createTempDir();
                writeReferencedImagesToTempFolder(imgurl_map, img_temp_dir);
                image_base_URL = img_temp_dir.getAbsoluteFile().toURI().toURL().toString();
            }

            StreamSource instream = new StreamSource(new StringReader(html));
            // instream.setSystemId(imagepath);
            formatter.formatHTML2PDF(instream, outstream, image_base_URL, pub_conf, out_conf, styles, export_log);
            // outstream.close();
            
            if (! direct_access) {  // delete temporary folder
                deleteFolderInBackgroundThread(img_temp_dir);
            }
        } catch (Exception ex) {
            if (ex instanceof DocException) throw (DocException) ex;
            else throw new DocRuntimeException(ex);
        }
    }

    void exportHTML(OutputStream outstream,
                    String node_id,
                    DocmaExportContext export_ctx)
    throws DocException
    {
        DocmaPublicationConfig pub_conf = export_ctx.getPublicationConfig();
        DocmaOutputConfig out_conf = export_ctx.getOutputConfig();
        ExportLog export_log = export_ctx.getExportLog();
        File outDir = null;
        try {
            FormattingEngine formatter = docmaSess.getFormatter();
            String image_base_URL = docmaSess.getOpenedStoreMediaBaseURL();

            if (pub_conf == null) {
                pub_conf = formatter.getDefaultPublicationConfig();
                export_ctx.setPublicationConfig(pub_conf);
                if (export_log != null) export_log.infoMsg("Using default publication configuration.");
            }
            if (out_conf == null) {
                out_conf = formatter.getDefaultHTMLOutputConfig();
                export_ctx.setOutputConfig(out_conf);
                if (export_log != null) export_log.infoMsg("Using default HTML output configuration.");
            }

            loadLanguageConfig(out_conf, export_log);

            String subformat = out_conf.getSubformat();
            boolean is_webhelp = subformat.startsWith("webhelp");
            boolean is_webhelp_1 = subformat.equals("webhelp1");
            boolean is_webhelp_new = is_webhelp && !is_webhelp_1;
            boolean is_epub = subformat.startsWith("epub");

            // String styleVariant = out_conf.getStyleVariant();
            DocmaStyle[] styles = null; // not needed for HTML transformation; docmaSess.getStyles(styleVariant);

            String[] applics = DocmaAppUtil.getFilterApplics(pub_conf, out_conf);
            out_conf.setEffectiveFilterApplics(applics);

            if (node_id == null) node_id = pub_conf.getContentRoot();
            DocmaNode node = docmaSess.getNodeById(node_id);
            StringBuilder html_buf = getPrintInstance(node, export_ctx);

            // Transform URLs
            SortedMap url_map = new TreeMap();
            ImageURLTransformer imgTransformer =
                    new ImageURLTransformer_HTML(docmaSess, applics, export_log, url_map);
            FileURLTransformer fileTransformer =
                    new FileURLTransformer_Default(docmaSess, applics, export_log, url_map);
            StringBuilder temp_buf = new StringBuilder(html_buf.length() + 10000);
            final boolean REMOVE_FILE_LINKS = false;
            transformLinks(html_buf, imgTransformer, fileTransformer, null, REMOVE_FILE_LINKS, temp_buf, export_ctx);
            html_buf.setLength(0);
            DocmaAppUtil.transformImageURLs(temp_buf.toString(), imgTransformer, html_buf);
            String html = html_buf.toString();

            // Write debugging output
            if (DocmaConstants.DEBUG) {
                FileWriter fw = new FileWriter(new File(DocmaConstants.DEBUG_DIR, "printinstance.html"));
                fw.write(html); fw.close();
            }

            // Set HTML header content
            String head_cont = resolveContent(out_conf.getHtmlCustomHeaderAlias(), imgTransformer, export_ctx);
            out_conf.setHtmlCustomHeaderContent(head_cont);
            // Set HTML footer content
            String foot_cont = resolveContent(out_conf.getHtmlCustomFooterAlias(), imgTransformer, export_ctx);
            out_conf.setHtmlCustomFooterContent(foot_cont);
            // Set draft watermark image
            if (pub_conf.isDraft()) {
                String draft_url = imgTransformer.getImageURLByAlias("draft_watermark");
                out_conf.setDraftImagePath(draft_url);
            }
            // Set cover image for output
            String cover_alias = getEffectiveCoverImageAlias(pub_conf, applics);
            out_conf.setCoverImagePath((cover_alias == null) ? null : imgTransformer.getImageURLByAlias(cover_alias));
            // Add navigational icons (note: for WebHelp2, navigational icons are added by the design package)
            if (out_conf.isHtmlNavigationalIcons() && !is_webhelp_new) {
                String nav_url = imgTransformer.getImageURLByAlias("home");
                imgTransformer.getImageURLByAlias("up");
                imgTransformer.getImageURLByAlias("next");
                imgTransformer.getImageURLByAlias("prev");
                if (nav_url != null) {
                    int p = nav_url.lastIndexOf('.');
                    if (p > 0) {
                        out_conf.setHtmlNavigationIconsExt(nav_url.substring(p));
                    } else {
                        if (export_log != null) export_log.errorMsg("Navigation icon filename has no extension: " + nav_url);
                    }
                    p = nav_url.lastIndexOf('/');
                    out_conf.setHtmlNavigationIconsPath(nav_url.substring(0, p + 1));
                }
            }

            DocmaNode conf_folder = docmaSess.getNodeByAlias(DocmaConstants.HTML_CONFIG_FOLDER_ALIAS_NAME);
            
            // Set custom HTML head tags in output configuration
            setCustomHTMLMetaTags(out_conf, conf_folder, export_log);

            // Create temporary output directories
            outDir = formatter.createTempDir();
            String root_url_path = is_epub ? "" : getHTMLRootUrlPath(out_conf);  // root path (using slash as folder separator)
            File rootDir;
            if (root_url_path.equals("")) {  // epub output or no root-folder configured
                rootDir = outDir;
            } else {
                String root_file_path = (File.separatorChar == '/') ? root_url_path : 
                                                                      root_url_path.replace('/', File.separatorChar);
                rootDir = new File(outDir, root_file_path);
                if (! rootDir.mkdirs()) {
                    if (export_log != null) {
                        export_log.errorMsg("Could not create output directory: " + rootDir.getAbsolutePath());
                    }
                    rootDir = outDir;  // fall back to base folder
                }
            }
            
            // Transform print instance to HTML
            formatter.formatHTML2HTML(html, rootDir, image_base_URL, pub_conf, out_conf, styles, export_log);

            if (is_webhelp) { 
                WebHelpBuildUtil.writeTemplateFiles(conf_folder, out_conf, rootDir, export_log);
            }
            if (! (is_webhelp || is_epub)) {  // if static HTML output
                // Create cover-image page for static HTML output.
                // Note: The cover-image page will be automatically created by
                // the ePUB stylesheet. The Web-Help output currently does not
                // support cover-page generation.
                String cover_img_url = out_conf.getCoverImagePath();  // see setCoverImagePath() above
                if ((cover_img_url != null) && (cover_img_url.trim().length() > 0)) {
                    FormatterUtil.createHTMLCoverPage(rootDir, cover_img_url, export_log);
                }
            }
            
            // Write custom files (to outDir)
            FormatterUtil.writeCustomOutputFiles(outDir, out_conf, docmaSess, export_ctx);

            ZipOutputStream zipout = new ZipOutputStream(outstream);

            if (is_epub) {
                // Add all files in outDir/META-INF and outDir/OEBPS to zip
                EPUBUtil.addMIMETypeFile(zipout);
                File metainf = new File(rootDir, "META-INF");
                File oebps = new File(rootDir, "OEBPS");
                if (metainf.exists()) ZipUtil.addDirectoryToZip(zipout, metainf, "META-INF");
                if (oebps.exists()) ZipUtil.addDirectoryToZip(zipout, oebps, "OEBPS");
            } else {  // WebHelp and static HTML
                // If files have been written to a sub-folder, add index.html 
                // to outDir, which redirects to index.html in the sub-folder.
                if (! root_url_path.equals("")) {  // if HTML was wriiten to sub-folder 
                    File indexHtml = new File(outDir, "index.html");
                    File indexHtm = new File(outDir, "index.htm");
                    if (! (indexHtml.exists() || indexHtm.exists())) {  // if no custom index file exists
                        FormatterUtil.createHTMLRedirectPage(indexHtml, root_url_path + "index.html", export_log);
                    }
                }
                // Add all files in outDir to zip
                ZipUtil.addDirectoryToZip(zipout, outDir);
            }

            // Add CSS file to zip
            String cssfn = DocmaConstants.HTML_CSS_FILENAME;
            if (is_epub) cssfn = "OEBPS/" + cssfn;
            else if (is_webhelp_1) cssfn = root_url_path + "content/" + cssfn;
            else {  // static HTML, WebHelp2
                if (! root_url_path.equals("")) cssfn = root_url_path + cssfn;
            }
            ZipEntry ze = new ZipEntry(cssfn);
            zipout.putNextEntry(ze);
            OutputStreamWriter writer = new OutputStreamWriter(zipout);
            DocmaAppUtil.exportContentCSS(docmaSess, out_conf, writer);
            // Insert custom CSS if configured
            String css_fn = out_conf.getHtmlCustomCSSFilename();
            if ((css_fn != null) && (css_fn.length() > 0) && (conf_folder != null) && conf_folder.isFolder()) {
                DocmaNode css_node = conf_folder.getChildByFilename(css_fn);
                if (css_node != null) {
                    writer.append("\n").append(css_node.getContentString());
                } else {
                    if (export_log != null) export_log.warningMsg("Custom CSS not found in configuration folder: " + css_fn);
                }
            }
            writer.flush();
            zipout.closeEntry();

            // Add custom JavaScript file to zip
            byte[] js_content = null;
            String js_fn = out_conf.getHtmlCustomJSFilename();
            if ((js_fn != null) && (js_fn.length() > 0) && (conf_folder != null) && conf_folder.isFolder()) {
                DocmaNode js_node = conf_folder.getChildByFilename(js_fn);
                if (js_node != null) js_content = js_node.getContent();
            }
            if (is_webhelp && (js_content == null)) {
                js_content = "// Put your custom JavaScript code here\n".getBytes("UTF-8");
            }
            if (js_content != null) {
                if (is_webhelp_1) { js_fn = root_url_path + "content/custom.js"; }
                else if (is_webhelp) { js_fn = root_url_path + "custom.js"; }
                else if (is_epub) { js_fn = "OEBPS/" + js_fn; }
                else {  // static HTML
                    if (! root_url_path.equals("")) js_fn = root_url_path + js_fn;
                }
                ze = new ZipEntry(js_fn);
                zipout.putNextEntry(ze);
                zipout.write(js_content);
                // zipout.flush();
                zipout.closeEntry();
            }

            // Add referenced images/files to zip
            addReferencedFilesToZip(url_map, zipout, root_url_path, is_webhelp_1, is_epub);

            zipout.close();
            // outstream.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            if (ex instanceof DocException) throw (DocException) ex;
            else throw new DocRuntimeException(ex);
        } finally {
            if ((outDir != null) && !DocmaConstants.DEBUG) {
                try {
                    DocmaUtil.recursiveFileDelete(outDir);
                } catch (Exception ex) {}
            }
        }
    }
    
    private String getHTMLRootUrlPath(DocmaOutputConfig out_conf)
    {
        String path = out_conf.getHtmlRootFolder();
        if (path == null) {
            return "";
        }
        path = path.trim().replace('\\', '/');
        if (path.equals("") || path.equals("/")) {
            return "";
        }
        return path.endsWith("/") ? path : path + "/";
    }
    
    private void setCustomHTMLMetaTags(DocmaOutputConfig out_conf, 
                                       DocmaNode conf_folder, 
                                       ExportLog export_log)
    {
        String meta_fn = out_conf.getHtmlCustomMetaFilename();
        String custom_head_tags = null;  // null means: use default head tags
        if ((meta_fn != null) && (meta_fn.length() > 0) && (conf_folder != null) && conf_folder.isFolder()) {
            DocmaNode meta_node = conf_folder.getChildByFilename(meta_fn);
            if (meta_node != null) {
                String xmlstr = meta_node.getContentString();
                xmlstr = (xmlstr == null) ? "" : xmlstr.trim();
                if (xmlstr.length() > 0) { // if not whitespace only: check if it's well-formed xml
                    try {
                        DocmaUtil.checkWellFormedXML(xmlstr);
                    } catch (Exception wfex) {
                        xmlstr = null;  // use default head tags if custom head tags are not well-formed
                        if (export_log != null) {
                            export_log.errorMsg("Custom HTML meta tag file '" + 
                                                meta_fn + "' does not contain valid XML: " +
                                                wfex.getMessage());
                        }
                    }
                }
                custom_head_tags = xmlstr;
            } else {
                if (export_log != null) {
                    export_log.errorMsg("Custom HTML meta tag file '" + meta_fn + 
                                        "' not found in configuration folder.");
                }
            }
        }
        out_conf.setHtmlCustomHeadTags(custom_head_tags);
    }

    void exportDocBook(OutputStream outstream,
                       String node_id,
                       DocmaExportContext export_ctx)
    throws DocException
    {
        DocmaPublicationConfig pub_conf = export_ctx.getPublicationConfig();
        DocmaOutputConfig out_conf = export_ctx.getOutputConfig();
        ExportLog export_log = export_ctx.getExportLog();
        try {
            FormattingEngine formatter = docmaSess.getFormatter();

            if (pub_conf == null) {
                pub_conf = formatter.getDefaultPublicationConfig();
                export_ctx.setPublicationConfig(pub_conf);
                if (export_log != null) export_log.infoMsg("Using default publication configuration.");
            }
            if (out_conf == null) {
                out_conf = formatter.getDefaultDocBookOutputConfig();
                export_ctx.setOutputConfig(out_conf);
                if (export_log != null) export_log.infoMsg("Using default DocBook output configuration.");
            }

            String[] applics = DocmaAppUtil.getFilterApplics(pub_conf, out_conf);
            out_conf.setEffectiveFilterApplics(applics);

            if (node_id == null) node_id = pub_conf.getContentRoot();
            DocmaNode node = docmaSess.getNodeById(node_id);
            StringBuilder html_buf = getPrintInstance(node, export_ctx);

            // Transform URLs
            SortedMap url_map = new TreeMap();
            ImageURLTransformer imgTransformer =
                    new ImageURLTransformer_HTML(docmaSess, applics, export_log, url_map);
            // Note: For DocBook, the same image URL transformation can be applied as for HTML export.
            FileURLTransformer fileTransformer =
                    new FileURLTransformer_Default(docmaSess, applics, export_log, url_map);
            StringBuilder temp_buf = new StringBuilder(html_buf.length() + 10000);
            final boolean REMOVE_FILE_LINKS = false;
            transformLinks(html_buf, imgTransformer, fileTransformer, null, REMOVE_FILE_LINKS, temp_buf, export_ctx);
            html_buf.setLength(0);
            DocmaAppUtil.transformImageURLs(temp_buf.toString(), imgTransformer, html_buf);
            String html = html_buf.toString();

            // Write debugging output
            if (DocmaConstants.DEBUG) {
                FileWriter fw = new FileWriter(new File(DocmaConstants.DEBUG_DIR, "printinstance.html"));
                fw.write(html); fw.close();
            }

            ZipOutputStream zipout = new ZipOutputStream(outstream);

            // Add DocBook file to zip
            ZipEntry ze = new ZipEntry("docbook.xml");
            zipout.putNextEntry(ze);
            // Transform print instance to HTML
            StreamSource instream = new StreamSource(new StringReader(html));
            // instream.setSystemId(imagepath);
            formatter.formatHTML2DocBook(instream, zipout, pub_conf, out_conf, export_log);
            zipout.closeEntry();

            // Add CSS file to zip
            ze = new ZipEntry(DocmaConstants.HTML_CSS_FILENAME);
            zipout.putNextEntry(ze);
            OutputStreamWriter writer = new OutputStreamWriter(zipout);
            DocmaAppUtil.exportContentCSS(docmaSess, out_conf, writer);
            writer.flush();
            zipout.closeEntry();

            // Add referenced images/files to zip
            addReferencedFilesToZip(url_map, zipout);

            zipout.close();
            // outstream.close();
        } catch (Exception ex) {
            if (ex instanceof DocException) throw (DocException) ex;
            else throw new DocRuntimeException(ex);
        }
    }

    boolean exportsExist(String storeId, DocVersionId verId, String versionState)
    {
        openPublicationArchive(storeId, verId);
        String[] pub_ids = archive.listPublications();
        if (versionState == null) {
            return (pub_ids.length > 0);
        } else {
            if (pub_ids.length == 0) return false;

            DocmaPublication[] pubs = listPublications(null);
            for (int i=0; i < pubs.length; i++) {
                String state = pubs[i].getPublicationState();
                if (versionState.equals(state)) return true;
            }
            return false;
        }
    }

    static boolean isExportFinished(String storeId, DocVersionId verId, String publicationId)
    {
        return (getExportJobPosition(storeId, verId, publicationId) < 0);
    }

    static synchronized int getExportJobPosition(String storeId,
                                                 DocVersionId verId,
                                                 String publicationId)
    {
        for (int i=0; i < globalExportQueue.size(); i++) {
            DocmaExportJob job = (DocmaExportJob) globalExportQueue.get(i);
            if (storeId.equals(job.getStoreId()) &&
                verId.equals(job.getVersionId()) &&
                publicationId.equals(job.getPublicationId())) {
                return i;
            }
        }
        return -1;
    }

    static synchronized DocmaExportJob[] getExportQueue()
    {
        return (DocmaExportJob[]) globalExportQueue.toArray(new DocmaExportJob[globalExportQueue.size()]);
    }

    static synchronized DocmaExportJob getExportJob(String storeId,
                                                    DocVersionId verId,
                                                    String publicationId)
    {
        Iterator it = globalExportQueue.iterator();
        while (it.hasNext()) {
            DocmaExportJob job = (DocmaExportJob) it.next();
            if (storeId.equals(job.getStoreId()) &&
                verId.equals(job.getVersionId()) &&
                publicationId.equals(job.getPublicationId())) {
                return job;
            }
        }
        return null;
    }

    void invalidateArchive(String storeId, DocVersionId verId) 
    {
        if (archive != null) {
            if (archive.getDocStoreId().equals(storeId) &&
                ((verId == null) || archive.getVersionId().equals(verId))) {
                archive = null;
            }
        }
        // if (verId == null) {
        //     allArchives.invalidateCache(storeId);
        // } else {
        //     allArchives.invalidateCache(storeId, verId);
        // }
    }

    /* --------------  private methods  ---------------------- */

    private void writeReferencedImagesToTempFolder(SortedMap img_url_map, File tempDir)
    {
        final boolean MAP_URL_TO_FILE = !File.separator.equals("/");
        
        Iterator it = img_url_map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String img_url = (String) entry.getKey();
            if (MAP_URL_TO_FILE) {
                img_url = img_url.replace('/', File.separatorChar);
            }
            DocmaNode imgfile_node = (DocmaNode) entry.getValue();
            
            File outfile = new File(tempDir, img_url);
            if (img_url.contains(File.separator)) {
                File parentDir = outfile.getParentFile();
                if (! parentDir.exists()) parentDir.mkdirs();
            }
            
            InputStream img_stream = imgfile_node.getContentStream();
            try {
                if (DocmaConstants.DEBUG) {
                    Log.info("Writing image stream to temporary file " + outfile.getAbsolutePath());
                }
                DocmaUtil.writeStreamToFile(img_stream, outfile);
            } catch (Exception ex) {
                throw new DocRuntimeException("Could not export image to temporary folder: " + outfile, ex);
            } finally {
                try { img_stream.close(); } catch (Exception ex) { ex.printStackTrace(); }
            }
        }
    }
    
    private void deleteFolderInBackgroundThread(final File folder) 
    {
        Thread th = new Thread(new Runnable() {

            public void run() {
                try {  Thread.sleep(200); } catch (Exception ex) { ex.printStackTrace(); }
                DocmaUtil.recursiveFileDelete(folder);
            }
            
        });
        try {
            th.start();
        } catch (Exception ex) {
            Log.error("Failed to start deleteFolderInBackgroundThread(): " + ex.getMessage());
        }
    }
    
    private void addReferencedFilesToZip(SortedMap url_map, 
                                         ZipOutputStream zipout)
    throws IOException
    {
        addReferencedFilesToZip(url_map, zipout, "", false, false);
    }
    
    private void addReferencedFilesToZip(SortedMap url_map, 
                                         ZipOutputStream zipout, 
                                         String root_url_path,
                                         boolean is_webhelp_1, 
                                         boolean is_epub) 
    throws IOException
    {
            Iterator it = url_map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                String file_url = (String) entry.getKey();
                DocmaNode imgfile_node = (DocmaNode) entry.getValue();

                if (is_epub) file_url = "OEBPS/" + file_url;
                else if (is_webhelp_1) file_url = root_url_path + "content/" + file_url;
                else {  // static HTML, WebHelp2
                    if (! root_url_path.equals("")) file_url = root_url_path + file_url;
                }
                ZipEntry file_ze = new ZipEntry(file_url);
                zipout.putNextEntry(file_ze);
                InputStream file_stream = imgfile_node.getContentStream();
                try {
                    DocmaUtil.copyStream(file_stream, zipout);
                } finally {
                    file_stream.close();
                }
                zipout.closeEntry();
            }
    }
    
    private String resolveContent(String node_alias,
                                  ImageURLTransformer url_transformer,
                                  DocmaExportContext export_ctx)
    {
        if ((node_alias != null) && (node_alias.length() > 0)) {
            ExportLog export_log = export_ctx.getExportLog();
            node_alias = node_alias.trim();
            DocmaNode node = docmaSess.getNodeByAlias(node_alias);
            if (node == null) {
                export_log.errorMsg("Content node with alias '" + node_alias + "' not found.");
                return null;
            }
            if (! node.isHTMLContent()) {
                export_log.errorMsg("Node with alias '" + node_alias + "' has wrong node type.");
                return null;
            }
            String cont = 
                ContentResolver.getContentRecursive(node, new HashSet(), export_ctx,
                                                    true, 0, null, false, null);
            StringBuilder html_buf = new StringBuilder();
            DocmaAppUtil.transformImageURLs(cont, url_transformer, html_buf);
            return html_buf.toString();
        } else {
            return null;
        }
    }

    private void loadLanguageConfig(DocmaOutputConfig out_conf, ExportLog export_log)
    {
        String langCode = docmaSess.getLanguageCode();
        out_conf.setLanguageCode(langCode);
        DocmaNode nd = docmaSess.getGenTextNode();
        if (nd == null) {
            out_conf.setGentextProps(null);
            if (export_log != null) export_log.infoMsg("No gentext file found.");
        } else {
            byte[] arr = nd.getContent();
            Properties props = new Properties();
            if (arr != null) {
                try {
                    props.load(new ByteArrayInputStream(arr));
                } catch (Exception ex) {
                    if (export_log != null) {
                        export_log.warning("publication.export.invalid_gentext");
                    }
                    Log.warning("Unable to load gentext file: " + ex.getMessage());
                }
            }
            out_conf.setGentextProps(props);
        }
    }

    private String getEffectiveCoverImageAlias(DocmaPublicationConfig pub_conf, 
                                               String[] filter_applics) 
    {
        return getEffectiveCoverImageAlias(pub_conf, filter_applics, null);
    }
    
    private String getEffectiveCoverImageAlias(DocmaPublicationConfig pub_conf, 
                                               String[] filter_applics, 
                                               ExportLog export_log) 
    {
        String alias = pub_conf.getCoverImageAlias();
        if (alias == null) {
            return null;
        }
        alias = alias.trim();
        if (alias.length() == 0) {
            return null;
        }
        if (pub_conf.isDraft()) {
            // In draft mode: If draft-cover exists show draft-cover.
            // If draft-cover is not applicable, suppress cover (i.e. return null).
            String draft_alias = alias + "_draft";
            if (docmaSess.getNodesByLinkAlias(draft_alias).length > 0) {  // if draft-cover exists
                if (docmaSess.getApplicableNodeByLinkAlias(draft_alias, filter_applics) != null) {
                    return draft_alias;
                } else {
                    if (export_log != null) {
                        export_log.info("text.cover_not_applicable", new Object[] { draft_alias });
                    }
                    return null; // suppress cover if draft-cover is not applicable
                }
            }
            // If draft-cover does not exist, then use normal cover
        }
        // If cover is not applicable or not found, suppress cover (i.e. return null).
        if (docmaSess.getNodesByLinkAlias(alias).length > 0) {  // if cover exists
            if (docmaSess.getApplicableNodeByLinkAlias(alias, filter_applics) != null) {
                return alias;
            } else {
                if (export_log != null) {
                    export_log.info("text.cover_not_applicable", new Object[] { alias });
                }
                return null; // suppress cover if cover is not applicable
            }
        } else {
            // Show warning if cover image with given alias does not exist.
            if (export_log != null) {
                export_log.warning("text.cover_not_found", new Object[] { alias });
            }
            return null; // suppress output of non-existing cover 
        }
    }
            
    private void insertDiv(StringBuilder html_buf, String divclass, String content)
    {
        if ((content != null) && !content.trim().equals("")) {
            html_buf.append("<div class=\"").append(divclass).append("\">").append(content).append("</div>");
        }
    }
    
    private void insertDivEncode(StringBuilder html_buf, String divclass, String content)
    {
        if ((content != null) && !content.trim().equals("")) {
            content = docmaSess.encodeCharEntities(content, false);
            html_buf.append("<div class=\"").append(divclass).append("\">").append(content).append("</div>");
        }
    }

    private String titlePageEncode(String content)
    {
        if (content == null) {
            return "";
        } else {
            return docmaSess.encodeCharEntities(content, false);
        }
    }

    private String getCustomTitlePagePrintInstance(DocmaNode titlepage_node, 
                                                   String pubtitle,
                                                   DocmaPublicationConfig pub_conf,
                                                   DocmaExportContext export_ctx, 
                                                   Set id_set) 
    {
        String authors = "";
        String author_list = "";
        String[] person_names = new String[0];
        String author_str = pub_conf.getAuthors();
        if ((author_str != null) && (author_str.trim().length() > 0)) {
            person_names = author_str.split("[\\n\\r]");  // split lines
            for (int i=0; i < person_names.length; i++) {
                person_names[i] = titlePageEncode(person_names[i]).trim();
            }
            authors = FormatterUtil.formatTitlePageAuthors(person_names);
            author_list = FormatterUtil.formatTitlePageAuthorList(person_names, export_ctx);
        }

        String credits = "";
        String credit_str = pub_conf.getCredit();
        String[] credit_names = new String[0];
        if ((credit_str != null) && (credit_str.trim().length() > 0)) {
            credit_names = credit_str.split("[\\n\\r]");  // split lines
            for (int i=0; i < credit_names.length; i++) {
                credit_names[i] = titlePageEncode(credit_names[i]).trim();
            }
            credits = FormatterUtil.formatTitlePageCredits(credit_names);
        }

        // Get the HTML of the node. Note that value 3 is passed as node_depth.
        // The node_depth has no effect for rendering of content nodes. 
        // However, if a section node is passed as custom title page, then 
        // the h4 tag is used for the section title. This is to avoid output 
        // of chapter/part-div inside the title-page.
        StringBuilder buf = new StringBuilder();
        final int MAX_NODE_CNT = 0;  // unlimited number of nodes
        DocmaAppUtil.getNodePrintInstance(buf, titlepage_node, 3, export_ctx, id_set, MAX_NODE_CNT);
        
        // Do not use a placeholder that is the prefix of another placeholder
        // (e.g. do not use placeholder %abc if &abcd is already used).
        // However, if a placeholder is a prefix of another placeholder, then
        // the longer placeholder has to be replaced first.
        String res = buf.toString();
        for (int count=1; count <= Math.min(9, person_names.length); count++) {
            res = res.replace("%author" + count, person_names[count - 1]);
        }
        for (int count=1; count <= Math.min(9, credit_names.length); count++) {
            res = res.replace("%credit" + count, credit_names[count - 1]);
        }
        String rel_info = FormatterUtil.replacePublicationConfigPlaceholders(docmaSess, pub_conf.getReleaseInfo());
        String pub_dt = FormatterUtil.replacePublicationConfigPlaceholders(docmaSess, pub_conf.getPubDate());
        String copy_yr = FormatterUtil.replacePublicationConfigPlaceholders(docmaSess, pub_conf.getCopyrightYear());
        return res.replace("%pub_title",    titlePageEncode(pubtitle))
                  .replace("%pub_subtitle", titlePageEncode(pub_conf.getSubtitle()))
                  .replace("%corporate",    titlePageEncode(pub_conf.getCorporate()))
                  .replace("%release_info", titlePageEncode(rel_info))
                  .replace("%pub_date",     titlePageEncode(pub_dt))
                  .replace("%publisher",    titlePageEncode(pub_conf.getPublisher()))
                  .replace("%biblio_id",    titlePageEncode(pub_conf.getBiblioId()))
                  .replace("%author_list",  author_list)
                  .replace("%authors",      authors)
                  .replace("%copy_year",    titlePageEncode(copy_yr))
                  .replace("%copy_holder",  titlePageEncode(pub_conf.getCopyrightHolder()))
                  .replace("%credits",      credits);
    }

    StringBuilder getPrintInstance(DocmaNode node, DocmaExportContext export_ctx)
    throws DocException
    {
        final int MAX_NODE_CNT = 0;  // unlimited number of nodes
        return getPrintInstance(node, export_ctx, MAX_NODE_CNT);
    }

    private StringBuilder getPrintInstance(DocmaNode node, 
                                           DocmaExportContext export_ctx, 
                                           int max_node_count)
    throws DocException, DocmaLimitException
    {
        if (node == null) {
            throw new DocException("Cannot get print instance: Node is null.");
        }
        DocmaPublicationConfig pub_conf = export_ctx.getPublicationConfig();
        DocmaOutputConfig out_conf = export_ctx.getOutputConfig();
        ExportLog export_log = export_ctx.getExportLog();

        String c_root = pub_conf.getContentRoot();
        boolean no_c_root = (c_root == null) || c_root.equals("");
        // If no content root is defined in the publication configuration, then
        // the document root node is assumed to be the root of the publication.
        int n_level = no_c_root ? node.getDepth() : // depth relative to document root node
                                  node.getDepthRelativeTo(c_root);
        // n_level == 0 means that node is the root of the publication. 
        boolean complete_publication = (n_level == 0);
        // boolean complete_publication = no_c_root ? node.isDocumentRoot() : 
        //                                            node.getId().equals(c_root);
        String lang = node.getDocmaSession().getLanguageCode().toLowerCase();
        String format = out_conf.getFormat();
        boolean is_html = format.equalsIgnoreCase("html");
        String subformat = out_conf.getSubformat();
        // boolean is_webhelp = subformat.startsWith("webhelp");
        boolean is_epub = subformat.startsWith("epub");
        
        String content1stLevel = out_conf.getRender1stLevel();  // save value to restore later
        boolean is_part = "part".equals(content1stLevel);
        boolean node_is_content = node.isContent() || node.isContentIncludeReference();
        boolean node_is_part_or_chap = (n_level == 1) || ((n_level == 2) && is_part); 
        
        StringBuilder html_buf = new StringBuilder();
        if ((complete_publication || node_is_part_or_chap) && !node_is_content) {
            // The output is rendered as book (DocBook book element).
            // Note: For book elements the first level has to be a chapter or part. 
            // Two cases:
            // n_level == 0: the complete publication is rendered.
            // n_level == 1: only one chapter/part is rendered.
            html_buf.append("<html><body lang=\"" + lang + "\" >");
        } else {
            // The output is rendered as article (DocBook article element).
            // An article can contain just content or can be structured using
            // the section element. However, the first level of an article
            // cannot be a chapter or a part. To render a chapter/part the book
            // element has to be used (see else-part of the if-statement).
            html_buf.append("<html><body class=\"article\" lang=\"" + lang + "\" >");
            complete_publication = false; // Just in case a content node has been defined as content root.
        }

        Set id_set = new HashSet();
        if (complete_publication) {
            //
            // Preview of complete publication
            //
            String[] filter_applics = out_conf.getEffectiveFilterApplics();

            // Insert title page information
            html_buf.append("<div class=\"doc-info\">");
            String pubTitle = pub_conf.getTitle();
            String pubSubtitle = pub_conf.getSubtitle();
            String pubCorporate = pub_conf.getCorporate();
            String pubReleaseInfo = FormatterUtil.replacePublicationConfigPlaceholders(docmaSess, pub_conf.getReleaseInfo());
            String pubDate = FormatterUtil.replacePublicationConfigPlaceholders(docmaSess, pub_conf.getPubDate());
            String publisher = pub_conf.getPublisher();
            String biblioId = pub_conf.getBiblioId();
            String pubAuthors = pub_conf.getAuthors();
            String pubCopyYear = FormatterUtil.replacePublicationConfigPlaceholders(docmaSess, pub_conf.getCopyrightYear());
            String pubCopyHolder = pub_conf.getCopyrightHolder();
            String pubCredit = pub_conf.getCredit();
            String abstrAlias = pub_conf.getPubAbstract();
            String legalAlias = pub_conf.getLegalNotice();
            String titlePage1Alias = pub_conf.getCustomTitlePage1();
            String titlePage2Alias = pub_conf.getCustomTitlePage2();
            String coverimageAlias = getEffectiveCoverImageAlias(pub_conf, filter_applics, export_log);

            if (pubTitle.trim().equals("")) {
                pubTitle = node.getTitle();
            }
            if (is_epub && ((biblioId == null) || biblioId.trim().equals(""))) {
                // Create a unique book ID for EPUB output. Otherwise there might
                // be synchronization problems if book is uploaded to eBook
                // reader (e.g. existing eBook with same/no ID might be overwritten.)
                biblioId = pub_conf.getId() + "_" + System.currentTimeMillis();
            }
            insertDivEncode(html_buf, "doc-title", pubTitle);
            insertDivEncode(html_buf, "doc-subtitle", pubSubtitle);
            insertDivEncode(html_buf, "doc-corpauthor", pubCorporate);
            insertDivEncode(html_buf, "doc-releaseinfo", pubReleaseInfo);
            insertDivEncode(html_buf, "doc-pubdate", pubDate);
            insertDivEncode(html_buf, "doc-publisher", publisher);
            insertDivEncode(html_buf, "doc-biblioid", biblioId);
            if (! pubAuthors.trim().equals("")) {
                html_buf.append("<div class=\"doc-authorgroup\">");
                String[] authors = pubAuthors.split("[\\n\\r]");  // split lines
                for (int i=0; i < authors.length; i++) {
                    insertDivEncode(html_buf, "doc-author", authors[i]);
                }
                html_buf.append("</div>");
            }
            if (! (pubCopyYear.trim().equals("") && pubCopyHolder.trim().equals(""))) {
                html_buf.append("<div class=\"doc-copyright\">");
                // Note: year is mandatory element of copyright in DocBook DTD!
                html_buf.append("<div class=\"doc-year\">").append(pubCopyYear).append("</div>");
                insertDivEncode(html_buf, "doc-holder", pubCopyHolder);
                html_buf.append("</div>");
            }
            if (! pubCredit.trim().equals("")) {
                html_buf.append("<div class=\"doc-credit\">");
                String[] credits = pubCredit.split("[\\n\\r]");  // split lines
                for (int i=0; i < credits.length; i++) {
                    // insertDivEncode(html_buf, "doc-othername", credits[i]);
                    html_buf.append("<div class=\"doc-othername\">")
                            .append(docmaSess.encodeCharEntities(credits[i], false))
                            .append("</div>");
                }
                html_buf.append("</div>");
            }
            if (! titlePage1Alias.trim().equals("")) {
                DocmaNode p1Node = docmaSess.getApplicableNodeByLinkAlias(titlePage1Alias, filter_applics);
                if (p1Node != null) {
                    html_buf.append("<div class=\"doc-titlepage1\">");
                    // DocmaAppUtil.getNodePrintInstance(html_buf, p1Node, 0, export_ctx, id_set);
                    html_buf.append(getCustomTitlePagePrintInstance(p1Node, pubTitle, pub_conf, export_ctx, id_set));
                    html_buf.append("</div>");
                }
            }
            if (! titlePage2Alias.trim().equals("")) {
                DocmaNode p2Node = docmaSess.getApplicableNodeByLinkAlias(titlePage2Alias, filter_applics);
                if (p2Node != null) {
                    html_buf.append("<div class=\"doc-titlepage2\">");
                    // DocmaAppUtil.getNodePrintInstance(html_buf, p2Node, 0, export_ctx, id_set);
                    html_buf.append(getCustomTitlePagePrintInstance(p2Node, pubTitle, pub_conf, export_ctx, id_set));
                    html_buf.append("</div>");
                }
            }
            if (! abstrAlias.trim().equals("")) {
                DocmaNode abstrNode = docmaSess.getApplicableNodeByLinkAlias(abstrAlias, filter_applics);
                if (abstrNode != null) {
                    html_buf.append("<div class=\"doc-abstract\">");
                    final int h_start = 3;  // if node is section, then start with h4 tag
                    DocmaAppUtil.getNodePrintInstance(html_buf, abstrNode, h_start, export_ctx, id_set, max_node_count);
                    html_buf.append("</div>");
                }
            }
            if (! legalAlias.trim().equals("")) {
                DocmaNode legalNode = docmaSess.getApplicableNodeByLinkAlias(legalAlias, filter_applics);
                if (legalNode != null) {
                    html_buf.append("<div class=\"doc-legalnotice\">");
                    final int h_start = 3;  // if node is section, then start with h4 tag
                    DocmaAppUtil.getNodePrintInstance(html_buf, legalNode, h_start, export_ctx, id_set, max_node_count);
                    html_buf.append("</div>");
                }
            }
            if (is_html && (coverimageAlias != null) && (coverimageAlias.trim().length() > 0)) {
                insertDiv(html_buf, "doc-coverimage", "<img src=\"image/" + coverimageAlias + "\" />");
            }
            html_buf.append("</div>");  // close doc-info div

            // Insert preface nodes
            DocmaNode prefaceRoot = null;
            String prefaceRootId = pub_conf.getPrefaceRoot();
            if ((prefaceRootId != null) && !prefaceRootId.equals("")) {
                prefaceRoot = docmaSess.getNodeById(prefaceRootId);
            }
            if (prefaceRoot != null) {
                out_conf.setRender1stLevel("preface");

                int cnt = prefaceRoot.getChildCount();
                for (int i=0; i < cnt; i++) {
                    DocmaNode child_node = prefaceRoot.getChild(i);
                    if (! DocmaAppUtil.isApplicable(child_node, out_conf)) {
                        continue;
                    }
                    if (child_node.isSection() || child_node.isSectionIncludeReference()) {
                        DocmaAppUtil.getNodePrintInstance(html_buf, child_node, 1, export_ctx, id_set, max_node_count);
                    } else
                    if (child_node.isContent() || child_node.isContentIncludeReference()) {
                        // if (export_log != null) {
                        //     export_log.warning("publication.export.content_ignored_in_preface_root");
                        // }
                        String tit = child_node.getTitleEntityEncoded();
                        html_buf.append(DocmaAppUtil.getDivStart(child_node.getAlias(),
                                                                 "doc-preface", tit));
                        html_buf.append("<h2>").append(tit).append("</h2>");
                        DocmaAppUtil.getNodePrintInstance(html_buf, child_node, 2, export_ctx, id_set, max_node_count);
                        html_buf.append(DocmaAppUtil.getDivEnd());
                    }
                }
            }

            // Insert content nodes
            out_conf.setRender1stLevel(content1stLevel);   // restore value
            boolean before_sections = true;
            int cnt = node.getChildCount();
            for (int i=0; i < cnt; i++) {
                DocmaNode child_node = node.getChild(i);
                if (! DocmaAppUtil.isApplicable(child_node, out_conf)) {
                    continue;
                }
                if (child_node.isSection() || child_node.isSectionIncludeReference()) {
                    before_sections = false;
                    DocmaAppUtil.getNodePrintInstance(html_buf, child_node, 1, export_ctx, id_set, max_node_count);
                } else
                if (before_sections && (child_node.isContent() || child_node.isContentIncludeReference())) {
                    // Insert root content nodes as preface sections
                    String tit = child_node.getTitleEntityEncoded();
                    html_buf.append(DocmaAppUtil.getDivStart(null, "doc-preface", tit));
                    html_buf.append("<h2>").append(tit).append("</h2>");
                    DocmaAppUtil.getNodePrintInstance(html_buf, child_node, 2, export_ctx, id_set, max_node_count);
                    html_buf.append(DocmaAppUtil.getDivEnd());
                }
            }

            // Insert appendix nodes
            DocmaNode appendixRoot = null;
            String appendixRootId = pub_conf.getAppendixRoot();
            if ((appendixRootId != null) && !appendixRootId.equals("")) {
                appendixRoot = docmaSess.getNodeById(appendixRootId);
            }
            if (appendixRoot != null) {
                out_conf.setRender1stLevel("appendix");

                cnt = appendixRoot.getChildCount();
                for (int i=0; i < cnt; i++) {
                    DocmaNode child_node = appendixRoot.getChild(i);
                    if (! DocmaAppUtil.isApplicable(child_node, out_conf)) {
                        continue;
                    }
                    if (child_node.isSection() || child_node.isSectionIncludeReference()) {
                        DocmaAppUtil.getNodePrintInstance(html_buf, child_node, 1, export_ctx, id_set, max_node_count);
                    } else
                    if (child_node.isContent() || child_node.isContentIncludeReference()) {
                        LogUtil.addWarning(export_log, child_node, "publication.export.content_ignored_in_appendix_root");
                    }
                }
            }

            if (out_conf.isIndex()) {
                html_buf.append("<div class=\"doc-index\"></div>");
            }
        } else {
            //
            // Create preview of single part/chapter/section or preview of a content node.
            //
            out_conf.setToc(false);
            out_conf.setIndex(false);
            // out_conf.setRender1stLevel((n_level <= 1) ? "chapter" : "section");
            DocmaAppUtil.getNodePrintInstance(html_buf, node, n_level, export_ctx, id_set, max_node_count);
        }
        html_buf.append("</body></html>");

        out_conf.setRender1stLevel(content1stLevel);   // restore value
        return html_buf;
    }

    private void transformLinks(StringBuilder html_in, // Set id_set,
                                ImageURLTransformer imgTransformer,
                                FileURLTransformer fileTransformer,
                                String linkTarget,
                                boolean removeFileLinks,
                                StringBuilder html_out,
                                DocmaExportContext export_ctx)
    {
        if (html_in == html_out) {
            throw new DocRuntimeException("Input buffer is same as output buffer!");
        }
        ExportLog export_log = export_ctx.getExportLog();
        // boolean has_log = (export_log != null);
        HashSet alias_set = new HashSet();  // new HashSet((3 * id_set.size()) + 50);
        // for (Iterator it = id_set.iterator(); it.hasNext(); ) {
        //     String node_id = (String) it.next();
        //     DocmaNode nd = docmaSess.getNodeById(node_id);
        //     String link_alias = nd.getLinkAlias();
        //     if (link_alias != null) alias_set.add(link_alias);
        //     if (nd.isHTMLContent()) {
        //         DocmaAnchor[] anch = nd.getContentAnchors();
        //         for (int i=0; i < anch.length; i++) alias_set.add(anch[i].getAlias());
        //     }
        // }
        ContentUtil.getIdValues(html_in, alias_set);
        if (DocmaConstants.DEBUG) System.out.println("ID value count: " + alias_set.size());

        List attNames = new ArrayList();
        List attValues = new ArrayList();
        int copypos = 0;   // content from 0...copypos has already been copied to html_out
        int startpos = 0;  // continue search for next link tag at startpos
        int len = html_in.length();
        while (startpos < len) {
            final String A_PATTERN = "<a";
            int tag_start = html_in.indexOf(A_PATTERN, startpos);
            if (tag_start < 0) break;

            startpos = tag_start + 1;  // continue after this position
            int att_start = tag_start + A_PATTERN.length();
            if (! Character.isWhitespace(html_in.charAt(att_start))) continue;

            int tag_end = XMLParser.parseTagAttributes(html_in, att_start, attNames, attValues);
            if (tag_end < 0) continue;

            startpos = tag_end + 1;  // continue search after opening tag

            int href_idx = attNames.indexOf("href");
            if (href_idx < 0) continue;
            String href_val = (String) attValues.get(href_idx);
            final String IMAGE_PREFIX = "image/";
            final String FILE_PREFIX = "file/";
            boolean isContentLink = href_val.startsWith("#");
            boolean isImageLink = href_val.startsWith(IMAGE_PREFIX);
            boolean isFileLink = href_val.startsWith(FILE_PREFIX);

            if (! (isContentLink || isImageLink || isFileLink)) { 
                continue;  // do not modify external links and other unknown links
            }

            String target_alias = null;
            if (isContentLink) target_alias = href_val.substring(1).trim();
            else if (isImageLink) target_alias = href_val.substring(IMAGE_PREFIX.length()).trim();
            else if (isFileLink) target_alias = href_val.substring(FILE_PREFIX.length()).trim();

            final String END_PATTERN = "</a>";
            int a_end = html_in.indexOf(END_PATTERN, tag_end);
            if (a_end < 0) break;

            int after_link = a_end + END_PATTERN.length();
            startpos = after_link;  // continue search after closing tag

            if (target_alias.equals("")) {
                LogUtil.addWarning(export_log, html_in, tag_start, a_end, 
                    "publication.export.missing_target_alias", href_val);
                continue;
            }

            String link_text = html_in.substring(tag_end + 1, a_end);
            if (isContentLink) {
                // 1) target alias exists as ID in html_in: do not modify link
                // 2) target alias does not exist: do not modify link, write warning
                // 3) target alias exists but is not included in publiation: 
                // 3.1) target alias is in referenced publication: remove link, replace link text
                // 3.2) target alias is not(!) in referenced publication: remove link, write warning
                if (! alias_set.contains(target_alias)) {
                    if (docmaSess.getNodeIdByAlias(target_alias) == null) {
                        LogUtil.addWarning(export_log, html_in, tag_start, a_end, 
                            "publication.export.cannot_resolve_target_alias", target_alias);
                    } else {
                        String ref_title = export_ctx.getNodeTitleInReferencedPub(target_alias);
                        if (ref_title != null) {
                            link_text = ref_title;
                        } else {
                            LogUtil.addWarning(export_log, html_in, tag_start, a_end, 
                                "publication.export.target_not_included_replacing_txt",
                                target_alias, escapeTags(link_text));
                        }
                        // Remove the opening tag <a ... > and the closing tag </a>
                        html_out.append(html_in, copypos, tag_start);
                        html_out.append(link_text);
                        copypos = after_link;
                    }
                }
            } else {  // isImageLink or isFileLink
                // 1) if removeFileLinks is true: remove link (e.g. for print output)
                // 2) otherwise if target exists transform link
                // 3) otherwise do not modify link, write warning
                String url = null;
                if (isImageLink) url = imgTransformer.getImageURLByAlias(target_alias);
                if (isFileLink) url = fileTransformer.getFileURLByAlias(target_alias);

                if (removeFileLinks) {  // Currently: is only true for print-output
                    LogUtil.addInfo(export_log, html_in, tag_start, a_end, 
                        "publication.export.removing_file_link", href_val);
                    int title_idx = attNames.indexOf("title");
                    if (title_idx >= 0) { // If link has title, ...
                        // ... then replace link text with link title
                        String title_val = (String) attValues.get(title_idx);
                        if ((title_val != null) && (title_val.trim().length() > 0)) {
                            if (title_val.contains("%target%") || title_val.contains("%target_print%")) {
                                // If "Use target title" option is set, then use filename as title
                                int fn_start = url.lastIndexOf('/');
                                String fn = (fn_start < 0) ? url : url.substring(fn_start + 1);
                                link_text = (fn.trim().length() > 0) ? fn : url;
                            } else {
                                link_text = title_val;
                            }
                        }
                    }
                    html_out.append(html_in, copypos, tag_start);
                    html_out.append(link_text);
                    copypos = after_link;
                } else {
                    if (url != null) {  // target_alias exists
                        if ((linkTarget != null) && !attNames.contains("target")) {
                            attNames.add("target");
                            attValues.add(linkTarget);
                        }
                        attValues.set(href_idx, url);   // set transformed url
                        // Write transformed link:
                        html_out.append(html_in, copypos, tag_start);
                        html_out.append("<a");
                        for (int i=0; i < attNames.size(); i++) {
                            String attname = (String) attNames.get(i);
                            String attvalue = (String) attValues.get(i);
                            html_out.append(" ").append(attname).append("=\"").append(attvalue).append("\"");
                        }
                        html_out.append(">").append(link_text).append("</a>");
                        copypos = after_link;
                    }
                }
            }  // end of isImageLink or isFileLink
        }  // end of while loop
        if (copypos < html_in.length()) {
            html_out.append(html_in, copypos, html_in.length());
        }
    }

    private static String escapeTags(String str)
    {
        return str.replace("<", "&lt;").replace(">", "&gt;");
    }

    private DocmaPublication get_Publication(String pubId)
    {
        DocmaPublication pub = (DocmaPublication) publicationMap.get(pubId);
        if (pub == null) {
            pub = getPublicationFromGlobalExportQueue(pubId);
            if (pub == null) {
                pub = new DocmaPublication(docmaSess, archive, pubId);
            }
            publicationMap.put(pubId, pub);
        }
        return pub;
    }

    private void openPublicationArchive()
    {
        openPublicationArchive(docmaSess.getStoreId(), docmaSess.getVersionId());
    }

    private void openPublicationArchive(String storeId, DocVersionId verId)
    {
        boolean is_new = true;
        if (archive != null) {
            if (archive.getDocStoreId().equals(storeId) &&
                archive.getVersionId().equals(verId)) {
                // return; // do nothing, archive already open
                is_new = false; 
            }
        }
        PublicationArchive current_archive = allArchives.getArchive(storeId, verId);
        if (is_new || 
            (archive != current_archive)) // old instance has been invalidated
        {
            publicationMap.clear();
        }
        archive = current_archive;
        // Note: Always get the archive instance from allArchives, because
        // a new archive instance may be returned if stores/versions have
        // been renamed.
    }


    private String get_DefaultPublicationFilename(String pubConfigId, DocmaOutputConfig outConf)
    {
        // openPublicationArchive();
        String storeId = docmaSess.getStoreId();
        DocVersionId verId = docmaSess.getVersionId();

        String ver_state = docmaSess.getVersionState(storeId, verId);
        boolean is_draft = !DocmaConstants.VERSION_STATE_RELEASED.equals(ver_state);

        String format = outConf.getFormat();
        String subformat = outConf.getSubformat();
        boolean is_pdf = format.equalsIgnoreCase("pdf");
        boolean is_html = format.equalsIgnoreCase("html");
        boolean is_docbook = format.equalsIgnoreCase("docbook");
        boolean is_epub = is_html && subformat.startsWith("epub");

        String ext = null;
        if (is_pdf) ext = ".pdf";
        else if (is_epub) ext = ".epub";
        else if (is_html || is_docbook) ext = ".zip";
        else {
            throw new DocRuntimeException("Cannot export publication. Unknown format: " + format);
        }

        String transMode = docmaSess.getTranslationMode();
        String filename = pubConfigId + "_" + verId.toString().replace('.', '-');
        if (is_draft) {
            filename += "Draft";
        }
        if (transMode != null) {
            filename += "_" + transMode;
        }

        filename += "_" + outConf.getId() + ext;
        return filename;
    }

    private void addToGlobalExportQueue(DocmaPublication pub)
    {
        addToGlobalExportQueue(archive.getDocStoreId(), archive.getVersionId(), pub);
    }

    private static synchronized void addToGlobalExportQueue(String storeId,
                                                            DocVersionId verId,
                                                            DocmaPublication pub)
    {
        DocmaExportJob job = new DocmaExportJob(storeId, verId, pub);
        globalExportQueue.add(job);
    }

    private DocmaPublication getPublicationFromGlobalExportQueue(String publicationId)
    {
        DocmaExportJob job = getExportJob(archive.getDocStoreId(), archive.getVersionId(), publicationId);
        return (job == null) ? null : job.getPublication();
    }

    private void removeFromGlobalExportQueue(String publicationId)
    {
        removeFromGlobalExportQueue(archive.getDocStoreId(), archive.getVersionId(), publicationId);
    }

    private static synchronized void removeFromGlobalExportQueue(String storeId,
                                                                 DocVersionId verId,
                                                                 String publicationId)
    {
        Iterator it = globalExportQueue.iterator();
        while (it.hasNext()) {
            DocmaExportJob job = (DocmaExportJob) it.next();
            if (storeId.equals(job.getStoreId()) &&
                verId.equals(job.getVersionId()) &&
                publicationId.equals(job.getPublicationId())) {
                it.remove();
            }
        }
    }

}
