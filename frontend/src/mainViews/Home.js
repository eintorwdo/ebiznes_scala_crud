import React from 'react';
import ConnectNavbar from '../partials/Navbar.js';
import Search from '../mainViews/Search.js';
import CategorySearch from '../mainViews/CategorySearch.js';
import ConnectProduct from '../mainViews/Product.js';
import Main from '../mainViews/Main.js';
import ConnectProfile from '../mainViews/Profile.js';
import Order from '../mainViews/Order.js';
import { BrowserRouter as Router, Route, Link } from "react-router-dom";


import Container from 'react-bootstrap/Container'

class Home extends React.Component {
    render(){
        return(
            <>
            <Router>
                <ConnectNavbar />
                <Container fluid id="mainDiv">
                    <Route exact path="/" component={Main}/>
                    <Route path="/search" component={Search}/>
                    <Route path="/category/:id" render={(props) => <CategorySearch {...props} type="category"/>}/>
                    <Route path="/subcategory/:id" render={(props) => <CategorySearch {...props} type="subcategory"/>}/>
                    <Route path="/product/:id" component={ConnectProduct} />
                    <Route path="/profile" component={ConnectProfile} />
                    <Route path="/order/:id" component={Order} />
                </Container>
            </Router>
            </>
        )
    }
}

export default Home;