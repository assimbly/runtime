package org.assimbly.dil.blocks.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.converter.stream.InputStreamCache;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class InputStreamToStringProcessor implements Processor {

    public void process(Exchange exchange) throws Exception {

        Object body = exchange.getIn().getBody();
        if (body instanceof InputStreamCache) {
            InputStreamCache inputStreamCache = (InputStreamCache) body;
            String bodyAsString = new BufferedReader(new InputStreamReader(inputStreamCache, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            // Reset the input stream so it can be read again by other processors or routes
            inputStreamCache.reset();
            // Set the body as string
            exchange.getIn().setBody(bodyAsString);
        }

    }

}