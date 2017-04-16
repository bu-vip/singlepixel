import "./App.css";

import React, {Component} from "react";
import {defaults, Line} from "react-chartjs-2";
import {sendGetStateRequest} from "./api";

// Disable animating charts by default.
defaults.global.animation = false;

class App extends Component {
    constructor(props) {
        super(props);

        let timer = setInterval(() => {
            sendGetStateRequest().then((sensors) => {
                this.setState({
                    sensors: sensors
                });
            }).catch(error => {
                if (this.state.timer) {
                    clearInterval(this.state.timer);
                    this.setState({
                        timer: null
                    });
                }
            });
        }, 10);
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
            },
            timer: timer
        };

    }

    render() {
        // Show an error if state request fails for any reason
        if (!this.state.timer) {
            return <div>An error occurred, please refresh the page</div>
        }

        // Convert people into data points
        let datasets = this.state.sensors.occupants.map((person) => {
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
                        min: this.state.sensors.bounds.minX,
                        max: this.state.sensors.bounds.maxX,
                    }
                }],
                yAxes: [{
                    ticks: {
                        min: this.state.sensors.bounds.minY,
                        max: this.state.sensors.bounds.maxY,
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
            <pre>
                {JSON.stringify(this.state.sensors, null, 2)}
            </pre>
        </div>);
    }
}

export default App
