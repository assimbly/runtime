package org.assimbly.dil.transpiler.ssl;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.SSLContextParametersAware;
import org.apache.camel.support.jsse.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;


public class SSLConfiguration {

	protected Logger log = LoggerFactory.getLogger(getClass());
	
	private static final String SERVER_IDENTITY_FILE = "server-identity.p12";

	public void setUseGlobalSslContextParameters(CamelContext context,String[] sslComponentNames) {
		for (String sslComponent : sslComponentNames) {
			setUseGlobalSslContextParameter(context, sslComponent);
		}
	}

	public void setUseGlobalSslContextParameter(CamelContext context, String componentName) {

		Component component = context.getComponent(componentName);

		if (component instanceof SSLContextParametersAware sslAware) {
			sslAware.setUseGlobalSslContextParameters(true);
		}

	}

	public SSLContextParameters createSSLContextParameters(String keystorePath, String keystorePassword, String truststorePath, String truststorePassword)  {

		SSLContextParameters sslContextParameters = new SSLContextParameters();

		//create keystore
		if(keystorePath!= null && keystorePassword != null){

			createKeystore(keystorePath);

			//The keystore is reloaded when the file changes (no restart needed after adding a certificate)
			sslContextParameters.setKeyManagers(new ReloadingKeyManagersParameters(keystorePath, keystorePassword));

		}

		//create truststore
		if(truststorePath!= null && truststorePassword != null){

			createKeystore(truststorePath);

			//Server certificates are validated against the truststore, client certificates are validated per flow (MutualTlsHandler)
			TrustManagersParameters tmp = new TrustManagersParameters();
			tmp.setTrustManager(new InboundClientAcceptingTrustManager(truststorePath, truststorePassword));

			sslContextParameters.setTrustManagers(tmp);

		}

		//Request (but don't require) a client certificate on inbound connections, so HTTPS sources can use mutual TLS
		SSLContextServerParameters serverParameters = new SSLContextServerParameters();
		serverParameters.setClientAuthentication(ClientAuthentication.WANT.name());
		sslContextParameters.setServerParameters(serverParameters);

		return sslContextParameters;
	}

