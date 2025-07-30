package org.assimbly.dil.loader;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowLoaderReport {

	protected Logger log = LoggerFactory.getLogger(getClass());
	private final String flowId;
	private final String flowName;
	private String report;
	private final JSONObject json;
	private final JSONArray steps;
	private int loaded;
	private int loadedSuccess;
	private int loadedError;
	private final JSONObject flow;
	private final JSONObject stepsLoaded;
	private final long startTime;

	public FlowLoaderReport(String flowId, String flowName) {
        log.info("initialize flow report | flowid={}", flowId);

		this.flowId = flowId;
		this.flowName = flowName;
		startTime = System.currentTimeMillis();
		json = new JSONObject();
		flow = new JSONObject();
		stepsLoaded = new JSONObject();
		steps = new JSONArray();
	}

	public void finishReport(String event, String version, String message){

		long time = System.currentTimeMillis() - startTime;

		flow.put("id",flowId);
		flow.put("name",flowName);
		flow.put("event",event);
		flow.put("message",message);
		flow.put("version",version);
		flow.put("time",time + " milliseconds");

		if(stepsLoaded!=null && loaded > 0){
			stepsLoaded.put("total", loaded);
			stepsLoaded.put("successfully", loadedSuccess);
			stepsLoaded.put("failed", loadedError);

			flow.put("stepsLoaded",stepsLoaded);
			flow.put("steps", steps);
		}

		json.put("flow", flow);

		report = json.toString(4);

		if(loaded == loadedSuccess){
            log.info("Flow loaded successfully | flowid={} | time={} milliseconds\n\n{}", flowId, time, report);
		}else{
            log.error("Flow failed to load | flowid={} | time={} milliseconds\n\n{}", flowId, time, report);
		}

	}

	public void setStep(String stepId, String stepUri, String stepType, String stepStatus, String message, String trace){

		loaded += 1;

		String id;
		if(stepId.contains("-")){
			id = StringUtils.substringAfterLast(stepId, "-");
		}else{
			id = stepId;
		}

		JSONObject step = new JSONObject();
		step.put("id", id);
		if(stepUri!=null){
			step.put("uri", stepUri);
		}
		step.put("type", stepType);

		if(stepStatus.equalsIgnoreCase("error")){
			step.put("status", "error");
			step.put("errorMessage", message);
			if(trace!=null){
				step.put("trace", trace);
			}
			loadedError = loadedError + 1;
		}else{
			step.put("status", stepStatus);
			loadedSuccess = loadedSuccess + 1;
		}
		steps.put(step);

	}

	public String getFlowId(){
		return flowId;
	}

	public String getReport(){
		return report;
	}

	public void logResult(String event){
		//logging
		if(loaded == loadedSuccess) {
			if(loadedSuccess == 1){
                log.info("{} step loaded successfully | flowid={}", loadedSuccess, flowId);
			}else if(loadedSuccess > 1){
                log.info("{} steps loaded successfully | flowid={}", loadedSuccess, flowId);
			}
            log.info("Start flow | name={} | flowid={}", flowName, flowId);
		}else{
			if(loadedError == 1){
                log.error("{} step failed to load | flowid={}", loadedError, flowId);
			}else if(loadedError > 1){
                log.error("{} steps failed to load | flowid={}", loadedError, flowId);
			}
            log.error("Event={} | name={} | flowid={}", event, flowName, flowId);
		}

	}

}