package org.assimbly.dil.transpiler;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.BasicConfigurationBuilder;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.builder.fluent.XMLBuilderParameters;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.commons.configuration2.tree.xpath.XPathExpressionEngine;
import org.assimbly.dil.transpiler.marshalling.Marshall;
import org.assimbly.dil.transpiler.marshalling.Unmarshall;
import org.assimbly.dil.transpiler.transform.Transform;
import org.assimbly.docconverter.DocConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.util.*;

public class XMLFileConfiguration {

	protected static Logger log = LoggerFactory.getLogger(XMLFileConfiguration.class);

	private List<TreeMap<String, String>> propertiesList;

	private XMLConfiguration conf;
	private FileHandler fh;

	private Document doc;

    public XMLFileConfiguration() {
		try {
			initConf();
		}catch (Exception e){
			log.error("Configuration failed",e);
		}
    }

	private void initConf() throws Exception {

		DocumentBuilder docBuilder = setDocumentBuilder();

		XMLBuilderParameters params = new Parameters().xml()
				.setFileName("dil.xml")
				.setDocumentBuilder(docBuilder)
				.setSchemaValidation(true)
				.setExpressionEngine(new XPathExpressionEngine());

		conf = new BasicConfigurationBuilder<>(XMLConfiguration.class).configure(params).getConfiguration();

		fh = new FileHandler(conf);

	}

	public List<TreeMap<String, String>> getFlowConfigurations(String integrationId, String xml) throws Exception {

		propertiesList = new ArrayList<>();
		Document document = DocConverter.convertStringToDoc(xml);

		List<String> flowIds = getFlowIds(integrationId,document);

		for(String flowId : flowIds){

			TreeMap<String, String> flowConfiguration = getFlowConfiguration(flowId, xml);

			if(flowConfiguration!=null) {
				propertiesList.add(flowConfiguration);
			}
		}

		return propertiesList;

	}

	public List<TreeMap<String, String>> getFlowConfigurations(String integrationId, URI uri) throws Exception {

		propertiesList = new ArrayList<>();
		Document document = DocConverter.convertUriToDoc(uri);

		List<String> flowIds = getFlowIds(integrationId,document);

		for(String flowId : flowIds){
			TreeMap<String, String> flowConfiguration = getFlowConfiguration(flowId, uri);

			if(flowConfiguration!=null) {
				propertiesList.add(flowConfiguration);
			}
		}

		return propertiesList;

	}

	public TreeMap<String, String> getFlowConfiguration(String flowId, String xml) throws Exception {

		log.info("Configuration File: {}", xml);

		if(!xml.endsWith("</dil>")){
			Transform transform = new Transform("transform-to-dil.xsl");
			xml = transform.transformToDil(xml);
		}

		fh.load(new StringReader(xml));

		return new Unmarshall().getProperties(conf,flowId);

	}

	public String getRouteConfiguration(String xml) throws Exception {

        log.info("Route Configuration File: {}", xml);

		Transform transform = new Transform("transform-to-route.xsl");
		return transform.transformToDil(xml);

	}

	public TreeMap<String, String> getFlowConfiguration(String flowId, URI uri) throws Exception {

		String scheme = uri.getScheme();

		Parameters params = new Parameters();

		DocumentBuilder docBuilder = setDocumentBuilder();

		if(scheme.startsWith("sonicfs")) {

			URL url = uri.toURL();

			InputStream is = url.openStream();

			conf = new BasicConfigurationBuilder<>(XMLConfiguration.class).configure(params.xml()).getConfiguration();
			fh = new FileHandler(conf);
			fh.load(is);

		}else if (scheme.startsWith("file")) {

			File xml = new File(uri.getRawPath());

			FileBasedConfigurationBuilder<XMLConfiguration> builder =
					new FileBasedConfigurationBuilder<>(XMLConfiguration.class)
							.configure(params.xml()
									.setFileName("dil.xml")
									.setFile(xml)
									.setDocumentBuilder(docBuilder)
									.setSchemaValidation(true)
									.setExpressionEngine(new XPathExpressionEngine())
							);

			// This will throw a ConfigurationException if the XML document does not
			// conform to its Schema.
			conf = builder.getConfiguration();

		}else if (scheme.startsWith("http")) {

			URL url = uri.toURL();

			FileBasedConfigurationBuilder<XMLConfiguration> builder = new FileBasedConfigurationBuilder<>(XMLConfiguration.class)
							.configure(params.xml()
									.setURL(url)
									.setFileName("dil.xml")
									.setDocumentBuilder(docBuilder)
									.setSchemaValidation(true)
									.setExpressionEngine(new XPathExpressionEngine())
							);

			// This will throw a ConfigurationException if the XML document does not
			// conform to its Schema.
			conf = builder.getConfiguration();
		}else {
			throw new Exception("URI scheme for " + uri.getRawPath() + " is not supported");
		}

        return new Unmarshall().getProperties(conf, flowId);
	}

	public String createConfiguration(String integrationId, List<TreeMap<String, String>> configurations) throws Exception {

		if(configurations == null || configurations.isEmpty()) {
			return "Error: Empty configuration (no configuration is set/running)";
		}else {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			doc = docBuilder.newDocument();

			doc = new Marshall().setProperties(doc,integrationId,configurations);

            return DocConverter.convertDocToString(doc);

		}

	}

	public String createFlowConfiguration(TreeMap<String, String> configuration) throws Exception {

		if(configuration == null || configuration.isEmpty()) {
			return "Error: Empty configuration (no configuration is set/running)";
		}

		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		doc = docBuilder.newDocument();

		doc = new Marshall().setProperties(doc, configuration);

		String xmlFlowConfiguration;
		if(doc!=null) {
			xmlFlowConfiguration = DocConverter.convertDocToString(doc);
		}else {
			xmlFlowConfiguration = "Error: Can't create configuration";
		}

		return xmlFlowConfiguration;
	}

	private DocumentBuilder setDocumentBuilder() throws SAXException, ParserConfigurationException {

		URL schemaUrl = this.getClass().getResource("/" + "dil.xsd");
		Schema schema = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1").newSchema(schemaUrl);

		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();

		docBuilderFactory.setSchema(schema);

		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();

		docBuilder.setErrorHandler(new ErrorHandler() {
			@Override
			public void warning(SAXParseException exception) throws SAXException {
				throw exception;
			}

			@Override
			public void error(SAXParseException exception) throws SAXException {
				throw exception;
			}

			@Override
			public void fatalError(SAXParseException exception)  throws SAXException {
				throw exception;
			}
		});

		return docBuilder;
	}

	private static List<String> getFlowIds(String integrationId, Document doc)  throws Exception {

		// Create XPath object
		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();
		XPathExpression expr = xpath.compile("/dil/integrations/integration[id=" + integrationId +"]/flows/flow/id/text()");

		// Create list of Ids
		List<String> list = new ArrayList<>();
		NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
		for (int i = 0; i < nodes.getLength(); i++) {
			list.add(nodes.item(i).getNodeValue());
		}

		return list;
	}

}

