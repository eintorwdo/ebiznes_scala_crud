import React from 'react';
import { BrowserRouter as Router, Route, Link } from "react-router-dom";

import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import Button from 'react-bootstrap/Button';
import Breadcrumb from 'react-bootstrap/Breadcrumb';

import addToCartHandler from '../utils/addToCartHandler.js';

import _ from 'lodash';
// import chunk from 'lodash/chunk';

let getProducts = async (ids = []) => {
    if(ids.length > 0){
        let query = "?";
        ids.forEach(id => {query+=`id=${id}&`});
        query = query.slice(0, -1);
        let products = await fetch(`http://localhost:9000/api/products/ids${query}`);
        let productsJson = await products.json();
        return productsJson;
    }
    else{
        return new Promise(resolve => {resolve([])});
    }
}

class Cart extends React.Component {
    constructor(props){
        super(props);
        const { cookies } = this.props;
        const cart = cookies.get('cart');
        const ids = cart.products.map(p => p.id);
        this.state = {ids, products: []};
    }

    componentDidMount(){
        getProducts(this.state.ids).then(prds => {
            this.setState({products: prds.products});
        })
    }

    componentDidUpdate(prevProps){
        
    }
    
    render(){
        return(
            <>

            </>
        );
    }
}

export default Cart;