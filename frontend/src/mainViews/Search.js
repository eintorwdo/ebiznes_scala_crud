import React from 'react';
// import { BrowserRouter as Router, Route, Link } from "react-router-dom";
import queryString from 'query-string';

import SearchList from '../partials/SearchList.js';

let getProducts = async (query) => {
    let products = await fetch(`http://localhost:9000/api/products?query=${query}`);
    let productsJson = await products.json();
    return productsJson;
}

class Search extends React.Component {
    constructor(props){
        super(props);
        this.state = {query: "", products: null};
        this.state.query = this.props.location.search;
    }

    componentDidMount(){
        let q = queryString.parse(this.props.location.search);
        getProducts(q.query).then(p => {
            this.setState({products: p.products});
        });
    }

    render(){        
        let productList = this.state.products ? <SearchList products={this.state.products}/> : null;
        return productList;
    }
}

export default Search;