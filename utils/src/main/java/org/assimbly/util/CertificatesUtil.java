package org.assimbly.util;

import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.*;
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

import javax.net.ssl.SSLSession;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

public final class CertificatesUtil {

	private static final Logger log = LoggerFactory.getLogger("org.assimbly.util.CertificatesUtil");
	
    public static final String PEER_CERTIFICATES = "PEER_CERTIFICATES";

	public Certificate[] downloadCertificates(String url) throws Exception {

		System.out.println("Start downloading certificates (url=" + url + ")");

		Certificate[] peercertificates;

        // create http response certificate interceptor
        HttpResponseInterceptor certificateInterceptor = (httpResponse, context) -> {
            ManagedHttpClientConnection routedConnection = (ManagedHttpClientConnection)context.getAttribute(HttpCoreContext.HTTP_CONNECTION);
            SSLSession sslSession = routedConnection.getSSLSession();
            if (sslSession != null) {

                // get the server certificates from the SSLSession
                java.security.cert.Certificate[] certificates = sslSession.getPeerCertificates();

                // add the certificates to the context, where we can later grab it from
                context.setAttribute(PEER_CERTIFICATES, certificates);
            }
        };

        try (CloseableHttpClient httpClient = HttpClients
                .custom()
                .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .addInterceptorLast(certificateInterceptor)
                .build()) {

			// make HTTP GET request to resource server
			HttpGet httpget = new HttpGet(url);

			log.info("Executing request {}", httpget.getRequestLine());

			// create http context where the certificate will be added
			HttpContext context = new BasicHttpContext();
			httpClient.execute(httpget, context);

			// obtain the server certificates from the context
			peercertificates = (Certificate[]) context.getAttribute(PEER_CERTIFICATES);

			if (peercertificates != null) {

				// loop over certificates and print meta-data
				for (Certificate certificate : peercertificates) {
					X509Certificate real = (X509Certificate) certificate;
					System.out.println("----------------------------------------");
					System.out.println("Type: " + real.getType());
					System.out.println("Signing Algorithm: " + real.getSigAlgName());
					System.out.println("IssuerDN Principal: " + real.getIssuerX500Principal());
					System.out.println("SubjectDN Principal: " + real.getSubjectX500Principal());
					System.out.println("Not After: " + DateUtils.formatDate(real.getNotAfter(), "dd-MM-yyyy"));
					System.out.println("Not Before: " + DateUtils.formatDate(real.getNotBefore(), "dd-MM-yyyy"));
				}

			} else {
				log.error("Certificates not found. URL: {})", url);
			}

        }

        return peercertificates;
		
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
            System.out.println("----------------------------------------");
            System.out.println("Type: " + real.getType());
            System.out.println("Signing Algorithm: " + real.getSigAlgName());
            System.out.println("IssuerDN Principal: " + real.getIssuerX500Principal());
            System.out.println("SubjectDN Principal: " + real.getSubjectX500Principal());
            System.out.println("Not After: " + DateUtils.formatDate(real.getNotAfter(), "dd-MM-yyyy"));
            System.out.println("Not Before: " + DateUtils.formatDate(real.getNotBefore(), "dd-MM-yyyy"));

            keystore.setCertificateEntry(certificateName, certificate);
            System.out.println("original alias:" + certificateName);
            System.out.println("cert alias" + keystore.getCertificateAlias(certificate));

			// Save the new keystore contents
			storeKeystore(keyStorePath,keystorePassword,keystore);

		}catch (Exception e) {
			log.error("Import certificate for keystore {} with name {} failed", keyStorePath, certificateName, e);
		}

