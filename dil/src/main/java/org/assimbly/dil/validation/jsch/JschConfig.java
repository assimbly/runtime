package org.assimbly.dil.validation.jsch;

import com.jcraft.jsch.JSch;

public class JschConfig {

    private static final String SERVER_HOST_KEY = "server_host_key";
    private static final String PUBKEY_ACCEPTED_ALGORITHMS = "PubkeyAcceptedAlgorithms";
    private static final String KEX = "kex";

    private static final String JSCH_SERVER_HOST_KEY_ENV_VAR = "JSCH_SERVER_HOST_KEY";
    private static final String JSCH_PUBKEY_ACCEPTED_ALGORITHMS_ENV_VAR = "JSCH_PUBKEY_ACCEPTED_ALGORITHMS";
    private static final String JSCH_KEX_ENV_VAR = "JSCH_KEX";

    public static void enableExtraConfigOnJsch() {
        setExtraConfigFromEnvironmentVar(SERVER_HOST_KEY, JSCH_SERVER_HOST_KEY_ENV_VAR);
        setExtraConfigFromEnvironmentVar(PUBKEY_ACCEPTED_ALGORITHMS, JSCH_PUBKEY_ACCEPTED_ALGORITHMS_ENV_VAR);
        setExtraConfigFromEnvironmentVar(KEX, JSCH_KEX_ENV_VAR);
    }

    private static String[] getExtraConfigFromEnvironmentVar(String envVar) {
        String envVarValue = System.getenv(envVar);
        if(envVarValue!=null && !envVarValue.isEmpty()) {
            return envVarValue.split(",");
        }
        return new String[0];
    }

    private static void setExtraConfigFromEnvironmentVar(String key, String envVar) {
        String[] extraConfArr = getExtraConfigFromEnvironmentVar(envVar);
        for (String extraConf: extraConfArr) {
            setJschConfig(key, extraConf);
        }
    }

    private static void setJschConfig(String key, String additionalKeyValue) {
        String keyValue = JSch.getConfig(key);
        boolean isKeyValueEmpty = keyValue.isEmpty();
        String additionalKeyValueMiddlePos = ","+additionalKeyValue+",";
        String additionalKeyValueEndPos = ","+additionalKeyValue;

        if(isKeyValueEmpty) {
            JSch.setConfig(key, additionalKeyValue);
        } else {
            if(!keyValue.equalsIgnoreCase(additionalKeyValue) && !keyValue.contains(additionalKeyValueMiddlePos) && !keyValue.endsWith(additionalKeyValueEndPos)) {
                JSch.setConfig(key, keyValue + additionalKeyValueEndPos);
            }
        }
    }

}
