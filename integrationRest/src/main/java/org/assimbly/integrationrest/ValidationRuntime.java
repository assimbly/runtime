package org.assimbly.integrationrest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Parameter;
import org.assimbly.dil.validation.HttpsCertificateValidator;
import org.assimbly.dil.validation.beans.ValidationExpression;
import org.assimbly.dil.validation.beans.FtpSettings;
import org.assimbly.dil.validation.beans.Regex;
import org.assimbly.dil.validation.beans.script.BadRequestResponse;
import org.assimbly.dil.validation.beans.script.EvaluationRequest;
import org.assimbly.dil.validation.beans.script.EvaluationResponse;
import org.assimbly.integration.Integration;
import org.assimbly.util.error.ValidationErrorMessage;
import org.assimbly.util.rest.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;

/**
 * Resource to return information about the currently running Spring profiles.
 */
@ControllerAdvice
@RestController
@RequestMapping("/api")
public class ValidationRuntime {

	protected Logger log = LoggerFactory.getLogger(getClass());

    private boolean plainResponse;

    private final Integration integration;

    public ValidationRuntime(IntegrationRuntime integrationRuntime) {
        this.integration = integrationRuntime.getIntegration();
    }

    //validations

    @GetMapping(
            path = "/validation/cron",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> validateCron(
            @RequestParam(value = "expression") String expression,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        plainResponse = true;

        try {

            ValidationErrorMessage cronResp = integration.validateCron(expression);

            if(cronResp!=null) {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                final ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(out, cronResp);
                return ResponseUtil.createSuccessResponse(1L, mediaType, "/validation/cron", out.toString(StandardCharsets.UTF_8), plainResponse);
            } else {
                return ResponseUtil.createNoContentResponse(1L, mediaType);
            }

        } catch (Exception e) {
            log.error("ErrorMessage",e);
            return ResponseUtil.createFailureResponse(1L, mediaType, "/validation/cron", e.getMessage(), plainResponse);
        }
    }

    @GetMapping(
            path = "/validation/certificate",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> validateCertificate(
            @RequestParam(value = "httpsUrl") String httpsUrl,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        plainResponse = true;

        try {

            HttpsCertificateValidator.ValidationResult certificateResp = integration.validateCertificate(httpsUrl);

            if(certificateResp!=null) {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                final ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(out, certificateResp);
                return ResponseUtil.createSuccessResponse(1L, mediaType, "/validation/certificate", out.toString(StandardCharsets.UTF_8), plainResponse);
            } else {
                return ResponseUtil.createSuccessResponse(1L, mediaType, "/validation/certificate", "", plainResponse);
            }
        } catch (Exception e) {
            log.error("ErrorMessage",e);
            return ResponseUtil.createFailureResponse(1L, mediaType, "/validation/certificate", e.getMessage(), plainResponse);
        }
    }

    @GetMapping(
            path = "/validation/url",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> validateUrl(
            @RequestParam(value = "httpUrl") String httpUrl,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        plainResponse = true;

        try {

            ValidationErrorMessage urlResp = integration.validateUrl(httpUrl);

            if(urlResp!=null) {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                final ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(out, urlResp);
                return ResponseUtil.createSuccessResponse(1L, mediaType, "/validation/url", out.toString(StandardCharsets.UTF_8), plainResponse);
            } else {
                return ResponseUtil.createNoContentResponse(1L, mediaType);
            }

        } catch (Exception e) {
            log.error("ErrorMessage",e);
            return ResponseUtil.createFailureResponse(1L, mediaType, "/validation/url", e.getMessage(), plainResponse);
        }
    }


    @PostMapping(
            path = "/validation/expression",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> validateExpression(
            @RequestBody String body,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestHeader(value = "IsPredicate", defaultValue = "false") boolean isPredicate
    ) {

        plainResponse = true;

        try {

            List<ValidationExpression> expressionsList = null;
            if(body!=null){
                expressionsList = new ObjectMapper().readValue(body, new TypeReference<>() {
                });
            }

            List<ValidationExpression> expressions = integration.validateExpressions(expressionsList, isPredicate);

            if(expressions!=null) {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                final ObjectMapper mapper = new ObjectMapper();
                mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL); // Ignore null fields
                mapper.writeValue(out, expressions);
                return ResponseUtil.createSuccessResponse(1L, mediaType, "/validation/expression", out.toString(StandardCharsets.UTF_8), plainResponse);
            } else {
                return ResponseUtil.createNoContentResponse(1L, mediaType);
            }

        } catch (Exception e) {
            log.error("Error",e);
            return ResponseUtil.createFailureResponse(1L, mediaType, "/validation/expression", e.getMessage(), plainResponse);
        }

    }

    @PostMapping(
            path = "/validation/ftp",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> validateFtp(
            @RequestBody String body,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestHeader(value = "StopTest", defaultValue = "false") boolean stopTest
    ) {

        plainResponse = true;

        try {

            FtpSettings ftpSettings = null;
            if(body!=null){
                ftpSettings = new ObjectMapper().readValue(body, FtpSettings.class);
            }

            ValidationErrorMessage ftpResp = integration.validateFtp(ftpSettings);

            if(ftpResp!=null) {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                final ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(out, ftpResp);
                return ResponseUtil.createSuccessResponse(1L, mediaType, "/validation/ftp", out.toString(StandardCharsets.UTF_8), plainResponse);
            } else {
                return ResponseUtil.createNoContentResponse(1L, mediaType);
            }

        } catch (Exception e) {
            log.error("Error",e);
            return ResponseUtil.createFailureResponse(1L, mediaType, "/validation/ftp", e.getMessage(), plainResponse);
        }

    }

    @PostMapping(
            path = "/validation/regex",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> validateRegex(
            @RequestBody String body,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestHeader(value = "StopTest", defaultValue = "false") boolean stopTest
    ) {

        plainResponse = true;

        try {

            Regex regex = null;
            if(body!=null){
                regex = new ObjectMapper().readValue(body, Regex.class);
            }

            AbstractMap.SimpleEntry<Integer, String> regexResp = integration.validateRegex(regex);

            if(regexResp!=null) {
                if (regexResp.getKey() == -1) {
                    return ResponseUtil.createFailureResponse(1L, mediaType, "/validation/regex", regexResp.getValue(), plainResponse);
                } else {
                    // success - return group count
                    return ResponseUtil.createSuccessResponse(1L, mediaType, "/validation/regex", regexResp.getValue());
                }
            } else {
                return ResponseUtil.createFailureResponse(1L, mediaType, "/validation/regex", "", plainResponse);
            }

        } catch (Exception e) {
            log.error("Error",e);
            return ResponseUtil.createFailureResponse(1L, mediaType, "/validation/regex", e.getMessage(), plainResponse);
        }

    }

    @PostMapping(
            path = "/validation/script",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> validateScript(
            @RequestBody String body,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestHeader(value = "StopTest", defaultValue = "false") boolean stopTest
    ) {

        plainResponse = true;

        try {

            EvaluationRequest scriptRequest = null;
            if(body!=null){
                scriptRequest = new ObjectMapper().readValue(body, EvaluationRequest.class);
            }

            EvaluationResponse scriptResp = integration.validateScript(scriptRequest);

            if(scriptResp!=null) {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                final ObjectMapper mapper = new ObjectMapper();
                if(scriptResp.getCode() == 1) {
                    mapper.writeValue(out, scriptResp);
                    return ResponseUtil.createSuccessResponse(1L, mediaType, "/validation/script", out.toString(StandardCharsets.UTF_8), plainResponse);
                } else {
                    mapper.writeValue(out, new BadRequestResponse(scriptResp.getResult()));
                    return ResponseUtil.createFailureResponse(1L, mediaType, "/validation/script", out.toString(StandardCharsets.UTF_8), plainResponse);
                }
            } else {
                return ResponseUtil.createFailureResponse(1L, mediaType, "/validation/script", "", plainResponse);
            }

        } catch (Exception e) {
            log.error("Error",e);
            return ResponseUtil.createFailureResponse(1L, mediaType, "/validation/script", e.getMessage(), plainResponse);
        }

    }

    @GetMapping(
            path = "/validation/uri",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> validateUri(
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestHeader(value = "Uri") String uri
    ) {
        try {
            String flowValidation = integration.validateFlow(uri);
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/validation/uri",flowValidation);
        } catch (Exception e) {
            return ResponseUtil.createFailureResponse(1L, mediaType,"/validation/urizx",e.getMessage());
        }
    }

    @PostMapping(path = "/validation/xslt",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> validateXslt(
            @RequestBody String body,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestHeader(value = "StopTest", defaultValue = "false") boolean stopTest
    ) {

        try {

            plainResponse = true;

            HashMap<String,String> paramList;

            if(body!=null){
                paramList = new ObjectMapper().readValue(body, new TypeReference<>() {
                });

                List<ValidationErrorMessage> expressionResp = integration.validateXslt(
                        paramList.get("xsltUrl"),
                        paramList.get("xsltBody")
                );

                if(expressionResp!=null) {
                    final ByteArrayOutputStream out = new ByteArrayOutputStream();
                    final ObjectMapper mapper = new ObjectMapper();
                    mapper.writeValue(out, expressionResp);
                    return ResponseUtil.createSuccessResponse(1L, mediaType, "/validation/xslt", out.toString(StandardCharsets.UTF_8), plainResponse);
                }

            }

            return ResponseUtil.createNoContentResponse(1L, mediaType);

        } catch (Exception e) {
            log.error("Error",e);
            return ResponseUtil.createFailureResponse(1L, mediaType, "/validation/xslt", e.getMessage(), plainResponse);
        }

    }

    @GetMapping(
            path = "/validation/connection/{host}/{port}/{timeout}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> testConnection(
            @PathVariable(value = "host") String host,
            @PathVariable(value = "port") int port,
            @PathVariable(value = "timeout") int timeout,
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType
    ) {

        try {
            String testConnectionResult = integration.testConnection(host, port, timeout);
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/testconnection/{host}/{port}/{timeout}",testConnectionResult);
        } catch (Exception e) {
            log.error("Test connection failed",e);
            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/testconnection/{host}/{port}/{timeout}",e.getMessage());
        }

    }

}