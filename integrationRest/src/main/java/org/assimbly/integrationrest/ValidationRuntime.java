package org.assimbly.integrationrest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Parameter;
import org.assimbly.dil.validation.HttpsCertificateValidator;
import org.assimbly.dil.validation.beans.Expression;
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
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private IntegrationRuntime integrationRuntime;

    private boolean plainResponse;

    private Integration integration;

    //validations

    @GetMapping(
            path = "/validation/{integrationId}/cron",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> validateCron(
            @Parameter(hidden = true) @RequestHeader("Accept") String mediaType,
            @Parameter String expression,
            @PathVariable Long integrationId
    ) throws Exception {

        plainResponse = true;

        try {
            integration = integrationRuntime.getIntegration();

            ValidationErrorMessage cronResp = integration.validateCron(expression);

            if(cronResp!=null) {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                final ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(out, cronResp);
                return ResponseUtil.createSuccessResponse(integrationId, mediaType, "/validation/{integrationId}/cron", out.toString(StandardCharsets.UTF_8), plainResponse);
            } else {
                return ResponseUtil.createNoContentResponse(integrationId, mediaType);
            }

        } catch (Exception e) {
            log.error("ErrorMessage",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType, "/validation/{integrationId}/cron", e.getMessage(), plainResponse);
        }
    }

    @GetMapping(
            path = "/validation/{integrationId}/certificate",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> validateCertificate(
            @Parameter(hidden = true) @RequestHeader("Accept") String mediaType,
            @Parameter String httpsUrl,
            @PathVariable Long integrationId
    ) throws Exception {

        plainResponse = true;

        try {
            integration = integrationRuntime.getIntegration();

            HttpsCertificateValidator.ValidationResult certificateResp = integration.validateCertificate(httpsUrl);

            if(certificateResp!=null) {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                final ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(out, certificateResp);
                return ResponseUtil.createSuccessResponse(integrationId, mediaType, "/validation/{integrationId}/certificate", out.toString(StandardCharsets.UTF_8), plainResponse);
            } else {
                return ResponseUtil.createSuccessResponse(integrationId, mediaType, "/validation/{integrationId}/certificate", "", plainResponse);
            }
        } catch (Exception e) {
            log.error("ErrorMessage",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType, "/validation/{integrationId}/certificate", e.getMessage(), plainResponse);
        }
    }

    @GetMapping(
            path = "/validation/{integrationId}/url",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> validateUrl(
            @Parameter(hidden = true) @RequestHeader("Accept") String mediaType,
            @Parameter String httpUrl,
            @PathVariable Long integrationId
    ) throws Exception {

        plainResponse = true;

        try {
            integration = integrationRuntime.getIntegration();

            ValidationErrorMessage urlResp = integration.validateUrl(httpUrl);

            if(urlResp!=null) {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                final ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(out, urlResp);
                return ResponseUtil.createSuccessResponse(integrationId, mediaType, "/validation/{integrationId}/url", out.toString(StandardCharsets.UTF_8), plainResponse);
            } else {
                return ResponseUtil.createNoContentResponse(integrationId, mediaType);
            }
        } catch (Exception e) {
            log.error("ErrorMessage",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType, "/validation/{integrationId}/url", e.getMessage(), plainResponse);
        }
    }


    @PostMapping(
            path = "/validation/{integrationId}/expression",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> validateExpression(
            @Parameter(hidden = true) @RequestHeader("Accept") String mediaType,
            @RequestHeader(value = "StopTest", defaultValue = "false") boolean stopTest,
            @RequestHeader(value = "IsPredicate", defaultValue = "false") boolean isPredicate,
            @PathVariable Long integrationId, @RequestBody String body
    ) throws Exception {

        plainResponse = true;

        try {
            integration = integrationRuntime.getIntegration();

            List<Expression> expressionsList = null;
            if(body!=null){
                expressionsList = new ObjectMapper().readValue(body, new TypeReference<List<Expression>>(){});
            }

            List<ValidationErrorMessage> expressionResp = integration.validateExpressions(expressionsList, isPredicate);

            if(expressionResp!=null) {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                final ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(out, expressionResp);
                return ResponseUtil.createSuccessResponse(integrationId, mediaType, "/validation/{integrationId}/expression", out.toString(StandardCharsets.UTF_8), plainResponse);
            } else {
                return ResponseUtil.createNoContentResponse(integrationId, mediaType);
            }

        } catch (Exception e) {
            log.error("Error",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType, "/validation/{integrationId}/expression", e.getMessage(), plainResponse);
        }

    }

    @PostMapping(
            path = "/validation/{integrationId}/ftp",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> validateFtp(
            @Parameter(hidden = true) @RequestHeader("Accept") String mediaType,
            @RequestHeader(value = "StopTest",defaultValue = "false") boolean stopTest,
            @PathVariable Long integrationId, @RequestBody String body
    ) throws Exception {

        plainResponse = true;

        try {
            integration = integrationRuntime.getIntegration();

            FtpSettings ftpSettings = null;
            if(body!=null){
                ftpSettings = new ObjectMapper().readValue(body, FtpSettings.class);
            }

            ValidationErrorMessage ftpResp = integration.validateFtp(ftpSettings);

            if(ftpResp!=null) {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                final ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(out, ftpResp);
                return ResponseUtil.createSuccessResponse(integrationId, mediaType, "/validation/{integrationId}/ftp", out.toString(StandardCharsets.UTF_8), plainResponse);
            } else {
                return ResponseUtil.createNoContentResponse(integrationId, mediaType);
            }

        } catch (Exception e) {
            log.error("Error",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType, "/validation/{integrationId}/ftp", e.getMessage(), plainResponse);
        }

    }

    @PostMapping(
            path = "/validation/{integrationId}/regex",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> validateRegex(
            @Parameter(hidden = true) @RequestHeader("Accept") String mediaType,
            @RequestHeader(value = "StopTest", defaultValue = "false") boolean stopTest,
            @PathVariable Long integrationId, @RequestBody String body
    ) throws Exception {

        plainResponse = true;

        try {
            integration = integrationRuntime.getIntegration();

            Regex regex = null;
            if(body!=null){
                regex = new ObjectMapper().readValue(body, Regex.class);
            }

            AbstractMap.SimpleEntry regexResp = integration.validateRegex(regex);

            if(regexResp!=null) {
                if ((Integer)regexResp.getKey() == -1) {
                    return ResponseUtil.createFailureResponse(integrationId, mediaType, "/validation/{integrationId}/regex", (String)regexResp.getValue(), plainResponse);
                } else {
                    // success - return group count
                    return ResponseUtil.createSuccessResponse(integrationId, mediaType, "/validation/{integrationId}/regex", (String)regexResp.getValue());
                }
            } else {
                return ResponseUtil.createFailureResponse(integrationId, mediaType, "/validation/{integrationId}/regex", "", plainResponse);
            }

        } catch (Exception e) {
            log.error("Error",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType, "/validation/{integrationId}/regex", e.getMessage(), plainResponse);
        }

    }

    @PostMapping(
            path = "/validation/{integrationId}/script",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> validateScript(
            @Parameter(hidden = true) @RequestHeader("Accept") String mediaType,
            @RequestHeader(value = "StopTest", defaultValue = "false") boolean stopTest,
            @PathVariable Long integrationId,
            @RequestBody String body
    ) throws Exception {

        plainResponse = true;

        try {
            integration = integrationRuntime.getIntegration();

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
                    return ResponseUtil.createSuccessResponse(integrationId, mediaType, "/validation/{integrationId}/script", out.toString(StandardCharsets.UTF_8), plainResponse);
                } else {
                    mapper.writeValue(out, new BadRequestResponse(scriptResp.getResult()));
                    return ResponseUtil.createFailureResponse(integrationId, mediaType, "/validation/{integrationId}/script", out.toString(StandardCharsets.UTF_8), plainResponse);
                }
            } else {
                return ResponseUtil.createFailureResponse(integrationId, mediaType, "/validation/{integrationId}/script", "", plainResponse);
            }

        } catch (Exception e) {
            log.error("Error",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType, "/validation/{integrationId}/script", e.getMessage(), plainResponse);
        }

    }

    @GetMapping(
            path = "/validation/{integrationId}/uri",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> validateUri(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @RequestHeader("Uri") String uri, @PathVariable Long integrationId) throws Exception {
        try {
            integration = integrationRuntime.getIntegration();
            String flowValidation = integration.validateFlow(uri);
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/validation/{integrationId}/uri",flowValidation);
        } catch (Exception e) {
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/validation/{integrationId}/urizx",e.getMessage());
        }
    }

    @PostMapping(path = "/validation/{integrationId}/xslt",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> validateXslt(
            @Parameter(hidden = true) @RequestHeader("Accept") String mediaType,
            @RequestHeader(value = "StopTest", defaultValue = "false") boolean stopTest,
            @PathVariable Long integrationId,
            @RequestBody String body
    ) throws Exception {

        try {

            plainResponse = true;

            HashMap<String,String> paramList = null;

            if(body!=null){
                paramList = new ObjectMapper().readValue(body, new TypeReference<HashMap<String,String>>(){});

                integration = integrationRuntime.getIntegration();
                List<ValidationErrorMessage> expressionResp = integration.validateXslt(
                        paramList.get("xsltUrl"),
                        paramList.get("xsltBody")
                );

                if(expressionResp!=null) {
                    final ByteArrayOutputStream out = new ByteArrayOutputStream();
                    final ObjectMapper mapper = new ObjectMapper();
                    mapper.writeValue(out, expressionResp);
                    return ResponseUtil.createSuccessResponse(integrationId, mediaType, "/validation/{integrationId}/xslt", out.toString(StandardCharsets.UTF_8), plainResponse);
                }

            }

            return ResponseUtil.createNoContentResponse(integrationId, mediaType);

        } catch (Exception e) {
            log.error("Error",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType, "/validation/{integrationId}/xslt", e.getMessage(), plainResponse);
        }

    }

    @GetMapping(
            path = "/validation/{integrationId}/connection/{host}/{port}/{timeout}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> testConnection(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @PathVariable Long integrationId, @PathVariable String host,@PathVariable int port, @PathVariable int timeout) throws Exception {

        try {
            String testConnectionResult = integration.testConnection(host, port, timeout);
            return ResponseUtil.createSuccessResponse(integrationId, mediaType,"/integration/{integrationId}/testconnection/{host}/{port}/{timeout}",testConnectionResult);
        } catch (Exception e) {
            log.error("Test connection failed",e);
            return ResponseUtil.createFailureResponse(integrationId, mediaType,"/integration/{integrationId}/testconnection/{host}/{port}/{timeout}",e.getMessage());
        }

    }

}