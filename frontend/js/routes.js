import React from 'react';
import {IndexRoute, Route} from 'react-router';
import Relay from 'react-relay';

import App from './components/App';
import Job from './components/Job';
import Execution from './components/Execution';
import JobList from './components/JobList';


const ViewerQueries = {
  viewer: () => Relay.QL`query { viewer }`
};

const JobQueries = {
  job: () => Relay.QL`query { job(id: $jobId) }`
}

const ExecutionQueries = {
  job: () => Relay.QL`query { job(id: $jobId) }`
}

function prepareExecutionParams(params, route) {
  return {
    ...params,
    revision: params.revision,
    timestamp: params.timestamp
  };
};


export default (
  <Route
    path="/" component={App}
    queries={ViewerQueries}
  >
  <IndexRoute
     component={JobList}
     queries={ViewerQueries}
   />
   <Route
     path="job/:jobId" component={Job}
     queries={JobQueries}
   />
   <Route
     path="job/:jobId/:revision/:timestamp" component={Execution}
     prepareParams={prepareExecutionParams}
    queries={ExecutionQueries}
   />
  </Route>
);

/*

    <Route
      path="job/:jobId" component={Job}
      queries={ViewerQueries}
    />
*/
/*

  <Route
    path="/" component={TodoApp}
    queries={ViewerQueries}
  >
    <IndexRoute
      component={TodoList}
      queries={ViewerQueries}
      prepareParams={() => ({status: 'any'})}
    />
    <Route
      path=":status" component={TodoList}
      queries={ViewerQueries}
    />
  </Route>
);
*/
