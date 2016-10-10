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

        this.state = {
            color: 'white'
        };
    }

    componentWillMount() {
    }

    handleColorChange = (event) => {
        this.setState({
            color: event.target.value
        });
    };

  render() {
      let content;
      if (this.props.isConnected) {
          content = (<div>
              <select value={this.state.color} onChange={this.handleColorChange}>
                  <option value="red">red</option>
                  <option value="green">green</option>
                  <option value="blue">blue</option>
                  <option value="white">white</option>
              </select>

              {this.props.groups.map((group) => {
              return (<div key={group.id}>
                  <h2>Group: {group.id}</h2>
                  <div style={[styles.groupBody]}>
                  {group.sensors.map((sensor) => {
                      return (<SensorPanel key={sensor.id} sensor={sensor} channels={[this.state.color]}/>);
                })}
            </div>
            </div>);
        })}

        </div>
        );
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
