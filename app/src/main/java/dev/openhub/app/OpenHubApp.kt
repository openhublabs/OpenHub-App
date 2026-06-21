package dev.openhub.app

import android.app.Application
import dev.openhub.app.data.EventRepository

class OpenHubApp : Application() {

    lateinit var eventRepository: EventRepository
        private set

    override fun onCreate() {
        super.onCreate()
        eventRepository = EventRepository.getInstance(this)
    }
}
