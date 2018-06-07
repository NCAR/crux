/*
 * Copyright (c) 2017. University Corporation for Atmospheric Research (UCAR). All rights reserved.
 */

package edu.ucar.ral.crux;

import junit.framework.Assert;
import org.junit.Test;

public class UtilsTest {
  
  @Test
  public void testIsLocalFile(){
      Assert.assertEquals(true, Utils.isLocalFile( "/tmp/cruxfile" ) );
      Assert.assertEquals(true, Utils.isLocalFile( "file:/tmp/cruxfile" ) );
      Assert.assertEquals(false, Utils.isLocalFile( "http://host.org/foo" ) );
      Assert.assertEquals(false, Utils.isLocalFile( "ftp://host.org" ) );
  }
  
}