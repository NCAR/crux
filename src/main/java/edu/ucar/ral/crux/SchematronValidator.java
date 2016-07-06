/*
 * Copyright (c) 2016. University Corporation for Atmospheric Research (UCAR). All rights reserved.
 */

package edu.ucar.ral.crux;

import net.sf.saxon.Transform;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
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
  /**
   * Validate the given file with Schematron rules
   * @param xmlFile the file to be validated
   * @param schematronFile the file which contains Schematron rules
   * @throws IOException
   */
  public void validate( String xmlFile, String schematronFile ) throws ValidationException, IOException {
    File xmlFileObj = new File( xmlFile );
    if( !xmlFileObj.exists() ){
      throw new IOException( String.format( "File %s does not exist", xmlFile ) );
    }
    if( !new File( schematronFile ).exists() ){
      throw new IOException( String.format( "File %s does not exist", schematronFile) );
    }
    //recreate every time in case the source file has changed
    File xslFile = File.createTempFile( "schematronTemp", ".xsl" );

    File xslOutputDir = new File( System.getProperty("java.io.tmpdir"), "cruxcache" );
    ensureXSLFilesOnDisk( xslOutputDir );
    File isoSchematronXSL = new File( xslOutputDir, "iso_schematron_message_xslt2.xsl" );

    //Compile Schematron schema into XSLT
    //java -jar saxon9.5-he.jar $schFile iso-schematron-xslt2/iso_schematron_message_xslt2.xsl -o:outputFile
//    System.out.println( "\tCompiling Schematron into XSLT (" + xslFile.getPath() + ")" );
    try {
      Transform.main( new String[]{ schematronFile, isoSchematronXSL.getPath(), "-o:" + xslFile.getPath() } );
    }
    catch(Exception e){
      throw new IOException( e );
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
      Transform.main( new String[]{ xmlFile, xslFile.getPath() } );
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

  /**
   * Saxon can only use XSL files on disk the way we are calling it.  Ensure that all of the required XSL files for
   * Schematron checking are available on disk, and if not that they are extracted from the JAR/classpath
   */
  public static void ensureXSLFilesOnDisk( File outputDir ){
    outputDir.mkdirs();
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
}