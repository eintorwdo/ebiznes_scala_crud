import React from 'react';

import Button from 'react-bootstrap/Button'
import Row from 'react-bootstrap/Row'
import Col from 'react-bootstrap/Col'
import Container from 'react-bootstrap/Container';
import { BrowserRouter as Router, Route, Link } from "react-router-dom";

let getProduct = async (id) => {
    let product = await fetch(`http://localhost:9000/api/product/${id}`);
    let productJson = await product.json();
    return productJson;
}

class Product extends React.Component {
    constructor(props){
        super(props);
        this.state = {product: undefined, id: this.props.match.params.id, reviews: []};
    }

    componentDidMount(){
        getProduct(this.state.id).then(p => {
            this.setState({product: p.product, reviews: p.reviews});
        });
    }

    render(){
        let prd;
        let description;
        if(this.state.product){
            description = <p><span className="font-weight-bold">Product description:</span> {this.state.product.info.description}</p>;
            prd = (
                <>
                <Row className="mt-lg-5 mt-0">
                    <Col sm={12} lg={6} className="text-md-left text-sm-center">
                        <Row>
                            <Col><h1>{this.state.product.info.name}</h1></Col>
                        </Row>
                        <Row className="mt-3">
                            <Col><h4>Manufacturer: {this.state.product.manufacturer.name}</h4></Col>
                        </Row>
                        <Row>
                            <Col><h4>In stock: {this.state.product.info.amount}</h4></Col>
                        </Row>
                        <Row className="mt-1">
                            <Col>
                                <ul style={{listStyleType: "none"}} className="p-0">
                                    <Link to={`/category/${this.state.product.category.id}`}><li><h5>{this.state.product.category.name}</h5></li></Link>
                                    <ul className="p-0 pl-md-4">
                                        <Link to={`/subcategory/${this.state.product.subcategory.id}`}><li><h6>{this.state.product.subcategory.name}</h6></li></Link>
                                    </ul>
                                </ul>
                            </Col>
                        </Row>
                    </Col>
                    <Col className="text-sm-center text-md-left text-lg-right">
                        <Row>
                            <Col sm={12}><h1>{this.state.product.info.price}zl</h1></Col>
                        </Row>
                        <Row className="d-flex justify-content-end">
                            <Col><Button className="m-0">Add to cart</Button></Col>
                        </Row>
                    </Col>
                </Row>
                </>
            );
        }
        return(
            <>
            <Row className="mt-3">
                <Container className="pt-3 pb-3 d-flex justify-content-center flex-wrap productListItem">
                    <Col >
                        <img id="productImage" src="https://dimensionmill.org/wp-content/uploads/2019/03/square-placeholder.jpg"></img>
                    </Col>
                    <Col md={6} xs={12} className="w-50 pt-md-4 pt-0 pb-4">
                        {prd}
                    </Col>
                    <Row className="mt-3 w-100">
                        <Col sm={12}>
                            {description}
                        </Col>
                    </Row>
                </Container>
            </Row>
            </>
        );
    }
}

export default Product;