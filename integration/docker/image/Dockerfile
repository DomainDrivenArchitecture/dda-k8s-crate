FROM ubuntu
RUN apt-get update -qq && apt-get upgrade -y -qq && apt-get install openjdk-8-jre -y -qq
COPY target/uberjar/dda-smeagol-standalone.jar /app/dda-smeagol-standalone.jar
COPY integration/docker/image/smeagol.edn /app/smeagol.edn

RUN java -jar /app/dda-smeagol-standalone.jar /app/smeagol.edn
