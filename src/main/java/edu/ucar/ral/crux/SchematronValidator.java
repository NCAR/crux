/*
 * Copyright (c) 2016. University Corporation for Atmospheric Research (UCAR). All rights reserved.
 */

package edu.ucar.ral.crux;

import net.sf.saxon.s9api.MessageListener;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
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

  private int debugLevel = 0;

  private ThreadLocal<HashMap<String,XsltExecutable>> templateCacheLocal = new ThreadLocal<>();
  private ThreadLocal<Processor> processorLocal = new ThreadLocal<>();

  public SchematronValidator(){
    System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
  }

  public SchematronValidator(int debugLevel){
    this.debugLevel = debugLevel;
  }

  public void validate( String xmlFile, String schematronFile ) throws ValidationException, IOException {
    long t1 = System.currentTimeMillis();
    File xmlFileObj = new File( xmlFile );
    if( !xmlFileObj.exists() ){
      throw new IOException( String.format( "File %s does not exist", xmlFile ) );
    }
    if( !new File( schematronFile ).exists() ){
      throw new IOException( String.format( "File %s does not exist", schematronFile) );
    }
    cacheDir.mkdirs();
    ensureISOSchematronXSLFilesOnDisk( cacheDir );
    if( debugLevel > 0 ) {
      System.out.println( "Ensuring ISO Schematron files on disk took " + ( System.currentTimeMillis() - t1 ) + " ms" );
    }

    try {
      t1 = System.currentTimeMillis();
      //compile the passed-in Schematron rules into XSL using the ISO Schematron XSL, if necessary
      File xslFile = compileSchematronRulesToXSLIfNeeded( schematronFile );
      if( debugLevel > 0 ) {
        System.out.printf( "Compiling Schematron rules to XSL took " + ( System.currentTimeMillis() - t1 ) + " ms\n" );
      }

      t1 = System.currentTimeMillis();
      //run the compiled XSL rules against the XML file
      String transformResult = transform( xslFile, new File( xmlFile ) );
      if( debugLevel > 0 ) {
        System.out.printf( "Transforming %s using %s took " + ( System.currentTimeMillis() - t1 ) + " ms\n", xmlFile, xslFile );
      }
    }
    catch( SaxonApiException e ){
      throw new IOException( e );
    }
  }

  private File compileSchematronRulesToXSLIfNeeded( String schematronFile ) throws ValidationException, IOException, SaxonApiException {
    File schematronFileObj = new File( schematronFile );
    String filename = schematronFileObj.getName();
    String[] split = filename.split( "\\." );
    String origExt = split[split.length-1];
    //convert the absolute path of the original file to its full path under the cache directory.  This ensures that if
    //there are two differing files on disk named 'xyz.sch' that they each have their own unique compiled xsl path
    String outputDirStr = Utils.uniquePathUnder( cacheDir, schematronFileObj );
    File outputFile = new File( outputDirStr, filename.replace( "."+origExt, ".xsl" ) );
    if( !outputFile.exists() ) {
      outputFile.getParentFile().mkdirs();
      //if compilation fails there is no graceful way to recover.  We are done
      transform( new File( cacheDir, "iso_schematron_message_xslt2.xsl" ), schematronFileObj, outputFile );
    }
    return outputFile;
  }

  /**
   * Transform an XML file using the supplied XSL file and return the output as a String
   * @throws ValidationException
   * @throws SaxonApiException
   */
  private String transform( File xslFile, File xmlFile ) throws ValidationException, SaxonApiException {
    ErrorListener errorListener = new ErrorListener( xmlFile.toString() );
    XsltExecutable templates = getTemplates( xslFile, errorListener );
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    XsltTransformer t = templates.load();
    XdmNode source = getProcessor().newDocumentBuilder().build(new StreamSource(xmlFile));
    t.setInitialContextNode(source);
    t.setErrorListener( errorListener );
    t.setMessageListener( errorListener );
    Serializer out = getProcessor().newSerializer();
    out.setOutputStream( baos );
    t.setDestination( out );
    t.transform();
    if( errorListener.errors.size() > 0 ){
      throw new ValidationException( VALIDATION_FAILED_PREFIX, errorListener.errors );
    }
    return new String( baos.toByteArray(), StandardCharsets.UTF_8 );
  }

  /**
   * Transform an XML file using the supplied XSL file, and write the output to an output file
   * @throws ValidationException
   * @throws SaxonApiException
   */
  private void transform( File xslFile, File xmlFile, File outputFile ) throws ValidationException, SaxonApiException {
    long t1 = System.currentTimeMillis();
    ErrorListener errorListener = new ErrorListener( xmlFile.toString() );
    XsltExecutable templates = getTemplates( xslFile, errorListener );
    XsltTransformer t = templates.load();
    XdmNode source = getProcessor().newDocumentBuilder().build(new StreamSource(xmlFile));
    t.setInitialContextNode(source);
    t.setErrorListener( errorListener );
    t.setMessageListener( errorListener );
    Serializer out = getProcessor().newSerializer();
    out.setOutputFile( outputFile );
    t.setDestination( out );
    t.transform();
    if( debugLevel > 0 ) {
      System.out.printf( "Transforming %s using %s took " + ( System.currentTimeMillis() - t1 ) + " ms\n", xmlFile, xslFile );
    }
    if( errorListener.errors.size() > 0 ){
      throw new ValidationException( VALIDATION_FAILED_PREFIX, errorListener.errors );
    }
  }

  /**
   * Saxon can only use XSL files on disk the way we are calling it.  Ensure that all of the required XSL files for
   * ISO Schematron checking are available on disk, and if not that they are extracted from the JAR/classpath
   * @param outputDir the directory where Schematron XSL files should be stored
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
   * Maintain prepared stylesheets in memory for reuse.  A ThreadLocal instance is maintained, as XsltExecutables are
   * not thread-safe
   */
  private XsltExecutable getTemplates(File xslFile, ErrorListener errorListener ) throws SaxonApiException {
    HashMap<String, XsltExecutable> templateCache = templateCacheLocal.get();
    if( templateCacheLocal.get() == null ){
      templateCache = new HashMap<>();
      templateCacheLocal.set( templateCache );
    }
    XsltExecutable templates = templateCache.get(xslFile.toString());
    if( templates == null ) {
      Processor proc = getProcessor();
      XsltCompiler comp = proc.newXsltCompiler();
      comp.setErrorListener( errorListener );
      templates = comp.compile( new StreamSource( xslFile ) );
      templateCache.put( xslFile.toString(), templates );
    }
    return templates;
  }

  private Processor getProcessor(){
    Processor proc = processorLocal.get();
    if( proc == null ) {
      proc = new Processor( false );
      processorLocal.set( proc );
    }
    return proc;
  }

  private class ErrorListener implements javax.xml.transform.ErrorListener, MessageListener{
    private List<ValidationError> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private String filename;

    private ErrorListener( String filename ){
      this.filename = filename;
    }

    @Override
    public void warning( TransformerException exception ) throws TransformerException {
      warnings.add( String.format( "Warning on line %d col %d: %s", exception.getLocator().getLineNumber(), exception.getLocator().getColumnNumber(), exception.getMessage() ) );
    }

    @Override
    public void error( TransformerException exception ) throws TransformerException {
      errors.add( translateException( exception ) );
    }

    @Override
    public void fatalError( TransformerException exception ) throws TransformerException {
      errors.add( translateException( exception ) );
    }

    private ValidationError translateException( TransformerException e ){
      SourceLocator locator = e.getLocator();
      if( locator != null ){
        return new ValidationError( e.getMessage(), filename, locator.getLineNumber(), locator.getColumnNumber() );
      }
      return new ValidationError( e.getMessage(), filename, null, null );
    }

    @Override
    public void message( XdmNode xdmNode, boolean b, SourceLocator sourceLocator ) {
      if( sourceLocator != null ){
        errors.add( new ValidationError( xdmNode.toString(), filename, sourceLocator.getLineNumber(), sourceLocator.getColumnNumber() ) );
      }
      else{
        errors.add( new ValidationError( xdmNode.toString(), filename, null, null ) );
      }
    }
  }
}