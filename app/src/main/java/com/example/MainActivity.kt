package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    CertificateApp(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertificateApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Switching state (true: Basic Certificate, false: Secondary Certificate)
    var isBasicSelected by remember { mutableStateOf(true) }
    var isScientificSelected by remember { mutableStateOf(true) }
    var switchMenuExpanded by remember { mutableStateOf(false) }

    // Personal info states
    var studentName by remember { mutableStateOf("") }
    var seatNumber by remember { mutableStateOf("") }
    var selectedGov by remember { mutableStateOf("") }
    var govMenuExpanded by remember { mutableStateOf(false) }

    // Distinct grade map states for independent inputs
    val basicGrades = remember { mutableStateMapOf<String, String>() }
    val secondaryGrades = remember { mutableStateMapOf<String, String>() }

    // Generated file reference
    var generatedFile by remember { mutableStateOf<File?>(null) }

    // Governorates list
    val governoratesList = remember {
        listOf(
            "أمانة العاصمة", "صنعاء", "عدن", "تعز", "حضرموت", "الحديدة",
            "إب", "مأرب", "ذمار", "عمران", "حجة", "صعدة", "أبين",
            "شبوة", "البيضاء", "لحج", "الضالع", "الجوف", "المهرة",
            "ريمة", "المحويت", "سقطرى"
        )
    }

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
        LazyColumn(
            modifier = modifier
                .background(Color.White)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 2. Intelligent Thin Header & Switcher
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9))
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Row for Basic vs Secondary selection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isBasicSelected = true
                                generatedFile = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBasicSelected) Color(0xFF1E3A8A) else Color(0xFFE2E8F0),
                                contentColor = if (isBasicSelected) Color.White else Color(0xFF475569)
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("select_basic_cert"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("الشهادة الأساسية", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                isBasicSelected = false
                                generatedFile = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isBasicSelected) Color(0xFF1E3A8A) else Color(0xFFE2E8F0),
                                contentColor = if (!isBasicSelected) Color.White else Color(0xFF475569)
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("select_secondary_cert"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("الشهادة الثانوية", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Secondary Subsections: Scientific vs Literary
                    AnimatedVisibility(
                        visible = !isBasicSelected,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "اختر تخصص وقسم الشهادة الثانوية:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        isScientificSelected = true
                                        generatedFile = null
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isScientificSelected) Color(0xFFD97706) else Color(0xFFE2E8F0),
                                        contentColor = if (isScientificSelected) Color.White else Color(0xFF475569)
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .testTag("select_scientific"),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("قسم علمي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        isScientificSelected = false
                                        generatedFile = null
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (!isScientificSelected) Color(0xFFD97706) else Color(0xFFE2E8F0),
                                        contentColor = if (!isScientificSelected) Color.White else Color(0xFF475569)
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .testTag("select_literary"),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("قسم أدبي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Compact Student Information Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "بيانات الطالب والجلوس:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        // Student Name Field
                        OutlinedTextField(
                            value = studentName,
                            onValueChange = { studentName = it },
                            label = { Text("الاسم الكامل للطالب رباعياً", fontSize = 9.sp, color = Color.Gray) },
                            placeholder = { Text("أدخل اسم الطالب", fontSize = 9.sp) },
                            singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.DarkGray,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("student_name_field"),
                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Next) }
                            ),
                            shape = RoundedCornerShape(2.dp)
                        )

                        // Row for Seat Number and Governorate Dropdown (Side-by-side)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Seat Number
                            OutlinedTextField(
                                value = seatNumber,
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() }) {
                                        seatNumber = input
                                    }
                                },
                                label = { Text("رقم الجلوس", fontSize = 9.sp, color = Color.Gray) },
                                placeholder = { Text("رقم الجلوس", fontSize = 9.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.DarkGray,
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("seat_number_field"),
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, textAlign = TextAlign.Center),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                ),
                                shape = RoundedCornerShape(2.dp)
                            )

                            // Governorate selection
                            Box(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(44.dp)
                            ) {
                                OutlinedTextField(
                                    value = selectedGov,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("المحافظة", fontSize = 9.sp, color = Color.Gray) },
                                    placeholder = { Text("اختر المحافظة", fontSize = 9.sp) },
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = { govMenuExpanded = true }) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Dropdown list",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.DarkGray,
                                        unfocusedBorderColor = Color(0xFFE2E8F0),
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black
                                    ),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("governorate_field"),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                    shape = RoundedCornerShape(2.dp)
                                )

                                DropdownMenu(
                                    expanded = govMenuExpanded,
                                    onDismissRequest = { govMenuExpanded = false }
                                ) {
                                    governoratesList.forEach { govName ->
                                        DropdownMenuItem(
                                            text = { Text(govName, fontSize = 12.sp) },
                                            onClick = {
                                                selectedGov = govName
                                                govMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Host the active certificate type fragment
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    if (isBasicSelected) {
                        BasicCertFragment(
                            grades = basicGrades,
                            onGeneratePdf = {
                                val missing = basicGrades.size < 7 || basicGrades.values.any { it.isEmpty() }
                                if (studentName.trim().isEmpty() || seatNumber.trim().isEmpty() || selectedGov.isEmpty() || missing) {
                                    Toast.makeText(context, context.getString(R.string.error_empty_fields), Toast.LENGTH_LONG).show()
                                    return@BasicCertFragment
                                }

                                val intGrades = basicGrades.mapValues { it.value.toIntOrNull() ?: 0 }
                                val pdfFile = generateBasicCertificatePdf(
                                    context = context,
                                    name = studentName.trim(),
                                    seatNo = seatNumber.trim(),
                                    gov = selectedGov,
                                    subjectGrades = intGrades
                                )

                                if (pdfFile != null) {
                                    generatedFile = pdfFile
                                    Toast.makeText(context, context.getString(R.string.success_pdf_generated), Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "فشل إنشاء مستند الـ PDF!", Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    } else {
                        SecondaryCertFragment(
                            isScientific = isScientificSelected,
                            grades = secondaryGrades,
                            onGeneratePdf = {
                                val activeKeys = if (isScientificSelected) {
                                    listOf("quran", "islamic", "arabic", "english", "math", "physics", "chemistry", "biology")
                                } else {
                                    listOf("quran", "islamic", "arabic", "english", "math_literary", "history", "geography", "psychology")
                                }
                                val missing = activeKeys.any { secondaryGrades[it]?.isEmpty() != false }
                                if (studentName.trim().isEmpty() || seatNumber.trim().isEmpty() || selectedGov.isEmpty() || missing) {
                                    Toast.makeText(context, context.getString(R.string.error_empty_fields), Toast.LENGTH_LONG).show()
                                    return@SecondaryCertFragment
                                }

                                val intGrades = activeKeys.associateWith { secondaryGrades[it]?.toIntOrNull() ?: 0 }
                                val pdfFile = generateSecondaryCertificatePdf(
                                    context = context,
                                    name = studentName.trim(),
                                    seatNo = seatNumber.trim(),
                                    gov = selectedGov,
                                    isScientific = isScientificSelected,
                                    subjectGrades = intGrades
                                )

                                if (pdfFile != null) {
                                    generatedFile = pdfFile
                                    Toast.makeText(context, context.getString(R.string.success_pdf_generated), Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "فشل إنشاء مستند الـ PDF!", Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    }
                }
            }

            // 5. Shared Interactive Output Control panel
            item {
                AnimatedVisibility(
                    visible = generatedFile != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val file = generatedFile
                    if (file != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(0.5.dp, Color(0xFFCBD5E1)),
                            shape = RoundedCornerShape(2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                                Column(
                                    modifier = Modifier.padding(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                Text(
                                    text = "تم إصدار مستند كشف الدرجات الرقمي بنجاح!",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E3A8A),
                                    textAlign = TextAlign.Center
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Print / View pdf button
                                    Button(
                                        onClick = { printPDF(context, file) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                                        shape = RoundedCornerShape(2.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                            .testTag("btn_print"),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("طباعة الكشف", fontSize = 11.sp)
                                    }

                                    // Share whatsapp button
                                    Button(
                                        onClick = { sharePdfViaWhatsApp(context, file) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(2.dp),
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .height(34.dp)
                                            .testTag("btn_share"),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share icon",
                                            modifier = Modifier.size(14.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("مشاركة الكترونياً", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
