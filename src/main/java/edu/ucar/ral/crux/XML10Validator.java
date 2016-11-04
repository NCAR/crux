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

  public XML10Validator(){}

  public XML10Validator( int debugLevel, String... catalogLocations ){
    if( catalogLocations.length > 0 ) {
      resolver = new DebugXMLCatalogResolver( debugLevel, catalogLocations, true );
    }
  }

  /**
   * Validate the given file (either an XSD or XML file), using the provided resolver
   * @param xsdOrXmlFile the XML or XSD file to be validated
   * @throws IOException if problems are encountered reading the file
   * @throws SAXException when SAX parsing problems are encountered
   * @throws ParserConfigurationException when SAX initialization fails
   * @throws ValidationException when validation failures occur
   */
  public void validate( String xsdOrXmlFile ) throws ParserConfigurationException, SAXException, ValidationException, IOException {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setValidating(true);
    factory.setNamespaceAware(true);

    MyErrorHandler errorHandler = new MyErrorHandler( xsdOrXmlFile );
    SAXParser parser = factory.newSAXParser();
    parser.setProperty( "http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema" );

    //if this is a schema document, validate it against the XML Schema 1.0 XSD
    if( xsdOrXmlFile.endsWith( ".xsd" ) ) {
      parser.setProperty( "http://java.sun.com/xml/jaxp/properties/schemaSource", "http://www.w3.org/2001/XMLSchema.xsd" );
    }

    XMLReader reader = parser.getXMLReader();
    if( resolver != null ) {
      reader.setProperty( "http://apache.org/xml/properties/internal/entity-resolver", resolver );
    }
    reader.setErrorHandler( errorHandler );
    reader.parse( new InputSource( xsdOrXmlFile ) );
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

    private List<ValidationError> failures = new ArrayList<ValidationError>();

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