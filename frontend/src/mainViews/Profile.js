import React from 'react';

import { BrowserRouter as Router, Route, Link } from "react-router-dom";
import { connect } from "react-redux";

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
        userId: state.userId,
        userName: state.userName
    }
}

const getUserInfo = async (id) => {
    const user = await fetch(`http://localhost:9000/api/user/${id}`);
    const userJson = await user.json();
    return userJson;
}

class Profile extends React.Component {
    constructor(props){
        super(props);
        this.state = {id: this.props.userId, name: '', email: this.props.userName, orders: []};
    }


    componentDidMount(){
        getUserInfo(this.state.id).then(u => {
            this.setState({name: u.name, email: u.email, orders: u.orders});
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
                        {/* <Row>
                            <Col><hr></hr></Col>
                        </Row> */}
                    </Tab>
                </Tabs>
            </Container>
            </>
        );
    }
}

const ConnectProfile = connect(select)(Profile)
export default ConnectProfile;