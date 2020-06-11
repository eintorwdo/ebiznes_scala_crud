import React from 'react';
// import logo from './logo.svg';
import './App.css';

import 'bootstrap/dist/css/bootstrap.min.css';
import ConnectHome from './mainViews/Home.js'
import { BrowserRouter as Router, Route, Link } from "react-router-dom";

function App() {
  return (
    <div className="App">
        <ConnectHome />
    </div>
  );
}

export default App;
