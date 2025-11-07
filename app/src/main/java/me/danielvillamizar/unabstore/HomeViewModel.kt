package me.danielvillamizar.unabstore

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {
    private val repo = ProductoRepository()
    val productos = MutableLiveData<List<Producto>>(emptyList())

    fun cargarProductos() {
        repo.obtenerProductos(
            onSuccess = { productos.value = it },
            onError = { productos.value = emptyList() }
        )
    }

    fun agregarProducto(nombre: String, descripcion: String, precio: Double, done: (Boolean, String?) -> Unit = {_,_->}) {
        val p = Producto(nombre = nombre, descripcion = descripcion, precio = precio)
        repo.agregarProducto(p) { ok, msg ->
            if (ok) cargarProductos()
            done(ok, msg)
        }
    }

    fun eliminarProducto(id: String, done: (Boolean, String?) -> Unit = {_,_->}) {
        repo.eliminarProducto(id) { ok, msg ->
            if (ok) cargarProductos()
            done(ok, msg)
        }
    }
}