# Conversion of XML file containing invoice, purchase order and sales order files into csv files and creation of Email-MIME body with base64 encoded zip files
	SAP ECC --> SAP PI --> Email Client

1.  Read the xml document from the inputstream.
2.  Use XSLT stylesheet to extract and convert records from xml into csv String.
3.  Separate each file from the csv String and base64 encode each file to be put into a TreeMap.
4.  Create the MIME part for the Mail with base 64 encoded csv files.
5.  Write the MIME part to the outputStream.
