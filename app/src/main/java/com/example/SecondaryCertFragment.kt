package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecondaryCertFragment(
    grades: MutableMap<String, String>,
    onGeneratePdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    val subjects = listOf(
        Pair("quran", R.string.sub_quran),
        Pair("islamic", R.string.sub_islamic),
        Pair("arabic", R.string.sub_arabic),
        Pair("english", R.string.sub_english),
        Pair("math", R.string.sub_math),
        Pair("physics", R.string.sub_physics),
        Pair("chemistry", R.string.sub_chemistry),
        Pair("biology", R.string.sub_biology)
    )

    // Ensure state keys exist
    LaunchedEffect(Unit) {
        subjects.forEach { pair ->
            if (!grades.containsKey(pair.first)) {
                grades[pair.first] = ""
            }
        }
    }

    // Calculations
    val totalSum = remember(grades.values.toList()) {
        grades.values.mapNotNull { it.toIntOrNull() }.sum()
    }

    val isPassedAll = remember(grades.values.toList()) {
        val validGrades = grades.values.mapNotNull { it.toIntOrNull() }
        if (validGrades.size == subjects.size) {
            validGrades.all { it >= 50 }
        } else false
    }

    val average = remember(grades.values.toList()) {
        val validGrades = grades.values.mapNotNull { it.toIntOrNull() }
        if (validGrades.size == subjects.size) {
            validGrades.sum() / subjects.size.toDouble()
        } else 0.0
    }

    Column(
        modifier = modifier
            .background(Color.White)
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Section header
        Text(
            text = "درجات التعليم الثانوي - القسم العلمي (8 مواد)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
        )

        // 2-Column Grid Layout for subjects
        val rows = subjects.chunked(2)
        rows.forEach { rowPairs ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                rowPairs.forEach { pair ->
                    val id = pair.first
                    val nameRes = pair.second
                    var textVal by remember(grades[id]) { mutableStateOf(grades[id] ?: "") }

                    // Customized Ultra-Compact Text Field
                    OutlinedTextField(
                        value = textVal,
                        onValueChange = { input ->
                            if (input.isEmpty() || (input.all { it.isDigit() } && input.toIntOrNull() in 0..100)) {
                                textVal = input
                                grades[id] = input
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(nameRes),
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                        },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.grade_placeholder),
                                fontSize = 9.sp
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.DarkGray,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedLabelColor = Color.Black,
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("input_$id"),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Next) }
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
                }

                // If row has only 1 item, pad it with an empty box
                if (rowPairs.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // Extremely compact summary box (pale yellow)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .background(Color(0xFFFFFBEB), shape = RoundedCornerShape(2.dp))
                .border(0.5.dp, Color(0xFFFCD34D), shape = RoundedCornerShape(2.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المجموع: ${formatToArabicIndicDigits(totalSum.toString())} / 800",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF78350F)
                )

                Text(
                    text = "المعدل: ${formatToArabicIndicDigits(String.format("%.2f", average))}%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF78350F)
                )

                val hasAllGrades = grades.values.filter { it.isNotEmpty() }.size == subjects.size
                val statusText = if (!hasAllGrades) "معلق" else if (isPassedAll) "ناجح" else "راسب"
                val statusColor = if (!hasAllGrades) Color.Gray else if (isPassedAll) Color(0xFF047857) else Color(0xFFB91C1C)

                Text(
                    text = "الحالة: $statusText",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }

        // Thin practical action button
        Button(
            onClick = onGeneratePdf,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E3A8A),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .testTag("btn_generate_secondary"),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = "توليد كشف درجات التعليم الثانوي",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
