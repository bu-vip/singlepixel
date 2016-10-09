import mqtt from 'mqtt';

class SensorService {
  constructor() {

  }

  connect = (host) => {
    this.client = mqtt.connect(host);
    this.client.on('*', this.messageArrived);
  };

  messageArrived = (topic, message) => {
    console.log(topic);
    console.log(message);
  };

  disconnect = () => {
    this.client.end();
  }
}

export default SensorService;
