FROM tomcat:10.1.40-jdk21


COPY target/dropboxproject.war /usr/local/tomcat/webapps/ROOT.war

# Tomcat portunu aç
EXPOSE 8080

# Tomcat'i başlat
CMD ["catalina.sh", "run"]