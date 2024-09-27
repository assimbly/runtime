package org.assimbly.dil.blocks.processors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.spi.Language;


public class ExceptionAsJsonProcessor {

	public String process(Exchange exchange) throws Exception {

		Language resolvedLanguage = exchange.getContext().resolveLanguage("simple");
		Expression expression = resolvedLanguage.createExpression("${exception}");
		String result = expression.evaluate(exchange, String.class);

		return result;

	}

}
