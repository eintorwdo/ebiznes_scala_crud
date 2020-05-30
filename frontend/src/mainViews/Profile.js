import React from 'react';

import { BrowserRouter as Router, Route, Link } from "react-router-dom";

import Row from 'react-bootstrap/Row'
import Col from 'react-bootstrap/Col'
import Container from 'react-bootstrap/Container'
import Tabs from 'react-bootstrap/Tabs'
import Tab from 'react-bootstrap/Tab'

const getUserInfo = async (id) => {
    const user = await fetch(`http://localhost:9000/api/user/${id}`);
    const userJson = await user.json();
    return userJson;
}

class Profile extends React.Component {
    constructor(props){
        super(props);
        this.state = {id: 1, name: '', email: '', orders: []};
    }


    componentDidMount(){
        getUserInfo(this.state.id).then(u => {
            this.setState({name: u.name, email: u.email, orders: u.orders});
        });
    }

    render(){
        const orders = this.state.orders.map(o => {
            return (
                <li className="pt-1 pb-1 orderListItem">
                    <Link to={`/order/${o.id}`}>
                        <Row className="d-flex justify-content-start">
                            <Col><h5>Id: {o.id}</h5></Col>
                        </Row>
                        <Row className="d-flex justify-content-start">
                            <Col><h5>Price: {o.price}</h5></Col>
                        </Row>
                        <Row className="d-flex justify-content-start">
                            <Col><h5>Ordered: {o.date}</h5></Col>
                        </Row>
                        <Row>
                            <Col><hr></hr></Col>
                        </Row>
                    </Link>
                </li>
            );
        });
        return(
            <>
            <Container className="productListItem mt-3 pt-1 pb-1" fluid>
                <Tabs defaultActiveKey="orders" id="uncontrolled-tab-example">
                    <Tab eventKey="orders" title="Your orders">
                        <ul className="orderList m-0 p-1">
                            {orders}
                        </ul>
                    </Tab>
                    <Tab eventKey="profile" title="Profile information" className="profile-info pt-2 pb-2">
                        <Row>
                            <Col><h5>Name: {this.state.name}</h5></Col>
                        </Row>
                        <Row>
                            <Col><h5>email: {this.state.email}</h5></Col>
                        </Row>
                        <Row>
                            <Col><hr></hr></Col>
                        </Row>
                    </Tab>
                </Tabs>
            </Container>
            </>
        )
    }
}

export default Profile;