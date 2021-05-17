package org.assimbly.brokerrest;

import io.swagger.annotations.ApiParam;
import org.assimbly.brokerrest.ManagedBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.HashMap;

/**
 * REST controller for managing Broker.
 */
@RestController
@RequestMapping("/api")
public class MessageBrokerResource {

    private final Logger log = LoggerFactory.getLogger(MessageBrokerResource.class);

    private static final String ENTITY_NAME = "broker";

    @Autowired
    private ManagedBroker broker;

    private String result;

    private static final long id = 0L;

    /**
     * GET  /brokers/{brokerType}/messages/{endpointName}/{filter} : get list of messages on endpoint.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param endpointName, the name of the queue
     * @param filter, the filter
     * @return list of messages with status 200 (OK) or with status 404 (Not Found)
     */
    @GetMapping(path = "/brokers/{brokerType}/messages/{endpointName}/{filter}", produces = {"text/plain","application/xml","application/json"})
    public Object listMessages(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @RequestParam String brokerType, @PathVariable String endpointName, @RequestParam(value = "filter", required = false) String filter)  throws Exception {

        log.debug("REST request to list messages for queue : {}", endpointName);

        try {
            //brokermanager = brokerManagerResource.getBrokerManager();
            result = broker.listMessages(brokerType, endpointName, filter, mediaType);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, "text", "/brokers/{brokerType}/messages/{endpointName}/{filter}", result);
        } catch (Exception e) {
            log.error("Can't list messages", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, mediaType, "/brokers/{brokerType}/messages/{endpointName}/{filter}", e.getMessage());
        }

    }

    /**
     * GET  /brokers/{brokerType}/messages/{endpointName}/{filter} : get list of messages on endpoint.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param endpointName, the name of the queue
     * @param filter, the filter
     * @return list of messages with status 200 (OK) or with status 404 (Not Found)
     */
    @GetMapping(path = "/brokers/{brokerType}/messages/{endpointName}/count", produces = {"text/plain","application/xml","application/json"})
    public Object countMessages(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @RequestParam String brokerType, @PathVariable String endpointName)  throws Exception {

        log.debug("REST request to list messages for queue : {}", endpointName);

        try {
            //brokermanager = brokerManagerResource.getBrokerManager();
            result = broker.countMessages(brokerType, endpointName);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, mediaType, "/brokers/{brokerType}/messages/{endpointName}/{filter}", result);
        } catch (Exception e) {
            log.error("Can't list messages", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, mediaType, "/brokers/{brokerType}/messages/{endpointName}/{filter}", e.getMessage());
        }

    }

    /**
     * GET  /brokers/{brokerType}/message/{endpointName}/browse/{messageId} : get a message on a endpoint by messageId.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param endpointName, the name of the queue
     * @param messageId, the messageId (retrieved to listMessages)
     * @return The message (body and headers) with status 200 (OK) or with status 404 (Not Found)
     */
    @GetMapping(path = "/brokers/{brokerType}/message/{endpointName}/browse/{messageId}", produces = {"text/plain","application/xml","application/json"})
    public Object browseMessage(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @RequestParam String brokerType, @PathVariable String endpointName, @PathVariable String messageId)  throws Exception {

        log.debug("REST request to browse message on: {}", endpointName);

        try {
            //brokermanager = brokerManagerResource.getBrokerManager();
            result = broker.browseMessage(brokerType,endpointName, messageId, mediaType);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, "text", "/brokers/{brokerType}/message/{endpointName}/browse/{messageId}", result);
        } catch (Exception e) {
            log.error("Can't browse message", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, mediaType, "/brokers/{brokerType}/message/{endpointName}/browse/{messageId}", e.getMessage());
        }

    }

    /**
     * GET  /brokers/{brokerType}/messages/{endpointName}/browse : list of messages on the specified endpoint
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param endpointName, the name of the queue
     * @return list of messages (body or headers) with status 200 (OK) or with status 404 (Not Found)
     */
    @GetMapping(path = "/brokers/{brokerType}/messages/{endpointName}/browse", produces = {"text/plain","application/xml","application/json"})
    public Object browseMessages(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @RequestParam String brokerType, @PathVariable String endpointName, @RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "numberOfMessages", required = false) Integer numberOfMessages)  throws Exception {

        log.debug("REST request to browse messages on: {}", endpointName);

        try {
            result = broker.browseMessages(brokerType,endpointName, page, numberOfMessages, mediaType);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, "text", "/brokers/{brokerType}/messages/{endpointName}/browse", result);
        } catch (Exception e) {
            log.error("Can't browse messages", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, mediaType, "/brokers/{brokerType}/messages/{endpointName}/browse", e.getMessage());
        }

    }


    /**
     * POST  /brokers/{brokerType}/message/{endpointName}/send/{messageHeaders} : send a message.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param endpointName, the name of the endpoint (queue or topic)
     * @param messageHeaders, the message headers
     * @return the status (success) with status 200 (OK) or with status 404 (Not Found)
     */
    @PostMapping(path = "/brokers/{brokerType}/message/{endpointName}/send/{messageHeaders}", consumes = {"text/plain","application/xml","application/json"}, produces = {"text/plain","application/xml","application/json"})
    public Object sendMessage(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @RequestParam String brokerType, @PathVariable String endpointName, @RequestParam(value = "messageHeaders", required = false) Map<String,String> messageHeaders, @RequestBody String messageBody) throws Exception {

        log.debug("REST request to send messages from queue : " + endpointName);

        try {
            result = broker.sendMessage(brokerType,endpointName,messageHeaders,messageBody);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, mediaType, "/brokers/{brokerType}/message/{endpointName}/send/{messageHeaders}", result);
        } catch (Exception e) {
            log.error("Can't send message", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, mediaType, "/brokers/{brokerType}/message/{endpointName}/send/{messageHeaders}", e.getMessage());
        }

    }


    /**
     * DELETE /brokers/{brokerType}/message/{endpointName}/{messageId : remove a message on a endpoint by Messageid.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param endpointName, the name of the queue or topic
     * @return the status (success) with status 200 (OK) or with status 404 (Not Found)
     */
    @DeleteMapping(path = "/brokers/{brokerType}/message/{endpointName}/{messageId}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> removeMessage(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @RequestParam String brokerType, @PathVariable String endpointName, @PathVariable String messageId)  throws Exception {

        log.debug("REST request to remove messages for queue : {}", endpointName);

        try {
            //brokermanager = brokerManagerResource.getBrokerManager();
            result = broker.removeMessage(brokerType,endpointName, messageId);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, mediaType, "/brokers/{brokerType}/message/{endpointName}/{messageId}", result);
        } catch (Exception e) {
            log.error("Can't remove message", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, mediaType, "/brokers/{brokerType}/message/{endpointName}/{messageId}", e.getMessage());
        }

    }

    /**
     * DELETE  /brokers/{brokerType}/messages/{endpointName} : remove all messages on a endpoint.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param endpointName, the name of the endpoint (topic or queue)
     * @return the status (success) with status 200 (OK) or with status 404 (Not Found)
     */
    @DeleteMapping(path = "/brokers/{brokerType}/messages/{endpointName}", produces = {"text/plain","application/xml","application/json"})
    public Object removeMessages(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @RequestParam String brokerType, @PathVariable String endpointName)  throws Exception {

        log.debug("REST request to remove messages for endpoint : {}", endpointName);

        try {
            result = broker.removeMessages(brokerType,endpointName);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, mediaType, "/brokers/{brokerType}/messages/{endpointName}", result);
        } catch (Exception e) {
            log.error("Can't remove messages", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, mediaType, "/brokers/{brokerType}/messages/{endpointName}", e.getMessage());
        }

    }

    /**
     * POST /brokers/{brokerType}/message/{sourceQueueName}/{targetQueueName}/{messageId} : move a message by messagId to another queue
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param sourceQueueName, the name of the source queue
     * @param targetQueueName, the name of the target queue
     * @return the status (source) with status 200 (OK) or with status 404 (Not Found)
     */
    @PostMapping(path = "/brokers/{brokerType}/message/{sourceQueueName}/{targetQueueName}/{messageId}", produces = {"text/plain","application/xml","application/json"})
    public Object moveMessage(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @RequestParam String brokerType, @PathVariable String sourceQueueName, @PathVariable String targetQueueName, String messageId)  throws Exception {

        log.debug("REST request to move messages from queue : " + sourceQueueName + " to " + targetQueueName);

        try {
            //brokermanager = brokerManagerResource.getBrokerManager();
            result = broker.moveMessage(brokerType,sourceQueueName, targetQueueName, messageId);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, mediaType, "/brokers/{brokerType}/message/{sourceQueueName}/{targetQueueName}/{messageId}", result);
        } catch (Exception e) {
            log.error("Can't move messsage", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, mediaType, "/brokers/{brokerType}/message/{sourceQueueName}/{targetQueueName}/{messageId}", e.getMessage());
        }

    }

    /**
     * POST  /brokers/{brokerType}/messages/{sourceQueueName}/{targetQueueName} : move all messages to another queue.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param sourceQueueName, the name of the source queue
     * @param targetQueueName, the name of the target queue
     * @return the status (success) with status 200 (OK) or with status 404 (Not Found)
     */
    @PostMapping(path = "/brokers/{brokerType}/messages/{sourceQueueName}/{targetQueueName}", produces = {"text/plain","application/xml","application/json"})
    public Object moveMessages(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @RequestParam String brokerType, @PathVariable String sourceQueueName, @PathVariable String targetQueueName)  throws Exception {

        log.debug("REST request to move messages from queue : " + sourceQueueName + " to " + targetQueueName);

        try {
            //brokermanager = brokerManagerResource.getBrokerManager();
            result = broker.moveMessages(brokerType,sourceQueueName, targetQueueName);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(id, mediaType, "/brokers/{brokerType}/messages/{sourceQueueName}/{targetQueueName}", result);
        } catch (Exception e) {
            log.error("Can't move messages", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(id, mediaType, "/brokers/{brokerType}/messages/{sourceQueueName}/{targetQueueName}", e.getMessage());
        }

    }

}
