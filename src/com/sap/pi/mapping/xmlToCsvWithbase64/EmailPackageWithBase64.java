package com.sap.pi.mapping.xmlToCsvWithbase64;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

public class EmailPackageWithBase64 extends AbstractTransformation {

	String CRLF = "\r\n";
	String boundary = "001a114bc6f2d60884056265dfdc";
	String contentType = "multipart/mixed; boundary=\"" + boundary + "\"";
	String mailContent = "Dear User," + CRLF + "PFA the required CSV files for the user" + CRLF + CRLF + CRLF
			+ "Regards," + CRLF + "SAP PI team";

	StringWriter csvWriter = new StringWriter();
	StringBuilder emailBuilder = new StringBuilder();
	Map<String, String> encodedCSVFileMap = new TreeMap<String, String>();
	
	public static void main(String args[]) throws ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException {
		File xmlFile = new File("sample_files/SAP_nobase64.xml");
		File xslFile = new File("sample_files/style_1.xsl");
		
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

		DocumentBuilder builder = builderFactory.newDocumentBuilder();
		Document document = builder.parse(xmlFile);

		StreamSource stylesource = new StreamSource(xslFile);
		Transformer transformer = TransformerFactory.newInstance().newTransformer(stylesource);
		Source source = new DOMSource(document);
		Result outputTarget = new StreamResult(new File("sample_files/test_file.csv"));
		transformer.transform(source, outputTarget);
		System.out.println("done");
		
		
	}

	@Override
	public void transform(TransformationInput arg0, TransformationOutput arg1) throws StreamTransformationException {
		InputStream inputStream = arg0.getInputPayload().getInputStream();
		OutputStream outputStream = arg1.getOutputPayload().getOutputStream();

		try {
			getTrace().addInfo("Transforming XML to String and adding it to StringWriter Object");
			transformXMLtoStringWriter(inputStream);
			
			getTrace().addInfo("Separating the csv files from the string writer and converting them into base64 encoded files." + CRLF + "adding the base64 encoded files to a Map with filename as key");
			
			base64EncodeCSVFiles();
			
			getTrace().addInfo("Creating the mail MIME body with encoded csv files as attachments");
			createEmailMIMEbodywithCSVFiles();

			getTrace().addInfo("Writing the entire MIME content to the outputstream and setting the content type of the outputheader");
			outputStream.write(emailBuilder.toString().getBytes());
			arg1.getOutputHeader().setContentType(contentType);

		} catch (ParserConfigurationException e) {
			throw new StreamTransformationException("Custom Exception handler:" + e.getMessage(), e);
		} catch (SAXException e) {
			throw new StreamTransformationException("Custom Exception handler:" + e.getMessage(), e);
		} catch (IOException e) {
			throw new StreamTransformationException("Custom Exception handler:" + e.getMessage(), e);
		} catch (TransformerConfigurationException e) {
			throw new StreamTransformationException("Custom Exception handler:" + e.getMessage(), e);
		} catch (TransformerFactoryConfigurationError e) {
			throw new StreamTransformationException("Custom Exception handler:" + e.getMessage(), e);
		} catch (TransformerException e) {
			throw new StreamTransformationException("Custom Exception handler:" + e.getMessage(), e);
		} catch (Exception e) {
			throw new StreamTransformationException("Custom Exception handler:" + e.getMessage(), e);
		}

	}

	public void transformXMLtoStringWriter(InputStream inputStream) throws ParserConfigurationException, SAXException,
			IOException, TransformerFactoryConfigurationError, TransformerException {

		File stylesheet = new File(getClass().getClassLoader().getResource("style.xsl").getFile());
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

		DocumentBuilder builder = builderFactory.newDocumentBuilder();
		Document document = builder.parse(inputStream);

		StreamSource stylesource = new StreamSource(stylesheet);
		Transformer transformer = TransformerFactory.newInstance().newTransformer(stylesource);
		Source source = new DOMSource(document);
		Result outputTarget = new StreamResult(csvWriter);
		transformer.transform(source, outputTarget);

	}

	public void base64EncodeCSVFiles() throws IOException {
		String csvFileArray[];
		String fileName;

		csvFileArray = csvWriter.toString().split(CRLF + CRLF);
		for (int fileCount = 1; fileCount < csvFileArray.length; fileCount++) {
			String csvFile = csvFileArray[fileCount].replaceFirst(CRLF, "");
			char fileType = csvFile.charAt(0);
			String encodedCSVFile = Base64.getEncoder().encodeToString(csvFile.getBytes());
			switch (fileType) {
			case 'I':
				fileName = "Invoice_";
				break;
			case 'O':
				fileName = "Purchase-Order_";
				break;
			case 'S':
				fileName = "Sales-Order_";
				break;
			default:
				fileName = null;
				break;
			}

			fileName = fileName + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
			encodedCSVFileMap.put(fileName, encodedCSVFile);
		}

		csvWriter.flush();
		csvWriter.close();

	}

	public void createEmailMIMEbodywithCSVFiles() {

		emailBuilder.append("ContentType: multipart/mixed; boundary=\"" + boundary + "\"" + CRLF + CRLF);
		emailBuilder.append("--" + boundary + CRLF + "Content-Type: text/plain; charset=UTF-8" + CRLF
				+ "Content-Disposition: inline" + CRLF + CRLF + mailContent + CRLF + CRLF);

		for (int fileCount = 0; fileCount < encodedCSVFileMap.size(); fileCount++) {

			emailBuilder.append("--" + contentType + CRLF + "Content-Type: text/csv; name=\""
					+ encodedCSVFileMap.keySet().toArray()[fileCount] + "\"" + CRLF
					+ "Content-Disposition: attachment; filename=\"" + encodedCSVFileMap.keySet().toArray()[fileCount]
					+ "\"" + CRLF + "Content-Transfer-Encoding: base64" + CRLF);

			emailBuilder.append(encodedCSVFileMap.get(encodedCSVFileMap.keySet().toArray()[fileCount]) + CRLF + CRLF);
		}

		emailBuilder.append("--" + contentType + "--" + CRLF);

	}

}
