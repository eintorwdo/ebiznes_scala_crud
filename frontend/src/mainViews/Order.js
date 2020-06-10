import React from 'react';

// import { BrowserRouter as Router, Route, Link } from "react-router-dom";

import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import Table from 'react-bootstrap/Table';
import Breadcrumb from 'react-bootstrap/Breadcrumb';

import { Redirect } from "react-router-dom";
import { connect } from "react-redux";

import checkIfLoggedIn from '../utils/checkIfLoggedIn.js';
import {logOut} from '../actions/index.js';

function select(state){
    return {
        loggedIn: state.loggedIn,
        token: state.token,
        tokenExpiry: state.tokenExpiry
    }
}

function mapDispatchToProps(dispatch){
    return {
        logout: () => dispatch(logOut())
    }
}

const getOrderInfo = async (id, props) => {
    const user = await fetch(`http://localhost:9000/api/order/${id}`, {
        headers: {
            'X-auth-token': props.token
        }
    });
    const userJson = await user.json();
    return userJson;
}

class Order extends React.Component {
    constructor(props){
        super(props);
        this.state = {order: {}, details: [], loggedIn: this.props.loggedIn};
    }


    componentDidMount(){
        if(this.props.loggedIn){
            if(checkIfLoggedIn(this.props.token, this.props.tokenExpiry)){
                getOrderInfo(this.props.match.params.id, this.props).then(o => {
                    this.setState({order: o.order, details: o.details});
                });
            }
            else{
                localStorage.clear();
                this.props.logout();
            }
        }
    }

    render(){
        let orderInfo;
        let deliveryStatus;
        if(this.state.order.info){
            if(this.state.order.info.sent === 0){
                deliveryStatus = "not sent";
            }
            else if(this.state.order.info.sent === 1){
                deliveryStatus = "sent";
            }
            else if(this.state.order.info.sent === 2){
                deliveryStatus = "delivered";
            }
            orderInfo = (
                <>
                <Row>
                    <Col><h5><span className="boldy">Order id:</span> {this.state.order.info.id}</h5></Col>
                </Row>
                <Row>
                    <Col><h5><span className="boldy">Total price:</span> {this.state.order.info.price}</h5></Col>
                </Row>
                <Row>
                    <Col><h5><span className="boldy">Ordered:</span> {this.state.order.info.date}</h5></Col>
                </Row>
                <Row>
                    <Col><h5><span className="boldy">Address:</span> {this.state.order.info.address}</h5></Col>
                </Row>
                <Row>
                    <Col><h5><span className="boldy">Paid:</span> {this.state.order.info.paid ? "yes" : "no"}</h5></Col>
                </Row>
                <Row>
                    <Col><h5><span className="boldy">Package number:</span> {this.state.order.info.packageNr.length > 0 ? this.state.order.info.packageNr : "no packge number assigned"}</h5></Col>
                </Row>
                <Row>
                    <Col><h5><span className="boldy">Delivery status:</span> {deliveryStatus}</h5></Col>
                </Row>
                </>
            );
        }

        const delivery = this.state.order.delivery ? (
            <Row>
                <Col><h5><span className="boldy">Delivery provider:</span> {this.state.order.delivery.name}</h5></Col>
            </Row>
        ) : "";

        const payment = this.state.order.payment ? (
            <Row>
                <Col><h5><span className="boldy">Payment method:</span> {this.state.order.payment.name}</h5></Col>
            </Row>
        ) : "";

        const deliveryRow = this.state.order.delivery && this.state.details ? (
            <tr>
                <td>
                    {this.state.details.length + 1}
                </td>
                <td>
                    {this.state.order.delivery.name}
                </td>
                <td>
                    {this.state.order.delivery.price}
                </td>
                <td>
                    1
                </td>
            </tr>
        ) : "";

        const details = this.state.details.map((o, i) => {
            return (
                <tr>
                    <td>
                        {i+1}
                    </td>
                    <td>
                        {o.name}
                    </td>
                    <td>
                        {o.price}
                    </td>
                    <td>
                        {o.amount}
                    </td>
                </tr>
            );
        });
        if(this.state.loggedIn){
            return(
                <>
                <Container className="productListItem mt-3 p-3 order-info" fluid>
                    <Breadcrumb>
                        <Breadcrumb.Item href="/">Home</Breadcrumb.Item>
                        <Breadcrumb.Item href="/profile">
                            Profile
                        </Breadcrumb.Item>
                        <Breadcrumb.Item active>Order</Breadcrumb.Item>
                    </Breadcrumb>
                    <Row>
                        <Col md={12} lg={6}>
                                {orderInfo}
                                {delivery}
                                {payment}
                                <hr className="hr-md"></hr>
                        </Col>
                        <Col>
                            <Row className="pl-3 pr-3 pt-0 pb-0">
                                <h4 className="mb-0">Order details:</h4>
                                <Table striped bordered hover className="mt-2">
                                    <thead>
                                        <tr>
                                            <th>#</th>
                                            <th>Product name</th>
                                            <th>Price</th>
                                            <th>Amount</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {details}
                                        {deliveryRow}
                                    </tbody>
                                </Table>
                            </Row>
                        </Col>
                    </Row>
                </Container>
                </>
            );
        }
        else{
            return <Redirect to={{pathname: "/error", state: "You must log in to view this page"}}/>
        }
    }
}

const ConnectOrder = connect(select, mapDispatchToProps)(Order)
export default ConnectOrder;