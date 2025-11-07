package me.danielvillamizar.unabstore

import com.google.firebase.firestore.FirebaseFirestore

class ProductoRepository {
    private val db = FirebaseFirestore.getInstance()
    private val col = db.collection("productos")

    fun agregarProducto(producto: Producto, onResult: (Boolean, String?) -> Unit) {
        col.add(producto)
            .addOnSuccessListener { onResult(true, it.id) }
            .addOnFailureListener { e -> onResult(false, e.message) }
    }

    fun obtenerProductos(onSuccess: (List<Producto>) -> Unit, onError: (String) -> Unit) {
        col.get()
            .addOnSuccessListener { snap ->
                onSuccess(snap.map { d -> d.toObject(Producto::class.java).copy(id = d.id) })
            }
            .addOnFailureListener { e -> onError(e.message ?: "Error al obtener productos") }
    }

    fun eliminarProducto(id: String, onResult: (Boolean, String?) -> Unit) {
        col.document(id).delete()
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.message) }
    }
}