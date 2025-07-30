package org.assimbly.util;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;

import java.io.FileInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;


public class DependencyUtil {

	public List<Path> resolveDependency(String groupId, String artifactId, String version) {
    	
        String dependency = groupId + ":" + artifactId + ":" + version;
        
        JkDependencySet deps = JkDependencySet.of()
                .and(dependency);

        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());

        return resolver.resolve(deps).getFiles().getEntries();

	}

    public List<Class<?>> loadDependency(List<Path> paths) throws Exception {

        List<Class<?>> classes = new ArrayList<>();

        for(Path path: paths){

            URL url = path.toUri().toURL();

            try(URLClassLoader child = new URLClassLoader(new URL[]{url}, this.getClass().getClassLoader())) {

                List<String> classNames;
                try {
                    classNames = getClassNamesFromJar(path.toString());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                for (String className : classNames) {
                    System.out.println("Class name: " + className);
                    try {
                        Class<?> classToLoad = Class.forName(className, true, child);
                        classes.add(classToLoad);
                    } catch (NoClassDefFoundError e) {
                        // Ignore missing dependencies
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

            }

        }

        return classes;

    }

    // Returns an arraylist of class names in a JarInputStream
    private List<String> getClassNamesFromJar(JarInputStream jarFile) throws Exception {
        List<String> classNames = new ArrayList<>();
        try {
            JarEntry jar;

            //Iterate through the contents of the jar file
            while (true) {
                jar = jarFile.getNextJarEntry();
                if (jar == null) {
                    break;
                }
                //Pick file that has the extension of .class
                if (jar.getName().endsWith(".class")) {
                    String className = jar.getName().replace("/", ".");
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
    private List<String> getClassNamesFromJar(String jarPath) throws Exception {
        return getClassNamesFromJar(new JarInputStream(new FileInputStream(jarPath)));
    }

    //These components are part of baseComponentsModule
    public enum CompiledDependency {

        AGGREGATE("aggregate"),
        ALERIS("aleris"),
        AMAZON("amazon"),
        AMAZONMQ("amazonmq"),
        AMQP("amqp"),
        AMQPS("amqps"),
        ARCHIVE("archive"),
        AS2("as2"),
        BASE64TOTEXT("base64totext"),
        BEAN("bean"),
        CONTROLBUS("controlbus"),
        CSVTOXML("csvtoxml"),
        DATAFORMAT("dataformat"),
        DELAY("delay"),
        DOCCONVERTER("docconverter"),
        EDIFACTDOTWEB("edifact-dotweb"),
        EDIFACTSTANDARDS("edifact-standards"),
        EDIFACTSTANDARDSTOXML("edifactstandardstoxml"),
        EDIFACTTOXML("edifacttoxml"),
        EDITOXML("editoxml"),
        ELASTICSEARCHREST("elasticsearch-rest"),
        ENCODER("encoder"),
        ENRICH("enrich"),
        EXCELTOXML("exceltoxml"),
        FILE("file"),
        FILTER("filter"),
        FLV("flv"),
        FMUTA("fmuta"),
        FORM2XML("form2xml"),
        FORMTOXML("formtoxml"),
        GOOGLEDRIVE("googledrive"),
        GROOVY("groovy"),
        HTTP("http"),
        HTTPS("https"),
        IBMMQ("ibmmq"),
        IMAP("imap"),
        IMAPS("imaps"),
        JETTYNOSSL("jetty-nossl"),
        JSONTOXML("jsontoxml"),
        LOG("log"),
        MLLP("mllp"),
        MULTIPART("multipart"),
        NETTY("netty"),
        NETTYHTTP("netty-http"),
        ORIFLAME("oriflame"),
        PDFTOTEXT("pdftotext"),
        POLLENRICH("pollenrich"),
        PRINT("print"),
        QUEUETHROTTLE("queuethrottle"),
        REMOVECOOKIE("removecookie"),
        REMOVEHEADERS("removeheaders"),
        REPLACE("replace"),
        REPLACEINPDF("replaceinpdf"),
        SANDBOX("sandbox"),
        SERVLET("servlet"),
        SETBODY("setbody"),
        SETCOOKIE("setcookie"),
        SETHEADER("setheader"),
        SETHEADERS("setheaders"),
        SETMESSAGE("setmessage"),
        SETOAUTH2TOKEN("setoauth2token"),
        SETPATTERN("setpattern"),
        SETPROPERTY("setproperty"),
        SETUUID("setuuid"),
        SIMPLEREPLACE("simplereplace"),
        SONICMQ("sonicmq"),
        SQLCUSTOM("sql-custom"),
        TEXTTOBASE64("texttobase64"),
        THROTTLE("throttle"),
        UNIVOCITYCSV("univocity-csv"),
        UNZIP("unzip"),
        WASTEBIN("wastebin"),
        WIRETAP("wiretap"),
        XMLTOCSV("xmltocsv"),
        XMLTOEDI("xmltoedi"),
        XMLTOEDIFACT("xmltoedifact"),
        XMLTOEDIFACTSTANDARDS("xmltoedifactstandards"),
        XMLTOEXCEL("xmltoexcel"),
        XMLTOJSON("xmltojson"),
        XMLTOJSONLEGACY("xmltojsonlegacy"),
        XSLT("xslt"),
        XSLTSAXON("xslt-saxon"),
        ZIP("zip"),
        ;

        private static final Map<String, CompiledDependency> byLabel = new HashMap<>();

        static {
            for (CompiledDependency cd : values()) {
                byLabel.put(cd.label, cd);
            }
        }

        public final String label;

        CompiledDependency(final String label) {
            this.label = label;
        }

        public static boolean hasCompiledDependency(String label){
            return byLabel.containsKey(label);
        }
    }

}