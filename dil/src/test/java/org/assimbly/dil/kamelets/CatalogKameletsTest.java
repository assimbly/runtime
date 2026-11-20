package org.assimbly.dil.kamelets;

import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.RouteTemplateDefinition;
import org.apache.camel.model.RouteTemplateParameterDefinition;
import org.apache.camel.support.PluginHelper;
import org.apache.camel.support.ResourceHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the catalog-derived DIL Kamelets load cleanly as Camel route templates and are indexed. */
class CatalogKameletsTest {

    private static final List<String> KAMELETS = List.of(
            "elasticsearch-action",
            "slack-source",
            "slack-action",
            "telegram-source",
            "telegram-action",
            "azure-servicebus-source",
            "azure-servicebus-action");

    private DefaultCamelContext context;

    @BeforeEach
    void createContext() {
        context = new DefaultCamelContext();
    }

    @AfterEach
    void stopContext() {
        context.stop();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "elasticsearch-action",
            "slack-source",
            "slack-action",
            "telegram-source",
            "telegram-action",
            "azure-servicebus-source",
            "azure-servicebus-action"
    })
    void loadsAsDilRouteTemplate(String name) throws Exception {
        PluginHelper.getRoutesLoader(context).loadRoutes(
                ResourceHelper.resolveMandatoryResource(context, "classpath:kamelets/" + name + ".kamelet.yaml"));

        RouteTemplateDefinition template = context.getRouteTemplateDefinition(name);
        assertNotNull(template, "Expected route template with id " + name);
        assertEquals("{{routeConfigurationId}}", template.getRoute().getRouteConfigurationId());

        Set<String> params = template.getTemplateParameters().stream()
                .map(RouteTemplateParameterDefinition::getName)
                .collect(Collectors.toSet());

        assertTrue(params.containsAll(List.of("in", "out", "routeId", "stepId", "routeConfigurationId")),
                "Missing DIL parameters in " + name + ": " + params);
        if (name.endsWith("-action")) {
            assertTrue(params.contains("sink"), "Missing sink parameter in " + name);
        }
    }

    @Test
    void allKameletsAreListedInClasspathIndex() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/kamelets/index.txt")) {
            assertNotNull(is, "/kamelets/index.txt should be generated on the classpath");
            Set<String> indexed = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.toSet());

            for (String name : KAMELETS) {
                assertTrue(indexed.contains(name + ".kamelet.yaml"),
                        "Expected " + name + ".kamelet.yaml in /kamelets/index.txt");
            }
        }
    }
}
