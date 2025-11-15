package org.assimbly.dil.transpiler.ssl;

import org.apache.camel.CamelContext;
import org.apache.camel.SSLContextParametersAware;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyStore;


public class SSLConfiguration {

	protected Logger log = LoggerFactory.getLogger(getClass());

	public void setUseGlobalSslContextParameters(CamelContext context,String[] sslComponentNames) {
		for (String sslComponent : sslComponentNames) {
			setUseGlobalSslContextParameter(context, sslComponent);
		}
	}


	public void setUseGlobalSslContextParameter(CamelContext context,String componentName) {
		if(context.getComponent(componentName)!=null){
			((SSLContextParametersAware) context.getComponent(componentName)).setUseGlobalSslContextParameters(true);
		}else{
            log.warn("Can't set SSL for component {}. Component is not loaded on the classpath", componentName);
		}
	}

	public SSLContextParameters createSSLContextParameters(String keystorePath, String keystorePassword, String truststorePath, String truststorePassword)  {

		SSLContextParameters sslContextParameters = new SSLContextParameters();

		//create keystore
		if(keystorePath!= null && keystorePassword != null){

			createKeystore(keystorePath);

			KeyStoreParameters keystoreParameters = createKeystoreParameters(keystorePath, keystorePassword);

			KeyManagersParameters kmp = new KeyManagersParameters();
			kmp.setKeyPassword(keystorePassword);
			kmp.setKeyStore(keystoreParameters);

			sslContextParameters.setKeyManagers(kmp);

		}

		//create truststore
		if(truststorePath!= null && truststorePassword != null){

			createKeystore(truststorePath);

			KeyStoreParameters truststoreParameters = createKeystoreParameters(truststorePath, truststorePassword);

			TrustManagersParameters tmp = new TrustManagersParameters();
			tmp.setKeyStore(truststoreParameters);

			sslContextParameters.setTrustManagers(tmp);

		}

		return sslContextParameters;
	}

	//This method assumes an empty keystore.jks is available as resource on the classpath
	public void createKeystore(String keystorePath){

		File file = new File(keystorePath);
		Path path = file.toPath();

		if(!file.exists()){
			try {
				boolean newFile = file.createNewFile();

				if(newFile){
					ClassLoader classloader = Thread.currentThread().getContextClassLoader();
					InputStream is = classloader.getResourceAsStream("keystore.jks");
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
		KeyStore ks = KeyStore.getInstance("JKS");
		try (InputStream is = new URL(keystoreResource).openStream()) {
			ks.load(is, keystorePassword.toCharArray());
		}

		KeyStoreParameters keyStoreParameters = new KeyStoreParameters();
		keyStoreParameters.setKeyStore(ks); // in-memory keystore
		keyStoreParameters.setPassword(keystorePassword);

		KeyManagersParameters keyManagers = new KeyManagersParameters();
		keyManagers.setKeyStore(keyStoreParameters);
		keyManagers.setKeyPassword(keystorePassword);

		// Load truststore from file
		KeyStore ts = KeyStore.getInstance("JKS");
		try (InputStream is = new FileInputStream(truststorePath)) {
			ts.load(is, truststorePassword.toCharArray());
		}

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

}