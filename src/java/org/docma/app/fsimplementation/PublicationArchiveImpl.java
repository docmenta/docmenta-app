/*
 * PublicationArchiveImpl.java
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

package org.docma.app.fsimplementation;

import java.io.*;
import java.util.*;
import org.docma.coreapi.*;
import org.docma.util.*;

/**
 *
 * @author MP
 */
public class PublicationArchiveImpl implements PublicationArchive
{
    private final static String PUB_PREFIX = "publication";
    private final static String PUB_PROP_FILENAME = "publication.properties";

    private String docStoreId;
    private DocVersionId versionId;
    private File baseDir;
    
    private Map attributesMap = new HashMap();
    private Map outStreamMap = new HashMap();
    // private Map logMap = new HashMap();

    public PublicationArchiveImpl(String docStoreId, DocVersionId versionId, File baseDir)
    {
        this.docStoreId = docStoreId;
        this.versionId = versionId;
        this.baseDir = baseDir;
    }

    /* --------------  Package local methods  --------------- */

    void prepareAccess()
    {
        if (! baseDir.exists()) {
            if (! baseDir.mkdirs()) {
                throw new DocRuntimeException("Could not create Publication archive directory: " +
                                              baseDir.getAbsolutePath());
            }
        }
        // if (! baseDir.exists()) {
        //     throw new DocRuntimeException("Publication archive directory does not exist: " +
        //             baseDir.getAbsolutePath());
        // }
    }

    /* --------------  Public methods  --------------- */
    
    public File getBaseDir()
    {
        prepareAccess();
        return baseDir;
    }

    /* --------------  Interface methods  --------------- */

    public String getDocStoreId()
    {
        return docStoreId;
    }

    public DocVersionId getVersionId()
    {
        return versionId;
    }

    public synchronized String createPublication(String language, String filename)
    {
        String nextid = getNextPublicationId();
        File dir = new File(getBaseDir(), nextid);
        create_publication(dir, nextid, language, filename);
        return nextid;
    }
    
    public String createPublication(String publicationId, String language, String filename)
    {
        File dir = new File(getBaseDir(), publicationId);
        if (dir.exists()) {   // publicationId is already used for another publication
            return createPublication(language, filename);  // use auto-generated id
        } else {
            create_publication(dir, publicationId, language, filename);
            return publicationId;
        }
    }
    
    private void create_publication(File dir, String publicationId, String language, String filename)
    {
        if (dir.mkdir()) {
            String now_millis = Long.toString(System.currentTimeMillis());
            setAttributes(publicationId,
                new String[] {ATTRIBUTE_PUBLICATION_FILENAME, 
                              ATTRIBUTE_PUBLICATION_LANGUAGE,
                              ATTRIBUTE_PUBLICATION_CREATION_TIME},
                new String[] {filename, language, now_millis});
        } else {
            throw new DocRuntimeException("Could not create publication directory: " +
                    dir.getAbsolutePath());
        }
    }

    public void deletePublication(String publicationId)
    {
        File f = new File(baseDir, publicationId);
        if (f.exists()) {
            // DocmaUtil.recursiveFileDelete(f);
            File[] files = f.listFiles();
            for (int i=0; i < files.length; i++) {
                files[i].delete();
            }
            if (! f.delete()) {
                throw new DocRuntimeException("Could not delete publication directory: " +
                        f.getAbsolutePath());
            }
        }
    }

    public String[] listPublications()
    {
        String[] dirs = baseDir.exists() ? baseDir.list() : new String[0];
        List list = new ArrayList(dirs.length);
        for (int i=0; i < dirs.length; i++) {
            String fn = dirs[i];
            if (fn.startsWith(PUB_PREFIX)) {
                list.add(fn);
            }
        }
        String[] ids = new String[list.size()];
        return (String[]) list.toArray(ids);
    }

    public String getAttribute(String publicationId, String attName)
    {
        PropertiesLoader propLoader = getPropLoader(publicationId);
        return propLoader.getProp(attName);
    }
    
