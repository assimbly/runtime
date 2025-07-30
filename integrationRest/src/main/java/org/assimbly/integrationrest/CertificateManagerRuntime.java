package org.assimbly.integrationrest;

import io.swagger.v3.oas.annotations.Parameter;
import org.assimbly.integration.Integration;
import org.assimbly.util.BaseDirectory;
import org.assimbly.util.CertificatesUtil;
import org.assimbly.util.rest.ResponseUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;


/**
 * REST controller for managing Security.
 */
@RestController
@RequestMapping("/api")
public class CertificateManagerRuntime {

    private final Logger log = LoggerFactory.getLogger(CertificateManagerRuntime.class);

    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();

    private final Integration integration;

    public CertificateManagerRuntime(IntegrationRuntime integrationRuntime) {
        this.integration = integrationRuntime.getIntegration();
    }

    /**
     * POST  /certificates/ : Sets TLS certificates.
     *
     * @return the ResponseEntity with status 200 (Successful) and status 400 (Bad Request) if the configuration failed
     */
    @PostMapping(
            path = "/certificates/set",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> setCertificates(
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestBody String url,
            @RequestHeader(value = "keystoreName") String keystoreName,
            @RequestHeader(value = "keystorePassword") String keystorePassword
    ) {

        log.debug("REST request to set certificates for url: {}", url);

        try {
            integration.setCertificatesInKeystore(keystoreName, keystorePassword, url);
            return ResponseUtil.createSuccessResponse(1L, mediaType,"/integration/setcertificates/{id}","Certificates set");
        } catch (Exception e) {
            log.error("Set certificates for keystore={} for url={} failed", keystoreName, url, e);

            return ResponseUtil.createFailureResponse(1L, mediaType,"/integration/setcertificates/{id}",e.getMessage());
        }

    }

    /**
     * POST  /certificates : import a new certificates.
     *
     * @param url the url to get the certificates
     * @return the ResponseEntity<String> with status 200 (Imported) and with body (certificates), or with status 400 (Bad Request) if the certificates failed to import
     */
    @PostMapping(
            path = "/certificates/import",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> importCertificates(
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestBody String url,
            @RequestHeader(value = "keystoreName") String keystoreName,
            @RequestHeader(value = "keystorePassword") String keystorePassword
    ) {

        log.debug("REST request to import certificates for url: {}", url);

        try {

            Certificate[] certificates = getCertificates(url);

            if(certificates == null || certificates.length == 0){
                throw new Exception("Certificates couldn't be downloaded.");
            }

            Map<String,Certificate> certificateMap = importCertificatesInKeystore(keystoreName, keystorePassword, certificates);

            String result = certificatesAsJSon(certificateMap, url, keystoreName);

            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(1, mediaType, "/certificates/import", result);

        } catch (Exception e) {
            log.error("Can't import certificates into keystore.", e);
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(1, mediaType, "/certificates/import", e.getMessage());
        }

    }


    @PostMapping(
            path = "/certificates/upload",
            consumes = {MediaType.TEXT_PLAIN_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> uploadCertificate(
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @Parameter(hidden = true) @RequestHeader(value = "Content-Type") String contentType,
            @RequestBody String certificate,
            @RequestHeader(value = "FileType") String fileType,
            @RequestHeader(value = "keystoreName") String keystoreName,
            @RequestHeader(value = "keystorePassword") String keystorePassword
    ) {

        try {

            Certificate cert;
            if(fileType.equalsIgnoreCase("pem")) {
                cert = CertificatesUtil.convertPemToX509Certificate(certificate);
            }else if(fileType.equalsIgnoreCase("p12")){
                return ResponseUtil.createFailureResponse(1L, mediaType,"/certificates/upload","use the p12 uploader");
            }else{
                //create certificate from String
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                InputStream certificateStream = new ByteArrayInputStream(certificate.getBytes(StandardCharsets.UTF_8));
                cert = cf.generateCertificate(certificateStream);
            }

            Certificate[] certificates = new X509Certificate[1];
            certificates[0] = cert;

            Map<String,Certificate> certificateMap = importCertificatesInKeystore(keystoreName, keystorePassword, certificates);

            String result = certificatesAsJSon(certificateMap, null, keystoreName);

            log.debug("Uploaded certificate: {}", cert);

            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(1, mediaType, "/certificates/upload", result);


        } catch (Exception e) {
            log.debug("Uploaded certificate failed: {}", e.getMessage());
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(1, mediaType, "/certificates/upload", e.getMessage());
        }

    }

    @PostMapping(
            path = "/certificates/uploadp12",
            consumes = {MediaType.TEXT_PLAIN_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> uploadP12Certificate(
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @Parameter(hidden = true) @RequestHeader(value = "Content-Type") String contentType,
            @RequestBody String certificate,
            @RequestHeader(value = "FileType") String fileType,
            @RequestHeader(value = "keystoreName") String keystoreName,
            @RequestHeader(value = "keystorePassword") String keystorePassword,
            @RequestHeader(value = "password") String password
    ) {

        try {

            Map<String,Certificate> certificateMap = importP12CertificateInKeystore(keystoreName, keystorePassword, certificate, password);

            String result = certificatesAsJSon(certificateMap, "P12", keystoreName);

            log.debug("Uploaded P12 certificate");

            return ResponseUtil.createSuccessResponse(1L, mediaType,"/securities/uploadcertificate",result);
        } catch (Exception e) {
            log.debug("Uploaded certificate failed: {}", e.getMessage());
            return ResponseUtil.createFailureResponse(1L, mediaType,"/securities/uploadcertificate",e.getMessage());
        }

    }

    @GetMapping(
            path = "/certificates/generate",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> generateCertificate(
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestHeader(value = "cn") String cn,
            @RequestHeader(value = "keystoreName") String keystoreName,
            @RequestHeader(value = "keystorePassword") String keystorePassword
    ) {

        try {

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(4096, new SecureRandom());
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            Certificate cert = CertificatesUtil.selfsignCertificate2(keyPair, cn);

            importCertificateInKeystore(keystoreName, keystorePassword, cn, cert);

            Map<String,Certificate> certificateMap = new HashMap<>();
            certificateMap.put("Self-Signed (" + cn + ")",cert);

            String result = certificatesAsJSon(certificateMap, null, keystoreName);

            log.debug("Generated certificate: {}", cert);

            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(1, mediaType, "/certificates/generate", result);


        } catch (Exception e) {
            log.debug("Generate self-signed certificate failed: {}", e.getMessage());
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(1, mediaType, "/certificates/generate", e.getMessage());
        }

    }



    /**
     * Get  /securities/:id : delete the "id" security.
     *
     * @param certificateName the name (alias) of the certificate to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @GetMapping(
            path = "/certificates/delete/{certificateName}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> deleteCertificate(
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @PathVariable(value = "certificateName") String certificateName,
            @RequestHeader(value = "keystoreName") String keystoreName,
            @RequestHeader(value = "keystorePassword") String keystorePassword
    ) {
        log.debug("REST request to delete certificate : {}", certificateName);

        try {
            deleteCertificateInKeystore(keystoreName, keystorePassword);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(1, "text/plain", "/certificates/{certificateName}", "success");
        }catch (Exception e) {
            log.debug("Remove url to Whitelist failed: {}", e.getMessage());
            return org.assimbly.util.rest.ResponseUtil.createFailureResponse(1, "text/plain", "/certificates/{certificateName}", e.getMessage());
        }
    }


    /**
     * Remote  /securities/:id : delete the "url" security.
     *
     * @return the ResponseEntity with status 200 (OK)
     */
    @PostMapping("/certificates/update")
    public ResponseEntity<String> updateCertificates(
            @RequestBody String certificates,
            @RequestHeader(value = "keystoreName") String keystoreName,
            @RequestHeader(value = "keystorePassword") String keystorePassword,
            @RequestParam(value = "url") String url
    ) throws Exception {
        log.debug("REST request to updates certificates in truststore for url {}", url);

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
                log.warn("Certificate '{}' for url {} is expired (Expiry Date: {})", certificateName, url, certificateExpiry);
            }else {
                log.info("Certificate '{}' for url {} is valid (Expiry Date: {})", certificateName, url, certificateExpiry);
            }

            X509Certificate real = CertificatesUtil.convertPemToX509Certificate(certificateFile);
            importCertificateInKeystore(keystoreName, keystorePassword, certificateName,real);
        }

        return ResponseEntity.ok().body("truststore updated");
    }



    private String certificatesAsJSon(Map<String,Certificate> certificateMap, String certificateUrl, String certificateStore) throws CertificateException {

        JSONObject certificatesObject  = new JSONObject();
        JSONObject certificateObject = new JSONObject();


        for (Map.Entry<String, Certificate> entry : certificateMap.entrySet()) {
            String key = entry.getKey();
            Certificate certificate = entry.getValue();
            X509Certificate real = (X509Certificate) certificate;

            Instant certificateExpiry = real.getNotAfter().toInstant();

            String certificateFile = CertificatesUtil.convertX509CertificateToPem(real);

            JSONObject certificateDetails = new JSONObject();

            certificateDetails.put("certificateFile",certificateFile);
            certificateDetails.put("certificateName", key);
            certificateDetails.put("certificateStore",certificateStore);

            certificateDetails.put("certificateExpiry",certificateExpiry);
            certificateDetails.put("certificateUrl",certificateUrl);

            certificateObject.append("certificate", certificateDetails);
        }

        certificatesObject.put("certificates",certificateObject);

        return certificatesObject.toString();

    }

    public Certificate[] getCertificates(String url) {
        try {
            CertificatesUtil util = new CertificatesUtil();
            return util.downloadCertificates(url);
        } catch (Exception e) {
            log.error("Download of certificate from url {} failed", url, e);
        }
        return new Certificate[0];
    }

    public Certificate getCertificateFromKeystore(String keystoreName, String keystorePassword, String certificateName) {
        String keystorePath = baseDir + "/security/" + keystoreName;
        CertificatesUtil util = new CertificatesUtil();
        return util.getCertificate(keystorePath, keystorePassword, certificateName);
    }

    public void setCertificatesInKeystore(String keystoreName, String keystorePassword, String url) {

        try {
            CertificatesUtil util = new CertificatesUtil();
            Certificate[] certificates = util.downloadCertificates(url);
            String keystorePath = baseDir + "/security/" + keystoreName;
            util.importCertificates(keystorePath, keystorePassword, certificates);
        } catch (Exception e) {
            log.error("Set Certificate in keystore {} for url {} failed", keystoreName, url, e);
        }
    }

    public void importCertificateInKeystore(String keystoreName, String keystorePassword, String certificateName, Certificate certificate) {

        CertificatesUtil util = new CertificatesUtil();

        String keystorePath = baseDir + "/security/" + keystoreName;

        util.importCertificate(keystorePath, keystorePassword, certificateName, certificate);

    }


    public Map<String,Certificate> importCertificatesInKeystore(String keystoreName, String keystorePassword, Certificate[] certificates) {

        CertificatesUtil util = new CertificatesUtil();

        String keystorePath = baseDir + "/security/" + keystoreName;

        return util.importCertificates(keystorePath, keystorePassword, certificates);

    }

    public Map<String,Certificate> importP12CertificateInKeystore(String keystoreName, String keystorePassword, String p12Certificate, String p12Password) throws Exception {

        CertificatesUtil util = new CertificatesUtil();

        String keystorePath = baseDir + "/security/" + keystoreName;
        return util.importP12Certificate(keystorePath, keystorePassword, p12Certificate, p12Password);

    }

    public void deleteCertificateInKeystore(String keystoreName, String keystorePassword) {

        String keystorePath = baseDir + "/security/" + keystoreName;

        CertificatesUtil util = new CertificatesUtil();
        util.deleteCertificate(keystorePath, keystorePassword);
    }

}
