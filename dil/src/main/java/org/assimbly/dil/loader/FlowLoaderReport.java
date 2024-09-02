package org.assimbly.dil.loader;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;


public class FlowLoaderReport {

	protected Logger log = LoggerFactory.getLogger(getClass());
	private String report;
	private JSONObject json;
	private JSONArray steps;
	private int loaded;
	private int loadedSuccess;
	private int loadedError;
	private JSONObject flow;
	private JSONObject stepsLoaded;
	private long startTime;
	private long endTime;


	public void initReport(String flowId, String flowName, String event){

		String eventCapitalized = StringUtils.capitalize(event);
		log.info(eventCapitalized + " flow | flowid=" + flowId);

		startTime = System.currentTimeMillis();
		json = new JSONObject();
		flow = new JSONObject();
		stepsLoaded = new JSONObject();
		steps = new JSONArray();

	}

	public void finishReport(String flowId, String flowName, String event, String version, String environment, String message){

		endTime = System.currentTimeMillis();
		long time = endTime - startTime;

		flow.put("id",flowId);
		flow.put("name",flowName);
		flow.put("event",event);
		flow.put("message",message);
		flow.put("version",version);
		//flow.put("environment",environment);
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

	}

	public void setStep(String stepId, String stepUri, String stepType, String stepStatus, String message){

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
			loadedError = loadedError + 1;
		}else{
			step.put("status", stepStatus);
			loadedSuccess = loadedSuccess + 1;
		}
		steps.put(step);

	}

	public String getReport(){
		return report;
	}

	public void logResult(String flowId, String flowName, String event){
		//logging
		if(loaded == loadedSuccess) {
			if(loadedSuccess == 1){
				log.info(loadedSuccess + " step loaded successfully | flowid=" + flowId);
			}else if(loadedSuccess > 1){
				log.info(loadedSuccess + " steps loaded successfully | flowid=" + flowId);
			}
			log.info("Start flow | name=" + flowName + " | flowid=" + flowId);
		}else{
			if(loadedError == 1){
				log.error(loadedError + " step failed to load | flowid=" + flowId);
			}else if(loadedError > 1){
				log.error(loadedError + " steps failed to load | flowid=" + flowId);
			}
			log.error("Event=" + event + " | name=" + flowName + " | flowid=" + flowId);
		}

	}

}