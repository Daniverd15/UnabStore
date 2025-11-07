package me.danielvillamizar.unabstore

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onClickLogout: () -> Unit = {}) {
    // Firebase auth
    val auth = Firebase.auth
    val user = auth.currentUser

    // VM + LiveData -> State
    val vm: HomeViewModel = viewModel()
    val productos by vm.productos.observeAsState(emptyList())

    // Estados UI
    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Snackbars (usar coroutines, NO LaunchedEffect dentro de onClick)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Confirmación de borrado
    var showConfirmDelete by remember { mutableStateOf(false) }
    var productoAEliminar by remember { mutableStateOf<Producto?>(null) }

    // Cargar al iniciar
    LaunchedEffect(Unit) { vm.cargarProductos() }

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text("Inicio") },
                actions = {
                    IconButton(onClick = { /* TODO: ir a carrito */ }) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = "Carrito")
                    }
                    IconButton(onClick = {
                        auth.signOut()
                        onClickLogout()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar Sesión"
                        )
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("HOME SCREEN", style = MaterialTheme.typography.headlineSmall)
            Text(user?.email ?: "No hay usuario", style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(16.dp))

            // ------- Formulario -------
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = precio,
                onValueChange = { precio = it },
                label = { Text("Precio") },
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMsg != null) {
                Spacer(Modifier.height(6.dp))
                Text(text = errorMsg!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val valor = precio.toDoubleOrNull()
                    when {
                        nombre.isBlank() -> errorMsg = "El nombre es obligatorio."
                        valor == null || valor <= 0.0 -> errorMsg = "El precio debe ser un número > 0."
                        else -> {
                            errorMsg = null
                            vm.agregarProducto(nombre, descripcion, valor) { ok, err ->
                                if (ok) {
                                    nombre = ""; descripcion = ""; precio = ""
                                    // IMPORTANTE: usar coroutine para snackbar
                                    scope.launch { snackbarHostState.showSnackbar("Producto guardado") }
                                } else {
                                    errorMsg = err ?: "No se pudo guardar"
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Agregar producto") }

            Spacer(Modifier.height(16.dp))
            Text("Productos", style = MaterialTheme.typography.titleMedium)

            // ------- Lista -------
            LazyColumn(Modifier.fillMaxSize()) {
                items(items = productos) { p: Producto ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(p.nombre, style = MaterialTheme.typography.titleLarge)
                                if (p.descripcion.isNotBlank()) Text(p.descripcion)
                                Text("Precio: $${p.precio}")
                            }
                            IconButton(
                                onClick = {
                                    productoAEliminar = p
                                    showConfirmDelete = true
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                            }
                        }
                    }
                }
            }
        }
    }

    // ------- Diálogo de confirmación -------
    if (showConfirmDelete && productoAEliminar != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Eliminar producto") },
            text = { Text("¿Seguro que deseas eliminar \"${productoAEliminar?.nombre}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    val id = productoAEliminar?.id
                    showConfirmDelete = false
                    if (id != null) {
                        vm.eliminarProducto(id) { ok, err ->
                            productoAEliminar = null
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (ok) "Producto eliminado" else (err ?: "No se pudo eliminar")
                                )
                            }
                        }
                    } else {
                        productoAEliminar = null
                    }
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmDelete = false
                    productoAEliminar = null
                }) { Text("Cancelar") }
            }
        )
    }
}
