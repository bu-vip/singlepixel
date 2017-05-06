import "./App.css";

import React, {Component} from "react";
import {defaults, Line} from "react-chartjs-2";
import {
  sendBackgroundRequest,
  sendGetStateRequest,
  sendToggleRecordingRequest
} from "./api";

// Disable animating charts by default.
defaults.global.animation = false;

// Turns off the timer, console won't make status requests
const DEBUG_NO_UPDATE = false;

class App extends Component {
  constructor(props) {
    super(props);

    let timer;
    if (!DEBUG_NO_UPDATE) {
      timer = setInterval(() => {
        sendGetStateRequest().then((sensors) => {
          this.setState({
            sensors: sensors
          });
        }).catch(error => {
          console.log(error);
          if (this.state.timer) {
            clearInterval(this.state.timer);
            this.setState({
              timer: null
            });
          }
        });
      }, 10);
    }
    this.state = {
      sensors: {
        bounds: {
          minX: 0,
          maxX: 5,
          minY: 0,
          maxY: 5
        },
        occupants: [
          {
            id: 1,
            estimatedPosition: {
              x: 3.4,
              z: 2.7
            },
            truePosition: {
              x: 3.5,
              z: 2.6
            },
            distance: 1
          }
        ]
      },
      timer: timer
    };

  }

  handleBackgroundClick = (event) => {
    sendBackgroundRequest().catch(error => {
      console.log(error);
    });
  };

  handleRecordingClick = (event) => {
    sendToggleRecordingRequest().catch(error => {
      console.log(error);
    });
  };

  render() {
    // Show an error if state request fails for any reason
    if (!DEBUG_NO_UPDATE && !this.state.timer) {
      return <div>An error occurred, please refresh the page</div>
    }

    // Convert people into data points
    let datasets = [].concat.apply([],
        this.state.sensors.occupants.map((person) => {
          const basePoint = {
            showLine: false,
            fill: false,
            pointRadius: 20,
            pointHoverRadius: 20,
          };

          let result = [];
          if (person.estimatedPosition) {
            result.push(
                {
                  label: 'Person ' + person.id + " estimated",
                  ...basePoint,
                  pointBackgroundColor: 'rgba(200, 0, 0, 1)',
                  data: [
                    {
                      x: person.estimatedPosition.x,
                      y: person.estimatedPosition.z
                    }
                  ]
                });
          }
          if (person.truePosition) {
            result.push(
                {
                  label: 'Person ' + person.id + " true",
                  ...basePoint,
                  pointBackgroundColor: 'rgba(0, 0, 200, 1)',
                  data: [
                    {
                      x: person.truePosition.x,
                      y: person.truePosition.z
                    }
                  ]
                }
            );
          }

          // Return two data sets, one for true and one for estimated
          return result;
        }));

    let data = {
      datasets: datasets
    };

    let options = {
      scales: {
        xAxes: [{
          type: 'linear',
          position: 'bottom',
          ticks: {
            min: this.state.sensors.bounds.minX,
            max: this.state.sensors.bounds.maxX,
          }
        }],
        yAxes: [{
          ticks: {
            min: this.state.sensors.bounds.minZ,
            max: this.state.sensors.bounds.maxZ,
          }
        }]
      },
      legend: {
        display: false
      }
    };

    return (<div>
      <h1>Demo</h1>
      <Line
          data={data}
          options={options}
      />
      <button onClick={this.handleBackgroundClick}>Background</button>
      <button onClick={this.handleRecordingClick}>Recording</button>
      <pre>
         {JSON.stringify(this.state.sensors, null, 2)}
      </pre>
    </div>);
  }
}

export default App
