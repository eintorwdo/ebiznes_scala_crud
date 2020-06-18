import React from 'react';
import { Link } from "react-router-dom";

class CategoriesOrdersUsers extends React.Component {
    constructor(props){
        super(props);
        this.state = {items: null};
    }

    componentDidMount(){
        fetch(`http://localhost:9000/api/${this.props.type}`, {headers: {'X-Auth-Token': this.props.tokenInfo.token}}).then(res => res.json().then(cats => {
            this.setState({items: cats});
        }));
    }

    render(){
        let items;
        if(this.props.type === 'categories'){
            items = this.state.items?.categories.map(cat => {
                return <Link key={cat.category.id} to={`/management/category/${cat.category.id}`}><li>{cat.category.name}</li></Link>
            });
        }
        else if(this.props.type === 'subcategories'){
            items = this.state.items?.map(cat => {
                return <Link key={cat.id} to={`/management/subcategory/${cat.id}`}><li>{cat.name}</li></Link>
            });
        }
        else if(this.props.type === 'orders'){
            items = this.state.items?.map(o => {
                return <Link key={o.id} to={`/management/order/${o.id}`}><li>{o.id}</li></Link>
            });
        }
        else if(this.props.type === 'users'){
            items = this.state.items?.users.map(u => {
                return <Link key={u.id} to={`/management/user/${u.id}`}><li>{u.email}</li></Link>
            });
        }
        else if(this.props.type === 'products'){
            items = this.state.items?.products.map(p => {
                return <Link key={p.id} to={`/management/product/${p.id}`}><li>{p.name}</li></Link>
            });
        }
        const addLink = this.props.type == 'categories' || this.props.type == 'subcategories' || this.props.type == 'products' ?
            <Link to={`/management/${this.props.type}/add`}><h4>ADD</h4></Link> : null;
        return(
            <>
            <h3>{this.props.type}:</h3>
            {addLink}
            <hr></hr>
            <ul className="mt-3">
                {items}
            </ul>
            <hr></hr>
            </>
        )
    }
}

export default CategoriesOrdersUsers;