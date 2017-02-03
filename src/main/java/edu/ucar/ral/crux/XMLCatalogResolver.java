/*
 * Copyright (c) 2016. University Corporation for Atmospheric Research (UCAR). All rights reserved.
 * 
 * -------------------------------------------------------------------------------------------------------------
 * Notice:
 *
 * This file is partially based on source code from IWXXM Validator (https://github.com/iblsoft/iwxxm-validator)
 * developed by IBLSoft. This was licensed under the Apache License, Version 2.0 (the "License"); you may not use 
 * this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License. 
 * 
 * Copyright (C) 2016, IBL Software Engineering spol. s r. o.
 */

package edu.ucar.ral.crux;

import org.apache.xerces.impl.xs.XSDDescription;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.ls.LSInput;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Extends Xerces's XMLCatalogResolver for debugging purposes
 */
public class XMLCatalogResolver extends org.apache.xerces.util.XMLCatalogResolver{
  private static final Logger LOG = LoggerFactory.getLogger( XMLCatalogResolver.class );

  //whether remote content should be resolved.  When false remote resources (i.e., schemas) are not loaded 
  private boolean allowRemoteResources = true;

  public XMLCatalogResolver() {super();}

  /**
   * @param catalogLocations the path to XML catalog files
   */
  public XMLCatalogResolver( String[] catalogLocations ) {
    setCatalogList( catalogLocations );
  }

  /**
   * @param catalogLocations the path to XML catalog files
   * @param preferPublic whether public or system matches are preferred
   * @param allowRemoteResources when false remote (non-local) resources are not resolved
   */
  public XMLCatalogResolver(String[] catalogLocations, boolean preferPublic, boolean allowRemoteResources) {
    this.allowRemoteResources = allowRemoteResources;
    setCatalogList(catalogLocations);
    setPreferPublic(preferPublic);
  }

  @Override
  public InputSource resolveEntity( String s, String s1 ) throws SAXException, IOException {
    return super.resolveEntity( s, s1 );
  }

  @Override
  public InputSource resolveEntity( String s, String s1, String s2, String s3 ) throws SAXException, IOException {
    return super.resolveEntity( s, s1, s2, s3 );
  }

  @Override
  public InputSource getExternalSubset( String s, String s1 ) throws SAXException, IOException {
    return super.getExternalSubset( s, s1 );
  }

  @Override
  public LSInput resolveResource( String s, String s1, String s2, String s3, String s4 ) {
    return super.resolveResource( s, s1, s2, s3, s4 );
  }

  @Override
  public XMLInputSource resolveEntity( XMLResourceIdentifier xmlResourceIdentifier ) throws XNIException, IOException {
    return super.resolveEntity( xmlResourceIdentifier );
  }

  @Override
  public String resolveIdentifier( XMLResourceIdentifier xmlResourceIdentifier ) throws IOException, XNIException {
    if( xmlResourceIdentifier == null || !(xmlResourceIdentifier instanceof XSDDescription) || xmlResourceIdentifier.getNamespace() == null ){
      return super.resolveIdentifier( xmlResourceIdentifier );
    }
    XSDDescription desc = (XSDDescription) xmlResourceIdentifier;
    String id = super.resolveIdentifier(xmlResourceIdentifier);

    String expandedSystemId = id;
    if (expandedSystemId == null) {
      expandedSystemId = xmlResourceIdentifier.getExpandedSystemId();
    }

    if( expandedSystemId == null ) {
      throw new IOException( String.format( "Identifier %s is not resolved, check if xsi:schemaLocation and xmlns:xsi attributes are correctly defined.", desc.getNamespace() ) );
    }
    
    if( !expandedSystemId.startsWith("file:") ) {
      if( !allowRemoteResources ) {
        LOG.warn( "Remote resources are disabled and identifier {} does not resolve to local path (resolved to {})", desc.getTargetNamespace(), expandedSystemId );
        throw new IOException(
          String.format("Identifier %s is not resolved to local path (resolved to %s). Only resources identified by the local catalog are enabled.",
            desc.getNamespace(), 
            expandedSystemId )
        );
      }
    }

    if( LOG.isDebugEnabled() ) {
      LOG.debug( "Resolved identifier: namespace: {} publicId={] systemId={} to {}", xmlResourceIdentifier.getNamespace(), desc.getPublicId(), desc.getLiteralSystemId(), expandedSystemId );
    }
    return id;
  }
}