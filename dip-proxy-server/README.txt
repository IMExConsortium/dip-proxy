---------------------------------------------------------------------
default: mvn clean jetty:run-war
    using jetty server and dev database connection
    mvn -Pdip-proxy-dev -Ddbid=201 clean jetty:run-war
---------------------------------------------------------------------
jetty server running commands:
    mvn -Pdip-proxy-dev -Ddbid=id clean jetty:run-war
    mvn -Pdip-proxy-beta -Ddbid=id clean jetty:run-war
    mvn -Pdip-proxy-production -Ddbid=id clean jetty:run-war
---------------------------------------------------------------------
tomcat server running commands:
    mvn -P tomcat-server, dip-proxy-dev -Ddbid=id clean jetty:run-war
    mvn -P tomcat-server, dip-proxy-beta -Ddbid=id clean jetty:run-war
    mvn -P tomcat-server, dip-proxy-production -Ddbid=id clean jetty:run-war
---------------------------------------------------------------------
To turn off overlayweaver logginng add:

ow.java.logger.logging=WARNING

to the conf/logging.properties file


