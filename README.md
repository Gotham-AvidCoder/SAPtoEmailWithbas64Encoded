# SAP PI Java Mapping - Attachments 3
Convert Invoice, purchase order and sales order into base 64 to be sent in a mail

1.  Read the xml document from the inputstream.
2.  Use XSLT stylesheet to extract and convert records from xml into csv String.
3.  Separate each file from the csv String and base64 encode each file to be put into a TreeMap.
4.  Create the MIME part for the Mail with base 64 encoded csv files.
5.  Write the MIME part to the outputStream.
