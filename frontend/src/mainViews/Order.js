import React from 'react';

import { BrowserRouter as Router, Route, Link } from "react-router-dom";

import Row from 'react-bootstrap/Row'
import Col from 'react-bootstrap/Col'
import Container from 'react-bootstrap/Container'
import Tabs from 'react-bootstrap/Tabs'
import Tab from 'react-bootstrap/Tab'

const getOrderInfo = async (id) => {
    const user = await fetch(`http://localhost:9000/api/order/${id}`);
    const userJson = await user.json();
    return userJson;
}

class Order extends React.Component {
    constructor(props){
        super(props);
        this.state = {order: {}, details: []};
    }


    componentDidMount(){
        getOrderInfo(this.props.match.params.id).then(o => {
            this.setState({order: o.order, details: o.details});
        });
    }

    render(){
        const orderInfo = this.state.order.info ? (
            <>
            <Row>
                <Col><h5>Id: {this.state.order.info.id}</h5></Col>
            </Row>
            <Row>
                <Col><h5>Total price: {this.state.order.info.price}</h5></Col>
            </Row>
            <Row>
                <Col><h5>Ordered: {this.state.order.info.date}</h5></Col>
            </Row>
            <Row>
                <Col><h5>Address: {this.state.order.info.address}</h5></Col>
            </Row>
            </>
        ) : "";

        const delivery = this.state.order.delivery ? (
            <Row>
                <Col><h5>Delivery provider: {this.state.order.delivery.name}</h5></Col>
            </Row>
        ) : "";

        const payment = this.state.order.payment ? (
            <Row>
                <Col><h5>Payment method: {this.state.order.payment.name}</h5></Col>
            </Row>
        ) : "";

        const paid = this.state.order.info ? (
            <Row>
                <Col><h5>Paid: {this.state.order.info.paid ? "yes" : "no"}</h5></Col>
            </Row>
        ) : "";

        const packageNr = this.state.order.info ? (
            <Row>
                <Col><h5>Package number: {this.state.order.info.packageNr.length > 0 ? this.state.order.info.packageNr : "no packge number assigned"}</h5></Col>
            </Row>
        ) : "";

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
        }
        const deliveryStatusNode = this.state.order.info ? (
            <Row>
                <Col><h5>Delivery status: {deliveryStatus}</h5></Col>
            </Row>
        ) : "";

        const details = this.state.details.map(o => {
            return (
                <li className="pt-1 pb-1 orderListItem">
                        <Row className="d-flex justify-content-start">
                            <Col><h5>Product name: {o.name}</h5></Col>
                        </Row>
                        <Row className="d-flex justify-content-start">
                            <Col><h5>Product price: {o.price}</h5></Col>
                        </Row>
                        <Row>
                            <Col><hr></hr></Col>
                        </Row>
                </li>
            );
        });
        return(
            <>
            <Container className="productListItem mt-3 p-3 order-info" fluid>
                {orderInfo}
                {delivery}
                {payment}
                {paid}
                {packageNr}
                {deliveryStatusNode}
                <Row className="mt-4 p-1">
                    <Col><h4 className="mb-0">Order details:</h4></Col>
                </Row>
                <ul className="orderList mt-0 p-1">
                    {details}
                </ul>
            </Container>
            </>
        )
    }
}

export default Order;