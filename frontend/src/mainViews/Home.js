import React from 'react';
import MyNavbar from '../partials/Navbar.js';
import Search from '../mainViews/Search.js';
import CategorySearch from '../mainViews/CategorySearch.js';
import Product from '../mainViews/Product.js';
import Main from '../mainViews/Main.js';
import { BrowserRouter as Router, Route, Link } from "react-router-dom";

// import Row from 'react-bootstrap/Row'
// import Col from 'react-bootstrap/Col'
import Container from 'react-bootstrap/Container'

class Home extends React.Component {
    render(){
        return(
            <>
            <Router>
                <MyNavbar />
                <Container fluid id="mainDiv">
                    <Route exact path="/" component={Main}/>
                    <Route path="/search" component={Search}/>
                    <Route path="/category/:id" render={(props) => <CategorySearch {...props} type="category"/>}/>
                    <Route path="/subcategory/:id" render={(props) => <CategorySearch {...props} type="subcategory"/>}/>
                    <Route path="/product/:id" component={Product} />
                </Container>
            </Router>
            </>
        )
    }
}

export default Home;