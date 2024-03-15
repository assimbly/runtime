package org.assimbly.brokerrest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

/**
 * REST controller for managing Broker.
 */
@RestController
@RequestMapping("/api")
public class MessageBrokerRuntime {

	protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private ManagedBrokerRuntime broker;

    private String result;

    private static final long ID = 0L;

    /**
     * GET  /brokers/{brokerType}/messages/{endpointName}/{filter} : get list of messages on endpoint.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param endpointName, the name of the queue
     * @param filter, the filter
     * @return list of messages with status 200 (OK) or with status 404 (Not Found)
     */
    @GetMapping(
            path = "/brokers/{brokerType}/messages/{endpointName}/{filter}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public Object listMessages(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable(value = "brokerType") String brokerType, @PathVariable(value = "endpointName") String endpointName, @RequestParam(value = "filter", required = false) String filter)  throws Exception {

        log.debug("REST request to list messages for queue : {}", endpointName);

        try {
            //brokermanager = brokerManagerResource.getBrokerManager();
            result = broker.listMessages(brokerType, endpointName, filter, mediaType);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, "text", "/brokers/{brokerType}/messages/{endpointName}/{filter}", result);
        } catch (Exception e) {
            log.error("Can't list messages", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/messages/{endpointName}/{filter}", e.getMessage());
        }

    }

