/*
 * Copyright (c) 2016. University Corporation for Atmospheric Research (UCAR). All rights reserved.
 */

package edu.ucar.ral.crux;

import junit.framework.Assert;
import org.junit.Test;

/**
 * Created by braeckel on 6/22/16.
 */
public class SchematronTest {

  @Test
  public void testSchematronPass() throws Exception{
    SchematronValidator validator = new SchematronValidator();
    String xmlFile = "src/test/resources/shiporder-pass1.xml";
    String schematronFile = "src/test/resources/shiporder.sch";
    validator.validate( xmlFile, schematronFile );
  }

  @Test
  public void testSchematronFail() throws Exception{
    SchematronValidator validator = new SchematronValidator();
    String xmlFile = "src/test/resources/shiporder-fail-schematron.xml";
    String schematronFile = "src/test/resources/shiporder.sch";
    try {
      validator.validate( xmlFile, schematronFile );
    }catch( ValidationException e){
      Assert.assertEquals( "Incorrect # of validation failures", 3, e.getValidationErrors().size() );
      Assert.assertEquals( "Incorrect schematron failure message 1", "Ship to name and address must both be present ((if(shiporder:name) then( shiporder:address ) else true()))", e.getValidationErrors().get( 0 ).getError() );
      Assert.assertTrue( "Incorrect schematron failure message 2", e.getValidationErrors().get( 1 ).getError().contains("Item price cannot exceed 10" ) );
      Assert.assertTrue( "Incorrect schematron failure message 3", e.getValidationErrors().get( 2 ).getError().contains( "Item quantity must be present" ) );
      return;
    }
    throw new Exception("Validation should have failed");
  }
}