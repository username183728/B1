package com.example.aidetest

/** Safe runtime metadata for Bit. Never stores tokens, passwords or credentials. */
object BitRuntimeContext {
    var activeToolId: String? = null
        private set
    var activeToolName: String? = null
        private set
    var lastCommand: String? = null
        private set
    var lastToolId: String? = null
        private set
    var lastToolName: String? = null
        private set
    var lastStatus: String? = null
        private set

    fun onCommand(command: String) { lastCommand = command.trim().take(500) }

    fun onToolOpened(id: String, name: String) {
        activeToolId = id
        activeToolName = name
        lastToolId = id
        lastToolName = name
        lastStatus = "opened"
    }
}
