import { createStore } from 'redux';
import rootReducer from '../reducers/reducer.js';

export const store = createStore(rootReducer);