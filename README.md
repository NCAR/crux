Crux
=====
Command line XML schema and Schematron validator

Usage
-----
The Crux JAR is also an executable file on MacOs and Linux systems (an executable ZIP), and may be used either by:

    java -jar crux.jar [options]

OR

    crux.jar [options]

The following examples utilize the executable ZIP option (direct execution).

Usage Examples
--------------
Execute XML Schema validation against a local XML file:

    crux.jar file.xml

Execute XML Schema validation and Schematron validation gainst a local XML file:

    crux.jar -s schematron.sch file.xml

Execute XML Schema validation against a local XSD file:

    crux.jar myschema.xsd


