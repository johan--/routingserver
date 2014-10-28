#!/bin/sh
zip -j geoserver_build.zip docker-geoserver/Dockerfile docker-geoserver/Dockerrun.aws.json 
zip geoserver_build.zip resources/

zip -j postgis_build.zip docker-postgis/Dockerfile 
#zip -j postgis_build.zip docker-postgis/Dockerrun.aws.json 
zip -j postgis_build.zip docker-postgis/postgres.conf
zip -j postgis_build.zip docker-postgis/sshd.conf
zip -j postgis_build.zip docker-postgis/setup.sh
zip -j postgis_build.zip docker-postgis/start-postgis.sh
zip -j postgis_build.zip docker-postgis/run-postgis-docker.sh
