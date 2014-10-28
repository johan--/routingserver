#--------- Generic stuff all our Dockerfiles should start with so we get caching ------------
FROM ubuntu:trusty
MAINTAINER Tim Sutton<tim@kartoza.com>

RUN  export DEBIAN_FRONTEND=noninteractive
ENV  DEBIAN_FRONTEND noninteractive
RUN  dpkg-divert --local --rename --add /sbin/initctl
#RUN  ln -s /bin/true /sbin/initctl

# Use local cached debs from host (saves your bandwidth!)
# Change ip below to that of your apt-cacher-ng host
# Or comment this line out if you do not with to use caching
#ADD 71-apt-cacher-ng /etc/apt/apt.conf.d/71-apt-cacher-ng


# from https://launchpad.net/~georepublic/+archive/ubuntu/pgrouting
# per http://workshop.pgrouting.org/chapters/installation.html
RUN echo "deb http://ppa.launchpad.net/georepublic/pgrouting/ubuntu trusty main" > /etc/apt/sources.list
RUN echo "deb-src http://ppa.launchpad.net/georepublic/pgrouting/ubuntu trusty main" > /etc/apt/sources.list

RUN echo "deb http://archive.ubuntu.com/ubuntu trusty main universe" > /etc/apt/sources.list
RUN apt-get -y update


# per http://askubuntu.com/questions/13065/how-do-i-fix-the-gpg-error-no-pubkey
RUN gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv 26568B05B65ADE33
RUN gpg --export --armor 26568B05B65ADE33 | sudo apt-key add -
RUN apt-get -y update
RUN apt-get -y install ca-certificates rpl pwgen

# pgrouting
RUN apt-get -y install software-properties-common
#RUN add-apt-repository -y ppa:ubuntugis/ppa
RUN add-apt-repository -y ppa:georepublic/pgrouting
RUN apt-get update


#-------------Application Specific Stuff ----------------------------------------------------

# Next line a workaround for https://github.com/dotcloud/docker/issues/963
#RUN apt-get install -y postgresql-9.3-postgis-2.1
RUN apt-get install -y postgresql-9.3-pgrouting
RUN service postgresql start && /bin/su postgres -c "createuser -d -s -r -l docker" && /bin/su postgres -c "psql postgres -c \"ALTER USER docker WITH ENCRYPTED PASSWORD 'docker'\"" && service postgresql stop

# Start with supervisor
ADD postgres.conf /etc/supervisor/conf.d/postgres.conf

# Open port 5432 so linked containers can see them
EXPOSE 5432

# Run any additional tasks here that are too tedious to put in
# this dockerfile directly.
ADD setup.sh /setup.sh
RUN chmod 0755 /setup.sh
RUN /setup.sh

# We will run any commands in this when the container starts
ADD start-postgis.sh /start-postgis.sh
RUN chmod 0755 /start-postgis.sh

ENTRYPOINT "/start-postgis.sh"
USER postgres
CMD "/start-postgis.sh"
