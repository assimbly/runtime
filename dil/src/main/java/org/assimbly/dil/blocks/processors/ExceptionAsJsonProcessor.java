package org.assimbly.dil.blocks.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.spi.Language;


public class ExceptionAsJsonProcessor {

	public String process(Exchange exchange) throws Exception {

		Language resolvedLanguage = exchange.getContext().resolveLanguage("simple");
		Expression expression = resolvedLanguage.createExpression("${exception}");

		return expression.evaluate(exchange, String.class);

	}

}
