package org.assimbly.dil.blocks.processors;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import org.apache.camel.*;
import org.apache.camel.spi.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;

//set endpoint by its routing rule
public class RoutingRulesProcessor implements Processor {

	protected Logger log = LoggerFactory.getLogger(getClass());

	public void process(Exchange exchange) throws Exception {

		String routingRules = exchange.getProperty("routingRules", String.class);
		String defaultEndpoint = exchange.getProperty("defaultEndpoint", String.class);

		String endpoint = "";

		if (routingRules != null) {
			if (routingRules.contains("#|#")) {
				String[] routingRulesSplitted = routingRules.split("#\\|#");

				for (String routingRule : routingRulesSplitted) {
					endpoint = evaluateRule(routingRule, exchange);
					if(!endpoint.isEmpty()){
						break;
					}
				}
			}else{
				endpoint = evaluateRule(routingRules, exchange);
			}
		}

		if(endpoint.isEmpty()){
			endpoint = defaultEndpoint;
		}

		exchange.setProperty("endpoint", endpoint);

	}

	public String evaluateRule(String rule, Exchange exchange) throws SaxonApiException {

		String[] routingRuleSplitted = rule.split("#;#");
		if (routingRuleSplitted != null && routingRuleSplitted.length == 3) {
			String language = routingRuleSplitted[0];
			String expression = routingRuleSplitted[1];
			String endpoint = routingRuleSplitted[2];
			boolean result = false;

			if (language == null || language.equalsIgnoreCase("constant")) {
				result = true;
			} else if (language.equalsIgnoreCase("xpath")) {

				StringReader body = new StringReader(exchange.getMessage().getBody(String.class));
				net.sf.saxon.s9api.Processor processor = new net.sf.saxon.s9api.Processor(false);
				XdmNode xdm = processor.newDocumentBuilder().build(new StreamSource(body));
				XdmValue xdmResult = processor.newXPathCompiler().evaluate(expression, xdm);

				String resultAsString = xdmResult.stream().asString();

				if(resultAsString.equalsIgnoreCase("true")||resultAsString.equalsIgnoreCase("false")){
				    result = Boolean.parseBoolean(resultAsString);
				}

			} else {
				Language resolvedLanguage = exchange.getContext().resolveLanguage(language);
				Expression expressionCreated = resolvedLanguage.createExpression(expression);

				result = expressionCreated.evaluate(exchange, Boolean.class);
			}

			if (result) {
				return endpoint;
			}

		} else {
			log.warn("Invalid routing rule: " + rule);
		}

		return "";

	}

}