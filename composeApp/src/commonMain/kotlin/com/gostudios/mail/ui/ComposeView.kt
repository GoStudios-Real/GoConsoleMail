package com.gostudios.mail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gostudios.mail.MailComposeState
import com.gostudios.mail.MailResult
import com.gostudios.mail.MailStore
import com.gostudios.mail.backend
import kotlinx.coroutines.launch

@Composable
fun ComposeView(onBack: () -> Unit) {
    val state = MailStore.compose.collectAsState().value
    var to by remember { mutableStateOf(state.to) }
    var subject by remember { mutableStateOf(state.subject) }
    var body by remember { mutableStateOf(state.body) }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val demo = MailStore.demoMode.collectAsState().value

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("New message", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    if (to.isBlank()) { error = "Enter a recipient."; return@Button }
                    error = ""
                    sending = true
                    scope.launch {
                        val res = backend?.send(to.split(',').map { it.trim() }.filter { it.isNotEmpty() }, subject, body)
                        sending = false
                        if (res is MailResult.Ok) {
                            onBack()
                        } else {
                            error = (res as? MailResult.Err)?.message ?: "Send failed"
                        }
                    }
                },
                enabled = !sending,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp),
            ) {
                if (sending) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                else { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(if (demo) "Queue" else "Send") }
            }
        }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (demo) {
                Text("Demo mode registers this message but does not connect to Gmail. Connect a real account to send.",
                    color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
            }
            OutlinedTextField(
                value = to, onValueChange = { to = it },
                label = { Text("To (comma separated)") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = subject, onValueChange = { subject = it },
                label = { Text("Subject") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = body, onValueChange = { body = it },
                label = { Text("Message") },
                minLines = 8, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors(),
            )
            if (error.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = Color(0xFF334155),
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = Color(0xFF94A3B8),
    cursorColor = MaterialTheme.colorScheme.primary,
)