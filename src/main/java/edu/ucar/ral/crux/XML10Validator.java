/*
 * Copyright (c) 2016. University Corporation for Atmospheric Research (UCAR). All rights reserved.
 */

package edu.ucar.ral.crux;

import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.EntityResolver2;

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

  private EntityResolver2 resolver = null;

  public XML10Validator(){
    resolver = new XMLCatalogResolver( null, true, true );
  }

  public XML10Validator( boolean allowRemoteResources, String... catalogLocations ){
    resolver = new XMLCatalogResolver( catalogLocations, true, allowRemoteResources );
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
    factory.setValidating(true);
    factory.setNamespaceAware(true);

    MyErrorHandler errorHandler = new MyErrorHandler( xsdOrXmlFilePath );
    SAXParser parser = factory.newSAXParser();
    parser.setProperty( "http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema" );

    //if this is a schema document, validate it against the XML Schema 1.0 XSD
    if( xsdOrXmlFilePath.endsWith( ".xsd" ) ) {
      parser.setProperty( "http://java.sun.com/xml/jaxp/properties/schemaSource", "http://www.w3.org/2001/XMLSchema.xsd" );
    }

    XMLReader reader = parser.getXMLReader();
    if( resolver != null ) {
      reader.setProperty( "http://apache.org/xml/properties/internal/entity-resolver", resolver );
    }
    reader.setErrorHandler( errorHandler );
    reader.parse( new InputSource( xsdOrXmlFilePath ) );
    List<ValidationError> failures = errorHandler.getFailures();
    if( failures.size() > 0 ){
      throw new ValidationException( VALIDATION_FAILED_PREFIX, failures );
    }
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