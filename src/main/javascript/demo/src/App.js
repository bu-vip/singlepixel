import "./App.css";

import React, {Component} from "react";
import {defaults, Line} from "react-chartjs-2";

// Disable animating charts by default.
defaults.global.animation = false;

class App extends Component {
  constructor(props) {
    super(props);

    this.state = {
      bounds: {
        minX: 0,
        maxX: 5,
        minY: 0,
        maxY: 5
      },
      occupants: [
        {
          id: 1,
          position: {
            x: 3.4,
            y: 2.7
          }
        },
        {
          id: 2,
          position: {
            x: 1.4,
            y: 4.1
          }
        }
      ]
    };
  }

  render() {

    let datasets = this.state.occupants.map((person) => {
      return {
        label: 'Person ' + person.id,
        showLine: false,
        fill: false,
        pointRadius: 20,
        pointHoverRadius: 20,
        pointBackgroundColor: 'rgba(0, 0, 200, 1)',
        data: [
          person.position
        ]
      };
    });

    let data = {
      datasets: datasets
    };

    let options = {
      scales: {
        xAxes: [{
          type: 'linear',
          position: 'bottom',
          ticks: {
            min: this.state.bounds.minX,
            max: this.state.bounds.maxX,
          }
        }],
        yAxes: [{
          ticks: {
            min: this.state.bounds.minY,
            max: this.state.bounds.maxY,
          }
        }]
      },
      legend: {
        display: false
      }
    };

    return (<div>
      <h1>Hello</h1>
      <Line
          data={data}
          width={100}
          height={50}
          options={options}
      />
    </div>);
  }
}

export default App
