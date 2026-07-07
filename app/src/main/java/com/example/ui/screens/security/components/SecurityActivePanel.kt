package com.example.ui.screens.security.components

import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entities.AppSettings
import com.example.ui.theme.EmeraldPrimary

@Composable
fun SecurityActivePanel(
    currentSettings: AppSettings,
    onCopyRecoveryPhrase: () -> Unit,
    onDeactivateSecurity: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val isDark = androidx.compose.foundation.isSystemInDarkTheme()
            val shieldBg = if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
            val shieldBorder = if (isDark) Color(0xFF10B981) else Color(0xFF34D399)
            val shieldTint = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
            val activeText = if (isDark) Color(0xFF34D399) else Color(0xFF065F46)
            val copyBtnBg = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF)
            val copyBtnText = if (isDark) Color(0xFF60A5FA) else EmeraldPrimary
            val deactivateContent = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
            val deactivateBorder = if (isDark) Color(0xFF7F1D1D) else Color(0xFFFECACA)

            // Big glowing emerald shield
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(shieldBg, CircleShape)
                    .border(width = 2.dp, color = shieldBorder, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = shieldTint,
                    modifier = Modifier.size(44.dp)
                )
            }

            Text(
                text = stringResource(id = R.string.sec_toast_active_success),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = activeText
            )

            Text(
                text = stringResource(id = R.string.sec_card_desc_warning),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (!currentSettings.recoveryHint.isNullOrBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.sec_hint_pattern, currentSettings.recoveryHint.orEmpty()),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }

            // Copy recovery button
            Button(
                onClick = onCopyRecoveryPhrase,
                colors = ButtonDefaults.buttonColors(containerColor = copyBtnBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = copyBtnText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.sec_btn_copy),
                        color = copyBtnText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

            // DEACTIVATE SECURITY BUTTON
            OutlinedButton(
                onClick = onDeactivateSecurity,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = deactivateContent),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, deactivateBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.sec_deactivate_btn),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
