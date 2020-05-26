import React from 'react';
import MyNavbar from '../partials/Navbar.js'
import Search from '../mainViews/Search.js'
import Product from '../mainViews/Product.js'
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
                    <Route path="/search" component={Search}/>
                    <Route path="/product/:id" component={Product} />
                </Container>
            </Router>
            </>
        )
    }
}

export default Home;