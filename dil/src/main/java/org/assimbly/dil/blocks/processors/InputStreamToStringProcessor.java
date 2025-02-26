package org.assimbly.dil.blocks.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.converter.stream.InputStreamCache;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class InputStreamToStringProcessor implements Processor {

    public void process(Exchange exchange) throws Exception {
        Object body = exchange.getIn().getBody();

        if (body instanceof InputStreamCache inputStreamCache) {

            String bodyAsString;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStreamCache, StandardCharsets.UTF_8))) {
                bodyAsString = reader.lines().collect(Collectors.joining("\n"));
            }

            // Reset the input stream so it can be read again by other processors or routes
            inputStreamCache.reset();

            // Set the body as string
            exchange.getIn().setBody(bodyAsString);
        }
    }

}