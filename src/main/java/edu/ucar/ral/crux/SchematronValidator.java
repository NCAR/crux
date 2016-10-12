/*
 * Copyright (c) 2016. University Corporation for Atmospheric Research (UCAR). All rights reserved.
 */

package edu.ucar.ral.crux;

import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Processor;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Validates XML files against Schematron rules.  This uses the ISO Schematron XSLT files, which are then executed by
 * Saxon.
 *
 * This class currently calls Saxon via the main() method.  While it is possible to call sub-classes of Saxon directly,
 * the Transform class which is called by the main() method is complicated and not very easy to figure out how to use
 * directly.  An improvement would be to call the Saxon classes directly rather than through main()
 */
public class SchematronValidator {
  private static final String VALIDATION_FAILED_PREFIX = "Schematron validation failed ";

  private File cacheDir = new File( System.getProperty("java.io.tmpdir"), "cruxcache" );

  //Thread safety guarded by "this"
  private HashMap<String,Templates> templateCache = new HashMap<String, Templates>();

  //Thread safety guarded by its own reference (errorListener.this)
  private final ErrorListener errorListener = new ErrorListener();

  public SchematronValidator(){
    System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
  }

  public void validate( String xmlFile, String schematronFile ) throws ValidationException, IOException {
    File xmlFileObj = new File( xmlFile );
    if( !xmlFileObj.exists() ){
      throw new IOException( String.format( "File %s does not exist", xmlFile ) );
    }
    if( !new File( schematronFile ).exists() ){
      throw new IOException( String.format( "File %s does not exist", schematronFile) );
    }
    cacheDir.mkdirs();
    ensureISOSchematronXSLFilesOnDisk( cacheDir );

    //compile the passed-in Schematron rules into XSL using the ISO Schematron XSL, if necessary
    try {
      File xslFile = compileSchematronRulesToXSLIfNeeded( schematronFile );

      //run the compiled XSL rules against the XML file
      String transformResult = transform( xslFile, new File( xmlFile ) );
      int k = 0;
    }
    catch( TransformerException e ){
      int j = 0;
    }
  }

  private File compileSchematronRulesToXSLIfNeeded( String schematronFile ) throws TransformerException, ValidationException {
    String filename = new File( schematronFile ).getName();
    String[] split = filename.split( "\\." );
    String origExt = split[split.length-1];
    File outputFile = new File( cacheDir, filename.replace( "."+origExt, ".xsl" ) );
    if( !outputFile.exists() ) {
      //if compilation fails there is no graceful way to recover.  We are done
      transform( new File( cacheDir, "iso_schematron_message_xslt2.xsl" ), new File( schematronFile ), outputFile );
    }
    return outputFile;
  }

  private String transform( File xslFile, File xmlFile ) throws TransformerException, ValidationException, IOException {
    long t1 = System.currentTimeMillis();
    Templates templates = getTemplates( xslFile );
    Transformer transformer = templates.newTransformer();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ErrorListener listener = new ErrorListener();
    transformer.setErrorListener( listener );
    transformer.transform( new StreamSource( xmlFile ), new StreamResult( out ) );
    System.out.printf( "Transforming %s using %s took " + ( System.currentTimeMillis() - t1 ) + " ms\n", xmlFile, xslFile );
    out.close();
    String result = new String( out.toByteArray(), StandardCharsets.UTF_8 );
    if( listener.errors.size() > 0 || result.length() > 0 ){
      throw new ValidationException( listener.errors );
    }
    return result;
  }

  private void transform( File xslFile, File xmlFile, File outputFile ) throws TransformerException, ValidationException {
    long t1 = System.currentTimeMillis();
    Templates templates = getTemplates( xslFile );
    Transformer transformer = templates.newTransformer();
    ErrorListener listener = new ErrorListener();
    transformer.setErrorListener( listener );
    transformer.transform( new StreamSource( xmlFile ), new StreamResult( outputFile ) );
    System.out.printf( "Transforming %s using %s took " + ( System.currentTimeMillis() - t1 ) + " ms\n", xmlFile, xslFile );
    if( listener.errors.size() > 0 ){
      throw new ValidationException( listener.errors );
    }
  }

