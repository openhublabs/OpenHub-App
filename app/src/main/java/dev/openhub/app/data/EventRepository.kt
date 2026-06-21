package dev.openhub.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dev.openhub.app.data.local.OpenHubDBHelper
import dev.openhub.app.data.remote.FirebaseEventosRepository
import dev.openhub.app.model.Evento

class EventRepository private constructor(context: Context) {

    private val dbHelper = OpenHubDBHelper(context.applicationContext)
    private val firebaseRepo = FirebaseEventosRepository()

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    companion object {
        @Volatile
        private var INSTANCE: EventRepository? = null

        fun getInstance(context: Context): EventRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EventRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    suspend fun obtenerEventos(): List<Evento> {
        return if (isOnline()) {
            try {
                val eventos = firebaseRepo.obtenerEventos()
                if (eventos.isNotEmpty()) {
                    dbHelper.eliminarTodo()
                    dbHelper.insertarEventos(eventos)
                }
                eventos
            } catch (e: Exception) {
                dbHelper.obtenerTodos()
            }
        } else {
            dbHelper.obtenerTodos()
        }
    }

    fun obtenerDeSQLite(): List<Evento> {
        return dbHelper.obtenerTodos()
    }

    fun buscarEnSQLite(query: String): List<Evento> {
        return dbHelper.buscar(query)
    }

    private fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
