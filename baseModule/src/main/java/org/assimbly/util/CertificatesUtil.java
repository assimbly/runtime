package org.assimbly.util;

import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.*;
import java.util.regex.Pattern;

import javax.net.ssl.SSLSession;

import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.methods.CloseableHttpResponse;
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
//import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CertificatesUtil {
	
    public static final String PEER_CERTIFICATES = "PEER_CERTIFICATES";
	private static Logger logger = LoggerFactory.getLogger("org.assimbly.connector.util.ConnectorUtil");
		
	public Certificate[] downloadCertificates(String url) throws Exception {

		Certificate[] peercertificates = null;

        // create http response certificate interceptor
        HttpResponseInterceptor certificateInterceptor = (httpResponse, context) -> {
            ManagedHttpClientConnection routedConnection = (ManagedHttpClientConnection)context.getAttribute(HttpCoreContext.HTTP_CONNECTION);
            SSLSession sslSession = routedConnection.getSSLSession();
            if (sslSession != null) {

                // get the server certificates from the {@Link SSLSession}
                java.security.cert.Certificate[] certificates = sslSession.getPeerCertificates();

                // add the certificates to the context, where we can later grab it from
                context.setAttribute(PEER_CERTIFICATES, certificates);
            }
        };

		CloseableHttpClient httpClient = HttpClients
				.custom()
				.setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
				.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
				.addInterceptorLast(certificateInterceptor)
				.build();

		try {

			// make HTTP GET request to resource server
            HttpGet httpget = new HttpGet(url);

			// create http context where the certificate will be added
            HttpContext context = new BasicHttpContext();

			CloseableHttpResponse x = httpClient.execute(httpget, context);

			// obtain the server certificates from the context
            peercertificates = (Certificate[])context.getAttribute(PEER_CERTIFICATES);

			if(peercertificates!=null){
				// loop over certificates and print meta-data
				for (Certificate certificate : peercertificates){
					X509Certificate real = (X509Certificate) certificate;
					System.out.println("----------------------------------------");
					System.out.println("Type: " + real.getType());
					System.out.println("Signing Algorithm: " + real.getSigAlgName());
					System.out.println("IssuerDN Principal: " + real.getIssuerX500Principal());
					System.out.println("SubjectDN Principal: " + real.getSubjectX500Principal());
					System.out.println("Not After: " + DateUtils.formatDate(real.getNotAfter(), "dd-MM-yyyy"));
					System.out.println("Not Before: " + DateUtils.formatDate(real.getNotBefore(), "dd-MM-yyyy"));
				}

			}else{
				System.out.println("No certificates found (url=" + url + ")");
			}

        } finally {
            // close httpclient
            httpClient.close();
        }

        return peercertificates;
		
	}


	public Certificate getCertificate(String keyStorePath, String keystorePassword, String certificateName) {

    	try {
    		//load keystore
    		File trustStore = new File(keyStorePath);
    		InputStream is = new FileInputStream(trustStore);
    		KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
	        keystore.load(is, keystorePassword.toCharArray());
	        Certificate certificate = keystore.getCertificate(certificateName);
	        is.close();
			
	        // Save the new keystore contents
			FileOutputStream out = new FileOutputStream(trustStore);
	        keystore.store(out, keystorePassword.toCharArray());
	        out.close();
	    
	        return certificate;
	        
		}catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;   	

	}
	

	public String importCertificate(String keyStorePath, String keystorePassword, String certificateName, Certificate certificate) {

    	try {

			//load keystore
    		File trustStore = new File(keyStorePath);
    		InputStream is = new FileInputStream(trustStore);
    		KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
	        keystore.load(is, keystorePassword.toCharArray());
	        is.close();

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
			FileOutputStream out = new FileOutputStream(trustStore);
	        keystore.store(out, keystorePassword.toCharArray());
	        out.close();

		}catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	return certificateName;

	}
	
	public Map<String,Certificate> importCertificates(String keyStorePath, String keystorePassword, Certificate[] certificates) {

        System.out.println("Importing certificates");
        Map<String,Certificate> certificateMap = new HashMap<String,Certificate>();

    	try {
    		//load keystore
    		File trustStore = new File(keyStorePath);
    		InputStream is = new FileInputStream(trustStore);
    		KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
	        keystore.load(is, keystorePassword.toCharArray());
	        is.close();
	
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
			FileOutputStream out = new FileOutputStream(trustStore);
	        keystore.store(out, keystorePassword.toCharArray());
	        out.close();
	        
		}catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

    	return certificateMap;
	}

	public Map<String,Certificate> importP12Certificate(String keyStorePath, String keystorePassword, String p12Certificate, String p12Password) throws Exception {

		KeyStore p12Store = loadKeystoreFromString(p12Certificate, p12Password, "pkcs12");

		KeyStore jksStore = loadKeystore(keyStorePath, keystorePassword, "jks");

		Enumeration aliases = p12Store.aliases();

		Map<String,Certificate> certificateMap = new HashMap<String,Certificate>();


		while (aliases.hasMoreElements()) {

			String alias = (String)aliases.nextElement();

			if (p12Store.isKeyEntry(alias)) {
				System.out.println("Adding key for alias " + alias);
				Key key = p12Store.getKey(alias, p12Password.toCharArray());

				Certificate[] chain = p12Store.getCertificateChain(alias);

				jksStore.setKeyEntry(alias, key, p12Password.toCharArray(), chain);

				for (Certificate certificate : chain){
					certificateMap.put(alias, certificate);
				}

			}
		}

		storeKeystore(jksStore,keyStorePath,keystorePassword);

		return certificateMap;

	}

	public void deleteCertificate(String keyStorePath, String keystorePassword, String certificateName) {

    	try {
    		//load keystore
    		File trustStore = new File(keyStorePath);
    		InputStream is = new FileInputStream(trustStore);
    		KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
	        String password = keystorePassword;
	        keystore.load(is, password.toCharArray());
	        keystore.deleteEntry(certificateName);
	        is.close();
			
	        // Save the new keystore contents
			FileOutputStream out = new FileOutputStream(trustStore);
	        keystore.store(out, password.toCharArray());
	        out.close();
	        
		}catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}



	public static String convertStringToBinary(String input) {

		String bString="";
		String temp="";
		for(int i=0;i<input.length();i++)
		{
			temp=Integer.toBinaryString(input.charAt(i));
			for(int j=temp.length();j<8;j++)
			{
				temp="0"+temp;
			}
			bString+=temp+" ";
		}

		System.out.println(bString);
		return bString;

	}

	public static String convertX509CertificateToPem(X509Certificate certificate) throws CertificateEncodingException {

		org.apache.commons.codec.binary.Base64 encoder = new org.apache.commons.codec.binary.Base64(64);
		byte[] derCertificate = certificate.getEncoded();
		String pemCertificate = new String(encoder.encode(derCertificate));

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


	private KeyStore loadKeystore(String keyStoreFile, String keystorePassword, String keystoreType) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {

		File file = new File(keyStoreFile);
		InputStream inputStream = new FileInputStream(file);

		KeyStore keystore = null;

		if(keystore == null){
			keystore = KeyStore.getInstance(KeyStore.getDefaultType());
		}else{
			keystore = KeyStore.getInstance(keystoreType);
		}

		keystore.load(inputStream, keystorePassword.toCharArray());

		return keystore;
	}

	private KeyStore loadKeystoreFromString(String keyStoreString, String keystorePassword, String keystoreType) throws Exception {

		Base64.Decoder decoder = Base64.getDecoder();
		byte[] decodedByte = decoder.decode(keyStoreString.split(",")[1]);


		InputStream inputStream = new ByteArrayInputStream(decodedByte);

		KeyStore keystore = null;

		if(keystore == null){
			keystore = KeyStore.getInstance(KeyStore.getDefaultType());
		}else{
			keystore = KeyStore.getInstance(keystoreType);
		}

		keystore.load(inputStream, keystorePassword.toCharArray());

		return keystore;
	}

	private void storeKeystore(KeyStore keystore, String keyStoreFile, String keystorePassword) throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {

		File file = new File(keyStoreFile);
		FileOutputStream out = new FileOutputStream(file);
		keystore.store(out, keystorePassword.toCharArray());
		out.close();
	}

}