    /**
     * GET  /brokers/{brokerType}/messages/{endpointName}/{filter} : get list of messages on endpoint.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param endpointNames, the name of the queue
     * @return list of messages with status 200 (OK) or with status 404 (Not Found)
     */
    @PostMapping(
            path = "/brokers/{brokerType}/messages/count",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public Object countMessagesFromList(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable String brokerType, @RequestBody String endpointNames)  throws Exception {

        log.debug("REST request to count messages for queue : {}", endpointNames);

        try {
            result = broker.countMessagesFromList(brokerType, endpointNames);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, mediaType, "/brokers/{brokerType}/messages/count", result);
        } catch (Exception e) {
            log.error("Can't list messages", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/messages/count", e.getMessage());
        }

    }


    /**
     * GET  /brokers/{brokerType}/messages/{endpointName}/{filter} : get list of messages on endpoint.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param endpointName, the name of the queue
     * @return list of messages with status 200 (OK) or with status 404 (Not Found)
     */
    @GetMapping(
            path = "/brokers/{brokerType}/messages/{endpointName}/count",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public Object countMessages(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable(value = "brokerType") String brokerType, @PathVariable(value = "endpointName") String endpointName)  throws Exception {

        log.debug("REST request to list messages for queue : {}", endpointName);

        try {
            //brokermanager = brokerManagerResource.getBrokerManager();
            result = broker.countMessages(brokerType, endpointName);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, mediaType, "/brokers/{brokerType}/messages/{endpointName}/count", result);
        } catch (Exception e) {
            log.error("Can't list messages", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/messages/{endpointName}/count", e.getMessage());
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
    @GetMapping(
            path = "/brokers/{brokerType}/message/{endpointName}/browse/{messageId}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public Object browseMessage(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable(value = "brokerType") String brokerType, @PathVariable(value = "endpointName") String endpointName, @PathVariable(value = "messageId") String messageId, @RequestParam(value = "excludeBody", required = false) boolean excludeBody)  throws Exception {

        log.debug("REST request to browse message on: {}", endpointName);

        try {
            //brokermanager = brokerManagerResource.getBrokerManager();
            result = broker.browseMessage(brokerType, endpointName, messageId, mediaType, excludeBody);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, "text", "/brokers/{brokerType}/message/{endpointName}/browse/{messageId}", result);
        } catch (Exception e) {
            log.error("Can't browse message", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/message/{endpointName}/browse/{messageId}", e.getMessage());
        }

    }

    /**
     * GET  /brokers/{brokerType}/messages/{endpointName}/browse : list of messages on the specified endpoint
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param endpointName, the name of the queue
     * @return list of messages (body or headers) with status 200 (OK) or with status 404 (Not Found)
     */
    @GetMapping(
            path = "/brokers/{brokerType}/messages/{endpointName}/browse",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public Object browseMessages(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable(value = "brokerType") String brokerType, @PathVariable(value = "endpointName") String endpointName, @RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "numberOfMessages", required = false) Integer numberOfMessages, @RequestParam(value = "excludeBody", required = false) boolean excludeBody)  throws Exception {

        log.debug("REST request to browse messages on: {}", endpointName);

        try {
            result = broker.browseMessages(brokerType,endpointName, page, numberOfMessages, mediaType, excludeBody);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, "text", "/brokers/{brokerType}/messages/{endpointName}/browse", result);
        } catch (Exception e) {
            log.error("Can't browse messages", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/messages/{endpointName}/browse", e.getMessage());
        }

    }


    /**
     * POST  /brokers/{brokerType}/message/{endpointName}/send/{messageHeaders} : send a message.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param endpointName, the name of the endpoint (queue or topic)
     * @param messageHeaders, the message headers (json map)
     * @return the status (success) with status 200 (OK) or with status 404 (Not Found)
     */
    @PostMapping(
            path = "/brokers/{brokerType}/message/{endpointName}/send",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public Object sendMessage(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable(value = "brokerType") String brokerType, @PathVariable(value = "endpointName") String endpointName, @RequestParam(value = "messageHeaders", required = false) String messageHeaders, @RequestBody String messageBody) throws Exception {

        log.debug("REST request to send messages from queue : " + endpointName);

        Map<String,Object> messageHeadersMap = null;
        if(messageHeaders!=null){
            messageHeadersMap = new ObjectMapper().readValue(messageHeaders, HashMap.class);
        }

        try {
            result = broker.sendMessage(brokerType,endpointName,messageHeadersMap,messageBody);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, mediaType, "/brokers/{brokerType}/message/{endpointName}/send/{messageHeaders}", result);
        } catch (Exception e) {
            log.error("Can't send message", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/message/{endpointName}/send/{messageHeaders}", e.getMessage());
        }

    }


    /**
     * DELETE /brokers/{brokerType}/message/{endpointName}/{messageId} : remove a message on a endpoint by Messageid.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param endpointName, the name of the queue or topic
     * @return the status (success) with status 200 (OK) or with status 404 (Not Found)
     */
    @DeleteMapping(
            path = "/brokers/{brokerType}/message/{endpointName}/{messageId}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> removeMessage(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable(value = "brokerType") String brokerType, @PathVariable(value = "endpointName") String endpointName, @PathVariable(value = "messageId") String messageId)  throws Exception {

        log.debug("REST request to remove messages for queue : {}", endpointName);

        try {
            result = broker.removeMessage(brokerType,endpointName, messageId);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, mediaType, "/brokers/{brokerType}/message/{endpointName}/{messageId}", result);
        } catch (Exception e) {
            log.error("Can't remove message", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/message/{endpointName}/{messageId}", e.getMessage());
        }

    }

    /**
     * DELETE  /brokers/{brokerType}/messages/{endpointName} : remove all messages on a endpoint.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param endpointName, the name of the endpoint (topic or queue)
     * @return the status (success) with status 200 (OK) or with status 404 (Not Found)
     */
    @DeleteMapping(
            path = "/brokers/{brokerType}/messages/{endpointName}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public Object removeMessages(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable(value = "brokerType") String brokerType, @PathVariable(value = "endpointName") String endpointName)  throws Exception {

        log.debug("REST request to remove messages for endpoint : {}", endpointName);

        try {
            result = broker.removeMessages(brokerType,endpointName);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, mediaType, "/brokers/{brokerType}/messages/{endpointName}", result);
        } catch (Exception e) {
            log.error("Can't remove messages", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/messages/{endpointName}", e.getMessage());
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
    @PostMapping(
            path = "/brokers/{brokerType}/message/{sourceQueueName}/{targetQueueName}/{messageId}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public Object moveMessage(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable(value = "brokerType") String brokerType, @PathVariable(value = "sourceQueueName") String sourceQueueName, @PathVariable(value = "targetQueueName") String targetQueueName, @PathVariable(value = "messageId") String messageId)  throws Exception {

        log.debug("REST request to move messages from queue : " + sourceQueueName + " to " + targetQueueName);

        try {
            //brokermanager = brokerManagerResource.getBrokerManager();
            result = broker.moveMessage(brokerType,sourceQueueName, targetQueueName, messageId);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, mediaType, "/brokers/{brokerType}/message/{sourceQueueName}/{targetQueueName}/{messageId}", result);
        } catch (Exception e) {
            log.error("Can't move messsage", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/message/{sourceQueueName}/{targetQueueName}/{messageId}", e.getMessage());
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
    @PostMapping(
            path = "/brokers/{brokerType}/messages/{sourceQueueName}/{targetQueueName}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public Object moveMessages(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable(value = "brokerType") String brokerType, @PathVariable(value = "sourceQueueName") String sourceQueueName, @PathVariable(value = "targetQueueName") String targetQueueName)  throws Exception {

        log.debug("REST request to move messages from queue : " + sourceQueueName + " to " + targetQueueName);

        try {
            //brokermanager = brokerManagerResource.getBrokerManager();
            result = broker.moveMessages(brokerType,sourceQueueName, targetQueueName);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, mediaType, "/brokers/{brokerType}/messages/{sourceQueueName}/{targetQueueName}", result);
        } catch (Exception e) {
            log.error("Can't move messages", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/messages/{sourceQueueName}/{targetQueueName}", e.getMessage());
        }

    }

}
