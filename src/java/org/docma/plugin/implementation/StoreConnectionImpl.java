/*
 * StoreConnectionImpl.java
 * 
 *  Copyright (C) 2016  Manfred Paula, http://www.docmenta.org
 *   
 *  This file is part of Docmenta. Docmenta is free software: you can 
 *  redistribute it and/or modify it under the terms of the GNU Lesser 
 *  General Public License as published by the Free Software Foundation, 
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Docmenta.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.docma.plugin.implementation;

import java.io.File;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.docma.app.DocmaAppUtil;
import org.docma.app.DocmaExportJob;
import org.docma.app.DocmaNode;
import org.docma.app.DocmaOutputConfig;
import org.docma.app.DocmaPublication;
import org.docma.app.DocmaPublicationConfig;
import org.docma.app.DocmaSession;
import org.docma.app.DocmaStyle;
import org.docma.app.ImageUtil;
import org.docma.coreapi.DocI18n;
import org.docma.coreapi.DocImageRendition;
import org.docma.coreapi.DocVersionId;
import org.docma.coreapi.DocmaExportLog;
import org.docma.plugin.CharEntity;
import org.docma.plugin.Content;
import org.docma.plugin.DocmaException;
import org.docma.plugin.ExportJob;
import org.docma.plugin.FileContent;
import org.docma.plugin.Folder;
import org.docma.plugin.FolderType;
import org.docma.plugin.Group;
import org.docma.plugin.ImageFile;
import org.docma.plugin.ImageRenditionInfo;
import org.docma.plugin.Language;
import org.docma.plugin.LogEntries;
import org.docma.plugin.Node;
import org.docma.plugin.NodeInfo;
import org.docma.plugin.OutputConfig;
import org.docma.plugin.PubContent;
import org.docma.plugin.PubSection;
import org.docma.plugin.Publication;
import org.docma.plugin.PublicationConfig;
import org.docma.plugin.Reference;
import org.docma.plugin.Style;
import org.docma.plugin.VersionId;
import org.docma.plugin.StoreConnection;
import org.docma.plugin.StoreClosedException;
import org.docma.plugin.UserSession;
import org.docma.plugin.VersionState;
import org.docma.app.EditContentTransformer;
import org.docma.webapp.ImageRenditions;

/**
 *
 * @author MP
 */
public class StoreConnectionImpl implements StoreConnection
{
    private final UserSession userSess;
    private DocmaSession docmaSess = null;
    private String connectionId = null;
    private final String storeId;
    private final DocVersionId docVerId;  // the internal version id
    private final VersionId verId;        // the plugin interface wrapper of the version id
    private final boolean isUIConnection;

    public StoreConnectionImpl(UserSession userSess, 
                               DocmaSession docmaSess, 
                               String storeId, 
                               DocVersionId docVerId, 
                               boolean isUI)
    {
        this.userSess = userSess;
        this.docmaSess = docmaSess;
        this.storeId = storeId;
        this.docVerId = docVerId;
        this.verId = VersionIdCreator.create(docVerId);
        this.isUIConnection = isUI;
    }

    // *******************************************************************    
    // ********* Interface StoreConnection (visible by plugins) **********
    // *******************************************************************

    //
    // ***************  Basic connection properties  *****************
    //
    
    public String getConnectionId()
    {
        if (connectionId == null) {
            connectionId = "scon" + hashCode() + "_" + storeId + "_" + verId;
        }
        return connectionId;
    }

    public UserSession getUserSession() 
    {
        return userSess;
    }

    public void close() throws DocmaException
    {
        if (docmaSess != null) {
            if (isUIConnection) {
                throw new DocmaException("Closing of UI store connection is not allowed.");
            }
            docmaSess.closeSession();
            docmaSess = null;
        }
    }


