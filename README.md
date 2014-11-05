# Routing Server for Baseball Run
This server is designed to be an interactive web map that can help solve complicated scheduling and routing questions that arise when trying to go running while attending as many baseball games as you can along the way.  See [thebaseballrun.com/about](http://thebaseballrun.com/about) for more on why you would possibly want to do such a thing.

## What's in this repo
This repo has Dockerfiles that define all server components the system depends on, as well as front-end and back-end application code.  There are a handful of server images, which are orchestrated by the fig utility.

## Server components
* PostgreSQL
    * postgis
    * pgrouting
* Geoserver
* A dropwizard java server
* NGINX to proxy the dropwizard app and geoserver, and to serve static html and assets
* static html and javascript
    * this is where the frontend [leaflet](http://leafletjs.com/) and interaction code is

## Setup
The dependencies are:

* [Docker](https://docker.com/)
    * for local development on your own computer, I would use [boot2docker](http://boot2docker.io/)
    * for deployment in the cloud, maybe choose an [Amazon Linux AMI](http://aws.amazon.com/amazon-linux-ami/), which comes preloaded with docker.
* [Fig](http://www.fig.sh/install.html)

Once these are installed, clone this repo, and run `fig up`

## What's not in this repo
There is a big chunk of code I have not yet committed, which is responsible for reading baseball schedules and performing graph traversal to determine high level day by day itineraries.  This data is then fed to pgrouting to determine the detailed street-level routing between baseball stadiums.

There is also some important code missing which I'm currently doing manually right now: managing the Geoserver data.  This code as is will install a default geoserver instance, and then you must manually add your PostGIS source, and manually add your SQL query layers.  There exists an [HTTP API for this](http://docs.geoserver.org/2.5.x/en/user/rest/index.html), I just need to get that part figured out.  I expect to be able to add this in the Dockerfile, so the whole system will "just work".

Lastly, I have not automated the part that loads scheduling and street data into PostGIS.  I'm currently running `shp2pgsql` to import the [NHPN](http://www.fhwa.dot.gov/planning/processes/tools/nhpn/) dataset, and I'm building my schedule data using some ruby code based on my [pitch_fx scraper gem](http://rubygems.org/gems/pitch_fx_scraper/)
