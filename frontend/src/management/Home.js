import React from 'react';
import { BrowserRouter as Router, Route, Link, Redirect } from "react-router-dom";

// import { BrowserRouter as Router, Route, Link } from "react-router-dom";

// import { connect } from "react-redux";
// import {hideLogin, logIn, logOut} from '../actions/index.js';

// import checkIfLoggedIn from '../utils/checkIfLoggedIn.js';

// function mapDispatchToProps(dispatch){
//     return {
//         hideLogin: () => dispatch(hideLogin()),
//         login: (payload) => dispatch(logIn(payload)),
//         logout: () => dispatch(logOut())
//     }
// }

// function select(state){
//     return {
//         showLoginModal: state.showLoginModal
//     }
// }

class Home extends React.Component {
    constructor(props){
        super(props);
    }

    render(){
        return(
            <>
            <ul>
                <Link to='/management/categories'><li>Categories</li></Link>
                <Link to='/management/subcategories'><li>Subcategories</li></Link>
                <Link to='/management/products'><li>Products</li></Link>
                <Link to='/management/orders'><li>Orders</li></Link>
                <Link to='/management/users'><li>Users</li></Link>
            </ul>
            </>
        )
    }
}

// const ConnectManagementRoot = connect(select, mapDispatchToProps)(ManagementRoot)
export default Home;