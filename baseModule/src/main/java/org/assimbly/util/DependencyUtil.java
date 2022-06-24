package org.assimbly.util;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.resolution.*;

import java.io.FileInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

//import static dev.jeka.core.api.depmanagement.JkJavaDepScopes.*;

public class DependencyUtil {

	public List<Path> resolveDependency(String groupId, String artifactId, String version) throws Exception {
    	
        String dependency = groupId + ":" + artifactId + ":" + version;
        
        JkDependencySet deps = JkDependencySet.of()
                .and(dependency);

        JkDependencyResolver<Void> resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        List<Path> paths = resolver.resolve(deps).getFiles().getEntries();

        return paths;

	}

    public List<Class> loadDependency(List<Path> paths) throws Exception {

        List<Class> classes = new ArrayList<>();

        for(Path path: paths){

            URL url = path.toUri().toURL();
            URLClassLoader child = new URLClassLoader(new URL[] {url}, this.getClass().getClassLoader());

            ArrayList<String> classNames = getClassNamesFromJar(path.toString());

            for (String className : classNames) {
                Class classToLoad = Class.forName(className, true, child);
                classes.add(classToLoad);
            }
        }

        return classes;

    }


    // Returns an arraylist of class names in a JarInputStream
    private ArrayList<String> getClassNamesFromJar(JarInputStream jarFile) throws Exception {
        ArrayList<String> classNames = new ArrayList<>();
        try {
            //JarInputStream jarFile = new JarInputStream(jarFileStream);
            JarEntry jar;

            //Iterate through the contents of the jar file
            while (true) {
                jar = jarFile.getNextJarEntry();
                if (jar == null) {
                    break;
                }
                //Pick file that has the extension of .class
                if ((jar.getName().endsWith(".class"))) {
                    String className = jar.getName().replaceAll("/", "\\.");
                    String myClass = className.substring(0, className.lastIndexOf('.'));
                    classNames.add(myClass);
                }
            }
        } catch (Exception e) {
            throw new Exception("Error while getting class names from jar", e);
        }
        return classNames;
    }

    // Returns an arraylist of class names in a JarInputStream
    // Calls the above function by converting the jar path to a stream
    private ArrayList<String> getClassNamesFromJar(String jarPath) throws Exception {
        return getClassNamesFromJar(new JarInputStream(new FileInputStream(jarPath)));
    }

    //These component are part of baseComponentsModule
    public enum CompiledDependency {

        ACTIVEMQ("activemq"),
        ALERIS("aleris"),
        AMAZON("amazon"),
        AMQP("amqp"),
        AS2("as2"),
        AWS2S3("aws2-s3"),
        DIRECT("direct"),
        ELASTICSEARCHREST("elasticsearch-rest"),
        ENCODER("encoder"),
        FILE("file"),
        FTP("ftp"),
        FTPS("ftps"),
        HTTP("http"),
        HTTPS("https"),
        IMAP("imap"),
        JMS("jms"),
        JETTY("jetty"),
        KAFKA("kafka"),
        LOG("log"),
        NETTY("netty"),
        NETTYHTTP("netty-http"),
        POP3("pop3"),
        RABBITMQ("rabbitmq"),
        REST("rest"),
        RESTSOPENAPI("rest-openapi"),
        RESTSWAGGER("rest-swagger"),
        //SCHEDULER("scheduler"),
        SFTP("sftp"),
        SEDA("seda"),
        SERVLET("servlet"),
        SIMPLEREPLACE("simplereplace"),
        SJMS("sjms"),
        SJMS2("sjms2"),
        SMTP("smtp"),
        SONICMQ("sonicmq"),
        SQL("sql"),
        SLACK("slack"),
        SPRING("spring"),
        STREAM("stream"),
        STUB("stub"),
        TIMER("timer"),
        UNDERTOW("undertow"),
        VELOCITY("velocity"),
        VERTXHTTP("vertx-http"),
        VM("vm"),
        WEBSOCKET("websocket"),
        XSLT("xslt"),
        XSLTSAXON("xslt-saxon"),
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