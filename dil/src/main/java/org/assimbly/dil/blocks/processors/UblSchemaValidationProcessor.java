package org.assimbly.dil.blocks.processors;

import com.helger.io.resource.ClassPathResource;
import com.helger.ubl21.UBL21Marshaller;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// validates a UBL 2.1 document against the schema of its document type (taken from the root element),
// using the schemas shipped in ph-ubl21. camel-ubl itself reads documents without schema validation.
public class UblSchemaValidationProcessor implements Processor {

	private final Map<String, Schema> schemas = new ConcurrentHashMap<>();

	public void process(Exchange exchange) throws Exception {

		byte[] document = exchange.getMessage().getMandatoryBody(byte[].class);

		String documentType = getRootElement(document);

		Validator validator = getSchema(documentType).newValidator();
		validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		validator.validate(new StreamSource(new ByteArrayInputStream(document)));

	}

	private Schema getSchema(String documentType) {
		return schemas.computeIfAbsent(documentType, type -> {
			Source[] xsds = getXsds(type).stream()
					.map(xsd -> new StreamSource(xsd.getAsURL().toExternalForm()))
					.toArray(Source[]::new);
			try {
				// the JDK implementation: Xerces on the classpath doesn't support the access properties
				SchemaFactory factory = SchemaFactory.newDefaultInstance();
				// the UBL schemas import each other from inside the jars
				factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "jar,file");
				factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
				return factory.newSchema(xsds);
			} catch (Exception e) {
				throw new IllegalStateException("Could not load UBL 2.1 schema for " + type, e);
			}
		});
	}

	// ph-ubl strips the schemaLocation of shared imports, so take the full list of XSDs it uses for the document type
	@SuppressWarnings("unchecked")
	private static List<ClassPathResource> getXsds(String documentType) {
		try {
			Method method = UBL21Marshaller.class.getMethod("getAll" + documentType + "XSDs");
			return (List<ClassPathResource>) method.invoke(null);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("Not a UBL 2.1 document: unknown document type '" + documentType + "'");
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Could not get UBL 2.1 schemas for " + documentType, e);
		}
	}

	private static String getRootElement(byte[] document) throws Exception {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

		XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(document));
		try {
			while (reader.hasNext()) {
				if (reader.next() == XMLStreamConstants.START_ELEMENT) {
					return reader.getLocalName();
				}
			}
		} finally {
			reader.close();
		}

		throw new IllegalArgumentException("Not a UBL 2.1 document: no root element");
	}

}
