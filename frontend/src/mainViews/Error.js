import React from 'react';

import queryString from 'query-string';
import Container from 'react-bootstrap/Container';

class Error extends React.Component {
    constructor(props){
        super(props);
        
    }

    render(){
        const q = queryString.parse(this.props.location.search);
        const message = this.props.location.state || q.msg
        return(
            <>
            <Container fluid className="main p-5 mt-5">
                <h2>{message}</h2>
            </Container>
            </>
        )
    }
}

export default Error;