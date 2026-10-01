package org.assimbly.integrationrest.docs;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.core.util.Json;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.customizers.ParameterCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static java.util.Map.entry;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

/**
 * OpenAPI (Swagger UI) metadata: the description and example of parameters that occur on many
 * endpoints, and the example request bodies (keyed by controller method name). An explicit {@code @Parameter}
 * on a method wins over the parameter defaults.
 */
@Configuration
public class OpenApiConfig {

    private record Doc(String description, Object example) {}

    /** Request body documentation; a null mediaType applies to every media type the endpoint consumes. */
    private record Body(String description, String example, String mediaType) {
        Body(String description, String example) {
            this(description, example, null);
        }
    }

    private static final Map<String, Doc> PARAMETERS = Map.ofEntries(
            entry("flowId", new Doc("Id of the flow", "69fc94aed4e0d00010000040")),
            entry("routeId", new Doc("Id of the Camel route", "route1")),
            entry("stepId", new Doc("Id of the step within the flow", "b1c55ffa-2cf0-4135-bfcd-980c57d832d4")),
            entry("collectorId", new Doc("Id of the collector configuration", "69fc94aed4e0d00010000040_log")),
            entry("componenttype", new Doc("Component type (Camel scheme)", "http")),
            entry("templatename", new Doc("Name of the step template", "setbody")),
            entry("timeout", new Doc("Max wait in milliseconds", 3000)),
            entry("maxNumberOfEntries", new Doc("Maximum number of entries to return", 100)),
            entry("topEntries", new Doc("Return only the top n entries", 10)),
            entry("filterByStatus", new Doc("Only flows with this status", "started")),
            entry("IncludeCustomComponents", new Doc("Also list custom components", true)),
            entry("Type", new Doc("Type of health check", "route")),
            entry("IncludeSteps", new Doc("Include the steps of the flow", true)),
            entry("IncludeError", new Doc("Include the last error", true)),
            entry("IncludeDetails", new Doc("Include details", true)),
            entry("IncludeMetaData", new Doc("Include flow metadata", true)),
            entry("FullStats", new Doc("Return all statistics instead of a summary", true)),
            entry("IsPredicate", new Doc("Evaluate as a predicate (true/false) instead of an expression", false)),
            entry("expression", new Doc("Cron expression", "0 * * * * ?")),
            entry("httpUrl", new Doc("URL to check", "https://example.com")),
            entry("httpsUrl", new Doc("HTTPS URL to check", "https://example.com")),
            entry("Uri", new Doc("Camel URI to check", "timer:demo?period=1000")),
            entry("host", new Doc("Host name or IP address", "example.com")),
            entry("port", new Doc("Port number", 443)),
            entry("keystoreName", new Doc("Name of the keystore file; defaults to outbound-truststore.p12, for identities to server-identity.p12", null)),
            entry("keystorePassword", new Doc("Password of the keystore; defaults to the runtime's keystore password", null)),
            entry("certificateName", new Doc("Name (alias) of the certificate in the keystore", "github-com-root-ca")),
            entry("domain", new Doc("Host name, optionally with port, to download the TLS certificates from", "www.github.com")),
            entry("certificateType", new Doc("Which certificates of the chain to store: root, intermediate, leaf or all", "root")),
            entry("numberOfDays", new Doc("Number of days from now", 30)),
            entry("cn", new Doc("Common name of the certificate", "example.com")),
            entry("password", new Doc("Password of the .p12 file", "supersecret")),
            entry("url", new Doc("URL the certificates belong to", "https://example.com")),
            entry("type", new Doc("Kamelet type (when the component is a kamelet)", "source")),
            entry("uid", new Doc("Unique id of the error", "0e5f4b1a-7f43-4c1e-9d1e-2a7a9c1f3b55")),
            entry("name", new Doc("Endpoint name", "timer")),
            entry("scheme", new Doc("Endpoint scheme", "http")),
            entry("tenant", new Doc("Tenant name", "default")),
            entry("numberOfTimes", new Doc("Number of times to send the message", 1)),
            entry("uri", new Doc("Camel URI to send to", "direct:start")),
            entry("serviceid", new Doc("Id of the service (connection) to use", "69fc94aed4e0d00010000050")),
            entry("serviceKeys", new Doc("Service properties as JSON", "{\"host\":\"localhost\",\"port\":\"5672\"}")),
            entry("headerKeys", new Doc("Message headers as JSON", "{\"Content-Type\":\"application/json\"}"))
    );

