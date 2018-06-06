/*
 * Copyright (c) 2016. University Corporation for Atmospheric Research (UCAR). All rights reserved.
 */

package edu.ucar.ral.crux;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXParseException;

/**
 * Tests for XML 1.0 Schema validation
 */
public class XML10ValidatorTest {

  @Before
  public void setup(){
  }

  @Test
  public void testSimpleInstanceValidation() throws Exception{
    XML10Validator validator = new XML10Validator();
    validator.validate( "src/test/resources/shiporder-pass1.xml" );
  }

  @Test
  public void testSimpleInstanceValidationMultipleFailures() throws Exception{
    XML10Validator validator = new XML10Validator();
    try {
      validator.validate( "src/test/resources/shiporder-fail-schema.xml" );
    }catch(ValidationException e){
      Assert.assertEquals( "Incorrect # of validation failures", 3, e.getValidationErrors().size() );
      return;
    }
    throw new Exception("Validation should have failed");
  }

  @Test
  public void testSimpleGMLInstanceValidationWithCatalog() throws Exception{
    XML10Validator validator = new XML10Validator( "src/test/resources/gml-system-catalog.xml" );
    validator.validate( "src/test/resources/simplegml.xml" );
  }

  @Test(expected = SAXParseException.class)
  public void testXXE1() throws Exception{
    XML10Validator validator = new XML10Validator();
    //XXE accessing local files - this should throw a DTD-related Exception
    //this example was modified to access a local file rather than /dev/random so that it is cross platform and doesn't
    //hang indefinitely (i.e., can be readily tested).  If a local file can be loaded then so can /dev/random
    validator.validate( "src/test/resources/attacks/xxe1.xml" );
  }

  @Test(expected = SAXParseException.class)
  public void testXXE2() throws Exception{
    XML10Validator validator = new XML10Validator();
    validator.validate( "src/test/resources/attacks/xxe2.xml" );
  }

  @Test(expected = SAXParseException.class)
  public void testEmbeddedSchema() throws Exception{
    XML10Validator validator = new XML10Validator();
    validator.validate( "src/test/resources/attacks/embedded-schema.xml" );
  }

  @Test(expected = SAXParseException.class, timeout = 2000)  //timeout needed for test failures - billion laughs will run for a LONG time
  public void testBillionLaughs() throws Exception{
    XML10Validator validator = new XML10Validator();
    validator.validate( "src/test/resources/attacks/billion-laughs.xml" );
  }

  @Test(expected = SAXParseException.class, timeout = 2000)
  public void testRecursiveEntity() throws Exception{
    XML10Validator validator = new XML10Validator();
    validator.validate( "src/test/resources/attacks/recursive-entity.xml" );
  }

  @Test(expected = SAXParseException.class, timeout = 2000)
  public void testQuadraticExplosion() throws Exception{
    XML10Validator validator = new XML10Validator();
    validator.validate( "src/test/resources/attacks/quadratic-explosion.xml" );
  }
}