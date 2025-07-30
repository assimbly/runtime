package org.assimbly.brokerrest;

import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing queues on the broker.
 */
@RestController
@RequestMapping("/api")
public class QueueManagerRuntime {

	protected Logger log = LoggerFactory.getLogger(getClass());

    private static final long ID = 0L;

    private final ManagedBrokerRuntime broker;

    public QueueManagerRuntime(ManagedBrokerRuntime broker) {
        this.broker = broker;
    }

    /**
     * POST  /brokers/{brokerType}/queue/{queueName} : creates a queue on the broker.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param queueName, the name of the queue
     * @return the status (success or failed) with status 200 (OK) or with status 404 (Not Found)
     */
    @PostMapping(
            path = "/brokers/{brokerType}/queue/{queueName}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> createQueue(
            @PathVariable(value = "brokerType") String brokerType,
            @PathVariable(value = "queueName") String queueName,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.debug("event=createQueue type=POST message=Create a new queue name={} type={}", queueName, brokerType);

        try {
            String result = broker.createQueue(brokerType,queueName);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, mediaType, "/brokers/{brokerType}/queue/{queueName}", result);
        } catch (Exception e) {
            log.error("event=createQueue type=POST name={} type={} reason={}", queueName, brokerType, e.getMessage(), e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/queue/{queueName}", e.getMessage());
        }

    }

    /**
     * DELETE  /brokers/{brokerType}/queue/{queueName} : delete the queue on the broker
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param queueName, the name of the queue
     * @return the status (success) with status 200 (OK) or with status 404 (Not Found)
     */
    @DeleteMapping(
            path = "/brokers/{brokerType}/queue/{queueName}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> deleteQueue(
            @PathVariable(value = "brokerType") String brokerType,
            @PathVariable(value = "queueName") String queueName,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.debug("event=deleteQueue type=DELETE message=Delete a queue name={} type={}", queueName, brokerType);

        try {
            String result = broker.deleteQueue(brokerType,queueName);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, mediaType, "/brokers/{brokerType}/queue/{queueName}", result);
        } catch (Exception e) {
            log.error("event=deleteQueue type=DELETE name={} type={} reason={}", queueName, brokerType, e.getMessage(), e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/queue/{queueName}", e.getMessage());
        }

    }

    /**
     * GET  /brokers/{brokerType}/queue/{queueName} : get information details on the specified queue.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param queueName, the name of the queue
     * @return Queue destination details
     */
    @GetMapping(
            path = "/brokers/{brokerType}/queue/{queueName}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getQueue(
            @PathVariable(value = "brokerType") String brokerType,
            @PathVariable(value = "queueName") String queueName,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.debug("event=getQueue type=GET message=Get queue info name={} type={}", queueName, brokerType);

        try {
            String result = broker.getQueue(brokerType,queueName, mediaType);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, "text", "/brokers/{brokerType}/queue/{queueName}", result);
        } catch (Exception e) {
            log.error("event=getQueue type=GET name={} type={} reason={}", queueName, brokerType, e.getMessage(), e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/queue/{queueName}", e.getMessage());
        }

    }

    /**
     * GET  /brokers/{brokerType}/queues : get information on all queues on the broker
     *
     * @param brokerType, the type of broker: classic or artemis
     * @return the status (stopped or started) with status 200 (OK) or with status 404 (Not Found)
     */
    @GetMapping(
            path= "/brokers/{brokerType}/queues",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getQueues(
            @PathVariable(value = "brokerType") String brokerType,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.debug("event=getQueues type=GET message=Get queues info type={}", brokerType);

        try {
            String result = broker.getQueues(brokerType, mediaType);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, "text", "/brokers/{brokerType}/queues", result);
        } catch (Exception e) {
            log.error("event=getQueues type=GET type={} reason={}", brokerType, e.getMessage(), e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/queues", e.getMessage());
        }

    }

    /**
     * POST  /brokers/{brokerType}/queue/{queueName}/clear : clears queue (deletes all messages on specified queue).
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param queueName, the name of the queue
     * @return the status (stopped or started) with status 200 (OK) or with status 404 (Not Found)
     */
    @PostMapping(
            path = "/brokers/{brokerType}/queue/{queueName}/clear",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> clearQueue(
            @PathVariable(value = "brokerType") String brokerType,
            @PathVariable(value = "queueName") String queueName,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.debug("event=clearQueue type=POST message=Deletes messages on the queue name={} type={}", queueName, brokerType);

        try {
            String result = broker.clearQueue(brokerType,queueName);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, mediaType, "/brokers/{brokerType}/queue/{queueName}/clear", result);
        } catch (Exception e) {
            log.error("event=clearQueue type=POST name={} type={} reason={}", queueName, brokerType, e.getMessage(), e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/queue/{queueName}/clear", e.getMessage());
        }

    }

    /**
     * POST  /brokers/{brokerType}/queues/clear : clears all queue (deletes all messages on the broker).
     *
     * @param brokerType, the type of broker: classic or artemis
     * @return the status (stopped or started) with status 200 (OK) or with status 404 (Not Found)
     */
    @PostMapping(
            path = "/brokers/{brokerType}/queues/clear",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> clearQueues(
            @PathVariable(value = "brokerType") String brokerType,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.debug("event=clearQueues type=POST message=Deletes messages on all queue type={}", brokerType);

        try {
            String result = broker.clearQueues(brokerType);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, mediaType, "/brokers/{brokerType}/queues/clear", result);
        } catch (Exception e) {
            log.error("event=clearQueues type=POST type={} reason={}", brokerType, e.getMessage(), e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/queues/clear", e.getMessage());
        }

    }

}
