/*
 * Copyright (c) 2016. University Corporation for Atmospheric Research (UCAR). All rights reserved.
 */

package edu.ucar.ral.crux;

import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Validator of XML and XSD files against XML schema 1.0
 */
public class XML10Validator {
  private static final String VALIDATION_FAILED_PREFIX = "Validation failed ";

  private XMLCatalogResolver resolver;
  private boolean allowingRemoteResources = false;

  public XML10Validator(){
    resolver = new XMLCatalogResolver( null, true );
  }

  /**
   * Construct a XML10Validator with a set of catalog file locations
   * @param catalogLocations the locations of catalog files to use during validation.  May be null
   */
  public XML10Validator( String... catalogLocations ){
    resolver = new XMLCatalogResolver( catalogLocations, true );
  }

  /**
   * Validate an XSD or XML file against its XML Schema
   * @param xsdOrXmlFilePath the XML or XSD file to be validated, either a local path such as "/tmp/foo.xml" or 
   *                         "file:///tmp/foo.xml", or a remote path such as "http://foo.org/foo.xml"
   * @throws IOException if problems are encountered reading the file
   * @throws SAXException when SAX parsing problems are encountered
   * @throws ParserConfigurationException when SAX initialization fails
   * @throws ValidationException when validation failures occur
   */
  public void validate( String xsdOrXmlFilePath ) throws ParserConfigurationException, SAXException, ValidationException, IOException {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setValidating( true );
    factory.setNamespaceAware( true );

    /////// SECURITY-RELATED RESTRICTIONS ///////
    //taken from OWASP guidelines (https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet#Java)
    //disable DTDs/doctypes - Xerces 2 only - http://xerces.apache.org/xerces-j/features.html#external-general-entities
    //DTDs enable a number of security attacks in XML messages such as the billion laughs exploit
    factory.setFeature( "http://xml.org/sax/features/external-general-entities", false );
    factory.setFeature( "http://apache.org/xml/features/nonvalidating/load-external-dtd", false );
    factory.setFeature( "http://apache.org/xml/features/disallow-doctype-decl", true );
    factory.setXIncludeAware( false );
    factory.setFeature( XMLConstants.FEATURE_SECURE_PROCESSING, true );

    MyErrorHandler errorHandler = new MyErrorHandler( xsdOrXmlFilePath );
    SAXParser parser = factory.newSAXParser();
    parser.setProperty( "http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema" );

    //if this is a schema document, validate it against the XML Schema 1.0 XSD
    if( xsdOrXmlFilePath.endsWith( ".xsd" ) ) {
      parser.setProperty( "http://java.sun.com/xml/jaxp/properties/schemaSource", "http://www.w3.org/2001/XMLSchema.xsd" );
    }

    XMLReader reader = parser.getXMLReader();
    /////// SECURITY-RELATED RESTRICTIONS ///////
    reader.setFeature( "http://xml.org/sax/features/external-general-entities", false );

    resolver.setAllowingRemoteResources( isAllowingRemoteResources() );
    reader.setProperty( "http://apache.org/xml/properties/internal/entity-resolver", resolver );
    reader.setErrorHandler( errorHandler );
    reader.parse( new InputSource( xsdOrXmlFilePath ) );
    List<ValidationError> failures = errorHandler.getFailures();
    if( failures.size() > 0 ){
      throw new ValidationException( VALIDATION_FAILED_PREFIX, failures );
    }
  }

  public boolean isAllowingRemoteResources() {
    return allowingRemoteResources;
  }

  /**
   * Set whether remote (non-local) schema files are resolved.  Remote schema resolution can be used to get validation
   * software to participate in denial of service attacks and other malicious activities. False by default
   */
  public void setAllowingRemoteResources( boolean allowingRemoteResources ) {
    this.allowingRemoteResources = allowingRemoteResources;
  }

  /**
   * Gathers the warnings and errors into a list of ValidationErrors
   */
  private static class MyErrorHandler implements ErrorHandler {
    private String fileName;
    private MyErrorHandler( String fileName ){ this.fileName = fileName; }

    public List<ValidationError> getFailures() {
      return failures;
    }

    private List<ValidationError> failures = new ArrayList<>();

    @Override
    public void warning(SAXParseException exception) throws SAXException {
      failures.add( createFailure( exception ) );
    }

    @Override
    public void error(SAXParseException exception) throws SAXException {
      failures.add( createFailure( exception ) );
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
      failures.add( createFailure( exception ) );
    }

    private ValidationError createFailure( SAXParseException exception ){
      return new ValidationError( exception.getMessage(), fileName, exception.getLineNumber(), exception.getColumnNumber() );
    }
  }
}