package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.ShareUtils
import com.example.ui.theme.BorderViolet
import com.example.ui.theme.DeepPlum
import com.example.ui.theme.ElectricAmethyst
import com.example.ui.theme.EmeraldSecure
import com.example.ui.theme.LavenderGlow
import com.example.ui.theme.NeonOrchid
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ShareDownloadDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var linkText by remember { mutableStateOf(ShareUtils.DEFAULT_DOWNLOAD_LINK) }
    var copied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepPlum,
        title = {
            Text(
                text = "Send App Download Link",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Share the VioletTunnel app link or paste your hosted APK download URL before sending.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = linkText,
                    onValueChange = {
                        linkText = it
                        copied = false
                    },
                    label = { Text("Download / Share URL") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.labelSmall,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricAmethyst,
                        unfocusedBorderColor = BorderViolet,
                        focusedTextColor = LavenderGlow,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("download_link_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            ShareUtils.copyDownloadLink(context, linkText)
                            copied = true
                        },
                        border = BorderStroke(1.dp, if (copied) EmeraldSecure else BorderViolet),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (copied) EmeraldSecure else LavenderGlow
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_download_link_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (copied) "Copied" else "Copy Link")
                    }

                    Button(
                        onClick = {
                            ShareUtils.shareApp(context, linkText)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonOrchid),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("send_download_link_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}