    public String[] getAttributeNames(String publicationId)
    {
        PropertiesLoader propLoader = getPropLoader(publicationId);
        return propLoader.getPropNames();
    }

    public void setAttribute(String publicationId, String attName, String attValue)
    {
        if (attName.equals(PublicationArchive.ATTRIBUTE_PUBLICATION_FILENAME)) {
            changeFilename(publicationId, attValue);
        }
        PropertiesLoader propLoader = getPropLoader(publicationId);
        propLoader.setProp(attName, attValue);
        try {
            propLoader.savePropFile();
        } catch (Exception ex) {
            throw new DocRuntimeException(ex);
        }
    }

    public void setAttributes(String publicationId, String[] attNames, String[] attValues)
    {
        PropertiesLoader propLoader = getPropLoader(publicationId);
        for (int i=0; i < attNames.length; i++) {
            String attName = attNames[i];
            if (attName.equals(PublicationArchive.ATTRIBUTE_PUBLICATION_FILENAME)) {
                changeFilename(publicationId, attValues[i]);
            }
            propLoader.setProp(attName, attValues[i]);
        }
        try {
            propLoader.savePropFile();
        } catch (Exception ex) {
            throw new DocRuntimeException(ex);
        }
    }

    public InputStream readPublicationStream(String publicationId)
    {
        File pubDir = new File(baseDir, publicationId);
        String filename = getAttribute(publicationId, ATTRIBUTE_PUBLICATION_FILENAME);
        File pubFile = new File(pubDir, filename);
        if (! pubFile.exists()) return null;

        try {
            return new FileInputStream(pubFile);
        } catch (Exception ex) {
            throw new DocRuntimeException(ex);
        }
    }

    public OutputStream openPublicationOutputStream(String publicationId)
    {
        String filename = getAttribute(publicationId, ATTRIBUTE_PUBLICATION_FILENAME);
        if ((filename == null) || filename.trim().equals("")) {
            throw new DocRuntimeException("Publication filename is null or empty string.");
        }
        File pubDir = new File(baseDir, publicationId);
        File pubFile = new File(pubDir, filename);
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(pubFile);
            outStreamMap.put(publicationId, fout);
            return fout;
        } catch (IOException ex) {
            try {
                if (fout != null) fout.close();
            } catch (Exception ex2) {}
            throw new DocRuntimeException(ex);
        }
    }

    public void closePublicationOutputStream(String publicationId)
    {
        FileOutputStream fout = (FileOutputStream) outStreamMap.get(publicationId);
        try {
            fout.close();
        } catch (IOException ex) {
            throw new DocRuntimeException(ex);
        }
        String now_millis = Long.toString(System.currentTimeMillis());
        setAttribute(publicationId, ATTRIBUTE_PUBLICATION_CLOSING_TIME, now_millis);
    }

    public boolean hasPublicationStream(String publicationId)
    {
        String closing_time = getAttribute(publicationId, ATTRIBUTE_PUBLICATION_CLOSING_TIME);
        return (closing_time != null) && !closing_time.equals("");
    }

    public long getPublicationSize(String publicationId)
    {
        File pubDir = new File(baseDir, publicationId);
        String filename = getAttribute(publicationId, ATTRIBUTE_PUBLICATION_FILENAME);
        File pubFile = new File(pubDir, filename);
        if (! pubFile.exists()) return 0;
        else return pubFile.length();
    }

