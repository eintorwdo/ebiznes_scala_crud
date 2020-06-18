import React from 'react';
import _ from 'lodash';

import Table from 'react-bootstrap/Table';

class ProductTable extends React.Component {
    constructor(props){
        super(props);
        this.state = {delivery: this.props.delivery, details: this.props.details};
    }

    componentDidUpdate(prevProps){
        if(!_.isEqual(this.props.details, prevProps.details) || !_.isEqual(this.props.delivery, prevProps.delivery)){
            this.setState({details: this.props.details, delivery: this.props.delivery});
        }
    }

    render(){
        const deliveryRow = this.state.delivery && this.state.details ? (
            <tr>
                <td>
                    {this.state.details.length + 1}
                </td>
                <td>
                    {this.state.delivery.name}
                </td>
                <td>
                    {this.state.delivery.price}
                </td>
                <td>
                    1
                </td>
            </tr>
        ) : "";

        // console.log(this.state.details)
        const details = this.state.details ? this.state.details.map((o, i) => {
            return (
                <tr key={i}>
                    <td>
                        {i+1}
                    </td>
                    <td>
                        {o.name}
                    </td>
                    <td>
                        {o.price}
                    </td>
                    <td>
                        {o.amount}
                    </td>
                </tr>
            );
        }) : "";

        return(
            <>
            <Table striped bordered hover className="mt-2">
                <thead>
                    <tr>
                        <th>#</th>
                        <th>Product name</th>
                        <th>Price</th>
                        <th>Amount</th>
                    </tr>
                </thead>
                <tbody>
                    {details}
                    {deliveryRow}
                </tbody>
            </Table>
            </>
        )
    }
}

export default ProductTable;