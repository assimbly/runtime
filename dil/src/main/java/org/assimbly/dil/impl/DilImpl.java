package org.assimbly.dil.impl;

import org.assimbly.dil.Dil;
import org.assimbly.dil.transpiler.JSONFileConfiguration;
import org.assimbly.dil.transpiler.XMLFileConfiguration;
import org.assimbly.dil.transpiler.YAMLFileConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

public abstract class DilImpl implements Dil {

	protected Logger log = LoggerFactory.getLogger(getClass());

	private final List<TreeMap<String, String>> properties = new ArrayList<>();
	private TreeMap<String, String> flowProperties;

	public void transpile(String flowId, String mediaType, String configuration) throws Exception {

		try {
			if(mediaType.toLowerCase().contains("xml")) {
				flowProperties = convertXMLToFlowConfiguration(flowId, configuration);
			}else if(mediaType.toLowerCase().contains("json")) {
				flowProperties = convertJSONToFlowConfiguration(flowId, configuration);
			}else {
				flowProperties = convertYAMLToFlowConfiguration(flowId, configuration);
			}

			setFlowConfiguration(flowProperties);

		} catch (Exception e) {
			log.error("Failed to transpile code. Reason:", e);
		}
	}

	public void setFlowConfiguration(TreeMap<String,String> configuration) throws Exception {

		removeFlowConfigurationIfExist(configuration);

		this.properties.add(configuration);

	}

	public void removeFlowConfigurationIfExist(TreeMap<String,String> configuration) throws Exception {

		String newId = configuration.get("id");

		Iterator<TreeMap<String, String>> i = this.properties.iterator();

		while (i.hasNext()) {
			TreeMap<String, String> currentConfiguration = i.next(); // must be called before you can call i.remove()
			String oldId= currentConfiguration.get("id");
			if(newId.equals(oldId)){
				i.remove();
			}
		}
	}

	//--> convert methods
	public TreeMap<String, String> convertXMLToFlowConfiguration(String flowId, String configuration) throws Exception {
		return new XMLFileConfiguration().getFlowConfiguration(flowId, configuration);
	}

	public TreeMap<String, String> convertJSONToFlowConfiguration(String flowId, String configuration) throws Exception {
		return new JSONFileConfiguration().getFlowConfiguration(flowId, configuration);
	}

	public TreeMap<String, String> convertYAMLToFlowConfiguration(String flowId, String configuration) throws Exception {
		return new YAMLFileConfiguration().getFlowConfiguration(flowId, configuration);
	}

}
