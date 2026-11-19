package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.VpnConnectionState
import com.example.ui.theme.AmberReconnect
import com.example.ui.theme.BorderViolet
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyanDownload
import com.example.ui.theme.DeepIris
import com.example.ui.theme.DeepPlum
import com.example.ui.theme.ElectricAmethyst
import com.example.ui.theme.ElevatedPlum
import com.example.ui.theme.EmeraldSecure
import com.example.ui.theme.LavenderGlow
import com.example.ui.theme.MagentaUpload
import com.example.ui.theme.MidnightViolet
import com.example.ui.theme.NeonOrchid
import com.example.ui.theme.RoyalIndigo
import com.example.ui.theme.SurfaceViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidObsidian
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Purple3DBackdrop(
    isConnected: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val glowAlpha by animateFloatAsState(
        targetValue = if (isConnected) 0.48f else 0.28f,
        animationSpec = tween(700),
        label = "backdropGlow"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoidObsidian)
    ) {
        // Subtle generated 3D violet wave artwork
        Image(
            painter = painterResource(id = R.drawable.img_purple_tunnel_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = if (isConnected) 0.28f else 0.16f,
            modifier = Modifier.fillMaxSize()
        )

        // Volumetric 3D radial aura & subtle perspective horizon mesh
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        VoidObsidian.copy(alpha = 0.72f),
                        DeepPlum.copy(alpha = 0.52f),
                        VoidObsidian.copy(alpha = 0.94f)
                    )
                )
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonOrchid.copy(alpha = glowAlpha),
                        DeepIris.copy(alpha = glowAlpha * 0.45f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.26f),
                    radius = w * 0.82f
                ),
                center = Offset(w * 0.5f, h * 0.26f),
                radius = w * 0.82f
            )

            // 3D perspective floor grid lines near lower portion
            val horizonY = h * 0.64f
            val vanishX = w * 0.5f
            val gridColor = ElectricAmethyst.copy(alpha = 0.08f)
            for (i in -6..6) {
                val bottomX = vanishX + i * (w * 0.22f)
                drawLine(
                    color = gridColor,
                    start = Offset(vanishX + i * 18f, horizonY),
                    end = Offset(bottomX, h),
                    strokeWidth = 1.2f
                )
            }
            for (row in 1..6) {
                val t = (row / 6f)
                val y = horizonY + (h - horizonY) * (t * t)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
            }
        }

        content()
    }
}

@Composable
fun Glass3DCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    isHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val borderBrush = Brush.verticalGradient(
        colors = if (isHighlighted) {
            listOf(
                ElectricAmethyst.copy(alpha = 0.78f),
                NeonOrchid.copy(alpha = 0.30f),
                BorderViolet.copy(alpha = 0.20f)
            )
        } else {
            listOf(
                LavenderGlow.copy(alpha = 0.22f),
                BorderViolet.copy(alpha = 0.35f),
                Color.Transparent
            )
        }
    )

    Surface(
        modifier = modifier
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = ElectricAmethyst),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        shape = shape,
        color = if (isHighlighted) ElevatedPlum.copy(alpha = 0.86f) else SurfaceViolet.copy(alpha = 0.76f),
        border = BorderStroke(1.dp, borderBrush),
        shadowElevation = if (isHighlighted) 14.dp else 6.dp
    ) {
        Box(
            modifier = Modifier.background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent,
                        MidnightViolet.copy(alpha = 0.45f)
                    )
                )
            ),
            content = content
        )
    }
}

