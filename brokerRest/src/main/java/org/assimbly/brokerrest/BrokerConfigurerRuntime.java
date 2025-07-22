package org.assimbly.brokerrest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for configuring the broker.
 */
@RestController
@RequestMapping("/api")
public class BrokerConfigurerRuntime {

	protected Logger log = LoggerFactory.getLogger(getClass());

    private final ManagedBrokerRuntime broker;

    public BrokerConfigurerRuntime(ManagedBrokerRuntime broker) {
        this.broker = broker;
    }

    /**
     * GET  /brokers/:id : get the broker configuration by "id".
     *
     * @param id the id of the brokerDTO to retrieve
     * @param brokerType, the type of broker: classic or artemis
     * @return the status (stopped or started) with status 200 (OK) or with status 404 (Not Found)
     */
    @GetMapping("/brokers/{id}/configure")
    public String getConfigurationBroker(
            @PathVariable(value = "id") Long id,
            @RequestParam(value = "brokerType") String brokerType
    ) {

        log.debug("event=getConfiguration type=GET message=Request to get the configuration of Broker id={} type={}", id, brokerType);

        String configuration = "unknown";

        try {
            configuration = broker.getConfiguration(brokerType);
        } catch (Exception e) {
            log.error("event=getConfigurationBroker type=GET id={} type={} reason={}", id, brokerType, e.getMessage(), e);
        }

        return configuration;
    }

    /**
     * POST  /brokers/:id : set the broker configuration by "id" and "configurationFile".
     *
     * @param id the id of the brokerDTO to retrieve
     * @param brokerType, the type of broker: classic or artemis
     * @return the status (stopped or started) with status 200 (OK) or with status 404 (Not Found)
     */
    @PostMapping(path = "/brokers/{id}/configure")
    public ResponseEntity<String> setConfigurationBroker(
            @PathVariable(value = "id") Long id,
            @RequestParam(value = "brokerType") String brokerType,
            @RequestParam(value = "brokerConfigurationType") String brokerConfigurationType,
            @RequestBody(required = false) String brokerConfiguration
    ) throws Exception {

        log.debug("event=setConfigurationBroker type=POST message=Request to set the configuration of Broker id={} type={}", id, brokerType);

       	try {
       		String result = broker.setConfiguration(brokerType, brokerConfiguration);
            if(result.equals("success")) {
            	return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, "text", "/brokers/{id}/configure", result);
            }else {
                log.error("event=setConfiguration type=POST id={} type={} reason={}", id, brokerType, result);
                return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, "text", "/brokers/{id}/configure", result);
            }
   		} catch (Exception e) {
            log.error("event=setConfiguration type=POST id={} type={} reason={}", id, brokerType, e.getMessage(), e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, "text", "/brokers/{id}/configure", e.getMessage());
   		}

    }

}