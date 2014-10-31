


var map = L.map('map').setView([39.00, -75.09], 8);


L.tileLayer('http://{s}.tile.stamen.com/toner-lite/{z}/{x}/{y}.png', {
    attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
    maxZoom: 18
}).addTo(map);


var mapLayers = [];
var allFeatures = L.featureGroup();
var allMarkers = new L.FeatureGroup();
map.addLayer(allMarkers);

var currentFeatureGroup = null;

function genNewFeatureGroup() {
    var fg = new L.featureGroup();
    fg.on('mouseover',highlightFeature);
    fg.on('mouseout',resetHighlight);
    fg.addTo(map);
    currentFeatureGroup = fg;
    return fg;
}


var layerCount = 0;
function onEachFeature(feature, featureLayer) {
    mapLayers[layerCount++] = featureLayer;
} 

var lineStyle = {
    "color" : "#ff0000"
}

function resetHighlight(e) {
    var lgLayers = e.target.getLayers();
    for (var i = 0; i < lgLayers.length ; i++) {
        lgLayers[i].resetStyle();
        lgLayers[i].setStyle({color: '#00F',
                              fillOpacity: 0.7});
    }
    info.update();
}

function highlightFeature(e) {
    var layer = e.target;

    layer.setStyle({
        weight: 5,
        color: '#F00',
        dashArray: '',
        fillOpacity: 1.0
    });

    if (!L.Browser.ie && !L.Browser.opera) {
        layer.bringToFront();
    }
    info.update({"desc":layer.getLayers()[0].properties["desc"],"stadiumCount":4});
}



/* custom control */
// var info = L.control();

// info.onAdd = function (map) {
//     this._div = L.DomUtil.create('div', 'info'); // create a div with a class "info"
//     this.update();
//     return this._div;
// };

// // method that we will use to update the control based on feature properties passed
// info.update = function (props) {
//     this._div.innerHTML = '<h4>Route:</h4>' +  (props ?
//         '<b>' + props.desc + '</b><br />' + props.stadiumCount + ' different stadiums'
//         : 'Hover over a route');
// };

// info.addTo(map);


// when a layer is clicked, open a small popup with
// a googl street view image corresponding to the coordinates
// where the user clicked
function handleLayerClick(e) {
    var popup = L.popup({minWidth:300})
    .setLatLng(e.latlng)
    .setContent("<img src=\"http://maps.googleapis.com/maps/api/streetview?size=300x200&location="+e.latlng.lat+","+e.latlng.lng+"&fov=90&heading=235&pitch=10\"/>")
    .openOn(map);
}

// add icons using the html pulled from the route data
function addIcons(computed_route) {

    var numRouteLegs = computed_route.routeLegs.length;

    for (var i = 0; i < computed_route.routeLegs.length; i++) {
        startMarkerLatLong = computed_route.routeLegs[i]["startLatLong"].reverse();
        var htmlStartIcon = new L.divIcon({html:"<div class='icon-circular'>"+computed_route.routeLegs[i].startIconHtml+"</div>"});
        var startMarker = new L.marker(startMarkerLatLong, {icon:htmlStartIcon});//.addTo(map);
        allMarkers.addLayer(startMarker);

        if (i == computed_route.routeLegs.length-1) {
            endMarkerLatLong = computed_route.routeLegs[i]["endLatLong"].reverse();
            var htmlEndIcon = new L.divIcon({html:"<div class='icon-circular'>"+computed_route.routeLegs[i].endIconHtml+"</div>"});
            var endMarker = new L.marker(endMarkerLatLong, {icon:htmlEndIcon});//.addTo(map);
            allMarkers.addLayer(endMarker);
        } 
    }

}

// given a json object representing a route, and a featureGroup,
// load the remote geojson for each leg of the route into the
// featureGroup
function addRoute(computed_route, featureGroup) {

    clearCurrentLayers(featureGroup);
    addIcons(computed_route);

    map.panTo(allMarkers.getBounds().getCenter());
    map.fitBounds(allMarkers.getBounds());


    $("#route_description").html("");

    for(var i=0; i< computed_route.routeLegs.length; i++) {

        $("#route_description").append("<p id=\"leg"+i+"\"></p>");

        // fetch the geojson for this leg of the route
        $.ajax({
            dataType: "json",
            url: computed_route.routeLegs[i]["url"],
            layerIndex: i,
            success: function(data) {

                var myLayer = L.geoJson(data);
                mapLayers[this.layerIndex] = myLayer;
                console.log("added to layer "+this.layerIndex);
                myLayer.on('click', handleLayerClick);
                showLayer(this.layerIndex, featureGroup);

                allFeatures.addLayer(myLayer);


                if (!myLayer.hasOwnProperty('properties')) {
                    myLayer.properties = {"desc":computed_route.desc};
                } else {
                    myLayer.properties["desc"] = computed_route.desc;
                }


                var legLength = data["features"][0]["properties"]["length_miles"];
                var legStart = computed_route.routeLegs[this.layerIndex]["startName"];
                var legEnd = computed_route.routeLegs[this.layerIndex]["endName"];

                 
                 $("#leg"+this.layerIndex).html("Leg "+(this.layerIndex+1)+": "+legLength+" miles. "+legStart+" to "+legEnd);

            },
            error: function (xhr, ajaxOptions, thrownError) {
                console.log(xhr.status);
                console.log(thrownError);
              }
        });
    }
}

function menuClickHandler(event) {
    addRoute(event.data.param1, event.data.param2);
}

// process json route data
// input is expected to be a list of json objects, each representing a route.
// we add a link for each route to the navigation menu, and then
// when the navigation link is clicked, we load the route into the map
function loadData(inputData) {
    var data = inputData["data"];
    var overlayMaps = {};

    for (var i = 0; i < data.length; i++) {
        var desc = data[i]["desc"];
        var count = data[i]["stadiumCount"];

        var msg = data[i]["routeLegs"][0]["startName"] + ", " + count + " stadiums";

        var fg = genNewFeatureGroup();         
        $("#coolMenuList").append(
            "<li role=\"presentation\"><a id=\"route"+(i+1)+"\" role=\"menuitem\" tabindex=\"-1\" href=\"#\">"+msg+"</a></li>");
        $("#route"+(i+1)).click({param1:data[i],param2:fg}, menuClickHandler);
    }

};


function clearCurrentLayers(featureGroup) {
    if (featureGroup == null) return;

    for (var i = 0; i < mapLayers.length; i++) {
        hideLayer(i, featureGroup);
    }
    //map.removeLayer(allMarkers);
    allMarkers.clearLayers();
    //featureGroup.clearLayers();
    //mapLayers = [];
}

function showLayer(id, featureGroup) {
    var l = mapLayers[id];
    featureGroup.addLayer(l);
}
function hideLayer(id,featureGroup) {
    var l = mapLayers[id];
    if (allFeatures != null) {
        allFeatures.removeLayer(l);
    }
    featureGroup.removeLayer(l);
    map.removeLayer(l);
    //map.removeLayer(featureGroup);
}

function toggleLayer(id, featureGroup) {
    var l = mapLayers[id];
    if (featureGroup.hasLayer(l)) {
        featureGroup.removeLayer(l);
    } else {
        map.panTo(l.getBounds().getCenter());
        featureGroup.addLayer(l);
        map.fitBounds(l.getBounds(),{paddingBottomRight:[150,0]});
    }
}



// read json data file
$.getJSON( "routes_starting_july21.json", function( data ) {
    loadData(data);
});


