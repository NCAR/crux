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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates XML files against Schematron rules.  This uses the ISO Schematron XSLT files, which are then executed by
 * Saxon.
 */
public class SchematronValidator {
  private static final Logger LOG = LoggerFactory.getLogger( SchematronValidator.class );
  private static final String VALIDATION_FAILED_PREFIX = "Schematron validation failed ";

  // the group in this pattern will capture the 'file.xml' inside of lines like "document('file.xml')"
  // this is defined here so it doesn't need to be repeatedly compiled with every Schematron validation step
  private static final Pattern DOCUMENT_PATTERN = Pattern.compile( "document\\(\\'(.+)\\'\\)" );

  private File cacheDir = new File( System.getProperty("java.io.tmpdir"), "cruxcache" );
  private ThreadLocal<HashMap<String,XsltExecutable>> templateCacheLocal = new ThreadLocal<>();
  private ThreadLocal<Processor> processorLocal = new ThreadLocal<>();
  //stores the set of dependent files for each Schematron file so we don't have to search the SCH file
  //every time validation is performed
  private Map<File,List<File>> schToReferencedFiles = new HashMap<>();

  public SchematronValidator(){
    System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
  }

  /**
   * Validate an XML file against a local Schematron definition
   * @param xmlFile the XML file to validate
   * @param schematronFile the Schematron definition file against which the XML is checked
   * @throws ValidationException if validation failures occur
   * @throws IOException if necessary files are not found
   */
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
    LOG.debug( "Ensuring ISO Schematron files on disk took " + ( System.currentTimeMillis() - t1 ) + " ms" );

    try {
      t1 = System.currentTimeMillis();
      //compile the passed-in Schematron rules into XSL using the ISO Schematron XSL, if necessary
      File xslFile = compileSchematronRulesToXSLIfNeeded( new File( schematronFile ) );
      LOG.debug( String.format( "Compiling Schematron rules to XSL took " + ( System.currentTimeMillis() - t1 ) + " ms" ) );

      t1 = System.currentTimeMillis();
      //run the compiled XSL rules against the XML file
      String transformResult = transform( xslFile, new File( xmlFile ) );
      LOG.debug( String.format( "Transforming %s using %s took " + ( System.currentTimeMillis() - t1 ) + " ms", xmlFile, xslFile ) );
    }
    catch( SaxonApiException e ){
      throw new IOException( e );
    }
  }

  private File compileSchematronRulesToXSLIfNeeded( File schematronFile ) throws ValidationException, IOException, SaxonApiException {
    String filename = schematronFile.getName();
    String[] split = filename.split( "\\." );
    String origExt = split[split.length-1];
    //convert the absolute path of the original file to its full path under the cache directory.  This ensures that if
    //there are two differing files on disk named 'xyz.sch' that they each have their own unique compiled xsl path
    String outputDirStr = Utils.uniquePathUnder( cacheDir, schematronFile );
    File outputFile = new File( outputDirStr, filename.replace( "."+origExt, ".xsl" ) );
    //re/create the XSL if the file doesn't exist or the file last modified times of the SCH and XSL files do not match.
    //For example, if the SCH has been modified and the XSL needs to be regenerated
    if( !outputFile.exists() || schematronFile.lastModified() != outputFile.lastModified() ) {
      outputFile.getParentFile().mkdirs();
      outputFile.delete();  //for when the file is being regenerated, this does nothing if the file does not exist
      schToReferencedFiles.remove( schematronFile );  //if the SCH file was updated our referenced file cache should be invalidated
      LOG.debug( "Creating cached XSL file: "+outputFile );
      //if compilation fails there is no graceful way to recover.  We are done
      transform( new File( cacheDir, "iso_schematron_message_xslt2.xsl" ), schematronFile, outputFile );
      outputFile.setLastModified( schematronFile.lastModified() );
    }
    cacheReferencedDocumentsIfNecessary( schematronFile );
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
    LOG.debug( String.format( "Transforming %s using %s took " + ( System.currentTimeMillis() - t1 ) + " ms\n", xmlFile, xslFile ) );
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
   * Cache any other documents referenced in a Schematron that are required.  This includes Schematron definitions like
   * document('file.xml') which effectively specify a strict dependency for a particular Schematron file to work correctly
   *
   * @param schematronFile
   */
  private void cacheReferencedDocumentsIfNecessary( File schematronFile ) throws IOException {
    long start = System.currentTimeMillis();
    //This map is used to reduce searching through the SCH file to a one-time process.  The performance improvement is
    //minor and mainly useful for high-volume validators that are processing many files per second
    List<File> referencedDocuments = schToReferencedFiles.get( schematronFile );
    if( referencedDocuments == null ) {
      referencedDocuments = new ArrayList<>();
      schToReferencedFiles.put( schematronFile, referencedDocuments );
      try( BufferedReader br = new BufferedReader( new FileReader( schematronFile ) ) ) {
        String line;
        while( ( line = br.readLine() ) != null ) {
          //if this line includes an external document definition of the form:   document('file.xml')
          Matcher documentMatcher = DOCUMENT_PATTERN.matcher( line );
          if( documentMatcher.find() ) {
            // group 0 is the entire 'document()' portion, group 1 is the file path inside of the document() portion
            String documentPath = documentMatcher.group( 1 );
            File documentFile = new File( documentPath );

            //translate relative paths into an absolute file path relative to the Schematron file
            if( ! documentFile.isAbsolute() ) {
              documentFile = new File( schematronFile.getParentFile(), documentPath );
            }
            referencedDocuments.add( documentFile );
          }
        }
      }
    }
    for( File f : referencedDocuments ){
      //note that if this file is referenced by the Schematron but does not exist a FileNotFound will be thrown here -
      //this ends this method and likely fails validation.  This is probably the desirable behavior as it fails early
      //but may need to be revisited
      cacheIfNecessary( f );
    }
    if( referencedDocuments.size() > 0 ) {
      LOG.debug( "Caching " + referencedDocuments.size() + " referenced Schematron docs took " + ( System.currentTimeMillis() - start ) + " ms" );
    }
  }

  /**
   * Cache a file by placing it into the cache dir if it does not already exist AND if the last modified times do not match.
   * A file that has a different last modified time than its cached version will be replaced
   * @param file
   * @return true of the file was cached, false otherwise
   * @throws IOException
   */
  private boolean cacheIfNecessary( File file ) throws IOException{
    //convert the absolute path of the original file to its full path under the cache directory.  This ensures that if
    //there are two differing files on disk named 'xyz.sch' that they each have their own unique cache file path
    String outputDirStr = Utils.uniquePathUnder( cacheDir, file );
    File outputFile = new File( outputDirStr, file.getName() );
    //re/create the cached file if the file doesn't already exist or the file last modified times do not match.
    //If the source file has been modified the cached file needs to be regenerated
    if( !outputFile.exists() || file.lastModified() != outputFile.lastModified() ) {
      outputFile.getParentFile().mkdirs();
      LOG.debug( "Creating cached file: "+outputFile );
      Files.copy( file.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING );
      outputFile.setLastModified( file.lastModified() );
      return true;
    }
    return false;
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
    XsltExecutable templates = templateCache.get( xslFile.toString() );
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