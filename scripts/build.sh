#!/bin/sh
zip -j geoserver_build.zip docker-geoserver/Dockerfile docker-geoserver/Dockerrun.aws.json 
zip geoserver_build.zip resources/

zip -j postgis_build.zip docker-postgis/Dockerfile docker-postgis/Dockerrun.aws.json
