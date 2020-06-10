import React from 'react';
import ConnectNavbar from '../partials/Navbar.js';
import Search from './Search.js';
import CategorySearch from './CategorySearch.js';
import ConnectProduct from './Product.js';
import Main from './Main.js';
import ConnectProfile from './Profile.js';
import ConnectOrder from './Order.js';
import Cart from './Cart.js';
import ConnectCheckout from './Checkout.js';
import Error from './Error.js';
import ConnectAuth from './Auth.js';

import { BrowserRouter as Router, Route, Link } from "react-router-dom";
import Modal from 'react-bootstrap/Modal'
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';

import { connect } from "react-redux";
import { withCookies } from 'react-cookie';
import {hideLogin, logIn, logOut} from '../actions/index.js';

import checkIfLoggedIn from '../utils/checkIfLoggedIn.js';

function mapDispatchToProps(dispatch){
    return {
        hideLogin: () => dispatch(hideLogin()),
        login: (payload) => dispatch(logIn(payload)),
        logout: () => dispatch(logOut())
    }
}

function select(state){
    return {
        showLoginModal: state.showLoginModal
    }
}

class Home extends React.Component {
    constructor(props){
        super(props);
        const { cookies } = this.props;
        const cart = cookies.get('cart');
        if(!cart){
            cookies.set('cart', {products: []}, { path: '/' });
        }
        if(checkIfLoggedIn(localStorage.getItem('token'), localStorage.getItem('tokenExpiry'))){
            const payload = {
                token: localStorage.getItem('token'),
                tokenExpiry: localStorage.getItem('tokenExpiry'),
                email: localStorage.getItem('email')
            };
            this.props.login(payload);
        }
        else{
            localStorage.clear();
            this.props.logout();
        }
    }

    handleLoginRequest = async (e, provider) => {
        window.location = `http://localhost:9000/auth/provider/${provider}`;
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
                    <Route path="/order/:id" component={ConnectOrder} />
                    <Route exact path="/cart" render={(props) => <Cart {...props} cookies={this.props.cookies}/>}/>
                    <Route path="/cart/checkout" render={(props) => <ConnectCheckout {...props} cookies={this.props.cookies}/>}/>
                    <Route path="/error" render={(props) => <Error {...props}/>}/>
                    <Route path="/auth" render={(props) => <ConnectAuth {...props}/>}/>

                    <Modal
                        show={this.props.showLoginModal}
                        size="lg"
                        aria-labelledby="contained-modal-title-vcenter"
                        centered
                        onHide={() => {this.props.hideLogin()}}
                        >
                        <Modal.Header closeButton onClick={() => {this.props.hideLogin()}}>
                            <Modal.Title id="contained-modal-title-vcenter">
                            Log in
                            </Modal.Title>
                        </Modal.Header>
                            <Modal.Body>
                                <Col>
                                <Row className="mb-3">
                                    <Col>
                                        <div className="loginProviderButton" onClick={(e) => this.handleLoginRequest(e, "google")}><h3 className="text-center"><i className="fab fa-google"></i> Log in with Google</h3></div>
                                    </Col>
                                </Row>
                                <Row>
                                    <Col>  
                                        <div className="loginProviderButton" onClick={(e) => this.handleLoginRequest(e, "facebook")}><h3 className="text-center"><i className="fab fa-facebook"></i> Log in with Facebook</h3></div>
                                    </Col>
                                </Row>
                                </Col>
                            </Modal.Body>
                            <Modal.Footer>
                                
                            </Modal.Footer>
                    </Modal>
                </Container>
            </Router>
            </>
        )
    }
}

const ConnectHome = connect(select, mapDispatchToProps)(Home)
export default withCookies(ConnectHome);