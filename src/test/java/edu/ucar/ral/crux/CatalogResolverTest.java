/*
 * Copyright (c) 2016. University Corporation for Atmospheric Research (UCAR). All rights reserved.
 */

package edu.ucar.ral.crux;

import org.apache.xml.resolver.apps.resolver;

/**
 * Tests of the Xerces catalog resolver
 */
public class CatalogResolverTest {
//  @Test - test disabled, this is here for debugging and reference
  public void testPrintCatalogResolverHelp() throws Exception{
    //get debug information about the simplegml catalog
    resolver.main( new String[]{ "-h" });
  }

//  @Test - test disabled, this is here for debugging and reference
  public void testSimpleGMLCatalogResolver() throws Exception{
    //get debug information about the simplegml catalog
    resolver.main( new String[]{"-d", "2",
                                "-c", "src/test/resources/gml-system-catalog.xml",
                                "-s", "http://schemas.opengis.net/gml/3.2.1/gml.xsd",
                                "system"
    });
  }

//  @Test - test disabled, this is here for debugging and reference
  public void testSimpleRelativeCatalogResolver() throws Exception{
    resolver.main( new String[]{"-d", "2",
      "-c", "src/test/resources/gml-rewritesystem-catalog.xml",
      "-s", "http://schemas.opengis.net/gml/3.2.1/gml.xsd",
      "system"
    });
  }
}