@Composable
fun Tunnel3DOrbButton(
    connectionState: VpnConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionState is VpnConnectionState.Connected
    val isBusy = connectionState is VpnConnectionState.Connecting ||
        connectionState is VpnConnectionState.Reconnecting

    val infiniteTransition = rememberInfiniteTransition(label = "orb3D")
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isBusy) 2400 else 9000, easing = LinearEasing)
        ),
        label = "orbitAngle"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isBusy) 900 else 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val coreColor by animateColorAsState(
        targetValue = when (connectionState) {
            is VpnConnectionState.Connected -> EmeraldSecure
            is VpnConnectionState.Connecting -> ElectricAmethyst
            is VpnConnectionState.Reconnecting -> AmberReconnect
            is VpnConnectionState.Error -> CrimsonAlert
            VpnConnectionState.Disconnected -> NeonOrchid
        },
        animationSpec = tween(450),
        label = "coreColor"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isConnected) 1.03f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "btnScale"
    )

    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseR = size.minDimension * 0.34f

            // Outer volumetric ambient bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreColor.copy(alpha = if (isConnected) 0.36f else 0.22f),
                        NeonOrchid.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = size.minDimension * 0.50f * pulse
                ),
                center = Offset(cx, cy),
                radius = size.minDimension * 0.50f * pulse
            )

            // 3D tilted orbital ring 1
            rotate(degrees = 26f + orbitAngle * 0.35f, pivot = Offset(cx, cy)) {
                drawOval(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            ElectricAmethyst.copy(alpha = 0.7f),
                            coreColor.copy(alpha = 0.9f),
                            Color.Transparent
                        )
                    ),
                    topLeft = Offset(cx - baseR * 1.42f, cy - baseR * 0.72f),
                    size = androidx.compose.ui.geometry.Size(baseR * 2.84f, baseR * 1.44f),
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )
            }

            // 3D tilted orbital ring 2 (counter-rotating)
            rotate(degrees = -34f - orbitAngle * 0.5f, pivot = Offset(cx, cy)) {
                drawOval(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            CyanDownload.copy(alpha = if (isConnected) 0.75f else 0.35f),
                            LavenderGlow.copy(alpha = 0.65f),
                            Color.Transparent
                        )
                    ),
                    topLeft = Offset(cx - baseR * 1.36f, cy - baseR * 0.64f),
                    size = androidx.compose.ui.geometry.Size(baseR * 2.72f, baseR * 1.28f),
                    style = Stroke(width = 2.6f, cap = StrokeCap.Round)
                )
            }

            // Orbiting energy node particles on ring 1
            val rad = (orbitAngle * PI / 180.0)
            val nodeX = cx + (baseR * 1.18f * cos(rad)).toFloat()
            val nodeY = cy + (baseR * 0.62f * sin(rad)).toFloat()
            drawCircle(
                color = LavenderGlow,
                radius = 5.5f,
                center = Offset(nodeX, nodeY)
            )
            drawCircle(
                color = coreColor.copy(alpha = 0.55f),
                radius = 12f,
                center = Offset(nodeX, nodeY)
            )

            // 3D Sphere Shadow Drop
            drawCircle(
                color = Color.Black.copy(alpha = 0.55f),
                radius = baseR * 1.06f,
                center = Offset(cx, cy + 12f)
            )

            // 3D Glossy Sphere Body with Top-Left Specular Light
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        LavenderGlow.copy(alpha = 0.92f),
                        ElectricAmethyst,
                        DeepIris,
                        MidnightViolet
                    ),
                    center = Offset(cx - baseR * 0.32f, cy - baseR * 0.34f),
                    radius = baseR * 1.45f
                ),
                radius = baseR,
                center = Offset(cx, cy)
            )

            // Inner glowing bezel ring
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.65f),
                        coreColor.copy(alpha = 0.85f),
                        RoyalIndigo.copy(alpha = 0.45f)
                    )
                ),
                radius = baseR - 3f,
                center = Offset(cx, cy),
                style = Stroke(width = 4f)
            )
        }

        // Tactile Central Power / Shield Trigger
        Surface(
            modifier = Modifier
                .size(116.dp)
                .scale(buttonScale)
                .clip(CircleShape)
                .testTag("vpn_connect_button")
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = Color.White, bounded = true),
                    onClick = onClick
                ),
            shape = CircleShape,
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Icon(
                    imageVector = when (connectionState) {
                        is VpnConnectionState.Connected -> Icons.Rounded.Shield
                        is VpnConnectionState.Reconnecting -> Icons.Rounded.Refresh
                        else -> Icons.Rounded.PowerSettingsNew
                    },
                    contentDescription = if (isConnected) "Disconnect VPN" else "Connect VPN",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (connectionState) {
                        is VpnConnectionState.Connected -> "ACTIVE"
                        is VpnConnectionState.Connecting -> "TUNING"
                        is VpnConnectionState.Reconnecting -> "RETRY"
                        is VpnConnectionState.Error -> "RETRY"
                        VpnConnectionState.Disconnected -> "CONNECT"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.95f)
                )
            }
        }
    }
}

@Composable
fun ThroughputWaveformCanvas(
    downSamples: List<Float>,
    upSamples: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        val w = size.width
        val h = size.height
        val count = downSamples.size.coerceAtLeast(2)
        val maxVal = maxOf(
            downSamples.maxOrNull() ?: 1f,
            upSamples.maxOrNull() ?: 1f,
            1024f
        )

        fun drawSeries(samples: List<Float>, strokeColor: Color, fillAlpha: Float) {
            val linePath = Path()
            val fillPath = Path()
            val stepX = w / (count - 1).toFloat()

            samples.forEachIndexed { idx, value ->
                val x = idx * stepX
                val normalized = (value / maxVal).coerceIn(0f, 1f)
                val y = h - (normalized * (h - 10f)) - 4f
                if (idx == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, h)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo(w, h)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(strokeColor.copy(alpha = fillAlpha), Color.Transparent)
                )
            )
            drawPath(
                path = linePath,
                color = strokeColor,
                style = Stroke(width = 2.6f, cap = StrokeCap.Round)
            )
        }

        // Subtle baseline
        drawLine(
            color = BorderViolet.copy(alpha = 0.35f),
            start = Offset(0f, h - 2f),
            end = Offset(w, h - 2f),
            strokeWidth = 1f
        )

        drawSeries(downSamples, CyanDownload, 0.24f)
        drawSeries(upSamples, MagentaUpload, 0.16f)
    }
}

@Composable
fun PingBadge(
    pingMs: Int?,
    isPinging: Boolean,
    modifier: Modifier = Modifier
) {
    val (color, label) = when {
        isPinging -> ElectricAmethyst to "..."
        pingMs == null -> TextMuted to "-- ms"
        pingMs <= 70 -> EmeraldSecure to "$pingMs ms"
        pingMs <= 160 -> AmberReconnect to "$pingMs ms"
        else -> CrimsonAlert to "$pingMs ms"
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.42f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SecurityStatusChip(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accent = if (active) EmeraldSecure else TextSecondary
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (active) EmeraldSecure.copy(alpha = 0.12f) else SurfaceViolet.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, accent.copy(alpha = if (active) 0.45f else 0.28f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (active) LavenderGlow else TextSecondary,
                maxLines = 1
            )
        }
    }
}
