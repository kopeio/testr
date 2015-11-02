import 'babel/polyfill';

import ReactRouterRelay from 'react-router-relay';
import React from 'react';
import ReactDOM from 'react-dom';
import Relay from 'react-relay';
import {Router} from 'react-router';
import {IndexRoute, Route} from 'react-router';

import App from './components/App';
import Job from './components/Job';

import { createHashHistory } from 'history'

import routes from './routes.js';


/*
import AppHomeRoute from './routes/AppHomeRoute';


ReactDOM.render(
  <Relay.RootContainer
    Component={App}
    route={new AppHomeRoute()}
  />,
  document.getElementById('root')
);
*/


/* ... */

const history = createHashHistory({queryKey: false});




ReactDOM.render((
  <Router history={history}
          createElement={ReactRouterRelay.createElement}
          routes={routes} />
), document.getElementById('root'));
