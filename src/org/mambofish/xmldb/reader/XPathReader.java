package org.mambofish.xmldb.reader;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XPathReader {

	private String xmlFile;

	private Document xmlDocument;

	private XPath xPath;

	public XPathReader(String xmlFile) {
		this.xmlFile = xmlFile;
		initObjects();
	}

	private void initObjects() {

		try {
			File f = new File(xmlFile);
			if (f.exists() == false) {
				f.createNewFile();
				PrintStream p = new PrintStream(xmlFile);
				p.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
				p.println("<List>");
				p.println("</List>");
				p.close();
			}
			xmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
			xPath = XPathFactory.newInstance().newXPath();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (SAXException ex) {
			ex.printStackTrace();
		} catch (ParserConfigurationException ex) {
			ex.printStackTrace();
		}
	}

	public Object read(String expression, QName returnType) {
		try {
			XPathExpression xPathExpression = xPath.compile(expression);
			return xPathExpression.evaluate(xmlDocument, returnType);
		} catch (XPathExpressionException ex) {
			ex.printStackTrace();
			return null;
		}
	}
}