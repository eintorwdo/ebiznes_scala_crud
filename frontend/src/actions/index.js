export function logIn(payload){
    return {type: "LOG_IN", payload}
}

export function logOut(){
    return {type: "LOG_OUT"}
}

export function showLogin(){
    return {type: "SHOW_LOGIN"}
}

export function hideLogin(){
    return {type: "HIDE_LOGIN"}
}