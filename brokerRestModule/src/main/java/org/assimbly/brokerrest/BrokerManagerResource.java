package org.assimbly.brokerrest;

import io.swagger.annotations.ApiParam;
import org.assimbly.brokerrest.ManagedBroker;
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
public class BrokerManagerResource {

    private final Logger log = LoggerFactory.getLogger(BrokerManagerResource.class);

    private static final String ENTITY_NAME = "broker";

    @Autowired
	private ManagedBroker broker;

    private String result;

    private static final long id = 0L;

    /**
     * GET  /brokers/:id : get the broker status by "id".
     *
     * @param id, the id of the broker to retrieve
     * @return the status (stopped or started) with status 200 (OK) or with status 404 (Not Found)
     */
    @GetMapping("/brokers/{id}/status")
    public String statusBroker(@PathVariable Long id, @RequestParam String brokerType) {
        log.debug("REST request to get status of Broker : {}", id);

        String status = "stopped";

        try {
            status = broker.getStatus(brokerType);
        } catch (Exception e1) {
            log.error("Can't get status", e1);
        }

        System.out.println("status=" + status);

        return status;
    }


    /**
     * GET  /brokers/:id : get the broker info by "id".
     *
     * @param id, the id of the broker to retrieve
     * @return the status (stopped or started) with status 200 (OK) or with status 404 (Not Found)
     */
    @GetMapping("/brokers/{id}/info")
    public String infoBroker(@PathVariable Long id, @RequestParam String brokerType) {
        log.debug("REST request to get status of Broker : {}", id);

        String info = "unknown";

        try {
            info = broker.getInfo(brokerType);
        } catch (Exception e1) {
            log.error("Can't get status", e1);
        }

        return info;
    }

    /**
     * GET  /brokers/:id : start the broker by "id".
     *
     * @param id the id of the brokerDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the brokerDTO, or with status 404 (Not Found)
     */
    @GetMapping("/brokers/{id}/start")
    public ResponseEntity<String> startBroker(@PathVariable Long id, @RequestParam String brokerType, @RequestParam String brokerConfigurationType) throws Exception {
        log.debug("REST request to start Broker : {}", id);

        try {
   			broker.start(brokerType,brokerConfigurationType);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, "text", "/brokers/{brokerType}/consumers", "started");
        } catch (Exception e) {
        	log.error("Can't start broker", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, "text", "/brokers/{id}/restart", e.getMessage());
    	}

    }

    /**
     * GET  /brokers/:id : restart the broker by "id".
     *
     * @param id the id of the brokerDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the brokerDTO, or with status 404 (Not Found)
     */
    @GetMapping("/brokers/{id}/restart")
    public ResponseEntity<String> restartBroker(@PathVariable Long id, @RequestParam String brokerType, @RequestParam String brokerConfigurationType) throws Exception {
        log.debug("REST request to restart Broker : {}", id);

        try {
   			broker.restart(brokerType,brokerConfigurationType);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, "text", "/brokers/{id}/restart", "restarted");
        } catch (Exception e) {
        	log.error("Can't restart broker", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, "text", "/brokers/{id}/restart", e.getMessage());
    	}


    }


    /**
     * GET  /brokers/:id : stop the broker by "id".
     *
     * @param id the id of the brokerDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the brokerDTO, or with status 404 (Not Found)
     */
    @GetMapping("/brokers/{id}/stop")
    public ResponseEntity<String> stopBroker(@PathVariable Long id, @RequestParam String brokerType) throws Exception {
        log.debug("REST request to stop Broker : {}", id);

        try {
            broker.stop(brokerType);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, "text", "/brokers/{id}/stop", "stopped");
        } catch (Exception e) {
        	log.error("Can't stop broker", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, "text", "/brokers/{id}/stop", e.getMessage());
    	}

    }


    /**
     * GET  /brokers/{brokerType}/connections: get list of all broker connections.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @return list of connections with status 200 (OK) or with status 404 (Not Found)
     */
    @GetMapping(path = "/brokers/{brokerType}/connections", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getConnections(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable String brokerType)  throws Exception {

        log.debug("REST request to get get connections : {}");

        try {
            result = broker.getConnections(brokerType, mediaType);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, "text", "/brokers/{brokerType}/topics", result);
        } catch (Exception e) {
            log.error("Can't get connections", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, mediaType, "/brokers/{brokerType}/topics", e.getMessage());
        }

    }

    /**
     * GET  /brokers/{brokerType}/consumers : list of all broker consumers.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @return list of consumers with status 200 (OK) or with status 404 (Not Found)
     */
    @GetMapping(path = "/brokers/{brokerType}/consumers", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getConsumers(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable String brokerType)  throws Exception {

        log.debug("REST request to get get consumers : {}");

        try {
            result = broker.getConsumers(brokerType, mediaType);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, "text", "/brokers/{brokerType}/consumers", result);
        } catch (Exception e) {
            log.error("Can't get topics information", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, mediaType, "/brokers/{brokerType}/consumers", e.getMessage());
        }

    }

}