/*
 * Copyright (c) 2016. University Corporation for Atmospheric Research (UCAR). All rights reserved.
 */

package edu.ucar.ral.crux;

import org.apache.xerces.impl.xs.XSDDescription;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.w3c.dom.ls.LSInput;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Extends Xerces's XMLCatalogResolver for debugging purposes.  By default debugging messages are disabled
 */
public class DebugXMLCatalogResolver extends org.apache.xerces.util.XMLCatalogResolver{
  int debugLevel = 0;

  public DebugXMLCatalogResolver() {super();}

  /**
   * @param debugLevel the debug level.  When greater than 0 debug messages will be printed to stdout
   * @param catalogLocations the path to XML catalog files
   */
  public DebugXMLCatalogResolver( int debugLevel, String[] catalogLocations ) {
    this.debugLevel = debugLevel;
    setCatalogList( catalogLocations );
  }

  /**
   * @param debugLevel the debug level.  When greater than 0 debug messages will be printed to stdout
   * @param catalogLocations the path to XML catalog files
   * @param preferPublic whether public or system matches are preferred
   */
  public DebugXMLCatalogResolver( int debugLevel, String[] catalogLocations, boolean preferPublic ) {
    this.debugLevel = debugLevel;
    setCatalogList( catalogLocations );
    setPreferPublic( preferPublic );
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
    if( xmlResourceIdentifier == null || !(xmlResourceIdentifier instanceof XSDDescription ) ||
      xmlResourceIdentifier.getNamespace() == null ){
      return super.resolveIdentifier( xmlResourceIdentifier );
    }
    XSDDescription desc = (XSDDescription) xmlResourceIdentifier;
    String id = super.resolveIdentifier( xmlResourceIdentifier );
    if( debugLevel > 0 ) {
      System.out.printf( "Resolved identifier: namespace: %s publicId=%s systemId=%s to %s\n" , xmlResourceIdentifier.getNamespace(), desc.getPublicId(), desc.getLiteralSystemId(), id);
    }
    return id;
  }
}