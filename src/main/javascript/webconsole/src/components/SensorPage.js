import {connect} from 'react-redux';

import SensorView from './SensorView';

const mapStateToProps = (state, ownProps) => {
  let groups = [];
  state.sensors.forEach((groupMap, groupId) => {
    let sensors = [];
    groupMap.forEach((sensor) => { sensors.push(sensor); });
    sensors.sort((a, b) => {
      return a.id.localeCompare(b.id);
    })
    groups.push({id : groupId, sensors});
  });

  return {groups, isConnected : state.isConnected};
};

const mapDispatchToProps = (dispatch) => { return {}; };

const SensorPage = connect(mapStateToProps, mapDispatchToProps)(SensorView);

const baseUrl = '/sensors';
SensorPage.route = baseUrl;
SensorPage.url = () => baseUrl;

export default SensorPage;
