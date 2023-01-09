package org.assimbly.integrationrest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Parameter;
import org.assimbly.dil.validation.HttpsCertificateValidator;
import org.assimbly.dil.validation.beans.Expression;
import org.assimbly.dil.validation.beans.FtpSettings;
import org.assimbly.dil.validation.beans.Regex;
import org.assimbly.integration.Integration;
import org.assimbly.util.error.ValidationErrorMessage;
import org.assimbly.util.rest.ResponseUtil;
import org.eclipse.jetty.util.security.CertificateValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;

/**
 * Resource to return information about the currently running Spring profiles.
 */
@ControllerAdvice
@RestController
@RequestMapping("/api")
public class ValidationResource {

	protected Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private IntegrationResource integrationResource;

    private boolean plainResponse;

    private Integration integration;

    //validations

    @GetMapping(path = "/validation/{integrationId}/cron", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> validateCron(
            @Parameter(hidden = true) @RequestHeader("Accept") String mediaType,
            @Parameter String expression,
            @PathVariable Long integrationId
    ) throws Exception {

        try {
            integration = integrationResource.getIntegration();

            ValidationErrorMessage cronResp = integration.validateCron(expression);

            if(cronResp!=null) {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                final ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(out, cronResp);
                return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType, "/validation/{integrationId}/expression", out.toString(), "", "");
            } else {
                return ResponseUtil.createNoContentResponse(integrationId, mediaType);
            }

        } catch (Exception e) {
            log.error("ErrorMessage",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType, "/validation/{integrationId}/cron", e.getMessage(), "","");
        }
    }

