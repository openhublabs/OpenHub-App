package dev.openhub.app.util

object EventoUtils {

    fun capitalizarPalabras(texto: String): String {
        return texto.split(" ").joinToString(" ") { palabra ->
            palabra.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
