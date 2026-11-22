package org.assimbly.dil.kamelets;

import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteTemplateDefinition;
import org.apache.camel.model.RouteTemplateParameterDefinition;
import org.apache.camel.model.StepDefinition;
import org.apache.camel.model.SwitchDefinition;
import org.apache.camel.model.language.HeaderExpression;
import org.apache.camel.model.language.LanguageExpression;
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies switch-router and switchbyheader-router Kamelets load cleanly as Camel route templates and are indexed. */
class SwitchKameletsTest {

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
    @ValueSource(strings = { "switch-router", "switchbyheader-router" })
    void loadsAsDilRouteTemplateWithSwitchEip(String name) throws Exception {
        PluginHelper.getRoutesLoader(context).loadRoutes(
                ResourceHelper.resolveMandatoryResource(context, "classpath:kamelets/" + name + ".kamelet.yaml"));

        RouteTemplateDefinition template = context.getRouteTemplateDefinition(name);
        assertNotNull(template, "Expected route template with id " + name);
        assertEquals("{{routeConfigurationId}}", template.getRoute().getRouteConfigurationId());

        Set<String> params = template.getTemplateParameters().stream()
                .map(RouteTemplateParameterDefinition::getName)
                .collect(Collectors.toSet());

        assertTrue(params.containsAll(List.of("in", "out", "out_default", "routeId", "stepId", "routeConfigurationId")),
                "Missing DIL parameters in " + name + ": " + params);

        ProcessorDefinition<?> firstOutput = template.getRoute().getOutputs().getFirst();
        StepDefinition step = assertInstanceOf(StepDefinition.class, firstOutput);
        SwitchDefinition switchDef = assertInstanceOf(SwitchDefinition.class, step.getOutputs().getFirst());

        assertNotNull(switchDef.getSelector(), "Expected switch selector in " + name);
        assertEquals(1, switchDef.getCases().size(), "Expected case definition in " + name);
        assertNotNull(switchDef.getOtherwise(), "Expected otherwise definition in " + name);
        assertEquals("{{out_default}}", switchDef.getOtherwise().getUri());

        if (name.equals("switch-router")) {
            assertTrue(params.containsAll(List.of("language", "expression", "value")),
                    "Missing switch-router parameters: " + params);
            LanguageExpression expr = assertInstanceOf(LanguageExpression.class, switchDef.getSelector().getExpressionType());
            assertEquals("{{language}}", expr.getLanguage());
            assertEquals("{{expression}}", expr.getExpression());
            assertEquals("{{value}}", switchDef.getCases().getFirst().getValue());
        } else {
            assertTrue(params.containsAll(List.of("headerName", "headerValue")),
                    "Missing switchbyheader-router parameters: " + params);
            HeaderExpression expr = assertInstanceOf(HeaderExpression.class, switchDef.getSelector().getExpressionType());
            assertEquals("{{headerName}}", expr.getExpression());
            assertEquals("{{headerValue}}", switchDef.getCases().getFirst().getValue());
        }
    }

    @Test
    void switchKameletsAreListedInClasspathIndex() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/kamelets/index.txt")) {
            assertNotNull(is, "/kamelets/index.txt should be generated on the classpath");
            Set<String> indexed = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.toSet());

            assertTrue(indexed.contains("switch-router.kamelet.yaml"), "Expected switch-router.kamelet.yaml in index");
            assertTrue(indexed.contains("switchbyheader-router.kamelet.yaml"), "Expected switchbyheader-router.kamelet.yaml in index");
        }
    }
}
