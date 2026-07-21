package org.assimbly.dil.blocks.connections.auth;

import org.apache.camel.CamelContext;
import org.eclipse.jetty.ee10.servlet.security.ConstraintMapping;
import org.eclipse.jetty.ee10.servlet.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.*;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.util.security.Password;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicAuthentication {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private final CamelContext context;
    private final EncryptableProperties properties;
    private final String connectionId;

    private String username;
    private String password;
    private String path = "";

    public BasicAuthentication(CamelContext context, EncryptableProperties properties, String connectionId) {
        this.context = context;
        this.properties = properties;
        this.connectionId = connectionId;
    }

    public void start() throws Exception {

        log.info("Setting Basic Authentication for connection={}",connectionId);

        setFields();

        if (username != null && password != null) {
            ConstraintSecurityHandler securityHandler = setHandlers();
            addToRegistry(securityHandler, connectionId);
        } else {
            throw new Exception("Basic Authentication: Username/password are required");
        }

    }

    private void setFields(){

        username = properties.getProperty("connection." + connectionId + ".username");
        password = properties.getProperty("connection." + connectionId + ".password");
        path = properties.getProperty("connection." + connectionId + ".path");

    }

    private ConstraintSecurityHandler setHandlers() {

        UserStore userStore = new UserStore();
        userStore.addUser(username, new Password(password), new String[]{"user"});

        HashLoginService loginService = new HashLoginService(connectionId);
        loginService.setUserStore(userStore);

        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        securityHandler.setAuthenticator(new BasicAuthenticator());
        securityHandler.setLoginService(loginService);
        securityHandler.setRealmName(connectionId);

        Constraint constraint = new Constraint.Builder()
                .name("auth")
                .roles("user")
                .build();

        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setConstraint(constraint);
        mapping.setPathSpec(path + "/*");
        securityHandler.addConstraintMapping(mapping);

        return securityHandler;

    }

    private void addToRegistry(ConstraintSecurityHandler securityHandler, String connectionId) throws Exception {

        context.getRegistry().bind(connectionId, Handler.class, securityHandler);

        Object isRegistered = context.getRegistry().lookupByName(connectionId);

        if(isRegistered != null){
            log.info("BasisAuthentication for connection {} is registered", connectionId);
        }else{
            throw new Exception("BasisAuthentication for connection " + connectionId + " cannot be registered. SecurityHandler is null");
        }

    }

}