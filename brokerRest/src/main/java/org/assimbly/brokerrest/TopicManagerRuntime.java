package org.assimbly.brokerrest;

import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing topics on the broker.
 */
@RestController
@RequestMapping("/api")
public class TopicManagerRuntime {

	protected Logger log = LoggerFactory.getLogger(getClass());

    private static final long ID = 0L;

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

        try {
            String result = broker.createTopic(brokerType, topicName);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, mediaType, "/brokers/{brokerType}/queue/{queueName}", result);
        } catch (Exception e) {
            log.error("event=createTopic type=POST name={} type={} reason={}", topicName, brokerType, e.getMessage(), e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/queue/{queueName}", e.getMessage());
        }

    }

    /**
     * DELETE  /brokers/{brokerType}/topic/{topicName} : delete a topic.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param topicName, the name of the topic
     * @return the status (success) with status 200 (OK) or with status 404 (Not Found)
     */
    @DeleteMapping(
            path = "/brokers/{brokerType}/topic/{topicName}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> deleteTopic(
            @PathVariable(value = "brokerType") String brokerType,
            @PathVariable(value = "topicName") String topicName,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.debug("event=deleteTopic type=DELETE message=Create new topic name={} type={}", topicName, brokerType);

        try {
            String result = broker.deleteTopic(brokerType,topicName);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, mediaType, "/brokers/{brokerType}/topic/{topicName}", result);
        } catch (Exception e) {
            log.error("event=deleteTopic type=DELETE name={} type={} reason={}", topicName, brokerType, e.getMessage(), e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/queue/{queueName}", e.getMessage());
        }

    }

    /**
     * GET  /brokers/{brokerType}/topic/{topicName} : get information details on specified topic.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param topicName, the name of the topic
     * @return topics with details with status 200 (OK) or with status 404 (Not Found)
     */
    @GetMapping(
            path = "/brokers/{brokerType}/topic/{topicName}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getTopic(
            @PathVariable(value = "brokerType") String brokerType,
            @PathVariable(value = "topicName") String topicName,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.debug("event=getTopic type=POST message=get topic name={} type={}", topicName, brokerType);

        try {
            String result = broker.getTopic(brokerType,topicName, mediaType);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, "text", "/brokers/{brokerType}/topic/{topicName}", result);
        } catch (Exception e) {
            log.error("event=getTopic type=GET name={} type={} reason={}", topicName, brokerType, e.getMessage(), e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/topic/{topicName}", e.getMessage());
        }

    }


    /**
     * GET  /brokers/{brokerType}/topics : get information details on all topics.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @return list of topics with details 200 (OK) or with status 404 (Not Found)
     */
    @GetMapping(
            path = "/brokers/{brokerType}/topics",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getTopics(
            @PathVariable(value = "brokerType") String brokerType,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.debug("event=getTopics type=GET message=Get list of topics type={}", brokerType);

        try {
            String result = broker.getTopics(brokerType, mediaType);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, "text", "/brokers/{brokerType}/topics", result);
        } catch (Exception e) {
            log.error("event=getTopics type=GET type={} reason={}", brokerType, e.getMessage(), e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/topics", e.getMessage());
        }

    }

    /**
     * POST  /brokers/{brokerType}/topic/{topicName}/clear : clears queue (deletes all messages on specified queue).
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param topicName, the name of the topic
     * @return the status (stopped or started) with status 200 (OK) or with status 404 (Not Found)
     */
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

        try {
            String result = broker.clearTopic(brokerType,topicName);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, mediaType, "/brokers/{brokerType}/topic/{topicName}/clear", result);
        } catch (Exception e) {
            log.error("event=clearTopic type=POST name={} type={} reason={}", topicName, brokerType, e.getMessage(), e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/topic/{topicName}/clear", e.getMessage());
        }

    }

    /**
     * POST  /brokers/{brokerType}/topics/clear : clears all queue (deletes all topic messages on the broker).
     *
     * @param brokerType, the type of broker: classic or artemis
     * @return the status (stopped or started) with status 200 (OK) or with status 404 (Not Found)
     */
    @PostMapping(
            path = "/brokers/{brokerType}/topics/clear",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> clearTopics(
            @PathVariable(value = "brokerType") String brokerType,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.debug("event=clearTopics type=POST message=Removes all topic messages on the broker type={}", brokerType);

        try {
            String result = broker.clearTopics(brokerType);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, mediaType, "/brokers/{brokerType}/topics/clear", result);
        } catch (Exception e) {
            log.error("event=clearTopics type=POST type={} reason={}", brokerType, e.getMessage(), e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/topics/clear", e.getMessage());
        }

    }

}
