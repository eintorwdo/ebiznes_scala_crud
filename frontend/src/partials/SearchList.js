import React from 'react';
import { BrowserRouter as Router, Route, Link } from "react-router-dom";

import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import Button from 'react-bootstrap/Button';

import _ from 'lodash';
// import chunk from 'lodash/chunk';

class SearchList extends React.Component {
    constructor(props){
        super(props);
        this.state = {products: this.props.products};
    }

    componentDidUpdate(prevProps){
        if(!_.isEqual(prevProps.products, this.props.products)){
            this.setState({products: this.props.products});
        }
    }

    render(){
        let productNodes = this.state.products.map(p => {
            return (
                <Row key={p.id} className="d-flex p-3 ml-5 mr-5 mb-4 mt-4 productListItem">
                    <Col className="listItemImageWrapper">
                        <Container fluid className="listItemImage"></Container>
                    </Col>
                    <Col xl={6} md={8}>
                        <Link to={`/product/${p.id}`} className="d-flex w-100" style={{color: "initial"}}>
                            <Row>
                            <Col><h2 className="text-left">{p.name}</h2></Col>
                            </Row>
                        </Link>
                        <Row>
                            <Col><h3 className="text-left">Manufacturer: {p.manufacturer}</h3></Col>
                        </Row>
                        <Row>
                            <Col><h4 className="text-left">Amount in stock: {p.amount}</h4></Col>
                        </Row>
                        
                    </Col>
                    <Col md={6} xl={3}> 
                        <Row>
                            <Col><h4 className="text-left text-xl-right">Price: {p.price}zl</h4></Col>
                        </Row>
                        <Row>
                            <Col className="d-flex justify-content-start justify-content-xl-end"><Button className="m-0 m-lg-2">Add to cart</Button></Col>
                        </Row>
                    </Col>
                </Row>
            );
        });
        
        return(
            <>
            <Row className="mt-3">
                <Col>
                    {productNodes}
                </Col>
            </Row>
            </>
        );
    }
}

export default SearchList;