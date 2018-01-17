package com.sap.pi.mapping.xmlToCsvWithbase64;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
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
	String mailContent = "Dear User," + CRLF + "PFA the required Shipment related files for the Warehouse" + CRLF + CRLF
			+ CRLF + "Regards," + CRLF + "SAP PI team";

	StringWriter csvWriter = new StringWriter();
	StringBuilder emailBuilder = new StringBuilder();
	Map<String, String> csvFileMap = new TreeMap<String, String>();
	Map<String, String> base64ZipFileMap = new TreeMap<String, String>();

	public static void main(String args[]) throws ParserConfigurationException, SAXException, IOException,
			TransformerFactoryConfigurationError, TransformerException {
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

		String emailType = arg0.getInputParameters().getString("EmailType");

		try {
			getTrace().addInfo("Transforming XML to String and adding it to StringWriter Object");
			transformXMLtoStringWriter(inputStream);

			getTrace().addInfo(
					"Separating the csv files from the string writer and converting them into base64 encoded files.");
			separateCSVFilesWithNames();

			if (emailType.equalsIgnoreCase("csv")) {
				getTrace().addInfo("Adding the base64 encoded files to a Map with filename as key");
				encodeCSVFilesInMap();
				getTrace().addInfo("Creating the mail MIME body with encoded csv files as attachments");
				createEmailMIMEbodywithCSVFiles();

			}

			if (emailType.equalsIgnoreCase("zip")) {
				getTrace().addInfo("Zipping the csv files into invoice.zip, purchase-order.zip and sales-order.zip");
				zipCSVFiles();
				getTrace().addInfo("Creating the mail body with encoded zip files as attachment");
				createEmailMIMEbodywithZipFiles();
			}

			getTrace().addInfo(
					"Writing the entire MIME content to the outputstream and setting the content type of the outputheader");
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

	public void separateCSVFilesWithNames() throws IOException {
		String csvFileArray[];
		String fileName;

		csvFileArray = csvWriter.toString().split(CRLF + CRLF);
		for (int fileCount = 1; fileCount < csvFileArray.length; fileCount++) {
			String csvFile = csvFileArray[fileCount].replaceFirst(CRLF, "");
			char fileType = csvFile.charAt(0);
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
				throw new IOException();
			}

			fileName = fileName + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
			csvFileMap.put(fileName, csvFile);
		}

		csvWriter.flush();
		csvWriter.close();

	}

	public void encodeCSVFilesInMap() {
		for (int fileCount = 0; fileCount < csvFileMap.size(); fileCount++) {
			String fileNameKey = (String) csvFileMap.keySet().toArray()[fileCount];
			String base64csvFile = Base64.getEncoder().encodeToString(csvFileMap.get(fileNameKey).getBytes());
			csvFileMap.put(fileNameKey, base64csvFile);
		}
	}

	public void zipCSVFiles() throws IOException {

		ByteArrayOutputStream invoicebaos = new ByteArrayOutputStream();
		ZipOutputStream invoicezos = new ZipOutputStream(new BufferedOutputStream(invoicebaos));

		ByteArrayOutputStream pobaos = new ByteArrayOutputStream();
		ZipOutputStream purchasezos = new ZipOutputStream(new BufferedOutputStream(pobaos));

		ByteArrayOutputStream sobaos = new ByteArrayOutputStream();
		ZipOutputStream saleszos = new ZipOutputStream(new BufferedOutputStream(sobaos));

		for (int fileCount = 0; fileCount < csvFileMap.size(); fileCount++) {
			String fileNameKey = (String) csvFileMap.keySet().toArray()[fileCount];
			File filetoZip = new File(fileNameKey);
			FileInputStream fis = new FileInputStream(filetoZip);
			ZipEntry zipEntry = new ZipEntry(filetoZip.getName());
			char fileType = fileNameKey.charAt(0);

			switch (fileType) {
			case 'I':
				invoicezos.putNextEntry(zipEntry);
				byte[] invoiceBytes = new byte[1024];
				int invoiceLength;
				while ((invoiceLength = fis.read(invoiceBytes)) >= 0) {
					invoicezos.write(invoiceBytes, 0, invoiceLength);
				}
				fis.close();
				break;

			case 'P':
				purchasezos.putNextEntry(zipEntry);
				byte[] poBytes = new byte[1024];
				int poLength;
				while ((poLength = fis.read(poBytes)) >= 0) {
					purchasezos.write(poBytes, 0, poLength);
				}
				fis.close();
				break;

			case 'S':
				saleszos.putNextEntry(zipEntry);
				byte[] soBytes = new byte[1024];
				int soLength;
				while ((soLength = fis.read(soBytes)) >= 0) {
					saleszos.write(soBytes, 0, soLength);
				}
				fis.close();
				break;

			default:
				fis.close();
				throw new IOException();
			}

			invoicezos.close();
			purchasezos.close();
			saleszos.close();

		}

		String base64InvoiceZip = Base64.getMimeEncoder().encodeToString(invoicebaos.toByteArray());
		String base64PurchaseZip = Base64.getMimeEncoder().encodeToString(pobaos.toByteArray());
		String base64SalesZip = Base64.getMimeEncoder().encodeToString(sobaos.toByteArray());

		base64ZipFileMap.put("Invoice_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".zip",
				base64InvoiceZip);
		base64ZipFileMap.put(
				"Purchase-Order_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".zip",
				base64PurchaseZip);
		base64ZipFileMap.put(
				"Sales-Order_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".zip",
				base64SalesZip);

	}

	public void createEmailMIMEbodywithCSVFiles() {

		emailBuilder.append("ContentType: multipart/mixed; boundary=\"" + boundary + "\"" + CRLF + CRLF);
		emailBuilder.append("--" + boundary + CRLF + "Content-Type: text/plain; charset=UTF-8" + CRLF
				+ "Content-Disposition: inline" + CRLF + CRLF + mailContent + CRLF + CRLF);

		for (int fileCount = 0; fileCount < csvFileMap.size(); fileCount++) {

			emailBuilder.append("--" + contentType + CRLF + "Content-Type: text/csv; name=\""
					+ csvFileMap.keySet().toArray()[fileCount] + "\"" + CRLF
					+ "Content-Disposition: attachment; filename=\"" + csvFileMap.keySet().toArray()[fileCount] + "\""
					+ CRLF + "Content-Transfer-Encoding: base64" + CRLF);

			emailBuilder.append(csvFileMap.get(csvFileMap.keySet().toArray()[fileCount]) + CRLF + CRLF);
		}

		emailBuilder.append("--" + contentType + "--" + CRLF);

	}

	public void createEmailMIMEbodywithZipFiles() {

		emailBuilder.append("ContentType: multipart/mixed; boundary=\"" + boundary + "\"" + CRLF + CRLF);
		emailBuilder.append("--" + boundary + CRLF + "Content-Type: text/plain; charset=UTF-8" + CRLF
				+ "Content-Disposition: inline" + CRLF + CRLF + mailContent + CRLF + CRLF);

		for (int fileCount = 0; fileCount < base64ZipFileMap.size(); fileCount++) {

			emailBuilder.append("--" + contentType + CRLF + "Content-Type: application/zip; name=\""
					+ base64ZipFileMap.keySet().toArray()[fileCount] + "\"" + CRLF
					+ "Content-Disposition: attachment; filename=\"" + base64ZipFileMap.keySet().toArray()[fileCount]
					+ "\"" + CRLF + "Content-Transfer-Encoding: base64" + CRLF);

			emailBuilder.append(base64ZipFileMap.get(base64ZipFileMap.keySet().toArray()[fileCount]) + CRLF + CRLF);
		}

		emailBuilder.append("--" + contentType + "--" + CRLF);

	}

}
