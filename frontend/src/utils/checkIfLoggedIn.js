const checkIfLoggedIn = (token, tokenExpiry) => {
    if(token && tokenExpiry){
        if(new Date(parseInt(tokenExpiry)) > new Date()){
            return true;
        }
        else{
            return false;
        }
    }
    else{
        return false;
    }
}

export default checkIfLoggedIn;