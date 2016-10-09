module.exports = {
  type: 'react-app',
  karma: {
    browsers: ['Chrome']
  },
  webpack: {
    extra: {
      devtool: '#inline-source-map'
    }
  }
}
