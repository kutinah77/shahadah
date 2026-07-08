package com.example.ui.screens.habayeb.components

import androidx.compose.material3.MaterialTheme

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entities.HabayebCustomer
import com.example.domain.StringUtils.getContactDetails
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.screens.CalculatorDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@Composable
fun AddCustomerPopup(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit,
    onCustomerAdded: () -> Unit = {},
    activeThemeColor: Color,
    activeSubColor: Color
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var nameStr by rememberSaveable { mutableStateOf("") }
    var phoneStr by rememberSaveable { mutableStateOf("") }
    var notesStr by rememberSaveable { mutableStateOf("") }
    
    // First atomic transaction fields
    var initialAmountStr by rememberSaveable { mutableStateOf("") }
    var initialType by rememberSaveable { mutableStateOf("OWED_BY_THEM") } // OWED_BY_THEM (عليه لي) or OWED_TO_THEM (له عندي)
    val currencySymbol = viewModel.settingsState.collectAsStateWithLifecycle().value.currencySymbol
    var selectedTransactionCurrency by rememberSaveable { mutableStateOf(currencySymbol) }
    var applyExchangeRate by rememberSaveable { mutableStateOf(selectedTransactionCurrency != currencySymbol) }
    var showRateSetupOverlay by rememberSaveable { mutableStateOf(false) }
    var tempRateStr by rememberSaveable { mutableStateOf("") }
    
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()

    val currencySar = context.getString(R.string.currency_sar)
    val currencyUsd = context.getString(R.string.currency_usd)
    val currencyYer = context.getString(R.string.currency_yer)
    
    val settingsRate = run {
        val currentRateVal = com.example.ui.screens.habayeb.utils.ExchangeRateHelper.getRate(settings.exchangeRatesJson, currencySymbol, selectedTransactionCurrency)
        if (currentRateVal > 0.0) {
            currentRateVal
        } else {
            when (selectedTransactionCurrency) {
                currencySar -> 160.0
                currencyUsd -> 600.0
                else -> 1.0
            }
        }
    }

    var showCalculator by rememberSaveable { mutableStateOf(false) }
    var isSavingCustomer by rememberSaveable { mutableStateOf(false) }

    // Date Picker state
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    val dateStr = remember(selectedCalendar) {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)
        sdf.format(selectedCalendar.time)
    }

    // Auto-focus & keyboard navigation setup
    val focusRequester = remember { FocusRequester() }
    val phoneFocusRequester = remember { FocusRequester() }
    val initialAmountFocusRequester = remember { FocusRequester() }
    val notesFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val softwareKeyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        try {
            focusRequester.requestFocus()
            softwareKeyboardController?.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Contact picker launcher
    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { contactUri ->
        contactUri?.let { uri ->
            val details = getContactDetails(context, uri)
            if (details != null) {
                nameStr = details.first
                phoneStr = details.second
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            contactPickerLauncher.launch(null)
        } else {
            Toast.makeText(context, context.getString(R.string.habayeb_toast_storage_permission), Toast.LENGTH_SHORT).show()
        }
    }

    // Interactive button spring scaling
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "ButtonScale"
    )

    Dialog(
        onDismissRequest = onDismiss
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .widthIn(max = 350.dp)
                    .fillMaxWidth(0.94f)
                    .imePadding()
                    .padding(2.dp)
            ) {
                Crossfade(targetState = showRateSetupOverlay, label = "CustomerFormTransition") { isSetup ->
                    if (isSetup) {
                        BackHandler {
                            showRateSetupOverlay = false
                            applyExchangeRate = false
                        }
                        ExchangeRateSetupContent(
                            currencySymbol = currencySymbol,
                            selectedCurrency = selectedTransactionCurrency,
                            initialRateStr = tempRateStr,
                            activeThemeColor = activeThemeColor,
                            onDismiss = {
                                showRateSetupOverlay = false
                                applyExchangeRate = false
                            },
                            onConfirm = { newRate, _ ->
                                val newSettings = settings.copy(
                                    exchangeRatesJson = com.example.ui.screens.habayeb.utils.ExchangeRateHelper.setRate(settings.exchangeRatesJson, currencySymbol, selectedTransactionCurrency, newRate)
                                )
                                viewModel.saveSettings(newSettings)
                                applyExchangeRate = true
                                showRateSetupOverlay = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .navigationBarsPadding()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Header Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 0.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(id = R.string.habayeb_cancel),
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = stringResource(id = R.string.dialog_title_add_account),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = activeThemeColor,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.size(24.dp))
                            }

                            // 1. الاسم (Account Name Input)
                            OutlinedTextField(
                                value = nameStr,
                                onValueChange = { nameStr = it },
                                label = { Text(stringResource(id = R.string.hint_account_name), fontSize = 10.sp) },
                                placeholder = { Text(stringResource(id = R.string.habayeb_edit_name_desc), fontSize = 10.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { initialAmountFocusRequester.requestFocus() }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = activeThemeColor,
                                    focusedLabelColor = activeThemeColor,
                                    cursorColor = activeThemeColor,
                                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                                )
                            )

                            // 2. المبلغ (Initial Amount Input)
                            OutlinedTextField(
                                value = initialAmountStr,
                                onValueChange = { initialAmountStr = it },
                                label = { Text(stringResource(id = R.string.hint_opening_balance), fontSize = 10.sp) },
                                placeholder = { Text("0", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { notesFocusRequester.requestFocus() }),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(initialAmountFocusRequester),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = activeThemeColor,
                                    focusedLabelColor = activeThemeColor,
                                    cursorColor = activeThemeColor,
                                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                                ),
                                leadingIcon = {
                                    IconButton(onClick = { showCalculator = true }, modifier = Modifier.size(24.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Calculate,
                                            contentDescription = stringResource(id = R.string.habayeb_calculator),
                                            tint = activeThemeColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                trailingIcon = {
                                    Text(
                                        text = selectedTransactionCurrency,
                                        fontSize = 10.sp,
                                        color = activeThemeColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                            )

                            // 3. خيارات العملة (Currency selection buttons)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val famousCurrencies = listOf(
                                    Pair(currencyYer, context.getString(R.string.currency_label_yer)),
                                    Pair(currencyUsd, context.getString(R.string.currency_label_usd)),
                                    Pair(currencySar, context.getString(R.string.currency_label_sar))
                                )
                                famousCurrencies.forEachIndexed { index, (sym, label) ->
                                    val isSelected = selectedTransactionCurrency == sym
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                if (selectedTransactionCurrency != sym) {
                                                    selectedTransactionCurrency = sym
                                                    applyExchangeRate = (sym != currencySymbol)
                                                }
                                                if (sym == currencySymbol) {
                                                    applyExchangeRate = false
                                                }
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color(0xFFE91E63) else Color.DarkGray
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .border(1.5.dp, if (isSelected) Color(0xFFE91E63) else Color.Gray, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFE91E63))
                                                )
                                            }
                                        }
                                    }
                                    if (index < famousCurrencies.size - 1) {
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }
                                }
                            }

                            // Optional currency exchange rate option
                            if (selectedTransactionCurrency != currencySymbol) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                ) {
                                    // Clickable checkbox + label
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                applyExchangeRate = !applyExchangeRate
                                            }
                                            .padding(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .border(1.5.dp, activeThemeColor, RoundedCornerShape(3.dp))
                                                .background(if (applyExchangeRate) activeThemeColor else Color.Transparent, RoundedCornerShape(3.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (applyExchangeRate) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(id = R.string.habayeb_add_with_rate_question),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.DarkGray
                                        )
                                    }

                                    // Clickable rate display badge with edit icon
                                    if (applyExchangeRate) {
                                        val currentRate = com.example.ui.screens.habayeb.utils.ExchangeRateHelper.getRate(settings.exchangeRatesJson, currencySymbol, selectedTransactionCurrency)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(activeThemeColor.copy(alpha = 0.08f))
                                                .clickable {
                                                    tempRateStr = if (currentRate > 0.0) currentRate.toString() else ""
                                                    showRateSetupOverlay = true
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "1 $selectedTransactionCurrency = $currentRate $currencySymbol",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = activeThemeColor
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "تعديل سعر الصرف",
                                                tint = activeThemeColor,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // 4. بيان العملية (Details/Statement field - notesStr)
                            OutlinedTextField(
                                value = notesStr,
                                onValueChange = { notesStr = it },
                                label = { Text(stringResource(id = R.string.hint_description), fontSize = 10.sp) },
                                placeholder = { Text(stringResource(id = R.string.hint_description), fontSize = 10.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(notesFocusRequester),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { phoneFocusRequester.requestFocus() }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = activeThemeColor,
                                    focusedLabelColor = activeThemeColor,
                                    cursorColor = activeThemeColor,
                                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                                ),
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            val year = selectedCalendar.get(Calendar.YEAR)
                                            val month = selectedCalendar.get(Calendar.MONTH)
                                            val day = selectedCalendar.get(Calendar.DAY_OF_MONTH)
                                            android.app.DatePickerDialog(
                                                context,
                                                { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                                                    val newCal = Calendar.getInstance().apply {
                                                        set(Calendar.YEAR, selectedYear)
                                                        set(Calendar.MONTH, selectedMonth)
                                                        set(Calendar.DAY_OF_MONTH, selectedDayOfMonth)
                                                        set(Calendar.HOUR_OF_DAY, 12)
                                                        set(Calendar.MINUTE, 0)
                                                        set(Calendar.SECOND, 0)
                                                        set(Calendar.MILLISECOND, 0)
                                                    }
                                                    selectedCalendar = newCal
                                                },
                                                year,
                                                month,
                                                day
                                            ).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = stringResource(id = R.string.habayeb_tx_date),
                                            tint = activeThemeColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            )

                            // 5. رقم الهاتف (Phone Input)
                            OutlinedTextField(
                                value = phoneStr,
                                onValueChange = { phoneStr = it },
                                label = { Text(stringResource(id = R.string.habayeb_phone_label), fontSize = 10.sp) },
                                placeholder = { Text(stringResource(id = R.string.habayeb_contact_picker), fontSize = 10.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(phoneFocusRequester),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = activeThemeColor,
                                    focusedLabelColor = activeThemeColor,
                                    cursorColor = activeThemeColor,
                                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                                ),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                        if (hasPermission) {
                                            contactPickerLauncher.launch(null)
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                        }
                                    }, modifier = Modifier.size(24.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Contacts,
                                            contentDescription = stringResource(id = R.string.habayeb_contact_picker),
                                            tint = activeThemeColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            )

                            // Merged Actions Row: له / عليه on the right (RTL start), Save button on the left (RTL end)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // "له / عليه" switcher (placed first in code to be rendered on the RIGHT side in RTL)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // "عليه" option
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                initialType = "OWED_BY_THEM"
                                            }
                                            .padding(horizontal = 2.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.habayeb_owed),
                                            fontSize = 10.sp,
                                            fontWeight = if (initialType == "OWED_BY_THEM") FontWeight.Bold else FontWeight.Normal,
                                            color = if (initialType == "OWED_BY_THEM") Color(0xFFDC2626) else Color.Gray
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .border(1.5.dp, if (initialType == "OWED_BY_THEM") Color(0xFFDC2626) else Color.LightGray, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (initialType == "OWED_BY_THEM") {
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFDC2626))
                                                )
                                            }
                                        }
                                    }

                                    // "له" option
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                initialType = "OWED_TO_THEM"
                                            }
                                            .padding(horizontal = 2.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.habayeb_to_them),
                                            fontSize = 10.sp,
                                            fontWeight = if (initialType == "OWED_TO_THEM") FontWeight.Bold else FontWeight.Normal,
                                            color = if (initialType == "OWED_TO_THEM") Color(0xFF10B981) else Color.Gray
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .border(1.5.dp, if (initialType == "OWED_TO_THEM") Color(0xFF10B981) else Color.LightGray, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (initialType == "OWED_TO_THEM") {
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF10B981))
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // "تأكيد وحفظ" (Save Button - placed second in code to be rendered on the LEFT side in RTL)
                                Button(
                                    enabled = !isSavingCustomer,
                                    onClick = {
                                        if (nameStr.trim().isBlank()) {
                                            Toast.makeText(context, context.getString(R.string.habayeb_toast_enter_name), Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val cleanAmountStr = com.example.ui.screens.habayeb.utils.CurrencyConfig.normalizeDigits(initialAmountStr).trim()
                                        if (cleanAmountStr.isBlank()) {
                                            Toast.makeText(context, context.getString(R.string.habayeb_required_field), Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val actualInitialAmount = cleanAmountStr.toDoubleOrNull() ?: 0.0
                                        
                                        if (actualInitialAmount < 0.0) {
                                            Toast.makeText(context, context.getString(R.string.habayeb_toast_initial_amount_negative), Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }

                                        val isForeignSelected = selectedTransactionCurrency != currencySymbol
                                        if (isForeignSelected && applyExchangeRate) {
                                            val hasStoredRate = com.example.ui.screens.habayeb.utils.ExchangeRateHelper.hasRate(settings.exchangeRatesJson, currencySymbol, selectedTransactionCurrency)
                                            if (!hasStoredRate) {
                                                tempRateStr = ""
                                                showRateSetupOverlay = true
                                                return@Button
                                            }
                                        }

                                        isSavingCustomer = true
                                        val transactionTimestamp = selectedCalendar.timeInMillis / 1000

                                        val newCustomer = HabayebCustomer(
                                            id = "cust_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
                                            name = nameStr.trim(),
                                            phone = phoneStr.trim(),
                                            notes = notesStr.trim(),
                                            createdAt = transactionTimestamp,
                                            initialType = initialType
                                        )
                                        val exchangeRate = if (isForeignSelected && applyExchangeRate) {
                                            settingsRate
                                        } else {
                                            1.0
                                        }
                                        val finalEquivalentAmount = if (isForeignSelected && applyExchangeRate) {
                                            com.example.ui.screens.habayeb.utils.CurrencyConfig.convertAmount(actualInitialAmount, currencySymbol, selectedTransactionCurrency, exchangeRate)
                                        } else {
                                            0.0
                                        }
                                        val finalAmount = if (isForeignSelected && applyExchangeRate) finalEquivalentAmount else actualInitialAmount

                                        val finalDetails = if (notesStr.trim().isBlank()) context.getString(R.string.habayeb_opening_balance_default_desc) else notesStr.trim()

                                        viewModel.saveHabayebCustomer(
                                            customer = newCustomer,
                                            initialAmount = finalAmount,
                                            initialType = initialType,
                                            customTimestamp = transactionTimestamp,
                                            initialDetails = com.example.ui.screens.habayeb.utils.CurrencyConfig.formatDescriptionWithCurrency(finalDetails, selectedTransactionCurrency),
                                            isForeign = isForeignSelected,
                                            currencyCode = selectedTransactionCurrency,
                                            foreignAmount = actualInitialAmount,
                                            exchangeRate = exchangeRate,
                                            isRateCalculated = isForeignSelected && applyExchangeRate,
                                            equivalentAmount = finalEquivalentAmount
                                        )
                                        Toast.makeText(context, context.getString(R.string.habayeb_toast_save_success), Toast.LENGTH_SHORT).show()
                                        onCustomerAdded()
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = activeThemeColor,
                                        contentColor = Color.White
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.btn_save),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCalculator) {
        CalculatorDialog(
            onDismiss = { showCalculator = false },
            onValueConfirmed = { value ->
                initialAmountStr = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
                showCalculator = false
            },
            activeThemeColor = activeThemeColor,
            activeSubColor = activeSubColor
        )
    }
}