	//This method assumes an empty server-identity.p12 is available as resource on the classpath
	public void createKeystore(String keystorePath){

		File file = new File(keystorePath);
		Path path = file.toPath();

		if(!file.exists()){
			try {
				boolean newFile = file.createNewFile();

				if(newFile){
					ClassLoader classloader = Thread.currentThread().getContextClassLoader();
					InputStream is = classloader.getResourceAsStream(SERVER_IDENTITY_FILE);
                    assert is != null;
                    Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING);
					is.close();
				}

			} catch (IOException e) {
				log.error("Create keystore for certificates failed (ssl/tls)",e);
			}
		}

	}

	public KeyStoreParameters createKeystoreParameters(String keystorePath, String keystorePassword){
		KeyStoreParameters keystoreParameters = new KeyStoreParameters();
		keystoreParameters.setResource("file:" + keystorePath);
		keystoreParameters.setPassword(keystorePassword);

		return keystoreParameters;
	}

	public SSLContextParameters createRuntimeSSLContext(String keystoreResource, String keystorePassword,
														String truststorePath, String truststorePassword) throws Exception {

		// Load the keystore from resource
		KeyStore ks = KeyStore.getInstance("PKCS12");
		try (ByteArrayInputStream is = new ByteArrayInputStream(readResource(keystoreResource))) {
			ks.load(is, keystorePassword.toCharArray());
		}

		KeyStoreParameters keyStoreParameters = new KeyStoreParameters();
		keyStoreParameters.setKeyStore(ks); // in-memory keystore
		keyStoreParameters.setPassword(keystorePassword);


		KeyManagersParameters keyManagers = new KeyManagersParameters();
		keyManagers.setKeyStore(keyStoreParameters);
		keyManagers.setKeyPassword(keystorePassword);

		KeyStore ts = createRuntimeTruststore(ks, truststorePath, truststorePassword);

		KeyStoreParameters trustStoreParameters = new KeyStoreParameters();
		trustStoreParameters.setKeyStore(ts); // in-memory truststore
		trustStoreParameters.setPassword(truststorePassword);

		TrustManagersParameters trustManagers = new TrustManagersParameters();
		trustManagers.setKeyStore(trustStoreParameters);

		SSLContextParameters sslContextParameters = new SSLContextParameters();

		sslContextParameters.setKeyManagers(keyManagers);
		sslContextParameters.setTrustManagers(trustManagers);

		return sslContextParameters;

	}

	// Builds an in-memory truststore that combines the JDK default trusted CAs (cacerts),
	// the outbound truststore file and the CA certificates from the chain of the client keystore
	private KeyStore createRuntimeTruststore(KeyStore keystore, String truststorePath, String truststorePassword) throws Exception {

		KeyStore ts = KeyStore.getInstance("PKCS12");
		ts.load(null, null);

		// 1. JDK default trusted CAs
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init((KeyStore) null);
		for (TrustManager trustManager : tmf.getTrustManagers()) {
			if (trustManager instanceof X509TrustManager x509TrustManager) {
				X509Certificate[] acceptedIssuers = x509TrustManager.getAcceptedIssuers();
				for (int i = 0; i < acceptedIssuers.length; i++) {
					ts.setCertificateEntry("jdk-" + i, acceptedIssuers[i]);
				}
			}
		}

		// 2. Outbound truststore
		KeyStore outboundTruststore = KeyStore.getInstance("PKCS12");
		try (InputStream is = Files.newInputStream(Paths.get(truststorePath))) {
			outboundTruststore.load(is, truststorePassword.toCharArray());
		}
		for (String alias : Collections.list(outboundTruststore.aliases())) {
			Certificate certificate = outboundTruststore.getCertificate(alias);
			if (certificate != null) {
				ts.setCertificateEntry("outbound-" + alias, certificate);
			}
		}

		// 3. CA certificates from the client keystore chain (skipping the client certificate itself)
		for (String alias : Collections.list(keystore.aliases())) {
			Certificate[] chain = keystore.getCertificateChain(alias);
			if (chain != null) {
				for (int i = 1; i < chain.length; i++) {
					ts.setCertificateEntry("client-ca-" + alias + "-" + i, chain[i]);
				}
			}
		}

		return ts;

	}

	// Loads all certificates from a resource (url, data url or base64): a PKCS12 keystore (.p12/.pfx)
	// or one or more PEM/DER certificates (.cer/.crt/.pem)
	public List<X509Certificate> loadCertificates(String resource, String password) throws Exception {

		byte[] bytes = readResource(resource);
		List<X509Certificate> certificates = new ArrayList<>();
		IOException keystoreException = null;

		if (password != null) {
			try (ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
				KeyStore ks = KeyStore.getInstance("PKCS12");
				ks.load(is, password.toCharArray());
				for (String alias : Collections.list(ks.aliases())) {
					Certificate[] chain = ks.getCertificateChain(alias);
					if (chain != null) {
						for (Certificate certificate : chain) {
							certificates.add((X509Certificate) certificate);
						}
					} else if (ks.getCertificate(alias) instanceof X509Certificate certificate) {
						certificates.add(certificate);
					}
				}
				return certificates;
			} catch (IOException e) {
				log.debug("Resource is not a PKCS12 keystore, trying to read it as certificate(s): {}", e.getMessage());
				keystoreException = e;
			}
		}

		try (ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
			for (Certificate certificate : CertificateFactory.getInstance("X.509").generateCertificates(is)) {
				certificates.add((X509Certificate) certificate);
			}
		} catch (CertificateException e) {
			// Neither a keystore nor certificates: report the keystore error (for example a wrong password)
			throw keystoreException != null ? keystoreException : e;
		}

		return certificates;

	}

	// Creates a trust manager that trusts the given certificates (and every certificate issued by them)
	public X509TrustManager createTrustManager(List<X509Certificate> certificates) throws Exception {

		KeyStore ts = KeyStore.getInstance("PKCS12");
		ts.load(null, null);
		for (int i = 0; i < certificates.size(); i++) {
			ts.setCertificateEntry("trusted-" + i, certificates.get(i));
		}

		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(ts);
		for (TrustManager trustManager : tmf.getTrustManagers()) {
			if (trustManager instanceof X509TrustManager x509TrustManager) {
				return x509TrustManager;
			}
		}

		throw new IllegalStateException("No X509TrustManager available");

	}

	// Reads a resource from an url or from a (data url) base64 string
	private byte[] readResource(String resource) throws IOException {

		if (resource.startsWith("http")) {
			try (InputStream is = URI.create(resource).toURL().openStream()) {
				return is.readAllBytes();
			}
		}

		String cleanBase64 = resource;

		// Strip the Data URL prefix if it exists
		if (cleanBase64.contains(",")) {
			cleanBase64 = cleanBase64.substring(cleanBase64.indexOf(",") + 1);
		}

		return Base64.getMimeDecoder().decode(cleanBase64.trim());

	}

}