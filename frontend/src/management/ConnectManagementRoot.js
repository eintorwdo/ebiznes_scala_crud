import React from 'react';
import Home from './Home.js';
import CategoriesOrdersUsers from './CategoriesOrdersUsers.js';
import Category from './Category.js';
import Product from './Product.js';
import Order from './Order.js';
import OrderUpdate from './OrderUpdate.js';
import User from './User.js';

import { BrowserRouter as Router, Route, Link, Redirect } from "react-router-dom";
import Container from 'react-bootstrap/Container';

import { connect } from "react-redux";
import {hideLogin, logOut} from '../actions/index.js';


function mapDispatchToProps(dispatch){
    return {
        hideLogin: () => dispatch(hideLogin()),
        logout: () => dispatch(logOut())
    }
}

function select(state){
    return {
        showLoginModal: state.showLoginModal,
        role: state.role,
        token: state.token,
        tokenExpiry: state.tokenExpiry
    }
}

class ManagementRoot extends React.Component {
    constructor(props){
        super(props);
        this.state = {allowed: false, loading: true};
    }

    componentDidMount(){
        if(this.props.role == "ADMIN"){
            this.setState({allowed: true, loading: false});
        }
        else{
            this.setState({allowed: false, loading: false});
        }
    }

    componentDidUpdate(prevProps){
        if(prevProps.token !== this.props.token){
            if(this.props.role !== "ADMIN"){
                this.setState({allowed: false});
            }
        }
    }

    render(){
        if(this.state.allowed){
            const tokenInfo = {
                token: this.props.token,
                tokenExpiry: this.props.tokenExpiry
            }
            return(
                <>
                <Router>
                        <Container className="mt-3">
                            <Link to='/management'><h3 className="mb-5">Home</h3></Link>
                            <Route exact path='/management' render={props => <Home {...props}/>}/>
                            <Route exact path='/management/categories' render={props => <CategoriesOrdersUsers {...props} type="categories" tokenInfo={tokenInfo}/>}/>
                            <Route exact path='/management/subcategories' render={props => <CategoriesOrdersUsers {...props} type="subcategories" tokenInfo={tokenInfo}/>}/>
                            <Route exact path='/management/products' render={props => <CategoriesOrdersUsers {...props} type="products" tokenInfo={tokenInfo}/>}/>
                            <Route exact path='/management/orders' render={props => <CategoriesOrdersUsers {...props} type="orders" tokenInfo={tokenInfo}/>}/>
                            <Route path='/management/users' render={props => <CategoriesOrdersUsers {...props} type="users" tokenInfo={tokenInfo}/>}/>
                            <Route path='/management/category/:id' render={props => <Category {...props} type="category" tokenInfo={tokenInfo}/>}/>
                            <Route path='/management/subcategory/:id' render={props => <Category {...props} type="subcategory" tokenInfo={tokenInfo}/>}/>
                            <Route path='/management/product/:id' render={props => <Product {...props} tokenInfo={tokenInfo}/>}/>
                            <Route path='/management/products/add' render={props => <Product {...props} type="add" tokenInfo={tokenInfo}/>}/>
                            <Route path='/management/categories/add' render={props => <Category {...props} type="category" method="add" tokenInfo={tokenInfo}/>}/>
                            <Route path='/management/subcategories/add' render={props => <Category {...props} type="subcategory" method="add" tokenInfo={tokenInfo}/>}/>
                            <Route exact path='/management/order/:id' render={props => <Order {...props} tokenInfo={tokenInfo}/>}/>
                            <Route path='/management/order/:id/update' render={props => <OrderUpdate {...props} tokenInfo={tokenInfo}/>}/>
                            <Route path='/management/user/:id' render={props => <User {...props} tokenInfo={tokenInfo}/>}/>
                        </Container>
                </Router>
                </>
            )
        }
        else if(this.state.loading == false){
            return <Redirect to={{pathname: '/error', state: 'You are not authorized to visit this page'}} />
        }
        else{
            return null;
        }
    }
}

const ConnectManagementRoot = connect(select, mapDispatchToProps)(ManagementRoot)
export default ConnectManagementRoot;