package org.assimbly.dil.validation;

import org.assimbly.dil.validation.beans.script.EvaluationRequest;
import org.assimbly.dil.validation.beans.script.EvaluationResponse;
import org.assimbly.dil.validation.beans.script.ExchangeDto;
import org.assimbly.dil.validation.beans.script.ScriptDto;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every language the Script editor offers can be tried on a sample message; the starters are the Gateway's (script-flow.ts). */
class ScriptValidatorTest {

    private final ScriptValidator validator = new ScriptValidator();

    @Test
    void datasonnet() {
        // Like the datasonnet-script kamelet, the result is the Document DataSonnet makes, as the Flow's Log Step shows it.
        EvaluationResponse response = run("datasonnet", "{\n  message: \"Hello world\"\n}", null);
        assertEquals(1, response.getCode(), response.getResult());
        assertTrue(response.getResult().contains("message=Hello world"), response.getResult());
    }

    @Test
    void groovy() {
        assertOk("Hello world", run("groovy", "'Hello ' + (body ?: 'world')", null));
        assertOk("Hello Groovy", run("groovy", "'Hello ' + (body ?: 'world')", "Groovy"));
    }

    @Test
    void jactl() {
        assertOk("Hello world", run("jactl", "\"Hello ${body ?: 'world'}\"", null));
    }

    @Test
    void javascript() {
        assertOk("Hello world", run("javascript", "'Hello ' + (body || 'world')", null));
        assertOk("Hello js", run("js", "'Hello ' + (body || 'world')", "js"));
    }

    @Test
    void python() {
        assertOk("Hello world", run("python", "f\"Hello {body or 'world'}\"", null));
    }

    @Test
    void simple() {
        EvaluationResponse response = run("simple", "Hello world at ${date:now:HH:mm:ss}", null);
        assertEquals(1, response.getCode(), response.getResult());
        assertTrue(response.getResult().matches("Hello world at \\d\\d:\\d\\d:\\d\\d"), response.getResult());
    }

    @Test
    void simpleReadsHeaders() {
        ExchangeDto exchange = new ExchangeDto(Map.of(), Map.of("name", "header"), "body");
        EvaluationResponse response = validator.validate(new EvaluationRequest(exchange, new ScriptDto("simple", "${body} and ${header.name}")));
        assertOk("body and header", response);
    }

    @Test
    void jslt() {
        assertOk("{\"message\":\"Hello world\"}", run("jslt", "{\n  \"message\": \"Hello world\"\n}", "{}"));
        assertOk("{\"greeting\":\"Hello World\"}", run("jslt", "{\"greeting\": \"Hello \" + .name}", "{\"name\":\"World\"}"));
    }

    @Test
    void xslt() {
        String starter = """
            <xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
              <xsl:template match="/">
                <message>Hello world</message>
              </xsl:template>
            </xsl:stylesheet>""";
        EvaluationResponse response = run("xslt", starter, "<root/>");
        assertEquals(1, response.getCode(), response.getResult());
        assertTrue(response.getResult().contains("<message>Hello world</message>"), response.getResult());
        assertFalse(response.getExchange().getHeaders().containsKey("CamelXsltStylesheet"));
    }

    @Test
    void transformationsAskForASampleBody() {
        EvaluationResponse jslt = run("jslt", "{\n  \"message\": \"Hello world\"\n}", null);
        assertEquals(-1, jslt.getCode());
        assertEquals("JSLT transforms the message body, and there is none: give a sample body in JSON to try the script.", jslt.getResult());

        EvaluationResponse xslt = run("xslt", "<xsl:stylesheet version=\"3.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"/>", null);
        assertEquals(-1, xslt.getCode());
        assertTrue(xslt.getResult().contains("sample body in XML"), xslt.getResult());
    }

    @Test
    void brokenGroovyReportsTheSyntaxError() {
        EvaluationResponse response = run("groovy", "'Hello ' +", null);
        assertError(response);
        assertTrue(response.getResult().startsWith("Syntax Error: "), response.getResult());
    }

    @Test
    void groovyStaysSandboxed() {
        EvaluationResponse response = run("groovy", "System.exit(0)", null);
        assertError(response);
        assertTrue(response.getResult().startsWith("Security Error: "), response.getResult());
    }

    @Test
    void brokenScriptsReportTheirError() {
        assertError(run("javascript", "unknownFunction()", null));
        assertError(run("simple", "${unknownFunction()}", null));
        assertError(run("python", "1 +", null));
        assertError(run("jslt", "{ \"a\": ", "{}"));
        assertError(run("xslt", "<xsl:stylesheet", "<root/>"));
    }

    @Test
    void unknownLanguageIsRefused() {
        EvaluationResponse response = run("cobol", "DISPLAY 'HELLO'.", null);
        assertEquals(-1, response.getCode());
        assertEquals("Unsupported scripting language: 'cobol'", response.getResult());
    }

    @Test
    void emptyRequestsAreRefused() {
        assertEquals(-1, validator.validate(null).getCode());
        assertEquals(-1, validator.validate(new EvaluationRequest(new ExchangeDto(), null)).getCode());
        assertEquals(-1, validator.validate(new EvaluationRequest(new ExchangeDto(), new ScriptDto("groovy", null))).getCode());
    }

    private EvaluationResponse run(String language, String script, String body) {
        return validator.validate(new EvaluationRequest(new ExchangeDto(null, null, body), new ScriptDto(language, script)));
    }

    private static void assertOk(String expected, EvaluationResponse response) {
        assertEquals(1, response.getCode(), response.getResult());
        assertEquals(expected, response.getResult());
    }

    private static void assertError(EvaluationResponse response) {
        assertEquals(-1, response.getCode(), response.getResult());
        assertTrue(response.getResult() != null && !response.getResult().isBlank());
    }
}