//    public void setPublicationStream(String publicationId,
//                                     InputStream pubStream,
//                                     // String filename,
//                                     DocmaExportLog log)
//    {
//        // if (filename == null) {
//        String filename = getAttribute(publicationId, ATTRIBUTE_PUBLICATION_FILENAME);
//        // } else {
//        //     setAttribute(publicationId, ATTRIBUTE_PUBLICATION_FILENAME, filename);
//        // }
//        if ((filename == null) || filename.trim().equals("")) {
//            throw new DocRuntimeException("Publication filename is null or empty string.");
//        }
//        File pubDir = new File(baseDir, publicationId);
//        File pubFile = new File(pubDir, filename);
//        FileOutputStream fout = null;
//        try {
//            fout = new FileOutputStream(pubFile);
//            DocmaUtil.copyStream(pubStream, fout);
//            fout.close();
//            pubStream.close();
//        } catch (IOException ex) {
//            try {
//                if (fout != null) fout.close();
//            } catch (Exception ex2) {}
//            throw new DocRuntimeException(ex);
//        }
//        if (log != null) {
//            setExportLog(publicationId, log);
//        }
//    }

    public DocmaExportLog readExportLog(String publicationId)
    {
        File pubDir = new File(baseDir, publicationId);
        File logFile = new File(pubDir, getExportLogFilename(publicationId));
        if (! logFile.exists()) return null;

        try {
            FileInputStream fin = new FileInputStream(logFile);
            return DocmaExportLog.loadFromXML(fin);
        } catch (Exception ex) {
            throw new DocRuntimeException(ex);
        }
    }

    public void writeExportLog(String publicationId, DocmaExportLog log)
    {
        File pubDir = new File(getBaseDir(), publicationId);
        File logFile = new File(pubDir, getExportLogFilename(publicationId));
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(logFile);
            BufferedOutputStream bufout = new BufferedOutputStream(fout);
            log.storeToXML(bufout);
            bufout.close();
            fout.close();
        } catch (IOException ex) {
            try {
                if (fout != null) fout.close();
            } catch (Exception ex2) {}
            throw new DocRuntimeException(ex);
        }
    }

    public boolean hasExportLog(String publicationId)
    {
        File pubDir = new File(baseDir, publicationId);
        File logFile = new File(pubDir, getExportLogFilename(publicationId));
        return logFile.exists();
    }


    public synchronized void refresh(String publicationId)
    {
        attributesMap.remove(publicationId);
    }
    
    public synchronized void invalidateCache()
    {
        attributesMap.clear();
    }


    /* --------------  Private methods  --------------- */

    private void changeFilename(String publicationId, String new_filename)
    {
        String filename = getAttribute(publicationId, ATTRIBUTE_PUBLICATION_FILENAME);
        if ((filename == null) || filename.trim().equals("")) {
            return;  // filename is set the first time; do nothing
        }
        File pubDir = new File(baseDir, publicationId);
        File oldFile = new File(pubDir, filename);
        if (oldFile.exists()) {
            File newFile = new File(pubDir, new_filename);
            oldFile.renameTo(newFile);
        }
    }

    private String getExportLogFilename(String publicationId)
    {
        return publicationId + "_log.xml";
    }

    private synchronized PropertiesLoader getPropLoader(String publicationId)
    {
        PropertiesLoader propLoader = (PropertiesLoader) attributesMap.get(publicationId);
        if (propLoader == null) {
            try {
                File pubDir = new File(baseDir, publicationId);
                File propFile = new File(pubDir, PUB_PROP_FILENAME);
                if (! propFile.exists()) {
                    propFile.createNewFile();
                }
                propLoader = new PropertiesLoader(propFile);
                attributesMap.put(publicationId, propLoader);
            } catch(Exception ex) {
                throw new DocRuntimeException(ex);
            }
        }
        return propLoader;
    }

    private String getNextPublicationId()
    {
        int max = -1;
        String[] dirs = getBaseDir().list();
        for (int i=0; i < dirs.length; i++) {
            String fn = dirs[i];
            if (fn.startsWith(PUB_PREFIX)) {
                String seq_str = fn.substring(PUB_PREFIX.length());
                try {
                    int num = Integer.parseInt(seq_str);
                    if (num > max) max = num;
                } catch (Exception ex) {}
            }
        }
        int next_num = (max == -1) ? 1 : (max + 1);
        return PUB_PREFIX + next_num;
    }
}
