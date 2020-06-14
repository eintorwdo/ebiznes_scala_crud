import React from 'react';
import Form from 'react-bootstrap/Form';
import Button from 'react-bootstrap/Button';
import Container from 'react-bootstrap/Container';
import Col from 'react-bootstrap/Col';

import { Redirect } from "react-router-dom";
import { connect } from "react-redux";
import {hideLogin, logIn, logOut} from '../actions/index.js';

function mapDispatchToProps(dispatch){
    return {
        hideLogin: () => dispatch(hideLogin()),
        login: (payload) => dispatch(logIn(payload))
    }
}

class Register extends React.Component {
    constructor(props){
        super(props);
        this.state = {redirect: null}
    }

    handleSubmit = async (e) => {
        e.preventDefault();
        const payload = {
            firstname: e.target[0].value,
            lastname: e.target[1].value,
            email: e.target[2].value,
            password: e.target[3].value
        }
        const res = await fetch('http://localhost:9000/auth/signup', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        });
        if(res.status === 200){
            const body = await res.json();
            localStorage.setItem('token', body.token);
            localStorage.setItem('tokenExpiry', parseInt(body.tokenExpiry));
            localStorage.setItem('email', body.email);
            localStorage.setItem('role', body.role);
            this.props.hideLogin();
            this.props.login(body);
            this.setState({redirect: '/'});
        }
        else{
            alert(`Error while signing up: ${res.statusText}`);
        }
    }

    render(){
        if(!this.state.redirect){
            return(
                <>
                <Container fluid className="main mt-4 p-4">
                <Form className="d-flex justify-content-center" onSubmit={e => this.handleSubmit(e)}>
                    <Col xs={6}>
                    <Form.Group>
                        <Form.Label>First name</Form.Label>
                        <Form.Control placeholder="Enter first name" />
                    </Form.Group>
                    <Form.Group>
                        <Form.Label>Last name</Form.Label>
                        <Form.Control placeholder="Enter last name" />
                    </Form.Group>
                    <Form.Group controlId="formBasicEmail">
                        <Form.Label>Email address</Form.Label>
                        <Form.Control type="email" placeholder="Enter email" />
                    </Form.Group>
                    <Form.Group controlId="formBasicPassword">
                        <Form.Label>Password</Form.Label>
                        <Form.Control type="password" placeholder="Password" />
                    </Form.Group>
                    <Button variant="primary" type="submit">
                        Submit
                    </Button>
                    </Col>
                </Form>
                </Container>
                </>
            )
        }
        else{
            return <Redirect to={{pathname: this.state.redirect}} />
        }
    }
}

const ConnectRegister = connect(null,mapDispatchToProps)(Register)
export default ConnectRegister;