package org.assimbly.util;

import dev.jeka.core.api.depmanagement.*;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public enum CompiledDependency {

        AMQP("amqp"),
        DIRECT("direct"),
        FILE("file"),
        FTP("ftp"),
        HTTP("http"),
        JMS("jms"),
        JETTY("jetty"),
        KAFKA("kafka"),
        LOG("log"),
        MAIL("mail"),
        MILO("milo"),
        NETTY("netty"),
        SEDA("seda"),
        SJMS("sjms"),
        SJMS2("sjms2"),
        SQL("sql"),
        SLACK("slack"),
        SPRING("spring"),
        STREAM("stream"),
        STUB("stub"),
        VM("vm"),
        ;

        private static Map<String, CompiledDependency> BY_LABEL = new HashMap<>();

        static {
            for (CompiledDependency cd : values()) {
                BY_LABEL.put(cd.label, cd);
            }
        }

        public final String label;

        CompiledDependency(final String label) {
            this.label = label;
        }

        public static boolean hasCompiledDependency(String label){
            return BY_LABEL.containsKey(label);
        }
    }
    
}