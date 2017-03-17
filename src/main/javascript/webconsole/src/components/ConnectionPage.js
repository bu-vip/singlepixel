import {connect} from 'react-redux';

import ConnectionView from './ConnectionView';

const mapStateToProps =
    (state, ownProps) => { return {connected : state.isConnected}; };

const mapDispatchToProps = (dispatch) => { return {}; };

const ConnectionPage =
    connect(mapStateToProps, mapDispatchToProps)(ConnectionView);

const baseUrl = '/';
ConnectionPage.route = baseUrl;
ConnectionPage.url = () => baseUrl;

export default ConnectionPage;
