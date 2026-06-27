package dev.openhub.app.ui.compose

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
private val ParticlePurple = Color(0xFF7C3AED)
private val ParticleGray = Color(0xFFF5F5F5)
private val ParticleText = Color(0xFF212121)
private val ParticleSecondary = Color(0xFF757575)

@Composable
fun EditPerfilScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val currentUser = auth.currentUser

    var nombreCompleto by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var ubicacion by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccessCard by remember { mutableStateOf(false) }

    // Cargar datos del usuario desde Firestore
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            db.collection("usuarios").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        nombreCompleto = document.getString("nombreCompleto") ?: ""
                        bio = document.getString("bio") ?: ""
                        ubicacion = document.getString("ubicacion") ?: ""
                    }
                }
        }
    }

    fun guardarCambios() {
        if (currentUser == null) return

        if (nombreCompleto.isBlank()) {
            Toast.makeText(context, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        val actualizacion = mapOf(
            "nombreCompleto" to nombreCompleto.trim(),
            "bio" to bio.trim(),
            "ubicacion" to ubicacion.trim(),
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        db.collection("usuarios").document(currentUser.uid)
            .set(actualizacion, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                isLoading = false
                showSuccessCard = true
                Toast.makeText(context, "Perfil actualizado", Toast.LENGTH_SHORT).show()
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
                        text = "Editar Perfil",
                        color = Color(0xFF000000),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mantén tu información actualizada",
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
                            "Cambios guardados exitosamente",
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

            // ─── Nombre Completo ───────────────────────
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Nombre completo",
                        color = ParticleText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = nombreCompleto,
                        onValueChange = { nombreCompleto = it },
                        placeholder = { Text("Ej: Juan Pérez", color = ParticleSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ParticleText,
                            unfocusedTextColor = ParticleText,
                            focusedBorderColor = ParticleBlue,
                            unfocusedBorderColor = Color(0xFFE5E5EA),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (isLoading) 0.6f else 1f),
                        singleLine = true
                    )
                }
            }

            // ─── Bio ───────────────────────
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Biografía",
                        color = ParticleText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { if (it.length <= 150) bio = it },
                        placeholder = { Text("Cuéntanos sobre ti (máx. 150 caracteres)", color = ParticleSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ParticleText,
                            unfocusedTextColor = ParticleText,
                            focusedBorderColor = ParticleBlue,
                            unfocusedBorderColor = Color(0xFFE5E5EA),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp)
                            .alpha(if (isLoading) 0.6f else 1f),
                        maxLines = 4
                    )
                    Text(
                        text = "${bio.length}/150",
                        color = ParticleSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp)
                    )
                }
            }

            // ─── Ubicación ───────────────────────
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Ubicación",
                        color = ParticleText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = ubicacion,
                        onValueChange = { ubicacion = it },
                        placeholder = { Text("Ej: Lima, Perú", color = ParticleSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ParticleText,
                            unfocusedTextColor = ParticleText,
                            focusedBorderColor = ParticleBlue,
                            unfocusedBorderColor = Color(0xFFE5E5EA),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (isLoading) 0.6f else 1f),
                        singleLine = true
                    )
                }
            }

            // ─── Información de ayuda ───────────────────────
            item {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .fillMaxWidth()
                        .background(Color(0xFFFEF3C7), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "💡 Tu bio y ubicación ayudan a otros a conocerte mejor en eventos",
                        color = Color(0xFF92400E),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
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
                    if (isLoading) ParticleBlue.copy(alpha = 0.5f) else ParticleBlue,
                    RoundedCornerShape(32.dp)
                )
                .alpha(if (isLoading) 0.7f else 1f)
                .spatialClickable {
                    if (!isLoading) guardarCambios()
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
                    text = "Guardar Cambios",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}