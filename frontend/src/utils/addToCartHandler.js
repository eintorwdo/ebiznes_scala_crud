const addToCartHandler = (cookies, state) => {
    if(cookies && state){
        let cart = cookies.get('cart');
        const productState = state.product
        if(productState){
            if(productState.info.amount > 0){
                let index = cart.products.findIndex(product => product.id === productState.info.id);
                // if(index >= 0){
                //     cart.products[index].amount += 1;
                    
                // }
                if(index == -1){
                    cart.products.push({id: productState.info.id, amount: 1});
                }
                cookies.set('cart', cart, { path: '/' });
            }
        }
    }
}

export default addToCartHandler;