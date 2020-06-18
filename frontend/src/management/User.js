import React from 'react';
import ConfirmModal from './partials/ConfirmModal.js';
import Button from 'react-bootstrap/Button';
import { Link, Redirect } from "react-router-dom";

import deleteItem from '../utils/deleteItem.js';

class User extends React.Component {
    constructor(props){
        super(props);
        this.state = {item: null, showDeleteModal: false, loading: true};
    }

    componentDidMount(){
        this.getData().then(user => {
            this.setState({item: user, loading: false});
        });
    }

    getData = async () => {
        const order = await fetch(`http://localhost:9000/api/user/${this.props.match.params.id}`, {headers: {'X-Auth-Token': this.props.tokenInfo.token}}).then(res => res.json());
        return order;
    }

    handleSubmit = async (e) => {
        e.preventDefault();
        const payload = {
            role: e.target[0].value
        };
        const url = `http://localhost:9000/api/user/${this.props.match.params.id}`;
        const res = await fetch(url, {
            method: 'PUT',
            headers:{
                'X-Auth-Token': this.props.tokenInfo.token,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });
        if(res.status == 200){
            const msg = 'user role updated';
            alert(msg);
        }
        else{
            alert(`error: ${res.statusText}`);
        }
    }

    onInputChange = (e, property) => {
        let item = this.state.item;
        item[property] = e.target.value;
        this.setState({ item });
    }

    handleDelete = async () => {
        const res = await deleteItem('user', this.props.match.params.id, this.props.tokenInfo.token);
        if(res.status == 200){
            alert('user deleted');
        }
        else{
            alert(`error: ${res.statusText}`);
        }
        this.setState({item: null, showDeleteModal: false});
    }

    render(){
        if(this.state.item){
            const roleSelect = (
                <>
                <option value='ADMIN' selected={this.state.item.role == "ADMIN"}>ADMIN</option>
                <option value='REGULAR' selected={this.state.item.role == "REGULAR"}>REGULAR</option>
                </>
            );
            const orders = this.state.item.orders.map(order => {
                return <Link to={`/management/order/${order.id}`}><li>id: {order.id}, ordered: {order.date}</li></Link>
            })
            const deleteButton = <Button variant="danger" className="mt-5" onClick={() => this.setState({showDeleteModal: true})}>Delete user</Button>;
            return(
                <>
                <ul>
                    <li>
                        id: {this.state.item.id}
                    </li>
                    <li>
                        firstname: {this.state.item.name}
                    </li>
                    <li>
                        lastname: {this.state.item.lastname}
                    </li>
                    <li>
                        email: {this.state.item.email}
                    </li>
                </ul>
                <form onSubmit={this.handleSubmit}>
                    <label htmlFor='role'>Role:</label><br></br>
                    <select id='role'>
                        {roleSelect}
                    </select><br></br>
                    <button type='submit' className='mt-2'>Submit</button>
                </form>
                <hr></hr>
                <h4>Orders</h4>
                <ul>
                    {orders}
                </ul>
                {deleteButton}
                <ConfirmModal show={this.state.showDeleteModal} handleClose={() => this.setState({showDeleteModal: false})} handleDelete={this.handleDelete}/>
                </>
            )
        }
        else if(!this.state.loading){
            return <Redirect to={{pathname: '/management/users'}} />
        }
        else{
            return null;
        }
    }
}

export default User;