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
public class TopicManagerResource {

	protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    public ManagedBroker broker;

    private String result;

    private static final long id = 0L;

    /**
     * POST  /brokers/{brokerType}/topic/{topicName} : creates a new topic.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param topicName, the name of the topic
     * @return the status (success) with status 200 (OK) or with status 404 (Not Found)
     */
    @PostMapping(path = "/brokers/{brokerType}/topic/{topicName}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> createTopic(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable String brokerType, @PathVariable String topicName)  throws Exception {

        log.debug("REST request to get create topic : {}", topicName);

        try {
            //brokermanager = brokerManagerResource.getBrokerManager();
            result = broker.createTopic(brokerType,topicName);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, mediaType, "/brokers/{brokerType}/queue/{queueName}", result);
        } catch (Exception e) {
            log.error("Can't create topic", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, mediaType, "/brokers/{brokerType}/queue/{queueName}", e.getMessage());
        }

    }

    /**
     * DELETE  /brokers/{brokerType}/topic/{topicName} : delete a topic.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param topicName, the name of the topic
     * @return the status (success) with status 200 (OK) or with status 404 (Not Found)
     */
    @DeleteMapping(path = "/brokers/{brokerType}/topic/{topicName}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> deleteTopic(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable String brokerType, @PathVariable String topicName)  throws Exception {

        log.debug("REST request to get delete topic : {}", topicName);
            try {
                //brokermanager = brokerManagerResource.getBrokerManager();
                result = broker.deleteTopic(brokerType,topicName);
                return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, mediaType, "/brokers/{brokerType}/topic/{topicName}", result);
            } catch (Exception e) {
                log.error("Can't delete topic", e);
                return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, mediaType, "/brokers/{brokerType}/queue/{queueName}", e.getMessage());
            }

    }

    /**
     * GET  /brokers/{brokerType}/topic/{topicName} : get information details on specified topic.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param topicName, the name of the topic
     * @return topics with details with status 200 (OK) or with status 404 (Not Found)
     */
    @GetMapping(path = "/brokers/{brokerType}/topic/{topicName}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getTopic(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable String brokerType, @PathVariable String topicName)  throws Exception {

        log.debug("REST request to get get topic : {}", topicName);

            try {
                //brokermanager = brokerManagerResource.getBrokerManager();
                result = broker.getTopic(brokerType,topicName, mediaType);
                return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, "text", "/brokers/{brokerType}/topic/{topicName}", result);
            } catch (Exception e) {
                log.error("Can't get topic information", e);
                return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, mediaType, "/brokers/{brokerType}/topic/{topicName}", e.getMessage());
            }

    }


    /**
     * GET  /brokers/{brokerType}/topics : get information details on all topics.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @return list of topics with details 200 (OK) or with status 404 (Not Found)
     */
    @GetMapping(path = "/brokers/{brokerType}/topics", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> getTopics(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable String brokerType)  throws Exception {

        log.debug("REST request to get get topics : {}");

        try {
            //brokermanager = brokerManagerResource.getBrokerManager();
            result = broker.getTopics(brokerType, mediaType);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, "text", "/brokers/{brokerType}/topics", result);
        } catch (Exception e) {
            log.error("Can't get topics information", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, mediaType, "/brokers/{brokerType}/topics", e.getMessage());
        }

    }

    /**
     * POST  /brokers/{brokerType}/topic/{topicName}/clear : clears queue (deletes all messages on specified queue).
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param topicName, the name of the topic
     * @return the status (stopped or started) with status 200 (OK) or with status 404 (Not Found)
     */
    @PostMapping(path = "/brokers/{brokerType}/topic/{topicName}/clear", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> clearTopic(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable String brokerType, @PathVariable String topicName) throws Exception {

        log.debug("REST request to clear topic : {}", topicName);

        try {
            //brokermanager = brokerManagerResource.getBrokerManager();
            result = broker.clearTopic(brokerType,topicName);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, mediaType, "/brokers/{brokerType}/topic/{topicName}/clear", result);
        } catch (Exception e) {
            log.error("Can't clear topic", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, mediaType, "/brokers/{brokerType}/topic/{topicName}/clear", e.getMessage());
        }

    }

    /**
     * POST  /brokers/{brokerType}/topics/clear : clears all queue (deletes all topic messages on the broker).
     *
     * @param brokerType, the type of broker: classic or artemis
     * @return the status (stopped or started) with status 200 (OK) or with status 404 (Not Found)
     */
    @PostMapping(path = "/brokers/{brokerType}/topics/clear", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> clearTopics(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable String brokerType)  throws Exception {

        log.debug("REST request to clear topics : this removes all messages on the broker!");

        try {
            //brokermanager = brokerManagerResource.getBrokerManager();
            result = broker.clearTopics(brokerType);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, mediaType, "/brokers/{brokerType}/topics/clear", result);
        } catch (Exception e) {
            log.error("Can't clear topics", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, mediaType, "/brokers/{brokerType}/topics/clear", e.getMessage());
        }

    }

}
