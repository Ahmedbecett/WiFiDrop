package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.util.QrCodeGenerator

@Composable
fun QrCodeView(
    data: String,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    darkColor: Color = Color(0xFF0F172A),
    lightColor: Color = Color.White
) {
    val qrMatrix = remember(data) {
        try {
            QrCodeGenerator.encode(data)
        } catch (_: Exception) {
            null
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(lightColor)
            .padding(12.dp)
            .testTag("qr_code_view"),
        contentAlignment = Alignment.Center
    ) {
        if (qrMatrix != null) {
            Canvas(modifier = Modifier.size(size - 24.dp)) {
                val matrixSize = qrMatrix.size
                val moduleSize = this.size.width / matrixSize

                for (r in 0 until matrixSize) {
                    for (c in 0 until matrixSize) {
                        if (qrMatrix[r, c]) {
                            drawRoundRect(
                                color = darkColor,
                                topLeft = Offset(c * moduleSize, r * moduleSize),
                                size = Size(moduleSize, moduleSize),
                                cornerRadius = CornerRadius(moduleSize * 0.25f, moduleSize * 0.25f)
                            )
                        }
                    }
                }
            }
        }
    }
}
