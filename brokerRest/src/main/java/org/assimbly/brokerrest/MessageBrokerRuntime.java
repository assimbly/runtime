package org.assimbly.brokerrest;

import org.springframework.web.bind.annotation.*;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.assimbly.util.rest.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

/**
 * REST controller for managing messages on the broker.
 */
@Tag(name = "Broker messages", description = "Browse, send, move and remove messages")
@RestController
@RequestMapping("/api")
public class MessageBrokerRuntime {

    private static final long ID = 0L;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	protected Logger log = LoggerFactory.getLogger(getClass());

    private final ManagedBrokerRuntime broker;

    public MessageBrokerRuntime(ManagedBrokerRuntime broker) {
        this.broker = broker;
    }

    /**
     * GET  /brokers/{brokerType}/messages/{endpointName}: get a list of messages by endpoint.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param endpointName, the name of the queue
     * @param filter, the filter
     * @return list of messages with status 200 (OK) or with status 404 (Not Found)
     */
    @Operation(summary = "List messages of a queue or topic")
    @GetMapping(
            path = "/brokers/{brokerType}/messages/{endpointName}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> listMessages(
            @PathVariable(value = "brokerType") String brokerType,
            @PathVariable(value = "endpointName") String endpointName,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestParam(value = "filter", required = false) String filter
    ) {

        log.debug("event=listMessages type=GET message=Get a list of messages by endpoint name={} type={}", endpointName, brokerType);

        return BrokerResponse.respondConverted(mediaType, "/brokers/{brokerType}/messages/{endpointName}",
                () -> broker.listMessages(brokerType, endpointName, filter, mediaType), endpointName, brokerType);

    }

