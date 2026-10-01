package org.assimbly.integrationrest;

import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.assimbly.integration.Integration;
import org.assimbly.util.BaseDirectory;
import org.assimbly.util.CertificateExpiry;
import org.assimbly.util.CertificatesUtil;
import org.assimbly.util.rest.ResponseUtil;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


/**
 * REST controller for managing the certificates in the runtime's keystores.
 * <p>
 * /certificates manages the trusted certificates (default keystore outbound-truststore.p12) and
 * /certificates/identity the server identities (default keystore server-identity.p12). The keystoreName and
 * keystorePassword headers are optional; the password defaults to the runtime's keystore password.
 */
@Tag(name = "Certificates", description = "Manage certificates in keystores")
@RestController
@RequestMapping("/api")
public class CertificateManagerRuntime {

    private final Logger log = LoggerFactory.getLogger(CertificateManagerRuntime.class);

    private static final String TRUSTSTORE = CertificatesUtil.TRUSTSTORE_FILE;
    private static final String IDENTITY_STORE = CertificatesUtil.IDENTITY_STORE_FILE;

    // names that are also literal paths next to /certificates/{certificateName} and /certificates/identity/{certificateName}
    private static final Set<String> RESERVED_CERTIFICATE_NAMES = Set.of("identity", "expired", "expiring");
    private static final Set<String> RESERVED_IDENTITY_NAMES = Set.of("generate");

    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();

    private final Integration integration;

    public CertificateManagerRuntime(IntegrationRuntime integrationRuntime) {
        this.integration = integrationRuntime.getIntegration();
    }

    // --- trusted certificates -------------------------------------------------------------------------------------

