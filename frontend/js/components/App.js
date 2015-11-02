import React from 'react';
import Relay from 'react-relay';
import { Link } from 'react-router';

class App extends React.Component {
  static propTypes = {
   children: React.PropTypes.node.isRequired
 };

  render() {
    const { children} = this.props;
  return (
      <div>
        <h1><Link to="/">Home</Link></h1>
        {children}
      </div>
    );
  }
}

export default Relay.createContainer(App, {
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