		return certificateName;

	}
	
	public Map<String,Certificate> importCertificates(String keyStorePath, String keystorePassword, Certificate[] certificates) {

        System.out.println("Importing certificates");
        Map<String,Certificate> certificateMap = new HashMap<>();

    	try {
    		//load keystore
			KeyStore keystore = loadKeyStore(keyStorePath, keystorePassword, null);

	        // Add the certificate to the store
            for (Certificate certificate : certificates){            	
                X509Certificate real = (X509Certificate) certificate;
                System.out.println("----------------------------------------");
                System.out.println("Type: " + real.getType());
                System.out.println("Signing Algorithm: " + real.getSigAlgName());
                System.out.println("IssuerDN Principal: " + real.getIssuerX500Principal());
                System.out.println("SubjectDN Principal: " + real.getSubjectX500Principal());
                System.out.println("Not After: " + DateUtils.formatDate(real.getNotAfter(), "dd-MM-yyyy"));
                System.out.println("Not Before: " + DateUtils.formatDate(real.getNotBefore(), "dd-MM-yyyy"));
                String alias = UUID.randomUUID().toString();
                certificateMap.put(alias, certificate);
                keystore.setCertificateEntry(alias, certificate);
                System.out.println("original alias:" + alias);
                System.out.println("cert alias" + keystore.getCertificateAlias(certificate));
                
            }
			
	        // Save the new keystore contents
			storeKeystore(keyStorePath,keystorePassword,keystore);
	        
		}catch (Exception e) {
			log.error("Import certificates for keystore {} failed", keyStorePath, e);
		}

		return certificateMap;
	}

	public Map<String,Certificate> importP12Certificate(String keyStorePath, String keystorePassword, String p12Certificate, String p12Password) throws Exception {

		KeyStore jksStore = loadKeyStore(keyStorePath, keystorePassword, null);

		KeyStore p12Store = loadKeystoreFromString(p12Certificate, p12Password, "pkcs12");

		Enumeration<String> aliases = p12Store.aliases();

		Map<String,Certificate> certificateMap = new HashMap<>();


		while (aliases.hasMoreElements()) {

			String alias = aliases.nextElement();

			if (p12Store.isKeyEntry(alias)) {
				System.out.println("Adding key for alias " + alias);
				Key key = p12Store.getKey(alias, p12Password.toCharArray());

				Certificate[] chain = p12Store.getCertificateChain(alias);

				jksStore.setKeyEntry(alias, key, keystorePassword.toCharArray(), chain);

				for (Certificate certificate : chain){
					certificateMap.put(alias, certificate);
				}

			}
		}

		storeKeystore(keyStorePath,keystorePassword,jksStore);

		return certificateMap;

	}

	public void deleteCertificate(String keyStorePath, String keystorePassword) {

    	try {
    		//load keystore
			KeyStore keystore = loadKeyStore(keyStorePath, keystorePassword,null);

			// Save the new keystore contents
			storeKeystore(keyStorePath,keystorePassword,keystore);
	        
		}catch (Exception e) {
			log.error("Delete certificate for keystore {} failed", keyStorePath, e);
		}

	}



	public static String convertStringToBinary(String input) {
		StringBuilder bString = new StringBuilder();

		for (int i = 0; i < input.length(); i++) {
			StringBuilder temp = new StringBuilder(Integer.toBinaryString(input.charAt(i)));

			// Ensure each binary value is 8 bits long (pad with leading zeros)
			while (temp.length() < 8) {
				temp.insert(0, "0");
			}

			bString.append(temp).append(" ");
		}

		return bString.toString();
	}

	public static String convertX509CertificateToPem(X509Certificate certificate) throws CertificateEncodingException {

		org.apache.commons.codec.binary.Base64 encoder = new org.apache.commons.codec.binary.Base64(64);
		byte[] derCertificate = certificate.getEncoded();
		String pemCertificate = new String(encoder.encode(derCertificate), StandardCharsets.UTF_8);

		return "-----BEGIN CERTIFICATE-----\n" + pemCertificate + "-----END CERTIFICATE-----";

	}

	public static X509Certificate convertPemToX509Certificate(String pemCertificate) throws CertificateException {

		org.apache.commons.codec.binary.Base64 decoder = new org.apache.commons.codec.binary.Base64(64);
		CertificateFactory cf = CertificateFactory.getInstance("X509");
		X509Certificate certificate = null;

		try {
			if (pemCertificate != null && !pemCertificate.trim().isEmpty()) {

				Pattern parse = Pattern.compile("(?m)(?s)^---*BEGIN.*---*$(.*)^---*END.*---*$.*");
				pemCertificate = parse.matcher(pemCertificate).replaceFirst("$1");

				byte[] derCertificate = decoder.decode(pemCertificate);

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
			FileInputStream is = new FileInputStream(file);
			keystore.load(is, keystorePassword.toCharArray());
			is.close();
		} else {

			createKeystoreFile(file);
			keystore.load(null, keystorePassword.toCharArray());

			FileOutputStream os = new FileOutputStream(file);
			keystore.store(os, keystorePassword.toCharArray());
			os.close();
		}

		return keystore;
	}

	public KeyStore loadKeystoreFromString(String keyStoreString, String keystorePassword, String keystoreType) throws Exception {

		Base64.Decoder decoder = Base64.getDecoder();
		byte[] decodedByte = decoder.decode(keyStoreString);


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
		FileOutputStream out = new FileOutputStream(file);
		keystore.store(out, keystorePassword.toCharArray());
		out.close();

	}

	public static void createKeystoreFile(File file) throws IOException {

		File securityPath = file.getParentFile();
		Path path = file.toPath();

		if (!securityPath.exists()) {
			boolean dirsCreated = securityPath.mkdirs();
			if(!dirsCreated){
				System.out.println("Keystore Directory: " + securityPath.getAbsolutePath() + " couldn't be created.");
			}
		}

		boolean newFile = file.createNewFile();
		if(!newFile){
			System.out.println("Keystore File: " + file.getAbsolutePath() + " couldn't be created.");
		}

		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		InputStream is = classloader.getResourceAsStream("keystore.jks");
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

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(startDate);
		calendar.add(Calendar.YEAR, 2); // <-- 2 Yr validity

		Date endDate = calendar.getTime();

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


