package org.assimbly.integration.impl.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class StartupManager {

    protected static final Logger log = LoggerFactory.getLogger(StartupManager.class);

    private static final String DEPENDENCY_HEALTH_URLS_ENV = "DEPENDENCY_HEALTH_URLS";
    private static final int DEPENDENCY_MAX_RETRIES = 60;   // 60 × 5s = 5 min total
    private static final int DEPENDENCY_RETRY_DELAY_MS = 5_000;

    public void waitForDependencies() {

        List<String> healthUrls = getDependencyHealthUrls();

        if (healthUrls.isEmpty()) {
            log.info("No dependency health URLs configured, skipping wait.");
            return;
        }

        log.info("Waiting for dependencies: {}", healthUrls.stream().map(StartupManager::withoutUserInfo).toList());

        for (int attempt = 1; attempt <= DEPENDENCY_MAX_RETRIES; attempt++) {
            boolean allUp = healthUrls.stream().allMatch(StartupManager::isHealthy);

            if (allUp) {
                log.info("All dependencies healthy (attempt {}/{})", attempt, DEPENDENCY_MAX_RETRIES);
                return;
            }

            log.info("Dependencies not ready yet, retrying in {}s (attempt {}/{})",
                    DEPENDENCY_RETRY_DELAY_MS / 1000, attempt, DEPENDENCY_MAX_RETRIES);

            try {
                Thread.sleep(DEPENDENCY_RETRY_DELAY_MS);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                log.warn("waitForDependencies interrupted");
                return;
            }
        }

        log.error("Dependencies did not become healthy after {} retries. Proceeding anyway.", DEPENDENCY_MAX_RETRIES);
    }

    private static boolean isHealthy(String url) {
        HttpURLConnection conn = null;
        try {
            URI uri = URI.create(url);

            conn = (HttpURLConnection) uri.toURL().openConnection();

            conn.setConnectTimeout(2_000);
            conn.setReadTimeout(3_000);
            conn.setRequestMethod("GET");

            if (uri.getUserInfo() != null) {
                String auth = Base64.getEncoder().encodeToString(uri.getUserInfo().getBytes(StandardCharsets.UTF_8));
                conn.setRequestProperty("Authorization", "Basic " + auth);
            }

            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            log.debug("Dependency not ready: {} — {}", withoutUserInfo(url), e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    // A health URL may carry credentials (user:password@host), which don't belong in the log
    static String withoutUserInfo(String url) {
        try {
            URI uri = URI.create(url);
            if (uri.getUserInfo() == null) {
                return url;
            }
            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment()).toString();
        } catch (Exception _) {
            return "<invalid url>";
        }
    }

    private List<String> getDependencyHealthUrls() {
        String env = System.getenv(DEPENDENCY_HEALTH_URLS_ENV);
        if (env == null || env.isBlank()) {
            return List.of();
        }
        return Arrays.stream(env.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
