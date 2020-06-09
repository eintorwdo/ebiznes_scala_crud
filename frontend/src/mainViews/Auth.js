import React from 'react';

import { connect } from "react-redux";
import queryString from 'query-string';
import { Redirect } from "react-router-dom";
// import Container from 'react-bootstrap/Container';

import {logIn} from '../actions/index.js';

function mapDispatchToProps(dispatch){
    return {
        login: (token) => dispatch(logIn(token))
    }
}

class Auth extends React.Component {
    constructor(props){
        super(props);
        const q = queryString.parse(this.props.location.search);
        localStorage.setItem('token', q.token);
        localStorage.setItem('tokenExpiry', parseInt(q.tokenExpiry));
        localStorage.setItem('email', q.email);
        this.props.login(q);
    }

    render(){
        return(
            <>
                <Redirect to={{pathname: "/"}}/>
            </>
        )
    }
}

const ConnectAuth = connect(null, mapDispatchToProps)(Auth)
export default ConnectAuth;