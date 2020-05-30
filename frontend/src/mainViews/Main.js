import React from 'react';
import { BrowserRouter as Router, Route, Link } from "react-router-dom";

import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import Jumbotron from 'react-bootstrap/Jumbotron';
import Carousel from 'react-bootstrap/Carousel';

let getProducts = async () => {
    let products = await fetch('http://localhost:9000/api/recproducts');
    let productsJson = await products.json();
    return productsJson;
}

class Main extends React.Component {
    constructor(props){
        super(props);
        this.state = {products: []};
    }

    componentDidMount(){
        getProducts().then(prds => {
            this.setState({products: prds.products});
        });
    }

    render(){
        const products = this.state.products.map(p => {
            return (
                <Carousel.Item key={p.id}>
                    <img
                    className="d-block carousel-img"
                    src="https://s28943.pcdn.co/wp-content/uploads/2019/09/placeholder.jpg"
                    alt="First slide"
                    />
                    <Carousel.Caption>
                        <Link to={`/product/${p.id}`}><h2>{p.name}</h2></Link>
                        <h4>{p.price}zl</h4>
                    </Carousel.Caption>
                </Carousel.Item>
            );
        });
        return(
            <>
            {/* <Container>
                <Row className="productListItem mt-2 mb-2 p-4">
                    <Col>
                        <h2>Best deals:</h2>
                    </Col>
                </Row>
            </Container> */}
            <Jumbotron fluid className="p-2 mt-4">
                <Container className="p-0">
                    <Carousel>
                        {products}
                    </Carousel>
                </Container>
            </Jumbotron>
            </>
        )
    }
}

export default Main;