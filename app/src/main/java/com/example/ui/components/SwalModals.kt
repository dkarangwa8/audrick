package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.SwrSyncStatus
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.AmberAlertSoft
import com.example.ui.theme.BralirwaCrimson
import com.example.ui.theme.CrimsonCritical
import com.example.ui.theme.CrimsonCriticalSoft
import com.example.ui.theme.EmeraldFresh
import com.example.ui.theme.EmeraldSoft

enum class SwalType {
    SUCCESS, WARNING, ERROR, INFO
}

data class SwalModalData(
    val type: SwalType,
    val title: String,
    val message: String,
    val confirmLabel: String = "OK",
    val cancelLabel: String? = null,
    val onConfirm: (() -> Unit)? = null
)

@Composable
fun DistributorSwalModal(
    modal: SwalModalData?,
    onDismiss: () -> Unit
) {
    if (modal == null) return
    val (icon, tint, containerBg) = when (modal.type) {
        SwalType.SUCCESS -> Triple(Icons.Filled.CheckCircle, EmeraldFresh, EmeraldSoft)
        SwalType.WARNING -> Triple(Icons.Filled.Warning, AmberAlert, AmberAlertSoft)
        SwalType.ERROR -> Triple(Icons.Filled.Error, CrimsonCritical, CrimsonCriticalSoft)
        SwalType.INFO -> Triple(Icons.Filled.Info, BralirwaCrimson, MaterialTheme.colorScheme.primaryContainer)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.88f, animationSpec = spring())
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .padding(16.dp)
                    .testTag("swal_modal_container"),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(containerBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = modal.type.name,
                            tint = tint,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = modal.title,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = modal.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (modal.cancelLabel != null) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("swal_cancel_btn"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(modal.cancelLabel)
                            }
                        }
                        Button(
                            onClick = {
                                modal.onConfirm?.invoke()
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("swal_confirm_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = tint)
                        ) {
                            Text(modal.confirmLabel, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SwrStatusPill(
    status: SwrSyncStatus,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "swr_pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "swr_alpha"
    )
    val dotColor = when {
        status.errorMessage != null -> CrimsonCritical
        status.isRevalidating -> AmberAlert.copy(alpha = alpha)
        else -> EmeraldFresh
    }
    val text = when {
        status.errorMessage != null -> "SWR • Offline Cache"
        status.isRevalidating -> "SWR • Revalidating"
        status.fromCache -> "SWR • Cached Live"
        else -> "SWR • Synced"
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ProductThumbnail(
    imageResKey: String,
    imageUri: String,
    contentDescription: String,
    size: Dp = 56.dp
) {
    val fallbackRes = when (imageResKey) {
        "soft_drinks" -> R.drawable.img_soft_drinks
        "dark_malt" -> R.drawable.img_dark_malt
        else -> R.drawable.img_beer_crate
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (imageUri.isNotBlank()) {
            AsyncImage(
                model = imageUri,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = painterResource(fallbackRes)
            )
        } else {
            Image(
                painter = painterResource(fallbackRes),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}
