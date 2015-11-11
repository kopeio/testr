module.exports = {
  entry: './js/app.js',
  module: {
    loaders: [
      {
        exclude: /node_modules/,
        loader: 'babel',
        query: {stage: 0, plugins: ['./build/babelRelayPlugin']},
        test: /\.js$/,
      }
    ]
  },
  output: {filename: 'public/[name].js'}
};
