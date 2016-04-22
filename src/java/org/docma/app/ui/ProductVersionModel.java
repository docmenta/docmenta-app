/*
 * ProductVersionModel.java
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

package org.docma.app.ui;

import org.docma.app.*;
import org.docma.coreapi.*;
import java.util.*;

/**
 *
 * @author MP
 */
public class ProductVersionModel implements Comparable
{
    private String       productId         = null;
    private DocVersionId versionId         = null;
    private String       language          = null;
    private String       state             = null;
    private Date         creationDate      = null;
    private Date         lastModifiedDate  = null;
    private Date         releaseDate       = null;
    private DocVersionId derivedFrom       = null;
    private String       comment           = null;

    public ProductVersionModel()
    {
    }

    public ProductVersionModel(DocmaSession docmaSess, String productId, DocVersionId verId)
    {
        initFields(docmaSess, productId, verId);
    }
    
    public void refresh(DocmaSession docmaSess)
    {
        initFields(docmaSess, productId, versionId);
    }

    void initFields(DocmaSession docmaSess, String productId, DocVersionId verId)
    {
        this.productId = productId;
        this.versionId = verId;
        this.language = docmaSess.getTranslationMode();
        if (this.language == null) {
            this.language = docmaSess.getOriginalLanguage().getCode();
        }
        this.state = docmaSess.getVersionState(productId, verId);
        this.creationDate = docmaSess.getVersionCreationDate(productId, verId);
        this.lastModifiedDate = docmaSess.getVersionLastModifiedDate(productId, verId);
        this.releaseDate = docmaSess.getVersionReleaseDate(productId, verId);
        this.derivedFrom = docmaSess.getVersionDerivedFrom(productId, verId);
        this.comment = docmaSess.getVersionComment(productId, verId);
    }

    public int compareTo(Object obj) {
        return getVersionId().compareTo(((ProductVersionModel) obj).getVersionId());
    }

    public String getLanguageCode() {
        return language;
    }

    public void setLanguageCode(String language) {
        this.language = language;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public DocVersionId getDerivedFrom() {
        return derivedFrom;
    }

    public void setDerivedFrom(DocVersionId derivedFrom) {
        this.derivedFrom = derivedFrom;
    }

    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public DocVersionId getVersionId() {
        return versionId;
    }

    public void setVersionId(DocVersionId versionId) {
        this.versionId = versionId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

}
