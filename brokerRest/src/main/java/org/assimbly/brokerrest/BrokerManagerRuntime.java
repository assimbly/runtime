package org.assimbly.brokerrest;

import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing the broker.
 */
@RestController
@RequestMapping("/api")
public class BrokerManagerRuntime {

	protected Logger log = LoggerFactory.getLogger(getClass());

    private static final long ID = 0L;

    private final ManagedBrokerRuntime broker;

    public BrokerManagerRuntime(ManagedBrokerRuntime broker) {
        this.broker = broker;
    }

    /**
     * GET  /brokers/:id : get the broker status by "id".
     *
     * @param id, the id of the broker to retrieve
     * @param brokerType, the type of broker: classic or artemis
     * @return the status (stopped or started) with status 200 (OK) or with status 404 (Not Found)
     */
    @GetMapping("/brokers/{id}/status")
    public String statusBroker(
            @PathVariable(value = "id") Long id,
            @RequestParam(value = "brokerType") String brokerType
    ) {

        log.debug("event=getBrokerStatus type=GET message=Request to get the status of the broker id={} type={}", id, brokerType);

        String status = "stopped";

        try {
            status = broker.getStatus(brokerType);
        } catch (Exception e) {
            log.error("event=getBrokerStatus type=GET id={} type={} reason={}", id, brokerType, e.getMessage(), e);
        }

        return status;
    }


    /**
     * GET  /brokers/:id : get the broker info by "id".
     *
     * @param id, the id of the broker to retrieve
     * @param brokerType, the type of broker: classic or artemis
     * @return the status (stopped or started) with status 200 (OK) or with status 404 (Not Found)
     */
    @GetMapping("/brokers/{id}/info")
    public String getBrokerInfo(
            @PathVariable(value = "id") Long id,
            @RequestParam(value = "brokerType") String brokerType
    ) {
        log.debug("event=getBrokerInfo type=GET message=Request to get info about the broker id={} type={}", id, brokerType);

        String info = "unknown";

        try {
            info = broker.getInfo(brokerType);
        } catch (Exception e) {
            log.error("event=getBrokerInfo type=GET id={} type={} reason={}", id, brokerType, e.getMessage(), e);
        }

        return info;
    }

    /**
     * GET  /brokers/:id : start the broker by "id".
     *
     * @param id the id of the broker
     * @param brokerType, the type of broker: classic or artemis
     * @return the ResponseEntity with status 200 (OK) and with body the brokerDTO, or with status 404 (Not Found)
     */
    @GetMapping("/brokers/{id}/start")
    public ResponseEntity<String> startBroker(
            @PathVariable(value = "id") Long id,
            @RequestParam(value = "brokerType") String brokerType,
            @RequestParam(value = "brokerConfigurationType") String brokerConfigurationType
    ) throws Exception {

        log.debug("event=startBroker type=GET message=Request to start the broker id={} type={} configurationType={}", id, brokerType, brokerConfigurationType);

        try {
   			String status = broker.start(brokerType,brokerConfigurationType);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, "text", "/brokers/{id}/start", status);
        } catch (Exception e) {
            log.error("event=startBroker type=GET id={} type={} reason={}", id, brokerType, e.getMessage(), e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, "text", "/brokers/{id}/start", e.getMessage());
        }

    }

    /**
     * GET  /brokers/:id : restart the broker by "id".
     *
     * @param id the id of the broker
     * @param brokerType, the type of broker: classic or artemis
     * @return the ResponseEntity with status 200 (OK) and with body the brokerDTO, or with status 404 (Not Found)
     */
    @GetMapping("/brokers/{id}/restart")
    public ResponseEntity<String> restartBroker(
            @PathVariable(value = "id") Long id,
            @RequestParam(value = "brokerType") String brokerType,
            @RequestParam(value = "brokerConfigurationType") String brokerConfigurationType
    ) throws Exception {

        log.debug("event=restartBroker type=GET message=Request to restart the broker id={} type={} brokerConfigurationType={}", id, brokerType, brokerConfigurationType);

        try {
            String status = broker.restart(brokerType,brokerConfigurationType);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, "text", "/brokers/{id}/restart", status);
        } catch (Exception e) {
            log.error("event=restartBroker type=GET id={} type={} brokerConfigurationType={} reason={}", id, brokerType, brokerConfigurationType, e.getMessage(), e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, "text", "/brokers/{id}/restart", e.getMessage());
        }

    }

    /**
     * GET  /brokers/:id : stop the broker by "id".
     *
     * @param id the id of the broker
     * @param brokerType, the type of broker: classic or artemis
     * @return the ResponseEntity with status 200 (OK) and with body the brokerDTO, or with status 404 (Not Found)
     */
    @GetMapping("/brokers/{id}/stop")
    public ResponseEntity<String> stopBroker(
            @PathVariable(value = "id") Long id,
            @RequestParam(value = "brokerType") String brokerType
    ) throws Exception {

        log.debug("event=stopBroker type=GET message=Request to stop the broker id={} type={}", id, brokerType);

        try {
            String status = broker.stop(brokerType);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, "text", "/brokers/{id}/stop", status);
        } catch (Exception e) {
            log.error("event=stopBroker type=GET id={} type={} reason={}", id, brokerType, e.getMessage(), e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, "text", "/brokers/{id}/stop", e.getMessage());
    	}

    }

    /**
     * GET  /brokers/{brokerType}/connections: get list of all broker connections.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @return list of connections with status 200 (OK) or with status 404 (Not Found)
     */
    @GetMapping(
            path = "/brokers/{brokerType}/connections",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getConnections(
            @PathVariable(value = "brokerType") String brokerType,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    )  throws Exception {

        log.debug("event=getConnections type=GET message=Get list of all broker connections type={}", brokerType);

        try {
            String result = broker.getConnections(brokerType, mediaType);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, "text", "/brokers/{brokerType}/topics", result);
        } catch (Exception e) {
            log.error("event=getConnections type=GET type={} reason={}", brokerType, e.getMessage(), e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/topics", e.getMessage());
        }

    }

    /**
     * GET  /brokers/{brokerType}/consumers : list of all broker consumers.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @return list of consumers with status 200 (OK) or with status 404 (Not Found)
     */
    @GetMapping(
            path = "/brokers/{brokerType}/consumers",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getConsumers(
            @PathVariable(value = "brokerType") String brokerType,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    )  throws Exception {

        log.debug("event=getConsumers type=GET message=Get list of all broker consumers type={}", brokerType);

        try {
            String result = broker.getConsumers(brokerType, mediaType);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, "text", "/brokers/{brokerType}/consumers", result);
        } catch (Exception e) {
            log.error("event=getConsumers type=GET type={} reason={}", brokerType, e.getMessage(), e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/consumers", e.getMessage());
        }

    }

}