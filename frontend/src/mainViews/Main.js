import React from 'react';
import { Link } from "react-router-dom";

import Container from 'react-bootstrap/Container';
import Jumbotron from 'react-bootstrap/Jumbotron';
import Carousel from 'react-bootstrap/Carousel';
import Tabs from 'react-bootstrap/Tabs'
import Tab from 'react-bootstrap/Tab'

let getProducts = async () => {
    let products = await fetch('http://localhost:9000/api/recproducts?cat1=1&cat2=2&cat3=3');
    let productsJson = await products.json();
    return productsJson;
}

class Main extends React.Component {
    constructor(props){
        super(props);
        this.state = {products: [], categories: []};
    }

    componentDidMount(){
        getProducts().then(prds => {
            this.setState({products: [prds.products1, prds.products2, prds.products3], categories: prds.categories});
        });
    }

    render(){
        const products = this.state.products.map((prds, i) => {
            return (
                <Tab eventKey={this.state.categories[i].name} title={this.state.categories[i].name}>
                    <Jumbotron fluid className="p-2 mt-0">
                        <Container className="p-0">
                            <Carousel>
                                {
                                    prds.map(p => {
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
                                    })
                                }
                            </Carousel>
                        </Container>
                    </Jumbotron>
                </Tab>
            );
        });
        return(
            <>
            <Container fluid className="main mt-3">
                <h4 className="mt-2 mb-2">Reccomended items:</h4>
            
            <Tabs id="uncontrolled-tab-example" className="d-flex justify-content-center mt-2">
                {products}
            </Tabs>
            </Container>
            </>
        )
    }
}

export default Main;