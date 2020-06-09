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

function select(state){
    return {
        loggedIn: state.loggedIn,
        userName: state.userName,
        token: state.token
    }
}

const getUserInfo = async (props) => {
    const res = await fetch('http://localhost:9000/api/user', {
        headers: {
            'X-Auth-Token': props.token
        }
    });
    const user = await res.json();
    return [user, res.status];
}

class Profile extends React.Component {
    constructor(props){
        super(props);
        const error = this.props.loggedIn ? null : "You must be logged in to view this page";
        this.state = {id: null, name: '', email: this.props.userName, orders: [], loggedIn: this.props.loggedIn, error};
    }


    componentDidMount(){
        getUserInfo(this.props).then(u => {
            console.log(u)
            if(u[1] === 200){
                this.setState({id: u[0].id, name: u[0].name, email: u[0].email, orders: u[0].orders});
            }
            else{
                this.setState({error: u[0].message});
            }
        });
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

        if(this.state.loggedIn && !this.state.error){
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
            return <Redirect to={{pathname: "/error", state: this.state.error}}/>
        }
    }
}

const ConnectProfile = connect(select)(Profile)
export default ConnectProfile;