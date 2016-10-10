import React, {Component, PropTypes} from 'react';
import Radium from 'radium';
import {Line} from 'react-chartjs-2';

let styles = {
    base: {
        width: 400,
        borderStyle: 'solid',
        borderColor: 'rgba(100, 100, 100, 0.1)',
        borderWidth: 1
    }
};

@Radium
class SensorPanel extends Component {
    constructor(props) {
        super(props);
    }

  render() {

    let labels = [];
    let redData = [];
    let greenData = [];
    let blueData = [];
    let whiteData = [];
    this.props.sensor.data.forEach((reading) => {
        labels.push(reading.timestamp);
        redData.push(reading.red);
        greenData.push(reading.green);
        blueData.push(reading.blue);
        whiteData.push(reading.white);
    });
    let data = [redData, greenData, blueData, whiteData];

    let redColor = 'rgba(255, 0, 0, 0.3)';
    let greenColor = 'rgba(0, 255, 0, 0.3)';
    let blueColor = 'rgba(0, 0, 255, 0.3)';
    let whiteColor = 'rgba(100, 100, 100, 0.3)';
    let colors = [redColor, greenColor, blueColor, whiteColor];

    let datasets = [];
    for (let i = 0; i < data.length; i++) {
        datasets.push({
            fill: false,
            borderColor: colors[i],
            pointBackgroundColor: colors[i],
            pointBorderColor: colors[i],
            data: data[i]
        });
    }

    let chartData = {
        labels: labels,
      datasets: datasets
    };

    let options = {
        animation: {
            duration: 0
        },
        legend: {
            display: false
        }
    };


    return (<div style={[styles.base]}>
        <h4>Sensor: {this.props.sensor.id}</h4>
        <Line data={chartData} options={options} />
    </div>);
  }
}

SensorPanel.propTypes = {
  sensor: PropTypes.object.isRequired
};

export default SensorPanel;