    @GetMapping(path = "/validation/{integrationId}/certificate", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> validateCertificate(
            @Parameter(hidden = true) @RequestHeader("Accept") String mediaType,
            @Parameter String httpsUrl,
            @PathVariable Long integrationId
    ) throws Exception {

        plainResponse = true;

        try {
            integration = integrationResource.getIntegration();

            HttpsCertificateValidator.ValidationResult certificateResp = integration.validateCertificate(httpsUrl);

            if(certificateResp!=null) {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                final ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(out, certificateResp);
                if(certificateResp.getValidationResultStatus().equals(HttpsCertificateValidator.ValidationResultStatus.VALID)) {
                    // return code 304
                    return ResponseUtil.createNotModifiedResponse(integrationId, "/validation/{integrationId}/certificate");
                } else {
                    // return code 200
                    return ResponseUtil.createSuccessResponse(integrationId, mediaType, "/validation/{integrationId}/certificate", out.toString(), plainResponse);
                }
            } else {
                return ResponseUtil.createSuccessResponse(integrationId, mediaType, "/validation/{integrationId}/certificate", "", plainResponse);
            }
        } catch (Exception e) {
            log.error("ErrorMessage",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType, "/validation/{integrationId}/certificate", e.getMessage(), "","");
        }
    }

    @GetMapping(path = "/validation/{integrationId}/url", produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> validateUrl(
            @Parameter(hidden = true) @RequestHeader("Accept") String mediaType,
            @Parameter String httpUrl,
            @PathVariable Long integrationId
    ) throws Exception {

        try {
            integration = integrationResource.getIntegration();

            ValidationErrorMessage urlResp = integration.validateUrl(httpUrl);

            if(urlResp!=null) {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                final ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(out, urlResp);
                return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType, "/validation/{integrationId}/url", out.toString(), "", "");
            } else {
                return ResponseUtil.createNoContentResponse(integrationId, mediaType);
            }
        } catch (Exception e) {
            log.error("ErrorMessage",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType, "/validation/{integrationId}/url", e.getMessage(), "","");
        }
    }


    @PostMapping(path = "/validation/{integrationId}/expression", consumes =  {"application/json"}, produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> validateExpression(
            @Parameter(hidden = true) @RequestHeader("Accept") String mediaType,
            @RequestHeader(value = "StopTest", defaultValue = "false") boolean stopTest,
            @PathVariable Long integrationId, @RequestBody String body
    ) throws Exception {

        try {
            integration = integrationResource.getIntegration();

            List<Expression> expressionsList = null;
            if(body!=null){
                expressionsList = new ObjectMapper().readValue(body, new TypeReference<List<Expression>>(){});
            }

            List<ValidationErrorMessage> expressionResp = integration.validateExpressions(expressionsList);

            if(expressionResp!=null) {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                final ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(out, expressionResp);
                return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType, "/validation/{integrationId}/expression", out.toString(), "", "");
            } else {
                return ResponseUtil.createNoContentResponse(integrationId, mediaType);
            }

        } catch (Exception e) {
            log.error("Error",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType, "/validation/{integrationId}/expression", e.getMessage(), "", "");
        }

    }

    @PostMapping(path = "/validation/{integrationId}/ftp", consumes =  {"application/json"}, produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> validateFtp(
            @Parameter(hidden = true) @RequestHeader("Accept") String mediaType,
            @RequestHeader(value = "StopTest",defaultValue = "false") boolean stopTest,
            @PathVariable Long integrationId, @RequestBody String body
    ) throws Exception {

        try {
            integration = integrationResource.getIntegration();

            FtpSettings ftpSettings = null;
            if(body!=null){
                ftpSettings = new ObjectMapper().readValue(body, FtpSettings.class);
            }

            ValidationErrorMessage ftpResp = integration.validateFtp(ftpSettings);

            if(ftpResp!=null) {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                final ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(out, ftpResp);
                return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType, "/validation/{integrationId}/ftp", out.toString(), "", "");
            } else {
                return ResponseUtil.createNoContentResponse(integrationId, mediaType);
            }

        } catch (Exception e) {
            log.error("Error",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType, "/validation/{integrationId}/ftp", e.getMessage(), "", "");
        }

    }

    @PostMapping(path = "/validation/{integrationId}/regex", consumes =  {"application/json"}, produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> validateRegex(
            @Parameter(hidden = true) @RequestHeader("Accept") String mediaType,
            @RequestHeader(value = "StopTest", defaultValue = "false") boolean stopTest,
            @PathVariable Long integrationId, @RequestBody String body
    ) throws Exception {

        try {
            integration = integrationResource.getIntegration();

            Regex regex = null;
            if(body!=null){
                regex = new ObjectMapper().readValue(body, Regex.class);
            }

            AbstractMap.SimpleEntry regexResp = integration.validateRegex(regex);

            if(regexResp!=null) {
                if (regexResp.getKey().equals("-1")) {
                    ValidationErrorMessage validationErrorMessage = new ValidationErrorMessage();
                    validationErrorMessage.setError((String)regexResp.getValue());
                    final ByteArrayOutputStream out = new ByteArrayOutputStream();
                    final ObjectMapper mapper = new ObjectMapper();
                    mapper.writeValue(out, regexResp);
                    return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType, "/validation/{integrationId}/regex", out.toString(), "", "");
                } else {
                    // success - return group count
                    return ResponseUtil.createSuccessResponse(integrationId, mediaType, "/validation/{integrationId}/regex", (String)regexResp.getValue());
                }
            } else {
                return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType, "/validation/{integrationId}/regex", "", "", "");
            }

        } catch (Exception e) {
            log.error("Error",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType, "/validation/{integrationId}/regex", e.getMessage(), "", "");
        }

    }

    @PostMapping(path = "/validation/{integrationId}/script", consumes =  {"application/json"}, produces = {"application/xml","application/json","text/plain"})
    public ResponseEntity<String> validateScript(@Parameter(hidden = true) @RequestHeader("Accept") String mediaType, @RequestHeader(value = "StopTest", defaultValue = "false") boolean stopTest, @PathVariable Long integrationId, @RequestBody String body) throws Exception {

        try {

            integration = integrationResource.getIntegration();

            return ResponseUtil.createSuccessResponseWithHeaders(integrationId, mediaType, "/validation/{integrationId}/script", "", "", "");
        } catch (Exception e) {
            log.error("Error",e);
            return ResponseUtil.createFailureResponseWithHeaders(integrationId, mediaType, "/validation/{integrationId}/script", e.getMessage(), "", "");
        }

    }

}