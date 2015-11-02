import React from 'react';
import Relay from 'react-relay';
import { Link } from 'react-router';

class JobList extends React.Component {
  render() {
    return (
      <div>
        <h1>Jobs</h1>
        <ul>
          {this.props.viewer.jobs.map(j =>
            <li key={j.id}>
              <Link to={`/job/${j.name}`}>{j.name}</Link>
            </li>
          )}

        </ul>
      </div>
    );
  }
}

export default Relay.createContainer(JobList, {
  fragments: {
    viewer: () => Relay.QL`
      fragment on User {
        jobs {
          name,
          repo,
          id
        },
      }
    `,
  },
});
