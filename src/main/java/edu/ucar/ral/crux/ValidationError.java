/*
 * Copyright (c) 2016. University Corporation for Atmospheric Research (UCAR). All rights reserved.
 */

package edu.ucar.ral.crux;

/**
 * A validation error class which represents the error and the relevant location within the validated file
 */
public class ValidationError {
  private String error;
  private String fileName;
  private Integer lineNumber;
  private Integer columnNumber;

  /**
   *
   * @param error The error that caused a validation problem
   * @param fileName The file in which the error occurred
   * @param lineNumber The line number of the end of the text that
   *                   caused the error or warning
   * @param columnNumber The column number of the end of the text that
   *                   caused the error or warning
   */
  public ValidationError( String error, String fileName, Integer lineNumber, Integer columnNumber ) {
    this.error = error;
    this.fileName = fileName;
    this.lineNumber = lineNumber;
    this.columnNumber = columnNumber;
  }

  public String getError() {
    return error;
  }

  public String getFileName() {return fileName;}

  public Integer getLineNumber() {
    return lineNumber;
  }

  public Integer getColumnNumber() {
    return columnNumber;
  }

  @Override
  public String toString() {
    return String.format( "%s line %d, col %d: %s", getFileName(), getLineNumber(), getColumnNumber(), getError() );
  }
}
