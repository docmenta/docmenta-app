/*
 * DocmaPublication.java
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

import org.docma.coreapi.*;
import org.docma.util.Log;
import org.docma.util.DocmaUtil;
import org.docma.util.ZipUtil;

/**
 *
 * @author MP
 */
public class DocmaPublication implements Comparable
{
    private final static String ATTRIBUTE_PUBLICATION_TITLE = "publication.title";
    private final static String ATTRIBUTE_PUBLICATION_STATE = "publication.state";
    private final static String ATTRIBUTE_PUBLICATION_CONFIG = "publication.config";
    private final static String ATTRIBUTE_PUBLICATION_OUTPUT_CONFIG = "publication.output.config";
    private final static String ATTRIBUTE_PUBLICATION_FORMAT = "publication.format";
    private final static String ATTRIBUTE_PUBLICATION_SUBFORMAT = "publication.subformat";
    private final static String ATTRIBUTE_PUBLICATION_RELEASE_TIME = "publication.release.time";
    private final static String ATTRIBUTE_PUBLICATION_EXPORTED_BY_USER = "publication.exported.by.user";
    private final static String ATTRIBUTE_PUBLICATION_COMMENT = "publication.comment";
    private final static String ATTRIBUTE_PUBLICATION_IS_ONLINE = "publication.is.online";
    private final static String ATTRIBUTE_PUBLICATION_ERROR_COUNT = "publication.error.count";
    private final static String ATTRIBUTE_PUBLICATION_WARNING_COUNT = "publication.warning.count";

    private DocmaSession docmaSess;
    private PublicationArchive archive;
    private String publicationId;

    private String exportProgressMsg = "";
    private DocmaExportLog exportLog = null;

    DocmaPublication(DocmaSession docmaSess, PublicationArchive archive, String publicationId)
    {
        this.docmaSess = docmaSess;
        this.archive = archive;
        this.publicationId = publicationId;
    }

    void setPublicationMetadata(String title,
                                String publicationState,
                                String publicationConfigId,
                                String outputConfigId,
                                String format,
                                String subformat,
                                Date releaseDate,
                                String exportedByUser,
                                String comment)
    {
        String[] attNames = {
            ATTRIBUTE_PUBLICATION_TITLE,
            ATTRIBUTE_PUBLICATION_STATE,
            ATTRIBUTE_PUBLICATION_CONFIG,
            ATTRIBUTE_PUBLICATION_OUTPUT_CONFIG,
            ATTRIBUTE_PUBLICATION_FORMAT,
            ATTRIBUTE_PUBLICATION_SUBFORMAT,
            ATTRIBUTE_PUBLICATION_RELEASE_TIME,
            ATTRIBUTE_PUBLICATION_EXPORTED_BY_USER,
            ATTRIBUTE_PUBLICATION_COMMENT
        };
        String rel_str = (releaseDate == null) ? "" : Long.toString(releaseDate.getTime());
        if (subformat == null) {  // subformat is optional
            subformat = "";
        }
        String[] attValues = {
            title, publicationState, publicationConfigId, outputConfigId, format,
            subformat, rel_str, exportedByUser, comment
        };
        archive.setAttributes(publicationId, attNames, attValues);
    }

    void setLogCounters(int error_count, int warning_count)
    {
        String[] attNames = {
            ATTRIBUTE_PUBLICATION_ERROR_COUNT,
            ATTRIBUTE_PUBLICATION_WARNING_COUNT
        };
        String[] attValues = { "" + error_count, "" + warning_count };
        archive.setAttributes(publicationId, attNames, attValues);
    }

    public String getId()
    {
        return publicationId;
    }

    public String getFilename()
    {
        return archive.getAttribute(publicationId, PublicationArchive.ATTRIBUTE_PUBLICATION_FILENAME);
    }

    public void setFilename(String fn)
    {
        archive.setAttribute(publicationId, PublicationArchive.ATTRIBUTE_PUBLICATION_FILENAME, fn);
    }

    public String getLanguage()
    {
        return archive.getAttribute(publicationId, PublicationArchive.ATTRIBUTE_PUBLICATION_LANGUAGE);
    }

