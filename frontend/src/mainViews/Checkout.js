import React from 'react';
import { BrowserRouter as Router, Route, Link } from "react-router-dom";

import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import Button from 'react-bootstrap/Button';
import Breadcrumb from 'react-bootstrap/Breadcrumb';
import Form from 'react-bootstrap/Form';

import { connect } from "react-redux";

import _ from 'lodash';
import getProducts from '../utils/getProducts.js';
import ProductTable from '../partials/ProductTable';
import { Redirect } from "react-router-dom";

function select(state, ownProps){
    return {
        userId: state.userId,
        userName: state.userName,
        cookies: ownProps.cookies,
        loggedIn: state.loggedIn
    }
}

const getDeliveries = async () => {
    let deliveries = await fetch('http://localhost:9000/api/deliveries');
    let deliveriesJson = await deliveries.json();
    return deliveriesJson;
}

const getPayments = async () => {
    let payments = await fetch('http://localhost:9000/api/payments');
    let paymentsJson = await payments.json();
    return paymentsJson;
}

class Checkout extends React.Component {
    constructor(props){
        super(props);
        this.state = {products: [], payments: [], deliveries: [], available: true, details: [], deliveryChosen: null, valid: false, redirect: null, errorMsg: null, userId: this.props.userId, loggedIn: this.props.loggedIn};
        this.addressRef1 = React.createRef();
        this.addressRef2 = React.createRef();
        this.addressRef3 = React.createRef();
        this.addressRef4 = React.createRef();
        this.deliveryRef = React.createRef();
        this.paymentRef = React.createRef();
    }

    componentDidMount(){
        const { cookies } = this.props;
        const cart = cookies.get('cart');
        if(cart.products.length === 0){
            this.setState({redirect: '/error', errorMsg: 'Empty cart'})
        }
        else{
            getProducts(cart.products.map(p => p.id)).then(prds => {
                getDeliveries().then(del => {
                    getPayments().then(pmts => {
                        let productsAvailable = true;
                        let details = [];
                        prds.products.forEach(p => {
                            const index = cart.products.findIndex(product => product.id === p.id);
                            if(p.amount - cart.products[index].amount < 0){
                                productsAvailable = false;
                            }
                            details.push({id: p.id, name: p.name, price: p.price, amount: cart.products[index].amount});
                        });
                        this.setState({products: prds.products, payments: pmts, deliveries: del, available: productsAvailable, details});
                    });
                });
            });
        }
    }

    deliveryChangeHandle = (e) => {
        const index = this.state.deliveries.findIndex(d => d.id === parseInt(e.target.id));
        this.deliveryRef = e.target;
        this.setState({deliveryChosen: this.state.deliveries[index]});
    }

    paymentChangeHandle = (e) => {
        this.paymentRef = e.target;
    }

    handleSubmit = async (event) => {
        const form = event.currentTarget;
        if (form.checkValidity() === false) {
          event.preventDefault();
          event.stopPropagation();
        }
        else{
            event.preventDefault();
            event.stopPropagation();
            const { cookies } = this.props;
            let cart = cookies.get('cart');
            const body = {
                details: cart.products,
                address: `${this.addressRef1.current.value} ${this.addressRef2.current.value} ${this.addressRef3.current.value} ${this.addressRef4.current.value}`,
                userId: this.props.userId,
                payment: parseInt(this.paymentRef.id),
                delivery: parseInt(this.deliveryRef.id)
            };
            const response = await fetch('http://localhost:9000/api/order', {
                method: 'POST',
                body: JSON.stringify(body),
                headers: {
                    'Content-Type': 'application/json'
                }
            });
            cart.products = [];
            cookies.set('cart', cart, { path: '/' });
            const responseJson = await response.json();
            if(response.status === 200){
                this.setState({redirect: '/profile'});
            }
            else{
                this.setState({redirect: '/error', errorMsg: responseJson.message});
            }
        }
        this.setState({valid: true});
      };