    @Operation(summary = "Get all certificates in the keystore with their expiry")
    @GetMapping(path = "/certificates", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> listCertificates(
            @RequestHeader(value = "keystoreName", required = false, defaultValue = TRUSTSTORE) String keystoreName,
            @RequestHeader(value = "keystorePassword", required = false) String keystorePassword
    ) {
        return list(keystoreName, keystorePassword, "/certificates");
    }

    @Operation(summary = "Get a certificate in the keystore with its expiry")
    @GetMapping(path = "/certificates/{certificateName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getCertificate(
            @PathVariable(value = "certificateName") String certificateName,
            @RequestHeader(value = "keystoreName", required = false, defaultValue = TRUSTSTORE) String keystoreName,
            @RequestHeader(value = "keystorePassword", required = false) String keystorePassword
    ) {
        return get(certificateName, keystoreName, keystorePassword, "/certificates/{certificateName}");
    }

    @Operation(
            summary = "Import trusted (CA) certificates into the truststore",
            description = "The body is PEM, or base64 encoded PEM or DER, optionally as a data URL. The certificate is stored under certificateName; "
                    + "the other certificates of a PEM bundle under certificateName-2, -3, ... "
                    + "Returns 409 when a name already holds a different certificate (use PUT to replace it)."
    )
    @PostMapping(
            path = "/certificates/{certificateName}",
            consumes = {MediaType.TEXT_PLAIN_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> importTrustedCertificates(
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @PathVariable(value = "certificateName") String certificateName,
            @RequestBody String certificates,
            @RequestHeader(value = "keystoreName", required = false, defaultValue = TRUSTSTORE) String keystoreName,
            @RequestHeader(value = "keystorePassword", required = false) String keystorePassword
    ) {

        log.debug("REST request to import trusted certificate {} into keystore {}", certificateName, keystoreName);

        try {
            CertificatesUtil.validateAlias(certificateName, RESERVED_CERTIFICATE_NAMES);
            List<X509Certificate> parsed = CertificatesUtil.parseCertificates(certificates);
            warnIfExpired(parsed);

            Map<String,Certificate> certificateMap = CertificatesUtil.importTrustedCertificates(keystorePath(keystoreName, true), password(keystorePassword), parsed, certificateName);

            log.info("Imported trusted certificates {} into keystore {}", certificateMap.keySet(), keystoreName);

            return ResponseUtil.createSuccessResponse(1L, mediaType, "/certificates/{certificateName}", certificatesAsJSon(certificateMap, null, keystoreName));
        } catch (Exception e) {
            log.error("Import trusted certificate {} into keystore {} failed", certificateName, keystoreName, e);
            return failure(mediaType, "/certificates/{certificateName}", e);
        }

    }

    @Operation(
            summary = "Create or replace a trusted (CA) certificate in the truststore",
            description = "The body is one certificate: PEM, or base64 encoded PEM or DER, optionally as a data URL. "
                    + "Returns 409 when certificateName is an identity (private key entry)."
    )
    @PutMapping(
            path = "/certificates/{certificateName}",
            consumes = {MediaType.TEXT_PLAIN_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> replaceTrustedCertificate(
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @PathVariable(value = "certificateName") String certificateName,
            @RequestBody String certificate,
            @RequestHeader(value = "keystoreName", required = false, defaultValue = TRUSTSTORE) String keystoreName,
            @RequestHeader(value = "keystorePassword", required = false) String keystorePassword
    ) {

        log.debug("REST request to replace trusted certificate {} in keystore {}", certificateName, keystoreName);

        try {
            CertificatesUtil.validateAlias(certificateName, RESERVED_CERTIFICATE_NAMES);
            List<X509Certificate> parsed = CertificatesUtil.parseCertificates(certificate);
            if (parsed.size() != 1) {
                throw new IllegalArgumentException("Replacing certificate '" + certificateName + "' needs exactly one certificate, the body has " + parsed.size());
            }
            warnIfExpired(parsed);

            Map<String,Certificate> certificateMap = CertificatesUtil.putTrustedCertificate(keystorePath(keystoreName, true), password(keystorePassword), certificateName, parsed.getFirst());

            log.info("Stored trusted certificate {} in keystore {}", certificateName, keystoreName);

            return ResponseUtil.createSuccessResponse(1L, mediaType, "/certificates/{certificateName}", certificatesAsJSon(certificateMap, null, keystoreName));
        } catch (Exception e) {
            log.error("Replace trusted certificate {} in keystore {} failed", certificateName, keystoreName, e);
            return failure(mediaType, "/certificates/{certificateName}", e);
        }

    }

    @Operation(summary = "Delete a certificate from the keystore")
    @DeleteMapping(
            path = "/certificates/{certificateName}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> deleteCertificate(
            @PathVariable(value = "certificateName") String certificateName,
            @RequestHeader(value = "keystoreName", required = false, defaultValue = TRUSTSTORE) String keystoreName,
            @RequestHeader(value = "keystorePassword", required = false) String keystorePassword
    ) {
        return delete(certificateName, keystoreName, keystorePassword, "/certificates/{certificateName}");
    }

    @Operation(summary = "Get the certificates in the keystore that expire within a number of days")
    @GetMapping(path = "/certificates/expiring/{numberOfDays}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getExpiringCertificates(
            @PathVariable(value = "numberOfDays") int numberOfDays,
            @RequestHeader(value = "keystoreName", required = false, defaultValue = TRUSTSTORE) String keystoreName,
            @RequestHeader(value = "keystorePassword", required = false) String keystorePassword
    ) {
        log.debug("REST request to get the certificates in keystore {} that expire within {} days", keystoreName, numberOfDays);

        if (numberOfDays < 0) {
            return ResponseUtil.createFailureResponse(1L, MediaType.APPLICATION_JSON_VALUE, "/certificates/expiring/{numberOfDays}", "numberOfDays can't be negative");
        }

        try {
            // daysUntilExpiry is rounded down, so "< numberOfDays" means it expires within numberOfDays days from now
            List<CertificateExpiry> expiring = certificatesExpiry(keystoreName, password(keystorePassword)).stream()
                    .filter(expiry -> expiry.valid() && expiry.daysUntilExpiry() < numberOfDays)
                    .toList();
            return ResponseEntity.ok(expiring);
        } catch (Exception e) {
            log.error("Get the expiring certificates in keystore {} failed", keystoreName, e);
            return ResponseUtil.createFailureResponse(1L, MediaType.APPLICATION_JSON_VALUE, "/certificates/expiring/{numberOfDays}", e.getMessage());
        }
    }

    @Operation(summary = "Get the expired certificates in the keystore")
    @GetMapping(path = "/certificates/expired", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getExpiredCertificates(
            @RequestHeader(value = "keystoreName", required = false, defaultValue = TRUSTSTORE) String keystoreName,
            @RequestHeader(value = "keystorePassword", required = false) String keystorePassword
    ) {
        log.debug("REST request to get the expired certificates in keystore {}", keystoreName);

        try {
            Instant now = Instant.now();
            Map<String, X509Certificate> expired = new TreeMap<>(CertificatesUtil.getCertificates(keystorePath(keystoreName), password(keystorePassword)));
            expired.values().removeIf(certificate -> !certificate.getNotAfter().toInstant().isBefore(now));
            return ResponseEntity.ok(expiredCertificates(expired, now));
        } catch (Exception e) {
            log.error("Get the expired certificates in keystore {} failed", keystoreName, e);
            return ResponseUtil.createFailureResponse(1L, MediaType.APPLICATION_JSON_VALUE, "/certificates/expired", e.getMessage());
        }
    }

    @Operation(summary = "Delete the expired certificates from the keystore", description = "Returns the deleted certificates. Expired key entries (identities with a private key) are kept.")
    @DeleteMapping(path = "/certificates/expired", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteExpiredCertificates(
            @RequestHeader(value = "keystoreName", required = false, defaultValue = TRUSTSTORE) String keystoreName,
            @RequestHeader(value = "keystorePassword", required = false) String keystorePassword
    ) {
        log.debug("REST request to delete the expired certificates in keystore {}", keystoreName);

        try {
            Instant now = Instant.now();
            Map<String, X509Certificate> deleted = CertificatesUtil.deleteExpiredCertificates(keystorePath(keystoreName), password(keystorePassword), now);
            log.info("Deleted {} expired certificates from keystore {}: {}", deleted.size(), keystoreName, deleted.keySet());
            return ResponseEntity.ok(expiredCertificates(deleted, now));
        } catch (Exception e) {
            log.error("Delete the expired certificates in keystore {} failed", keystoreName, e);
            return ResponseUtil.createFailureResponse(1L, MediaType.APPLICATION_JSON_VALUE, "/certificates/expired", e.getMessage());
        }
    }

    // --- downloaded certificates of a domain ----------------------------------------------------------------------

    @Operation(
            summary = "Get the downloaded certificates of a domain with their expiry",
            description = "The certificates stored for the domain as <domain>-root-ca, <domain>-intermediate-ca(-n) and <domain>-leaf."
    )
    @GetMapping(path = "/certificates/domain/{domain}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getDomainCertificates(
            @PathVariable(value = "domain") String domain,
            @RequestHeader(value = "keystoreName", required = false, defaultValue = TRUSTSTORE) String keystoreName,
            @RequestHeader(value = "keystorePassword", required = false) String keystorePassword
    ) {
        log.debug("REST request to get the certificates of domain {} in keystore {}", domain, keystoreName);

        try {
            String prefix = CertificatesUtil.certificateAliasPrefix(CertificatesUtil.domainUrl(domain));
            Map<String, X509Certificate> certificates = CertificatesUtil.getDomainCertificates(keystorePath(keystoreName), password(keystorePassword), prefix);
            return ResponseEntity.ok(certificatesExpiry(certificates));
        } catch (Exception e) {
            log.error("Get the certificates of domain {} in keystore {} failed", domain, keystoreName, e);
            return ResponseUtil.createFailureResponse(1L, MediaType.APPLICATION_JSON_VALUE, "/certificates/domain/{domain}", e.getMessage());
        }
    }

    @Operation(
            summary = "Download the certificates of a domain into the keystore",
            description = "Downloads the TLS certificate chain of https://<domain>/ and stores the selected certificates as <domain>-root-ca, "
                    + "<domain>-intermediate-ca(-n) and <domain>-leaf. Returns 409 when the domain already has certificates (use PUT to renew them)."
    )
    @PostMapping(
            path = "/certificates/domain/{domain}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> addDomainCertificates(
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @PathVariable(value = "domain") String domain,
            @Parameter(description = "Which certificates of the chain to store: root, intermediate, leaf or all")
            @RequestHeader(value = "certificateType", required = false, defaultValue = CertificatesUtil.CERTIFICATE_TYPE_ROOT) String certificateType,
            @RequestHeader(value = "keystoreName", required = false, defaultValue = TRUSTSTORE) String keystoreName,
            @RequestHeader(value = "keystorePassword", required = false) String keystorePassword
    ) {
        return downloadForDomain(mediaType, domain, certificateType, keystoreName, keystorePassword, false);
    }

    @Operation(
            summary = "Renew the certificates of a domain in the keystore",
            description = "Downloads the TLS certificate chain of https://<domain>/ and, only when that succeeds, replaces all certificates of the domain "
                    + "with the selected ones in one keystore write."
    )
    @PutMapping(
            path = "/certificates/domain/{domain}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> renewDomainCertificates(
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @PathVariable(value = "domain") String domain,
            @Parameter(description = "Which certificates of the chain to store: root, intermediate, leaf or all")
            @RequestHeader(value = "certificateType", required = false, defaultValue = CertificatesUtil.CERTIFICATE_TYPE_ROOT) String certificateType,
            @RequestHeader(value = "keystoreName", required = false, defaultValue = TRUSTSTORE) String keystoreName,
            @RequestHeader(value = "keystorePassword", required = false) String keystorePassword
    ) {
        return downloadForDomain(mediaType, domain, certificateType, keystoreName, keystorePassword, true);
    }

    @Operation(summary = "Delete the downloaded certificates of a domain from the keystore", description = "Returns the deleted certificates.")
    @DeleteMapping(path = "/certificates/domain/{domain}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteDomainCertificates(
            @PathVariable(value = "domain") String domain,
            @RequestHeader(value = "keystoreName", required = false, defaultValue = TRUSTSTORE) String keystoreName,
            @RequestHeader(value = "keystorePassword", required = false) String keystorePassword
    ) {
        log.debug("REST request to delete the certificates of domain {} from keystore {}", domain, keystoreName);

        try {
            String prefix = CertificatesUtil.certificateAliasPrefix(CertificatesUtil.domainUrl(domain));
            String keystorePath = keystorePath(keystoreName, true);
            Map<String, X509Certificate> deleted = new File(keystorePath).isFile()
                    ? CertificatesUtil.deleteDomainCertificates(keystorePath, password(keystorePassword), prefix)
                    : Map.of();
            log.info("Deleted the certificates {} of domain {} from keystore {}", deleted.keySet(), domain, keystoreName);
            return ResponseEntity.ok(certificatesExpiry(deleted));
        } catch (Exception e) {
            log.error("Delete the certificates of domain {} from keystore {} failed", domain, keystoreName, e);
            return ResponseUtil.createFailureResponse(1L, MediaType.APPLICATION_JSON_VALUE, "/certificates/domain/{domain}", e.getMessage());
        }
    }

    private ResponseEntity<String> downloadForDomain(String mediaType, String domain, String certificateType, String keystoreName, String keystorePassword, boolean replace) {
        log.debug("REST request to {} the {} certificates of domain {}", replace ? "renew" : "download", certificateType, domain);

        try {
            String url = CertificatesUtil.domainUrl(domain);
            String prefix = CertificatesUtil.certificateAliasPrefix(url);

            // download and select before touching the keystore, so a failed download changes nothing
            Certificate[] certificates = getCertificates(url);
            if (certificates == null || certificates.length == 0) {
                throw new CertificateException("The certificates of " + url + " couldn't be downloaded");
            }
            Map<String,Certificate> selected = CertificatesUtil.selectCertificates(url, certificates, certificateType);

            Map<String,Certificate> certificateMap = CertificatesUtil.storeDomainCertificates(keystorePath(keystoreName, true), password(keystorePassword), prefix, selected, replace);

            log.info("Stored the certificates {} of domain {} in keystore {}", certificateMap.keySet(), domain, keystoreName);

            return ResponseUtil.createSuccessResponse(1L, mediaType, "/certificates/domain/{domain}", certificatesAsJSon(certificateMap, url, keystoreName));
        } catch (Exception e) {
            log.error("{} the certificates of domain {} in keystore {} failed", replace ? "Renewing" : "Downloading", domain, keystoreName, e);
            return failure(mediaType, "/certificates/domain/{domain}", e);
        }
    }

    // --- identities -----------------------------------------------------------------------------------------------

    @Operation(summary = "Get all identities in the keystore with their expiry")
    @GetMapping(path = "/certificates/identity", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> listIdentities(
            @RequestHeader(value = "keystoreName", required = false, defaultValue = IDENTITY_STORE) String keystoreName,
            @RequestHeader(value = "keystorePassword", required = false) String keystorePassword
    ) {
        return list(keystoreName, keystorePassword, "/certificates/identity");
    }

    @Operation(summary = "Get an identity in the keystore with its expiry")
    @GetMapping(path = "/certificates/identity/{certificateName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getIdentity(
            @PathVariable(value = "certificateName") String certificateName,
            @RequestHeader(value = "keystoreName", required = false, defaultValue = IDENTITY_STORE) String keystoreName,
            @RequestHeader(value = "keystorePassword", required = false) String keystorePassword
    ) {
        return get(certificateName, keystoreName, keystorePassword, "/certificates/identity/{certificateName}");
    }

    @Operation(
            summary = "Import a server identity (private key + certificate chain) into the keystore",
            description = "The body is a PKCS12 (.p12/.pfx) file, base64 encoded, optionally as a data URL. The identity is stored under certificateName; "
                    + "further private keys in the file under certificateName-2, -3, ... Returns 409 when a name exists (use PUT to replace it)."
    )
    @PostMapping(
            path = "/certificates/identity/{certificateName}",
            consumes = {MediaType.TEXT_PLAIN_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> importIdentity(
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @PathVariable(value = "certificateName") String certificateName,
            @RequestBody String p12,
            @RequestHeader(value = "password") String password,
            @RequestHeader(value = "keystoreName", required = false, defaultValue = IDENTITY_STORE) String keystoreName,
            @RequestHeader(value = "keystorePassword", required = false) String keystorePassword
    ) {
        return storeIdentity(mediaType, certificateName, p12, password, keystoreName, keystorePassword, false);
    }

    @Operation(
            summary = "Create or replace (renew) a server identity in the keystore",
            description = "The body is a PKCS12 (.p12/.pfx) file with one private key, base64 encoded, optionally as a data URL."
    )
    @PutMapping(
            path = "/certificates/identity/{certificateName}",
            consumes = {MediaType.TEXT_PLAIN_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> replaceIdentity(
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @PathVariable(value = "certificateName") String certificateName,
            @RequestBody String p12,
            @RequestHeader(value = "password") String password,
            @RequestHeader(value = "keystoreName", required = false, defaultValue = IDENTITY_STORE) String keystoreName,
            @RequestHeader(value = "keystorePassword", required = false) String keystorePassword
    ) {
        return storeIdentity(mediaType, certificateName, p12, password, keystoreName, keystorePassword, true);
    }

    @Operation(summary = "Delete an identity from the keystore")
    @DeleteMapping(
            path = "/certificates/identity/{certificateName}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> deleteIdentity(
            @PathVariable(value = "certificateName") String certificateName,
            @RequestHeader(value = "keystoreName", required = false, defaultValue = IDENTITY_STORE) String keystoreName,
            @RequestHeader(value = "keystorePassword", required = false) String keystorePassword
    ) {
        return delete(certificateName, keystoreName, keystorePassword, "/certificates/identity/{certificateName}");
    }

    @Operation(
            summary = "Generate a server identity with a self-signed certificate",
            description = "Generates an RSA key pair and a self-signed certificate for the common name, and stores both as an identity. "
                    + "The identity is named after the common name, unless certificateName is given. Returns 409 when the name exists."
    )
    @PostMapping(
            path = "/certificates/identity/generate",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> generateIdentity(
            @Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType,
            @RequestHeader(value = "cn") String cn,
            @Parameter(description = "Name (alias) for the identity. Defaults to the common name")
            @RequestHeader(value = "certificateName", required = false) String certificateName,
            @RequestHeader(value = "keystoreName", required = false, defaultValue = IDENTITY_STORE) String keystoreName,
            @RequestHeader(value = "keystorePassword", required = false) String keystorePassword
    ) {

        log.debug("REST request to generate a self-signed identity for {} in keystore {}", cn, keystoreName);

        try {
            if (certificateName != null && !certificateName.isBlank()) {
                CertificatesUtil.validateAlias(certificateName, RESERVED_IDENTITY_NAMES);
            }

            Map<String,Certificate> certificateMap = CertificatesUtil.generateIdentity(keystorePath(keystoreName, true), password(keystorePassword), cn, certificateName);

            log.info("Generated self-signed identity {} in keystore {}", certificateMap.keySet(), keystoreName);

            return ResponseUtil.createSuccessResponse(1L, mediaType, "/certificates/identity/generate", certificatesAsJSon(certificateMap, null, keystoreName));
        } catch (Exception e) {
            log.error("Generate self-signed identity for {} in keystore {} failed", cn, keystoreName, e);
            return failure(mediaType, "/certificates/identity/generate", e);
        }

    }

    // --- shared ---------------------------------------------------------------------------------------------------

    private ResponseEntity<?> list(String keystoreName, String keystorePassword, String path) {
        log.debug("REST request to get the certificates in keystore {}", keystoreName);

        try {
            return ResponseEntity.ok(certificatesExpiry(keystoreName, password(keystorePassword)));
        } catch (Exception e) {
            log.error("Get the certificates in keystore {} failed", keystoreName, e);
            return ResponseUtil.createFailureResponse(1L, MediaType.APPLICATION_JSON_VALUE, path, e.getMessage());
        }
    }

    private ResponseEntity<?> get(String certificateName, String keystoreName, String keystorePassword, String path) {
        log.debug("REST request to get certificate {} in keystore {}", certificateName, keystoreName);

        try {
            X509Certificate certificate = CertificatesUtil.getCertificates(keystorePath(keystoreName), password(keystorePassword)).get(certificateName);
            if (certificate == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(CertificateExpiry.of(certificateName, certificate, Instant.now()));
        } catch (Exception e) {
            log.error("Get certificate {} in keystore {} failed", certificateName, keystoreName, e);
            return ResponseUtil.createFailureResponse(1L, MediaType.APPLICATION_JSON_VALUE, path, e.getMessage());
        }
    }

    private ResponseEntity<String> delete(String certificateName, String keystoreName, String keystorePassword, String path) {
        log.debug("REST request to delete certificate {} from keystore {}", certificateName, keystoreName);

        try {
            // a certificate that is not (or no longer) in the keystore is not an error, so the caller can still remove its record
            String keystorePath = keystorePath(keystoreName, true);
            boolean deleted = new File(keystorePath).isFile() && new CertificatesUtil().deleteCertificate(keystorePath, password(keystorePassword), certificateName);
            String message = deleted ? "success" : "certificate not found in keystore";
            return ResponseUtil.createSuccessResponse(1, "text/plain", path, message);
        } catch (Exception e) {
            log.error("Delete certificate {} from keystore {} failed", certificateName, keystoreName, e);
            return ResponseUtil.createFailureResponse(1, "text/plain", path, e.getMessage());
        }
    }

    private ResponseEntity<String> storeIdentity(String mediaType, String certificateName, String p12, String p12Password, String keystoreName, String keystorePassword, boolean replace) {
        log.debug("REST request to {} identity {} in keystore {}", replace ? "replace" : "import", certificateName, keystoreName);

        try {
            CertificatesUtil.validateAlias(certificateName, RESERVED_IDENTITY_NAMES);

            Map<String,Certificate> certificateMap = new CertificatesUtil().importIdentity(keystorePath(keystoreName, true), password(keystorePassword), p12, p12Password, certificateName, replace);

            log.info("Stored identities {} in keystore {}", certificateMap.keySet(), keystoreName);

            return ResponseUtil.createSuccessResponse(1L, mediaType, "/certificates/identity/{certificateName}", certificatesAsJSon(certificateMap, null, keystoreName));
        } catch (Exception e) {
            log.error("Store identity {} in keystore {} failed", certificateName, keystoreName, e);
            return failure(mediaType, "/certificates/identity/{certificateName}", e);
        }
    }

    /** 409 when an existing entry is in the way, otherwise 400. */
    private ResponseEntity<String> failure(String mediaType, String path, Exception e) {
        ResponseEntity<String> response = ResponseUtil.createFailureResponse(1L, mediaType, path, e.getMessage());
        if (e instanceof CertificatesUtil.AliasExistsException) {
            return ResponseEntity.status(HttpStatus.CONFLICT).headers(response.getHeaders()).body(response.getBody());
        }
        return response;
    }

    private void warnIfExpired(List<X509Certificate> certificates) {
        Instant now = Instant.now();
        for (X509Certificate certificate : certificates) {
            if (certificate.getNotAfter().toInstant().isBefore(now)) {
                log.warn("Certificate {} is expired (Expiry Date: {})", certificate.getSubjectX500Principal(), certificate.getNotAfter().toInstant());
            }
        }
    }

    private static String password(String keystorePassword) {
        return keystorePassword == null || keystorePassword.isEmpty() ? CertificatesUtil.runtimeKeystorePassword() : keystorePassword;
    }

    private List<CertificateExpiry> certificatesExpiry(String keystoreName, String keystorePassword) throws Exception {
        return certificatesExpiry(CertificatesUtil.getCertificates(keystorePath(keystoreName), keystorePassword));
    }

    private List<CertificateExpiry> certificatesExpiry(Map<String, X509Certificate> certificates) {
        Instant now = Instant.now();
        return certificates.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().getNotAfter()))
                .map(entry -> CertificateExpiry.of(entry.getKey(), entry.getValue(), now))
                .toList();
    }

    private List<CertificateExpiry.Expired> expiredCertificates(Map<String, X509Certificate> certificates, Instant now) {
        return certificates.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().getNotAfter()))
                .map(entry -> CertificateExpiry.Expired.of(entry.getKey(), entry.getValue(), now))
                .toList();
    }

    private String keystorePath(String keystoreName) throws KeyStoreException {
        return keystorePath(keystoreName, false);
    }

    // loading a keystore that doesn't exist creates it, so reading endpoints check first
    private String keystorePath(String keystoreName, boolean createIfMissing) throws KeyStoreException {
        String keystorePath = baseDir + "/security/" + keystoreName;
        if (!createIfMissing && !new File(keystorePath).isFile()) {
            throw new KeyStoreException("Keystore " + keystoreName + " doesn't exist");
        }
        return keystorePath;
    }

    private String certificatesAsJSon(Map<String,Certificate> certificateMap, String certificateUrl, String certificateStore) throws CertificateException {

        JSONObject certificatesObject  = new JSONObject();
        JSONObject certificateObject = new JSONObject();

        for (Map.Entry<String, Certificate> entry : certificateMap.entrySet()) {
            X509Certificate real = (X509Certificate) entry.getValue();

            JSONObject certificateDetails = new JSONObject();

            certificateDetails.put("certificateFile", CertificatesUtil.convertX509CertificateToPem(real));
            certificateDetails.put("certificateName", entry.getKey());
            certificateDetails.put("certificateStore", certificateStore);
            certificateDetails.put("certificateExpiry", real.getNotAfter().toInstant());
            certificateDetails.put("certificateUrl", certificateUrl);

            certificateObject.append("certificate", certificateDetails);
        }

        certificatesObject.put("certificates", certificateObject);

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

}
