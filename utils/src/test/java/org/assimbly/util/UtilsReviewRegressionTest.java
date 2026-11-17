package org.assimbly.util;

import org.assimbly.util.api.ApiUtils;
import org.assimbly.util.domain.FlowInfo;
import org.assimbly.util.domain.FlowRegistry;
import org.assimbly.util.domain.FlowState;
import org.assimbly.util.error.ValidationErrorMessage;
import org.assimbly.util.helper.PropertiesHelper;
import org.assimbly.util.rest.LogUtil;
import org.assimbly.util.rest.ResponseUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.ResponseEntity;

import java.nio.file.Files;
import java.nio.file.Path;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

class UtilsReviewRegressionTest {

    private static final String ROUTES = "<routes><route id=\"first\"><from uri=\"direct:a\"/></route><route id=\"second\"><from uri=\"direct:b\"/></route></routes>";

    @Test
    void validationErrorMessagesWithTheSameErrorAreEqual() {
        assertThat(new ValidationErrorMessage("invalid url")).isEqualTo(new ValidationErrorMessage("invalid url"));
        assertThat(new ValidationErrorMessage("invalid url")).isNotEqualTo(new ValidationErrorMessage("invalid cron"));
        assertThat(new ValidationErrorMessage()).isEqualTo(new ValidationErrorMessage());
    }

    @Test
    void flowWithoutTenantBelongsToNoTenant() {
        FlowRegistry registry = FlowRegistry.getInstance();
        new FlowInfo("test", "review-flow-1", "name", "1", "group", "1", null, FlowState.ACTIVE).register();
        new FlowInfo("test", "review-flow-2", "name", "1", "group", "1", "tenant", FlowState.ACTIVE).register();

        try {
            assertThat(registry.belongsToTenant("review-flow-1", "tenant")).isFalse();
            assertThat(registry.belongsToTenant("review-flow-2", "tenant")).isTrue();
            assertThat(registry.belongsToTenant("review-flow-2", null)).isFalse();
            assertThat(registry.belongsToTenant("unknown", "tenant")).isFalse();
            assertThat(registry.getRegisteredFlows()).containsKeys("review-flow-1", "review-flow-2");
        } finally {
            registry.unregister("review-flow-1");
            registry.unregister("review-flow-2");
        }
    }

    @Test
    void tailKeepsCharactersOfSeveralBytes(@TempDir Path tempDir) throws Exception {
        Path file = Files.writeString(tempDir.resolve("test.log"), "één\ntwee €\ndrie ü\n");

        assertThat(LogUtil.tail(file.toFile(), 2)).isEqualTo("twee €\ndrie ü\n");
        assertThat(LogUtil.tail(file.toFile(), 10)).isEqualTo("één\ntwee €\ndrie ü\n");
    }

    @Test
    void tailOfAMissingFileIsNull(@TempDir Path tempDir) {
        assertThat(LogUtil.tail(tempDir.resolve("missing.log").toFile(), 2)).isNull();
    }

    @Test
    void propertyIsReadFromTheConfigFile() {
        assertThat(PropertiesHelper.getProperty("max_uncompressed_file_size")).isEqualTo("52428800");
        assertThat(PropertiesHelper.getProperty("unknown")).isNull();
    }

    @Test
    void routeIsExtractedById() throws Exception {
        assertThat(ApiUtils.extractRouteFromXmlByRouteId(ROUTES, "second")).contains("direct:b").doesNotContain("direct:a");
        assertThat(ApiUtils.extractRouteFromXmlByRouteId(ROUTES, "unknown")).isNull();
    }

    @Test
    void responseBodyFollowsTheMediaType() {
        ResponseEntity<String> json = ResponseUtil.createSuccessResponse(1L, "application/json", "/path", "done");
        ResponseEntity<String> xml = ResponseUtil.createFailureResponse(1L, "application/XML", "/path", "wrong");
        ResponseEntity<String> text = ResponseUtil.createFailureResponse(1L, "text/plain", "/path", "wrong");

        assertThat(json.getStatusCode().value()).isEqualTo(200);
        assertThatJson(json.getBody()).isObject()
                .containsEntry("id", "1").containsEntry("details", "successful").containsEntry("path", "/path").containsEntry("message", "done")
                .containsKeys("status", "timestamp");
        assertThat(xml.getStatusCode().value()).isEqualTo(400);
        assertThat(xml.getBody()).startsWith("<response>").endsWith("</response>")
                .contains("<status>400</status>", "<message>wrong</message>", "<details>failed. See the log for a complete stack trace</details>");
        assertThat(text.getBody()).isEqualTo("wrong");
    }

    @Test
    void plainResponseAndHeaders() {
        assertThat(ResponseUtil.createSuccessResponse(1L, "application/json", "/path", "done", true).getBody()).isEqualTo("done");
        assertThat(ResponseUtil.createFailureResponse(1L, "application/json", "/path", "wrong", true).getBody()).isEqualTo("wrong");

        ResponseEntity<String> failure = ResponseUtil.createFailureResponseWithHeaders(1L, "text/plain", "/path", "wrong", "alert", "flow");
        assertThat(failure.getStatusCode().value()).isEqualTo(400);
        assertThat(failure.getHeaders().getFirst("X-gatewayApp-alert")).isEqualTo("alert");
        assertThat(failure.getHeaders().getFirst("X-gatewayApp-params")).isEqualTo("flow");
    }

    @Test
    void routeIdIsNotReadAsXPath() throws Exception {
        assertThat(ApiUtils.extractRouteFromXmlByRouteId(ROUTES, "x' or '1'='1")).isNull();
    }

}
