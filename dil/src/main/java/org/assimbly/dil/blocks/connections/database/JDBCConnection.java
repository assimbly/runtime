package org.assimbly.dil.blocks.connections.database;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.Registry;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;


public class JDBCConnection {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private CamelContext context;
    private EncryptableProperties properties;
    private String componentName;
    private String connectionId;

    private String url;
    private String username;
    private String password;
    private String driver;

    public JDBCConnection(CamelContext context, EncryptableProperties properties, String connectionId, String componentName) {
        this.context = context;
        this.properties = properties;
        this.connectionId = connectionId;
        this.componentName = componentName;
    }

    public void start(String direction, Object stepId) throws Exception {

        log.info("Setting up jms client connection for ActiveMQ.");

        setFields();

        if (url != null) {
            log.info("Create datasource for url: " + url + "(driver=" + driver + ")");
            setConnection(direction, stepId);
        } else {
            log.error("Database JDBC url is missing.");
            throw new Exception("Database JDBC url is missing.\n");
        }
    }

    private void setFields(){

        driver = properties.getProperty("connection." + connectionId + ".driver");
        url = properties.getProperty("connection." + connectionId + ".url");
        username = properties.getProperty("connection." + connectionId + ".username");
        password = properties.getProperty("connection." + connectionId + ".password");

    }

    private void setConnection(String direction, Object stepId) throws Exception {

        String connectionIdValue;
        if(direction.equals("error")) {
            connectionIdValue = properties.getProperty(direction + ".connection.id");
        }else {
            connectionIdValue = properties.getProperty(direction + "." + stepId + ".connection.id");
        }


        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName(driver);
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);

        //Add datasource to registry
        Registry registry = context.getRegistry();
        registry.bind(connectionIdValue, ds);

        log.info("Datasource has been created");

    }

}
