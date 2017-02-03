/*
 * Copyright (c) 2016. University Corporation for Atmospheric Research (UCAR). All rights reserved.
 */

package edu.ucar.ral.crux;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * Tests for XML 1.0 Schema validation
 */
public class XML10ValidatorTest {

  @Before
  public void setup(){
  }

  @Test
  public void testSimpleInstanceValidation() throws Exception{
    File xmlFile = new File( "src/test/resources/shiporder-pass1.xml" );
    XML10Validator validator = new XML10Validator();
    validator.validate( xmlFile.getAbsolutePath() );
  }

  @Test
  public void testSimpleInstanceValidationMultipleFailures() throws Exception{
    File xmlFile = new File( "src/test/resources/shiporder-fail-schema.xml" );
    XML10Validator validator = new XML10Validator();
    try {
      validator.validate( xmlFile.getAbsolutePath() );
    }catch(ValidationException e){
      Assert.assertEquals( "Incorrect # of validation failures", 3, e.getValidationErrors().size() );
      return;
    }
    throw new Exception("Validation should have failed");
  }

  @Test
  public void testSimpleGMLInstanceValidationWithCatalog() throws Exception{
    File xmlFile = new File( "src/test/resources/simplegml.xml" );
    XML10Validator validator = new XML10Validator( false, "src/test/resources/gml-system-catalog.xml" );
    validator.validate( xmlFile.getAbsolutePath() );
  }
}