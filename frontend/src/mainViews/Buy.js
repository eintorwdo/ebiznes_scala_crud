import React from 'react';
import { BrowserRouter as Router, Route, Link } from "react-router-dom";

import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import Button from 'react-bootstrap/Button';
import Breadcrumb from 'react-bootstrap/Breadcrumb';

import { connect } from "react-redux";

import _ from 'lodash';

function select(state, ownProps){
    return {
        userId: state.userId,
        userName: state.userName,
        cookies: ownProps.cookies
    }
}

class Buy extends React.Component {
    constructor(props){
        super(props);
        const { cookies } = this.props;
        const cart = cookies.get('cart');
        this.state = {products: []};
    }

    componentDidMount(){
        
    }

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

        const products = this.state.products.map(p => {
            const index = cart.products.findIndex(product => product.id === p.id);

            if(index !== -1){
                
            }
        });

        return(
            <>
            <Container fluid className="main mt-3 p-3">
            <Breadcrumb>
                <Breadcrumb.Item href="/">Home</Breadcrumb.Item>
                <Breadcrumb.Item href="/cart">Cart</Breadcrumb.Item>
                <Breadcrumb.Item active>Finalize order</Breadcrumb.Item>
            </Breadcrumb>
                
            </Container>
            </>
        );
    }
}

const ConnectBuy = connect(select)(Buy);
export default ConnectBuy;