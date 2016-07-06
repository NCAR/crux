/*
 * Copyright (c) 2016. University Corporation for Atmospheric Research (UCAR). All rights reserved.
 */

package edu.ucar.ral.crux;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Utility methods
 */
public class Utils {

  /**
   * Determines whether a URL or file path string points to a local file (as opposed to a remote URL)
   * @param path the file path or URL
   * @return true if the path represents a local file
   */
  public static boolean isLocalFile( String path ) {
    try {
      new URL(path);
      return false;
    } catch (MalformedURLException e) {
      return true;
    }
  }

  /**
   * Export a resource embedded in a JAR file to a local file on disk.
   *
   * @param resourceName ie.: "/foo.txt"
   * @param outputFile the local path to where the resource should be written
   * @throws IOException if the resource name cannot be found in the classpath
   */
  static public void writeResourceToFile( String resourceName, File outputFile ) throws IOException {
    InputStream inputStream = null;
    OutputStream resStreamOut = null;
    try {
      inputStream = Utils.class.getResourceAsStream( resourceName );
      resStreamOut = new FileOutputStream( outputFile );
      if(inputStream == null) {
        throw new IOException("Cannot get resource \"" + resourceName + "\" from Jar file.");
      }
      writeToFile( inputStream, outputFile );
    } finally {
      if( inputStream != null )
        inputStream.close();
      if( resStreamOut != null )
        resStreamOut.close();
    }
  }

  /**
   * Write an InputStream to an output file.  This method will close the InputStream when writing is finished
   * @param inputStream the input stream
   * @param outputFile the output file location the input stream will be written to
   */
  public static void writeToFile( InputStream inputStream, File outputFile ) throws IOException{
    if(inputStream == null) {
      throw new NullPointerException("Input stream cannot be null");
    }

    int readBytes;
    byte[] buffer = new byte[4096];
    OutputStream resStreamOut = new FileOutputStream(outputFile);
    try {
      while( ( readBytes = inputStream.read( buffer ) ) > 0 ) {
        resStreamOut.write( buffer, 0, readBytes );
      }
    }finally{
      inputStream.close();
      resStreamOut.close();
    }
  }
}