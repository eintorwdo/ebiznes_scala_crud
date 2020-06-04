const getProducts = async (ids = []) => {
    if(ids.length > 0){
        let query = "?";
        ids.forEach(id => {query+=`id=${id}&`});
        query = query.slice(0, -1);
        let products = await fetch(`http://localhost:9000/api/products/ids${query}`);
        let productsJson = await products.json();
        return productsJson;
    }
    else{
        return new Promise(resolve => {resolve({products: []})});
    }
}

export default getProducts;