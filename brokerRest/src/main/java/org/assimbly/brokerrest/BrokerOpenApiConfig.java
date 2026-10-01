package org.assimbly.brokerrest;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.customizers.ParameterCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

/**
 * Description and example of the path/query parameters that occur on many broker endpoints, and the example
 * request bodies (keyed by controller method name), so they don't have to be repeated per method.
 * An explicit {@code @Parameter} on a method wins.
 */
@Configuration
public class BrokerOpenApiConfig {

    private record Doc(String description, Object example, List<String> allowed) {
        Doc(String description, Object example) {
            this(description, example, null);
        }
    }

    private static final Map<String, Doc> PARAMETERS = Map.ofEntries(
            entry("brokerType", new Doc("Type of broker", "classic", List.of("classic", "artemis"))),
            entry("brokerConfigurationType", new Doc("Where the broker configuration comes from", "file")),
            entry("id", new Doc("Id of the broker", "1")),
            entry("queueName", new Doc("Name of the queue", "flow.1.step.1")),
            entry("topicName", new Doc("Name of the topic", "notifications")),
            entry("endpointName", new Doc("Name of the queue or topic", "flow.1.step.1")),
            entry("sourceQueueName", new Doc("Name of the queue to move from", "flow.1.step.1")),
            entry("targetQueueName", new Doc("Name of the queue to move to", "flow.1.step.2")),
            entry("messageId", new Doc("Id of the message", "ID:5f0c2b1e-3e7a-11ef-8a1c-0242ac110002")),
            entry("filter", new Doc("Message selector (JMS syntax)", "JMSPriority > 4")),
            entry("excludeEmptyQueues", new Doc("Skip queues without messages", true)),
            entry("excludeBody", new Doc("Return only headers, not the message body", true)),
            entry("page", new Doc("Page number (starts at 1)", 1)),
            entry("numberOfMessages", new Doc("Messages per page", 20)),
            entry("messageHeaders", new Doc("Message headers as JSON", "{\"priority\":\"5\"}"))
    );

    private record Body(String description, String example) {}

    private static final Map<String, Body> BODIES = Map.of(
            "countMessagesFromList", new Body("Comma separated queue or topic names", "flow.1.step.1,flow.1.step.2"),
            "sendMessage", new Body("Message body", "Hello world")
    );

    // the gateway app loads these classes too: only document our own controllers, parameter names like id or host are ambiguous
    private static boolean ownController(Class<?> type) {
        return type != null && type.getPackageName().startsWith("org.assimbly.brokerrest");
    }

    @Bean
    public OperationCustomizer brokerBodyDocs() {
        return (operation, handlerMethod) -> {
            Body body = ownController(handlerMethod.getBeanType()) ? BODIES.get(handlerMethod.getMethod().getName()) : null;
            var requestBody = operation.getRequestBody();
            if (body != null && requestBody != null && requestBody.getContent() != null) {
                requestBody.setDescription(body.description());
                requestBody.getContent().values().forEach(media -> media.setExample(body.example()));
            }
            return operation;
        };
    }

    @Bean
    public ParameterCustomizer brokerParameterDocs() {
        return (parameter, methodParameter) -> {
            Doc doc = parameter == null || !ownController(methodParameter.getContainingClass()) ? null : PARAMETERS.get(parameter.getName());
            if (doc != null) {
                if (parameter.getDescription() == null) parameter.setDescription(doc.description());
                if (parameter.getExample() == null && parameter.getExamples() == null) parameter.setExample(doc.example());
                if (doc.allowed() != null && parameter.getSchema() != null) parameter.getSchema().setEnum(doc.allowed());
            }
            return parameter;
        };
    }
}
