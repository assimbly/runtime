package org.assimbly.dil.blocks.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.spi.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
		exchange.removeProperty("routingRules");
		exchange.removeProperty("defaultEndpoint");

	}

	public String evaluateRule(String rule, Exchange exchange) {

		String[] routingRuleSplitted = rule.split("#;#");

		if (routingRuleSplitted.length != 3) {
			log.warn("Invalid routing rule: {}", rule);
			return "";
		}

		boolean result = false;
		String language = routingRuleSplitted[0];
		String expression = routingRuleSplitted[1];
		String endpoint = routingRuleSplitted[2];

		try {
			Language resolvedLanguage = exchange.getContext().resolveLanguage(language);
			Predicate predicate = resolvedLanguage.createPredicate(expression);
			result = predicate.matches(exchange);
		}catch (Exception e){
			log.error("Expression: {} for language {} failed" , expression, language, e);
		}

		if (result) {
			return endpoint;
		}

		return "";

	}

}