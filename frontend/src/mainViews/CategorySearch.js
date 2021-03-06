import React from 'react';

import SearchList from '../partials/SearchList.js';

let getProducts = async (catId, type = "category") => {
    let products = await fetch(`http://localhost:9000/api/${type}/${catId}`);
    let productsJson = await products.json();
    return productsJson;
}

class CategorySearch extends React.Component {
    constructor(props){
        super(props);
        const type = this.props.type === "category" || this.props.type === "subcategory" ? this.props.type : "category";
        this.state = {id: this.props.match.params.id, products: null, type, category: null, subcategory: null};
    }

    componentDidMount(){
        getProducts(this.state.id, this.state.type).then(p => {
            this.setState({products: p.products, category: p.category, subcategory: p.subcategory});
        });
    }

    componentDidUpdate(prevProps){
        const type = this.props.type === "category" || this.props.type === "subcategory" ? this.props.type : "category";
        const id = this.props.match.params.id;
        if(id !== prevProps.match.params.id || type !== prevProps.type){
            getProducts(id, type).then(p => {
                this.setState({products: p.products, id, type});
            });
        }
    }

    render(){   
        let productList = this.state.products ? <SearchList products={this.state.products} type={this.props.type} category={this.state.category} subcategory={this.state.subcategory} cookies={this.props.cookies}/> : null;
        return productList;
    }
}

export default CategorySearch;