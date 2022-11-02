package org.assimbly.dil.transpiler;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.BasicConfigurationBuilder;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.commons.configuration2.tree.xpath.XPathExpressionEngine;
import org.assimbly.dil.transpiler.marshalling.Marshall;
import org.assimbly.dil.transpiler.marshalling.Unmarshall;
import org.assimbly.dil.transpiler.transform.Transform;
import org.assimbly.util.IntegrationUtil;
import org.assimbly.docconverter.DocConverter;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XMLFileConfiguration {

	private TreeMap<String, String> properties;
	private List<TreeMap<String, String>> propertiesList;

	private String xmlFlowConfiguration;

	private XMLConfiguration conf;

	private Document doc;

	public List<TreeMap<String, String>> getFlowConfigurations(String integrationId, String xml) throws Exception {

		propertiesList = new ArrayList<>();
		Document doc = DocConverter.convertStringToDoc(xml);

		List<String> flowIds = getFlowIds(integrationId,doc);

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
		Document doc = DocConverter.convertUriToDoc(uri);

		List<String> flowIds = getFlowIds(integrationId,doc);

		for(String flowId : flowIds){
			TreeMap<String, String> flowConfiguration = getFlowConfiguration(flowId, uri);

			if(flowConfiguration!=null) {
				propertiesList.add(flowConfiguration);
			}
		}

		return propertiesList;

	}

	public TreeMap<String, String> getFlowConfiguration(String flowId, String xml) throws Exception {
		
		if(!xml.endsWith("</dil>")){
			xml = Transform.transformToDil(xml);
		}
		
		DocumentBuilder docBuilder = setDocumentBuilder("dil.xsd");

		conf = new BasicConfigurationBuilder<>(XMLConfiguration.class).configure(new Parameters().xml()
				.setFileName("dil.xml")
				.setDocumentBuilder(docBuilder)
				.setSchemaValidation(true)
				.setExpressionEngine(new XPathExpressionEngine())
		).getConfiguration();

		
		FileHandler fh = new FileHandler(conf);

		fh.load(DocConverter.convertStringToStream(xml));
	
		properties = new Unmarshall().getProperties(conf,flowId);

		//IntegrationUtil.printTreemap(properties);

		return properties;

	}

	public TreeMap<String, String> getFlowConfiguration(String flowId, URI uri) throws Exception {

		String scheme = uri.getScheme();
		//load uri to configuration
		Parameters params = new Parameters();

		DocumentBuilder docBuilder = setDocumentBuilder("dil.xsd");

		if(scheme.startsWith("sonicfs")) {

			URL Url = uri.toURL();

			InputStream is = Url.openStream();

			conf = new BasicConfigurationBuilder<>(XMLConfiguration.class).configure(params.xml()).getConfiguration();
			FileHandler fh = new FileHandler(conf);
			fh.load(is);

		}else if (scheme.startsWith("file")) {

			File xml = new File(uri.getRawPath());

			FileBasedConfigurationBuilder<XMLConfiguration> builder =
					new FileBasedConfigurationBuilder<XMLConfiguration>(XMLConfiguration.class)
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

			URL Url = uri.toURL();

			FileBasedConfigurationBuilder<XMLConfiguration> builder =
					new FileBasedConfigurationBuilder<XMLConfiguration>(XMLConfiguration.class)
							.configure(params.xml()
									.setURL(Url)
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

		properties = new Unmarshall().getProperties(conf,flowId);

		return properties;
	}

	public String createConfiguration(String integrationId, List<TreeMap<String, String>> configurations) throws Exception {

		if(configurations == null || configurations.isEmpty()) {
			return "Error: Empty configuration (no configuration is set/running)";
		}else {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			doc = docBuilder.newDocument();

			doc = new Marshall().setProperties(doc,integrationId,configurations);

			String xmlConfiguration = DocConverter.convertDocToString(doc);

			return xmlConfiguration;

		}

	}


	public String createFlowConfiguration(TreeMap<String, String> configuration) throws Exception {

		if(configuration == null || configuration.isEmpty()) {
			return "Error: Empty configuration (no configuration is set/running)";
		}

		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		doc = docBuilder.newDocument();

		doc = new Marshall().setProperties(doc,"live",configuration);

		if(doc!=null) {
			xmlFlowConfiguration = DocConverter.convertDocToString(doc);
		}else {
			xmlFlowConfiguration = "Error: Can't create configuration";
		}

		return xmlFlowConfiguration;
	}

	private DocumentBuilder setDocumentBuilder(String schemaFilename) throws SAXException, ParserConfigurationException {

		URL schemaUrl = this.getClass().getResource("/" + schemaFilename);
		Schema schema = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1").newSchema(schemaUrl);

		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();

		docBuilderFactory.setSchema(schema);

		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		//if you want an exception to be thrown when there is invalid xml document,
		//you need to set your own ErrorHandler because the default
		//behavior is to just print an error message.
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


	public static class MapEntryConverter implements Converter {

		public boolean canConvert(Class clazz) {
			return AbstractMap.class.isAssignableFrom(clazz);
		}

		public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {

			AbstractMap map = (AbstractMap) value;
			for (Object obj : map.entrySet()) {
				Map.Entry entry = (Map.Entry) obj;
				writer.startNode(entry.getKey().toString());
				Object val = entry.getValue();
				if ( null != val ) {
					writer.setValue(val.toString());
				}
				writer.endNode();
			}

		}

		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {

			Map<String, String> map = new HashMap<String, String>();

			while(reader.hasMoreChildren()) {
				reader.moveDown();

				String key = reader.getNodeName(); // nodeName aka element's name
				String value = reader.getValue();
				map.put(key, value);

				reader.moveUp();
			}

			return map;
		}

	}

}

