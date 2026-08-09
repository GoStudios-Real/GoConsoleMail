package com.gostudios.mail

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object MailStore {
    val email = MutableStateFlow("")
    val displayName = MutableStateFlow("")
    val password = MutableStateFlow("")
    val connected = MutableStateFlow(false)
    val busy = MutableStateFlow(false)
    val status = MutableStateFlow("Not connected to Gmail")
    val demoMode = MutableStateFlow(false)

    private val _folders = MutableStateFlow<List<MailFolder>>(emptyList())
    val folders: StateFlow<List<MailFolder>> = _folders.asStateFlow()

    private val _messages = MutableStateFlow<List<MailMessage>>(emptyList())
    val messages: StateFlow<List<MailMessage>> = _messages.asStateFlow()

    private val _current = MutableStateFlow<MailMessage?>(null)
    val current: StateFlow<MailMessage?> = _current.asStateFlow()

    private val _compose = MutableStateFlow<MailComposeState>(MailComposeState())
    val compose: StateFlow<MailComposeState> = _compose.asStateFlow()

    var composeOpen = MutableStateFlow(false)

    fun setFolders(list: List<MailFolder>) { _folders.value = list }
    fun setMessages(list: List<MailMessage>) { _messages.value = list }
    fun setCurrent(msg: MailMessage?) { _current.value = msg }
    fun setCompose(c: MailComposeState) {
        _compose.value = c
    }
    fun openCompose() { composeOpen.value = true }
    fun closeCompose() { composeOpen.value = false }
    fun setStatus(text: String) { status.value = text }
    fun setBusy(b: Boolean) { busy.value = b }
    fun setConnected(c: Boolean, demo: Boolean) {
        connected.value = c
        currentMode = demo
    }
}

var backend: MailBackend? = null
var currentMode = false

data class MailComposeState(
    val to: String = "",
    val subject: String = "",
    val body: String = "",
)