const checkIfLoggedIn = (token, tokenExpiry) => {
    if(token && tokenExpiry){
        if(new Date(parseInt(tokenExpiry)) > new Date()){
            return true;
        }
        return false;
    }
    return false;
}

export default checkIfLoggedIn;