package org.assimbly.util;

import com.google.common.reflect.ClassPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("rawtypes")
public class ClassesInPackage {

        protected static final Logger log = LoggerFactory.getLogger("org.assimbly.util.ClassesInPackage");

        public Set<Class> findClasses(String packageName) throws IOException {

            Set<Class> classes = Set.of();
            InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(packageName.replaceAll("[.]", "/"));

            if (stream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8))) {

                    classes = reader.lines()
                            .filter(line -> line.endsWith(".class"))
                            .map(line -> getClass(line, packageName))
                            .collect(Collectors.toSet());

                }
            }

            return classes;
        }

        private Class getClass(String className, String packageName) {
            try {
                return Class.forName(packageName + "." + className.substring(0, className.lastIndexOf('.')));
            } catch (ClassNotFoundException e) {
                log.error("Class not found: {}. Reason: {}", className, e.getMessage());
            }
            return null;
        }

    public Set<Class> findAllClasses(String packageName) throws IOException {
        return ClassPath.from(ClassLoader.getSystemClassLoader())
                .getAllClasses()
                .stream()
                .filter(clazz -> clazz.getPackageName()
                        .equalsIgnoreCase(packageName))
                .map(ClassPath.ClassInfo::load)
                .collect(Collectors.toSet());
    }

}