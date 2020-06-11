import React from 'react';

import { BrowserRouter as Router, Route, Link } from "react-router-dom";
import { connect } from "react-redux";
import { Redirect } from "react-router-dom";

import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import Tabs from 'react-bootstrap/Tabs';
import Tab from 'react-bootstrap/Tab';
import Table from 'react-bootstrap/Table';
import Button from 'react-bootstrap/Button';
import Breadcrumb from 'react-bootstrap/Breadcrumb';

import checkIfLoggedIn from '../utils/checkIfLoggedIn.js';
import {logOut} from '../actions/index.js';

function select(state){
    return {
        loggedIn: state.loggedIn,
        userName: state.userName,
        token: state.token,
        tokenExpiry: state.tokenExpiry
    }
}

function mapDispatchToProps(dispatch){
    return {
        logout: () => dispatch(logOut())
    }
}

const getUserInfo = async (props) => {
    const res = await fetch('http://localhost:9000/api/user', {
        headers: {
            'X-Auth-Token': props.token
        }
    });
    const user = res.status === 200 ? await res.json() : {message: res.statusText};
    return [user, res.status];
}

class Profile extends React.Component {
    constructor(props){
        super(props);
        const error = this.props.loggedIn ? null : "You must be logged in to view this page";
        this.state = {id: null, name: '', email: this.props.userName, orders: [], loggedIn: this.props.loggedIn, error, redirect: null};
    }


    componentDidMount(){
        if(this.props.loggedIn){
            if(checkIfLoggedIn(this.props.token, this.props.tokenExpiry)){
                getUserInfo(this.props).then(u => {
                    if(u[1] === 200){
                        this.setState({id: u[0].id, name: u[0].name, email: u[0].email, orders: u[0].orders});
                    }
                    else{
                        console.log(u[0])
                        this.setState({error: u[0].message});
                    }
                });
            }
            else{
                localStorage.clear();
                this.props.logout();
            }
        }
    }

    componentDidUpdate(prevProps){
        if(this.props.loggedIn !== prevProps.loggedIn){
            this.setState({loggedIn: this.props.loggedIn, redirect: '/'});
        }
    }

    render(){
        const orderList = this.state.orders.map((o,i) => {
            return (
                <tr>
                    <td className="p-2">{i+1}</td>
                    <td className="p-2"><Link to={`/order/${o.id}`}><Button className="w-50">{o.id}</Button></Link></td>
                    <td className="p-2">{o.price}</td>
                    <td className="p-2">{o.date}</td>
                </tr>
            );
        });

        if(this.state.loggedIn){
            return(
                <>
                <Container className="productListItem mt-3 p-3" fluid>
                    <Breadcrumb>
                        <Breadcrumb.Item href="/">Home</Breadcrumb.Item>
                        <Breadcrumb.Item active>Profile</Breadcrumb.Item>
                    </Breadcrumb>
                    <Tabs defaultActiveKey="orders" id="uncontrolled-tab-example">
                        <Tab eventKey="orders" title="Your orders">
                            <Table striped bordered hover className="mt-2">
                                <thead>
                                    <tr>
                                    <th>#</th>
                                    <th>Order id</th>
                                    <th>Price</th>
                                    <th>Date of order</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {orderList}
                                </tbody>
                            </Table>
                        </Tab>
                        <Tab eventKey="profile" title="Profile information" className="profile-info pt-2 pb-2">
                            <Row>
                                <Col><h5>Name: {this.state.name}</h5></Col>
                            </Row>
                            <Row>
                                <Col>
                                    <h5>email: {this.state.email}</h5>
                                    <hr className="mt-3 mb-1"></hr>
                                </Col>
                            </Row>
                        </Tab>
                    </Tabs>
                </Container>
                </>
            );
        }
        else{
            return <Redirect to={{pathname: this.state.redirect || "/error", state: this.state.error}}/>
        }
    }
}

const ConnectProfile = connect(select, mapDispatchToProps)(Profile)
export default ConnectProfile;