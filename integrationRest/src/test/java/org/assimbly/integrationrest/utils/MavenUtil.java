package org.assimbly.integrationrest.utils;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MavenUtil {

    public static String getModelVersion() throws Exception {
        String version = null;
        File pomFile = new File("pom.xml");
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(pomFile.toURI()), StandardCharsets.UTF_8)) {
            MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
            Model model = xpp3Reader.read(reader);
            version = model.getModelVersion();
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }
        return version;
    }
}
