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
 * REST controller for managing topics on the broker.
 */
@Tag(name = "Broker topics", description = "Manage topics on the broker")
@RestController
@RequestMapping("/api")
public class TopicManagerRuntime {

	protected Logger log = LoggerFactory.getLogger(getClass());

    private final ManagedBrokerRuntime broker;

    public TopicManagerRuntime(ManagedBrokerRuntime broker) {
        this.broker = broker;
    }

    /**
     * POST  /brokers/{brokerType}/topic/{topicName} : creates a new topic.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param topicName, the name of the topic
     * @return the status (success) with status 200 (OK) or with status 404 (Not Found)
     */
    @Operation(summary = "Create a topic")
    @PostMapping(
            path = "/brokers/{brokerType}/topic/{topicName}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> createTopic(
            @PathVariable(value = "brokerType") String brokerType,
            @PathVariable(value = "topicName") String topicName,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.debug("event=createTopic type=POST message=Create new topic name={} type={}", topicName, brokerType);

        return BrokerResponse.respond(mediaType, "/brokers/{brokerType}/topic/{topicName}",
                () -> broker.createTopic(brokerType, topicName), topicName, brokerType);

    }

    /**
     * DELETE  /brokers/{brokerType}/topic/{topicName} : delete a topic.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param topicName, the name of the topic
     * @return the status (success) with status 200 (OK) or with status 404 (Not Found)
     */
    @Operation(summary = "Delete a topic")
    @DeleteMapping(
            path = "/brokers/{brokerType}/topic/{topicName}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> deleteTopic(
            @PathVariable(value = "brokerType") String brokerType,
            @PathVariable(value = "topicName") String topicName,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.debug("event=deleteTopic type=DELETE message=Delete topic name={} type={}", topicName, brokerType);

        return BrokerResponse.respond(mediaType, "/brokers/{brokerType}/topic/{topicName}",
                () -> broker.deleteTopic(brokerType,topicName), topicName, brokerType);

    }

    /**
     * GET  /brokers/{brokerType}/topic/{topicName} : get information details on specified topic.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param topicName, the name of the topic
     * @return topics with details with status 200 (OK) or with status 404 (Not Found)
     */
    @Operation(summary = "Get topic details")
    @GetMapping(
            path = "/brokers/{brokerType}/topic/{topicName}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getTopic(
            @PathVariable(value = "brokerType") String brokerType,
            @PathVariable(value = "topicName") String topicName,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.debug("event=getTopic type=GET message=Get topic name={} type={}", topicName, brokerType);

        return BrokerResponse.respondConverted(mediaType, "/brokers/{brokerType}/topic/{topicName}",
                () -> broker.getTopic(brokerType,topicName, mediaType), topicName, brokerType);

    }


    /**
     * GET  /brokers/{brokerType}/topics : get information details on all topics.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @return list of topics with details 200 (OK) or with status 404 (Not Found)
     */
    @Operation(summary = "List topics with details")
    @GetMapping(
            path = "/brokers/{brokerType}/topics",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getTopics(
            @PathVariable(value = "brokerType") String brokerType,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.debug("event=getTopics type=GET message=Get list of topics type={}", brokerType);

        return BrokerResponse.respondConverted(mediaType, "/brokers/{brokerType}/topics",
                () -> broker.getTopics(brokerType, mediaType), brokerType);

    }

    /**
     * POST  /brokers/{brokerType}/topic/{topicName}/clear : clears queue (deletes all messages on specified queue).
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param topicName, the name of the topic
     * @return the status (stopped or started) with status 200 (OK) or with status 404 (Not Found)
     */
    @Operation(summary = "Remove all messages of a topic")
    @PostMapping(
            path = "/brokers/{brokerType}/topic/{topicName}/clear",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> clearTopic(
            @PathVariable(value = "brokerType") String brokerType,
            @PathVariable(value = "topicName") String topicName,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.debug("event=clearTopic type=POST message=Clear topic name={} type={}", topicName, brokerType);

        return BrokerResponse.respond(mediaType, "/brokers/{brokerType}/topic/{topicName}/clear",
                () -> broker.clearTopic(brokerType,topicName), topicName, brokerType);

    }

    /**
     * POST  /brokers/{brokerType}/topics/clear : clears all queue (deletes all topic messages on the broker).
     *
     * @param brokerType, the type of broker: classic or artemis
     * @return the status (stopped or started) with status 200 (OK) or with status 404 (Not Found)
     */
    @Operation(summary = "Remove all messages of all topics")
    @PostMapping(
            path = "/brokers/{brokerType}/topics/clear",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> clearTopics(
            @PathVariable(value = "brokerType") String brokerType,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.debug("event=clearTopics type=POST message=Removes all topic messages on the broker type={}", brokerType);

        return BrokerResponse.respond(mediaType, "/brokers/{brokerType}/topics/clear",
                () -> broker.clearTopics(brokerType), brokerType);

    }

}
