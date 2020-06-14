import React from 'react';
import Button from 'react-bootstrap/Button';
import { BrowserRouter as Router, Route, Link, Redirect } from "react-router-dom";

class Order extends React.Component {
    constructor(props){
        super(props);
        this.state = {item: null};
    }

    componentDidMount(){
        this.getData().then(order => {
            this.setState({item: order});
        });
    }

    getData = async () => {
        const order = await fetch(`http://localhost:9000/api/order/${this.props.match.params.id}`, {headers: {'X-Auth-Token': this.props.tokenInfo.token}}).then(res => res.json());
        return order;
    }

    render(){
        const delivery = <li>delivery: {this.state.item?.order.delivery.name}</li>
        const payment = <li>payment: {this.state.item?.order.payment.name}</li>
        const infoEntries = this.state.item ? Object.entries(this.state.item.order.info) : [] ;
        const orderInfo = infoEntries.map(entry => {
            if(entry[0] != "payment" && entry[0] != "delivery"){
                return <li>{entry[0]}: {entry[1]}</li>;
            }
            else{
                return null;
            }
        });
        const details = this.state.item?.details.map(detail => {
            const entries = Object.entries(detail);
            const detailList = entries.map(e => {
                return <li>{e[0]}: {e[1]}</li>;
            });
            return <ul>{detailList}</ul>;
        });
        if(this.state.item){
            return(
                <>
                <h3>Order info:</h3>
                <Button as={Link} to={`/management/order/${this.props.match.params.id}/update`}>Update</Button>
                <ul className="mt-3">
                    {orderInfo}
                    {delivery}
                    {payment}
                </ul>
                <hr></hr>
                <h3>Order details:</h3>
                <ul className="mt-3">{details}</ul>
                </>
            )
        }
        else{
            return null;
        }
    }
}

export default Order;