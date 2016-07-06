Command-line Refuter of Unshapely XML (CRUX)
=====
Command line and library support for XML schema and Schematron validation for any
platform with a Java runtime.

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

Build
-----
Crux requires Java 7+ and Maven 3 to build.  The bin/build-crux.sh script can be used to
build a fully executable JAR.