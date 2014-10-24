#!/bin/sh
zip -j build.zip docker-geoserver/Dockerfile docker-geoserver/Dockerrun.aws.json 
zip build.zip resources/
