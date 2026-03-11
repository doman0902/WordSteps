package com.doman.wordsteps.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy          = Color(0xFF0F1B2D)
private val NavyLight     = Color(0xFF1A2E4A)
private val NavyMid       = Color(0xFF142338)
private val Amber         = Color(0xFFFFC044)
private val Teal          = Color(0xFF00C9A7)
private val Rose          = Color(0xFFFF6B8A)
private val TextPrimary   = Color(0xFFF0F4FF)
private val TextSecondary = Color(0xFF8A9BB5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current
    var showResetDialog by remember { mutableStateOf(false) }

    // Reset confirmations after 2 seconds
    LaunchedEffect(state.isSaved, state.resetDone) {
        if (state.isSaved || state.resetDone) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearConfirmations()
        }
    }

    Scaffold(
        containerColor = Navy,
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            SectionCard {
                Text(
                    "ML Server IP",
                    color = TextSecondary, fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "The address of your Flask server on the local network.",
                    color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value         = state.serverIp,
                    onValueChange = { viewModel.onIpChanged(it) },
                    placeholder   = { Text("192.168.0.98:5000", color = TextSecondary) },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction    = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboard?.hide()
                        viewModel.saveIp()
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Amber,
                        unfocusedBorderColor = NavyLight,
                        focusedTextColor     = TextPrimary,
                        unfocusedTextColor   = TextPrimary,
                        cursorColor          = Amber
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick  = { keyboard?.hide(); viewModel.saveIp() },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (state.isSaved) Teal else Amber
                    )
                ) {
                    Text(
                        if (state.isSaved) "Saved ✓" else "Save",
                        color = Navy, fontWeight = FontWeight.Bold, fontSize = 15.sp
                    )
                }
            }

            SectionCard {
                Text(
                    "Progress",
                    color = TextSecondary, fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Permanently deletes all attempts, streaks, and pattern mastery data.",
                    color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick  = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (state.resetDone) Teal else Rose
                    )
                ) {
                    Text(
                        if (state.resetDone) "Reset complete ✓" else "Reset Progress",
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp
                    )
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor   = NavyMid,
            title = {
                Text("Reset Progress?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This will permanently delete all your attempts, streaks, and pattern mastery. This cannot be undone.",
                    color = TextSecondary, fontSize = 13.sp, lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    viewModel.resetProgress()
                }) {
                    Text("Reset", color = Rose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF142338))
            .border(1.dp, NavyLight, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        content()
    }
}