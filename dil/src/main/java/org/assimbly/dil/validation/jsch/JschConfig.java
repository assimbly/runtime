package org.assimbly.dil.validation.jsch;

import com.jcraft.jsch.JSch;

public class JschConfig {

    private static final String SERVER_HOST_KEY = "server_host_key";
    private static final String PUBKEY_ACCEPTED_ALGORITHMS = "PubkeyAcceptedAlgorithms";
    private static final String KEX = "kex";

    private static final String SSH_RSA = "ssh-rsa";
    private static final String DIFFIE_HELLMAN_GROUP1_SHA1 = "diffie-hellman-group1-sha1";
    private static final String DIFFIE_HELLMAN_GROUP14_SHA1 = "diffie-hellman-group14-sha1";

    public static void enableExtraConfigOnJsch(JSch jsch) {
        setJschConfig(jsch, SERVER_HOST_KEY, SSH_RSA);
        setJschConfig(jsch, PUBKEY_ACCEPTED_ALGORITHMS, SSH_RSA);
        setJschConfig(jsch, KEX, DIFFIE_HELLMAN_GROUP1_SHA1);
        setJschConfig(jsch, KEX, DIFFIE_HELLMAN_GROUP14_SHA1);
    }

    private static void setJschConfig(JSch jsch, String key, String additionalKeyValue) {
        String keyValue = jsch.getConfig(key);
        boolean isKeyValueEmpty = keyValue.isEmpty();
        String additionalKeyValueMiddlePos = ","+additionalKeyValue+",";
        String additionalKeyValueEndPos = ","+additionalKeyValue;

        if(isKeyValueEmpty) {
            jsch.setConfig(key, additionalKeyValue);
        } else {
            if(!keyValue.equalsIgnoreCase(additionalKeyValue) && !keyValue.contains(additionalKeyValueMiddlePos) && !keyValue.endsWith(additionalKeyValueEndPos)) {
                jsch.setConfig(key, keyValue + additionalKeyValueEndPos);
            }
        }
    }

}
