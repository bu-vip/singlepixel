export const SENSOR_DATA_RECEIVED = 'SENSOR_DATA_RECEIVED';
export function sensorDataReceived(groupId, sensorId, reading) {
  return {type: SENSOR_DATA_RECEIVED, groupId, sensorId, reading};
}

export const CONNECTED = 'CONNECTED';
export function connected() {
  return {type: CONNECTED};
}

export const DISCONNECTED = 'DISCONNECTED';
export function disconnected() {
  return {type: DISCONNECTED};
}
