import React from 'react';
import { Redirect } from "react-router-dom";
import ConfirmModal from './partials/ConfirmModal.js';
import Button from 'react-bootstrap/Button';

import deleteItem from '../utils/deleteItem.js';

class Category extends React.Component {
    constructor(props){
        super(props);
        this.state = {item: null, showDeleteModal: false, categories: null, loading: true};
    }

    componentDidMount(){
        if(this.props.method !== 'add'){
            fetch(`http://localhost:9000/api/${this.props.type}/${this.props.match.params.id}`).then(res => res.json().then(cat => {
                if(this.props.type == 'subcategory'){
                    this.getCategories().then(res => {
                        this.setState({item: cat, categories: res, loading: false});
                    });
                }
                else{
                    this.setState({item: cat, loading: false});
                }
            }));
        }
        else{
            const item = {
                category: {
                    name: ''
                },
                subcategory: {
                    name: '',
                    category: 1
                }
            }
            this.getCategories().then(res => {
                this.setState({item, categories: res, loading: false});
            });
        }
    }

    getCategories = async () => {
        const categories = await fetch('http://localhost:9000/api/categories').then(res => res.json());
        return categories;
    }

    onInputChange = (e, property) => {
        let item = this.state.item;
        item[this.props.type][property] = e.target.value;
        this.setState({ item });
    }

    handleDelete = async () => {
        const res = await deleteItem(this.props.type, this.props.match.params.id, this.props.tokenInfo.token);
        if(res.status == 200){
            alert(`${this.props.type} deleted`);
        }
        else{
            alert(`error: ${res.statusText}`);
        }
        this.setState({item: null, showDeleteModal: false});
    }

    handleSubmit = async (e) => {
        e.preventDefault();
        const method = this.props.method == 'add' ? 'POST' : 'PUT';
        const payload = {
            name: e.target[0].value,
            category: parseInt(e.target[1].value) || null
        };
        const url = this.props.method === 'add' ? `http://localhost:9000/api/${this.props.type}` :
            `http://localhost:9000/api/${this.props.type}/${this.props.match.params.id}`
        const res = await fetch(url, {
            method,
            headers:{
                'X-Auth-Token': this.props.tokenInfo.token,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });
        if(res.status == 200){
            const msg = this.props.method === 'add' ? `${this.props.type} added` : `${this.props.type} updated`;
            alert(msg);
        }
        else{
            alert(`error: ${res.statusText}`);
        }
    }

    render(){
        let categories;
        let categorySelect;
        if(this.props.type === 'subcategory'){
            categories = this.state.categories?.categories.map(c => {
                return <option value={c.category.id} selected={c.category.id == this.state.item?.category.id}>{c.category.name}</option>;
            });
            categorySelect = (
                <>
                <label htmlFor='cat'>Category:</label><br></br>
                <select id='cat'>
                    {categories}
                </select><br></br>
                </>
            );
        }
        const deleteButton = this.props.method == 'add' ? null :
            <Button variant="danger" className="mt-5" onClick={() => this.setState({showDeleteModal: true})}>Delete {this.props.type}</Button>;
        if(this.state.item){
            return(
                <>
                <form onSubmit={this.handleSubmit}>
                    <label htmlFor='name'>Name:</label><br></br>
                    <input type='text' id='name' value={this.state.item?.[this.props.type].name} onChange={e => this.onInputChange(e, 'name')}></input><br></br>
                    {categorySelect}
                    <button type='submit' className='mt-2'>Submit</button>
                </form>
                {deleteButton}
                <ConfirmModal show={this.state.showDeleteModal} handleClose={() => this.setState({showDeleteModal: false})} handleDelete={this.handleDelete}/>
                </>
            )
        }
        else if(!this.state.loading){
            return <Redirect to={{pathname: '/management'}} />
        }
        else{
            return null;
        }
    }
}

export default Category;