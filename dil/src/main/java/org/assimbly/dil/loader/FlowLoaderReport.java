package org.assimbly.dil.loader;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FlowLoaderReport {

	protected Logger log = LoggerFactory.getLogger(getClass());

	String report;
	private boolean isFlowLoaded = true;
	private JSONObject json;
	private JSONArray steps;
	private int loaded = 0;
	private int loaded_success = 0;
	private int loaded_error = 0;
	private JSONObject flow;
	private JSONObject stepsLoaded;


	public void initReport(String flowId, String flowName){

		log.info("Start flow | name=" + flowName + " | id=" + flowId);

		json = new JSONObject();
		flow = new JSONObject();
		stepsLoaded = new JSONObject();
		steps = new JSONArray();

	}

	public void finishReport(String flowId, String flowName){

		flow.put("id",flowId);
		flow.put("name",flowName);

		stepsLoaded.put("total", loaded);
		stepsLoaded.put("successfully", loaded_success);
		stepsLoaded.put("failed", loaded_error);

		flow.put("stepsLoaded",stepsLoaded);
		flow.put("steps", steps);

		json.put("flow", flow);

		report = json.toString(4);

		if(loaded == loaded_success) {
			if(loaded_success == 1){
				log.info(loaded_success + " step loaded succesfully");
			}else{
				log.info(loaded_success + " steps loaded succesfully");
			}
			log.info("Start flow | name=" + flowName + " | id=" + flowId);
		}else{
			if(loaded_error == 1){
				log.error(loaded_error + " step failed to load");
			}else{
				log.error(loaded_error + " steps failed to load");
			}
			log.error("Start flow failed | name=" + flowName + " | id=" + flowId);
		}
	}

	public void setStep(String stepId, String stepUri, String stepType, String stepStatus, String message){

		loaded = loaded + 1;

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
			loaded_error = loaded_error + 1;
			isFlowLoaded = false;
		}else{
			step.put("status", stepStatus);
			loaded_success = loaded_success + 1;
		}
		steps.put(step);

	}

	public String getReport(){
		return report;
	}


}