import React from 'react';


import Container from 'react-bootstrap/Container';

class Error extends React.Component {
    constructor(props){
        super(props);
        
    }

    render(){
        return(
            <>
            <Container fluid className="main p-5 mt-5">
                <h2>{this.props.location.state}</h2>
            </Container>
            </>
        )
    }
}

export default Error;