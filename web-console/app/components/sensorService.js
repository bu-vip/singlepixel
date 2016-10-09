var sensorService = angular.module('sensorService', [])

sensorService.factory('SensorService', ['$rootScope', "$timeout", function ($rootScope, $timeout) {
    var service = {
        public: {
            groups: {},
            state: {
                connected: false
            }
        }
    }

    function Group(aGroupId) {
        this.id = aGroupId;
        this.sensors = {};
    }

    Group.prototype.getSensor = function (aSensorId) {
        // check if we DON'T have the sensor id
        if (!this.sensors[aSensorId]) {
            // add it
            this.sensors[aSensorId] = new Sensor(aSensorId)
        }
        // get sensor
        return this.sensors[aSensorId];
    }

    function Sensor(aSensorId) {
        this.id = aSensorId;
        this.value = [0, 0, 0, 0, 0, 0];
        this.color = "rgb(0, 0, 0)";
        this.fpsStartTime = (new Date()).getTime();
        this.fpsCounter = 0;
        this.fps = 0.0;
    }

    Sensor.prototype.addReading = function (aReading) {
        this.value = aReading;

        //convert reading to RGB color
        var maxVal = 1.0;
        this.color = "rgb(" + Math.floor(aReading[0] / maxVal * 255);
        this.color += ", " + Math.floor(aReading[1] / maxVal * 255);
        this.color += ", " + Math.floor(aReading[2] / maxVal * 255) + ")";

        //monitor FPS
        this.fpsCounter++;
        var fpsWindow = 100;
        if (this.fpsCounter > fpsWindow) {
            var d = new Date();
            var endTime = d.getTime();
            var secondsElapsed = (endTime - this.fpsStartTime) / 1000.0; //time is in milliseconds
            this.fps = fpsWindow / secondsElapsed;
            this.fpsStartTime = (new Date()).getTime();
            this.fpsCounter = 0;
        }
    };

    service.public.connect = function (aHostname, aPort, aClientId, aUser, aPass, aPrefix) {
        // process the prefix
        if (aPrefix) {
            // ensure the prefix starts with "/"
            if (!aPrefix.startsWith('/')) {
                aPrefix = '/' + aPrefix
            }
            service.destination = aPrefix + '/#'
            service.prefixSize = aPrefix.split('/').length
        } else {
            service.prefixSize = 0
            service.destination = '/#'
        }

        // create client
        service.client = new Paho.MQTT.Client(aHostname, Number(aPort), aClientId)

        // set callback handlers
        service.client.onConnectionLost = onConnectionLost
        service.client.onMessageArrived = onMessageArrived

        // connect the client
        service.client.connect({
            userName: aUser,
            password: aPass,
            onSuccess: onConnect
        })
    }

    service.public.disconnect = function () {
        service.client.disconnect();
    }

    function onConnect() {
        service.client.subscribe(service.destination)
        service.public.state.connected = true;
        //empty function causes a digest
        //effectively updating all clients
        $timeout(function () {});
    }

    // called when a message arrives
    function onMessageArrived(message) {
        //console.log('onMessageArrived:' + message.payloadString)

        // topic example:
        //    /<prefix>/group/<group-id>/sensor/<sensor-id>
        // split the topic into it's levels
        var levels = message.destinationName.split('/')
        // splice off the empty "" before the first "/"
        levels = levels.slice(1)

        // basic checking for invalid topics
        if (levels.length == service.prefixSize + 4) {
            if (levels[service.prefixSize] == 'group') {
                if (levels[service.prefixSize + 2] == 'sensor') {
                    // get the group id
                    var groupId = levels[service.prefixSize + 1]

                    // check if we DON'T have that groupId
                    if (!service.public.groups[groupId]) {
                        // add it
                        service.public.groups[groupId] = new Group(groupId);
                    }
                    // get group
                    var group = service.public.groups[groupId]

                    // guranteed to have the group now
                    // get the sensor id
                    var sensorId = levels[service.prefixSize + 3]

                    // get sensor
                    var sensor = group.getSensor(sensorId);

                    // parse the message contents
                    // message contents should be CSV of 4 nums
                    // e.g. "123, 456, 789, 149"
                    var messageContents = message.payloadString.split(',')
// check the values are valid
                    if (messageContents.length != 6) {
                        console.log('Invalid sensor reading size')
                    } else {
                        // convert to number array
                        var numArray = [];
                        for (var i = 0; i < messageContents.length; i++) {
                          var sig = 10000;
                          var max = 0.3;
                            numArray.push(Math.floor(Number(messageContents[i]) / max * sig) / sig)
                        }
                        //sensor.value = numArray
                        sensor.addReading(numArray);

                        //empty function causes a digest
                        //effectively updating all clients
                        $timeout(function () {});
                    }
                } else {
                    console.log("Received reading without 'sensor' present at the correct level")
                }
            } else {
                console.log("Received reading without 'group' present at the correct level")
            }
        } else {
            console.log('Received reading with invalid topic length')
        }
    }

    // called when the client loses its connection
    function onConnectionLost(responseObject) {
        if (responseObject.errorCode !== 0) {
            console.log('onConnectionLost:' + responseObject.errorMessage)
        }
        service.public.state.connected = false;
        service.public.groups = {};
        //empty function causes a digest
        //effectively updating all clients
        $timeout(function () {});
    }

    return service.public;
}])
