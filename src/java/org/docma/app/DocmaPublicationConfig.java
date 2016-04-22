/*
 * DocmaPublicationConfig.java
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
public class DocmaPublicationConfig
{
    private static final String PROP_VERSION_PUBCONFIG_IDS = "docversion.pubconfig.ids";
    private static final String PROP_VERSION_PUBCONFIG_ROOT = "docversion.pubconfig.root";
    private static final String PROP_VERSION_PUBCONFIG_PREFACE = "docversion.pubconfig.preface";
    private static final String PROP_VERSION_PUBCONFIG_APPENDIX = "docversion.pubconfig.appendix";
    private static final String PROP_VERSION_PUBCONFIG_COVER = "docversion.pubconfig.cover";
    private static final String PROP_VERSION_PUBCONFIG_FILTER = "docversion.pubconfig.filter";
    private static final String PROP_VERSION_PUBCONFIG_REFERENCED_PUBS = "docversion.pubconfig.refpubs";
    private static final String PROP_VERSION_PUBCONFIG_CUSTOM_TITLE1 = "docversion.pubconfig.custom.title1";
    private static final String PROP_VERSION_PUBCONFIG_CUSTOM_TITLE2 = "docversion.pubconfig.custom.title2";
    private static final String PROP_VERSION_PUBCONFIG_TITLE = "docversion.pubconfig.title";
    private static final String PROP_VERSION_PUBCONFIG_SUBTITLE = "docversion.pubconfig.subtitle";
    private static final String PROP_VERSION_PUBCONFIG_RELEASEINFO = "docversion.pubconfig.releaseinfo";
    private static final String PROP_VERSION_PUBCONFIG_CORPORATE = "docversion.pubconfig.corporate";
    private static final String PROP_VERSION_PUBCONFIG_AUTHORS = "docversion.pubconfig.authors";
    private static final String PROP_VERSION_PUBCONFIG_PUB_DATE = "docversion.pubconfig.pubdate";
    private static final String PROP_VERSION_PUBCONFIG_COPYRIGHT_YEAR = "docversion.pubconfig.copyright.year";
    private static final String PROP_VERSION_PUBCONFIG_COPYRIGHT_HOLDER = "docversion.pubconfig.copyright.holder";
    private static final String PROP_VERSION_PUBCONFIG_ABSTRACT = "docversion.pubconfig.abstract";
    private static final String PROP_VERSION_PUBCONFIG_LEGALNOTICE = "docversion.pubconfig.legalnotice";
    private static final String PROP_VERSION_PUBCONFIG_CREDIT = "docversion.pubconfig.credit";
    private static final String PROP_VERSION_PUBCONFIG_PUBLISHER = "docversion.pubconfig.publisher";
    private static final String PROP_VERSION_PUBCONFIG_BIBLIOID = "docversion.pubconfig.biblioid";

    private boolean draft = false;  // not persisted

    private String id;
    private String contentRoot = "";
    private String prefaceRoot = "";
    private String appendixRoot = "";
    private String cover = "";
    private String filterSetting = "";
    private String referencedPubs = "";
    private String customTitlePage1 = "";
    private String customTitlePage2 = "";
    private String title = "";
    private String subtitle = "";
    private String releaseInfo = "";
    private String corporate = "";
    private String authors = "";
    private String pubDate = "";
    private String copyrightYear = "";
    private String copyrightHolder = "";
    private String pubAbstract = "";
    private String legalNotice = "";
    private String credit = DocmaConstants.DISPLAY_APP_CREDIT;
    private String publisher = "";
    private String biblioID = "";

    /* -----------  Public constructor  ------------------ */

    public DocmaPublicationConfig(String pubConfigId)
    {
        id = pubConfigId;
    }

    /* -----------  Package local  ------------------ */

    static String[] getIds(DocmaSession docmaSess)
    {
        String sid = docmaSess.getStoreId();
        DocVersionId vid = docmaSess.getVersionId();
        String pub_ids = docmaSess.getVersionProperty(sid, vid, PROP_VERSION_PUBCONFIG_IDS);
        String[] id_arr;
        if ((pub_ids == null) || (pub_ids.trim().length() == 0)) {
            id_arr = new String[0];
        } else {
            id_arr = pub_ids.split(",");
        }
        return id_arr;
    }

    static boolean createId(DocmaSession docmaSess, String pubConfId) throws DocException
    {
        String sid = docmaSess.getStoreId();
        DocVersionId vid = docmaSess.getVersionId();
        List id_list = Arrays.asList(getIds(docmaSess));
        if (! id_list.contains(pubConfId)) {
            // add new publication configuration to list
            List ids_new = new ArrayList(id_list);
            ids_new.add(pubConfId);
            Collections.sort(ids_new);
            String pub_ids = DocmaUtil.concatStrings(ids_new, ",");
            docmaSess.setVersionProperty(sid, vid, PROP_VERSION_PUBCONFIG_IDS, pub_ids);
            return true;
        } else {
            return false;
        }
    }

    void init(DocmaSession docmaSess, String publicationId)
    {
        id = publicationId;
        String sid = docmaSess.getStoreId();
        DocVersionId vid = docmaSess.getVersionId();
        contentRoot = docmaSess.getVersionProperty(sid, vid, PROP_VERSION_PUBCONFIG_ROOT + "." + id);
        prefaceRoot = docmaSess.getVersionProperty(sid, vid, PROP_VERSION_PUBCONFIG_PREFACE + "." + id);
        appendixRoot = docmaSess.getVersionProperty(sid, vid, PROP_VERSION_PUBCONFIG_APPENDIX + "." + id);
        cover = docmaSess.getVersionProperty(sid, vid, PROP_VERSION_PUBCONFIG_COVER + "." + id);
        filterSetting = docmaSess.getVersionProperty(sid, vid, PROP_VERSION_PUBCONFIG_FILTER + "." + id);
        referencedPubs = docmaSess.getVersionProperty(sid, vid, PROP_VERSION_PUBCONFIG_REFERENCED_PUBS + "." + id);
        customTitlePage1 = docmaSess.getVersionProperty(sid, vid, PROP_VERSION_PUBCONFIG_CUSTOM_TITLE1 + "." + id);
        customTitlePage2 = docmaSess.getVersionProperty(sid, vid, PROP_VERSION_PUBCONFIG_CUSTOM_TITLE2 + "." + id);
        title = docmaSess.getVersionProperty(sid, vid, PROP_VERSION_PUBCONFIG_TITLE + "." + id);
        subtitle = docmaSess.getVersionProperty(sid, vid, PROP_VERSION_PUBCONFIG_SUBTITLE + "." + id);
        releaseInfo = docmaSess.getVersionProperty(sid, vid, PROP_VERSION_PUBCONFIG_RELEASEINFO + "." + id);
        corporate = docmaSess.getVersionProperty(sid, vid, PROP_VERSION_PUBCONFIG_CORPORATE + "." + id);
        authors = docmaSess.getVersionProperty(sid, vid, PROP_VERSION_PUBCONFIG_AUTHORS + "." + id);
        pubDate = docmaSess.getVersionProperty(sid, vid, PROP_VERSION_PUBCONFIG_PUB_DATE + "." + id);
        copyrightYear = docmaSess.getVersionProperty(sid, vid, PROP_VERSION_PUBCONFIG_COPYRIGHT_YEAR + "." + id);
        copyrightHolder = docmaSess.getVersionProperty(sid, vid, PROP_VERSION_PUBCONFIG_COPYRIGHT_HOLDER + "." + id);
        pubAbstract = docmaSess.getVersionProperty(sid, vid, PROP_VERSION_PUBCONFIG_ABSTRACT + "." + id);
        legalNotice = docmaSess.getVersionProperty(sid, vid, PROP_VERSION_PUBCONFIG_LEGALNOTICE + "." + id);
        credit = docmaSess.getVersionProperty(sid, vid, PROP_VERSION_PUBCONFIG_CREDIT + "." + id);
        publisher = docmaSess.getVersionProperty(sid, vid, PROP_VERSION_PUBCONFIG_PUBLISHER + "." + id);
        biblioID = docmaSess.getVersionProperty(sid, vid, PROP_VERSION_PUBCONFIG_BIBLIOID + "." + id);
    }

    void save(DocmaSession docmaSess) throws DocException
    {
        String sid = docmaSess.getStoreId();
        DocVersionId vid = docmaSess.getVersionId();
        createId(docmaSess, this.id);   // create new configuration if not already existent
        String[] prop_names = {
            PROP_VERSION_PUBCONFIG_ROOT + "." + this.id,
            PROP_VERSION_PUBCONFIG_PREFACE + "." + this.id,
            PROP_VERSION_PUBCONFIG_APPENDIX + "." + this.id,
            PROP_VERSION_PUBCONFIG_COVER + "." + this.id,
            PROP_VERSION_PUBCONFIG_FILTER + "." + this.id,
            PROP_VERSION_PUBCONFIG_REFERENCED_PUBS + "." + this.id,
            PROP_VERSION_PUBCONFIG_CUSTOM_TITLE1 + "." + this.id,
            PROP_VERSION_PUBCONFIG_CUSTOM_TITLE2 + "." + this.id,
            PROP_VERSION_PUBCONFIG_TITLE + "." + this.id,
            PROP_VERSION_PUBCONFIG_SUBTITLE + "." + this.id,
            PROP_VERSION_PUBCONFIG_RELEASEINFO + "." + this.id,
            PROP_VERSION_PUBCONFIG_CORPORATE + "." + this.id,
            PROP_VERSION_PUBCONFIG_AUTHORS + "." + this.id,
            PROP_VERSION_PUBCONFIG_PUB_DATE + "." + this.id,
            PROP_VERSION_PUBCONFIG_COPYRIGHT_YEAR + "." + this.id,
            PROP_VERSION_PUBCONFIG_COPYRIGHT_HOLDER + "." + this.id,
            PROP_VERSION_PUBCONFIG_ABSTRACT + "." + this.id,
            PROP_VERSION_PUBCONFIG_LEGALNOTICE + "." + this.id,
            PROP_VERSION_PUBCONFIG_CREDIT + "." + this.id,
            PROP_VERSION_PUBCONFIG_PUBLISHER + "." + this.id,
            PROP_VERSION_PUBCONFIG_BIBLIOID + "." + this.id
        };
        String[] prop_values = {
            contentRoot,
            prefaceRoot,
            appendixRoot,
            cover,
            filterSetting,
            referencedPubs,
            customTitlePage1,
            customTitlePage2,
            title,
            subtitle,
            releaseInfo,
            corporate,
            authors,
            pubDate,
            copyrightYear,
            copyrightHolder,
            pubAbstract,
            legalNotice,
            credit,
            publisher,
            biblioID
        };
        docmaSess.setVersionProperties(sid, vid, prop_names, prop_values);
    }

    void delete(DocmaSession docmaSess) throws DocException
    {
        String sid = docmaSess.getStoreId();
        DocVersionId vid = docmaSess.getVersionId();
        String pub_ids = docmaSess.getVersionProperty(sid, vid, PROP_VERSION_PUBCONFIG_IDS);
        if ((pub_ids == null) || (pub_ids.trim().length() == 0)) {
            return;
        }
        String[] id_arr = pub_ids.split(",");
        List id_list = new ArrayList(Arrays.asList(id_arr));
        id_list.remove(this.id);
        pub_ids = DocmaUtil.concatStrings(id_list, ",");
        String[] prop_names = {
            PROP_VERSION_PUBCONFIG_IDS,
            PROP_VERSION_PUBCONFIG_ROOT + "." + this.id,
            PROP_VERSION_PUBCONFIG_PREFACE + "." + this.id,
            PROP_VERSION_PUBCONFIG_APPENDIX + "." + this.id,
            PROP_VERSION_PUBCONFIG_COVER + "." + this.id,
            PROP_VERSION_PUBCONFIG_FILTER + "." + this.id,
            PROP_VERSION_PUBCONFIG_REFERENCED_PUBS + "." + this.id,
            PROP_VERSION_PUBCONFIG_CUSTOM_TITLE1 + "." + this.id,
            PROP_VERSION_PUBCONFIG_CUSTOM_TITLE2 + "." + this.id,
            PROP_VERSION_PUBCONFIG_TITLE + "." + this.id,
            PROP_VERSION_PUBCONFIG_SUBTITLE + "." + this.id,
            PROP_VERSION_PUBCONFIG_RELEASEINFO + "." + this.id,
            PROP_VERSION_PUBCONFIG_CORPORATE + "." + this.id,
            PROP_VERSION_PUBCONFIG_AUTHORS + "." + this.id,
            PROP_VERSION_PUBCONFIG_PUB_DATE + "." + this.id,
            PROP_VERSION_PUBCONFIG_COPYRIGHT_YEAR + "." + this.id,
            PROP_VERSION_PUBCONFIG_COPYRIGHT_HOLDER + "." + this.id,
            PROP_VERSION_PUBCONFIG_ABSTRACT + "." + this.id,
            PROP_VERSION_PUBCONFIG_LEGALNOTICE + "." + this.id,
            PROP_VERSION_PUBCONFIG_CREDIT + "." + this.id,
            PROP_VERSION_PUBCONFIG_PUBLISHER + "." + this.id,
            PROP_VERSION_PUBCONFIG_BIBLIOID + "." + this.id
        };
        String[] prop_values = { pub_ids, 
                                 null, null, null, null, null,
                                 null, null, null, null, null,
                                 null, null, null, null, null,
                                 null, null, null, null, null, 
                                 null };
        docmaSess.setVersionProperties(sid, vid, prop_names, prop_values);
    }


    /* -----------  Public methods  ------------------ */

    public boolean isDraft()
    {
        return draft;
    }

    /**
     * Set draft mode indication for formatter. This field is not persisted.
     * @param draft
     */
    public void setDraft(boolean draft)
    {
        this.draft = draft;
    }


    public String getContentRoot() 
    {
        return fixNodeId(contentRoot);
    }

    public void setContentRoot(String contentRoot) 
    {
        this.contentRoot = contentRoot;
    }

    public String getFilterSetting() 
    {
        return filterSetting;
    }

    public String[] getFilterSettingApplics() 
    {
        if ((filterSetting == null) || (filterSetting.trim().equals(""))) {
            return new String[0];
        }
        String[] arr = filterSetting.split("[ \\t,]+");
        // for (int i=0; i < arr.length; i++) {
        //     arr[i] = arr[i].trim();
        // }
        return arr;
    }

    public void setFilterSetting(String filterSetting) 
    {
        this.filterSetting = filterSetting;
    }

    public String getReferencedPubs() 
    {
        return referencedPubs;
    }

    public String[] getReferencedPubIds() 
    {
        if ((referencedPubs == null) || (referencedPubs.trim().equals(""))) {
            return new String[0];
        }
        String[] arr = referencedPubs.split("[ \\t,]+");
        // for (int i=0; i < arr.length; i++) {
        //     arr[i] = arr[i].trim();
        // }
        return arr;
    }

    public void setReferencedPubs(String id_list) 
    {
        this.referencedPubs = id_list;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAppendixRoot() {
        return fixNodeId(appendixRoot);
    }

    public void setAppendixRoot(String appendixRoot) {
        this.appendixRoot = appendixRoot;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getCopyrightHolder() {
        return copyrightHolder;
    }

    public void setCopyrightHolder(String copyrightHolder) {
        this.copyrightHolder = copyrightHolder;
    }

    public String getCopyrightYear() {
        return copyrightYear;
    }

    public void setCopyrightYear(String copyrightYear) {
        this.copyrightYear = copyrightYear;
    }

    public String getCorporate() {
        return corporate;
    }

    public void setCorporate(String corporate) {
        this.corporate = corporate;
    }
    
    public String getCoverImageAlias() {
        return cover;
    }

    public void setCoverImageAlias(String cover) {
        this.cover = cover;
    }

    public String getCredit() {
        return credit;
    }

    public void setCredit(String credit) {
        this.credit = credit;
    }

    public String getCustomTitlePage1() {
        return customTitlePage1;
    }

    public void setCustomTitlePage1(String customTitlePage1) {
        this.customTitlePage1 = customTitlePage1;
    }

    public String getCustomTitlePage2() {
        return customTitlePage2;
    }

    public void setCustomTitlePage2(String customTitlePage2) {
        this.customTitlePage2 = customTitlePage2;
    }

    public String getLegalNotice() {
        return legalNotice;
    }

    public void setLegalNotice(String legalNotice) {
        this.legalNotice = legalNotice;
    }

    public String getPrefaceRoot() {
        return fixNodeId(prefaceRoot);
    }

    public void setPrefaceRoot(String prefaceRoot) {
        this.prefaceRoot = prefaceRoot;
    }

    public String getPubAbstract() {
        return pubAbstract;
    }

    public void setPubAbstract(String pubAbstract) {
        this.pubAbstract = pubAbstract;
    }

    public String getPubDate() {
        return pubDate;
    }

    public void setPubDate(String pubDate) {
        this.pubDate = pubDate;
    }

    public String getReleaseInfo() {
        return releaseInfo;
    }

    public void setReleaseInfo(String releaseInfo) {
        this.releaseInfo = releaseInfo;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getBiblioId() {
        return biblioID;
    }

    public void setBiblioId(String bibid) {
        this.biblioID = bibid;
    }

    /* -----------  Private methods  ------------------ */

    private String fixNodeId(String node_id)
    {
        if ((node_id != null) && node_id.startsWith("d")) {
            // Transform Docmenta 1.0 node-ids to Docmenta 1.1 node-ids
            try {
                long nodenum = Long.parseLong(node_id.substring(1));
                node_id = Long.toString(nodenum);
            } catch (Exception ex) {}
        }
        return node_id;
    }
}
