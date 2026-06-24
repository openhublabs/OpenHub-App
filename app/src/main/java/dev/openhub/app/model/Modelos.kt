package dev.openhub.app.model

data class Evento(
    val id: String,
    val titulo: String,
    val descripcion: String = "",
    val categoria: String = "",
    val ubicacion: String = "",
    val fecha: String = "",
    val horaInicio: String = "",
    val horaFin: String = "",
    val organizador: String = "",
    val imagenUrl: String = "",
    val url: String = "",
    val source: String = "",
    val clips: Int = 0,
    val tiempoTexto: String = ""
)


