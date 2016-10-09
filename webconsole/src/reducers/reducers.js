import {routerReducer} from 'react-router-redux';
import {combineReducers} from 'redux';

import {Map} from 'immutable';

function sensors(state = new Map(), action) {
  return state;
}

function isConnected(state = false, action) {
  return state;
}

const reducers = combineReducers({
  sensors,
  isConnected,
  routing : routerReducer
});
export default reducers;