    public boolean isClosed() 
    {
        // There are two cases:
        // 1. If this is a temporary store connection, then the connection is
        // closed by the plugin itself by calling closeTempStoreConnection(...)   
        // on the plugin UserSession object or in onSessionClose() if session is
        // closed by user. In this case docmaSess is set to null.
        // 2. If this is a store connection returned by  
        // UserSession.getOpenedStore() then this is the store currently opened
        // in the UI. User can switch to another store in the UI at any time.
        // Therefore, before each method-call that accesses the store, it needs 
        // to be checked whether docmaSess is still connected to the same store 
        // as when this StoreConnection object has been created.

        return (docmaSess == null) || 
               (! storeId.equals(docmaSess.getStoreId())) || 
               (! docVerId.equals(docmaSess.getVersionId()));
    }

    public String getStoreId() 
    {
        return storeId;
    }

    public VersionId getVersionId() 
    {
        return verId;
    }

    public String getStoreTitle() throws DocmaException 
    {
        checkClosed();
        return docmaSess.getDocStoreTitle();
    }

    public String[] getTextFileExtensions() throws DocmaException 
    {
        checkClosed();
        return docmaSess.getTextFileExtensions();
    }

    public boolean isTextFileExtension(String ext) throws DocmaException 
    {
        checkClosed();
        return docmaSess.isTextFileExtension(ext);
    }

    //
    // ***************  Language related methods  *****************
    //
    
