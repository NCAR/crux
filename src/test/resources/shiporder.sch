<?xml version="1.0" encoding="UTF-8"?>
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
   <sch:title>Schematron validation</sch:title>
   <sch:ns prefix="shiporder" uri="http://www.w3schools.com/schema/shiporder"/>
   <sch:pattern id="rule1">
      <sch:rule context="//shiporder:shiporder/shiporder:shipto">
         <sch:assert test="(if(shiporder:name) then( shiporder:address ) else true())">Ship to name and address must both be present</sch:assert>
      </sch:rule>
   </sch:pattern>
   <sch:pattern id="rule2">
      <sch:rule context="//shiporder:shiporder/shiporder:item">
         <sch:assert test="number(shiporder:price) lt 10.0">Item price cannot exceed 10</sch:assert>
      </sch:rule>
   </sch:pattern>
   <sch:pattern id="rule3">
      <sch:rule context="//shiporder:shiporder/shiporder:item">
         <sch:assert test="(shiporder:quantity)">Item quantity must be present</sch:assert>
      </sch:rule>
   </sch:pattern>
</sch:schema>
