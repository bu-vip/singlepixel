import {routerReducer} from 'react-router-redux';
import {combineReducers} from 'redux';
import {Map} from 'immutable';

import * as ACTIONS from '../actions/actions';
import Sensor from '../components/Sensor';

function sensors(state = new Map(), action) {
  switch (action.type) {
    case ACTIONS.SENSOR_DATA_RECEIVED:
      // Check if group already exists
      if (!state.has(action.groupId)) {
        state = state.set(action.groupId, new Map());
      }
      let groupMap = state.get(action.groupId);

      // Check if sensor already exists
      if (!groupMap.has(action.sensorId)) {
        groupMap = groupMap.set(action.sensorId, new Sensor({id: action.sensorId}));
      }
      let sensor = groupMap.get(action.sensorId);

      // Add data to sensor
      let data = sensor.data;
      data = data.push(action.reading);
      // Only keep X measurements
      const MAX_READINGS = 30;
      if (data.size > MAX_READINGS) {
        data = data.slice(data.size - MAX_READINGS, data.size);
      }
      sensor = sensor.set('data', data);

      // Put new objects into the state
      groupMap = groupMap.set(sensor.id, sensor);
      state = state.set(action.groupId, groupMap);

      break;
    case ACTIONS.DISCONNECTED:
      // On disconnect, clear all sensor data
      state = new Map();
      break;
    default:
      break;
  }

  return state;
}

function isConnected(state = false, action) {
  switch (action.type) {
    case ACTIONS.CONNECTED:
      state = true;
      break;
    case ACTIONS.DISCONNECTED:
      state = false;
      break;
    default:
      break;
  }

  return state;
}

const reducers = combineReducers({
  sensors,
  isConnected,
  routing : routerReducer
});
export default reducers;
