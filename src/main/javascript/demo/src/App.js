import "./App.css";

import React, {Component} from "react";
import {defaults, Line} from "react-chartjs-2";

// Disable animating charts by default.
defaults.global.animation = false;

class App extends Component {
  constructor(props) {
    super(props);

    this.state = {
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

    let data = {
      datasets: [{
        label: 'Scatter Dataset',
        fill: false,
        showLine: false,
        data: [
          {
            x: -10,
            y: 0
          },
          {
            x: 0,
            y: 10
          },
          {
            x: 10,
            y: 5
          }
        ],
      }]
    };

    let options = {
      scales: {
        xAxes: [{
          type: 'linear',
          position: 'bottom'
        }]
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
