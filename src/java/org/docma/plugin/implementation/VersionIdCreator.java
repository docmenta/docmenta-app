/*
 */
package org.docma.plugin.implementation;

import org.docma.coreapi.DocVersionId;
import org.docma.plugin.VersionId;

/**
 *
 * @author MP
 */
public class VersionIdCreator 
{
    private VersionIdCreator()
    {
    }
    

    /**
     * Factory method.
     * @param docVerId
     * @return VersionId instance.
     */
    static VersionId create(DocVersionId docVerId)
    {
        return new VersionIdImpl(docVerId);
    }
    
}
