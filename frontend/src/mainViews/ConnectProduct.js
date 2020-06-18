import React from 'react';

import Button from 'react-bootstrap/Button';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import OverlayTrigger from 'react-bootstrap/OverlayTrigger';
import Tooltip from 'react-bootstrap/Tooltip';
import Modal from 'react-bootstrap/Modal';
import Form from 'react-bootstrap/Form';

import addToCartHandler from '../utils/addToCartHandler.js';

import { Link } from "react-router-dom";
import { Redirect } from "react-router-dom";
import { connect } from "react-redux";

function select(state, ownProps){
    return {
        loggedIn: state.loggedIn,
        cookies: ownProps.cookies,
        userId: state.userId,
        token: state.token
    }
}

let getProduct = async (id) => {
    let product = await fetch(`http://localhost:9000/api/product/${id}`);
    let productJson = await product.json();
    return productJson;
}

class Product extends React.Component {
    constructor(props){
        super(props);
        this.state = {userId: this.props.userId, product: undefined, id: this.props.match.params.id, reviews: [], loading: true, loggedIn: this.props.loggedIn, modalVisible: false, reviewValidated: null, redirect: null, errorMsg: null};
    }

    componentDidMount(){
        getProduct(this.state.id).then(p => {
            this.setState({product: p.product, reviews: p.reviews, loading: false});
        });
    }

    addToCart = (e) => {
        e.preventDefault();
        const { cookies } = this.props;
        addToCartHandler(cookies, this.state);
    }

    addReview = (e) => {
        e.preventDefault();
        this.setState({modalVisible: true});
    }

    handleSubmit = async (e) => {
        const form = e.currentTarget;
        if (form.checkValidity() === false) {
            e.preventDefault();
            e.stopPropagation();
        }
        else{
            e.preventDefault();
            e.stopPropagation();
            const body = {
                description: e.target.description.value,
                product: this.state.product.info.id
            }
            const response = await fetch('http://localhost:9000/api/review', {
                method: 'POST',
                body: JSON.stringify(body),
                headers: {
                    'Content-Type': 'application/json',
                    'X-Auth-Token': this.props.token
                }
            });
            const responseJson = await response.json();
            if(response.status === 200){
                let reviews = this.state.reviews;
                reviews.push(responseJson);
                this.setState({modalVisible: false, reviews});
            }
            else{
                this.setState({redirect: '/error', errorMsg: responseJson.message});
            }
        }
        this.setState({reviewValidated: true})
    }

    render(){
        let prd;
        let description;
        let reviews;
        let addReviewButton = <Button onClick={this.addReview}>Add review</Button>;
        let subcategory;


        if(this.state.product){
            if(this.state.loading === false && this.state.loggedIn === false){
                addReviewButton = (
                    <OverlayTrigger
                        key="right"
                        placement="right"
                        overlay={
                        <Tooltip>
                            Log in to add reviews
                        </Tooltip>
                        }
                    >
                        <Button>Add review</Button>
                    </OverlayTrigger>
                );
            }

            // if(this.state.product){
                description = <p><span className="font-weight-bold">Product description:</span> {this.state.product.info.description}</p>;
                if(this.state.product.subcategory){
                    if(this.state.product.subcategory.id !== 0){
                        subcategory = (
                            <ul className="p-0 pl-md-4">
                                <Link to={`/subcategory/${this.state.product.subcategory.id}`}><li><h6>{this.state.product.subcategory.name}</h6></li></Link>
                            </ul>
                        );
                    }
                }
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
                                        {subcategory}
                                        
                                    </ul>
                                </Col>
                            </Row>
                        </Col>
                        <Col className="text-sm-center text-md-left text-lg-right">
                            <Row>
                                <Col sm={12}><h1>{this.state.product.info.price}zl</h1></Col>
                            </Row>
                            <Row className="d-flex justify-content-end">
                                <Col><Button className="m-0" onClick={this.addToCart}>Add to cart</Button></Col>
                            </Row>
                        </Col>
                    </Row>
                    </>
                );
            // }

            if(this.state.reviews.length > 0){
                reviews = this.state.reviews.map(r => {
                    return (
                        <div key={r.id}>
                        <Row className="text-left">
                            <Col>
                                <p className="mb-0">{r.username}:</p>
                            </Col>
                        </Row>
                        <Row className="text-left mt-2">
                            <Col>
                                <h6 className="mb-0">{r.description}</h6>
                            </Col>
                        </Row>
                        <Row className="text-left">
                            <Col>
                                <p className="mb-0">{r.date}</p>
                                <hr></hr>
                            </Col>
                        </Row>
                        </div>
                    );
                });
            }
            else if(this.state.reviews.length === 0 && this.state.loading === false){
                reviews = <h4>No reviews</h4>
            }

            if(!this.state.redirect){
                return(
                    <>
                    <Row className="mt-3">
                        <Container className="pt-3 pb-3 d-flex justify-content-center flex-wrap productListItem">
                            <Col>
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
                    <Row className="mt-3">
                        <Container>
                            <Col sm={12}className="text-sm-center text-lg-left">
                                <h3>Reviews:</h3>
                            </Col>
                        </Container>
                    </Row>
                    <Row className="mt-1">
                        <Container>
                            <Col sm={12}className="text-sm-center text-lg-left">
                                {addReviewButton}
                            </Col>
                        </Container>
                    </Row>
                    <Row className="mt-3">
                        <Container className="pt-3 pb-3 d-flex justify-content-center flex-wrap productListItem">
                            <Col sm={12}>
                                {reviews}
                            </Col>
                        </Container>
                    </Row>
                    <Modal
                        show={this.state.modalVisible}
                        size="lg"
                        aria-labelledby="contained-modal-title-vcenter"
                        centered
                        onHide={() => {this.setState({modalVisible: false})}}
                        >
                        <Modal.Header closeButton onClick={() => {this.setState({modalVisible: false})}}>
                            <Modal.Title id="contained-modal-title-vcenter">
                            Add review
                            </Modal.Title>
                        </Modal.Header>
                        <Form noValidate validated={this.state.reviewValidated} onSubmit={this.handleSubmit}>
                            <Modal.Body>
                                <Form.Group controlId="description">
                                    <Form.Label>Your review:</Form.Label>
                                    <Form.Control as="textarea" rows="3" required/>
                                </Form.Group>
                            </Modal.Body>
                                <Modal.Footer>
                                    <Button variant="primary" type="submit" size="lg">
                                        Submit Review
                                    </Button>
                                </Modal.Footer>
                        </Form>
                    </Modal>
                    </>
                );
            }
            else{
                return <Redirect to={{pathname: this.state.redirect, state: this.state.errorMsg}}/>
            }
        }
        else{
            return null;
        }
    }
}

const ConnectProduct = connect(select)(Product)
export default ConnectProduct;