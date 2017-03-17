import React, {Component} from 'react';
import {connect} from 'react-redux';
import Radium from 'radium';
import {push} from 'react-router-redux';

import SensorService from './SensorService';
import ConnectionPage from './components/ConnectionPage';
import SensorPage from './components/SensorPage';

const PADDING = 16;
let styles = {
    base: {
        height: '100%',
        minHeight: 400,
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'stretch'
    },
    topBar: {
        background: 'rgba(100, 100, 100, 0.4)'
    },
    topBarTitle: {
        paddingLeft: PADDING,
        paddingTop: PADDING,
        paddingRight: PADDING,
        color: 'white',
        fontSize: 40
    },
    linkBar: {
        display: 'flex',
        flexDirection: 'row',
        height: 32
    },
    linkButton: {
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        paddingLeft: PADDING,
        paddingRight: PADDING,
        ':hover' : {
            background: 'rgba(255, 255, 255, 0.1)'
        }
    },
    link: {
        color: 'white'
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
        <div style={[styles.topBar]}>
            <div style={[styles.topBarTitle]}>Web Console</div>
            <div style={[styles.linkBar]}>
                <span style={[styles.linkButton]} key={'linkConnection'} onClick={this.props.handleConnectionLink}>
                    <div style={[styles.link]}>Connection</div>
                </span>
                <span style={[styles.linkButton]} key={'linkSensors'} onClick={this.props.handleSensorsLink}>
                    <div style={[styles.link]}>Sensors</div>
                </span>
            </div>
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
        dispatch,
        handleConnectionLink: (event) => {
            dispatch(push(ConnectionPage.url()));
        },
        handleSensorsLink: (event) => {
            dispatch(push(SensorPage.url()));
        }
    };
};

const App = connect(mapStateToProps, mapDispatchToProps)(AppView);

export default App;
