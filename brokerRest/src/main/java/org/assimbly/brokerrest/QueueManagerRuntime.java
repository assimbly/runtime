package org.assimbly.brokerrest;

import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * REST controller for managing queues on the broker.
 */
@Tag(name = "Broker queues", description = "Manage queues on the broker")
@RestController
@RequestMapping("/api")
public class QueueManagerRuntime {

	protected Logger log = LoggerFactory.getLogger(getClass());

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
    @Operation(summary = "Create a queue")
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

        return BrokerResponse.respond(mediaType, "/brokers/{brokerType}/queue/{queueName}",
                () -> broker.createQueue(brokerType,queueName), queueName, brokerType);

    }

    /**
     * DELETE  /brokers/{brokerType}/queue/{queueName} : delete the queue on the broker
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param queueName, the name of the queue
     * @return the status (success) with status 200 (OK) or with status 404 (Not Found)
     */
    @Operation(summary = "Delete a queue")
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

        return BrokerResponse.respond(mediaType, "/brokers/{brokerType}/queue/{queueName}",
                () -> broker.deleteQueue(brokerType,queueName), queueName, brokerType);

    }

    /**
     * GET  /brokers/{brokerType}/queue/{queueName} : get information details on the specified queue.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param queueName, the name of the queue
     * @return Queue destination details
     */
    @Operation(summary = "Get queue details")
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

        return BrokerResponse.respondConverted(mediaType, "/brokers/{brokerType}/queue/{queueName}",
                () -> broker.getQueue(brokerType,queueName, mediaType), queueName, brokerType);

    }

    /**
     * GET  /brokers/{brokerType}/queues : get information on all queues on the broker
     *
     * @param brokerType, the type of broker: classic or artemis
     * @return the status (stopped or started) with status 200 (OK) or with status 404 (Not Found)
     */
    @Operation(summary = "List queues with details")
    @GetMapping(
            path= "/brokers/{brokerType}/queues",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getQueues(
            @PathVariable(value = "brokerType") String brokerType,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.debug("event=getQueues type=GET message=Get queues info type={}", brokerType);

        return BrokerResponse.respondConverted(mediaType, "/brokers/{brokerType}/queues",
                () -> broker.getQueues(brokerType, mediaType), brokerType);

    }

    /**
     * POST  /brokers/{brokerType}/queue/{queueName}/clear : clears queue (deletes all messages on specified queue).
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param queueName, the name of the queue
     * @return the status (stopped or started) with status 200 (OK) or with status 404 (Not Found)
     */
    @Operation(summary = "Remove all messages of a queue")
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

        return BrokerResponse.respond(mediaType, "/brokers/{brokerType}/queue/{queueName}/clear",
                () -> broker.clearQueue(brokerType,queueName), queueName, brokerType);

    }

    /**
     * POST  /brokers/{brokerType}/queues/clear : clears all queue (deletes all messages on the broker).
     *
     * @param brokerType, the type of broker: classic or artemis
     * @return the status (stopped or started) with status 200 (OK) or with status 404 (Not Found)
     */
    @Operation(summary = "Remove all messages of all queues")
    @PostMapping(
            path = "/brokers/{brokerType}/queues/clear",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> clearQueues(
            @PathVariable(value = "brokerType") String brokerType,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.debug("event=clearQueues type=POST message=Deletes messages on all queue type={}", brokerType);

        return BrokerResponse.respond(mediaType, "/brokers/{brokerType}/queues/clear",
                () -> broker.clearQueues(brokerType), brokerType);

    }

}
