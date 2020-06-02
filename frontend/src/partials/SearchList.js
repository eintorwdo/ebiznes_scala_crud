import React from 'react';
import { BrowserRouter as Router, Route, Link } from "react-router-dom";

import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import Button from 'react-bootstrap/Button';
import Breadcrumb from 'react-bootstrap/Breadcrumb';

import addToCartHandler from '../utils/addToCartHandler.js';

import _ from 'lodash';
// import chunk from 'lodash/chunk';

class SearchList extends React.Component {
    constructor(props){
        super(props);
        this.state = {products: this.props.products, type: this.props.type, category: this.props.category, subcategory: this.props.subcategory};
    }

    componentDidUpdate(prevProps){
        if(!_.isEqual(prevProps.products, this.props.products) || !_.isEqual(prevProps.type, this.props.type) || !_.isEqual(prevProps.category, this.props.category) || !_.isEqual(prevProps.subcategory, this.props.subcategory)){
            this.setState({products: this.props.products, type: this.props.type, category: this.props.category, subcategory: this.props.subcategory});
        }
    }
    
    addToCart = (e) => {
        e.preventDefault();
        const { cookies } = this.props;
        if(this.state.products){
            const prd = this.state.products.find(p => p.id === parseInt(e.target.id));
            const state = {
                product: {
                    info: prd
                }
            }
            addToCartHandler(cookies, state);
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
                            <Col className="d-flex justify-content-start justify-content-xl-end"><Button className="m-0 m-lg-2" id={p.id} onClick={this.addToCart}>Add to cart</Button></Col>
                        </Row>
                    </Col>
                </Row>
            );
        });
        
        let breadcrumbItems;
        if(this.state.category && !this.state.subcategory && this.state.products){
            if(this.state.products.length > 0){
                breadcrumbItems = (
                    <>
                    <Breadcrumb.Item href="/">Home</Breadcrumb.Item>
                    <Breadcrumb.Item active>{this.state.category.name}</Breadcrumb.Item>
                    </>
                );
            }
        }
        else if(this.state.subcategory && this.state.products){
            if(this.state.products.length > 0){
                breadcrumbItems = (
                    <>
                    <Breadcrumb.Item href="/">Home</Breadcrumb.Item>
                    <Breadcrumb.Item href={`/category/${this.state.category.id}`}>{this.state.category.name}</Breadcrumb.Item>
                    <Breadcrumb.Item active>{this.state.subcategory.name}</Breadcrumb.Item>
                    </>
                );
            }
        }
        else{
            if(this.state.products.length > 0){
                breadcrumbItems = (
                    <>
                    <Breadcrumb.Item href="/">Home</Breadcrumb.Item>
                    <Breadcrumb.Item active>All categories</Breadcrumb.Item>
                    </>
                );
            }
        }

        return(
            <>
            <Row>
                <Col className="ml-5 mr-5 pl-3 pr-3 mt-3">
                        <Breadcrumb className="mb-0 searchNav">
                            {breadcrumbItems}
                        </Breadcrumb>
                </Col>
            </Row>
            <Row className="mt-0">
                <Col>
                    {productNodes}
                </Col>
            </Row>
            </>
        );
    }
}

export default SearchList;