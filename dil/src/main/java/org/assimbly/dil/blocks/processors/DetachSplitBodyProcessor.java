package org.assimbly.dil.blocks.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;

public class DetachSplitBodyProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {

        Node original = exchange.getMessage().getBody(Node.class);

        if (original == null) {
            return;
        }

        Document newDocument =
                DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .newDocument();

        Node clone = newDocument.importNode(original, true);
        newDocument.appendChild(clone);

        exchange.getMessage().setBody(clone);
    }
}