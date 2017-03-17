import './index.css'

import React from 'react';
import {render} from 'react-dom';

import { Provider } from 'react-redux';
import { createStore, applyMiddleware, combineReducers } from 'redux';
import { Router, Route, IndexRoute, browserHistory } from 'react-router';
import { syncHistoryWithStore, routerMiddleware} from 'react-router-redux';

import App from './App';
import reducers from './reducers/reducers';
import SensorPage from './components/SensorPage';
import ConnectionPage from './components/ConnectionPage';

const middleware = routerMiddleware(browserHistory);
const store = createStore(
    reducers,
  window.devToolsExtension && window.devToolsExtension(),
  applyMiddleware(middleware)
);

// Create an enhanced history that syncs navigation events with the store
const history = syncHistoryWithStore(browserHistory, store);

render(
  <Provider store={store}>
    <Router history={history}>
      <Route path="/" component={App}>
          <IndexRoute component={ConnectionPage} />
          <Route path={SensorPage.route} component={SensorPage} />
      </Route>
    </Router>
  </Provider>,
  document.querySelector('#app')
);
