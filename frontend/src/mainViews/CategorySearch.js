import React from 'react';
// import { BrowserRouter as Router, Route, Link } from "react-router-dom";
import queryString from 'query-string';

import SearchList from '../partials/SearchList.js';

let getProducts = async (catId) => {
    let products = await fetch(`http://localhost:9000/api/category/${catId}`);
    let productsJson = await products.json();
    return productsJson;
}

class Search extends React.Component {
    constructor(props){
        super(props);
        this.state = {id: this.props.match.params.id, products: null};
    }

    componentDidMount(){
        getProducts(this.state.id).then(p => {
            this.setState({products: p.products});
        });
    }

    render(){        
        let productList = this.state.products ? <SearchList products={this.state.products}/> : null;
        return productList;
    }
}

export default Search;