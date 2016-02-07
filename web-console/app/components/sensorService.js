var sensorService = angular.module('sensorService', [])

sensorService.factory('SensorService', [ function () {
  var service = {
    public: {
      groups: {},
    },
    onUpdateListeners: {}
  }

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

    // called when the client connects
    function onConnect () {
      // Once a connection has been made, make a subscription and send a message.
      console.log('onConnect')
      service.client.subscribe(service.destination)

    /*
    message = new Paho.MQTT.Message("Hello")
    message.destinationName = "/World"
    client.send(message)
    */
    }

    // called when a message arrives
    function onMessageArrived (message) {
      console.log('onMessageArrived:' + message.payloadString)

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
              service.public.groups[groupId] = {
                id: groupId,
                sensors: {}
              }
            }
            // get group
            var group = service.public.groups[groupId]

            // guranteed to have the group now
            // get the sensor id
            var sensorId = levels[service.prefixSize + 3]

            // check if we DON'T have the sensor id
            if (!service.public.groups[groupId].sensors[sensorId]) {
              // add it
              service.public.groups[groupId].sensors[sensorId] = {
                id: sensorId,
                value: [0, 0, 0, 0],
                color: "rgb(0, 0, 0)"
              }
            }
            // get sensor
            var sensor = group.sensors[sensorId]

            // parse the message contents
            // message contents should be CSV of 4 nums
            // e.g. "123, 456, 789, 149"
            var messageContents = message.payloadString.split(',')
            // check the values are valid
            if (messageContents.length != 4) {
              console.log('Invalid sensor reading size')
            } else {
              // convert to number array
              var numArray = []
              for (var i = 0; i < messageContents.length; i++) {
                numArray.push(Number(messageContents[i]))
              }
              sensor.value = numArray
              var maxVal = 255.0;
              sensor.color = "rgb(" + (numArray[0] / maxVal * 255);
              sensor.color += ", " + (numArray[1] / maxVal * 255);
              sensor.color += ", " + (numArray[2] / maxVal * 255) + ")";

              //call all of the onUpdateListeners
              for (var listener in service.onUpdateListeners)
              {
                  service.onUpdateListeners[listener]();
              }
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
    function onConnectionLost (responseObject) {
      if (responseObject.errorCode !== 0) {
        console.log('onConnectionLost:' + responseObject.errorMessage)
      }
    }
  }


  service.public.addOnUpdateListener = function(aName, aCallback) {
    service.onUpdateListeners[aName] = aCallback;
  };

  return service.public
}])
