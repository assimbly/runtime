package org.assimbly.dil.blocks.processors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import java.util.HashMap;
import java.util.Map;


public class SetLogProcessor implements Processor {

	public void process(Exchange exchange) throws Exception {

		JsonExchangeFormatter jsonFormatter = new JsonExchangeFormatter();
		jsonFormatter.setShowAll(true);

		String json = jsonFormatter.format(exchange);

		System.out.println("-------------MyLog --------------------->\n\n" + json);

	}


	private String formatExchangeToString(Exchange exchange) throws JsonProcessingException {

		Object inBody = exchange.getIn().getBody();

		Map<String, Object> map = new HashMap<>();
		map.put("ExchangeId", exchange.getExchangeId());
		map.put("FromRouteId", exchange.getFromRouteId());
		map.put("ExchangePattern", exchange.getPattern().toString());
		map.put("Body", inBody != null ? inBody.toString() : "null");
		map.put("Headers", exchange.getIn().getHeaders());

		ObjectMapper mapper = new ObjectMapper();
		String jsonString = mapper.writeValueAsString(map);

		return jsonString;
	}

}