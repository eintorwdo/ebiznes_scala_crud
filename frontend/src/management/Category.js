import React from 'react';
import { BrowserRouter as Router, Route, Link, Redirect } from "react-router-dom";

class Category extends React.Component {
    constructor(props){
        super(props);
        this.state = {item: null};
    }

    componentDidMount(){
        fetch(`http://localhost:9000/api/${this.props.type}/${this.props.match.params.id}`).then(res => res.json().then(cat => {
            this.setState({item: cat});
        }));
    }

    render(){
        let item;
        let parentCategoryId;
        if(this.props.type === 'category'){
            item = this.state.item?.category;
        }
        else{
            item = this.state.item?.subcategory;
            parentCategoryId = <h3>parent category id: {this.state.item?.category.id}</h3>;
        }
        return(
            <>
            <h3>{this.props.type}: {item?.name}</h3>
            <h3>id: {item?.id}</h3>
            {parentCategoryId}
            </>
        )
    }
}

// const ConnectCategories = connect(select)(Categories)
export default Category;