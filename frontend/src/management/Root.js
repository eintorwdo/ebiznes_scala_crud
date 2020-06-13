import React from 'react';
import Home from './Home.js';
import CategoriesOrdersUsers from './CategoriesOrdersUsers.js';
import Category from './Category.js';
import Product from './Product.js';
import Order from './Order.js';
import OrderUpdate from './OrderUpdate.js';

import { BrowserRouter as Router, Route, Link, Redirect } from "react-router-dom";
import Container from 'react-bootstrap/Container';

import { connect } from "react-redux";
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
        showLoginModal: state.showLoginModal,
        role: state.role
    }
}

class ManagementRoot extends React.Component {
    constructor(props){
        super(props);
        // if(checkIfLoggedIn(localStorage.getItem('token'), localStorage.getItem('tokenExpiry'))){
        //     const payload = {
        //         token: localStorage.getItem('token'),
        //         tokenExpiry: localStorage.getItem('tokenExpiry'),
        //         email: localStorage.getItem('email'),
        //         role: localStorage.getItem('role')
        //     };
        //     this.props.login(payload);
        // }
        // else{
        //     localStorage.clear();
        //     this.props.logout();
        // }
    }

    render(){
        const path = this.props.location.pathname
        return(
            <>
            <Router>
                    <Container className="mt-3">
                        <Link to='/management'><h3 className="mb-5">Home</h3></Link>
                        <Route exact path='/management'>
                            {/* {this.props.role === 'REGULAR' ? <Home {...this.props}/> : <Redirect to='/management/login' />} */}
                            <Home {...this.props}/>
                        </Route>
                        <Route exact path='/management/categories'>
                            {/* {this.props.role === 'REGULAR' ? <Categories {...this.props}/> : <Redirect to='/management/login' />} */}
                            <CategoriesOrdersUsers {...this.props} type="categories"/>
                        </Route>
                        <Route exact path='/management/subcategories'>
                            {/* {this.props.role === 'REGULAR' ? <Categories {...this.props}/> : <Redirect to='/management/login' />} */}
                            <CategoriesOrdersUsers {...this.props} type="subcategories"/>
                        </Route>
                        <Route exact path='/management/products'>
                            {/* {this.props.role === 'REGULAR' ? <Categories {...this.props}/> : <Redirect to='/management/login' />} */}
                            <CategoriesOrdersUsers {...this.props} type="products"/>
                        </Route>
                        <Route exact path='/management/orders'>
                            {/* {this.props.role === 'REGULAR' ? <Categories {...this.props}/> : <Redirect to='/management/login' />} */}
                            <CategoriesOrdersUsers {...this.props} type="orders"/>
                        </Route>
                        <Route path='/management/users'>
                            {/* {this.props.role === 'REGULAR' ? <Categories {...this.props}/> : <Redirect to='/management/login' />} */}
                            <CategoriesOrdersUsers {...this.props} type="users"/>
                        </Route>
                        <Route path='/management/category/:id' render={props => <Category {...props} type="category"/>}/>
                        <Route path='/management/subcategory/:id' render={props => <Category {...props} type="subcategory"/>}/>
                        <Route path='/management/product/:id' render={props => <Product {...props}/>}/>
                        <Route path='/management/products/add' render={props => <Product {...props} type="add"/>}/>
                        <Route path='/management/categories/add' render={props => <Category {...props} type="category" method="add"/>}/>
                        <Route path='/management/subcategories/add' render={props => <Category {...props} type="subcategory" method="add"/>}/>
                        <Route exact path='/management/order/:id' render={props => <Order {...props}/>}/>
                        <Route path='/management/order/:id/update' render={props => <OrderUpdate {...props}/>}/>
                    </Container>
            </Router>
            </>
        )
    }
}

const ConnectManagementRoot = connect(select, mapDispatchToProps)(ManagementRoot)
export default ConnectManagementRoot;