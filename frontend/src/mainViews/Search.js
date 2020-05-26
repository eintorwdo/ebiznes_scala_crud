import React from 'react';
import { BrowserRouter as Router, Route, Link } from "react-router-dom";
import queryString from 'query-string'

import Button from 'react-bootstrap/Button'
import Row from 'react-bootstrap/Row'
import Col from 'react-bootstrap/Col'
import Container from 'react-bootstrap/Container';
// import Container from 'react-bootstrap/Container'

let getProducts = async (query) => {
    let products = await fetch(`http://localhost:9000/api/products?query=${query}`);
    let productsJson = await products.json();
    return productsJson;
}

class Search extends React.Component {
    constructor(props){
        super(props);
        this.state = {query: "", products: []};
        this.state.query = this.props.location.search;
    }

    componentDidMount(){
        let q = queryString.parse(this.props.location.search);
        getProducts(q.query).then(p => {
            this.setState({products: p.products});
        });
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

export default Search;