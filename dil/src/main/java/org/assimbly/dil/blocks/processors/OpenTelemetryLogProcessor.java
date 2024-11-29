package org.assimbly.dil.blocks.processors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;


public class OpenTelemetryLogProcessor implements Processor {

	public void process(Exchange exchange) throws Exception {

		String json = format(exchange);

		System.out.println(json);

	}


	private String format(Exchange exchange) throws JsonProcessingException {

		Object inBody = exchange.getIn().getBody();
		Map<String, Object> exchangeMap = new HashMap<>();
		exchangeMap.put("ExchangeId", exchange.getExchangeId());
		exchangeMap.put("ExchangePattern", exchange.getPattern().toString());
		exchangeMap.put("Body", inBody != null ? inBody.toString() : "null");
		exchangeMap.put("Headers", exchange.getIn().getHeaders());

		Instant now = Instant.now();

		// Convert Instant to string using a formatter
		String formattedTime = DateTimeFormatter
				.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
				.withZone(ZoneOffset.UTC)
				.format(now);

		Map<String, Object> map = new HashMap<>();
		map.put("timestamp", formattedTime);
		map.put("logLevel", "INFO");
		map.put("serviceName", exchange.getFromRouteId());
		map.put("message", exchange.getFromRouteId());
		map.put("attributes",exchangeMap);

		ObjectMapper mapper = new ObjectMapper();
		String jsonString = mapper.writeValueAsString(map);

		return jsonString;
	}

}