    public void enterTranslationMode(String langCode)  throws DocmaException
    {
        checkClosed();
        try {
            docmaSess.enterTranslationMode(langCode);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void leaveTranslationMode()  throws DocmaException
    {
        checkClosed();
        try {
            docmaSess.leaveTranslationMode();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getTranslationMode()  throws DocmaException
    {
        checkClosed();
        try {
            return docmaSess.getTranslationMode();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public Language getCurrentLanguage() throws DocmaException 
    {
        checkClosed();
        try {
            return docmaSess.getLanguage();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public Language getOriginalLanguage() throws DocmaException 
    {
        checkClosed();
        try {
            return docmaSess.getOriginalLanguage();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public Language[] getTranslationLanguages() throws DocmaException 
    {
        checkClosed();
        try {
            return docmaSess.getTranslationLanguages();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public boolean hasTranslationLanguage(String lang_code) 
    {
        checkClosed();
        try {
            return docmaSess.hasTranslationLanguage(storeId, lang_code);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    //
    // ***************  Transaction methods  *****************
    //
    
    public void startTransaction() throws DocmaException 
    {
        checkClosed();
        try {
            docmaSess.startTransaction();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void commitTransaction() throws DocmaException 
    {
        checkClosed();
        try {
            docmaSess.commitTransaction();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void rollbackTransaction() 
    {
        checkClosed();
        docmaSess.rollbackTransaction();
    }

    public boolean runningTransaction() 
    {
        checkClosed();
        return docmaSess.runningTransaction();
    }


    //
    // ***************  Node creation methods  ****************
    //
    
    public FileContent createFileContent(String filename) throws DocmaException 
    {
        return createFileContent(filename, false);
    }

    public FileContent createFileContent(String filename, boolean setAlias) throws DocmaException 
    {
        String ext = "";
        String name = filename;
        int p = filename.lastIndexOf('.');
        if (p > 0) {
            ext = filename.substring(p + 1);
            name = filename.substring(0, p);
        }
        DocmaNode nd;
        try {
            if ((ext.length() > 0) && ImageUtil.isSupportedImageExtension(ext)) {
                nd = docmaSess.createImageContent();
                if (setAlias) {
                    nd.setAlias(name);
                    nd.setFileExtension(ext);
                } else {
                    nd.setFileName(filename);
                }
                String mime = ImageUtil.guessMIMETypeByExt(ext);
                if (mime != null) {
                    nd.setContentType(mime);
                }
            } else {
                nd = docmaSess.createFileContent();
                nd.setFileName(filename);
                if (setAlias) {
                    nd.setAlias(name);
                }
            }
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
        return (FileContent) NodeImpl.createNodeInstance(this, nd);
    }

    public Folder createFolder(String name, FolderType folderType) throws DocmaException 
    {
        DocmaNode nd;
        try {
            if ((folderType != null) && folderType.equals(FolderType.IMAGE)) {
                nd = docmaSess.createImageFolder();
            } else {
                nd = docmaSess.createFileFolder();
            }
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
        Folder f = (Folder) NodeImpl.createNodeInstance(this, nd);
        if (name != null) {
            f.setName(name);
        }
        return f;
    }

    public PubContent createPubContent(String title) throws DocmaException 
    {
        DocmaNode nd;
        try {
            nd = docmaSess.createHTMLContent();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
        PubContent pcont = (PubContent) NodeImpl.createNodeInstance(this, nd);
        if (title != null) {
            pcont.setTitle(title);
        }
        return pcont;
    }

    public PubSection createPubSection(String title) throws DocmaException 
    {
        DocmaNode nd;
        try {
            nd = docmaSess.createSection();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
        PubSection psect = (PubSection) NodeImpl.createNodeInstance(this, nd);
        if (title != null) {
            psect.setTitle(title);
        }
        return psect;
    }

    public Reference createContentInclusion() throws DocmaException 
    {
        DocmaNode nd;
        try {
            nd = docmaSess.createContentIncludeReference();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
        return (Reference) NodeImpl.createNodeInstance(this, nd);
    }

    public Reference createSectionInclusion() throws DocmaException 
    {
        DocmaNode nd;
        try {
            nd = docmaSess.createSectionIncludeReference();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
        return (Reference) NodeImpl.createNodeInstance(this, nd);
    }
    
    //
    // ***************  Node retrieval methods  *****************
    // 
    
    public Group getRoot() throws DocmaException 
    {
        checkClosed();
        DocmaNode nd;
        try {
            nd = docmaSess.getRoot();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
        return (nd == null) ? null : (Group) NodeImpl.createNodeInstance(this, nd);
    }

    public Node getNodeById(String id) throws DocmaException
    {
        checkClosed();
        DocmaNode nd;
        try {
            nd = docmaSess.getNodeById(id);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
        return (nd == null) ? null : NodeImpl.createNodeInstance(this, nd);
    }

    public Node getNodeByAlias(String alias) throws DocmaException 
    {
        checkClosed();
        DocmaNode nd;
        try {
            nd = docmaSess.getNodeByAlias(alias);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
        return (nd == null) ? null : NodeImpl.createNodeInstance(this, nd);
    }

    public String getNodeIdByAlias(String alias) throws DocmaException 
    {
        checkClosed();
        try {
            return docmaSess.getNodeIdByAlias(alias);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public Node[] getNodesByLinkName(String linkName) throws DocmaException 
    {
        checkClosed();
        try {
            DocmaNode[] arr = docmaSess.getNodesByLinkAlias(linkName);
            Node[] res = new Node[arr.length];
            for (int i = 0; i < res.length; i++) {
                res[i] = NodeImpl.createNodeInstance(this, arr[i]);
            }
            return res;
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String[] getAliases(Class nodeCls) throws DocmaException 
    {
        if (nodeCls == null) {
            nodeCls = Node.class;
        }
        String clsName = nodeCls.getName();
        
        if (Node.class.getName().equals(clsName)) {
            return docmaSess.getNodeAliases();
        } else if (Group.class.getName().equals(clsName)) {
            return docmaSess.getGroupAliases();
        } else if (PubSection.class.getName().equals(clsName)) {
            return docmaSess.getSectionAliases();
        } else if (Folder.class.getName().equals(clsName)) {
            return docmaSess.getFolderAliases();
        } else if (Content.class.getName().equals(clsName)) {
            return docmaSess.getContentAliases();
        } else if (PubContent.class.getName().equals(clsName)) {
            return docmaSess.getHTMLContentAliases();
        } else if (FileContent.class.getName().equals(clsName)) {
            // On the persistence layer, an image is no specialization
            // of a file, but is a content type of its own.
            // In the plugin API ImageFile is derived from FileContent.
            // Therefore in the plugin API the image aliases have to be 
            // added to the file aliases.
            String[] a1 = docmaSess.getFileAliases();
            String[] a2 = docmaSess.getImageAliases();
            // Merge two sorted arrays a1 and a2 into one sorted array
            String[] res = new String[a1.length + a2.length];
            int i1 = 0; 
            int i2 = 0;
            int i = 0;
            while (i < res.length) {
                if (a1[i1].compareTo(a2[i2]) <= 0) {
                    res[i++] = a1[i1++];
                } else {
                    res[i++] = a2[i2++];
                }
            }
            return res;
        } else if (ImageFile.class.getName().equals(clsName)) {
            return docmaSess.getImageAliases();
        } else if (Reference.class.getName().equals(clsName)) {
            return docmaSess.getReferenceAliases();
        } else {
            throw new DocmaException("Unknown class " + clsName);
        }
    }

    public NodeInfo[] getNodeInfos(Class nodeCls) throws DocmaException 
    {
        if (nodeCls == null) {
            nodeCls = Node.class;
        }
        String clsName = nodeCls.getName();

        List<org.docma.coreapi.NodeInfo> res;
        if (Node.class.getName().equals(clsName)) {
            res = docmaSess.listNodeInfos();
        } else if (Group.class.getName().equals(clsName)) {
            res = docmaSess.listGroupInfos();
        } else if (PubSection.class.getName().equals(clsName)) {
            res = docmaSess.listSectionInfos();
        } else if (Folder.class.getName().equals(clsName)) {
            res = docmaSess.listFolderInfos();
        } else if (Content.class.getName().equals(clsName)) {
            res = docmaSess.listContentInfos();
        } else if (PubContent.class.getName().equals(clsName)) {
            res = docmaSess.listHTMLContentInfos();
        } else if (FileContent.class.getName().equals(clsName)) {
            // On the persistence layer, an image is no specialization
            // of a file, but is a content type of its own.
            // In the plugin API ImageFile is derived from FileContent.
            // Therefore in the plugin API the image infos have to be 
            // added to the file infos.
            List<org.docma.coreapi.NodeInfo> list1 = docmaSess.listFileInfos();
            List<org.docma.coreapi.NodeInfo> list2 = docmaSess.listImageInfos();
            res = new ArrayList<org.docma.coreapi.NodeInfo>(Math.max(16, list1.size() + list2.size()));
            res.addAll(list1);
            res.addAll(list2);
        } else if (ImageFile.class.getName().equals(clsName)) {
            res = docmaSess.listImageInfos();
        } else if (Reference.class.getName().equals(clsName)) {
            res = docmaSess.listReferenceInfos();
        } else {
            throw new DocmaException("Unknown class " + clsName);
        }
        
        // Convert to array and cast from org.docma.coreapi.NodeInfo 
        // to org.docma.plugin.NodeInfo.
        return res.toArray(new NodeInfo[res.size()]);
    }


    //
    // ***************  Style methods  *****************
    //
    
    public String[] getStyleIds() throws DocmaException 
    {
        checkClosed();
        try {
            return docmaSess.getStyleIds();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String[] getStyleVariantNames() throws DocmaException 
    {
        checkClosed();
        try {
            return docmaSess.getStyleVariantIds();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public Style[] getStyles() throws DocmaException
    {
        checkClosed();
        try {
            return docmaSess.getStyles();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public Style[] getStylesById(String... id_values) throws DocmaException 
    {
        checkClosed();
        try {
            return docmaSess.getStyles(id_values);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public Style[] getStyles(String variantName) throws DocmaException 
    {
        checkClosed();
        try {
            return docmaSess.getStyles(variantName);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public Style getStyleById(String styleId) throws DocmaException 
    {
        checkClosed();
        try {
            return docmaSess.getStyle(styleId);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public Style getStyleVariant(String baseId, String variantName) throws DocmaException 
    {
        checkClosed();
        try {
            return docmaSess.getStyleVariant(baseId, variantName);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public Style createStyle(String styleId, boolean blockStyle, String styleName, String css) throws DocmaException 
    {
        checkClosed();
        try {
            String stype = blockStyle ? DocmaStyle.BLOCK_STYLE : DocmaStyle.INLINE_STYLE;
            return new DocmaStyle(styleId, stype, styleName, css);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public Style createStyle(String styleId, boolean blockStyle, String styleTitle, Style template) throws DocmaException
    {
        checkClosed();
        try {
            DocmaStyle res = (DocmaStyle) ((DocmaStyle) template).clone();
            res.setId(styleId);
            res.setType(blockStyle ? DocmaStyle.BLOCK_STYLE : DocmaStyle.INLINE_STYLE);
            res.setTitle(styleTitle);
            return res;
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void saveStyle(Style style) throws DocmaException 
    {
        checkClosed();
        try {
            docmaSess.saveStyle((DocmaStyle) style);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void deleteStyle(String styleId) throws DocmaException 
    {
        checkClosed();
        try {
            docmaSess.deleteStyle(styleId);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getStylesCSS() throws DocmaException 
    {
        checkClosed();
        try {
            return docmaSess.getCSS();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getStylesCSS(String variant) throws DocmaException 
    {
        checkClosed();
        try {
            return docmaSess.getCSS(variant);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String getCSSForPreview(OutputConfig conf) throws DocmaException 
    {
        return getCSSComplete(conf, false, false);
    }
    
    public String getCSSForEdit(OutputConfig conf) throws DocmaException 
    {
        return getCSSComplete(conf, false, true);
    }
    
    public String getCSSForExport(OutputConfig conf) throws DocmaException 
    {
        return getCSSComplete(conf, true, false);
    }
    
    //
    // ***************  Applicability methods  *****************
    //

    public String[] getDeclaredApplics() throws DocmaException 
    {
        checkClosed();
        try {
            return docmaSess.getDeclaredApplics();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void setDeclaredApplics(String... applics) throws DocmaException 
    {
        checkClosed();
        try {
            docmaSess.setDeclaredApplics(applics);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void addDeclaredApplics(String... applics) throws DocmaException 
    {
        checkClosed();
        try {
            docmaSess.addDeclaredApplics(applics);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void removeDeclaredApplics(String... applics) throws DocmaException 
    {
        checkClosed();
        try {
            docmaSess.removeDeclaredApplics(applics);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    //
    // ***************  Output configuration methods  *****************
    //

    public String[] getOutputConfigIds() throws DocmaException 
    {
        checkClosed();
        try {
            return docmaSess.getOutputConfigIds();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public OutputConfig getOutputConfig(String outConfigId) throws DocmaException 
    {
        checkClosed();
        try {
            if (Arrays.asList(docmaSess.getOutputConfigIds()).contains(outConfigId)) {
                return new OutputConfigImpl(docmaSess.getOutputConfig(outConfigId));
            } else {
                return null;
            }
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public OutputConfig createOutputConfig(String outConfigId, String format, String subFormat) throws DocmaException 
    {
        checkClosed();
        try {
            return new OutputConfigImpl(new DocmaOutputConfig(outConfigId, format, subFormat));
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void saveOutputConfig(OutputConfig outConf) throws DocmaException 
    {
        checkClosed();
        try {
            DocmaOutputConfig dc = ((OutputConfigImpl) outConf).getDocmaOutputConfig();
            docmaSess.saveOutputConfig(dc);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void deleteOutputConfig(String outConfigId) throws DocmaException 
    {
        checkClosed();
        try {
            docmaSess.deleteOutputConfig(outConfigId);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    //
    // ***************  Publication configuration methods  *****************
    //

    public String[] getPublicationConfigIds() throws DocmaException 
    {
        checkClosed();
        try {
            return docmaSess.getPublicationConfigIds();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public PublicationConfig getPublicationConfig(String pubConfigId) throws DocmaException 
    {
        checkClosed();
        try {
            if (Arrays.asList(docmaSess.getPublicationConfigIds()).contains(pubConfigId)) {
                return docmaSess.getPublicationConfig(pubConfigId);
            } else {
                return null;
            }
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public PublicationConfig createPublicationConfig(String pubConfigId) throws DocmaException 
    {
        checkClosed();
        try {
            return new DocmaPublicationConfig(pubConfigId);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void savePublicationConfig(PublicationConfig pubConf) throws DocmaException 
    {
        checkClosed();
        try {
            docmaSess.savePublicationConfig((DocmaPublicationConfig) pubConf);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void deletePublicationConfig(String pubConfigId) throws DocmaException 
    {
        checkClosed();
        try {
            docmaSess.deletePublicationConfig(pubConfigId);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    //
    // ***************  Publication export methods  *****************
    //

    public Publication[] listPublications(String langCode, VersionState versionState) throws DocmaException 
    {
        checkClosed();
        try {
            DocmaPublication[] pubs = docmaSess.listPublications(langCode, versionState.toString());
            Publication[] res = new Publication[pubs.length];
            for (int i = 0; i < res.length; i++) {
                res[i] = new PublicationImpl(pubs[i]);
            }
            return res;
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void deletePublication(String publicationId) throws DocmaException 
    {
        checkClosed();
        try {
            docmaSess.deletePublication(publicationId);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String[] getPublicationIds() throws DocmaException
    {
        checkClosed();
        try {
            return docmaSess.getPublicationIds();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public Publication getPublication(String publicationId) throws DocmaException 
    {
        checkClosed();
        try {
            if (Arrays.asList(docmaSess.getPublicationIds()).contains(publicationId)) {
                DocmaPublication pub = docmaSess.getPublication(publicationId);
                return new PublicationImpl(pub);
            } else {
                return null;
            }
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public Publication exportPublication(String pubConfigId, String outConfigId, String langCode, String filename) throws DocmaException 
    {
        checkClosed();
        try {
            String pub_id = docmaSess.createPublication(pubConfigId, outConfigId, langCode, filename);
            docmaSess.exportPublication(pub_id);
            return new PublicationImpl(docmaSess.getPublication(pub_id));
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public Publication exportPublicationAsync(String pubConfigId, String outConfigId, String langCode, String filename) throws DocmaException 
    {
        checkClosed();
        try {
            String pub_id = docmaSess.createPublication(pubConfigId, outConfigId, langCode, filename);
            Publication res = new PublicationImpl(docmaSess.getPublication(pub_id));
            docmaSess.exportPublicationAsync(pub_id);
            return res;
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public ExportJob getExportJob(String publicationId) throws DocmaException 
    {
        checkClosed();
        try {
            DocmaExportJob docJob = docmaSess.getExportJob(storeId, docVerId, publicationId);
            return (docJob == null) ? null : new ExportJobImpl(docJob);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public int getExportJobPosition(String publicationId) throws DocmaException 
    {
        checkClosed();
        try {
            return docmaSess.getExportJobPosition(storeId, docVerId, publicationId);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public LogEntries previewPDF(OutputStream outstream, String node_id, PublicationConfig pconf, OutputConfig oconf) throws DocmaException 
    {
        checkClosed();
        try {
            DocmaPublicationConfig doc_pc = (DocmaPublicationConfig) pconf;
            DocmaOutputConfig doc_oc = ((OutputConfigImpl) oconf).getDocmaOutputConfig();
            DocmaExportLog log = docmaSess.previewPDF(outstream, node_id, doc_pc, doc_oc);
            return (log == null) ? null : new LogEntriesImpl(log);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }
    
    //
    // ***************  Character entity methods  *****************
    //

    public CharEntity[] getCharEntities() throws DocmaException
    {
        checkClosed();
        try {
            return docmaSess.getCharEntities();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String decodeCharEntities(String str) throws DocmaException 
    {
        checkClosed();
        try {
            return docmaSess.decodeCharEntities(str);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String toCharEntities(String text, boolean symbolic) throws DocmaException
    {
        checkClosed();
        try {
            return docmaSess.encodeCharEntities(text, symbolic);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String toCharEntities(String text, boolean symbolic, boolean keepEntities) throws DocmaException 
    {
        checkClosed();
        try {
            return docmaSess.encodeCharEntities(text, symbolic, keepEntities);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public String toCharEntity(char ch, boolean symbolic) throws DocmaException 
    {
        checkClosed();
        try {
            return docmaSess.toCharEntity(ch, symbolic);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    //
    // ***************  Other methods  *****************
    //

    public void checkUpdateVersionAllowed() throws DocmaException
    {
        checkClosed();
        try {
            docmaSess.checkUpdateVersionAllowed();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }
    
    public void copyNodes(Node[] sourceNodes, Node targetParent, Node refChild) throws DocmaException 
    {
        checkClosed();
        try {
            DocmaNode[] src = new DocmaNode[sourceNodes.length];
            for (int i = 0; i < src.length; i++) {
                src[i] = ((NodeImpl) sourceNodes[i]).docNode;
            }
            DocmaNode par = ((NodeImpl) targetParent).docNode;
            DocmaNode ref = (refChild == null) ? null : ((NodeImpl) refChild).docNode;
            docmaSess.copyNodes(src, par, ref);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void exportNodesToFile(Node[] nodes, File exportFile) throws DocmaException 
    {
        checkClosed();
        try {
            DocmaNode[] src = new DocmaNode[nodes.length];
            for (int i = 0; i < src.length; i++) {
                src[i] = ((NodeImpl) nodes[i]).docNode;
            }
            docmaSess.exportNodesToFile(src, exportFile);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public void importNodes(Node parentNode, File importDir, Language[] langs) throws DocmaException 
    {
        checkClosed();
        try {
            DocmaNode par = ((NodeImpl) parentNode).docNode;
            docmaSess.importNodes(par, importDir, PlugHelper.toDocmaLanguages(langs));
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public FileContent getGenTextFile() throws DocmaException 
    {
        checkClosed();
        DocmaNode nd;
        try {
            nd = docmaSess.getGenTextNode();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
        return (nd == null) ? null : (FileContent) NodeImpl.createNodeInstance(this, nd);
    }

    public Properties getGenTextProperties() throws DocmaException 
    {
        checkClosed();
        try {
            return docmaSess.getGenTextProps();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public boolean isValidAlias(String name) 
    {
        return DocmaAppUtil.isValidAlias(name);
    }

    public String[] listImageRenditionNames() throws DocmaException 
    {
        // Currently only the pre-defined thumb names are listed.
        return ImageRenditions.listThumbRenditionNames();
    }

    public ImageRenditionInfo getImageRenditionInfo(String renditionName) throws DocmaException 
    {
        DocImageRendition rend = ImageRenditions.getImageRenditionInfo(renditionName);
        return (rend == null) ? null : new ImageRenditionInfoImpl(rend);
    }
    
    public LogEntries prepareHTMLForSave(StringBuilder content, String nodeId, Map<Object, Object> props) throws DocmaException
    {
        return prepareHTMLForSave(content, nodeId, true, props);
    }
    
    public LogEntries prepareHTMLForSave(StringBuilder content, String nodeId, boolean autoCorrect, Map<Object, Object> props) throws DocmaException
    {
        try {
            return EditContentTransformer.prepareHTMLForSave(content, nodeId, props, autoCorrect, docmaSess);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    public LogEntries checkHTML(StringBuilder content, String nodeId, boolean autoCorrect, Map<Object, Object> props) throws DocmaException
    {
        try {
            return EditContentTransformer.checkHTML(content, nodeId, props, autoCorrect, docmaSess);
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

    // ********************************************************************
    // ********* Other methods (not directly visible by plugins) **********
    // ********************************************************************

    DocmaSession getDocmaSession()
    {
        return docmaSess;
    }
    
    DocI18n getI18n()
    {
        return docmaSess.getI18n();
    }
    
    private void checkClosed()
    {
        if (isClosed()) { 
            throw new StoreClosedException("The connection to store " + storeId + "/" + verId + " is closed.");
        }
    }
    
    void setConnectionId(String connId)
    {
        if (connectionId != null) {
            throw new RuntimeException("Cannot overwrite store connection ID.");
        }
        this.connectionId = connId;
    }
    
    DocVersionId getDocVersionId()
    {
        return docVerId;
    }

    private String getCSSComplete(OutputConfig conf, boolean exportMode, boolean editMode) throws DocmaException 
    {
        checkClosed();
        try {
            StringWriter sw = new StringWriter();
            DocmaOutputConfig outConf = ((OutputConfigImpl) conf).getDocmaOutputConfig();
            DocmaAppUtil.writeContentCSS(docmaSess, outConf, exportMode, editMode, sw);
            return sw.toString();
        } catch (Exception ex) {
            throw new DocmaException(ex);
        }
    }

}
