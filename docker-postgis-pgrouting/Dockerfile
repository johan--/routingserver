FROM ubuntu:12.04
MAINTAINER daveism <daveism@gmail.com>

RUN echo "deb http://archive.ubuntu.com/ubuntu precise main universe" > /etc/apt/sources.list
RUN echo "deb http://us-east-1.ec2.archive.ubuntu.com/ubuntu/ precise multiverse" >> /etc/apt/sources.list
RUN echo "deb-src http://us-east-1.ec2.archive.ubuntu.com/ubuntu/ precise multiverse" >> /etc/apt/sources.list
RUN echo "deb http://us-east-1.ec2.archive.ubuntu.com/ubuntu/ precise-updates multiverse" >> /etc/apt/sources.list
RUN echo "deb-src http://us-east-1.ec2.archive.ubuntu.com/ubuntu/ precise-updates multiverse" >> /etc/apt/sources.list

RUN apt-get -y update
RUN apt-get -y install wget
RUN wget --quiet --no-check-certificate -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | apt-key add -
RUN echo "deb http://apt.postgresql.org/pub/repos/apt/ precise-pgdg main" >> /etc/apt/sources.list
RUN apt-get -y update
RUN apt-get -y upgrade
RUN apt-get -y install postgresql-9.1 postgresql-contrib-9.1 postgresql-9.1-postgis-2 postgis

RUN apt-get -y install python-software-properties
RUN add-apt-repository -y ppa:georepublic/pgrouting-unstable
RUN apt-get -y  update
RUN apt-get -y install postgresql-9.1-pgrouting

RUN echo "listen_addresses = '*'" >> /etc/postgresql/9.1/main/postgresql.conf
RUN echo "host    all             all             0.0.0.0/0               md5" >> /etc/postgresql/9.1/main/pg_hba.conf
RUN service postgresql start && /bin/su postgres -c "createuser -d -s -r -l docker" && /bin/su postgres -c "psql postgres -c \"ALTER USER docker WITH ENCRYPTED PASSWORD 'docker'\" && psql postgres -c \"CREATE DATABASE docker\" && psql postgres -c \"GRANT ALL ON DATABASE docker to docker\"" && service postgresql stop

RUN service postgresql start && /bin/su postgres -c "createdb routes -U postgres -O postgres"  && service postgresql stop
RUN service postgresql start && /bin/su postgres -c "psql postgres -d routes -c \"create extension postgis;\""  && service postgresql stop
RUN service postgresql start && /bin/su postgres -c "psql postgres -d routes -c \"create extension pgrouting;\""  && service postgresql stop
RUN service postgresql start && /bin/su postgres -c "psql postgres -d routes -c \"create extension hstore;\""  && service postgresql stop
RUN service postgresql start && /bin/su postgres -c "psql postgres -d routes -c 'create extension \"uuid-ossp\";'"  && service postgresql stop

EXPOSE 5432

RUN service postgresql stop
RUN service postgresql start 

ADD start.sh /start.sh
RUN chmod 0755 /start.sh

CMD ["/start.sh"]