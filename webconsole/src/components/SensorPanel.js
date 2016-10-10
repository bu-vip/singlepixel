import React, {Component, PropTypes} from 'react';
import Radium from 'radium';
import {Line} from 'react-chartjs-2';

let styles = {
    base: {
        width: '20%',
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
    let data = {
        red: [],
        green: [],
        blue: [],
        white: []
    };
    this.props.sensor.data.forEach((reading) => {
        labels.push(reading.timestamp);
        data.red.push(reading.red);
        data.green.push(reading.green);
        data.blue.push(reading.blue);
        data.white.push(reading.white);
    });

    let colors = {
        red: 'rgba(255, 0, 0, 0.3)',
        green: 'rgba(0, 255, 0, 0.3)',
        blue: 'rgba(0, 0, 255, 0.3)',
        white: 'rgba(100, 100, 100, 0.3)'
    }

    let datasets = [];
    for (let i = 0; i < this.props.channels.length; i++) {
        let channel = this.props.channels[i];
        datasets.push({
            fill: true,
            borderColor: colors[channel],
            backgroundColor: colors[channel],
            pointBackgroundColor: colors[channel],
            pointBorderColor: colors[channel],
            data: data[channel]
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
        },
        scales: {
            yAxes: [{
                ticks: {
                    beginAtZero: true,
                    max: 0.5,
                    min: 0
                }
            }]
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
