package org.assimbly.dil;

/**
 * <pre>
 * This interface is meant to transpile DIL to Apache Camel.
 * </pre>
 */
public interface Dil {

	/**
	 * Sets the integration configuration from a string of a specific format (XML,JSON,YAML).
	 * The configuration cleared after a integration is reinitialized.
	 *
	 * @param mediaType     (XML,JSON,YAML)
	 * @param configuration (DIL file in XML, JSON or YAML format)
	 * @throws Exception if configuration can't be set
	 */
    void transpile(String flowId, String mediaType, String configuration) throws Exception;

}