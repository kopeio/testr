import React from 'react';
import Relay from 'react-relay';
import { Link } from 'react-router';

class Job extends React.Component {
  render() {
    return (
      <div>
        <h1>Job</h1>
<h2>
      {this.props.job.name}
</h2>

<ul>
  {this.props.job.executions.map(e =>
    <li key={e.id}>
      <Link to={`/job/${this.props.job.name}/${e.revision}/${e.timestamp}`}>{e.revision} {e.timestamp}</Link>
      {e.success ? "SUCCESS" : "FAIL"}
    </li>
  )}

</ul>
      </div>
    );
  }
}

export default Relay.createContainer(Job, {
  fragments: {
    job: () => Relay.QL`
      fragment on Job {
        id,
        name,
        executions {
          revision,
          timestamp,
          success
        }
      }
    `,
  },
});
