package org.assimbly.integrationrest;

import org.assimbly.util.BaseDirectory;
import org.assimbly.util.CertificatesUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** An identity is imported from a JSON body, so that the password of the PKCS12 file isn't sent as a header. */
class CertificateIdentityFileTest {

    private static final String KEYSTORE = "identity-file-test.p12";

    @TempDir
    Path baseDirectory;

    private String originalBaseDirectory;
    private MockMvc mockMvc;
    private String body;

    @BeforeEach
    void setUp() throws Exception {
        originalBaseDirectory = BaseDirectory.getInstance().getBaseDirectory();
        BaseDirectory.getInstance().setBaseDirectory(baseDirectory.toString());
        Files.createDirectories(baseDirectory.resolve("security"));

        mockMvc = MockMvcBuilders.standaloneSetup(new CertificateManagerRuntime(mock(IntegrationRuntime.class))).build();

        try (InputStream p12 = getClass().getResourceAsStream("/certificates/keystore.p12")) {
            String base64 = Base64.getEncoder().encodeToString(p12.readAllBytes());
            body = new ObjectMapper().writeValueAsString(Map.of("p12", base64, "password", "changeit"));
        }
    }

    @AfterEach
    void tearDown() {
        BaseDirectory.getInstance().setBaseDirectory(originalBaseDirectory);
    }

    @Test
    void importsAndReplacesAnIdentityWithThePasswordInTheBody() throws Exception {
        mockMvc.perform(post("/api/certificates/identity/example")
                        .header("keystoreName", KEYSTORE)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        String keystorePath = baseDirectory.resolve("security").resolve(KEYSTORE).toString();
        assertTrue(CertificatesUtil.getCertificates(keystorePath, CertificatesUtil.runtimeKeystorePassword()).containsKey("example"));

        // a second import of the same name is refused, a replace isn't
        mockMvc.perform(post("/api/certificates/identity/example")
                        .header("keystoreName", KEYSTORE)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/certificates/identity/example")
                        .header("keystoreName", KEYSTORE)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }
}
