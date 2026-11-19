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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
    var linkText by remember { mutableStateOf(ShareUtils.GITHUB_RELEASES_LATEST) }
    var copied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepPlum,
        title = {
            Text(
                text = "Send APK Download Link",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Share or open the GitHub Releases APK link for 67kmobile/Deepa-sahu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = linkText == ShareUtils.GITHUB_RELEASES_LATEST,
                        onClick = {
                            linkText = ShareUtils.GITHUB_RELEASES_LATEST
                            copied = false
                        },
                        label = { Text("Latest Release") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonOrchid.copy(alpha = 0.35f),
                            selectedLabelColor = LavenderGlow
                        )
                    )
                    FilterChip(
                        selected = linkText == ShareUtils.GITHUB_RELEASES_ALL,
                        onClick = {
                            linkText = ShareUtils.GITHUB_RELEASES_ALL
                            copied = false
                        },
                        label = { Text("All Releases") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonOrchid.copy(alpha = 0.35f),
                            selectedLabelColor = LavenderGlow
                        )
                    )
                }

                OutlinedTextField(
                    value = linkText,
                    onValueChange = {
                        linkText = it
                        copied = false
                    },
                    label = { Text("APK / Release URL") },
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
                        Text(if (copied) "Copied" else "Copy")
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

                OutlinedButton(
                    onClick = { ShareUtils.openDownloadPage(context, linkText) },
                    border = BorderStroke(1.dp, ElectricAmethyst.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricAmethyst),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_releases_page_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Releases Page")
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
