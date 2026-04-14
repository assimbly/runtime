package org.assimbly.dil.blocks.connections.database;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.Registry;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;


public class JDBCConnection {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private final CamelContext context;
    private final EncryptableProperties properties;
    private final String connectionId;

    private String database;
    private String url;
    private String username;
    private String password;
    private String driver;

    public JDBCConnection(CamelContext context, EncryptableProperties properties, String connectionId) {
        this.context = context;
        this.properties = properties;
        this.connectionId = connectionId;
    }

    public void start(String direction, Object stepId) throws Exception {

        log.info("Setting up jms client connection for ActiveMQ.");

        setFields();

        if (url != null) {
            log.info("Create datasource for url: {} (driver={})", url, driver);
            setConnection(direction, stepId);
        } else {
            log.error("Database JDBC url is missing.");
            throw new Exception("Database JDBC url is missing.\n");
        }
    }

    private void setFields(){

        username = properties.getProperty("connection." + connectionId + ".username");
        password = properties.getProperty("connection." + connectionId + ".password");

        database = properties.getProperty("connection." + connectionId + ".database");

        setDriverName();
        setUrl();

    }

    private void setDriverName() {

        driver = properties.getProperty("connection." + connectionId + ".driver");

        if(driver!=null){
            return;
        }

        switch (database.toLowerCase()) {
            case "mysql":
                driver = "com.mysql.jdbc.Driver";
                break;
            case "oracle":
                driver = "oracle.jdbc.driver.OracleDriver";
                break;
            case "postgres","postgresql":
                driver = "org.postgresql.Driver";
                break;
            case "sqlserver":
                driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
                break;
            case "db2":
                driver = "com.ibm.db2.jcc.DB2Driver";
                 break;
            case "informix","informix-sqli":
                driver = "com.informix.jdbc.IfxDriver";
                break;
            case "sqlite":
                driver = "org.sqlite.JDBC";
                break;
            default:
                throw new IllegalArgumentException("Unsupported database=" + database);
        }
    }

    private void setUrl() {

        url = properties.getProperty("connection." + connectionId + ".url");

        if(url!=null){
            return;
        }

        String host = properties.getProperty("connection." + connectionId + ".host");
        String port = properties.getProperty("connection." + connectionId + ".port");

        switch (database.toLowerCase()) {
            case "mysql":
                url = (host != null && port != null)
                        ? "jdbc:mysql://" + host + ":" + port + "/"
                        : "jdbc:mysql://localhost:3306/";
                break;
            case "oracle":
                url = (host != null && port != null)
                        ? "jdbc:oracle:thin:@" + host + ":" + port
                        : "jdbc:oracle:thin:@localhost:1521:orcl";
                break;
            case "postgres", "postgresql":
                url = (host != null && port != null)
                        ? "jdbc:postgresql://" + host + ":" + port + "/"
                        : "jdbc:postgresql://localhost:5432/";
                break;
            case "sqlserver":
                url = (host != null && port != null)
                        ? "jdbc:sqlserver://" + host + ":" + port
                        : "jdbc:sqlserver://localhost:1433;";
                break;
            case "db2":
                url = (host != null && port != null)
                        ? "jdbc:db2://" + host + ":" + port + "/"
                        : "jdbc:db2://localhost:50000/";
                break;
            case "informix", "informix-sqli":
                url = (host != null && port != null)
                        ? "jdbc:informix-sqli://" + host + ":" + port + "/"
                        : "jdbc:informix-sqli://localhost:9088/";
                break;
            case "sqlite":
                url = (host != null && port != null)
                        ? "jdbc:sqlite://" + host + ":" + port + "/"
                        : "jdbc:sqlite://localhost/";
                break;
            default:
                throw new IllegalArgumentException("Unsupported database: " + database);
        }
    }


    private void setConnection(String direction, Object stepId) {

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

        log.info("Datasource has been created for {} with connection ID: {}", database, connectionIdValue);

    }

}
