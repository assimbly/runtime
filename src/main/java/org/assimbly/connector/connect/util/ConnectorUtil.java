package org.assimbly.connector.connect.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.net.ssl.SSLSession;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.assimbly.docconverter.DocConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.*;
import java.net.URL;
import org.xml.sax.SAXException;
import java.io.IOException;



public final class ConnectorUtil {
	
	private static Logger logger = LoggerFactory.getLogger("org.assimbly.connector.util.ConnectorUtil");
	
	public static boolean isValidUri(String name) throws Exception {
		try {
		    URI uri = new URI(name);
		    
		    if(uri.getScheme() == null){
		    	return false;
		    }else{ 
		    	return true;
		    }
		    
		} catch (URISyntaxException e) {
			return false;
		}
		
	}	
    
	public static String isValidXML(URL schemaFile, String xml) {
		
		String result = null;

		Source xmlFile = new StreamSource(new StringReader(xml));
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		try {
		  Schema schema = schemaFactory.newSchema(schemaFile);
		  Validator validator = schema.newValidator();
		  validator.validate(xmlFile);
		  result = "xml is valid";
		} catch (SAXException e) {
		  result = "xml is NOT valid. Reason:" + e;
		} catch (IOException e) {}
		
		return result;
		
	}
	
	public static List<String> getXMLParameters(XMLConfiguration conf, String prefix) throws ConfigurationException {
	  	   
	  	   Iterator<String> keys;
	  	   
	  	   if(prefix == null || prefix.isEmpty()){
	  		 keys = conf.getKeys();
	  	   }else{
	  		 keys = conf.getKeys(prefix);
	  	   }
	  	     
	  	   List<String> keyList = new ArrayList<String>();
	    
	  	   while(keys.hasNext()){
	  		   keyList.add(keys.next());
	  	   }

	  	   return keyList;
	}

	public static void printTreemap(TreeMap<String, String> treeMap) throws Exception {

	    Map<String, String> id = treeMap.entrySet()
	    	      .stream()
	    	      .filter(map -> map.getKey().equals("id"))
	    	      .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));

		
	    Map<String, String> flow = treeMap.entrySet()
	    	      .stream()
	    	      .filter(map -> map.getKey().startsWith("flow"))
	    	      .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));

	    Map<String, String> from = treeMap.entrySet()
	    	      .stream()
	    	      .filter(map -> map.getKey().startsWith("from"))
	    	      .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
	    
	    Map<String, String> to = treeMap.entrySet()
	    	      .stream()
	    	      .filter(map -> map.getKey().startsWith("to"))
	    	      .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
	    
	    Map<String, String> error = treeMap.entrySet()
	    	      .stream()
	    	      .filter(map -> map.getKey().startsWith("error"))
	    	      .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));

	    Map<String, String> header = treeMap.entrySet()
	    	      .stream()
	    	      .filter(map -> map.getKey().startsWith("header"))
	    	      .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
	    
	    Map<String, String> service = treeMap.entrySet()
	    	      .stream()
	    	      .filter(map -> map.getKey().startsWith("service"))
	    	      .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
	    
		System.out.println("");
		System.out.println("FLOW CONFIGURATION");
		System.out.println("-----------------------------------------------------------\n");

		for(Map.Entry<String,String> entry : id.entrySet()) {

			  String key = entry.getKey();
			  String value = entry.getValue();
			  System.out.printf("%-30s %s\n", key + ":", value);  
			  
		}

		for(Map.Entry<String,String> entry : flow.entrySet()) {

			  String key = entry.getKey();
			  String value = entry.getValue();
			  System.out.printf("%-30s %s\n", key + ":", value);  
			  
		}

		System.out.println("\nENDPOINTS\n");
		
		for(Map.Entry<String,String> entry : from.entrySet()) {

			  String key = entry.getKey();
			  String value = entry.getValue();
			  System.out.printf("%-30s %s\n", key + ":", value);  
			  
		}
		
		for(Map.Entry<String,String> entry : to.entrySet()) {

			  String key = entry.getKey();
			  String value = entry.getValue();
			  System.out.printf("%-30s %s\n", key + ":", value);  
			  
		}

		for(Map.Entry<String,String> entry : error.entrySet()) {

			  String key = entry.getKey();
			  String value = entry.getValue();
			  System.out.printf("%-30s %s\n", key + ":", value);  
			  
		}

		if(!header.isEmpty()) {
			
			System.out.println("\nHEADERS\n");
		
			for(Map.Entry<String,String> entry : header.entrySet()) {
	
				  String key = entry.getKey();
				  String value = entry.getValue();
				  if(key.contains("password"))
					  System.out.printf("%-30s %s\n", key + ":", "***********");
				  else {
					  System.out.printf("%-30s %s\n", key + ":", value);  
				  }
				  
			}
		}

		if(!service.isEmpty()) {

			System.out.println("\nSERVICES\n");
			
			for(Map.Entry<String,String> entry : service.entrySet()) {
	
				  String key = entry.getKey();
				  String value = entry.getValue();
				  if(key.contains("password"))
					  System.out.printf("%-30s %s\n", key + ":", "***********");
				  else {
					  System.out.printf("%-30s %s\n", key + ":", value);  
				  }
				  
			}
		}
		
		System.out.println("-----------------------------------------------------------\n");

	}

}
