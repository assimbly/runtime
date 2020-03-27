package org.assimbly.connector.connect.util;

import dev.jeka.core.api.depmanagement.*;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;

import org.apache.camel.CamelContext;

import static dev.jeka.core.api.depmanagement.JkJavaDepScopes.*;

public class DependencyUtil {

	public void resolveDependency(String groupId, String artifactId, String version) throws Exception {
    	
        String dependency = groupId + ":" + artifactId + ":" + version;
        
        JkDependencySet deps = JkDependencySet.of()
                .and(dependency)
                .withDefaultScopes(COMPILE_AND_RUNTIME);

        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepo.ofMavenCentral());
        List<Path> paths = resolver.resolve(deps, RUNTIME).getFiles().getEntries();
        
        for (Path path : paths) {
           loadDependency(path);
        }
	}   

	
    public void loadDependency(Path path) throws Exception {
       	
    	URL url = path.toUri().toURL();
    	
    	URLClassLoader classLoader = (URLClassLoader)ClassLoader.getSystemClassLoader();
    	Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
    	method.setAccessible(true);
    	method.invoke(classLoader, url);
       	
    }
    
}