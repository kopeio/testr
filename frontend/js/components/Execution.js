import React from 'react';
import Relay from 'react-relay';
import { Link } from 'react-router';

class Execution extends React.Component {
  render() {
    return (
      <div>
        <h1>Execution</h1>
<h2>
<Link to={`/job/${this.props.job.name}`}>{this.props.job.name}</Link><br/>
<a href={this.props.job.execution.githubUrl}>{this.props.job.execution.revision}</a><br/>
{this.props.job.execution.timestamp}<br/>
{this.props.job.execution.success ? "SUCCESS" : "FAIL"}
</h2>
<ul>
  {this.props.job.execution.artifacts.map(a =>
    <li key={a.path}>
      {a.path} {a.size}
      </li>
  )}

</ul>
<div>
  {this.props.job.execution.log.map(l =>
    <pre key={l.id}>{l.line}</pre>
  )}

</div>

      </div>
    );
  }
}

export default Relay.createContainer(Execution, {
  initialVariables: {
    revision: null,
    timestamp: null
  },

  fragments: {
    job: () => Relay.QL`
      fragment on Job {
        id,
        name,
        execution(revision: $revision, timestamp: $timestamp) {
          id,
          revision,
          timestamp,
          githubUrl,
          success,
          artifacts {
            path,
            size
          },
          log {
            line
          }
        }
      }
    `,
  },
});
