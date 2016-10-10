import 'paho-mqtt';

import {push} from 'react-router-redux';

import {connected, disconnected, sensorDataReceived} from './actions/actions';
import ConnectionPage from './components/ConnectionPage';
import SensorPage from './components/SensorPage';
import SensorReading from './components/SensorReading';

class SensorService {
  constructor(dispatch) { this.dispatch = dispatch; }

  connect = (host, port, prefix) => {
    // process the prefix
    if (prefix) {
      // ensure the prefix starts with "/"
      if (!prefix.startsWith('/')) {
        prefix = '/' + prefix;
      }
      this.destination = prefix + '/#';
      this.prefixSize = prefix.split('/').length;
    } else {
      this.prefixSize = 0;
      this.destination = '/#';
    }

    this.client = new Paho.MQTT.Client(
        host, Number(port), "webconsole-" + Math.floor(Math.random() * 10000));

    this.client.onConnectionLost = this.onConnectionLost;
    this.client.onMessageArrived = this.onMessageArrived;

    this.client.connect({onSuccess : this.onConnect});
  };

  disconnect = () => { this.client.disconnect(); }

  onConnect = () => {
    console.log("Connected");
    this.client.subscribe(this.destination);
    this.dispatch(connected());
    this.dispatch(push(SensorPage.url()));
  };

  onConnectionLost = (responseObject) => {
    if (responseObject.errorCode !== 0) {
      console.log("onConnectionLost:" + responseObject.errorMessage);
    }
    this.dispatch(disconnected());
    this.dispatch(push(ConnectionPage.url()));
  };

  onMessageArrived = (message) => {
    console.log("onMessageArrived:" + message.payloadString);
    // Topic example:
    //    /<prefix>/group/<group-id>/sensor/<sensor-id>
    // Split the topic into it's levels
    let levels = message.destinationName.split('/');
    // Splice off the empty "" before the first "/"
    levels = levels.slice(1);

    // basic checking for invalid topics
    if (levels.length == this.prefixSize + 4) {
      if (levels[this.prefixSize] == 'group') {
        if (levels[this.prefixSize + 2] == 'sensor') {
          let groupId = levels[this.prefixSize + 1];
          let sensorId = levels[this.prefixSize + 3];

          // Parse the message contents
          // Message contents should be CSV of 4 nums
          // e.g. "123, 456, 789, 149"
          let messageContents = message.payloadString.split(',');
          if (messageContents.length != 6) {
            console.log('Invalid sensor reading size');
          } else {
            // Create a new sensor reading
            let reading = new SensorReading({
              red : Number(messageContents[0]),
              green : Number(messageContents[1]),
              blue : Number(messageContents[2]),
              white : Number(messageContents[3]),
              timestamp : Number(messageContents[4])
            });

            this.dispatch(sensorDataReceived(groupId, sensorId, reading));
          }

        } else {
          console.log(
              "Received reading without 'sensor' present at the correct level");
        }
      } else {
        console.log(
            "Received reading without 'group' present at the correct level");
      }
    } else {
      console.log('Received reading with invalid topic length');
    }
  };
}

export default SensorService;
