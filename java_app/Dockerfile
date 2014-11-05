FROM dockerfile/java:oracle-java7
MAINTAINER David Sachitano

RUN apt-get update
RUN apt-get -y install gdal-bin
RUN apt-get -y install maven
RUN apt-get -y install postgresql-client

ADD ./application /media/application
WORKDIR /media/application
RUN mvn package

EXPOSE 8090
CMD java -jar target/application-1.0-SNAPSHOT.jar server hello-world.yml
