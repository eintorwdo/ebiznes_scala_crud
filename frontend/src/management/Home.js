import React from 'react';
import { Link } from "react-router-dom";


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

export default Home;