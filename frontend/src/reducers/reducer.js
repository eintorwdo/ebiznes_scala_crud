const initialState = {
    loggedIn: false,
    userId: null,
    userName: null,
    showLoginModal: false,
    token: null,
    tokenExpiry: null,
    role: null
}

export const rootReducer = (state = initialState, action) => {
    if(action.type === "LOG_IN"){
        return Object.assign({}, state, {
            loggedIn: true,
            userName: action.payload.email,
            token: action.payload.token,
            tokenExpiry: parseInt(action.payload.tokenExpiry),
            role: action.payload.role
        });
    }
    else if(action.type === "LOG_OUT"){
        return Object.assign({}, state, {
            loggedIn: false,
            userName: null,
            token: null,
            tokenExpiry: null,
            role: null
        });
    }
    else if(action.type === "SHOW_LOGIN"){
        return Object.assign({}, state, {
            showLoginModal: true
        });
    }
    else if(action.type === "HIDE_LOGIN"){
        return Object.assign({}, state, {
            showLoginModal: false
        });
    }
    return state;
}

export default rootReducer;