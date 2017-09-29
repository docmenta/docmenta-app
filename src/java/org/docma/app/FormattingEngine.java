/*
 * FormattingEngine.java
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

import org.docma.coreapi.DocException;
import org.docma.coreapi.ExportLog;
import org.docma.util.*;

import java.io.*;
import java.net.URI;
import java.util.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.sax.SAXResult;

import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FopFactoryBuilder;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.apps.FOUserAgent;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;

/**
 *
 * @author MP
 */
public class FormattingEngine
{
    /*
    static final String[][] CSS_PROPS = {
            { "font-family",         null },
            { "font-size",           null },
            { "font-stretch",        null },
            { "font-size-adjust",    null },
            { "font-style",          null },
            { "font-variant",        null },
            { "font-weight",         null },
            { "color",               null },
            { "background-color",    null },
            { "letter-spacing",      null },
            { "text-decoration",     null },
            { "text-shadow",         null },
            { "text-transform",      null },
            { "word-spacing",        null },
            { "text-indent",         null },
            { "line-height",         null },
            { "vertical-align",      null },
            { "text-align",          null },
            { "white-space",         null },
            { "margin",              null },
            { "margin-top",          null },
            { "margin-bottom",       null },
            { "margin-left",         null },
            { "margin-right",        null },
            { "padding",             null },
            { "padding-top",         null },
            { "padding-bottom",      null },
            { "padding-left",        null },
            { "padding-right",       null },
            { "padding-start",       null },
            { "padding-end",         null },
            { "border",              null },
            { "border-top",          null },
            { "border-bottom",       null },
            { "border-left",         null },
            { "border-right",        null },
            { "border-start",        null },
            { "border-end",          null },
            { "border-width",        null },
            { "border-top-width",    null },
            { "border-bottom-width", null },
            { "border-left-width",   "border-start-width" },
            { "border-right-width",  "border-end-width" },
            { "border-start-width",  null },
            { "border-end-width",    null },
            { "border-style",        null },
            { "border-top-style",    null },
            { "border-bottom-style", null },
            { "border-left-style",   "border-start-style" },
            { "border-right-style",  "border-end-style" },
            { "border-start-style",  null },
            { "border-end-style",    null },
            { "border-color",        null },
            { "border-top-color",    null },
            { "border-bottom-color", null },
            { "border-left-color",   "border-start-color" },
            { "border-right-color",  "border-end-color" },
            { "border-start-color",  null },
            { "border-end-color",    null }
    };
    */

    private static final Map map_CSS_FO = new HashMap(200);
    // private static final Map map_FO_MIRRORED = new HashMap();
    private static final String TEMPFILE_PREFIX = "temp";

    private final File configBaseDir;
    // private final File html2DocbookXSLFile;
    private final File tempDir;
    private int  tempDeleteCounter = 0;

    private final File   docbookXSLDir;
    private String fo_XSLFileURL;
    private String htmlSingle_XSLFileURL;
    private String htmlMultiple_XSLFileURL;
    private String webhelp_XSLFileURL;
    private String epub_XSLFileURL;

    private String customLayerIncludes = "";
    private String customLayerIncludes_PDF = "";
    private String customLayerIncludes_HTML = "";
    private String coverPage_XSL_FO = "";

    private final String html2DbXsl_PDF;
    private final String html2DbXsl_HTML;

    private final TransformerFactory tFactory;
    private final List<Transformer> html2DbTransformers_PDF = new ArrayList<Transformer>();
    private final List<Transformer> html2DbTransformers_HTML = new ArrayList<Transformer>();
    
    private WebFormatter webFormatter;

    private DocmaOutputConfig defaultPDFOutConfig = null;
    private DocmaOutputConfig defaultHTMLOutConfig = null;
    private DocmaOutputConfig defaultDocBookOutConfig = null;


    /* --------------  Static initialization  --------------- */
    
    static 
    {
        Iterator<CSSPropInfo> it = CSSPropInfo.allProps();
        while (it.hasNext()) {
            CSSPropInfo pinfo = it.next();
            String css_prop = pinfo.getCssName();
            String fo_prop = pinfo.getFoName();
            if (fo_prop == null) fo_prop = css_prop;
            map_CSS_FO.put(css_prop, fo_prop);
        }

        // Following code was used to allow the mirroring of style properties
        // in the header/footer table for even pages (double-sided output).
        // Code has been removed, because it could be too confusing and difficult
        // to handle all possible CSS notations (2 values, 3 values, 4 values). 
        // Now only text-align is mirrored. See method getFOFromCSS(..., mirrored).
        
        //        map_FO_MIRRORED.put("margin-left", "margin-right");
        //        map_FO_MIRRORED.put("margin-right", "margin-left");
        //        
        //        map_FO_MIRRORED.put("padding-left", "padding-right");
        //        map_FO_MIRRORED.put("padding-right", "padding-left");
        //        map_FO_MIRRORED.put("padding-start", "padding-end");
        //        map_FO_MIRRORED.put("padding-end", "padding-start");
        //        
        //        final String[] suff = new String[] { "", "-width", "-style", "-color" };
        //        for (int i=0; i < suff.length; i++) {
        //            final String nm = suff[i];
        //            map_FO_MIRRORED.put("border-left" + nm, "border-right" + nm);
        //            map_FO_MIRRORED.put("border-right" + nm, "border-left" + nm);
        //            map_FO_MIRRORED.put("border-start" + nm, "border-end" + nm);
        //            map_FO_MIRRORED.put("border-end" + nm, "border-start" + nm);
        //        }
    }
    
    /* --------------  Constructors  --------------- */

    public FormattingEngine(File configDir, File docbookXSLDir, File tempDir)
    throws Exception
    {
        this.configBaseDir = configDir;
        this.docbookXSLDir = docbookXSLDir;
        this.tempDir = tempDir;

        File html2DocbookFile = new File(configDir, "html2docbook.xsl");
        if (! html2DocbookFile.exists()) {
            throw new DocException("HTML to Docbook XSL file does not exist.");
        }
        String html2Db_all = getHtml2DbPlugs_all();
        String html2Db_print = getHtml2DbPlugs_print();
        String html2Db_PDF = html2Db_all + html2Db_print + getHtml2DbPlugs_PDF();
        String html2Db_HTML = html2Db_all + getHtml2DbPlugs_HTML();
        
        String html2dbXsl = DocmaUtil.readFileToString(html2DocbookFile);
        html2DbXsl_PDF  = html2dbXsl.replace("<!--plugins_xsl-->", html2Db_PDF);
        html2DbXsl_HTML = html2dbXsl.replace("<!--plugins_xsl-->", html2Db_HTML);
        
        customLayerIncludes = readCustomLayer();
        String customLayer_Print = readCustomLayer_Print();
        customLayerIncludes_PDF = customLayer_Print + readCustomLayer_PDF();
        customLayerIncludes_HTML = readCustomLayer_HTML();
        
        File coverpageXSLFile = new File(configDir, "coverpage_pdf.xsl");
        if (coverpageXSLFile.exists()) {
            coverPage_XSL_FO = DocmaUtil.readFileToString(coverpageXSLFile);
        }
        
        // initXalan();

        tFactory = TransformerFactory.newInstance();
        Log.info("Transformer factory class: " + tFactory.getClass().getName());

        File fo_xsl = new File(docbookXSLDir, "fo" + File.separator + "docbook.xsl");
        File htmlSingle_xsl = new File(docbookXSLDir, "html" + File.separator + "onechunk.xsl");
        File htmlMultiple_xsl = new File(docbookXSLDir, "html" + File.separator + "chunkfast.xsl");
        File webhelp_xsl = new File(docbookXSLDir,
                "webhelp" + File.separator + "xsl" + File.separator + "webhelp.xsl");
        File epub_xsl = new File(docbookXSLDir, "epub" + File.separator + "docbook.xsl");
        this.fo_XSLFileURL = fo_xsl.toURI().toURL().toString();
        // foXSLFileURL = "file:///" + fo_xsl.getAbsolutePath().replace('\\', '/');
        this.htmlSingle_XSLFileURL = htmlSingle_xsl.toURI().toURL().toString();
        this.htmlMultiple_XSLFileURL = htmlMultiple_xsl.toURI().toURL().toString();
        this.webhelp_XSLFileURL = webhelp_xsl.toURI().toURL().toString();
        this.epub_XSLFileURL = epub_xsl.toURI().toURL().toString();
        Log.info("FO XSL Path: " + this.fo_XSLFileURL);
        Log.info("HTML single file XSL Path: " + this.htmlSingle_XSLFileURL);
        Log.info("HTML multiple files XSL Path: " + this.htmlMultiple_XSLFileURL);
        Log.info("WebHelp XSL Path: " + this.webhelp_XSLFileURL);
        Log.info("EPUB XSL Path: " + this.epub_XSLFileURL);
        
        this.webFormatter = new WebFormatter(configDir, docbookXSLDir, tempDir);
    }

