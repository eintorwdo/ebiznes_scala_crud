import React from 'react';
import { BrowserRouter as Router, Route, Link, Redirect } from "react-router-dom";

class Product extends React.Component {
    constructor(props){
        super(props);
        this.state = {item: null, manufacturers: null, categories: null, subcategories: null};
    }

    componentDidMount(){
        this.getData().then(d => {
            this.setState({item: d[0], manufacturers: d[1], categories: d[2], subcategories: d[3]});
        });
    }

    getData = async () => {
        const product = await fetch(`http://localhost:9000/api/product/${this.props.match.params.id}`).then(res => res.json());
        const manufacturers = await fetch('http://localhost:9000/api/manufacturers').then(res => res.json());
        const categories = await fetch('http://localhost:9000/api/categories').then(res => res.json());
        const subcategories = await fetch('http://localhost:9000/api/subcategories').then(res => res.json());
        return [product, manufacturers, categories, subcategories];
    }

    handleSubmit = (e) => {
        e.preventDefault();
        const method = this.props.type == 'add' ? 'POST' : 'PUT';
        const payload = {
            name: e.target[0].value,
            description: e.target[1].value,
            amount: parseInt(e.target[2].value),
            price: parseInt(e.target[3].value),
            manufacturer: parseInt(e.target[4].value),
            category: parseInt(e.target[5].value),
            subcategory: parseInt(e.target[6].value)
        };
        fetch(`http://localhost:9000/api/product/${this.props.match.params.id}`, {
            method,
            headers:{
                'X-Auth-Token': 'xD',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        }).then(res => {
            if(res.status == 200){
                alert("product updated");
            }
            else{
                alert(`error: ${res.statusText}`);
            }
        });
    }

    onInputChange = (e, property) => {
        let item = this.state.item;
        item.product.info[property] = e.target.value;
        this.setState({ item });
    }

    render(){
        if(this.state.item?.product){
            const prd = this.state.item?.product.info;
            const manufacturers = this.state.manufacturers?.map(m => {
                return <option value={m.id} selected={m.id == prd.manufacturer}>{m.name}</option>;
            });
            const categories = this.state.categories?.categories.map(c => {
                return <option value={c.category.id} selected={c.category.id == prd.category}>{c.category.name}</option>;
            });
            const subcategories = this.state.subcategories?.map(s => {
                return <option value={s.id} selected={s.id == prd.subcategory}>{s.name}</option>;
            });
            return(
                <>
                <form onSubmit={this.handleSubmit}>
                    <label htmlFor='name'>Name:</label><br></br>
                    <input type='text' id='name' value={this.state.item?.product.info.name} onChange={e => this.onInputChange(e, 'name')}></input><br></br>
                    <label htmlFor='desc' >Description:</label><br></br>
                    <textarea cols="30" rows="5" id='desc' value={this.state.item?.product.info.description} onChange={e => this.onInputChange(e, 'description')}></textarea><br></br>
                    <label htmlFor='amt'>Amount:</label><br></br>
                    <input type='text' id='amt' value={this.state.item?.product.info.amount} onChange={e => this.onInputChange(e, 'amount')}></input><br></br>
                    <label htmlFor='prc'>Price:</label><br></br>
                    <input type='text' id='prc' value={this.state.item?.product.info.price} onChange={e => this.onInputChange(e, 'price')}></input><br></br>
                    <label htmlFor='man'>Manufacturer:</label><br></br>
                    <select id='man'>
                        {manufacturers}
                    </select><br></br>
                    <label htmlFor='cat'>Category:</label><br></br>
                    <select id='cat'>
                        {categories}
                    </select><br></br>
                    <label htmlFor='subcat'>Subcategory:</label><br></br>
                    <select id='subcat'>
                        {subcategories}
                    </select><br></br>
                    <button type='submit' className='mt-2'>Submit</button>
                </form>
                </>
            )
        }
        else{
            return null;
        }
    }
}

export default Product;