    public String getTitle()
    {
        return archive.getAttribute(publicationId, ATTRIBUTE_PUBLICATION_TITLE);
    }

    public String getFormat()
    {
        return archive.getAttribute(publicationId, ATTRIBUTE_PUBLICATION_FORMAT);
    }

    public String getSubformat()
    {
        return archive.getAttribute(publicationId, ATTRIBUTE_PUBLICATION_SUBFORMAT);
    }

    public String getPublicationState()
    {
        return archive.getAttribute(publicationId, ATTRIBUTE_PUBLICATION_STATE);
    }

    public String getPublicationStateLabel()
    {
        String pub_state = getPublicationState();
        if ((pub_state == null) || pub_state.equals("")) {
            return "-";
        }
        DocI18n i18 = docmaSess.getI18n();
        return i18.getLabel("label.publicationstatus." + pub_state.toLowerCase());
    }

    public String getPublicationConfigId()
    {
        return archive.getAttribute(publicationId, ATTRIBUTE_PUBLICATION_CONFIG);
    }

    public String getOutputConfigId()
    {
        return archive.getAttribute(publicationId, ATTRIBUTE_PUBLICATION_OUTPUT_CONFIG);
    }

    public DocVersionId getVersionId()
    {
        return archive.getVersionId();
    }

    public Date getReleaseDate()
    {
        String time_str = archive.getAttribute(publicationId, ATTRIBUTE_PUBLICATION_RELEASE_TIME);
        if ((time_str == null) || time_str.equals("")) return null;
        long millis;
        try {
            millis = Long.parseLong(time_str);
        } catch (Exception ex) {
            Log.error("Invalid publication release time: " + time_str);
            return null;
        }
        return new Date(millis);
    }

    public Date getExportDate()
    {
        String time_str = archive.getAttribute(publicationId, PublicationArchive.ATTRIBUTE_PUBLICATION_CREATION_TIME);
        if ((time_str == null) || time_str.equals("")) return null;
        long millis;
        try {
            millis = Long.parseLong(time_str);
        } catch (Exception ex) {
            Log.error("Invalid publication release time: " + time_str);
            return null;
        }
        return new Date(millis);
    }

    public String getExportedByUser()
    {
        return archive.getAttribute(publicationId, ATTRIBUTE_PUBLICATION_EXPORTED_BY_USER);
    }

    public String getComment()
    {
        return archive.getAttribute(publicationId, ATTRIBUTE_PUBLICATION_COMMENT);
    }

    public long getContentSize()
    {
        return archive.getPublicationSize(publicationId);
    }

    public InputStream getContentStream()
    {
        return archive.readPublicationStream(publicationId);
    }

    public DocmaExportLog getExportLog()
    {
        if (exportLog == null) {
            exportLog = archive.readExportLog(publicationId);
        }
        return exportLog;
    }

    public boolean isExportFinished()
    {
        // return archive.hasPublicationStream(publicationId) &&
        //        archive.hasExportLog(publicationId);
        return PublicationManager.isExportFinished(archive.getDocStoreId(),
                                                   archive.getVersionId(),
                                                   publicationId);
    }

    public String getExportProgressMessage()
    {
        return exportProgressMsg;
    }

    void setExportProgressMessage(String msg)
    {
        exportProgressMsg = msg;
    }

    public int getErrorCount()
    {
        String str = archive.getAttribute(publicationId, ATTRIBUTE_PUBLICATION_ERROR_COUNT);
        if ((str == null) || str.equals("")) {
            return 0;
        }
        try {
            return Integer.parseInt(str);
        } catch (Exception ex) {
            throw new DocRuntimeException(ex);
        }
    }

    public int getWarningCount()
    {
        String str = archive.getAttribute(publicationId, ATTRIBUTE_PUBLICATION_WARNING_COUNT);
        if ((str == null) || str.equals("")) {
            return 0;
        }
        try {
            return Integer.parseInt(str);
        } catch (Exception ex) {
            throw new DocRuntimeException(ex);
        }
    }

    public boolean hasError()
    {
        // getExportLog();
        // return (exportLog != null) && exportLog.hasError();
        return (getErrorCount() > 0);
    }

    public boolean hasWarning()
    {
        // getExportLog();
        // return (exportLog != null) && exportLog.hasWarning();
        return (getWarningCount() > 0);
    }

