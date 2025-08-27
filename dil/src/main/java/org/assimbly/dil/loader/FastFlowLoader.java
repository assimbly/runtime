package org.assimbly.dil.loader;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
import org.apache.camel.CamelContext;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.assimbly.dil.model.Dil;
import org.assimbly.dil.model.Link;
import org.assimbly.dil.model.Options;
import org.assimbly.dil.model.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class FastFlowLoader {

	protected Logger log = LoggerFactory.getLogger(getClass());
	private final ObjectMapper mapper = new ObjectMapper().registerModule(new BlackbirdModule());
	private FlowLoaderReport flowLoaderReport;
	private boolean isFlowLoaded = true;

	public String load(String flowId, String configuration, CamelContext context) throws Exception {

		flowLoaderReport = new FlowLoaderReport(flowId, flowId);

		mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
		Dil dil = mapper.readValue(configuration, Dil.class);

		List<Step> steps = getSteps(dil);

		for (Step step : steps) {

			try {
				log.info("Load step:\n\n{}", step);
				String routeId = flowId + "_" + step.id();
				String templateId = getTemplateId(step);
				Map<String, Object> parameters = getParameters(step);
				context.addRouteFromTemplate(routeId, templateId, parameters);
				flowLoaderReport.setStep(step.id(), step.uri(), step.type(), "success", null, null);

			}catch (Exception e) {
				log.error("Failed loading step | stepid={}", step.id());
				flowLoaderReport.setStep(step.id(), step.uri(), step.type(), "error", e.getMessage(), ExceptionUtils.getStackTrace(e));
				isFlowLoaded = false;
			}

		}

		if(isFlowLoaded) {
			flowLoaderReport.finishReport(flowId, "start", "Started flow successfully");
		}else {
			flowLoaderReport.finishReport(flowId, "error", "Failed starting flow");
		}

		return flowLoaderReport.getReport();

	}


	private List<Step> getSteps(Dil dil){

		return dil
				.root()
				.integrations()
				.integration()
				.flows()
				.flow()
				.steps()
				.step();
	}

	private String getTemplateId(Step step){
		String type = step.type();
		String uri = step.uri();
		if(uri.contains(":")){
			int colonIndex = uri.indexOf(':');
			String scheme = colonIndex != -1 ? uri.substring(0, colonIndex) : uri;
			return scheme + "-" + type;
		}

		return uri + "-" + type;

	}

	private Map<String, Object> getParameters(Step step) {

		Options options = step.options();

		Map<String, Object> map;
		if(options!=null){
			map = options.properties();
		}else{
			map = new HashMap<>();
		}

		StringBuilder optionsSB = new StringBuilder();
		boolean first = true;

		for (Map.Entry<String, Object> entry : map.entrySet()) {
			map.put(entry.getKey(), entry.getValue());

			if (first) {
				first = false;
			} else {
				optionsSB.append("&");
			}
			optionsSB.append(entry.getKey()).append("=").append(entry.getValue());
		}
		String optionsString = optionsSB.toString();

		// Build URI and add options
		String baseUri = step.uri();
		if (!optionsString.isEmpty()) {
			map.put("uri", baseUri + "?" + optionsString);
			map.put("options", optionsString);
		} else {
			map.put("uri", baseUri);
		}

		int colonIndex = baseUri.indexOf(':');
		if (colonIndex != -1) {
			map.put("scheme", baseUri.substring(0, colonIndex));
			map.put("path", baseUri.substring(colonIndex + 1));
		} else {
			map.put("scheme", baseUri);
		}

		List<Link> links = step.links().link();
		for (Link link : links) {
			String transport = link.transport();
			String id = link.id();
			String uri = transport + ":" + id;
			map.put(link.bound(), uri);
		}

		return map;
	}


	public String getReport(){
		return flowLoaderReport.getReport();
	}

	public boolean isFlowLoaded(){
		return isFlowLoaded;
	}

}