package org.assimbly.gateway.web.rest;

import io.swagger.annotations.ApiParam;
import org.assimbly.connector.Connector;
import org.assimbly.connectorrest.ConnectorResource;
import org.assimbly.util.CertificatesUtil;
import org.assimbly.util.rest.ResponseUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Map;


/**
 * REST controller for managing Security.
 */
@RestController
@RequestMapping("/api")
public class CertificateResource {

    private final Logger log = LoggerFactory.getLogger(CertificateResource.class);

	@Autowired
	private ConnectorResource connectorResource;

    /**
     * POST  /certificates : import a new certificates.
     *
     * @param url the url to get the certificates
     * @return the ResponseEntity<String> with status 200 (Imported) and with body (certificates), or with status 400 (Bad Request) if the certificates failed to import
     * @throws Exception
     */
    @PostMapping(path = "/certificates/import", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> importCertificates(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType, @RequestBody String url, @RequestHeader String keystoreName, @RequestHeader String keystorePassword) throws Exception {

        log.debug("REST request to import certificates for url: {}", url);

        try {

            Connector connector = connectorResource.getConnector();
            Certificate[] certificates = connector.getCertificates(url);
            Map<String,Certificate> certificateMap = connector.importCertificatesInKeystore(keystoreName, keystorePassword, certificates);

            String result = certificatesAsJSon(certificateMap, url);

            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(1, mediaType, "/certificates/import", result);

        } catch (Exception e) {
            log.error("Can't clear queues", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(1, mediaType, "/certificates/import", e.getMessage());
        }

    }


    @PostMapping(path = "/certificates/upload", consumes = {"text/plain"}, produces = {"text/plain","application/xml", "application/json"})
    public ResponseEntity<String> uploadCertificate(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType,@ApiParam(hidden = true) @RequestHeader("Content-Type") String contentType, @RequestHeader("FileType") String fileType, @RequestHeader String keystoreName, @RequestHeader String keystorePassword, @RequestBody String certificate) throws Exception {

        try {

            //get connector
            Connector connector = connectorResource.getConnector();

            Certificate cert;
            if(fileType.equalsIgnoreCase("pem")) {
                CertificatesUtil util = new CertificatesUtil();
                cert = util.convertPemToX509Certificate(certificate);
            }else if(fileType.equalsIgnoreCase("p12")){
                return ResponseUtil.createFailureResponse(1L, mediaType,"/certificates/upload","use the p12 uploader");
            }else{
                //create certificate from String
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                InputStream certificateStream = new ByteArrayInputStream(certificate.getBytes());
                cert = cf.generateCertificate(certificateStream);
            }

            Certificate[] certificates = new X509Certificate[1];
            certificates[0] = cert;

            //import certificate into truststore
            Map<String,Certificate> certificateMap = connector.importCertificatesInKeystore(keystoreName, keystorePassword, certificates);

            String result = certificatesAsJSon(certificateMap, null);

            log.debug("Uploaded certificate: " + cert);

            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(1, mediaType, "/certificates/upload", result);


        } catch (Exception e) {
            log.debug("Uploaded certificate failed: ", e.getMessage());
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(1, mediaType, "/certificates/upload", e.getMessage());
        }

    }

    @PostMapping(path = "/certificates/uploadp12", consumes = {"text/plain"}, produces = {"text/plain","application/xml", "application/json"})
    public ResponseEntity<String> uploadP12Certificate(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType,@ApiParam(hidden = true) @RequestHeader("Content-Type") String contentType, @RequestHeader("FileType") String fileType, @RequestHeader String keystoreName, @RequestHeader String keystorePassword, @RequestHeader("password") String password, @RequestBody String certificate) throws Exception {

        try {
            //get connector
            Connector connector = connectorResource.getConnector();

            Map<String,Certificate> certificateMap = connector.importP12CertificateInKeystore(keystoreName, keystorePassword, certificate, password);

            String result = certificatesAsJSon(certificateMap, null);

            log.debug("Uploaded P12 certificate: ");

            return ResponseUtil.createSuccessResponse(1L, mediaType,"/securities/uploadcertificate",result);
        } catch (Exception e) {
            log.debug("Uploaded certificate failed: ", e.getMessage());
            return ResponseUtil.createFailureResponse(1L, mediaType,"/securities/uploadcertificate",e.getMessage());
        }

    }

    /**
     * Get  /securities/:id : delete the "id" security.
     *
     * @param certificateName the name (alias) of the certificate to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @GetMapping(path = "/certificates/delete/{certificateName}", produces = {"text/plain","application/xml","application/json"})
    public ResponseEntity<String> deleteCertificate(@ApiParam(hidden = true) @RequestHeader("Accept") String mediaType,  @RequestHeader String keystoreName, @RequestHeader String keystorePassword, @PathVariable String certificateName) throws Exception {
        log.debug("REST request to delete certificate : {}", certificateName);
        Connector connector = connectorResource.getConnector();

        try {
            connector.deleteCertificateInKeystore(keystoreName, keystorePassword, certificateName);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(1, "text/plain", "/certificates/{certificateName}", "success");
        }catch (Exception e) {
            log.debug("Remove url to Whitelist failed: ", e.getMessage());
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(1, "text/plain", "/certificates/{certificateName}", e.getMessage());
        }
    }


    /**
     * Remote  /securities/:id : delete the "url" security.
     *
     * @return the ResponseEntity with status 200 (OK)
     */
    @PostMapping("/certificates/update")
    public ResponseEntity<String> updateCertificates(@RequestBody String certificates, @RequestHeader String keystoreName, @RequestHeader String keystorePassword, @RequestParam String url) throws Exception {
        log.debug("REST request to updates certificates in truststore for url ", url);
        Connector connector = connectorResource.getConnector();

        if(certificates.isEmpty()) {
            return ResponseEntity.ok().body("no certificates found");
        }

        JSONObject jsonObject = new JSONObject(certificates);

        JSONArray jsonArray = jsonObject.getJSONArray("certificate");

        Instant dateNow = Instant.now();

        for (int i = 0 ; i < jsonArray.length(); i++) {
            JSONObject certificate = jsonArray.getJSONObject(i);
            String certificateName = certificate.getString("certificateName");
            String certificateFile = certificate.getString("certificateFile");
            Instant certificateExpiry = Instant.parse(certificate.getString("certificateExpiry"));

            if(dateNow.isAfter(certificateExpiry)) {
                log.warn("Certificate '" + certificateName + "' for url " + url  + " is expired (Expiry Date: " + certificateExpiry + ")");
            }else {
                log.info("Certificate '" + certificateName + "' for url " + url + " is valid (Expiry Date: " + certificateExpiry + ")");
            }

            CertificatesUtil util = new CertificatesUtil();
            X509Certificate real = util.convertPemToX509Certificate(certificateFile);
            connector.importCertificateInKeystore(keystoreName, keystorePassword, certificateName,real);
        }

        return ResponseEntity.ok().body("truststore updated");
    }



    private String certificatesAsJSon(Map<String,Certificate> certificateMap, String certificateUrl) throws CertificateException {

        JSONObject certificatesObject  = new JSONObject();
        JSONObject certificateObject = new JSONObject();


        for (Map.Entry<String, Certificate> entry : certificateMap.entrySet()) {
            String key = entry.getKey();
            Certificate certificate = entry.getValue();
            X509Certificate real = (X509Certificate) certificate;

            String certificateName = key;
            Instant certificateExpiry = real.getNotAfter().toInstant();

            CertificatesUtil util = new CertificatesUtil();
            String certificateFile = util.convertX509CertificateToPem(real);

            JSONObject certificateDetails = new JSONObject();

            certificateDetails.put("certificateFile",certificateFile);
            certificateDetails.put("certificateName",certificateName);
            certificateDetails.put("certificateExpiry",certificateExpiry);
            certificateDetails.put("certificateUrl",certificateUrl);

            certificateObject.append("certificate", certificateDetails);
        }

        certificatesObject.put("certificates",certificateObject);

        return certificatesObject.toString();

    }

}