    private static final Map<String, Body> BODIES = Map.ofEntries(
            entry("setFlowConfiguration", new Body("Flow configuration", ApiExamples.FLOW_JSON, APPLICATION_JSON_VALUE)),
            entry("installFlow", new Body("Flow configuration", ApiExamples.FLOW_JSON, APPLICATION_JSON_VALUE)),
            entry("testFlow", new Body("Flow configuration", ApiExamples.FLOW_JSON, APPLICATION_JSON_VALUE)),
            entry("installRoute", new Body("Camel route", ApiExamples.ROUTE_XML, APPLICATION_XML_VALUE)),
            entry("getHealthByFlowIds", new Body("Comma separated flow ids", "69fc94aed4e0d00010000040,69fc94aed4e0d00010000041")),
            entry("getStatsByFlowIds", new Body("Comma separated flow ids", "69fc94aed4e0d00010000040,69fc94aed4e0d00010000041")),
            entry("setBaseDirectory", new Body("Path of the base directory", "/data/assimbly")),
            entry("getListOfSoapActions", new Body("URL of the WSDL", "https://example.com/service?wsdl")),
            entry("addCollectors", new Body("List of collector configurations", "[" + ApiExamples.COLLECTOR_JSON + "]", APPLICATION_JSON_VALUE)),
            entry("removeCollectors", new Body("List of collector configurations", "[" + ApiExamples.COLLECTOR_JSON + "]", APPLICATION_JSON_VALUE)),
            entry("addCollectorConfiguration", new Body("Collector configuration", ApiExamples.COLLECTOR_JSON, APPLICATION_JSON_VALUE)),
            entry("send", new Body("Message body", "Hello world")),
            entry("sendRequest", new Body("Message body", "Hello world")),
            entry("importTrustedCertificates", new Body("PEM certificate(s), or base64 encoded PEM or DER", ApiExamples.CERTIFICATE_PEM)),
            entry("replaceTrustedCertificate", new Body("One PEM certificate, or base64 encoded PEM or DER", ApiExamples.CERTIFICATE_PEM)),
            entry("importIdentity", new Body("Base64 encoded PKCS12 (.p12/.pfx) file", "MIIKdAIBAzCCCi4GCSqGSIb3DQEHAaCCCh8Egg...")),
            entry("replaceIdentity", new Body("Base64 encoded PKCS12 (.p12/.pfx) file with one private key", "MIIKdAIBAzCCCi4GCSqGSIb3DQEHAaCCCh8Egg...")),
            entry("validateExpression", new Body("Expressions to validate", "[{\"name\":\"CheckInvoice\",\"expression\":\"1 + 1\",\"language\":\"groovy\",\"nextNode\":\"nextStep\"}]")),
            entry("validateFtp", new Body("FTP connection settings", "{\"host\":\"test.rebex.net\",\"port\":21,\"user\":\"demo\",\"pwd\":\"password\",\"protocol\":\"ftp\"}")),
            entry("validateRegex", new Body("Regular expression", "{\"expression\":\"^[a-zA-Z0-9]+$\"}")),
            entry("validateScript", new Body("Script and the exchange to run it on", "{\"script\":{\"language\":\"groovy\",\"script\":\"return 1 + 1;\"},\"exchange\":{\"body\":\"\",\"headers\":{},\"properties\":{}}}")),
            entry("validateXslt", new Body("XSLT as url or body", "{\"xsltUrl\":\"https://www.w3schools.com/xml/cdcatalog_client.xsl\"}")),
            entry("validateCredentials", new Body("Provider and credentials", "{\"provider\":\"openai\",\"apiKey\":\"sk-...\"}")),
            entry("getAvailableModels", new Body("Provider and credentials", "{\"provider\":\"openai\",\"apiKey\":\"sk-...\"}"))
    );

    // the gateway app loads these classes too: only document our own controllers, parameter names like id or host are ambiguous
    private static boolean ownController(Class<?> type) {
        return type != null && type.getPackageName().startsWith("org.assimbly.integrationrest");
    }

    @Bean
    public ParameterCustomizer parameterDocs() {
        return (parameter, methodParameter) -> {
            Doc doc = parameter == null || !ownController(methodParameter.getContainingClass()) ? null : PARAMETERS.get(parameter.getName());
            if (doc != null) {
                if (parameter.getDescription() == null) parameter.setDescription(doc.description());
                if (parameter.getExample() == null && parameter.getExamples() == null) parameter.setExample(doc.example());
            }
            return parameter;
        };
    }

    // JSON as a tree, so Swagger UI shows it pretty-printed instead of as one quoted string
    private static Object example(String mediaType, String text) {
        if (!APPLICATION_JSON_VALUE.equals(mediaType)) return text;
        try {
            return Json.mapper().readTree(text);
        } catch (JsonProcessingException _) {
            return text;
        }
    }

    @Bean
    public OperationCustomizer bodyDocs() {
        return (operation, handlerMethod) -> {
            Body body = ownController(handlerMethod.getBeanType()) ? BODIES.get(handlerMethod.getMethod().getName()) : null;
            var requestBody = operation.getRequestBody();
            if (body != null && requestBody != null && requestBody.getContent() != null) {
                requestBody.setDescription(body.description());
                requestBody.getContent().forEach((type, media) -> {
                    if (body.mediaType() == null || body.mediaType().equals(type)) {
                        media.setExample(example(type, body.example()));
                    }
                });
            }
            return operation;
        };
    }
}
