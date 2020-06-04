import React from 'react';
import { BrowserRouter as Router, Route, Link } from "react-router-dom";

import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import Button from 'react-bootstrap/Button';
import Breadcrumb from 'react-bootstrap/Breadcrumb';

// import addToCartHandler from '../utils/addToCartHandler.js';

import _ from 'lodash';
import getProducts from '../utils/getProducts.js';

// let getProducts = async (ids = []) => {
//     if(ids.length > 0){
//         let query = "?";
//         ids.forEach(id => {query+=`id=${id}&`});
//         query = query.slice(0, -1);
//         let products = await fetch(`http://localhost:9000/api/products/ids${query}`);
//         let productsJson = await products.json();
//         return productsJson;
//     }
//     else{
//         return new Promise(resolve => {resolve({products: []})});
//     }
// }

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

    cartDecrement = (id) => {
        const { cookies } = this.props;
        let cart = cookies.get('cart');
        const index = cart.products.findIndex(product => product.id === id);
        if(index >= 0){
            if(cart.products[index].amount > 1){
                cart.products[index].amount -= 1;
                cookies.set('cart', cart, { path: '/' });
            }  
        }
    }

    cartIncrement = (id) => {
        const { cookies } = this.props;
        let cart = cookies.get('cart');
        const index = cart.products.findIndex(product => product.id === id);
        if(index >= 0){
            cart.products[index].amount += 1;
            cookies.set('cart', cart, { path: '/' });   
        }
    }

    removeItem = (id) => {
        const { cookies } = this.props;
        let cart = cookies.get('cart');
        const index = cart.products.findIndex(product => product.id === id);
        if(index >= 0){
            cart.products.splice(index, 1);
            cookies.set('cart', cart, { path: '/' });
            let products = this.state.products;
            const stateIndex = products.findIndex(product => product.id === id);
            products.splice(stateIndex, 1);
            this.setState({products});
        }
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

        const products = this.state.products.length > 0 ? this.state.products.map(p => {
            const index = cart.products.findIndex(product => product.id === p.id);

            if(index !== -1){
                const minusSign = cart.products[index].amount > 1 ? <i className="fas fa-minus cartNum" onClick={() => this.cartDecrement(p.id)}></i> : <i className="pl-2 pr-2"></i>;
                const plusSign = cart.products[index].amount < p.amount ? <i className="fas fa-plus cartNum" onClick={() => this.cartIncrement(p.id)}></i> : <i className="pl-2 pr-2"></i>;
                return(
                    <Row key={p.id} className="mt-3 justify-content-center justify-content-lg-start">
                        <Col className="listItemImageWrapper">
                            <Container fluid className="listItemImage"></Container>
                        </Col>
                        <Col className="d-flex align-items-center justify-content-center justify-content-lg-start" md={12} lg={2} style={{overflow: "hidden"}}>
                            <Link to={`/product/${p.id}`}><h4>{p.name}</h4></Link>
                        </Col>
                        <Col className="d-flex align-items-center justify-content-center justify-content-lg-start" md={12} lg={2}>
                            <h5>{minusSign}</h5><h5 className="ml-2 mr-2">{cart.products[index].amount}</h5><h5>{plusSign}</h5>
                        </Col>
                        <Col className="d-flex align-items-center justify-content-center justify-content-lg-start" md={12} lg={2}>
                            <h5 className="mr-lg-3">{p.price}zl</h5>
                        </Col>
                        <Col className="d-flex align-items-center justify-content-center justify-content-lg-start" md={12} lg={1}>
                            <h5><i className="fas fa-trash-alt" onClick={() => this.removeItem(p.id)}></i></h5>
                        </Col>
                    </Row>
                );
            }
        }) : <h3>The cart is empty</h3>;

        const checkoutButton = cart.products.length > 0 ? <Link to="/cart/checkout"><Button>Finalize order</Button></Link> :
            <Link to="#"><Button variant="secondary" disabled>Finalize order</Button></Link>

        return(
            <>
            <Container fluid className="main mt-3 p-3">
            <Breadcrumb>
                <Breadcrumb.Item href="/">Home</Breadcrumb.Item>
                <Breadcrumb.Item active>Cart</Breadcrumb.Item>
            </Breadcrumb>
                <Row>
                    <Col>
                        <h3 className="text-left">Your cart:</h3>
                        <hr></hr>
                    </Col>
                </Row>
                <Row>
                    <Col md={8} sm={12}>
                        {products}
                    </Col>
                    <Col className="cartTotal mt-4 mt-md-0 ml-3 mr-3">
                        <Row>
                            <Col>
                                <h3 className="text-sm-center text-md-left mt-2 mt-md-0">Total: {totalSum ? totalSum : 0}zl</h3>
                            </Col>
                        </Row>
                        <Row>
                            <Col className="d-flex justify-content-center justify-content-md-start mt-2">
                                {checkoutButton}
                            </Col>
                        </Row>
                    </Col>
                </Row>
            </Container>
            </>
        );
    }
}

export default Cart;