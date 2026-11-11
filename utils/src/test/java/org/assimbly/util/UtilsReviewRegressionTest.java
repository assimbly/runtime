package org.assimbly.util;

import org.assimbly.util.api.ApiUtils;
import org.assimbly.util.domain.FlowInfo;
import org.assimbly.util.domain.FlowRegistry;
import org.assimbly.util.domain.FlowState;
import org.assimbly.util.error.ValidationErrorMessage;
import org.assimbly.util.helper.PropertiesHelper;
import org.assimbly.util.rest.LogUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

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
    void routeIdIsNotReadAsXPath() throws Exception {
        assertThat(ApiUtils.extractRouteFromXmlByRouteId(ROUTES, "x' or '1'='1")).isNull();
    }

}
