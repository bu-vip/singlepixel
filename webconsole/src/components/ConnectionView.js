import React, {Component, PropTypes} from 'react';
import Radium from 'radium';

let styles = {
    base: {
    }
};

@Radium
class ConnectionView extends Component {
    constructor(props) {
        super(props);
    }

  render() {
    return (<div style={[styles.base]}>
      Connectionview
    </div>);
  }
}

ConnectionView.propTypes = {
};

export default ConnectionView;
