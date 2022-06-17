FROM openjdk:17-jdk-alpine
EXPOSE 9081
ARG JAR_FILE=target/dukascopy-api-websocket-1.0.war
ADD ${JAR_FILE} dukascopy-api-websocket.war
ENTRYPOINT ["java","-jar","dukascopy-api-websocket.war","--dukascopy.credential-username=username", "--dukascopy.credential-password=password"]