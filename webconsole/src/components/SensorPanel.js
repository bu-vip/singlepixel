import React, {Component, PropTypes} from 'react';
import Radium from 'radium';
import {Line} from 'react-chartjs-2';

let styles = {
    base: {
    }
};

@Radium
class SensorPanel extends Component {
    constructor(props) {
        super(props);
    }

  render() {
    let chartData = {
      labels: this.state.labels,
      datasets: [
        {
          fillColor: "rgba(220,220,220,0.2)",
          strokeColor: "rgba(220,220,220,1)",
          pointColor: "rgba(220,220,220,1)",
          pointStrokeColor: "#fff",
          data: this.state.data
        }
      ]
    };

    let options = {
        animation: {
            duration: 0
        }
    };


    return (<div style={[styles.base]}>
        <Line data={chartData} options={options} />
    </div>);
  }
}

SensorPanel.propTypes = {
  id: PropTypes.string.isRequired,
  labels: PropTypes.array.isRequired,
  data: PropTypes.array.isRequired
};

export default SensorPanel;
