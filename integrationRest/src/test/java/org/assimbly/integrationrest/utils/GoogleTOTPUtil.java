package org.assimbly.integrationrest.utils;

import com.warrenstrange.googleauth.*;

public class GoogleTOTPUtil {

    public static int generateToken(String secret) {

        if(secret == null) {
            return -1;
        }

        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        return gAuth.getTotpPassword(secret);
    }
}
