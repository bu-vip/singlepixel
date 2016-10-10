import Radium from 'radium';
import React, {Component, PropTypes} from 'react';

let styles = {base : {}};

@Radium
class ConnectionView extends Component {
  constructor(props) {
    super(props);

    this.state = { host : 'localhost', port : 61614, prefix : '' }
  }

  handleHostChange = (event) => {
    this.setState({
      host : event.target.value,
    });
  };

  handlePortChange = (event) => {
    this.setState({
      port : event.target.value,
    });
  };

  handlePrefixChange = (event) => {
    this.setState({
      prefix : event.target.value,
    });
  };

  handleConnect = (event) => {
    console.log("Connecting...");
    this.props.sensorService.connect(this.state.host, this.state.port,
                                     this.state.prefix);
  };

  handleDisconnect = (event) => {
    console.log("Disconnecting...");
    this.props.sensorService.disconnect();
  };

  render() {
    if (!this.props.connected) {
        return (<div style={[styles.base]}>
            Host:
            <input
                type = "text"
                value = { this.state.host }
                onChange =
                {
                    this.handleHostChange
                } />
                Port:
                <input
                    type="number"
                    value={this.state.port}
                    onChange={this.handlePortChange}/ >
                    Prefix: < input
                    type = "text"
                    value={
                        this.state.prefix}
                        onChange={
                            this.handlePrefixChange}/>
                        <button onClick={this.handleConnect}>Connect</button>
                        Connectionview
                    </div>);
    } else {
        return (<button onClick={this.handleDisconnect}>Disconnect</button>)
    }
  }
}

ConnectionView.contextTypes = {
  router : React.PropTypes.object
};

ConnectionView.propTypes = {};

export default ConnectionView;
