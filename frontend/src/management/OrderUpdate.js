import React from 'react';
import ConfirmModal from './partials/ConfirmModal.js';
import Button from 'react-bootstrap/Button';
import { Redirect } from "react-router-dom";

import deleteItem from '../utils/deleteItem.js';

class OrderUpdate extends React.Component {
    constructor(props){
        super(props);
        this.state = {item: null, showDeleteModal: false, loading: true};
    }

    componentDidMount(){
        this.getData().then(order => {
            this.setState({item: order, loading: false});
        });
    }

    getData = async () => {
        const order = await fetch(`http://localhost:9000/api/order/${this.props.match.params.id}`).then(res => res.json());
        return order;
    }

    handleSubmit = async (e) => {
        e.preventDefault();
        const payload = {
            price: parseInt(e.target[0].value),
            address: e.target[1].value,
            sent: parseInt(e.target[2].value),
            paid: parseInt(e.target[3].value),
            packageNr: e.target[4].value
        };
        const url = `http://localhost:9000/api/order/${this.props.match.params.id}`;
        const res = await fetch(url, {
            method: 'PUT',
            headers:{
                'X-Auth-Token': 'xD',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });
        if(res.status == 200){
            const msg = 'order updated';
            alert(msg);
        }
        else{
            alert(`error: ${res.statusText}`);
        }
    }

    onInputChange = (e, property) => {
        let item = this.state.item;
        item.order.info[property] = e.target.value;
        this.setState({ item });
    }

    handleDelete = async () => {
        const res = await deleteItem('order', this.props.match.params.id);
        if(res.status == 200){
            alert('order deleted');
        }
        else{
            alert(`error: ${res.statusText}`);
        }
        this.setState({item: null, showDeleteModal: false});
    }

    render(){
        if(this.state.item){
            const sentSelect = (
                <>
                <option value='0' selected={this.state.item.order.info.sent == 0}>No</option>
                <option value='1' selected={this.state.item.order.info.sent == 1}>Yes</option>
                <option value='2' selected={this.state.item.order.info.sent == 2}>Delivered</option> 
                </>
            );
            const paidSelect = (
                <>
                <option value='0' selected={this.state.item.order.info.paid == 0}>No</option>
                <option value='1' selected={this.state.item.order.info.paid == 1}>Yes</option>
                </>
            );
            const deleteButton = <Button variant="danger" className="mt-5" onClick={() => this.setState({showDeleteModal: true})}>Delete order</Button>;
            return(
                <>
                <form onSubmit={this.handleSubmit}>
                    <label htmlFor='price'>Price:</label><br></br>
                    <input type='text' id='price' value={this.state.item?.order.info.price} onChange={e => this.onInputChange(e, 'price')}></input><br></br>
                    <label htmlFor='address' >Address:</label><br></br>
                    <textarea cols="30" rows="5" id='address' value={this.state.item?.order.info.address} onChange={e => this.onInputChange(e, 'address')}></textarea><br></br>
                    <label htmlFor='sent' >Sent:</label><br></br>
                    <select id='sent'>
                        {sentSelect}
                    </select><br></br>
                    <label htmlFor='paid'>Paid:</label><br></br>
                    <select id='paid'>
                        {paidSelect}
                    </select><br></br>
                    <label htmlFor='package'>Package nr:</label><br></br>
                    <input type='text' id='package' value={this.state.item?.order.info.packageNr} onChange={e => this.onInputChange(e, 'packageNr')}></input><br></br>
                    <button type='submit' className='mt-2'>Submit</button>
                </form>
                {deleteButton}
                <ConfirmModal show={this.state.showDeleteModal} handleClose={() => this.setState({showDeleteModal: false})} handleDelete={this.handleDelete}/>
                </>
            )
        }
        else if(!this.state.loading){
            return <Redirect to={{pathname: '/management/orders'}} />
        }
        else{
            return null;
        }
    }
}

export default OrderUpdate;