    /**
     * POST  /brokers/{brokerType}/messages/count : count the messages of the listed endpoints.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param endpointNames, the name of the queue
     * @return list of messages with status 200 (OK) or with status 404 (Not Found)
     */
    @Operation(summary = "Count messages of several queues or topics")
    @PostMapping(
            path = "/brokers/{brokerType}/messages/count",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> countMessagesFromList(
            @PathVariable(value = "brokerType") String brokerType,
            @RequestBody String endpointNames,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.debug("event=countMessagesFromList type=POST message=Count number of messages names={} type={}", endpointNames, brokerType);

        return BrokerResponse.respond(mediaType, "/brokers/{brokerType}/messages/count",
                () -> broker.countMessagesFromList(brokerType, endpointNames), endpointNames, brokerType);

    }

    /**
     * GET  /brokers/{brokerType}/flows/message/count : get a list of number of messages for each flow.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @return list of flows with status 200 (OK) or with status 404 (Not Found)
     */
    @Operation(summary = "Count messages per flow")
    @GetMapping(
            path = "/brokers/{brokerType}/flows/message/count",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowsMessageCountList(
            @PathVariable(value = "brokerType") String brokerType,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestParam(value = "excludeEmptyQueues", defaultValue = "false") boolean excludeEmptyQueues
    ) {

        log.debug("event=getFlowsMessageCountList type=GET message=Get a list of number of messages for each flow excludeEmptyQueues={} type={}", excludeEmptyQueues, brokerType);

        try {
            final String result = broker.getFlowMessageCountsList(brokerType, excludeEmptyQueues);
            return ResponseUtil.createSuccessResponse(ID, mediaType, "/brokers/{brokerType}/flows/message/count", result, true);
        } catch (Exception e) {
            log.error("event=getFlowsMessageCountList type=GET type={} reason={}", brokerType, e.getMessage(), e);
            return ResponseUtil.createFailureResponse(ID, mediaType, "/brokers/{brokerType}/flows/message/count", e.getMessage());
        }

    }

    /**
     * GET  /brokers/{brokerType}/messages/{endpointName}/count : count the number of messages on an endpoint.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param endpointName, the name of the queue or topic
     * @return list of messages with status 200 (OK) or with status 404 (Not Found)
     */
    @Operation(summary = "Count messages")
    @GetMapping(
            path = "/brokers/{brokerType}/messages/{endpointName}/count",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> countMessages(
            @PathVariable(value = "brokerType") String brokerType,
            @PathVariable(value = "endpointName") String endpointName,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.debug("event=countMessages type=GET message=Count the number of messages on an endpoint name={} type={}", endpointName, brokerType);

        return BrokerResponse.respond(mediaType, "/brokers/{brokerType}/messages/{endpointName}/count",
                () -> broker.countMessages(brokerType, endpointName), endpointName, brokerType);

    }

    /**
     * GET  /brokers/{brokerType}/delayedmessages/{endpointName}/count : count the number of messages on endpoint.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param endpointName, the name of the queue or topic
     * @return list of messages with status 200 (OK) or with status 404 (Not Found)
     */
    @Operation(summary = "Count delayed messages")
    @GetMapping(
            path = "/brokers/{brokerType}/delayedmessages/{endpointName}/count",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> countDelayedMessages(
            @PathVariable(value = "brokerType") String brokerType,
            @PathVariable(value = "endpointName") String endpointName,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.debug("event=countDelayedMessages type=GET message=Count the number of delayed messages on an endpoint name={} type={}", endpointName, brokerType);

        return BrokerResponse.respond(mediaType, "/brokers/{brokerType}/delayedmessages/{endpointName}/count",
                () -> broker.countDelayedMessages(brokerType, endpointName), endpointName, brokerType);

    }

    /**
     * GET  /brokers/{brokerType}/message/{endpointName}/browse/{messageId} : get a message on an endpoint by messageId.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param endpointName, the name of the queue
     * @param messageId, the messageId (retrieved to listMessages)
     * @return The message (body and headers) with status 200 (OK) or with status 404 (Not Found)
     */
    @Operation(summary = "Browse a message")
    @GetMapping(
            path = "/brokers/{brokerType}/message/{endpointName}/browse/{messageId}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> browseMessage(
            @PathVariable(value = "brokerType") String brokerType,
            @PathVariable(value = "endpointName") String endpointName,
            @PathVariable(value = "messageId") String messageId,
            @Parameter(hidden = true) @RequestHeader("Accept") String mediaType,
            @RequestParam(value = "excludeBody", defaultValue = "false") boolean excludeBody
    ) {

        log.debug("event=browseMessage type=GET message=Browse message name={} messageId={} type={}", endpointName, messageId, brokerType);

        return BrokerResponse.respondConverted(mediaType, "/brokers/{brokerType}/message/{endpointName}/browse/{messageId}",
                () -> broker.browseMessage(brokerType, endpointName, messageId, mediaType, excludeBody), endpointName, brokerType);

    }

    /**
     * GET  /brokers/{brokerType}/messages/{endpointName}/browse : list of messages on the specified endpoint
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param endpointName, the name of the queue
     * @return list of messages (body or headers) with status 200 (OK) or with status 404 (Not Found)
     */
    @Operation(summary = "Browse messages (paged)")
    @GetMapping(
            path = "/brokers/{brokerType}/messages/{endpointName}/browse",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> browseMessages(
            @PathVariable(value = "brokerType") String brokerType,
            @PathVariable(value = "endpointName") String endpointName,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "numberOfMessages", required = false) Integer numberOfMessages,
            @RequestParam(value = "excludeBody", defaultValue = "false") boolean excludeBody
    ) {

        log.debug("event=browseMessages type=GET message=List of messages on the specified endpoint name={} type={}", endpointName, brokerType);

        return BrokerResponse.respondConverted(mediaType, "/brokers/{brokerType}/messages/{endpointName}/browse",
                () -> broker.browseMessages(brokerType,endpointName, page, numberOfMessages, mediaType, excludeBody), endpointName, brokerType);

    }

    /**
     * POST  /brokers/{brokerType}/message/{endpointName}/send : send a message.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param endpointName, the name of the endpoint (queue or topic)
     * @param messageHeaders, the message headers (json map)
     * @return the status (success) with status 200 (OK) or with status 404 (Not Found)
     */
    @Operation(summary = "Send a message")
    @PostMapping(
            path = "/brokers/{brokerType}/message/{endpointName}/send",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> sendMessage(
            @PathVariable(value = "brokerType") String brokerType,
            @PathVariable(value = "endpointName") String endpointName,
            @RequestBody String messageBody,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestParam(value = "messageHeaders", required = false) String messageHeaders
    ) {

        log.debug("event=sendMessageToEndpoint type=POST message=Send message specified endpoint name={} type={}", endpointName, brokerType);

        return BrokerResponse.respond(mediaType, "/brokers/{brokerType}/message/{endpointName}/send",
                () -> broker.sendMessage(brokerType, endpointName, toHeaders(messageHeaders), messageBody), endpointName, brokerType);

    }

    /**
     * DELETE /brokers/{brokerType}/message/{endpointName}/{messageId} : remove a message on an endpoint by Messageid.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param endpointName, the name of the queue or topic
     * @return the status (success) with status 200 (OK) or with status 404 (Not Found)
     */
    @Operation(summary = "Remove a message")
    @DeleteMapping(
            path = "/brokers/{brokerType}/message/{endpointName}/{messageId}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> removeMessage(
            @PathVariable(value = "brokerType") String brokerType,
            @PathVariable(value = "endpointName") String endpointName,
            @PathVariable(value = "messageId") String messageId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.debug("event=removeMessage type=DELETE message=Remove message on an endpoint name={} messageId={} type={}", endpointName, messageId, brokerType);

        return BrokerResponse.respond(mediaType, "/brokers/{brokerType}/message/{endpointName}/{messageId}",
                () -> broker.removeMessage(brokerType,endpointName, messageId), endpointName, brokerType);

    }

    /**
     * DELETE  /brokers/{brokerType}/messages/{endpointName} : remove all messages on a endpoint.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param endpointName, the name of the endpoint (topic or queue)
     * @return the status (success) with status 200 (OK) or with status 404 (Not Found)
     */
    @Operation(summary = "Remove all messages")
    @DeleteMapping(
            path = "/brokers/{brokerType}/messages/{endpointName}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> removeMessages(
            @PathVariable(value = "brokerType") String brokerType,
            @PathVariable(value = "endpointName") String endpointName,
            @Parameter(hidden = true) @RequestHeader("Accept") String mediaType
    ) {

        log.debug("event=removeMessages type=DELETE message=Remove all messages on a endpoint name={} type={}", endpointName, brokerType);

        return BrokerResponse.respond(mediaType, "/brokers/{brokerType}/messages/{endpointName}",
                () -> broker.removeMessages(brokerType,endpointName), endpointName, brokerType);

    }

    /**
     * POST /brokers/{brokerType}/message/{sourceQueueName}/{targetQueueName}/{messageId} : move a message by messagId to another queue
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param sourceQueueName, the name of the source queue
     * @param targetQueueName, the name of the target queue
     * @return the status (source) with status 200 (OK) or with status 404 (Not Found)
     */
    @Operation(summary = "Move a message to another queue")
    @PostMapping(
            path = "/brokers/{brokerType}/message/{sourceQueueName}/{targetQueueName}/{messageId}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> moveMessage(
            @PathVariable(value = "brokerType") String brokerType,
            @PathVariable(value = "sourceQueueName") String sourceQueueName,
            @PathVariable(value = "targetQueueName") String targetQueueName,
            @PathVariable(value = "messageId") String messageId,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.debug("event=moveMessage type=POST message=Move messages by id sourceName={} targetName={} messageId={} type={}", sourceQueueName, targetQueueName, messageId, brokerType);

        return BrokerResponse.respond(mediaType, "/brokers/{brokerType}/message/{sourceQueueName}/{targetQueueName}/{messageId}",
                () -> broker.moveMessage(brokerType,sourceQueueName, targetQueueName, messageId), sourceQueueName, targetQueueName, messageId, brokerType);

    }

    /**
     * POST  /brokers/{brokerType}/messages/{sourceQueueName}/{targetQueueName} : move all messages to another queue.
     *
     * @param brokerType, the type of broker: classic or artemis
     * @param sourceQueueName, the name of the source queue
     * @param targetQueueName, the name of the target queue
     * @return the status (success) with status 200 (OK) or with status 404 (Not Found)
     */
    @Operation(summary = "Move all messages to another queue")
    @PostMapping(
            path = "/brokers/{brokerType}/messages/{sourceQueueName}/{targetQueueName}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> moveMessages(
            @PathVariable(value = "brokerType") String brokerType,
            @PathVariable(value = "sourceQueueName") String sourceQueueName,
            @PathVariable(value = "targetQueueName") String targetQueueName,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        log.debug("event=moveMessages type=POST message=Move all messages sourceName={} targetName={} type={}", sourceQueueName, targetQueueName, brokerType);

        return BrokerResponse.respond(mediaType, "/brokers/{brokerType}/messages/{sourceQueueName}/{targetQueueName}",
                () -> broker.moveMessages(brokerType,sourceQueueName, targetQueueName), sourceQueueName, targetQueueName, brokerType);

    }

    private static Map<String, Object> toHeaders(String messageHeaders) {
        Map<String, Object> headers = messageHeaders == null ? null
                : OBJECT_MAPPER.readValue(messageHeaders, new TypeReference<>() {});
        return headers == null ? Map.of() : headers;
    }

}