    render(){
        const { cookies } = this.props;
        const cart = cookies.get('cart');
        let totalSum;

        if(this.state.products.length > 0){
            totalSum = cart.products.reduce((acc, el) => {
                const prd = this.state.products.find(p => p.id === el.id);
                return acc + el.amount * prd.price;
            }, 0);
        }

        totalSum = this.state.deliveryChosen ? totalSum + this.state.deliveryChosen.price : totalSum;

        const deliveryOptions = this.state.deliveries.map(d => {
            return(
                <Form.Check key={d.id}
                type="radio"
                label={`${d.name} (${d.price}zl)`}
                name={`delivery`}
                id={d.id}
                className="d-flex justify-content-start"
                onChange={(e) => this.deliveryChangeHandle(e)}
                required
                />
            );
        })

        const paymentOptions = this.state.payments.map(p => {
            return(
                <Form.Check key={p.id}
                type="radio"
                label={p.name}
                name={`payment`}
                id={p.id}
                className="d-flex justify-content-start"
                onChange={(e) => this.paymentChangeHandle(e)}
                required
                />
            );
        })
        
        if(!this.state.redirect && this.state.loggedIn){
            return(
                <>
                <Container fluid className="main mt-3 p-3">
                <Breadcrumb>
                    <Breadcrumb.Item href="/">Home</Breadcrumb.Item>
                    <Breadcrumb.Item href="/cart">Cart</Breadcrumb.Item>
                    <Breadcrumb.Item active>Finalize order</Breadcrumb.Item>
                </Breadcrumb>
                    <Row>
                        <Col lg={6} md={12}>
                            <ProductTable details={this.state.details}  />
                        </Col>
                        <Col>
                        <Form noValidate validated={this.state.valid} onSubmit={this.handleSubmit}>
                            <Form.Group as={Row} controlId="formHorizontalEmail">
                                <Form.Label column sm={2}>
                                Line 1
                                </Form.Label>
                                <Col sm={10}>
                                <Form.Control type="text" placeholder="Address line 1" required ref={this.addressRef1}/>
                                </Col>
                            </Form.Group>

                            <Form.Group as={Row} controlId="formHorizontalEmail">
                                <Form.Label column sm={2}>
                                City
                                </Form.Label>
                                <Col sm={10}>
                                <Form.Control type="text" placeholder="City" required ref={this.addressRef2}/>
                                </Col>
                            </Form.Group>

                            <Form.Group as={Row} controlId="formHorizontalEmail">
                                <Form.Label column sm={2}>
                                State
                                </Form.Label>
                                <Col sm={3}>
                                <Form.Control type="text" placeholder="State" required ref={this.addressRef3}/>
                                </Col>
                                <Form.Label column sm={3}>
                                Postal code
                                </Form.Label>
                                <Col sm={4}>
                                <Form.Control type="text" placeholder="Postal code" required ref={this.addressRef4}/>
                                </Col>
                            </Form.Group>
                            <fieldset>
                                <Form.Group as={Row}>
                                <Form.Label as="deliveries" column sm={2}>
                                    Delivery
                                </Form.Label>
                                <Col sm={10}>
                                    {deliveryOptions}
                                </Col>
                                </Form.Group>
                            </fieldset>
                            <fieldset>
                                <Form.Group as={Row}>
                                <Form.Label as="payments" column sm={2}>
                                    Payment
                                </Form.Label>
                                <Col sm={10}>
                                    {paymentOptions}
                                </Col>
                                </Form.Group>
                            </fieldset>

                            <Row>
                                <Col>
                                    <h2 className="text-md-left text-center">Total sum: {totalSum}zl</h2>
                                </Col>
                            </Row>

                            <Form.Group as={Row}>
                                <Col className="d-flex justify-content-center justify-content-md-start">
                                <Button type="submit" size="lg">Checkout</Button>
                                </Col>
                            </Form.Group>
                            </Form>
                        </Col>
                    </Row>
                </Container>
                </>
            );
        }
        else{
            return <Redirect to={{pathname: '/error', state: this.state.errorMsg || 'You must be logged in to view this page'}} />
        }
    }
}

const ConnectCheckout = connect(select)(Checkout);
export default ConnectCheckout;