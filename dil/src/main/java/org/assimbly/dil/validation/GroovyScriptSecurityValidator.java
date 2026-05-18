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

        public SecurityCheckCustomizer() {
            super(CompilePhase.SEMANTIC_ANALYSIS);
        }

        @Override
        public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) {
            classNode.getMethods().forEach(method ->
                    method.getCode().visit(new CodeVisitorSupport() {

                        // Catches fully-qualified static calls: java.lang.System.exit()
                        @Override
                        public void visitStaticMethodCallExpression(StaticMethodCallExpression call) {
                            String owner = call.getOwnerType().getName();
                            String name  = call.getMethod();

                            if ("java.lang.System".equals(owner) && "exit".equals(name)) {
                                throw new SecurityException("Sandbox Denial: System.exit() is not allowed.");
                            }
                            if ("java.util.TimeZone".equals(owner) && "setDefault".equals(name)) {
                                throw new SecurityException("Sandbox Denial: Cannot change global TimeZone.");
                            }
                            super.visitStaticMethodCallExpression(call);
                        }

                        // Catches both instance/dynamic calls AND unqualified static calls
                        // e.g. TimeZone.setDefault(), System.exit(), obj.getClass()
                        @Override
                        public void visitMethodCallExpression(MethodCallExpression call) {
                            String name     = call.getMethodAsString();
                            String receiver = call.getObjectExpression().getText();

                            if ("getClass".equals(name) || "class".equals(name)) {
                                throw new SecurityException("Sandbox Denial: Reflection is forbidden.");
                            }
                            if ("java.lang.System".equals(receiver) && "exit".equals(name)) {
                                throw new SecurityException("Sandbox Denial: System.exit() is not allowed.");
                            }
                            if ("java.util.TimeZone".equals(receiver) && "setDefault".equals(name)) {
                                throw new SecurityException("Sandbox Denial: Cannot change global TimeZone.");
                            }
                            super.visitMethodCallExpression(call);
                        }
                    })
            );
        }
    }
}