    private String getHtml2DbPlugs_all() throws IOException
    {
        File[] arr = configBaseDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) 
            {
                return name.startsWith("html2db-") && name.endsWith(".xsl");
            }
        });
        Arrays.sort(arr);
        return DocmaUtil.readFilesToString(arr, null);
    }

    private String getHtml2DbPlugs_print() throws IOException
    {
        File[] arr = configBaseDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) 
            {
                return name.startsWith("html2db_print-") && name.endsWith(".xsl");
            }
        });
        Arrays.sort(arr);
        return DocmaUtil.readFilesToString(arr, null);
    }
    
    private String getHtml2DbPlugs_PDF() throws IOException
    {
        File[] arr = configBaseDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) 
            {
                return name.startsWith("html2db_pdf-") && name.endsWith(".xsl");
            }
        });
        Arrays.sort(arr);
        return DocmaUtil.readFilesToString(arr, null);
    }
    
    private String getHtml2DbPlugs_HTML() throws IOException
    {
        File[] arr = configBaseDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) 
            {
                return name.startsWith("html2db_html-") && name.endsWith(".xsl");
            }
        });
        Arrays.sort(arr);
        return DocmaUtil.readFilesToString(arr, null);
    }
    
    private String readCustomLayer() throws IOException
    {
        File f = new File(configBaseDir, "customlayer.xsl");
        String clay = f.exists() ? DocmaUtil.readFileToString(f) : "";
        File[] arr = configBaseDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) 
            {
                return name.startsWith("custom-") && name.endsWith(".xsl");
            }
        });
        if ((arr != null) && (arr.length > 0)) {
            Arrays.sort(arr);
            clay += DocmaUtil.readFilesToString(arr, null);
        }
        return clay;
    }
    
    private String readCustomLayer_Print() throws IOException
    {
        File[] arr = configBaseDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) 
            {
                return (name.startsWith("custom_print-") && name.endsWith(".xsl"));
            }
        });
        Arrays.sort(arr);
        return DocmaUtil.readFilesToString(arr, null);
    }
    
    private String readCustomLayer_PDF() throws IOException
    {
        File f = new File(configBaseDir, "customlayer_pdf.xsl");
        String clay = f.exists() ? DocmaUtil.readFileToString(f) : "";
        File[] arr = configBaseDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) 
            {
                return name.startsWith("custom_pdf-") && name.endsWith(".xsl");
            }
        });
        if ((arr != null) && (arr.length > 0)) {
            Arrays.sort(arr);
            clay += DocmaUtil.readFilesToString(arr, null);
        }
        return clay;
    }
    
    private String readCustomLayer_HTML() throws IOException
    {
        File f = new File(configBaseDir, "customlayer_html.xsl");
        String clay = f.exists() ? DocmaUtil.readFileToString(f) : "";
        File[] arr = configBaseDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) 
            {
                return name.startsWith("custom_html-") && name.endsWith(".xsl");
            }
        });
        if ((arr != null) && (arr.length > 0)) {
            Arrays.sort(arr);
            clay += DocmaUtil.readFilesToString(arr, null);
        }
        return clay;
    }
    
    /* --------------  Public methods  --------------- */

    public DocmaPublicationConfig getDefaultPublicationConfig()
    {
        DocmaPublicationConfig pub_config = new DocmaPublicationConfig("_default_publication_");
        // pub_config.setTitle("Preview");
        return pub_config;
    }

    public DocmaOutputConfig getDefaultPDFOutputConfig()
    {
        if (defaultPDFOutConfig == null) {
            defaultPDFOutConfig = new DocmaOutputConfig("_default_pdf_output_");
            defaultPDFOutConfig.setFormat("pdf");
        }
        return defaultPDFOutConfig;
    }

    public DocmaOutputConfig getDefaultHTMLOutputConfig()
    {
        if (defaultHTMLOutConfig == null) {
            defaultHTMLOutConfig = new DocmaOutputConfig("_default_html_output_");
            defaultHTMLOutConfig.setFormat("html");
        }
        return defaultHTMLOutConfig;
    }

    public DocmaOutputConfig getDefaultDocBookOutputConfig()
    {
        if (defaultDocBookOutConfig == null) {
            defaultDocBookOutConfig = new DocmaOutputConfig("_default_docbook_output_");
            defaultDocBookOutConfig.setFormat("docbook");
        }
        return defaultDocBookOutConfig;
    }

    public void formatHTML2DocBook(StreamSource htmlPrintInstance,
                                   OutputStream output,
                                   DocmaPublicationConfig pub_config,
                                   DocmaOutputConfig out_config,
                                   ExportLog export_log)
    throws Exception
    {
        if (pub_config == null) {
            pub_config = getDefaultPublicationConfig();
        }
        if (out_config == null) {
            out_config = getDefaultDocBookOutputConfig();
        }
        if (DocmaConstants.DEBUG) {
            Log.info("Formatting HTML to DocBook...");
            Log.info("Publication Id: " + pub_config.getId());
            Log.info("Output Id: " + out_config.getId());
        }
        if (! out_config.getFormat().equalsIgnoreCase("docbook")) {
            throw new DocException("Invalid output configuration: wrong format.");
        }
        export_log.infoMsg("Starting transformation from HTML to DocBook.");
        StreamResult docbookresult = new StreamResult(output);
        // try {
        html2docbook(htmlPrintInstance, out_config, docbookresult);
        // } catch (Exception ex) {
        //     ex.printStackTrace();
        //     export_log.errorMsg("Exception: " + ex.getMessage());
        // }
        export_log.infoMsg("Transformation from HTML to DocBook finished.");
    }

    public File formatHTML2HTML(String html_in,
                                File outputDir,
                                String base_URL,
                                DocmaPublicationConfig pub_config,
                                DocmaOutputConfig out_config,
                                DocmaStyle[] styles,
                                ExportLog export_log)
    throws Exception
    {
        if (pub_config == null) {
            pub_config = getDefaultPublicationConfig();
        }
        if (out_config == null) {
            out_config = getDefaultHTMLOutputConfig();
        }
        String subformat = out_config.getSubformat();

        if (DocmaConstants.DEBUG) {
            Log.info("Formatting HTML to HTML...");
            Log.info("Subformat: " + subformat);
            Log.info("Publication Id: " + pub_config.getId());
            Log.info("Output Id: " + out_config.getId());
        }
        if (! out_config.getFormat().equalsIgnoreCase("html")) {
            throw new DocException("Invalid output configuration: wrong format.");
        }
        boolean is_webhelp = subformat.startsWith("webhelp");
        boolean is_webhelp_1 = subformat.equals("webhelp1");
        boolean is_webhelp_new = is_webhelp && !is_webhelp_1;
        boolean is_epub = subformat.startsWith("epub");
        
        String format_str = (subformat.length() == 0) ? "HTML (static)" : subformat.toUpperCase();
        export_log.infoMsg("Starting transformation to " + format_str + ".");
        
        if (outputDir == null) {
            outputDir = createTempDir();
        }
        
        if (is_webhelp_new) {
            // Create the WebHelp2 output:
            webFormatter.format(html_in, outputDir, pub_config, out_config, export_log);
            // Create the index files for fulltext search:
            File workDir = createTempDir();
            try {
                WebHelpBuildUtil.buildWebHelp2(workDir, outputDir, docbookXSLDir, out_config, export_log);
            } finally {
                if (! DocmaConstants.DEBUG) DocmaUtil.recursiveFileDelete(workDir);
            }
        } else {
            StreamSource htmlPrintInstance = new StreamSource(new StringReader(html_in));
            File docbookfile = html2docbook(htmlPrintInstance, out_config);

            if (is_webhelp) {
                docbook2webhelp(docbookfile, outputDir, base_URL, pub_config, out_config, styles, export_log);
            } else {
                if (is_epub) {
                    // Move docbookfile to outputDir because the epub transformation
                    // script writes output in same directory as the input file:
                    File f = new File(outputDir, docbookfile.getName());
                    if (docbookfile.renameTo(f)) docbookfile = f;
                    else export_log.warningMsg("Could not move DocBook file to output folder: " + outputDir.getPath());
                }
                StreamSource src = new StreamSource(docbookfile);
                // src.setSystemId(htmlPrintInstance.getSystemId());  // set base path for relative image URIs
                docbook2html(src, outputDir, base_URL, pub_config, out_config, styles, export_log);
            }
            if (! DocmaConstants.DEBUG) docbookfile.delete();
        }
        export_log.infoMsg("Transformation to " + format_str + " finished.");
        return outputDir;
    }

    public void formatHTML2PDF(StreamSource htmlPrintInstance,
                               OutputStream output,
                               String base_URL,
                               DocmaPublicationConfig pub_config,
                               DocmaOutputConfig out_config,
                               DocmaStyle[] styles,
                               ExportLog export_log)
    throws Exception
    {
        if (pub_config == null) {
            pub_config = getDefaultPublicationConfig();
        }
        if (out_config == null) {
            out_config = getDefaultPDFOutputConfig();
        }
        if (DocmaConstants.DEBUG) {
            Log.info("Formatting HTML to PDF...");
            Log.info("Publication Id: " + pub_config.getId());
            Log.info("Output Id: " + out_config.getId());
        }
        if (! out_config.getFormat().equalsIgnoreCase("pdf")) {
            throw new DocException("Invalid output configuration: wrong format.");
        }
        export_log.infoMsg("Starting transformation from HTML to DocBook.");
        File docbookfile = html2docbook(htmlPrintInstance, out_config);
        export_log.infoMsg("Transformation from HTML to DocBook finished.");
        StreamSource src = new StreamSource(docbookfile);
        // src.setSystemId(htmlPrintInstance.getSystemId());  // set base path for relative image URIs
        export_log.infoMsg("Starting transformation from DocBook to PDF.");
        docbook2pdf(src, output, base_URL, pub_config, out_config, styles, export_log);
        export_log.infoMsg("Transformation from DocBook to PDF finished.");
        if (! DocmaConstants.DEBUG) docbookfile.delete();
    }

    /* --------------  Private methods  --------------- */

    private static void initXalan()
    {
        System.setProperty("javax.xml.transform.TransformerFactory",
                           "org.apache.xalan.processor.TransformerFactoryImpl");
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
                           "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
        System.setProperty("javax.xml.parsers.SAXParserFactory",
                           "org.apache.xerces.jaxp.SAXParserFactoryImpl");

    }

    private File html2docbook(StreamSource htmlPrintInstance, DocmaOutputConfig outConfig) throws Exception
    {
        File docbookfile = createTempFile("xml");
        FileOutputStream docbookout = new FileOutputStream(docbookfile);
        StreamResult docbookresult = new StreamResult(docbookout);
        html2docbook(htmlPrintInstance, outConfig, docbookresult);
        docbookout.close();
        return docbookfile;
    }

    private void html2docbook(StreamSource htmlPrintInstance,
                              DocmaOutputConfig outConfig,
                              StreamResult output) throws Exception
    {
        String out_format = outConfig.getFormat();
        if (DocmaConstants.DEBUG) {
            System.out.println("Output configuration format: " + out_format);
        }
        boolean is_pdf = outConfig.getFormat().equalsIgnoreCase("pdf");
        String out_type = is_pdf ? "print" : "interactive";
        boolean fit_images = out_type.equals("print") && outConfig.isPdfFitImages();
        
        Transformer htmlTransformer = acquireHtml2DocbookTransformer(is_pdf);
        try {
            htmlTransformer.setParameter("docma_output_type", out_type);
            htmlTransformer.setParameter("docma_fit_images", fit_images ? "true" : "false");
            htmlTransformer.transform(htmlPrintInstance, output);
        } finally {
            releaseHtml2DocbookTransformer(is_pdf, htmlTransformer);
        }
    }
    
    private synchronized Transformer acquireHtml2DocbookTransformer(boolean is_pdf) throws Exception
    {
        Transformer transfo;
        List<Transformer> list = is_pdf ? html2DbTransformers_PDF : html2DbTransformers_HTML;
        String xsl = is_pdf ? html2DbXsl_PDF : html2DbXsl_HTML;
        if (list.isEmpty()) {
            StreamSource streamSrc = new StreamSource(new StringReader(xsl));
            transfo = tFactory.newTransformer(streamSrc);
        } else {
            transfo = list.remove(list.size() - 1);
        }
        return transfo;
    }
    
    private synchronized void releaseHtml2DocbookTransformer(boolean is_pdf, Transformer transformer)
    {
        List<Transformer> list = is_pdf ? html2DbTransformers_PDF : html2DbTransformers_HTML;
        list.add(transformer);
    }

    private void docbook2html(StreamSource src,
                              File outDir,
                              String base_URL,
                              DocmaPublicationConfig pub_config,
                              DocmaOutputConfig out_config,
                              DocmaStyle[] styles,
                              ExportLog export_log)
    throws Exception
    {
        File gentext_file = createDocBookXSLGentextFile(out_config);
        String custom_layer = createDocBookXSLCustomLayer(pub_config, out_config, styles, outDir, base_URL, gentext_file);

        StreamSource docbook_xslt = new StreamSource(new StringReader(custom_layer));
        Transformer docbook2HTMLTransformer = tFactory.newTransformer(docbook_xslt);

        // Set the value of a <param> in the stylesheet
        // transformer.setParameter("versionParam", "2.0");

        // FileOutputStream htmlout = new FileOutputStream(new File(outDir, "mein_index.html"));
        try {
            StringWriter dummy = new StringWriter();
            StreamResult res = new StreamResult(dummy);
            // Start XSLT transformation
            docbook2HTMLTransformer.transform(src, res);
        } finally {
            //     htmlout.close();
            if (! DocmaConstants.DEBUG) {
                if (gentext_file != null) gentext_file.delete();
            }
        }
    }


    private void docbook2webhelp(File docbookfile,
                                 File outDir,
                                 String base_URL,
                                 DocmaPublicationConfig pub_config,
                                 DocmaOutputConfig out_config,
                                 DocmaStyle[] styles,
                                 ExportLog export_log)
    throws Exception
    {
        File gentext_file = createDocBookXSLGentextFile(out_config);
        String custom_layer = createDocBookXSLCustomLayer(pub_config, out_config, styles, outDir, base_URL, gentext_file);

        File inputDir = createTempDir();
        File customlayer_file = new File(inputDir, "custom_layer_webhelp.xsl");
        DocmaUtil.writeStringToFile(custom_layer, customlayer_file, "UTF-8");

        try {
            WebHelpBuildUtil.build(docbookfile, inputDir, outDir, docbookXSLDir, out_config, export_log);
        } finally {
            if (! DocmaConstants.DEBUG) DocmaUtil.recursiveFileDelete(inputDir);
        }
    }


    private void docbook2pdf(StreamSource src, 
                             OutputStream out,
                             String base_URL,
                             DocmaPublicationConfig pub_config,
                             DocmaOutputConfig out_config,
                             DocmaStyle[] styles,
                             ExportLog export_log)
    throws Exception
    {
        File gentext_file = createDocBookXSLGentextFile(out_config);
        String custom_layer = createDocBookXSLCustomLayer(pub_config, out_config, styles, null, base_URL, gentext_file);

        //
        // Configure fopFactory as desired
        //

        // FOP 1.1 
        // FopFactory fopFactory = FopFactory.newInstance();
        
        // FOP 2.1
        FopFactoryBuilder fopBuilder = new FopFactoryBuilder(new URI(base_URL));
        
        String fop_conf_path = out_config.getPdfCustomFOPConfigPath();
        if ((fop_conf_path != null) && !fop_conf_path.trim().equals("")) {
            File fop_conf_file = new File(fop_conf_path);
            if (fop_conf_file.exists()) {
                export_log.infoMsg("Setting custom FOP configuration: " + fop_conf_file.getAbsolutePath());
                
                // FOP 1.1
                // fopFactory.setUserConfig(fop_conf_file);
                
                // FOP 2.1
                DefaultConfigurationBuilder cfgBuilder = new DefaultConfigurationBuilder();
                Configuration cfg = cfgBuilder.buildFromFile(fop_conf_file);
                fopBuilder.setConfiguration(cfg);
            } else {
                export_log.warningMsg("FOP configuration file not found: " + fop_conf_file.getAbsolutePath());
            }
        }
        
        // fopFactory.setStrictValidation(false);
        int source_dpi = out_config.getPdfSourceResolution();
        if (source_dpi <= 0) source_dpi = 96;  // default is 96dpi (dots/pixels per Inch)
        
        // FOP 1.1
        // fopFactory.setSourceResolution(source_dpi);

        // FOP 2.1
        fopBuilder.setSourceResolution(source_dpi);
        FopFactory fopFactory = fopBuilder.build();

        FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
        // configure foUserAgent as desired
        foUserAgent.getEventBroadcaster().addEventListener(new FOPEventListener(export_log));

        // FOP 1.1: foUserAgent.setBaseURL(base_URL);

        foUserAgent.setProducer("Docmenta");
        // foUserAgent.setCreator("John Doe");
        // foUserAgent.setAuthor("John Doe");
        foUserAgent.setCreationDate(new java.util.Date());
        foUserAgent.setTitle(pub_config.getTitle());
        // foUserAgent.setKeywords("XML XSL-FO");
        int target_dpi = out_config.getPdfTargetResolution();
        if (target_dpi <= 0) target_dpi = 300;  // default is 300dpi (dots/pixels per Inch)
        foUserAgent.setTargetResolution(target_dpi);

        try {
            // Construct fop with desired output format
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, out);

            StreamSource docbook_xslt = new StreamSource(new StringReader(custom_layer));
            Transformer docbook2PDFTransformer = tFactory.newTransformer(docbook_xslt);

            // Set the value of a <param> in the stylesheet
            // transformer.setParameter("versionParam", "2.0");

            // Resulting SAX events (the generated FO) must be piped through to FOP
            Result res = new SAXResult(fop.getDefaultHandler());

            // Start XSLT transformation and FOP processing
            docbook2PDFTransformer.transform(src, res);
        } finally {
            out.close();
            if (! DocmaConstants.DEBUG) {
                if (gentext_file != null) gentext_file.delete();
            }
        }
    }

    private File createDocBookXSLGentextFile(DocmaOutputConfig out_config) throws Exception
    {
        // try {
            String lang = out_config.getLanguageCode();
            Properties props = out_config.getGentextProps();
            if ((props == null) || props.isEmpty()) return null;

            String gentext_xsl = DocBookXSLUtil.createCustomGeneratedTextI18nXML(props, lang);
            File f = createTempFile("xsl");
            OutputStream fout = new FileOutputStream(f);
            Writer wout = new OutputStreamWriter(fout, "UTF-8");
            wout.write(gentext_xsl);
            wout.close();
            fout.close();
            return f;
        // } catch (Exception ex) {}
    }

    private String createDocBookXSLCustomLayer(DocmaPublicationConfig pub_config,
                                               DocmaOutputConfig out_config,
                                               DocmaStyle[] styles,
                                               File outputDir,
                                               String image_base_URL,
                                               File gentext)
    {
        // Note: styles must already be filtered by variant given in out_config!

        boolean is_html = out_config.getFormat().equalsIgnoreCase("html");
        boolean is_pdf = out_config.getFormat().equalsIgnoreCase("pdf");
        String subformat = out_config.getSubformat();
        boolean is_webhelp = false;
        boolean is_epub = false;

        String xsl_file = null;
        if (is_pdf) {
            xsl_file = fo_XSLFileURL;
        } else
        if (is_html) {
            is_webhelp = subformat.startsWith("webhelp");
            is_epub = subformat.startsWith("epub");
            if (is_webhelp) {
                xsl_file = webhelp_XSLFileURL;
            } else
            if (is_epub) {
                xsl_file = epub_XSLFileURL;
            } else
            if (out_config.isHtmlSingleFile()) {
                xsl_file = htmlSingle_XSLFileURL;
            } else {
                xsl_file = htmlMultiple_XSLFileURL;
            }
        }

        StringBuilder buf = new StringBuilder(64*1024);
        buf.append("<?xml version=\"1.0\" ?>\n");
        buf.append("<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" ");
        if (is_pdf) {
            buf.append("xmlns:fo=\"http://www.w3.org/1999/XSL/Format\" ");
        }
        if (out_config.isPdfCustomHeaderFooter()) {  // extension needed for timestamp in header/footer
            buf.append(" xmlns:date=\"http://exslt.org/dates-and-times\" exclude-result-prefixes=\"date\" ");
        }
        buf.append(" version=\"1.0\">\n");
        buf.append("<xsl:import href=\"" + xsl_file + "\" />\n");

        buf.append(replaceStylePlaceholders(customLayerIncludes, styles));

        // Set gentext template
        if (gentext != null) {
            try {
                String gentext_url = gentext.toURI().toURL().toString();
                buf.append("<xsl:param name=\"local.l10n.xml\" select=\"document('")
                   .append(gentext_url).append("')\" />");
            } catch (Exception ex) {
                Log.warning("Could not create gentext file URL: " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        // Set image base path
        addParam(buf, "img.src.path", is_pdf ? image_base_URL : "");

        // Publication options

        boolean is_draft = pub_config.isDraft();
        addParam(buf, "draft.mode", is_draft ? "yes" : "no");
        addParam(buf, "draft.watermark.image", is_draft ? out_config.getDraftImagePath() : "");

        // General output options

        if (out_config.isToc()) {
            String gentoc = "book toc,title";
            if (out_config.isPartToc()) gentoc += " part toc,title";
            if (out_config.isChapterToc()) gentoc += " chapter toc,title appendix toc,title preface toc,title";
            if (out_config.isSectionToc()) gentoc += " section toc";
            addParam(buf, "generate.toc", gentoc);
            int max_toc_depth = out_config.getMaxTocDepth();
            int sect_depth = out_config.getSectionTocDepth();
            boolean is_component_toc = out_config.isPartToc() || 
                                       out_config.isChapterToc() || 
                                       out_config.isSectionToc();
            if ((sect_depth <= 0) || !is_component_toc) {
                sect_depth = max_toc_depth;  // no restriction by toc.section.depth
            }
            addParam(buf, "toc.max.depth", "" + max_toc_depth);
            addParam(buf, "toc.section.depth", "" + sect_depth);
            addParam(buf, "generate.section.toc.level", "" + out_config.getSectionTocLevels());
            String toc_indent = out_config.getTocIndentWidth();
            if (toc_indent != null) {
                toc_indent = toc_indent.trim().toLowerCase();
                if (toc_indent.endsWith("pt")) {
                    toc_indent = toc_indent.substring(0, toc_indent.length() - 2);
                }
                if (toc_indent.length() > 0) {
                    addParam(buf, "toc.indent.width", toc_indent);
                }
            }
        } else {
            addParam(buf, "generate.toc", "book nop");
        }
        String titleP = out_config.getTitlePlacement();
        addParam(buf, "formal.title.placement",
                      "\nfigure " + titleP +
                      "\nexample " + titleP +
                      "\nequation " + titleP +
                      "\ntable " + titleP +
                      "\nprocedure " + titleP + "\n");

        addParam(buf, "appendix.autolabel", out_config.getAppendixNumbering());
        addParam(buf, "chapter.autolabel", out_config.getChapterNumbering());
        addParam(buf, "part.autolabel", out_config.getPartNumbering());
        addParam(buf, "section.autolabel", out_config.getSectionNumbering());
        addParam(buf, "section.autolabel.max.depth", "" + out_config.getNumberingDepth());
        if (! out_config.isExclude1stLevelNumber()) {
            addParam(buf, "section.label.includes.component.label", "1");
        }
        if (out_config.isRestartPart()) {
            addParam(buf, "label.from.part", "1");
            addParam(buf, "component.label.includes.part.label", "1");
        }
        String footnote_num = out_config.getFootnoteNumbering();
        if (footnote_num.equals("")) footnote_num = "1";  // default
        addParam(buf, "footnote.number.format", footnote_num);

        // Add cover page templates
        String cover_img_url = out_config.getCoverImagePath();
        boolean has_cover_image = (cover_img_url != null) && (cover_img_url.length() > 0);
        StringBuilder cover_fo = null;
        if (is_pdf && has_cover_image) {
            // This adds the page master for the coverpage and depending on the  
            // output settings adds an extra cover page and an extra blank page.
            String cover_mode = out_config.getPdfCoverMode();
            boolean extrapage = ! cover_mode.equals("titlepage");  // "extrapage" or "extrapage_blank"
            boolean blank = cover_mode.equals("extrapage_blank");
            String cover_css = getStyle_CSS(styles, "coverpage", out_config, "");
            cover_fo = new StringBuilder();
            getFOFromCSS(cover_fo, cover_css, true);
            String background_col = extractXslAttribute(cover_fo, "background-color");
            String cover_x = out_config.getPdfCoverHorizontalPosition();
            String cover_y = out_config.getPdfCoverVerticalPosition();
            final String blank_fo = "<fo:block><xsl:text>&#160;</xsl:text></fo:block>";
            String cover_xsl = coverPage_XSL_FO.replace("###extra_coverpage###", extrapage ? "1" : "0")
                                               .replace("###coverimage_url###", cover_img_url)
                                               .replace("###cover_pos_x###", cover_x.equals("") ? "center" : cover_x)
                                               .replace("###cover_pos_y###", cover_y.equals("") ? "center" : cover_y)
                                               .replace("###coverpage_attributes###", background_col)
                                               .replace("###cover_pagebreak###", blank ? "break-after=\"'page'\"" : "")
                                               .replace("###coverpage_blank###", blank ? blank_fo : "");
            buf.append(cover_xsl);
        }
            
        // Set custom title page
        String titlepage_alias = pub_config.getCustomTitlePage1().trim();
        boolean pdf_image_on_titlepage = is_pdf && out_config.getPdfCoverMode().equals("titlepage") && has_cover_image;
        if (pdf_image_on_titlepage) {
            // Select page-master "coverpage" for titlepage to avoid borders around the cover image
            writeSelectUserPageMasterTitlepageCover(buf);
        }
        if (pdf_image_on_titlepage && titlepage_alias.equals("")) {  
            // Create default title page with background image
            addDefaultTitlePageFO(buf, cover_fo);
        }
        if (! titlepage_alias.equals("")) {  // custom title page (front)
            addCustomTitlePage(buf, "recto", is_pdf, cover_fo);
        }
        if (! pub_config.getCustomTitlePage2().trim().equals("")) {  // custom title page (back)
            addCustomTitlePage(buf, "verso", is_pdf, null);
        }

        // HTML options

        if (is_html) {
            String is_epub_str = is_epub ? "1" : "0";
            String crumb_sep = out_config.getHtmlBreadcrumbSeparator();
            if ((crumb_sep == null) || crumb_sep.equals("")) {
                crumb_sep = "&gt;";
            }
            String custIncHTML = customLayerIncludes_HTML.replace("###is_epub###", is_epub_str) 
                                                         .replace("###bread_separator###", crumb_sep);
            custIncHTML = replaceStylePlaceholders(custIncHTML, styles);
            buf.append(custIncHTML);

            String out_dir = outputDir.getAbsolutePath();
            if (! out_dir.endsWith(File.separator)) {
                out_dir += File.separator;
            }
            // String prefix = out_config.getHtmlFilePrefix().trim();
            // if ((prefix.indexOf("/") >= 0) || (prefix.indexOf("\\") >= 0) || (prefix.indexOf("..") >= 0)) {
            //     throw new DocRuntimeException("Invalid HTML file prefix!");
            // }
            // out_dir += prefix;
            if (! (is_webhelp || is_epub)) {  // setting base.dir is not allowed for Web Help
                addParam(buf, "base.dir", out_dir);
            }
            addParam(buf, "chunk.quietly", "1");
            // addParam(buf, "root.filename", "start");  // "index" by default
            String out_encoding = out_config.getHtmlOutputEncoding();
            if ((out_encoding != null) && (out_encoding.length() > 0)) {
                addParam(buf, "chunker.output.encoding", out_encoding);
            }
            if (is_webhelp || !out_config.isHtmlSingleFile()) {  // if chunk into multiple files
                addParam(buf, "use.id.as.filename", out_config.isHtmlAliasFilename() ? "1" : "0");
                addParam(buf, "chunk.tocs.and.lots", out_config.isHtmlSeparateTOC() ? "1" : "0");
                addParam(buf, "chunk.separate.lots", out_config.isHtmlSeparateEachTable() ? "1" : "0");
                addParam(buf, "chunk.section.depth", "" + out_config.getHtmlSeparateFileLevel());
                addParam(buf, "chunk.first.sections", out_config.isHtmlInclude1stSection() ? "0" : "1");
            }
            boolean is_nav_icons = out_config.isHtmlNavigationalIcons();
            addParam(buf, "navig.graphics", is_nav_icons ? "1" : "0");
            if (is_nav_icons) {
                addParam(buf, "navig.graphics.path", out_config.getHtmlNavigationIconsPath());
                addParam(buf, "navig.graphics.extension", out_config.getHtmlNavigationIconsExt());
            }
            addParam(buf, "navig.showtitles", out_config.isHtmlNavigationalTitles() ? "1" : "0");
            boolean is_breadcrumbs = out_config.isHtmlBreadcrumbs() && !out_config.isHtmlSingleFile();
            addParam(buf, "docma.breadcrumbs", is_breadcrumbs ? "true" : "false");
            addParam(buf, "html.stylesheet", DocmaConstants.HTML_CSS_FILENAME);
            addParam(buf, "para.propagates.style", "1");   // propagate role attribute as css class
            addParam(buf, "entry.propagates.style", "1");   // propagate role attribute as css class
            addParam(buf, "emphasis.propagates.style", "1");  // propagate role attribute as css class
            addParam(buf, "phrase.propagates.style", "1");  // propagate role attribute as css class
            String targetwin = out_config.getHtmlURLTargetWindow();
            if (targetwin.equals("")) targetwin = DocmaConstants.DEFAULT_URL_TARGET_WINDOW;
            addParam(buf, "ulink.target", targetwin);
            // addParam(buf, "", "");

            buf.append("<xsl:template name=\"system.head.content\">");
            String head_tags = out_config.getHtmlCustomHeadTags();
            if (head_tags != null) {  // custom head tags are defined
                buf.append(head_tags);
            } else {
                // Set default meta tag for HTML output if no custom tags are defined
                if (! is_epub) {
                    buf.append("<xsl:element name=\"meta\">")
                       .append("  <xsl:attribute name=\"http-equiv\">X-UA-Compatible</xsl:attribute>")
                       .append("  <xsl:attribute name=\"content\">IE=8</xsl:attribute>")
                       .append("</xsl:element>");
                }
            }
            buf.append("</xsl:template>\n");
            if (! is_webhelp) {
                String head_content = out_config.getHtmlCustomHeaderContent();
                if ((head_content != null) && (head_content.length() > 0)) {
                    buf.append("<xsl:template name=\"user.header.navigation\">")
                       .append(head_content).append("</xsl:template>\n");
                }

                String js_fn = out_config.getHtmlCustomJSFilename();
                if ((js_fn != null) && (js_fn.length() > 0)) {
                    buf.append("<xsl:template name=\"user.head.content\"><xsl:element name=\"script\"><xsl:attribute name=\"src\">")
                       .append(js_fn)
                       .append("</xsl:attribute><xsl:attribute name=\"type\">text/javascript</xsl:attribute></xsl:element></xsl:template>\n");
                }
            }
            String foot_content = out_config.getHtmlCustomFooterContent();
            if ((foot_content != null) && (foot_content.length() > 0)) {
                buf.append("<xsl:template name=\"user.footer.navigation\">")
                   .append(foot_content).append("</xsl:template>\n");
            }
        } else

        // PDF options

        if (is_pdf) {
            String papertype = out_config.getPdfPaperSize();
            if ((papertype == null) || papertype.equals("") || papertype.equals("custom")) {
                addDimParam(buf, "page.width.portrait", out_config.getPdfPageWidth());
                addDimParam(buf, "page.height.portrait", out_config.getPdfPageHeight());
            } else {
                addParam(buf, "paper.type", papertype);
            }
            if (out_config.isPdfDoubleSided()) {
                addParam(buf, "double.sided", "1");
            }
            if (out_config.isPdfBookmarks() || out_config.isPdfDoubleSided()) {
                // addParam(buf, "fop.extensions", "1");
                addParam(buf, "fop1.extensions", "1");
            }
            if (out_config.isPdfNumbersInRefs()) {
                addParam(buf, "insert.xref.page.number", "yes");
            }
            String showExtHRef = out_config.getPdfShowExternalHRef();
            boolean is_showExtHRef = !(showExtHRef.trim().equals("") || showExtHRef.equalsIgnoreCase("no"));
            addParam(buf, "ulink.show", is_showExtHRef ? "1" : "0");
            addParam(buf, "ulink.footnotes", showExtHRef.equalsIgnoreCase("footnote") ? "1" : "0");
            addParam(buf, "page.orientation", out_config.getPdfPageOrientation());
            addDimParam(buf, "page.margin.inner", out_config.getPdfPageInner());
            addDimParam(buf, "page.margin.outer", out_config.getPdfPageOuter());
            addDimParam(buf, "page.margin.top", out_config.getPdfPageTop());
            addDimParam(buf, "page.margin.bottom", out_config.getPdfPageBottom());
            addDimParam(buf, "body.margin.top", out_config.getPdfBodyTop());
            addDimParam(buf, "body.margin.bottom", out_config.getPdfBodyBottom());
            addDimParam(buf, "body.start.indent", out_config.getPdfBodyStart());
            addDimParam(buf, "body.end.indent", out_config.getPdfBodyEnd());
            addDimParam(buf, "region.before.extent", out_config.getPdfHeaderHeight());
            addDimParam(buf, "region.after.extent", out_config.getPdfFooterHeight());
            addParam(buf, "column.count.index", "" + out_config.getPdfIndexColumnCount());
            addDimParam(buf, "column.gap.index", out_config.getPdfIndexColumnGap());

            // Set default style
            DocmaStyle def_style = getStyle(styles, "default", out_config);
            addAttributeSet(buf, "root.properties", def_style);

            // Create para spacing XSL string
            String p_space = out_config.getParaSpace();
            if ((p_space == null) || p_space.equals("")) {
                p_space = "0";
            }
            StringBuilder para_spacing = new StringBuilder();
            addAttribute(para_spacing, "space-before.minimum", p_space);
            addAttribute(para_spacing, "space-before.optimum", p_space);
            addAttribute(para_spacing, "space-before.maximum", p_space);
            addAttribute(para_spacing, "space-after.minimum", "0");
            addAttribute(para_spacing, "space-after.optimum", "0");
            addAttribute(para_spacing, "space-after.maximum", "0");

            // Set spacing of paragraphs
            startAttributeSet(buf, "normal.para.spacing");
            buf.append(para_spacing);
            endAttributeSet(buf);
            // Set spacing of objects with title, like figures, tables,...
            startAttributeSet(buf, "formal.object.properties");
            buf.append(para_spacing);
            addAttribute(buf, "keep-together.within-column", "always");
            endAttributeSet(buf);
            // Set spacing of objects without title, like informal figures, tables,...
            startAttributeSet(buf, "informal.object.properties");
            buf.append(para_spacing);
            endAttributeSet(buf);

            // Create paragraph and table indent XSL string
            String p_indent = out_config.getParaIndent();
            if ((p_indent == null) || p_indent.equals("")) {
                p_indent = DocmaConstants.DEFAULT_PARA_INDENT;
            }
            StringBuilder pindent_xsl = new StringBuilder("<xsl:choose>");
            StringBuilder tindent_xsl = new StringBuilder("<xsl:choose>");
            String unit = CSSUtil.getSizeUnit(p_indent);
            if (p_indent.contains(".")) {
                float indent_step = CSSUtil.getSizeFloat(p_indent);
                float indent_val = indent_step;
                for (int i=1; i <= DocmaConstants.MAX_INDENT_LEVELS; i++) {
                    String val_str = CSSUtil.formatFloatSize(indent_val);
                    pindent_xsl.append("<xsl:when test=\"$level_num = '").append(i).append("'\">")
                               .append(val_str).append(unit).append("</xsl:when>");
                    tindent_xsl.append("<xsl:when test=\"starts-with(@class, 'indent-level").append(i).append("')\">")
                               .append(val_str).append(unit).append("</xsl:when>");
                    indent_val += indent_step;
                }
            } else {
                int indent_step = CSSUtil.getSizeInt(p_indent);
                int indent_val = indent_step;
                for (int i=1; i <= DocmaConstants.MAX_INDENT_LEVELS; i++) {
                    pindent_xsl.append("<xsl:when test=\"$level_num = '").append(i).append("'\">")
                              .append(indent_val).append(unit).append("</xsl:when>");
                    tindent_xsl.append("<xsl:when test=\"starts-with(@class, 'indent-level").append(i).append("')\">")
                              .append(indent_val).append(unit).append("</xsl:when>");
                    indent_val += indent_step;
                }
            }
            pindent_xsl.append("</xsl:choose>");
            tindent_xsl.append("<xsl:otherwise>0pt</xsl:otherwise>");
            tindent_xsl.append("</xsl:choose>");

            // Create table row/cell properties XSL (based on user-defined styles)
            StringBuilder props_by_class = new StringBuilder();
            if (styles.length > 0) {
                props_by_class.append("<xsl:choose>");
                for (DocmaStyle s : styles) {
                    props_by_class.append("<xsl:when test=\"$class_val = '").append(s.getBaseId()).append("'\">");
                    getFOFromCSS(props_by_class, s.getCSS(), true);
                    props_by_class.append("</xsl:when>");
                }
                props_by_class.append("</xsl:choose>");
            }

            // User-defined styles
            StringBuilder when_block_style = new StringBuilder(500 * styles.length);
            for (DocmaStyle s : styles) {
                String bs_id = s.getBaseId();
                String css = s.getCSS();

                // Add style for the DocBook phrase element (inline formatting) 
                addInlineStyleTemplate(buf, bs_id, css);

                // Add style for the DocBook para element (block formatting)
                when_block_style.append("<xsl:when test=\"contains($role_val, ' ")
                        .append(bs_id).append(" ')\">");
                // .append("<xsl:when test=\"(@role = '")
                // .append(bs_id).append("') or contains($role_val, ' ")
                // .append(bs_id).append("') or starts-with(@role, '")
                addBlockStyleAttributes(when_block_style, bs_id, css);
                when_block_style.append("</xsl:when>");
            }

            // Add the PDF include file
            String custIncPDF = customLayerIncludes_PDF.replace("###para_spacing###", para_spacing)
                                                       .replace("###when_block_style###", when_block_style)
                                                       .replace("###para_indent###", pindent_xsl)
                                                       .replace("###table_indent###", tindent_xsl)
                                                       .replace("###table_classes###", props_by_class)
                                                       .replace("###table_row_classes###", props_by_class)
                                                       .replace("###table_cell_classes###", props_by_class);
            custIncPDF = replaceStylePlaceholders(custIncPDF, styles);
            buf.append(custIncPDF);

            // Set link style
            DocmaStyle link_style = getStyle(styles, "link", out_config);
            DocmaStyle extlink_style = getStyle(styles, "link_external", out_config);
            if (extlink_style == null) extlink_style = link_style;
            startAttributeSet(buf, "xref.properties");
            // buf.append("<xsl:choose><xsl:when test=\"self::ulink\">");
            // if (extlink_style != null) getFOFromCSS(buf, extlink_style.getCSS(), true);
            // buf.append("</xsl:when><xsl:otherwise>");
            if (link_style != null) getFOFromCSS(buf, link_style.getCSS(), true);
            // buf.append("</xsl:otherwise></xsl:choose>");
            endAttributeSet(buf);

            // Set caption style for figure and table titles
            DocmaStyle title_style = getStyle(styles, "caption", out_config);
            startAttributeSet(buf, "formal.title.properties");
            if (title_style != null) getFOFromCSS(buf, title_style.getCSS(), true);
            addAttribute(buf, "space-before.minimum", "0");
            addAttribute(buf, "space-before.optimum", "0");
            addAttribute(buf, "space-before.maximum", "0");
            addAttribute(buf, "space-after.minimum", "0");
            addAttribute(buf, "space-after.optimum", "0");
            addAttribute(buf, "space-after.maximum", "0");
            endAttributeSet(buf);

            // Set list style
            startAttributeSet(buf, "list.block.spacing");
            addAttribute(buf, "margin-left", out_config.getListIndent());
            buf.append(para_spacing); // set default para spacing for list blocks
            endAttributeSet(buf);
            startAttributeSet(buf, "list.item.spacing");
            buf.append("<xsl:attribute name=\"margin-top\"><xsl:choose>");
            buf.append("<xsl:when test=\"not(preceding-sibling::*) and (not(ancestor::listitem) or (ancestor::listitem[1]/@override = 'none'))\">0pt</xsl:when>");
            buf.append("<xsl:otherwise>").append(out_config.getItemSpace()).append("</xsl:otherwise>");
            buf.append("</xsl:choose></xsl:attribute>");
            buf.append("<xsl:attribute name=\"margin-bottom\">0em</xsl:attribute>");
            endAttributeSet(buf);
            addAttributeSet(buf, "orderedlist.label.properties", getStyle(styles, "orderedlist_label", out_config));
            addDimParam(buf, "orderedlist.label.width", out_config.getOrderedListLabelWidth());

            // Set blockquote style (is used as workaround to indent lists)
            startAttributeSet(buf, "blockquote.properties");
            addAttribute(buf, "margin-left", out_config.getListIndent());
            addAttribute(buf, "space-before.minimum", "0");
            addAttribute(buf, "space-before.optimum", "0");
            addAttribute(buf, "space-before.maximum", "0");
            addAttribute(buf, "space-after.minimum", "0");
            addAttribute(buf, "space-after.optimum", "0");
            addAttribute(buf, "space-after.maximum", "0");
            endAttributeSet(buf);

            // Set header styles
            // Note: header1 is book title style on title-page
            // addAttributeSet(buf, "component.title.properties", getStyle(styles, "header2", out_config));
            DocmaStyle h2style = getStyle(styles, "header2", out_config);
            if (h2style != null) {
                buf.append("<xsl:attribute-set name=\"component.title.properties\">\n");
                buf.append("<xsl:attribute name=\"keep-with-next.within-column\">always</xsl:attribute>");
                buf.append("<xsl:attribute name=\"hyphenate\">false</xsl:attribute>");
                buf.append("<xsl:attribute name=\"start-indent\">0pt</xsl:attribute>");
                getFOFromCSS(buf, h2style.getCSS(), true);
                buf.append("</xsl:attribute-set>\n");
            }
            addAttributeSet(buf, "section.title.level1.properties", getStyle(styles, "header3", out_config));
            addAttributeSet(buf, "section.title.level2.properties", getStyle(styles, "header4", out_config));
            addAttributeSet(buf, "section.title.level3.properties", getStyle(styles, "header5", out_config));
            addAttributeSet(buf, "section.title.level4.properties", getStyle(styles, "header6", out_config));
            addAttributeSet(buf, "section.title.level5.properties", getStyle(styles, "header7", out_config));

            // Set custom TOC header and TOC line styles
            PdfTocUtil.writeCustomTocLineXsl(buf, configBaseDir, out_config, styles);
            
            // Set Index styles
            addCustomIndexHeader(buf, getStyle(styles, "index_header", out_config));
            addCustomIndexSubHeaderProperties(buf, getStyle(styles, "index_subheader", out_config));
            addCustomIndexEntryProperties(buf, getStyle(styles, "index_entry", out_config));
            
            // Set title-page styles
            String css = getStyle_CSS(styles, "header1", out_config, "");
            addAttributeSet_CSS(buf, "docma.titlepage.title.style", css);
            css = getStyle_CSS(styles, "subtitle", out_config, DocmaConstants.STYLE_SUBTITLE_CSS);
            addAttributeSet_CSS(buf, "docma.titlepage.subtitle.style", css);
            css = getStyle_CSS(styles, "partheader", out_config, DocmaConstants.STYLE_PARTHEADER_CSS);
            addAttributeSet_CSS(buf, "docma.part.title.style", css);
            css = getStyle_CSS(styles, "title_back", out_config, DocmaConstants.STYLE_TITLEBACK_CSS);
            addAttributeSet_CSS(buf, "docma.titlepage.back.title.style", css);
            css = getStyle_CSS(styles, "corpauthor", out_config, DocmaConstants.STYLE_CORPAUTHOR_CSS);
            addAttributeSet_CSS(buf, "docma.titlepage.corpauthor.style", css);
            css = getStyle_CSS(styles, "corpauthor_back", out_config, "");
            addAttributeSet_CSS(buf, "docma.titlepage.back.corpauthor.style", css);
            css = getStyle_CSS(styles, "authorgroup", out_config, DocmaConstants.STYLE_AUTHORGROUP_CSS);
            addAttributeSet_CSS(buf, "docma.titlepage.authorgroup.style", css);
            css = getStyle_CSS(styles, "authorgroup_back", out_config, "");
            addAttributeSet_CSS(buf, "docma.titlepage.back.authorgroup.style", css);
            css = getStyle_CSS(styles, "author", out_config, DocmaConstants.STYLE_AUTHOR_CSS);
            addAttributeSet_CSS(buf, "docma.titlepage.author.style", css);
            css = getStyle_CSS(styles, "author_back", out_config, "");
            addAttributeSet_CSS(buf, "docma.titlepage.back.author.style", css);
            css = getStyle_CSS(styles, "othercredit", out_config, "");
            addAttributeSet_CSS(buf, "docma.titlepage.othercredit.style", css);
            css = getStyle_CSS(styles, "releaseinfo", out_config, DocmaConstants.STYLE_RELEASEINFO_CSS);
            addAttributeSet_CSS(buf, "docma.titlepage.releaseinfo.style", css);
            css = getStyle_CSS(styles, "copyright", out_config, "");
            addAttributeSet_CSS(buf, "docma.titlepage.copyright.style", css);
            css = getStyle_CSS(styles, "legalnotice", out_config, DocmaConstants.STYLE_LEGALNOTICE_CSS);
            addAttributeSet_CSS(buf, "docma.titlepage.legalnotice.style", css);
            css = getStyle_CSS(styles, "pubdate", out_config, DocmaConstants.STYLE_PUBDATE_CSS);
            addAttributeSet_CSS(buf, "docma.titlepage.pubdate.style", css);
            css = getStyle_CSS(styles, "abstract", out_config, DocmaConstants.STYLE_ABSTRACT_CSS);
            addAttributeSet_CSS(buf, "docma.titlepage.abstract.style", css);

            // Set page-header and page-footer styles
            addBlockAttributeSet(buf, "header.content.properties", getStyle(styles, "page_header", out_config));
            addBlockAttributeSet(buf, "footer.content.properties", getStyle(styles, "page_footer", out_config));
            addBlockAttributeSet(buf, "header.table.properties", getStyle(styles, "page_header_box", out_config));
            addBlockAttributeSet(buf, "footer.table.properties", getStyle(styles, "page_footer_box", out_config));
            addParam(buf, "header.rule", "0");  // turn off default header rule
            addParam(buf, "footer.rule", "0");  // turn off default footer rule
            if (out_config.isPdfCustomHeaderFooter()) {
                String headfoot_xsl = out_config.getPdfCustomHeaderFooterXsl();
                if (headfoot_xsl != null) buf.append(headfoot_xsl);
            }
            // Set section level for section title in header/footer
            addParam(buf, "marker.section.level", "" + out_config.getPdfMarkerSectionLevel());

            // Set footnote style
            addAttributeSet(buf, "footnote.properties", getStyle(styles, "footnote", out_config));

//            String css = def_style.getCSS();
//            String font_family = getPropValueFromCSS(css, "font-family");
//            if (font_family != null) {
//                addParam(buf, "body.font.family", font_family);
//            }
//            String font_size = getPropValueFromCSS(css, "font-size");
//            if ((font_size != null) && (font_size.endsWith("pt"))) {
//                addParam(buf, "body.font.master", font_size.substring(0, font_size.length() - 2));
//            }
//            String txt_align = getPropValueFromCSS(css, "text-align");
//            if (txt_align != null) {
//                addParam(buf, "alignment", txt_align);
//            }
//            String line_height = getPropValueFromCSS(css, "line-height");
//            if (line_height != null) {
//                addParam(buf, "line-height", line_height);
//            }

            // addMatchRoleStyle(buf, "align-left", "text-align:left;", false);
            // addMatchRoleStyle(buf, "align-right", "text-align:right;", false);
            // addMatchRoleStyle(buf, "align-center", "text-align:center;", false);
            // addMatchRoleStyle(buf, "align-full", "text-align:justify;", false);
        }

        buf.append("</xsl:stylesheet>");

        String buf_str = buf.toString();
        if (DocmaConstants.DEBUG) {
            try {
                File fname = new File(DocmaConstants.DEBUG_DIR, "custom_layer.xsl");
                FileWriter fout = new FileWriter(fname);
                fout.write(buf_str);
                fout.close();
            } catch (Exception ex) {}
        }

        return buf_str;
    }

    private static String replaceStylePlaceholders(String xsl, DocmaStyle[] styles)
    {
        final String START_PATTERN = "<!--style:";
        final String END_PATTERN = "-->";
        int pos = xsl.indexOf(START_PATTERN);
        if (pos < 0) {
            return xsl;
        }
        
        StringBuilder buf = new StringBuilder();
        int copy_pos = 0;
        do {
            // Copy up to start of placeholder
            buf.append(xsl, copy_pos, pos);
            copy_pos = pos;
            pos += START_PATTERN.length();
            
            int pos2 = xsl.indexOf(END_PATTERN, pos);
            if (pos2 < 0) {
                break;
            }
            String style_id = xsl.substring(pos, pos2).trim();
            DocmaStyle sty = getStyleById(styles, style_id);
            if (sty != null) {  // valid placeholder
                String css = sty.getCSS();
                getFOFromCSS(buf, css, true);
                pos = pos2 + END_PATTERN.length();  // continue after placeholder
                copy_pos = pos;  // do not copy placeholder
            }
            pos = xsl.indexOf(START_PATTERN, pos); // search next placeholder
        } while (pos >= 0);
        
        // Copy remaining string after the last placeholder
        if (copy_pos < xsl.length()) {
            buf.append(xsl, copy_pos, xsl.length());
        }
        return buf.toString();
    }
    
    private static DocmaStyle getStyleById(DocmaStyle[] styles, String style_id)
    {
        for (DocmaStyle s : styles) {
            if (style_id.equals(s.getBaseId())) {
                return s;
            }
        }
        return null;
    }

    static DocmaStyle getStyle(DocmaStyle[] styles, String style_base_id, DocmaOutputConfig out_config)
    {
        String variant = out_config.getStyleVariant();
        if ((variant != null) && variant.trim().equals("")) variant = null;
        DocmaStyle def_style = null;
        for (int i=0; i < styles.length; i++) {
            DocmaStyle style = styles[i];
            if (style.getBaseId().equals(style_base_id)) {
                String style_var = style.getVariantId();
                if (style_var == null) {
                    if (variant == null) return style;
                    else def_style = style;
                } else
                if ((variant != null) && variant.equals(style_var)) {
                    return style;
                }
            }
        }
        return def_style;
    }

    private static String getStyle_CSS(DocmaStyle[] styles, String style_base_id, DocmaOutputConfig out_config,
                                       String default_css)
    {
        DocmaStyle ds = getStyle(styles, style_base_id, out_config);
        if (ds == null) return default_css;
        else return ds.getCSS();
    }

    private void startAttributeSet(StringBuilder buf, String set_name)
    {
        buf.append("<xsl:attribute-set name=\"").append(set_name).append("\">\n");
    }

    private void endAttributeSet(StringBuilder buf)
    {
        buf.append("</xsl:attribute-set>\n");
    }

    private void addAttributeSet(StringBuilder buf, String set_name, DocmaStyle style)
    {
        if (style == null) return;
        addAttributeSet_CSS(buf, set_name, style.getCSS());
    }

    private void addAttributeSet_CSS(StringBuilder buf, String set_name, String css)
    {
        buf.append("<xsl:attribute-set name=\"").append(set_name).append("\">\n");
        getFOFromCSS(buf, css, true);
        buf.append("</xsl:attribute-set>\n");
    }

    private void addAttributeSet(StringBuilder buf, String set_name, String[] atts)
    {
        buf.append("<xsl:attribute-set name=\"").append(set_name).append("\">\n");
        int i = 0;
        while (i < atts.length) {
            String att_name = atts[i++];
            String att_value = atts[i++];
            addAttribute(buf, att_name, att_value);
        }
        buf.append("</xsl:attribute-set>\n");
    }

    private void addInlineStyleTemplate(StringBuilder buf, String styleId, String css)
    {
        buf.append("<xsl:template match=\"phrase[@role = '" + styleId + "']\">");
        buf.append("<fo:inline ");
        getFOFromCSS(buf, css, false);
        buf.append(">");
        buf.append("<xsl:apply-templates/>");
        buf.append("</fo:inline>");
        buf.append("</xsl:template>\n");
    }

    private void addBlockAttributeSet(StringBuilder buf, String set_name, DocmaStyle style)
    {
        if (style == null) return;
        startAttributeSet(buf, set_name);
        String css = style.getCSS();
        if (css == null) {
            css = "";   // just to be sure we get no NullPointerException
        }
        if ((css.indexOf("margin") < 0) && (css.indexOf("MARGIN") < 0)) {
            // Set margin due to FOP problem: if margin is not set, then
            // left padding of a block within a table cell is ignored.
            buf.append("<xsl:attribute name=\"margin\">0pt</xsl:attribute>");
        }
        if ((css.indexOf("padding") < 0) && (css.indexOf("PADDING") < 0)) {
            // If user has not set any padding, then set padding to 0, just  
            // to be sure that no extra space is added.
            buf.append("<xsl:attribute name=\"padding\">0pt</xsl:attribute>");
        }
        getFOFromCSS(buf, css, true);
        endAttributeSet(buf);
    }

    private void addBlockStyleAttributes(StringBuilder buf, String styleId, String css)
    {
        // System.out.println("addBlockStyleAttributes " + styleId + ": " + css);
        getFOFromCSS(buf, css, true);
        if (styleId.startsWith("header")) {
            addAttribute(buf, "keep-with-next.within-column", "always");
        }
        if ((css.indexOf("margin") < 0) && (css.indexOf("MARGIN") < 0)) {
            // set margin due to FOP problem: if margin is not set, then
            // left padding of a block within a table cell is ignored.
            addAttribute(buf, "margin", "0");
        }
        // buf.append("   xsl:use-attribute-sets=\"normal.para.spacing\" ");
    }

    private void addCustomIndexHeader(StringBuilder buf, DocmaStyle index_header)
    {
        if (index_header == null) {
            return;
        }
        buf.append("<xsl:template name=\"index.titlepage\" priority=\"1\">");
        buf.append("<fo:block>");
        buf.append("<xsl:attribute name=\"keep-with-next.within-column\">always</xsl:attribute>");
        buf.append("<xsl:attribute name=\"start-indent\">0pt</xsl:attribute>");
        getFOFromCSS(buf, index_header.getCSS(), true);
        buf.append("<xsl:call-template name=\"gentext\">");
        buf.append(" <xsl:with-param name=\"key\" select=\"'Index'\"/>");
        buf.append("</xsl:call-template>");
        buf.append("</fo:block>");
        buf.append("</xsl:template>\n");
    }

    private void addCustomIndexSubHeaderProperties(StringBuilder buf, DocmaStyle sub_head)
    {
        if (sub_head == null) {
            return;
        }
        buf.append("<xsl:attribute-set name=\"index.div.title.properties\">\n");
        buf.append("<xsl:attribute name=\"keep-with-next.within-column\">always</xsl:attribute>");
        buf.append("<xsl:attribute name=\"start-indent\">0pt</xsl:attribute>");
        getFOFromCSS(buf, sub_head.getCSS(), true);
        buf.append("</xsl:attribute-set>\n");
    }

    private void addCustomIndexEntryProperties(StringBuilder buf, DocmaStyle entry_style)
    {
        if (entry_style == null) {
            return;
        }
        buf.append("<xsl:attribute-set name=\"index.entry.properties\">\n");
        buf.append("<xsl:attribute name=\"start-indent\">0pt</xsl:attribute>");
        getFOFromCSS(buf, entry_style.getCSS(), true);
        buf.append("</xsl:attribute-set>\n");
    }

    private void addDefaultTitlePageFO(StringBuilder buf, StringBuilder fo_attribs)
    {
        buf.append("<xsl:template name=\"book.titlepage.recto\">");
        buf.append("<fo:block>");
        if (fo_attribs != null) {
            buf.append(extractXslAttribute(fo_attribs, "padding"));
            buf.append(extractXslAttribute(fo_attribs, "padding-top"));
            buf.append(extractXslAttribute(fo_attribs, "padding-bottom"));
            buf.append(extractXslAttribute(fo_attribs, "padding-left"));
            buf.append(extractXslAttribute(fo_attribs, "padding-right"));
        }
        buf.append("<xsl:apply-templates mode=\"book.titlepage.recto.auto.mode\" select=\"bookinfo/title\"/>");
        buf.append("<xsl:apply-templates mode=\"book.titlepage.recto.auto.mode\" select=\"bookinfo/subtitle\"/>");
        buf.append("<xsl:apply-templates mode=\"book.titlepage.recto.auto.mode\" select=\"bookinfo/corpauthor\"/>");
        buf.append("<xsl:apply-templates mode=\"book.titlepage.recto.auto.mode\" select=\"bookinfo/authorgroup\"/>");
        buf.append("<xsl:apply-templates mode=\"book.titlepage.recto.auto.mode\" select=\"bookinfo/author\"/>");
        buf.append("</fo:block>");
        buf.append("</xsl:template>\n");
    }
    
    private void addCustomTitlePage(StringBuilder buf, 
                                    String pagetype, 
                                    boolean is_fo, 
                                    StringBuilder fo_attribs)
    {
        buf.append("<xsl:template name=\"book.titlepage." + pagetype + "\">");
        if (is_fo) {
            buf.append("<fo:block>");
            if (fo_attribs != null) {
                buf.append(extractXslAttribute(fo_attribs, "padding"));
                buf.append(extractXslAttribute(fo_attribs, "padding-top"));
                buf.append(extractXslAttribute(fo_attribs, "padding-bottom"));
                buf.append(extractXslAttribute(fo_attribs, "padding-left"));
                buf.append(extractXslAttribute(fo_attribs, "padding-right"));
            }
        }
        buf.append("<xsl:apply-templates select=\"/book/bookinfo/abstract[@role='titlepage." + pagetype + "']/*\" />");
        if (is_fo) {
            buf.append("</fo:block>");
        }
        buf.append("</xsl:template>\n");
    }
    
    private void writeBackgroundImageAttribs(StringBuilder buf, String img_url) 
    {
        buf.append("<xsl:attribute name=\"background-image\">")
           .append(" <xsl:call-template name=\"fo-external-image\">")
           .append("  <xsl:with-param name=\"filename\" select=\"'").append(img_url).append("'\"/>")
           .append(" </xsl:call-template>")
           .append("</xsl:attribute>")
           .append("<xsl:attribute name=\"background-attachment\">fixed</xsl:attribute>")
           .append("<xsl:attribute name=\"background-repeat\">no-repeat</xsl:attribute>")
           .append("<xsl:attribute name=\"background-position-horizontal\">center</xsl:attribute>")
           .append("<xsl:attribute name=\"background-position-vertical\">center</xsl:attribute>");
    }

    private void writeSelectUserPageMasterTitlepageCover(StringBuilder buf) 
    {
        buf.append("\n")
           .append("<xsl:template name=\"select.user.pagemaster\">")
           .append(" <xsl:param name=\"element\" />")
           .append(" <xsl:param name=\"pageClass\" />")
           .append(" <xsl:param name=\"default-pagemaster\" />")
           .append(" <xsl:choose>")
           .append("   <xsl:when test=\"(($default-pagemaster = 'titlepage') or ($default-pagemaster = 'titlepage-draft')) and ($element = 'book')\">")
           .append("     <xsl:value-of select=\"'coverpage'\" />")
           .append("   </xsl:when>")
           .append("   <xsl:otherwise>")
           .append("     <xsl:value-of select=\"$default-pagemaster\" />")
           .append("   </xsl:otherwise>")
           .append(" </xsl:choose>")
           .append("</xsl:template>\n");
    }
            
    static void getFOFromCSS(StringBuilder buf, 
                             String css, 
                             boolean as_attributes)
    {
        getFOFromCSS(buf, css, as_attributes, false);
    }
    
    static void getFOFromCSS(StringBuilder buf, 
                             String css, 
                             boolean as_attributes, 
                             boolean mirrored)
    {
        if (css == null) return;
        css = css.trim();
        if (css.length() == 0) return;

        SortedMap<String, String> props = CSSParser.parseCSSProperties(css);
        for (Map.Entry<String, String> prop : props.entrySet()) {
            String pname_css = prop.getKey().toLowerCase();
            String pname_fo = (String) map_CSS_FO.get(pname_css);
            if (pname_fo != null) {
                String pval = prop.getValue();
                if (pval.length() > 0) {
                    if (mirrored) {
                        // String mirr_name = (String) map_FO_MIRRORED.get(pname_fo);
                        // if (mirr_name != null) {
                        //     pname_fo = mirr_name;
                        // }

                        // mirror text-align (on even pages for double-sided output)
                        if (pname_fo.equals("text-align")) { 
                            if (pval.equalsIgnoreCase("left")) {
                                pval = "right";
                            } else
                            if (pval.equalsIgnoreCase("right")) {
                                pval = "left";
                            }
                        }
                    }
                    if (as_attributes) {
                        addAttribute(buf, pname_fo, pval);
                    } else {
                        buf.append(pname_fo).append("=\"")
                           .append(XMLUtil.escapeDoubleQuotedCDATA(pval))
                           .append("\" ");
                    }
                }
            }
        }

//        String css_lower = css.toLowerCase() + ";";
//        for (int i=0; i < CSS_PROPS.length; i++) {
//            String PROP = CSS_PROPS[i][0];
//            String propval = getPropValueFromCSS(css, css_lower, PROP);
//            if (propval != null) {
//                if (as_attributes) {
//                    addAttribute(buf, PROP, propval);
//                } else {
//                    buf.append(PROP + "=\"" + propval + "\" ");
//                }
//            }
//        }
    }

//    private String getPropValueFromCSS(String css, String css_lower, String prop_name)
//    {
//        int idx = css_lower.indexOf(prop_name + ":");
//        if (idx >= 0) {
//            idx += prop_name.length() + 1;
//            int idx2 = css_lower.indexOf(';', idx);
//            if (idx2 > 0) {
//                return css.substring(idx, idx2).trim();
//            }
//        }
//        return null;
//    }

    private static String extractXslAttribute(StringBuilder attribs, String att_name)
    {
        final String PATTERN = " name=\"" + att_name + "\"";
        final String START_TAG = "<xsl:attribute";
        final String END_TAG = "</xsl:attribute>";
        int name_start = attribs.indexOf(PATTERN);
        if (name_start > 0) {
            int att_start = attribs.lastIndexOf(START_TAG, name_start);
            int att_end = attribs.indexOf(END_TAG, name_start + PATTERN.length());
            if ((att_start >= 0) && (att_end >= 0)) {
                return attribs.substring(att_start, att_end + END_TAG.length());
            }
        }
        return "";   // attribute not found
    }
    
    private static void addParam(StringBuilder buf, String name, String value)
    {
        buf.append("<xsl:param name=\"").append(name).append("\">");
        buf.append(value);
        buf.append("</xsl:param>\n");
    }

    private static void addDimParam(StringBuilder buf, String name, String value)
    {
        if ((value != null) && (value.trim().length() > 2)) {
            buf.append("<xsl:param name=\"").append(name).append("\">");
            buf.append(value);
            buf.append("</xsl:param>\n");
        }
    }

    private static void addAttribute(StringBuilder buf, String name, String value)
    {
        buf.append("<xsl:attribute name=\"").append(name).append("\">");
        buf.append(XMLUtil.escapePCDATA(value));
        buf.append("</xsl:attribute>\n");
    }

    File createTempFile(String file_extension)
    {
        deleteOldTempFiles();
        File f;
        do {
            long stamp = System.currentTimeMillis();
            f = new File(tempDir, TEMPFILE_PREFIX + stamp + "." + file_extension);
        } while (f.exists());
        return f;
    }

    File createTempDir()
    {
        deleteOldTempFiles();
        File f;
        do {
            long stamp = System.currentTimeMillis();
            f = new File(tempDir, TEMPFILE_PREFIX + stamp);
            if (! f.exists()) {
                f.mkdirs();
                break;
            }
        } while (true);
        return f;
    }

    private void deleteOldTempFiles()
    {
        if (++tempDeleteCounter >= 10) {  // delete old temp files after every 10th call
            tempDeleteCounter = 0;
            long currentTime = System.currentTimeMillis();
            String[] filenames = tempDir.list();
            int pref_len = TEMPFILE_PREFIX.length();
            for (int i=0; i < filenames.length; i++) {
                String fn = filenames[i];
                if (fn.startsWith(TEMPFILE_PREFIX)) {
                    int p = fn.indexOf('.');
                    String num = (p < 0) ? fn.substring(pref_len) : fn.substring(pref_len, p);
                    try {
                        long stamp = Long.parseLong(num);
                        if ((currentTime - stamp) > (5 * 24 * 60 * 60 * 1000)) { // delete after 5 days
                            File del_file = new File(tempDir, fn);
                            if (del_file.isDirectory()) {
                                DocmaUtil.recursiveFileDelete(del_file);
                            } else {
                                del_file.delete();
                            }
                        }
                    } catch (Exception ex) {}
                }
            }
        }
    }


}
