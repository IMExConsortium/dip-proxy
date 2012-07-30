---------------------------------------------------------------------
jetty server running commands:
    mvn -P dip-proxy-dev clean jetty:run-war
    mvn -Pdip-proxy-beta clean jetty:run-war
    mvn -Pdip-proxy-production clean jetty:run-war
---------------------------------------------------------------------
tomcat server running commands:
    mvn -P tomcat-server, dip-proxy-dev clean jetty:run-war
    mvn -P tomcat-server, dip-proxy-beta clean jetty:run-war
    mvn -P tomcat-server, dip-proxy-production clean jetty:run-war
---------------------------------------------------------------------
