Command-line Refuter of Unshapely XML (CRUX)
=====
Command line and library support for XML schema and Schematron validation for any
platform with a Java runtime.

[![Build Status](https://travis-ci.org/NCAR/crux.svg?branch=master)](https://travis-ci.org/NCAR/crux)

Execution
---------
The Crux JAR is also an executable file on MacOS and Unix/Linux systems (an executable ZIP),
and may be used either by:

    java -jar crux.jar [options]

OR

    crux.jar [options]

The following examples utilize the second executable ZIP option (direct execution) for brevity.

Usage Examples
--------------
Execute XML Schema validation against a local XML file:

    crux.jar file.xml

Execute XML Schema validation and Schematron validation against a local XML file:

    crux.jar -s rules.sch file.xml

Execute XML Schema validation and Schematron validation against multiple XML files:

    crux.jar file1.xml file2.xml file3.xsd file4.xml

Execute XML Schema validation against a set of local XML files based on the schema locations in each file:

    crux.jar *.xml

Execute XML Schema validation against a set of local XML files with a single unknown character based on the schema locations in each file (matches file1.xml, fileA.xml, etc.):

    crux.jar file?.xml

Execute XML Schema validation against a remote XML file and a remote XSD file:

    crux.jar http://foo.org/file.xml http://foo.org/myschema.xsd

Execute XML Schema validation against a local XML file using local copies of schemas as defined in catalog.xml:

    crux.jar file.xml -c catalog.xml

Execute XML Schema validation against a local XSD file:

    crux.jar myschema.xsd

Related Projects
-----
### WMO/ICAO Web Validator
Web/HTTP-based validator for WMO and ICAO XML schemas/models which runs in Java servlet containers.  Utilizes Crux for
XML Schema 1.0 and Schematron validation of XML messages. This is an HTTP-based Java toolkit with two components:

* a web user interface for validating messages
* a server-side component that can be used to support validation as a service. This takes XML to validate in an HTTP
POST body and returns validation results in JSON. This can be used to provide a long-running and performant validation
service using Crux - the JVM is only started once and reused elements are cached

### IBLSoft IWXXM Validation Web Service
IWXXM Validation Web Service is a JSON-RPC 2.0 web service which allows to validate IWXXM data against XSD schemas and
Schematron rules stored in the local filesystem with XML catalogue.

Build
-----
Crux requires Java 8+ and Maven 3 to build.  The bin/build-crux.sh script can be used to
build a fully executable JAR.