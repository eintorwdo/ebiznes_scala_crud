const initialState = {
    loggedIn: true,
    userId: 1,
    userName: "test123@test.pl"
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
    return state;
}

export default rootReducer;