  /**
   * Saxon can only use XSL files on disk the way we are calling it.  Ensure that all of the required XSL files for
   * Schematron checking are available on disk, and if not that they are extracted from the JAR/classpath
   */
  public static void ensureISOSchematronXSLFilesOnDisk( File outputDir ){
    String resourcePrefix = "iso-schematron-xslt2";
    String[] xslFileNames = new String[]{ "iso_schematron_message_xslt2.xsl", "iso_schematron_skeleton_for_saxon.xsl" };
    for( String xslFileName : xslFileNames ){
      File localFile = new File( outputDir, xslFileName );
      if( !localFile.exists() ){
        try {
          Utils.writeResourceToFile( "/"+resourcePrefix+"/"+xslFileName, localFile );
        } catch( IOException e ) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Maintain prepared stylesheets in memory for reuse
   */
  private synchronized Templates getTemplates(File xslFile) throws TransformerException {
    Templates templates = templateCache.get(xslFile.toString());
    if( templates==null ) {
      TransformerFactory factory = TransformerFactory.newInstance();
      templates = factory.newTemplates( new StreamSource( xslFile ) );
      templateCache.put( xslFile.toString(), templates );
    }
    return templates;
  }

  private class ErrorListener implements javax.xml.transform.ErrorListener{
    private List<ValidationError> errors = new ArrayList<ValidationError>();
    private List<String> warnings = new ArrayList<String>();

    @Override
    public void warning( TransformerException exception ) throws TransformerException {
      warnings.add( String.format( "Warning on line %d col %d: %s", exception.getLocator().getLineNumber(), exception.getLocator().getColumnNumber(), exception.getMessage() ) );
    }

    @Override
    public void error( TransformerException exception ) throws TransformerException {
      errors.add( new ValidationError(
        exception.getMessage(),
        exception.getLocator().getSystemId(),
        exception.getLocator().getLineNumber(),
        exception.getLocator().getColumnNumber() ) );
    }

    @Override
    public void fatalError( TransformerException exception ) throws TransformerException {
      errors.add( new ValidationError(
        exception.getMessage(),
        exception.getLocator().getSystemId(),
        exception.getLocator().getLineNumber(),
        exception.getLocator().getColumnNumber() ) );
    }
  }

  public static void main( String[] args ) throws Exception {
    Processor processor = new Processor( true );
    Configuration config = processor.getUnderlyingConfiguration();
    config.setVersionWarning( true );
  }

  /**
   * Validate the given file with Schematron rules
   * @param xmlFile the file to be validated
   * @param schematronFile the file which contains Schematron rules
   * @throws IOException
   *
  public void validateOLD( String xmlFile, String schematronFile ) throws ValidationException, IOException {
  File xmlFileObj = new File( xmlFile );
  if( !xmlFileObj.exists() ){
  throw new IOException( String.format( "File %s does not exist", xmlFile ) );
  }
  if( !new File( schematronFile ).exists() ){
  throw new IOException( String.format( "File %s does not exist", schematronFile) );
  }
  cacheDir.mkdirs();

  File intermediateXslFile = new File( "schematronTemp", ".xsl" );
  //    ensureISOSchematronXSLFilesOnDisk( cacheDir );
  File isoSchematronXSL = new File( cacheDir, "iso_schematron_message_xslt2.xsl" );

  //Compile Schematron schema into XSLT
  //java -jar saxon9.5-he.jar $schFile iso-schematron-xslt2/iso_schematron_message_xslt2.xsl -o:outputFile -quit:off
  //    System.out.println( "\tCompiling Schematron into XSLT (" + xslFile.getPath() + ")" );
  if( !intermediateXslFile.exists() ) {
  long t1 = System.currentTimeMillis();
  try {
  Templates templates = getTemplates( "/iso-schematron-xslt2/iso_schematron_message_xslt2.xsl" );
  Transformer transformer = templates.newTransformer();
  transformer.transform( new StreamSource( schematronFile ), new StreamResult( intermediateXslFile ) );
  //        Transform.main( new String[]{ schematronFile, isoSchematronXSL.getPath(), "-o:" + intermediateXslFile.getPath(), "-quit:off" } );
  }
  catch( Exception e ) {
  throw new IOException( e );
  }
  System.out.println( "Compiling Schematron into XSLT took " + ( System.currentTimeMillis() - t1 ) + " ms" );
  }



  //java -jar saxon9.5-he.jar $xmlFile schema-compiled.xsl
  //replace sys.out and sys.err temporarily so we can check the output of Saxon
  PrintStream originalSysOut = System.out;
  PrintStream originalSysErr = System.err;
  ByteArrayOutputStream outStream = new ByteArrayOutputStream();
  PrintStream replacementSysOut = new PrintStream( outStream );
  ByteArrayOutputStream errStream = new ByteArrayOutputStream();
  PrintStream replacementSysErr = new PrintStream( errStream );
  try{
  System.setOut( replacementSysOut );
  System.setErr( replacementSysErr );
  Transform.main( new String[]{ xmlFile, intermediateXslFile.getPath() } );
  }
  catch(Exception e ){
  throw new IOException( e );
  }
  finally{
  System.setOut( originalSysOut );
  System.setErr( originalSysErr );
  }
  String sysout = new String( outStream.toByteArray() );
  String syserr = new String( errStream.toByteArray() );
  if( syserr.length() > 0 ){
  //each line is a parsing error
  String[] lines = syserr.split( "\\n" );
  List<ValidationError> failures = new ArrayList<ValidationError>( lines.length );
  for( String line : lines ){
  failures.add( new ValidationError( line, xmlFile, null, null ) );
  }
  throw new ValidationException( failures );
  }

  List<ValidationError> failures = new ArrayList<ValidationError>();
  if( failures.size() > 0 ){
  throw new ValidationException( VALIDATION_FAILED_PREFIX, failures );
  }
  }
   */
}