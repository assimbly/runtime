package org.assimbly.brokerrest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * REST controller for managing Broker.
 */
@RestController
@RequestMapping("/api")
public class BrokerConfigurerRuntime {

	protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
	private ManagedBrokerRuntime broker;

    /**
     * GET  /brokers/:id : get the broker configuration by "id".
     *
     * @param id the id of the brokerDTO to retrieve
     * @return the status (stopped or started) with status 200 (OK) or with status 404 (Not Found)
     */
    @GetMapping("/brokers/{id}/configure")
    public String getConfigurationBroker(@PathVariable("id") Long id, @RequestParam("brokerType") String brokerType) {
        log.debug("REST request to get configuration of Broker : {}", id);

        String configuration = "unknown";

        try {
            configuration = broker.getConfiguration(brokerType);
        } catch (Exception e) {
            log.error("Can't get status", e);
        }

        return configuration;
    }


    /**
     * POST  /brokers/:id : set the broker configuration by "id" and "configurationFile".
     *
     * @param id the id of the brokerDTO to retrieve
     * @return the status (stopped or started) with status 200 (OK) or with status 404 (Not Found)
     * @throws Exception
     */
    @PostMapping(path = "/brokers/{id}/configure")
    public ResponseEntity<String> setConfigurationBroker(@PathVariable("id") Long id, @RequestParam("brokerType") String brokerType, @RequestParam("brokerConfigurationType") String brokerConfigurationType, @RequestBody(required = false) String brokerConfiguration) throws Exception {
        log.debug("REST request to set configuration of Broker : {}", id);

       	try {
       		String result = broker.setConfiguration(brokerType,brokerConfigurationType, brokerConfiguration);
            if(result.equals("configuration set")) {
            	return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, "text", "setConfiguration", result);
            }else {
            	return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, "text", "setConfiguration", result);
            }
   		} catch (Exception e) {
   			return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, "text", "setConfiguration", e.getMessage());
   		}

    }

}
