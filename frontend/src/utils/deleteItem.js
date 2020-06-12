const deleteItem = async (type, id, token) => {
    const res = await fetch(`http://localhost:9000/api/${type}/${id}`, {
        method: 'DELETE',
        headers:{
            'X-Auth-Token': token
        }
    });
    return res;
}

export default deleteItem;
