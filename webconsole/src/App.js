import React, {Component} from 'react';
import {connect} from 'react-redux';
import Radium from 'radium';
import {Link} from 'react-router';

import SensorService from './SensorService';
import ConnectionPage from './components/ConnectionPage';
import SensorPage from './components/SensorPage';

let styles = {
    base: {
        height: '100%',
        minHeight: 400,
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'stretch'
    }
};

@Radium
class AppView extends Component {
    constructor(props) {
        super(props);

        this.sensorService = new SensorService(this.props.dispatch);
    }

  render() {
    return (<div style={[styles.base]}>
        <div>
            <h1>Web Console</h1>
            <Link to={ConnectionPage.url()}>Connection</Link>
            <Link to={SensorPage.url()}>Sensors</Link>
        </div>
        {/*This will be replaced by react router */}
        {this.props.children && React.cloneElement(this.props.children, {
              sensorService: this.sensorService
          })}
    </div>);
  }
}

AppView.contextTypes = {
    router : React.PropTypes.object
};

const mapStateToProps = (state, ownProps) => {
    return {};
};

const mapDispatchToProps = (dispatch) => {
    return {
        dispatch
    };
};

const App = connect(mapStateToProps, mapDispatchToProps)(AppView);

export default App;
