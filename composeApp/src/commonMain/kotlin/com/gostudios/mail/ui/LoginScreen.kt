package com.gostudios.mail.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gostudios.mail.DemoBackend
import com.gostudios.mail.MailStore
import com.gostudios.mail.MailAccount
import com.gostudios.mail.MailResult
import com.gostudios.mail.backend
import com.gostudios.mail.createRealBackend
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState

@Composable
fun LoginScreen() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val busy by MailStore.busy.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(48.dp))
        Logo()
        Spacer(Modifier.height(16.dp))
        Text("GoConsoleMail", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Sign in to Gmail to read & send mail.", color = Color(0xFF94A3B8), fontSize = 14.sp)
        Spacer(Modifier.height(36.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Gmail address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors(),
        )
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("App password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors(),
        )

        Text(
            "Tip: use a Gmail App Password (Enable 2-Step Verification in Google Account security).",
            color = Color(0xFF7C8BA1), fontSize = 12.sp,
            modifier = Modifier.padding(vertical = 10.dp),
        )

        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                error = ""
                if (email.isBlank() || password.isBlank()) { error = "Enter your Gmail and app password."; return@Button }
                MailStore.setBusy(true)
                MailStore.setStatus("Connecting to Gmail…")
                scope.launch {
                    val acc = MailAccount(email = email.trim(), password = password)
                    val real = createRealBackend(acc)
                    val res = real?.connect()
                    if (real != null && res is MailResult.Ok) {
                        backend = real
                        MailStore.setFolders(res.value)
                        MailStore.setBusy(false)
                        MailStore.setConnected(true, false)
                        MailStore.setStatus("Signed in as $email")
                    } else {
                        MailStore.setBusy(false)
                        val msg = if (real == null) "Create a backend first." else (res as? MailResult.Err)?.message ?: "Unknown error"
                        error = msg
                        MailStore.setStatus(msg)
                    }
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("Sign in to Gmail", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = {
            backend = DemoBackend()
            MailStore.setBusy(true)
            scope.launch {
                val res = backend?.connect()
                MailStore.setBusy(false)
                if (res is MailResult.Ok) {
                    MailStore.setFolders(res.value)
                    MailStore.setConnected(true, true)
                    MailStore.setStatus("Browsing demo inbox")
                }
            }
        }) {
            Text("Try the demo inbox", color = MaterialTheme.colorScheme.primary)
        }

        Text(MailStore.status.collectAsState().value, color = Color(0xFF64748B), fontSize = 12.sp)
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun Logo() {
    Box(
        Modifier.size(72.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("M", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
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