FROM dda-k8s-crate

RUN apt-get install curl -y -qq

RUN curl -L -o /app/serverspec.jar https://github.com/DomainDrivenArchitecture/dda-serverspec-crate/releases/download/1.2.2/dda-serverspec-crate-1.2.2-standalone.jar

COPY integration/docker/test/serverspec.edn /app/serverspec.edn

RUN java -jar /app/serverspec.jar /app/serverspec.edn
