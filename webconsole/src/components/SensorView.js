import React, {Component, PropTypes} from 'react';
import Radium from 'radium';

import SensorPanel from './SensorPanel';
import ConnectionPage from './ConnectionPage';

let styles = {
    base: {
    },
    groupBody: {
        display: 'flex',
        justifyContent: 'flex-start',
        alignItems: 'flex-start',
        flexWrap: 'wrap',
        alignContent: 'flex-start'
    }
};

@Radium
class SensorView extends Component {
    constructor(props) {
        super(props);
    }

    componentWillMount() {
    }

  render() {
      let content;
      if (this.props.isConnected) {
          content = this.props.groups.map((group) => {
              return (<div key={group.id}>
                  <h2>Group: {group.id}</h2>
                  <div style={[styles.groupBody]}>
                  {group.sensors.map((sensor) => {
                      return (<SensorPanel key={sensor.id} sensor={sensor} />);
                })}
            </div>
            </div>);
        });
    } else {
        content = (<p>Not connected.</p>)
    }

    return (<div style={[styles.base]}>
      {content}
    </div>);
  }
}

SensorView.contextTypes = {router : React.PropTypes.object};

SensorView.propTypes = {
    groups: PropTypes.array.isRequired
};

export default SensorView;
