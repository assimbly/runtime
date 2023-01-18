package org.assimbly.brokerrest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
     * GET  /brokers/{brokerType}/messages/{stepName}/{filter} : get list of messages on step.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param stepName, the name of the queue
     * @param filter, the filter
     * @return list of messages with status 200 (OK) or with status 404 (Not Found)
     */
    @GetMapping(path = "/brokers/{brokerType}/messages/{stepName}/{filter}", produces = {"text/plain","application/xml","application/json"})
    public Object listMessages(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable String brokerType, @PathVariable String stepName, @RequestParam(value = "filter", required = false) String filter)  throws Exception {

        log.debug("REST request to list messages for queue : {}", stepName);

        try {
            //brokermanager = brokerManagerResource.getBrokerManager();
            result = broker.listMessages(brokerType, stepName, filter, mediaType);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, "text", "/brokers/{brokerType}/messages/{stepName}/{filter}", result);
        } catch (Exception e) {
            log.error("Can't list messages", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/messages/{stepName}/{filter}", e.getMessage());
        }

    }

    /**
     * GET  /brokers/{brokerType}/messages/{stepName}/{filter} : get list of messages on step.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param stepName, the name of the queue
     * @return list of messages with status 200 (OK) or with status 404 (Not Found)
     */
    @GetMapping(path = "/brokers/{brokerType}/messages/{stepName}/count", produces = {"text/plain","application/xml","application/json"})
    public Object countMessages(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable String brokerType, @PathVariable String stepName)  throws Exception {

        log.debug("REST request to list messages for queue : {}", stepName);

        try {
            //brokermanager = brokerManagerResource.getBrokerManager();
            result = broker.countMessages(brokerType, stepName);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, mediaType, "/brokers/{brokerType}/messages/{stepName}/count", result);
        } catch (Exception e) {
            log.error("Can't list messages", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/messages/{stepName}/count", e.getMessage());
        }

    }

    /**
     * GET  /brokers/{brokerType}/message/{stepName}/browse/{messageId} : get a message on a step by messageId.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param stepName, the name of the queue
     * @param messageId, the messageId (retrieved to listMessages)
     * @return The message (body and headers) with status 200 (OK) or with status 404 (Not Found)
     */
    @GetMapping(path = "/brokers/{brokerType}/message/{stepName}/browse/{messageId}", produces = {"text/plain","application/xml","application/json"})
    public Object browseMessage(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable String brokerType, @PathVariable String stepName, @PathVariable String messageId, @RequestParam(value = "excludeBody", required = false) boolean excludeBody)  throws Exception {

        log.debug("REST request to browse message on: {}", stepName);

        try {
            //brokermanager = brokerManagerResource.getBrokerManager();
            result = broker.browseMessage(brokerType,stepName, messageId, mediaType, excludeBody);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, "text", "/brokers/{brokerType}/message/{stepName}/browse/{messageId}", result);
        } catch (Exception e) {
            log.error("Can't browse message", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/message/{stepName}/browse/{messageId}", e.getMessage());
        }

    }

    /**
     * GET  /brokers/{brokerType}/messages/{stepName}/browse : list of messages on the specified step
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param stepName, the name of the queue
     * @return list of messages (body or headers) with status 200 (OK) or with status 404 (Not Found)
     */
    @GetMapping(path = "/brokers/{brokerType}/messages/{stepName}/browse", produces = {"text/plain","application/xml","application/json"})
    public Object browseMessages(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable String brokerType, @PathVariable String stepName, @RequestParam(value = "page", required = false) Integer page, @RequestParam(value = "numberOfMessages", required = false) Integer numberOfMessages, @RequestParam(value = "excludeBody", required = false) boolean excludeBody)  throws Exception {

        log.debug("REST request to browse messages on: {}", stepName);

        try {
            result = broker.browseMessages(brokerType,stepName, page, numberOfMessages, mediaType, excludeBody);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, "text", "/brokers/{brokerType}/messages/{stepName}/browse", result);
        } catch (Exception e) {
            log.error("Can't browse messages", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/messages/{stepName}/browse", e.getMessage());
        }

    }


    /**
     * POST  /brokers/{brokerType}/message/{stepName}/send/{messageHeaders} : send a message.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param stepName, the name of the step (queue or topic)
     * @param messageHeaders, the message headers (json map)
     * @return the status (success) with status 200 (OK) or with status 404 (Not Found)
     */
    @PostMapping(path = "/brokers/{brokerType}/message/{stepName}/send", consumes = {"text/plain","application/xml","application/json"}, produces = {"text/plain","application/xml","application/json"})
    public Object sendMessage(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable String brokerType, @PathVariable String stepName, @RequestParam(value = "messageHeaders", required = false) String messageHeaders, @RequestBody String messageBody) throws Exception {

        log.debug("REST request to send messages from queue : " + stepName);

        Map<String,Object> messageHeadersMap = null;
        if(messageHeaders!=null){
            messageHeadersMap = new ObjectMapper().readValue(messageHeaders, HashMap.class);
        }

        try {
            result = broker.sendMessage(brokerType,stepName,messageHeadersMap,messageBody);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, mediaType, "/brokers/{brokerType}/message/{stepName}/send/{messageHeaders}", result);
        } catch (Exception e) {
            log.error("Can't send message", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/message/{stepName}/send/{messageHeaders}", e.getMessage());
        }

    }


    /**
     * DELETE /brokers/{brokerType}/message/{stepName}/{messageId : remove a message on a step by Messageid.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param stepName, the name of the queue or topic
     * @return the status (success) with status 200 (OK) or with status 404 (Not Found)
     */
    @DeleteMapping(path = "/brokers/{brokerType}/message/{stepName}/{messageId}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> removeMessage(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable String brokerType, @PathVariable String stepName, @PathVariable String messageId)  throws Exception {

        log.debug("REST request to remove messages for queue : {}", stepName);

        try {
            result = broker.removeMessage(brokerType,stepName, messageId);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, mediaType, "/brokers/{brokerType}/message/{stepName}/{messageId}", result);
        } catch (Exception e) {
            log.error("Can't remove message", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/message/{stepName}/{messageId}", e.getMessage());
        }

    }

    /**
     * DELETE  /brokers/{brokerType}/messages/{stepName} : remove all messages on a step.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param stepName, the name of the step (topic or queue)
     * @return the status (success) with status 200 (OK) or with status 404 (Not Found)
     */
    @DeleteMapping(path = "/brokers/{brokerType}/messages/{stepName}", produces = {"text/plain","application/xml","application/json"})
    public Object removeMessages(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable String brokerType, @PathVariable String stepName)  throws Exception {

        log.debug("REST request to remove messages for step : {}", stepName);

        try {
            result = broker.removeMessages(brokerType,stepName);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(ID, mediaType, "/brokers/{brokerType}/messages/{stepName}", result);
        } catch (Exception e) {
            log.error("Can't remove messages", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/messages/{stepName}", e.getMessage());
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
    public Object moveMessage(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable String brokerType, @PathVariable String sourceQueueName, @PathVariable String targetQueueName, @PathVariable String messageId)  throws Exception {

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
    @PostMapping(path = "/brokers/{brokerType}/messages/{sourceQueueName}/{targetQueueName}", produces = {"text/plain","application/xml","application/json"})
    public Object moveMessages(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable String brokerType, @PathVariable String sourceQueueName, @PathVariable String targetQueueName)  throws Exception {

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
