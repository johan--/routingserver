FROM dockerfile/java:oracle-java7
MAINTAINER David Sachitano

RUN apt-get update
RUN apt-get -y install gdal-bin
RUN apt-get -y install maven
RUN apt-get -y install postgresql-client
RUN apt-get -y install postgis

ADD ./application /media/application
WORKDIR /media/application
RUN mvn package

EXPOSE 8090
EXPOSE 5005
CMD java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar target/application-1.0-SNAPSHOT.jar server hello-world.yml
