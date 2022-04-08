package org.assimbly.brokerrest;

import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing Broker.
 */
@RestController
@RequestMapping("/api")
public class QueueManagerResource {

    private final Logger log = LoggerFactory.getLogger(QueueManagerResource.class);

    @Autowired
    private ManagedBroker broker;

	private String result;

    private static final long id = 0L;

    /**
     * POST  /brokers/{brokerType}/queue/{queueName} : creates a queue on the broker.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param queueName, the name of the queue
     * @return the status (success or failed) with status 200 (OK) or with status 404 (Not Found)
     */
    @PostMapping(path = "/brokers/{brokerType}/queue/{queueName}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> createQueue(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable String brokerType, @PathVariable String queueName) throws Exception {

        log.debug("REST request to create queue : {}", queueName);

        try {
            result = broker.createQueue(brokerType,queueName);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, mediaType, "/brokers/{brokerType}/queue/{queueName}", result);
        } catch (Exception e) {
            log.error("Can't create queue", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, mediaType, "/brokers/{brokerType}/queue/{queueName}", e.getMessage());
        }

    }

    /**
     * DELETE  /brokers/{brokerType}/queue/{queueName} : delete the queue on the broker
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param queueName, the name of the queue
     * @return the status (success) with status 200 (OK) or with status 404 (Not Found)
     */
    @DeleteMapping(path = "/brokers/{brokerType}/queue/{queueName}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> deleteQueue(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable String brokerType, @PathVariable String queueName) throws Exception {

        log.debug("REST request to get delete queue : {}", queueName);

        try {
            result = broker.deleteQueue(brokerType,queueName);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, mediaType, "/brokers/{brokerType}/queue/{queueName}", result);
        } catch (Exception e) {
            log.error("Can't delete queue", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, mediaType, "/brokers/{brokerType}/queue/{queueName}", e.getMessage());
        }

    }

    /**
     * GET  /brokers/{brokerType}/queue/{queueName} : get information details on the specified queue.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param queueName, the name of the queue
     * @return Queue destination details
     */
    @GetMapping(path = "/brokers/{brokerType}/queue/{queueName}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getQueue(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable String brokerType, @PathVariable String queueName) throws Exception {

        log.debug("REST request to get get queue information : {}", queueName);

        try {
            result = broker.getQueue(brokerType,queueName, mediaType);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, "text", "/brokers/{brokerType}/queue/{queueName}", result);
        } catch (Exception e) {
            log.error("Can't get queue information", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, mediaType, "/brokers/{brokerType}/queue/{queueName}", e.getMessage());
        }

    }


    /**
     * GET  /brokers/{brokerType}/queues : get information on all queues on the broker
     *
     * @param brokerType, the type of broker: classic or artemis
     * @return the status (stopped or started) with status 200 (OK) or with status 404 (Not Found)
     */
    @GetMapping(path= "/brokers/{brokerType}/queues", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getQueues(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable String brokerType) throws Exception {

        log.debug("REST request to get get queues : {}");

        try {
            result = broker.getQueues(brokerType, mediaType);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, "text", "/brokers/{brokerType}/queues", result);
        } catch (Exception e) {
            log.error("Can't get queues information", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, mediaType, "/brokers/{brokerType}/queues", e.getMessage());
        }

    }

    /**
     * POST  /brokers/{brokerType}/queue/{queueName}/clear : clears queue (deletes all messages on specified queue).
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param queueName, the name of the queue
     * @return the status (stopped or started) with status 200 (OK) or with status 404 (Not Found)
     */
    @PostMapping(path = "/brokers/{brokerType}/queue/{queueName}/clear", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> clearQueue(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable String brokerType, @PathVariable String queueName) throws Exception {

        log.debug("REST request to clear queue : {}", queueName);

        try {
            result = broker.clearQueue(brokerType,queueName);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, mediaType, "/brokers/{brokerType}/queue/{queueName}/clear", result);
        } catch (Exception e) {
            log.error("Can't clear queue", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, mediaType, "/brokers/{brokerType}/queue/{queueName}/clear", e.getMessage());
        }

    }

    /**
     * POST  /brokers/{brokerType}/queues/clear : clears all queue (deletes all messages on the broker).
     *
     * @param brokerType, the type of broker: classic or artemis
     * @return the status (stopped or started) with status 200 (OK) or with status 404 (Not Found)
     */
    @PostMapping(path = "/brokers/{brokerType}/queues/clear", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> clearQueues(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable String brokerType)  throws Exception {

        log.debug("REST request to clear queues : this removes all messages on the broker!");

        try {
            result = broker.clearQueues(brokerType);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, mediaType, "/brokers/{brokerType}/queues/clear", result);
        } catch (Exception e) {
            log.error("Can't clear queues", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, mediaType, "/brokers/{brokerType}/queues/clear", e.getMessage());
        }

    }

}
