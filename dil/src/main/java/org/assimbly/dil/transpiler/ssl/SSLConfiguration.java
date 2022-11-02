package org.assimbly.dil.transpiler.ssl;

import org.apache.camel.*;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;


public class SSLConfiguration {

	protected Logger log = LoggerFactory.getLogger(getClass());

	public void setUseGlobalSslContextParameters(CamelContext context,String componentName) throws Exception {
		((SSLContextParametersAware) context.getComponent(componentName)).setUseGlobalSslContextParameters(true);
	}

	public SSLContextParameters createSSLContextParameters(String keystorePath, String keystorePassword, String truststorePath, String truststorePassword) throws GeneralSecurityException, IOException {

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
				file.createNewFile();

				ClassLoader classloader = Thread.currentThread().getContextClassLoader();
				InputStream is = classloader.getResourceAsStream("keystore.jks");
				Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING);
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public KeyStoreParameters createKeystoreParameters(String keystorePath, String keystorePassword){

		KeyStoreParameters keystoreParameters = new KeyStoreParameters();
		keystoreParameters.setResource(keystorePath);
		keystoreParameters.setPassword(keystorePassword);

		return keystoreParameters;
	}

}