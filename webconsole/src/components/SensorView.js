import React, {Component, PropTypes} from 'react';
import Radium from 'radium';

import SensorPanel from './SensorPanel';

let styles = {
    base: {
    }
};

@Radium
class SensorView extends Component {
    constructor(props) {
        super(props);
    }

    componentWillMount() {
        if (!this.props.isConnected) {
            this.context.router.push('/');
        }
    }

  render() {
    return (<div style={[styles.base]}>
      SensorView
      {this.props.sensors.map((sensor) => {
          return (<SensorPanel key={sensor.id} {...sensor} />);
      })}
    </div>);
  }
}

SensorView.contextTypes = {router : React.PropTypes.object};

SensorView.propTypes = {
    sensors: PropTypes.array.isRequired
};

export default SensorView;
