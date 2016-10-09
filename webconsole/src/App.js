import React, {Component} from 'react';
import {connect} from 'react-redux';
import Radium from 'radium';

import SensorService from './SensorService';

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

        this.sensorService = new SensorService();
    }

  render() {
    return (<div style={[styles.base]}>
        <div>
            <h1>Web Console</h1>
        </div>
        {/*This will be replaced by react router */}
        {this.props.children}
    </div>);
  }
}

AppView.contextTypes = {router : React.PropTypes.object};

const mapStateToProps = (state, ownProps) => {
    return {};
};

const mapDispatchToProps = (dispatch) => { return {
}; };

const App = connect(mapStateToProps, mapDispatchToProps)(AppView);

export default App;