    public boolean isOnline()
    {
        String str = archive.getAttribute(publicationId, ATTRIBUTE_PUBLICATION_IS_ONLINE);
        if ((str == null) || str.equals("")) return false;
        return Boolean.parseBoolean(str);
    }

    public void setOnline(boolean is_online)
    {
        if (is_online == isOnline()) return; // do nothing

        String onlinePath = docmaSess.getApplicationProperty(DocmaConstants.PROP_PUBLICATION_ONLINE_PATH);
        File onlineDir = new File(onlinePath);
        File onlineStoreDir = new File(onlineDir, archive.getDocStoreId());
        File onlineVersionDir = new File(onlineStoreDir, archive.getVersionId().toString());
        File onlinePublicationDir = new File(onlineVersionDir, getId());

        String fname = getFilename();
        boolean is_zip = false;
        int pos = fname.lastIndexOf('.');
        if (pos > 0) {
            String ext = fname.substring(pos);
            is_zip = ext.equalsIgnoreCase(".zip");
        }

        if (is_online) {  // Write publication to online directory

            // Delete directory if it already exists (maybe an old export)
            if (onlinePublicationDir.exists()) {
                DocmaUtil.recursiveFileDelete(onlinePublicationDir);
            }

            // Create directory
            if (! onlinePublicationDir.exists()) {
                if (! onlinePublicationDir.mkdirs()) {
                    throw new DocRuntimeException("Cannot set publication online. Unable to create directory: " +
                                                  onlinePublicationDir.getAbsolutePath());
                }
            }

            // Write publication to directory
            InputStream pub_stream = getContentStream();
            try {
                if (is_zip) {
                    ZipUtil.extractZipStream(pub_stream, onlinePublicationDir);
                } else {
                    File out_file = new File(onlinePublicationDir, fname);
                    FileOutputStream pub_out = new FileOutputStream(out_file);
                    DocmaUtil.copyStream(pub_stream, pub_out);
                    pub_out.close();
                }
            } catch (Exception ex) {
                throw new DocRuntimeException(ex);
            } finally {
                try {
                    pub_stream.close();
                } catch (Exception ex) {}
            }
        } else {   // Remove publication from online directory

            if (onlinePublicationDir.exists()) {
                DocmaUtil.recursiveFileDelete(onlinePublicationDir);
            }
        }
        archive.setAttribute(publicationId, ATTRIBUTE_PUBLICATION_IS_ONLINE, "" + is_online);
    }

    public File getOnlineDirectory()
    {
        return null; // not implemented yet
    }

    public boolean verifyOnlineConsistency()
    {
        return true; // not implemented yet
    }

    public void refresh()
    {
        archive.refresh(publicationId);
    }

    public int compareTo(Object obj)
    {
        if (obj instanceof DocmaPublication) {
            DocmaPublication otherPub = (DocmaPublication) obj;
            String self_id = this.getId();
            if (self_id == null) return 1;  // Should not occur. Sort to the end of a list.
            if (self_id.equals(otherPub.getId())) return 0;

            // sort by versionId, publication_config_id, output_config_id, export_date
            DocVersionId self_ver = this.getVersionId();
            if (self_ver == null) return 1;  // Should not occur. Sort to the end of a list.
            int val = self_ver.compareTo(otherPub.getVersionId());
            if (val != 0) return val;

            String self_pubconf = this.getPublicationConfigId();
            if (self_pubconf == null) return 1;  // Should not occur. Sort to the end of a list.
            val = self_pubconf.compareTo(otherPub.getPublicationConfigId());
            if (val != 0) return val;

            String self_outconf = this.getOutputConfigId();
            if (self_outconf == null) return 1;  // Should not occur. Sort to the end of a list.
            val = self_outconf.compareTo(otherPub.getOutputConfigId());
            if (val != 0) return val;

            Date self_exp = this.getExportDate();
            if (self_exp == null) return 1;  // Should not occur. Sort to the end of a list.
            val = self_exp.compareTo(otherPub.getExportDate());
            return val;
        } else {
            throw new DocRuntimeException("Cannot compare DocmaPublication with " + obj.getClass().getName());
        }
    }

}
