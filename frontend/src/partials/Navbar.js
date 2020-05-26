import React from 'react';
import Navbar from 'react-bootstrap/Navbar'
import Nav from 'react-bootstrap/Nav'
import Form from 'react-bootstrap/Form'
import FormControl from 'react-bootstrap/FormControl'
import Button from 'react-bootstrap/Button'
import NavDropdown from 'react-bootstrap/NavDropdown'
import NavItem from 'react-bootstrap/NavItem';
import Dropdown from 'react-bootstrap/Dropdown'
import DropdownButton from 'react-bootstrap/DropdownButton'

import { BrowserRouter as Router, Route, Link } from "react-router-dom";
// import ButtonGroup from 'react-bootstrap/ButtonGroup'
// import Row from 'react-bootstrap/Row'
// import Col from 'react-bootstrap/Col'
// import Container from 'react-bootstrap/Container'

let getCategories = async () => {
    let categories = await fetch('http://localhost:9000/api/categories');
    let categoriesJson = await categories.json();
    return categoriesJson;
}

class MyNavbar extends React.Component {
    constructor(){
        super();
        this.state = {loggedIn: false, username: "testname@gmail.com", categories: [], query: ""};
    }

    componentDidMount(){
        getCategories().then(data => {
            this.setState({categories: data});
        });
    }

    searchClick = (e) => {
        e.preventDefault();
        var q = document.getElementById("searchBar").value;
        if(q.length > 0){
            this.setState({query: `/search?query=${q}`}, ()=>{
                document.getElementById("srcLink").click();
            });
        }
        else{
            this.setState({query: '#'}, ()=>{
                document.getElementById("srcLink").click();
            });
        }
    }

    render(){
        let userInfo;
        let categoryList;
        if(this.state.loggedIn){
            userInfo = (
                <>
                <NavItem className="d-flex align-items-center justify-content-center m-2">
                    <DropdownButton alignRight title={this.state.username} id="dropdown-menu-align-right" className="w-100">
                        <Dropdown.Item >Profile</Dropdown.Item>
                        <Dropdown.Item >Orders</Dropdown.Item>
                        <Dropdown.Divider />
                        <Dropdown.Item >Log out</Dropdown.Item>
                    </DropdownButton>
                </NavItem>
                </>
            );
        }
        else{
            userInfo = <Button className="m-2">Log in</Button>
        }

        categoryList = this.state.categories.map(cat => {
            return <NavDropdown.Item key={cat.id} id={cat.id}>{cat.name}</NavDropdown.Item>;
        });

        return(
            <>
            <Navbar bg="dark" variant="dark" expand="lg">
                <Navbar.Brand href="/">Store</Navbar.Brand>
                <Navbar.Toggle aria-controls="basic-navbar-nav" />
                <Navbar.Collapse id="basic-navbar-nav">
                    <Nav className="mr-auto">
                        <NavItem className="d-flex align-items-center justify-content-center m-2">
                            <NavDropdown title="Categories" id="dropdown-menu" className="w-100">
                                {categoryList}
                            </NavDropdown>
                        </NavItem>
                    </Nav>
                    <Nav >
                        <Form inline className="p-2 mr-md-2 justify-content-center">
                            <FormControl type="text" placeholder="Search" id="searchBar" className="mr-sm-2 mr-xs-0 mb-sm-0 mb-2" />
                            <Button onClick={this.searchClick} variant="outline-info">Search</Button>
                            <Link to={this.state.query} style={{display: "none"}} id='srcLink'></Link>
                        </Form>
                        <Button className="m-2"><i className="fas fa-shopping-cart"></i></Button>
                        {userInfo}
                    </Nav>
                </Navbar.Collapse>
            </Navbar>
            </>
        );
    }
}

export default MyNavbar;