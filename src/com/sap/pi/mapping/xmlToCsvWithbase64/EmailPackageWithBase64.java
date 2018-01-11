package com.sap.pi.mapping.xmlToCsvWithbase64;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Base64;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.sap.aii.mapping.api.*;




public class EmailPackageWithBase64 extends AbstractTransformation{
	
	static String CRLF = "\r\n";
	List<StringBuilder> csvFileList = new ArrayList<StringBuilder>();

	public static void main(String[] args) throws Exception{
		File stylesheet = new File("src/main/resources/style.xsl");
        File xmlSource = new File("src/main/resources/Data_noBase64.xml");
        StringWriter writer = new StringWriter();
        String file[];

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder1 = factory.newDocumentBuilder();
        Document document = builder1.parse(xmlSource);

        StreamSource stylesource = new StreamSource(stylesheet);
        Transformer transformer = TransformerFactory.newInstance()
                .newTransformer(stylesource);
        Source source = new DOMSource(document);
        Result outputTarget = new StreamResult(new File("src/main/resources/x.csv"));
        Result outputTarget2 = new StreamResult(writer);
        transformer.transform(source, outputTarget2);
        transformer.transform(source, outputTarget);
        file = writer.toString().split(CRLF+CRLF);
        System.out.println(file.length);
        for (int i=2;i<file.length;i++) {
        	String a = file[i].replaceFirst(CRLF, "");
        	System.out.println(a);
        }

	}

	@Override
	public void transform(TransformationInput arg0, TransformationOutput arg1) throws StreamTransformationException {
		InputStream inputStream = arg0.getInputPayload().getInputStream();
		OutputStream outputStream = arg1.getOutputPayload().getOutputStream();
		File stylesheet = new File(getClass().getClassLoader().getResource("style.xsl").getFile());
        StringWriter csvWriter = new StringWriter();
        String csvFileArray[];
        String fileName ;
        Map<String, String> encodedCSVFileMap = new TreeMap<String, String>();
		
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		
		try {
			DocumentBuilder builder = builderFactory.newDocumentBuilder();
			Document document = builder.parse(inputStream);
			
			StreamSource stylesource = new StreamSource(stylesheet);
	        Transformer transformer = TransformerFactory.newInstance()
	                .newTransformer(stylesource);
	        Source source = new DOMSource(document);
	        Result outputTarget = new StreamResult(csvWriter);
	        transformer.transform(source, outputTarget);
	        
	        csvFileArray = csvWriter.toString().split(CRLF+CRLF);
	        for (int fileCount = 2; fileCount < csvFileArray.length; fileCount++) {
	        	String csvFile = csvFileArray[fileCount].replaceFirst(CRLF, "");
	        	String encodedCSVFile = Base64.getEncoder().encodeToString(csvFile.getBytes());
	        	switch (fileCount) {
	        	case 2:
	        		fileName = "Invoice_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
	        		break;
	        	case 3:
	        		fileName = "Purchase_Order_";
	        		break;
	        	case 4:
	        		fileName = "Sales_Order_";
	        		break;
	        	default:
	        		fileName = "-null-";
	        		break;
	        	}
	        	
	        	fileName = fileName + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
	        	encodedCSVFileMap.put(fileName, encodedCSVFile);
	        }
			
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

}
