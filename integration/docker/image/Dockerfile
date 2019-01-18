FROM ubuntu
RUN apt-get update -qq && apt-get upgrade -y -qq && apt-get install openjdk-8-jre -y -qq
COPY target/uberjar/dda-k8s-standalone.jar /app/dda-k8s-standalone.jar
COPY integration/docker/image/k8s.edn /app/k8s.edn

RUN java -jar /app/dda-k8s-standalone.jar /app/k8s.edn
