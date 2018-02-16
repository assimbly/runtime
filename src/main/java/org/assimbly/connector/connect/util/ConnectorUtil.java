package org.assimbly.connector.connect.util;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;

public final class ConnectorUtil {

	
    public static String convertStreamToString(InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
    
    public static String getContentFromJdbc(String connectorID, URI configurationUri){
    	
    	return "";
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

	
	public static boolean isValidURI(String name) throws Exception {
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
    
}
