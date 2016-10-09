import {connect} from 'react-redux';

import SensorView from './SensorView';

const mapStateToProps = (state, ownProps) => {
  let sensors = [];
  state.sensors.forEach((sensor) => {
    sensors.push({
      id: sensor.id,
      labels: [],
      data: []
    });
  });

  return {
    sensors,
    isConnected: state.isConnected
  };
};

const mapDispatchToProps = (dispatch) => { return {}; };

const SensorPage = connect(mapStateToProps, mapDispatchToProps)(SensorView);

const baseUrl = '/sensors';
SensorPage.route = baseUrl;
SensorPage.url = () => baseUrl;

export default SensorPage;
