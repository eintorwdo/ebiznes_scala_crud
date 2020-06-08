const initialState = {
    loggedIn: false,
    userId: null,
    userName: null,
    showLoginModal: false
}

export const rootReducer = (state = initialState, action) => {
    if(action.type === "LOG_IN"){
        return Object.assign({}, state, {
            loggedIn: true,
            userId: action.payload.id,
            userName: action.payload.name
        });
    }
    else if(action.type === "LOG_OUT"){
        return Object.assign({}, state, {
            loggedIn: false,
            userId: null,
            userName: null
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