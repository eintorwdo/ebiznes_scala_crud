import React from 'react';
import ConnectNavbar from '../partials/Navbar.js';
import Search from '../mainViews/Search.js';
import CategorySearch from '../mainViews/CategorySearch.js';
import ConnectProduct from '../mainViews/Product.js';
import Main from '../mainViews/Main.js';
import ConnectProfile from '../mainViews/Profile.js';
import Order from '../mainViews/Order.js';
import Cart from '../mainViews/Cart.js';
import ConnectCheckout from '../mainViews/Checkout.js';
import Error from '../mainViews/Error.js';
import { BrowserRouter as Router, Route, Link } from "react-router-dom";

import { withCookies } from 'react-cookie';

import Container from 'react-bootstrap/Container';

class Home extends React.Component {
    constructor(props){
        super(props);
        const { cookies } = this.props;
        const cart = cookies.get('cart');
        if(!cart){
            cookies.set('cart', {products: []}, { path: '/' });
        }
    }

    render(){
        return(
            <>
            <Router>
                <ConnectNavbar cookies={this.props.cookies}/>
                <Container fluid id="mainDiv">
                    <Route exact path="/" component={Main}/>
                    <Route path="/search" render={(props) => <Search {...props} cookies={this.props.cookies}/>}/>
                    <Route path="/category/:id" render={(props) => <CategorySearch {...props} type="category" cookies={this.props.cookies}/>}/>
                    <Route path="/subcategory/:id" render={(props) => <CategorySearch {...props} type="subcategory" cookies={this.props.cookies}/>}/>
                    <Route path="/product/:id" render={(props) => <ConnectProduct {...props} cookies={this.props.cookies}/>}/>
                    <Route path="/profile" component={ConnectProfile} />
                    <Route path="/order/:id" component={Order} />
                    <Route exact path="/cart" render={(props) => <Cart {...props} cookies={this.props.cookies}/>}/>
                    <Route path="/cart/checkout" render={(props) => <ConnectCheckout {...props} cookies={this.props.cookies}/>}/>
                    <Route path="/error" render={(props) => <Error {...props}/>}/>
                </Container>
            </Router>
            </>
        )
    }
}

export default withCookies(Home);