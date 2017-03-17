import {Record, List} from 'immutable';

export const Sensor = Record({
  id : undefined,
  data: new List()
});
export default Sensor;
