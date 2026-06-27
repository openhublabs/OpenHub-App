package dev.openhub.app.ui.compose

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dev.openhub.app.ui.theme.spatialClickable

private val ParticleBlue = Color(0xFF2563EB)
private val ParticleText = Color(0xFF212121)
private val ParticleSecondary = Color(0xFF757575)

// Categorías disponibles en la app
val CATEGORIAS_DISPONIBLES = listOf(
    "Música",
    "Deportes",
    "Cine",
    "Teatro",
    "Danza",
    "Gastronomía",
    "Tecnología",
    "Arte",
    "Conferencias",
    "Networking",
    "Educación",
    "Fiesta"
)

@Composable
fun PreferenciasScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val currentUser = auth.currentUser

    var categoriasSeleccionadas by remember { mutableStateOf(setOf<String>()) }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccessCard by remember { mutableStateOf(false) }

    // Cargar preferencias del usuario
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            db.collection("usuarios").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        @Suppress("UNCHECKED_CAST")
                        val categorias = document.get("categoriasPreferidas") as? List<String> ?: emptyList()
                        categoriasSeleccionadas = categorias.toSet()
                    }
                }
        }
    }

    fun toggleCategoria(categoria: String) {
        categoriasSeleccionadas = if (categoriasSeleccionadas.contains(categoria)) {
            categoriasSeleccionadas - categoria
        } else {
            categoriasSeleccionadas + categoria
        }
    }

    fun guardarPreferencias() {
        if (currentUser == null) return

        isLoading = true
        val actualizacion = mapOf(
            "categoriasPreferidas" to categoriasSeleccionadas.toList(),
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        db.collection("usuarios").document(currentUser.uid)
            .set(actualizacion, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                isLoading = false
                showSuccessCard = true
                Toast.makeText(context, "Preferencias guardadas", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Error al guardar: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // ─── Botón X (cerrar) ───────────────────────
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(end = 20.dp, top = 16.dp)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                            .background(Color(0xFFE5E5EA), CircleShape)
                            .spatialClickable { navController.navigateUp() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF3C3C43).copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // ─── Título ───────────────────────
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Text(
                        text = "Preferencias",
                        color = Color(0xFF000000),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Elige las categorías que más te interesan",
                        color = ParticleSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            // ─── Success Card ───────────────────────
            item {
                AnimatedVisibility(
                    visible = showSuccessCard,
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .fillMaxWidth()
                            .background(Color(0xF0E8F5E9), RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Preferencias guardadas",
                            color = Color(0xFF2E7D32),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .spatialClickable { showSuccessCard = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Descartar",
                                tint = Color(0xFF2E7D32).copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // ─── Categorías en Grid ───────────────────────
            item {
                Text(
                    text = "Selecciona tus categorías",
                    color = ParticleText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            items(CATEGORIAS_DISPONIBLES.chunked(2)) { rowCategorias ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowCategorias.forEach { categoria ->
                        val isSelected = categoriasSeleccionadas.contains(categoria)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .background(
                                    if (isSelected) ParticleBlue else Color.White,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) ParticleBlue else Color(0xFFE5E5EA),
                                    RoundedCornerShape(12.dp)
                                )
                                .alpha(if (isLoading) 0.5f else 1f)
                                .spatialClickable {
                                    if (!isLoading) toggleCategoria(categoria)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = categoria,
                                color = if (isSelected) Color.White else ParticleText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // ─── Información de ayuda ───────────────────────
            item {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                        .fillMaxWidth()
                        .background(Color(0xFFFEF3C7), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "💡 Selecciona las categorías que te interesan para recibir recomendaciones personalizadas en tu feed",
                        color = Color(0xFF92400E),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // ─── Contador de selecciones ───────────────────────
            item {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (categoriasSeleccionadas.isEmpty()) Color(0xFFE5E5EA) else ParticleBlue,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "${categoriasSeleccionadas.size} seleccionadas",
                            color = if (categoriasSeleccionadas.isEmpty()) ParticleSecondary else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        // ─── Botón Guardar (fixed bottom) ───────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    if (isLoading || categoriasSeleccionadas.isEmpty()) ParticleBlue.copy(alpha = 0.5f) else ParticleBlue,
                    RoundedCornerShape(32.dp)
                )
                .alpha(if (isLoading || categoriasSeleccionadas.isEmpty()) 0.6f else 1f)
                .spatialClickable {
                    if (!isLoading && categoriasSeleccionadas.isNotEmpty()) {
                        guardarPreferencias()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Guardar Preferencias",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}