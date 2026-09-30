package org.assimbly.util;

import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.util.*;
import org.apache.hc.client5.http.ssl.*;
import org.bouncycastle.asn1.x509.*;

import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.utils.DateUtils;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpResponseInterceptor;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.ssl.SSLContexts;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509ExtensionUtils;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.cert.Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public final class CertificatesUtil {

	private static final Logger log = LoggerFactory.getLogger("org.assimbly.util.CertificatesUtil");

	public static final String PEER_CERTIFICATES = "PEER_CERTIFICATES";

	private static final Pattern PEM_BOUNDARY = Pattern.compile("-+(BEGIN|END)[^-]*-+");
	/** The keystore with the trusted certificates for outbound TLS connections. */
	public static final String TRUSTSTORE_FILE = "outbound-truststore.p12";
	/** The keystore with the server identities (private key + certificate chain). */
	public static final String IDENTITY_STORE_FILE = "server-identity.p12";
	private static final String SERVER_IDENTITY_FILE = IDENTITY_STORE_FILE;
	private static final String KEYSTORE_PWD = "KEYSTORE_PWD";
	private static final String DEFAULT_KEYSTORE_PWD = "supersecret";
	private static final Pattern NON_ALIAS_CHARACTERS = Pattern.compile("[^a-z0-9]+");
	private static final Pattern VALID_ALIAS = Pattern.compile("[a-z0-9]([a-z0-9._-]*[a-z0-9])?");
	private static final Pattern VALID_DOMAIN = Pattern.compile("[A-Za-z0-9.-]+(:\\d{1,5})?");

	public static final String CERTIFICATE_TYPE_ROOT = "root";
	public static final String CERTIFICATE_TYPE_INTERMEDIATE = "intermediate";
	public static final String CERTIFICATE_TYPE_LEAF = "leaf";
	public static final String CERTIFICATE_TYPE_ALL = "all";
	private static final Set<String> CERTIFICATE_TYPES = Set.of(CERTIFICATE_TYPE_ROOT, CERTIFICATE_TYPE_INTERMEDIATE, CERTIFICATE_TYPE_LEAF, CERTIFICATE_TYPE_ALL);

	public Certificate[] downloadCertificates(String url) throws Exception {

		IO.println("Start downloading certificates (url=" + url + ")");

		Certificate[] peerCertificates;

		// Use an atomic reference to capture certificates from the response handler
		AtomicReference<Certificate[]> certificateHolder = new AtomicReference<>();

		// 1. Build the SSLContext with TrustAllStrategy
		SSLContext sslContext = SSLContexts.custom()
				.loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
				.build();

		// 2. Create the TLS Strategy using the modern API (replaces deprecated SSLConnectionSocketFactory)
		TlsSocketStrategy tlsSocketStrategy = new DefaultClientTlsStrategy(
				sslContext,
				NoopHostnameVerifier.INSTANCE
		);

		// 3. Create the Connection Manager using the modern TLS strategy
		PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
				.setTlsSocketStrategy(tlsSocketStrategy)
				.build();

		// 4. Create the response interceptor to capture SSL certificates
		//    Uses the modern non-deprecated HttpClientContext.cast() instead of adapt()
		HttpResponseInterceptor certificateInterceptor = (HttpResponse response, EntityDetails entityDetails, HttpContext context) -> {
			HttpClientContext clientContext = HttpClientContext.cast(context);
			SSLSession sslSession = clientContext.getSSLSession();
			if (sslSession != null) {
				try {
					certificateHolder.set(sslSession.getPeerCertificates());
				} catch (SSLPeerUnverifiedException e) {
					log.warn("Could not retrieve peer certificates from SSL session", e);
				}
			}
		};

		try (CloseableHttpClient httpClient = HttpClients.custom()
				.setConnectionManager(connectionManager)
				.addResponseInterceptorLast(certificateInterceptor)
				.build()) {

			HttpGet httpGet = new HttpGet(url);
			log.info("Executing request {} {}", httpGet.getMethod(), httpGet.getUri());

			// 5. Use the modern execute() with a response handler — no HttpContext needed
			httpClient.execute(httpGet, response -> {
				// Consume the response entity to ensure the connection is properly released
				EntityUtils.consume(response.getEntity());
				return null;
			});

			peerCertificates = certificateHolder.get();

			if (peerCertificates != null) {
				for (Certificate certificate : peerCertificates) {
					X509Certificate real = (X509Certificate) certificate;
					IO.println("----------------------------------------");
					IO.println("Type: " + real.getType());
					IO.println("Signing Algorithm: " + real.getSigAlgName());
					IO.println("IssuerDN Principal: " + real.getIssuerX500Principal());
					IO.println("SubjectDN Principal: " + real.getSubjectX500Principal());
					IO.println("Not After: " + DateUtils.formatStandardDate(real.getNotAfter().toInstant()));
					IO.println("Not Before: " + DateUtils.formatStandardDate(real.getNotBefore().toInstant()));
				}
			} else {
				log.error("Certificates not found. URL: {}", url);
			}
		}

		return peerCertificates;
	}

	public Certificate getCertificate(String keyStorePath, String keystorePassword, String certificateName) {

		try {
			//load keystore
			KeyStore keystore = loadKeyStore(keyStorePath, keystorePassword, null);

			Certificate certificate = keystore.getCertificate(certificateName);

			storeKeystore(keyStorePath,keystorePassword,keystore);

			return certificate;

		}catch (Exception e) {
			log.error("Get certificate for keystore {} with name {} failed", keyStorePath, certificateName, e);
		}
		return null;

	}


	public String importCertificate(String keyStorePath, String keystorePassword, String certificateName, Certificate certificate) {

		try {

			//load keystore
			KeyStore keystore = loadKeyStore(keyStorePath, keystorePassword, null);

			// Add the certificate to the store
			X509Certificate real = (X509Certificate) certificate;
            IO.println("----------------------------------------");
            IO.println("Type: " + real.getType());
            IO.println("Signing Algorithm: " + real.getSigAlgName());
            IO.println("IssuerDN Principal: " + real.getIssuerX500Principal());
            IO.println("SubjectDN Principal: " + real.getSubjectX500Principal());
            IO.println("Not After: " + DateUtils.formatStandardDate(real.getNotAfter().toInstant()));
            IO.println("Not Before: " + DateUtils.formatStandardDate(real.getNotBefore().toInstant()));
			keystore.setCertificateEntry(certificateName, certificate);
            IO.println("Original alias: " + certificateName);
            IO.println("Cert alias: " + keystore.getCertificateAlias(certificate));
			IO.println("----------------------------------------");

			// Save the new keystore contents
			storeKeystore(keyStorePath,keystorePassword,keystore);

		}catch (Exception e) {
			log.error("Import certificate for keystore {} with name {} failed", keyStorePath, certificateName, e);
		}

		return certificateName;

	}

	public Map<String,Certificate> storeCertificates(String keyStorePath, String keystorePassword, Certificate[] certificates) {

        IO.println("Importing certificates");
		Map<String,Certificate> certificateMap = new ConcurrentHashMap<>();

		try {
			//load keystore
			KeyStore keystore = loadKeyStore(keyStorePath, keystorePassword, null);

			// Add the certificate to the store
			for (Certificate certificate : certificates){
				X509Certificate real = (X509Certificate) certificate;
                IO.println("----------------------------------------");
                IO.println("Type: " + real.getType());
                IO.println("Signing Algorithm: " + real.getSigAlgName());
                IO.println("IssuerDN Principal: " + real.getIssuerX500Principal());
                IO.println("SubjectDN Principal: " + real.getSubjectX500Principal());
                IO.println("Not After: " + DateUtils.formatStandardDate(real.getNotAfter().toInstant()));
                IO.println("Not Before: " + DateUtils.formatStandardDate(real.getNotBefore().toInstant()));				String alias = UUID.randomUUID().toString();
				certificateMap.put(alias, certificate);
				keystore.setCertificateEntry(alias, certificate);
                IO.println("Original alias:" + alias);
                IO.println("Cert alias" + keystore.getCertificateAlias(certificate));
				IO.println("----------------------------------------");

			}

			// Save the new keystore contents
			storeKeystore(keyStorePath,keystorePassword,keystore);

		}catch (Exception e) {
			log.error("Import certificates for keystore {} failed", keyStorePath, e);
		}

		return certificateMap;
	}

	/**
	 * Creates the alias prefix for certificates downloaded from a url: the host without "www.",
	 * with every run of characters other than a-z and 0-9 replaced by a hyphen (https://www.github.com/ becomes github-com).
	 */
	public static String certificateAliasPrefix(String url) {

		String host = url == null ? null : URI.create(url.trim()).getHost();
		if (host == null || host.isBlank()) {
			throw new IllegalArgumentException("Can't determine the host of url " + url);
		}

		host = host.toLowerCase(Locale.ROOT);
		if (host.startsWith("www.")) {
			host = host.substring(4);
		}

		String prefix = toAlias(host);
		if (prefix.isEmpty()) {
			throw new IllegalArgumentException("Can't determine the host of url " + url);
		}

		return prefix;
	}

	/**
	 * Creates the alias of a certificate from its common name (CN=Sectigo Public Server Authentication Root E46 becomes
	 * sectigo-public-server-authentication-root-e46). Without a common name the alias is certificate-&lt;start of the SHA-256 fingerprint&gt;.
	 */
	public static String certificateAlias(X509Certificate certificate) throws CertificateException {

		RDN[] commonNames = X500Name.getInstance(certificate.getSubjectX500Principal().getEncoded()).getRDNs(BCStyle.CN);
		String alias = commonNames.length == 0 ? "" : toAlias(IETFUtils.valueToString(commonNames[0].getFirst().getValue()));

		if (alias.isEmpty()) {
			try {
				byte[] fingerprint = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
				alias = "certificate-" + HexFormat.of().formatHex(fingerprint, 0, 8);
			} catch (NoSuchAlgorithmException e) {
				throw new CertificateException(e);
			}
		}

		return alias;
	}

	/** The given name as alias, or the alias of the certificate when no name is given. */
	private static String aliasFor(X509Certificate certificate, String name) throws CertificateException {
		if (name == null || name.isBlank()) {
			return certificateAlias(certificate);
		}
		String alias = toAlias(name);
		if (alias.isEmpty()) {
			throw new IllegalArgumentException("Certificate name '" + name + "' must contain letters or digits");
		}
		return alias;
	}

	/** Lowercase, with every run of characters other than a-z and 0-9 replaced by a hyphen. */
	private static String toAlias(String name) {
		return NON_ALIAS_CHARACTERS.matcher(name.toLowerCase(Locale.ROOT)).replaceAll("-").replaceAll("^-+|-+$", "");
	}

	/**
	 * Selects the certificates of a downloaded chain and names them &lt;host&gt;-root-ca, &lt;host&gt;-intermediate-ca(-n) and &lt;host&gt;-leaf.
	 * When the server didn't send the root, it's looked up in the JVM default truststore.
	 *
	 * @param url the url the chain was downloaded from
	 * @param chain the peer certificates, leaf first
	 * @param certificateType root (default when empty), intermediate, leaf or all
	 * @return the selected certificates by alias, ordered root, intermediates, leaf
	 */
	public static Map<String, Certificate> selectCertificates(String url, Certificate[] chain, String certificateType) throws CertificateException {
		return selectCertificates(url, chain, certificateType, null);
	}

	static Map<String, Certificate> selectCertificates(String url, Certificate[] chain, String certificateType, X509Certificate[] trustAnchors) throws CertificateException {

		String type = certificateType == null || certificateType.isBlank() ? CERTIFICATE_TYPE_ROOT : certificateType.trim().toLowerCase(Locale.ROOT);
		if (!CERTIFICATE_TYPES.contains(type)) {
			throw new IllegalArgumentException("Unknown certificateType '" + certificateType + "' (expected root, intermediate, leaf or all)");
		}

		if (chain == null || chain.length == 0) {
			throw new CertificateException("No certificates downloaded from " + url);
		}

		String prefix = certificateAliasPrefix(url);

		X509Certificate leaf = (X509Certificate) chain[0];
		X509Certificate root = null;
		List<X509Certificate> intermediates = new ArrayList<>();

		for (int i = 1; i < chain.length; i++) {
			X509Certificate certificate = (X509Certificate) chain[i];
			if (root == null && isSelfSigned(certificate)) {
				root = certificate;
			} else {
				intermediates.add(certificate);
			}
		}

		boolean includeRoot = type.equals(CERTIFICATE_TYPE_ROOT) || type.equals(CERTIFICATE_TYPE_ALL);
		boolean includeIntermediates = type.equals(CERTIFICATE_TYPE_INTERMEDIATE) || type.equals(CERTIFICATE_TYPE_ALL);
		boolean includeLeaf = type.equals(CERTIFICATE_TYPE_LEAF) || type.equals(CERTIFICATE_TYPE_ALL);

		if (root == null && includeRoot) {
			if (intermediates.isEmpty() && isSelfSigned(leaf)) {
				root = leaf;
			} else {
				// servers usually don't send the root: find the trusted issuer of the chain, walking up from the leaf
				X509Certificate[] anchors = trustAnchors != null ? trustAnchors : defaultTrustAnchors();
				for (int i = 0; i < chain.length && root == null; i++) {
					root = findTrustAnchor((X509Certificate) chain[i], anchors);
				}
			}
		}

		Map<String, Certificate> selected = new LinkedHashMap<>();

		if (includeRoot) {
			if (root == null) {
				throw new CertificateException("Root certificate for " + prefix + " was not sent by the server and was not found in the JVM default truststore");
			}
			selected.put(prefix + "-root-ca", root);
		}

		if (includeIntermediates) {
			if (intermediates.isEmpty() && !type.equals(CERTIFICATE_TYPE_ALL)) {
				throw new CertificateException("The server of " + url + " didn't send any intermediate certificates");
			}
			for (int i = 0; i < intermediates.size(); i++) {
				selected.put(prefix + "-intermediate-ca" + (i == 0 ? "" : "-" + (i + 1)), intermediates.get(i));
			}
		}

		// a self-signed leaf is its own root, store it only once
		if (includeLeaf && !(includeRoot && leaf.equals(root))) {
			selected.put(prefix + "-leaf", leaf);
		}

		return selected;
	}

	/**
	 * Finds the certificate in the JVM default truststore that issued the given certificate.
	 *
	 * @return the issuer, or null when it's not in the truststore
	 */
	public static X509Certificate findTrustAnchor(X509Certificate certificate) throws CertificateException {
		return findTrustAnchor(certificate, defaultTrustAnchors());
	}

	static X509Certificate findTrustAnchor(X509Certificate certificate, X509Certificate[] anchors) {
		for (X509Certificate anchor : anchors) {
			if (anchor.getSubjectX500Principal().equals(certificate.getIssuerX500Principal()) && isSignedBy(certificate, anchor)) {
				return anchor;
			}
		}
		return null;
	}

	private static X509Certificate[] defaultTrustAnchors() throws CertificateException {
		try {
			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init((KeyStore) null);
			for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
				if (trustManager instanceof X509TrustManager x509TrustManager) {
					return x509TrustManager.getAcceptedIssuers();
				}
			}
		} catch (GeneralSecurityException e) {
			throw new CertificateException("Can't load the JVM default truststore", e);
		}
		return new X509Certificate[0];
	}

	private static boolean isSelfSigned(X509Certificate certificate) {
		return certificate.getSubjectX500Principal().equals(certificate.getIssuerX500Principal()) && isSignedBy(certificate, certificate);
	}

	private static boolean isSignedBy(X509Certificate certificate, X509Certificate issuer) {
		try {
			certificate.verify(issuer.getPublicKey());
			return true;
		} catch (GeneralSecurityException _) {
			return false;
		}
	}

	/**
	 * Parses one or more X.509 certificates. The content can be PEM (a bundle of several certificates is allowed),
	 * or base64 encoded PEM or DER, optionally as a data URL (data:...;base64,...).
	 */
	public static List<X509Certificate> parseCertificates(String content) throws CertificateException {

		if (content == null || content.isBlank()) {
			throw new CertificateException("No certificate content");
		}

		byte[] bytes;
		if (content.contains("-----BEGIN")) {
			bytes = content.getBytes(StandardCharsets.UTF_8);
		} else {
			try {
				bytes = decodeBase64(content);
			} catch (IllegalArgumentException e) {
				throw new CertificateException("The content is not a PEM or base64 encoded certificate", e);
			}
		}

		List<X509Certificate> certificates = new ArrayList<>();
		for (Certificate certificate : CertificateFactory.getInstance("X.509").generateCertificates(new ByteArrayInputStream(bytes))) {
			certificates.add((X509Certificate) certificate);
		}

		if (certificates.isEmpty()) {
			throw new CertificateException("No certificates found in the content");
		}

		return certificates;
	}

	/** Thrown when a certificate or identity would replace an existing keystore entry that it may not replace. */
	public static class AliasExistsException extends KeyStoreException {
		public AliasExistsException(String message) {
			super(message);
		}
	}

	/**
	 * Checks that a name can be used as alias for a new entry: lowercase letters, digits, dots, underscores and hyphens,
	 * starting and ending with a letter or digit (PKCS12 aliases are case-insensitive), and not one of the reserved names.
	 */
	public static void validateAlias(String name, Set<String> reserved) {
		if (name == null || !VALID_ALIAS.matcher(name).matches()) {
			throw new IllegalArgumentException("Invalid certificate name '" + name + "': use lowercase letters, digits, '.', '_' and '-', starting and ending with a letter or digit");
		}
		if (reserved.contains(name)) {
			throw new IllegalArgumentException("Certificate name '" + name + "' is reserved");
		}
	}

	/**
	 * Imports certificates as trusted certificate entries: the first under the given name, the others (of a bundle)
	 * under name-2, name-3, ... Importing a certificate that is already stored under its name changes nothing.
	 *
	 * @return the imported certificates by alias
	 * @throws AliasExistsException when one of the names holds a different certificate or a private key; nothing is stored then
	 */
	public static Map<String, Certificate> importTrustedCertificates(String keyStorePath, String keystorePassword, List<X509Certificate> certificates, String name) throws Exception {

		KeyStore keystore = loadKeyStore(keyStorePath, keystorePassword, null);

		Map<String, Certificate> imported = new LinkedHashMap<>();
		for (int i = 0; i < certificates.size(); i++) {
			String alias = i == 0 ? name : name + "-" + (i + 1);
			X509Certificate certificate = certificates.get(i);
			if (keystore.isKeyEntry(alias) || (keystore.containsAlias(alias) && !certificate.equals(keystore.getCertificate(alias)))) {
				throw new AliasExistsException("Certificate '" + alias + "' already exists, use PUT to replace it");
			}
			imported.put(alias, certificate);
		}

		for (Map.Entry<String, Certificate> entry : imported.entrySet()) {
			keystore.setCertificateEntry(entry.getKey(), entry.getValue());
		}
		storeKeystore(keyStorePath, keystorePassword, keystore);

		return imported;
	}

	/**
	 * Stores the certificate as trusted certificate entry under the given name, replacing a trusted certificate with that name.
	 *
	 * @throws AliasExistsException when the name holds a private key (an identity)
	 */
	public static Map<String, Certificate> putTrustedCertificate(String keyStorePath, String keystorePassword, String name, X509Certificate certificate) throws Exception {

		KeyStore keystore = loadKeyStore(keyStorePath, keystorePassword, null);

		if (keystore.isKeyEntry(name)) {
			throw new AliasExistsException("'" + name + "' is an identity with a private key and can't be replaced by a trusted certificate");
		}

		keystore.setCertificateEntry(name, certificate);
		storeKeystore(keyStorePath, keystorePassword, keystore);

		return Map.of(name, certificate);
	}

	/**
	 * Imports the private key entries (identities) of a PKCS12 file: the first under the given name, the others under
	 * name-2, name-3, ...
	 *
	 * @param p12Content the PKCS12 file, base64 encoded, optionally as a data URL
	 * @param replace false to refuse existing names, true to replace the identity with that name; the file must then
	 *                hold exactly one private key
	 * @return the certificate of each imported identity by alias
	 * @throws AliasExistsException when replace is false and one of the names exists; nothing is stored then
	 */
	public Map<String, Certificate> importIdentity(String keyStorePath, String keystorePassword, String p12Content, String p12Password, String name, boolean replace) throws Exception {

		KeyStore p12Store = loadKeystoreFromString(p12Content, p12Password, "pkcs12");

		List<String> keyAliases = new ArrayList<>();
		for (String p12Alias : Collections.list(p12Store.aliases())) {
			Certificate[] chain = p12Store.isKeyEntry(p12Alias) ? p12Store.getCertificateChain(p12Alias) : null;
			if (chain != null && chain.length > 0) {
				keyAliases.add(p12Alias);
			}
		}

		if (keyAliases.isEmpty()) {
			throw new KeyStoreException("The PKCS12 file doesn't contain a private key with a certificate");
		}
		if (replace && keyAliases.size() > 1) {
			throw new IllegalArgumentException("Replacing identity '" + name + "' needs a PKCS12 file with one private key, this one has " + keyAliases.size());
		}

		KeyStore keystore = loadKeyStore(keyStorePath, keystorePassword, null);

		Map<String, Certificate> imported = new LinkedHashMap<>();
		for (int i = 0; i < keyAliases.size(); i++) {
			String alias = i == 0 ? name : name + "-" + (i + 1);
			if (!replace && keystore.containsAlias(alias)) {
				throw new AliasExistsException("Identity '" + alias + "' already exists, use PUT to replace it");
			}
			String p12Alias = keyAliases.get(i);
			Certificate[] chain = p12Store.getCertificateChain(p12Alias);
			keystore.setKeyEntry(alias, p12Store.getKey(p12Alias, p12Password.toCharArray()), keystorePassword.toCharArray(), chain);
			imported.put(alias, chain[0]);
		}

		storeKeystore(keyStorePath, keystorePassword, keystore);

		return imported;
	}

	/**
	 * Generates a key pair with a self-signed certificate for the common name and stores it as an identity
	 * (private key entry), named after the common name (or after the given name).
	 *
	 * @return the certificate of the identity by alias
	 * @throws AliasExistsException when the name exists
	 */
	public static Map<String, Certificate> generateIdentity(String keyStorePath, String keystorePassword, String cn, String name) throws Exception {

		KeyStore keystore = loadKeyStore(keyStorePath, keystorePassword, null);

		String requestedAlias = name == null || name.isBlank() ? null : name;
		if (requestedAlias != null && keystore.containsAlias(requestedAlias)) {
			throw new AliasExistsException("Identity '" + requestedAlias + "' already exists");
		}

		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(4096, new SecureRandom());
		KeyPair keyPair = keyPairGenerator.generateKeyPair();

		X509Certificate certificate = (X509Certificate) selfsignCertificate2(keyPair, cn);
		String alias = aliasFor(certificate, name);
		if (keystore.containsAlias(alias)) {
			throw new AliasExistsException("Identity '" + alias + "' already exists");
		}

		keystore.setKeyEntry(alias, keyPair.getPrivate(), keystorePassword.toCharArray(), new Certificate[]{certificate});
		storeKeystore(keyStorePath, keystorePassword, keystore);

		return Map.of(alias, certificate);
	}

	/** The password of the runtime's keystores: the KEYSTORE_PWD environment variable, else the default password. */
	public static String runtimeKeystorePassword() {
		String keystorePassword = System.getenv(KEYSTORE_PWD);
		return keystorePassword == null || keystorePassword.isEmpty() ? DEFAULT_KEYSTORE_PWD : keystorePassword;
	}

	/** Decodes base64 content, ignoring line breaks and a data URL prefix (data:...;base64,). */
	static byte[] decodeBase64(String content) {
		String base64 = content.trim();
		if (base64.startsWith("data:") && base64.indexOf(',') > 0) {
			base64 = base64.substring(base64.indexOf(',') + 1);
		}
		return Base64.getMimeDecoder().decode(base64);
	}

	public Map<String,Certificate> importP12Certificate(String keyStorePath, String keystorePassword, String p12Certificate, String p12Password) throws Exception {

		KeyStore serverIdentity = loadKeyStore(keyStorePath, keystorePassword, null);

		KeyStore p12Store = loadKeystoreFromString(p12Certificate, p12Password, "pkcs12");

		Enumeration<String> aliases = p12Store.aliases();

		Map<String,Certificate> certificateMap = new ConcurrentHashMap<>();


		while (aliases.hasMoreElements()) {

			String alias = aliases.nextElement();

			if (p12Store.isKeyEntry(alias)) {
                IO.println("Adding key for alias " + alias);
				Key key = p12Store.getKey(alias, p12Password.toCharArray());

				Certificate[] chain = p12Store.getCertificateChain(alias);

				serverIdentity.setKeyEntry(alias, key, keystorePassword.toCharArray(), chain);

				// the identity's own certificate is the first of its chain
				certificateMap.put(alias, chain[0]);

			}
		}

		storeKeystore(keyStorePath,keystorePassword,serverIdentity);

		return certificateMap;

	}

	/**
	 * Deletes the certificate with the given alias from the keystore.
	 *
	 * @return true if the certificate was found and deleted, false if the alias doesn't exist
	 */
	public boolean deleteCertificate(String keyStorePath, String keystorePassword, String certificateName) throws Exception {

		KeyStore keystore = loadKeyStore(keyStorePath, keystorePassword, null);

		if (!keystore.containsAlias(certificateName)) {
			log.warn("Certificate {} not found in keystore {}", certificateName, keyStorePath);
			return false;
		}

		keystore.deleteEntry(certificateName);

		storeKeystore(keyStorePath, keystorePassword, keystore);

		return true;

	}

	/**
	 * Returns the X.509 certificate of every entry in the keystore (for a key entry the first certificate of its chain).
	 *
	 * @return the certificates by alias, sorted by alias
	 */
	public static Map<String, X509Certificate> getCertificates(String keyStorePath, String keystorePassword) throws Exception {

		KeyStore keystore = loadKeyStore(keyStorePath, keystorePassword, null);

		Map<String, X509Certificate> certificates = new TreeMap<>();
		for (String alias : Collections.list(keystore.aliases())) {
			if (keystore.getCertificate(alias) instanceof X509Certificate certificate) {
				certificates.put(alias, certificate);
			}
		}

		return certificates;
	}

	/**
	 * The https url of a domain: a host name with an optional port, like www.github.com or api.example.com:8443.
	 */
	public static String domainUrl(String domain) {
		if (domain == null || !VALID_DOMAIN.matcher(domain).matches()) {
			throw new IllegalArgumentException("Invalid domain '" + domain + "': use a host name with an optional port, like www.github.com or api.example.com:8443");
		}
		return "https://" + domain + "/";
	}

	/** The aliases selectCertificates gives the certificates of a domain: prefix-root-ca, prefix-intermediate-ca(-n) and prefix-leaf. */
	static Pattern domainAliases(String prefix) {
		return Pattern.compile(Pattern.quote(prefix) + "-(root-ca|intermediate-ca(-\\d+)?|leaf)");
	}

	/**
	 * Returns the downloaded certificates of a domain.
	 *
	 * @param prefix the alias prefix of the domain (see certificateAliasPrefix)
	 * @return the certificates by alias, sorted by alias
	 */
	public static Map<String, X509Certificate> getDomainCertificates(String keyStorePath, String keystorePassword, String prefix) throws Exception {
		Pattern aliases = domainAliases(prefix);
		Map<String, X509Certificate> certificates = new TreeMap<>(getCertificates(keyStorePath, keystorePassword));
		certificates.keySet().removeIf(alias -> !aliases.matcher(alias).matches());
		return certificates;
	}

	/**
	 * Stores the downloaded certificates of a domain in one keystore write.
	 *
	 * @param prefix the alias prefix of the domain (see certificateAliasPrefix)
	 * @param selected the certificates to store by alias (see selectCertificates)
	 * @param replace false to refuse when the domain has certificates, true to remove all of them first (renewal)
	 * @return the stored certificates by alias
	 * @throws AliasExistsException when replace is false and the domain has certificates; nothing is stored then
	 */
	public static Map<String, Certificate> storeDomainCertificates(String keyStorePath, String keystorePassword, String prefix, Map<String, Certificate> selected, boolean replace) throws Exception {

		KeyStore keystore = loadKeyStore(keyStorePath, keystorePassword, null);

		Pattern aliases = domainAliases(prefix);
		List<String> existing = Collections.list(keystore.aliases()).stream()
				.filter(alias -> aliases.matcher(alias).matches())
				.sorted()
				.toList();

		if (!existing.isEmpty() && !replace) {
			throw new AliasExistsException("The domain already has certificates " + existing + ", use PUT to renew them");
		}

		for (String alias : existing) {
			if (keystore.isCertificateEntry(alias)) {
				keystore.deleteEntry(alias);
			} else {
				log.warn("Key entry {} in keystore {} is not a downloaded certificate and is kept", alias, keyStorePath);
			}
		}

		for (Map.Entry<String, Certificate> entry : selected.entrySet()) {
			if (keystore.isKeyEntry(entry.getKey())) {
				throw new AliasExistsException("'" + entry.getKey() + "' is an identity with a private key and can't be replaced by a downloaded certificate");
			}
			keystore.setCertificateEntry(entry.getKey(), entry.getValue());
		}

		storeKeystore(keyStorePath, keystorePassword, keystore);

		return selected;
	}

	/**
	 * Deletes the downloaded certificates of a domain.
	 *
	 * @param prefix the alias prefix of the domain (see certificateAliasPrefix)
	 * @return the deleted certificates by alias
	 */
	public static Map<String, X509Certificate> deleteDomainCertificates(String keyStorePath, String keystorePassword, String prefix) throws Exception {

		KeyStore keystore = loadKeyStore(keyStorePath, keystorePassword, null);

		Pattern aliases = domainAliases(prefix);
		Map<String, X509Certificate> deleted = new TreeMap<>();
		for (String alias : Collections.list(keystore.aliases())) {
			if (aliases.matcher(alias).matches() && keystore.isCertificateEntry(alias)
					&& keystore.getCertificate(alias) instanceof X509Certificate certificate) {
				keystore.deleteEntry(alias);
				deleted.put(alias, certificate);
			}
		}

		if (!deleted.isEmpty()) {
			storeKeystore(keyStorePath, keystorePassword, keystore);
		}

		return deleted;
	}

	/**
	 * Deletes the expired certificate entries from the keystore. Expired key entries (identities with a private key) are kept.
	 *
	 * @return the deleted certificates by alias
	 */
	public static Map<String, X509Certificate> deleteExpiredCertificates(String keyStorePath, String keystorePassword, Instant now) throws Exception {

		KeyStore keystore = loadKeyStore(keyStorePath, keystorePassword, null);

		Map<String, X509Certificate> deleted = new TreeMap<>();
		for (String alias : Collections.list(keystore.aliases())) {
			if (!(keystore.getCertificate(alias) instanceof X509Certificate certificate) || !certificate.getNotAfter().toInstant().isBefore(now)) {
				continue;
			}
			if (keystore.isCertificateEntry(alias)) {
				keystore.deleteEntry(alias);
				deleted.put(alias, certificate);
			} else {
				log.warn("Expired key entry {} in keystore {} is not deleted", alias, keyStorePath);
			}
		}

		if (!deleted.isEmpty()) {
			storeKeystore(keyStorePath, keystorePassword, keystore);
		}

		return deleted;
	}



	public static String convertStringToBinary(String input) {
		StringBuilder bString = new StringBuilder();

		for (int i = 0; i < input.length(); i++) {
			StringBuilder temp = new StringBuilder(Integer.toBinaryString(input.charAt(i)));

			// Ensure each binary value is 8 bits long (pad with leading zeros)
			while (temp.length() < 8) {
				temp.insert(0, "0");
			}

			bString.append(temp).append(' ');
		}

		return bString.toString();
	}

	public static String convertX509CertificateToPem(X509Certificate certificate) throws CertificateEncodingException {

		byte[] derCertificate = certificate.getEncoded();

		java.util.Base64.Encoder encoder = java.util.Base64.getMimeEncoder(64, new byte[]{'\r', '\n'});

		String pemCertificate = new String(encoder.encode(derCertificate), StandardCharsets.UTF_8);

		return "-----BEGIN CERTIFICATE-----\r\n" + pemCertificate + "\r\n-----END CERTIFICATE-----";

	}

	public static X509Certificate convertPemToX509Certificate(String pemCertificate) throws CertificateException {

		java.util.Base64.Decoder decoder = java.util.Base64.getDecoder();
		CertificateFactory cf = CertificateFactory.getInstance("X509");
		X509Certificate certificate = null;

		try {
			if (pemCertificate != null && !pemCertificate.trim().isEmpty()) {

				// Strip header/footer wherever they are (the footer is not always on its own line) and all whitespace
				String base64Certificate = PEM_BOUNDARY.matcher(pemCertificate).replaceAll("").replaceAll("\\s", "");

				byte[] derCertificate = decoder.decode(base64Certificate);

				certificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(derCertificate));

			}
		} catch (CertificateException e) {
			throw new CertificateException(e);
		}
		return certificate;
	}


	public static KeyStore loadKeyStore(String keyStorePath, String keystorePassword, String keystoreType) throws Exception {

		File file = new File(keyStorePath);

		KeyStore keystore;

		if(keystoreType == null){
			keystore = KeyStore.getInstance(KeyStore.getDefaultType());
		}else{
			keystore = KeyStore.getInstance(keystoreType);
		}

		if (file.exists()) {
			InputStream is = Files.newInputStream(file.toPath());
			keystore.load(is, keystorePassword.toCharArray());
			is.close();
		} else {

			createKeystoreFile(file);
			keystore.load(null, keystorePassword.toCharArray());

			OutputStream os = Files.newOutputStream(file.toPath());
			keystore.store(os, keystorePassword.toCharArray());
			os.close();
		}

		return keystore;
	}

	public KeyStore loadKeystoreFromString(String keyStoreString, String keystorePassword, String keystoreType) throws Exception {

		byte[] decodedByte = decodeBase64(keyStoreString);

		InputStream inputStream = new ByteArrayInputStream(decodedByte);

		KeyStore keystore;

		if(keystoreType == null){
			keystore = KeyStore.getInstance(KeyStore.getDefaultType());
		}else{
			keystore = KeyStore.getInstance(keystoreType);
		}

		keystore.load(inputStream, keystorePassword.toCharArray());

		return keystore;
	}

	public static void storeKeystore(String keyStorePath, String keystorePassword, KeyStore keystore)  throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {

		// Save the new keystore contents
		File file = new File(keyStorePath);
		OutputStream out = Files.newOutputStream(file.toPath());
		keystore.store(out, keystorePassword.toCharArray());
		out.close();

	}

	public static void createKeystoreFile(File file) throws IOException {

		File securityPath = file.getParentFile();
		Path path = file.toPath();

		if (!securityPath.exists()) {
			boolean dirsCreated = securityPath.mkdirs();
			if(!dirsCreated){
                IO.println("Keystore Directory: " + securityPath.getAbsolutePath() + " couldn't be created.");
			}
		}

		boolean newFile = file.createNewFile();
		if(!newFile){
            IO.println("Keystore File: " + file.getAbsolutePath() + " couldn't be created.");
		}

		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		InputStream is = classloader.getResourceAsStream(SERVER_IDENTITY_FILE);
		if (is != null) {
			Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING);
			is.close();
		}

	}

	/**
	 * Generates a self signed certificate using the BouncyCastle lib.
	 *
	 * @param keyPair used for signing the certificate with PrivateKey
	 * @param hashAlgorithm Hash function
	 * @param cn Common Name to be used in the subject dn
	 * @param days validity period in days of the certificate
	 *
	 * @return self-signed X509Certificate
	 *
	 * @throws OperatorCreationException on creating a key id
	 * @throws CertIOException on building JcaContentSignerBuilder
	 * @throws CertificateException on getting certificate from provider
	 */
	public static X509Certificate selfsignCertificate(final KeyPair keyPair,
													  final String hashAlgorithm,
													  final String cn,
													  final int days)
			throws OperatorCreationException, CertificateException, CertIOException
	{
		final Instant now = Instant.now();
		final Date notBefore = Date.from(now);
		final Date notAfter = Date.from(now.plus(Duration.ofDays(days)));

		final ContentSigner contentSigner = new JcaContentSignerBuilder(hashAlgorithm).build(keyPair.getPrivate());
		final X500Name x500Name = new X500Name("CN=" + cn);
		final X509v3CertificateBuilder certificateBuilder =
				new JcaX509v3CertificateBuilder(x500Name,
						BigInteger.valueOf(now.toEpochMilli()),
						notBefore,
						notAfter,
						x500Name,
						keyPair.getPublic())
						.addExtension(Extension.subjectKeyIdentifier, false, createSubjectKeyId(keyPair.getPublic()))
						.addExtension(Extension.authorityKeyIdentifier, false, createAuthorityKeyId(keyPair.getPublic()))
						.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));

		return new JcaX509CertificateConverter()
				.setProvider(new BouncyCastleProvider()).getCertificate(certificateBuilder.build(contentSigner));
	}

	public static Certificate selfsignCertificate2(KeyPair keyPair, String subjectDN) throws OperatorCreationException, CertificateException, IOException
	{
		Provider bcProvider = new BouncyCastleProvider();
		Security.addProvider(bcProvider);

		long now = System.currentTimeMillis();
		Date startDate = new Date(now);

		X500Name dnName = new X500Name("CN=" + subjectDN);
		BigInteger certSerialNumber = new BigInteger(Long.toString(now)); // <-- Using the current timestamp as the certificate serial number

		Date endDate = Date.from(startDate.toInstant()
				.atZone(ZoneId.systemDefault())
				.toLocalDate()
				.plusYears(2)
				.atStartOfDay(ZoneId.systemDefault())
				.toInstant());

		String signatureAlgorithm = "SHA256WithRSA"; // <-- Use appropriate signature algorithm based on your keyPair algorithm.

		ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm).build(keyPair.getPrivate());

		JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(dnName, certSerialNumber, startDate, endDate, dnName, keyPair.getPublic());

		// Extensions --------------------------

		// Basic Constraints
		BasicConstraints basicConstraints = new BasicConstraints(true); // <-- true for CA, false for EndEntity

		certBuilder.addExtension(new ASN1ObjectIdentifier("2.5.29.19"), true, basicConstraints); // Basic Constraints is usually marked as critical.

		// -------------------------------------

		return new JcaX509CertificateConverter().setProvider(bcProvider).getCertificate(certBuilder.build(contentSigner));
	}



	/**
	 * Creates the hash value of the public key.
	 *
	 * @param publicKey of the certificate
	 * @return SubjectKeyIdentifier hash
	 */
	private static SubjectKeyIdentifier createSubjectKeyId(final PublicKey publicKey) throws OperatorCreationException {
		final SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
		final DigestCalculator digCalc =
				new BcDigestCalculatorProvider().get(new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1));

		return new X509ExtensionUtils(digCalc).createSubjectKeyIdentifier(publicKeyInfo);
	}

	/**
	 * Creates the hash value of the authority public key.
	 *
	 * @param publicKey of the authority certificate
	 * @return AuthorityKeyIdentifier hash

	 */
	private static AuthorityKeyIdentifier createAuthorityKeyId(final PublicKey publicKey)
			throws OperatorCreationException
	{
		final SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
		final DigestCalculator digCalc =
				new BcDigestCalculatorProvider().get(new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1));


		return new X509ExtensionUtils(digCalc).createAuthorityKeyIdentifier(publicKeyInfo);
	}

}