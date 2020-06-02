import React from 'react';
// import { BrowserRouter as Router, Route, Link } from "react-router-dom";
import queryString from 'query-string';

import SearchList from '../partials/SearchList.js';

let getProducts = async (query, category) => {
    let catQuery = "";
    if(category){
        catQuery = `&category=${category}`;
    }
    let products = await fetch(`http://localhost:9000/api/products?query=${query}${catQuery}`);
    let productsJson = await products.json();
    return productsJson;
}

let getCategory = async (id) => {
    let category = await fetch(`http://localhost:9000/api/category/${id}`);
    let categoryJson = await category.json();
    return categoryJson;
}


class Search extends React.Component {
    constructor(props){
        super(props);
        this.state = {query: "", products: null, category: null};
        this.state.query = this.props.location.search;
    }

    getData = async(q) => {
        getProducts(q.query, q.category).then(p => {
            if(q.category){
                getCategory(q.category).then(cat => {
                    this.setState({products: p.products, category: cat.category, query: this.props.location.search});
                });
            }
            else{
                this.setState({products: p.products, category: null, query: this.props.location.search});
            }
        });
    }

    componentDidMount(){
        let q = queryString.parse(this.props.location.search);
        this.getData(q);
    }

    componentDidUpdate(prevProps){
        if(prevProps.location.search !== this.props.location.search){
            let q = queryString.parse(this.props.location.search);
            this.getData(q);
        }
    }

    render(){
        const type = this.state.category ? "category" : null;
        let productList = this.state.products ? <SearchList products={this.state.products} type={type} category={this.state.category} cookies={this.props.cookies}/> : null;
        return productList;
    }
}

export default Search;