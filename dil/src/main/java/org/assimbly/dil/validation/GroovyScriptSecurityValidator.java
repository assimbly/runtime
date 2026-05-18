package org.assimbly.dil.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.*;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.w3c.dom.*;
import org.yaml.snakeyaml.Yaml;

import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GroovyScriptSecurityValidator {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String SCRIPT = "script";
    private static final String GROOVY = "groovy";

    private GroovyScriptSecurityValidator() {}

    public static void validate(String mediaType, String configuration) throws Exception {
        List<String> scripts;

        if (mediaType.toLowerCase().contains("xml")) {
            scripts = extractFromXml(configuration);
        } else if (mediaType.toLowerCase().contains("json")) {
            scripts = extractFromJson(configuration);
        } else {
            scripts = extractFromYaml(configuration);
        }

        for (int i = 0; i < scripts.size(); i++) {
            validateScript(scripts.get(i), i + 1);
        }
    }

    // -------------------------------------------------------------------------
    // Extractors
    // -------------------------------------------------------------------------

    private static List<String> extractFromXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); // disabling DOCTYPE - Classic XXE (XML External Entity) vulnerability warning
        Document doc = factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes()));

        NodeList nodes = (NodeList) XPathFactory.newInstance().newXPath()
                .compile("//*[local-name()='script']/*[local-name()='groovy']")
                .evaluate(doc, XPathConstants.NODESET);

        List<String> scripts = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            String text = nodes.item(i).getTextContent().trim();
            if (!text.isBlank()) scripts.add(text);
        }
        return scripts;
    }

    private static List<String> extractFromJson(String json) throws Exception {
        List<String> scripts = new ArrayList<>();
        collectFromJsonNode(mapper.readTree(json), scripts);
        return scripts;
    }

    private static void collectFromJsonNode(JsonNode node, List<String> scripts) {
        if (node.isObject()) {
            if (node.has(SCRIPT) && node.get(SCRIPT).has(GROOVY)) {
                String text = node.get(SCRIPT).get(GROOVY).asText().trim();
                if (!text.isBlank()) scripts.add(text);
            }
            node.fields().forEachRemaining(entry -> collectFromJsonNode(entry.getValue(), scripts));
        } else if (node.isArray()) {
            node.forEach(child -> collectFromJsonNode(child, scripts));
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractFromYaml(String yaml) {
        List<String> scripts = new ArrayList<>();
        Object parsed = new Yaml().load(yaml);
        collectFromYamlObject(parsed, scripts);
        return scripts;
    }

    @SuppressWarnings("unchecked")
    private static void collectFromYamlObject(Object obj, List<String> scripts) {
        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            if (map.containsKey(SCRIPT) && map.get(SCRIPT) instanceof Map) {
                Map<String, Object> scriptBlock = (Map<String, Object>) map.get(SCRIPT);
                if (scriptBlock.containsKey(GROOVY)) {
                    String text = String.valueOf(scriptBlock.get(GROOVY)).trim();
                    if (!text.isBlank()) scripts.add(text);
                }
            }
            map.values().forEach(v -> collectFromYamlObject(v, scripts));
        } else if (obj instanceof List) {
            ((List<?>) obj).forEach(item -> collectFromYamlObject(item, scripts));
        }
    }

    // -------------------------------------------------------------------------
    // AST Validator
    // -------------------------------------------------------------------------

    public static void validateScript(String scriptText, int index) {
        CompilerConfiguration config = new CompilerConfiguration();
        config.addCompilationCustomizers(new SecurityCheckCustomizer());

        try {
            new groovy.lang.GroovyShell(config).parse(scriptText);
        } catch (Exception e) {
            throw new SecurityException(
                    "Groovy script #" + index + " failed to parse for security reasons: " + e.getMessage(), e);
        }
    }

    private static class SecurityCheckCustomizer extends CompilationCustomizer {

        private static final Map<String, Set<String>> FORBIDDEN_STATIC_CALLS = Map.of(
                "java.lang.System",   Set.of("exit"),
                "java.util.TimeZone", Set.of("setDefault")
        );

        private static final Set<String> FORBIDDEN_METHOD_NAMES = Set.of("getClass", "class");

        public SecurityCheckCustomizer() {
            super(CompilePhase.SEMANTIC_ANALYSIS);
        }

        @Override
        public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) {
            classNode.getMethods().forEach(method ->
                    method.getCode().visit(new CodeVisitorSupport() {

                        @Override
                        public void visitStaticMethodCallExpression(StaticMethodCallExpression call) {
                            checkForbiddenCall(call.getOwnerType().getName(), call.getMethod());
                            super.visitStaticMethodCallExpression(call);
                        }

                        @Override
                        public void visitMethodCallExpression(MethodCallExpression call) {
                            String name = call.getMethodAsString();
                            if (FORBIDDEN_METHOD_NAMES.contains(name)) {
                                throw new SecurityException("Sandbox Denial: Reflection is forbidden.");
                            }
                            checkForbiddenCall(call.getObjectExpression().getText(), name);
                            super.visitMethodCallExpression(call);
                        }
                    })
            );
        }

        private static void checkForbiddenCall(String receiver, String method) {
            Set<String> forbidden = FORBIDDEN_STATIC_CALLS.get(receiver);
            if (forbidden != null && forbidden.contains(method)) {
                throw new SecurityException("Sandbox Denial: " + receiver + "." + method + "() is not allowed.");
            }
        }
    }
}