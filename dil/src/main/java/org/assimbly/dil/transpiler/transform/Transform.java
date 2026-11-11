package org.assimbly.dil.transpiler.transform;

import net.sf.saxon.s9api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;

public class Transform {

    static final Logger log = LoggerFactory.getLogger(Transform.class);

    private final XsltTransformer transformer;

    private final Processor processor;

    public Transform(String stylesheet) throws SaxonApiException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        processor = new Processor(false);
        try (InputStream is = classloader.getResourceAsStream(stylesheet)) {
            if (is == null) {
                throw new IllegalStateException("Stylesheet " + stylesheet + " is not found on the classpath");
            }
            XsltCompiler compiler = processor.newXsltCompiler();
            XsltExecutable executable = compiler.compile(new StreamSource(is));
            transformer = executable.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to close stylesheet " + stylesheet, e);
        }
    }

    public String transformToDil(String xml) throws Exception {

        String dilXml = transformXML(xml, transformer, processor);

        log.debug("DIL format:\n\n{}", dilXml);

        return dilXml;

	}

    public String transformXML(String xmlString, XsltTransformer transformer, Processor processor ) throws SaxonApiException {

        DocumentBuilder builder = processor.newDocumentBuilder();

        XdmNode sourceDocument = builder.build(new StreamSource(new StringReader(xmlString)));

        transformer.setInitialContextNode(sourceDocument);

        Serializer out = processor.newSerializer();
        StringWriter stringWriter = new StringWriter();
        out.setOutputWriter(stringWriter);

        transformer.setDestination(out);
        transformer.transform();

        return stringWriter.toString();

    }

}
