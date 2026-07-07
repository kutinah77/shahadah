package com.example.ui.screens.habayeb.components

import androidx.compose.material3.MaterialTheme

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.local.entities.HabayebCustomer
import com.example.data.local.entities.HabayebTransaction
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.screens.CalculatorDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun AddTransactionPopup(
    customer: HabayebCustomer,
    viewModel: FinanceViewModel,
    initialSelectedType: String = "OWED_BY_THEM",
    editingTransaction: HabayebTransaction? = null,
    onDismiss: () -> Unit,
    activeThemeColor: Color,
    activeSubColor: Color
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    val customersUiState by viewModel.customersUiState.collectAsStateWithLifecycle()
    val customerState = customersUiState.customers.find { it.id == customer.id }
    val netDebt = customerState?.netDebt ?: 0.0

    // If netDebt >= 0, we are lending (we are owed). If netDebt < 0, we are borrowing (we owe).
    val isOwedByThem = netDebt >= 0.0

    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val currencySymbol = settings.currencySymbol

    val initialCurrencyAndDesc = remember(editingTransaction) {
        if (editingTransaction != null) {
            com.example.ui.screens.habayeb.utils.CurrencyConfig.parseTransactionCurrency(
                editingTransaction.description,
                currencySymbol
            )
        } else {
            Pair(currencySymbol, "")
        }
    }

    var selectedTransactionCurrency by rememberSaveable {
        mutableStateOf(editingTransaction?.currency_code?.let { if (it == "DEFAULT") currencySymbol else it } ?: initialCurrencyAndDesc.first)
    }

    val isForeignSelected = selectedTransactionCurrency != currencySymbol
    var applyExchangeRate by rememberSaveable { mutableStateOf(editingTransaction?.is_rate_calculated ?: false) }
    
    val currentRateVal = com.example.ui.screens.habayeb.utils.ExchangeRateHelper.getRate(settings.exchangeRatesJson, currencySymbol, selectedTransactionCurrency)
    val settingsRate = if (currentRateVal <= 0.0) 1.0 else currentRateVal

    var amountStr by rememberSaveable {
        mutableStateOf(
            editingTransaction?.let {
                if (it.is_foreign) {
                    if (it.foreign_amount % 1.0 == 0.0) it.foreign_amount.toInt().toString() else it.foreign_amount.toString()
                } else {
                    if (it.amount % 1.0 == 0.0) it.amount.toInt().toString() else it.amount.toString()
                }
            } ?: ""
        )
    }
    var descStr by rememberSaveable { mutableStateOf(if (editingTransaction != null) initialCurrencyAndDesc.second else "") }
    var selectedType by rememberSaveable { mutableStateOf(editingTransaction?.type ?: (if (isOwedByThem) "OWED_BY_THEM" else "OWED_TO_THEM")) }
    
    val amountFocusRequester = remember { FocusRequester() }
    val descFocusRequester = remember { FocusRequester() }
    val softwareKeyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        try {
            amountFocusRequester.requestFocus()
            softwareKeyboardController?.show()
        } catch(e: Exception) {}
    }

    val isLendOperationSelected = customer.initialType == "OWED_BY_THEM"
    var dateMillis by rememberSaveable { mutableStateOf(editingTransaction?.timestamp?.let { it * 1000 } ?: System.currentTimeMillis()) }
    var showCalculator by rememberSaveable { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showRateSetupOverlay by rememberSaveable { mutableStateOf(false) }
    var tempRateStr by rememberSaveable { mutableStateOf("") }

    val activeColor = if (selectedType == "OWED_BY_THEM" || selectedType == "OWED_TO_THEM") Color(0xFFEF5350) else Color(0xFF66BB6A)

    val executeSave = { finalActionType: String ->
        softwareKeyboardController?.hide()
        if (!isSaving) {
            isSaving = true

            val cleanAmountStr = com.example.ui.screens.habayeb.utils.CurrencyConfig.normalizeDigits(amountStr).trim()
            val amount = cleanAmountStr.toDoubleOrNull() ?: 0.0
            val hasStoredRate = com.example.ui.screens.habayeb.utils.ExchangeRateHelper.hasRate(settings.exchangeRatesJson, currencySymbol, selectedTransactionCurrency)
            if (isForeignSelected && applyExchangeRate && !hasStoredRate) {
                tempRateStr = ""
                showRateSetupOverlay = true
                isSaving = false
            } else if (amount <= 0.0 && descStr.trim().isBlank()) {
                Toast.makeText(context, context.getString(R.string.habayeb_toast_amount_or_details_required), Toast.LENGTH_SHORT).show()
                isSaving = false
            } else if (amount < 0.0) {
                Toast.makeText(context, context.getString(R.string.habayeb_toast_valid_amount), Toast.LENGTH_SHORT).show()
                isSaving = false
            } else {
                if (editingTransaction != null) {
                    viewModel.deleteHabayebTransaction(editingTransaction.id)
                }

                val finalEquivalentAmount = if (isForeignSelected && applyExchangeRate) {
                    amount * settingsRate
                } else {
                    0.0
                }

                val presetMainTxId = null

                viewModel.addHabayebTransaction(
                    customerId = customer.id,
                    type = finalActionType,
                    amount = if (isForeignSelected && applyExchangeRate) finalEquivalentAmount else amount,
                    desc = com.example.ui.screens.habayeb.utils.CurrencyConfig.formatDescriptionWithCurrency(descStr.trim(), selectedTransactionCurrency),
                    timestamp = dateMillis / 1000,
                    linkedMainTxId = presetMainTxId,
                    isForeign = isForeignSelected,
                    currencyCode = selectedTransactionCurrency,
                    foreignAmount = amount,
                    exchangeRate = if (applyExchangeRate) settingsRate else 1.0,
                    isRateCalculated = isForeignSelected && applyExchangeRate,
                    equivalentAmount = finalEquivalentAmount
                )
                Toast.makeText(context, context.getString(R.string.habayeb_toast_tx_save_success), Toast.LENGTH_SHORT).show()
                onDismiss()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            val debtInteractionSource = remember { MutableInteractionSource() }
            val isDebtPressed by debtInteractionSource.collectIsPressedAsState()
            val debtScale by animateFloatAsState(
                targetValue = if (isDebtPressed) 0.95f else 1f,
                animationSpec = spring(
                    dampingRatio = 0.5f,
                    stiffness = 1500f
                ),
                label = "DebtBtnScale"
            )

            val payInteractionSource = remember { MutableInteractionSource() }
            val isPayPressed by payInteractionSource.collectIsPressedAsState()
            val payScale by animateFloatAsState(
                targetValue = if (isPayPressed) 0.95f else 1f,
                animationSpec = spring(
                    dampingRatio = 0.5f,
                    stiffness = 1500f
                ),
                label = "PayBtnScale"
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Crossfade(targetState = showRateSetupOverlay, label = "FormTransition") { isSetup ->
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
                            onConfirm = { newRate ->
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
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                        // Header (title and back click)
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).padding(bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (editingTransaction != null) stringResource(id = R.string.add_transaction_title_edit) else stringResource(id = R.string.add_transaction_title_new),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = activeThemeColor
                        )
                        Text(
                            text = stringResource(id = R.string.add_transaction_account_label, "${customer.name.take(15)}${if (customer.name.length > 15) ".." else ""}"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(24.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.habayeb_go_back), tint = activeThemeColor, modifier = Modifier.size(16.dp))
                    }
                }

                val dynamicThemeColor = if (isLendOperationSelected) Color(0xFFEF4444) else Color(0xFF10B981)
                val dynamicSubColor = if (isLendOperationSelected) Color(0xFFFEE2E2) else Color(0xFFD1FAE5)

                // Input box Centered with Calculator leading and YR trailing
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(amountFocusRequester),
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.habayeb_amount) + " *",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { descFocusRequester.requestFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                        unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                        focusedBorderColor = dynamicThemeColor,
                        unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outlineVariant else Color.LightGray.copy(alpha = 0.6f),
                        cursorColor = dynamicThemeColor
                    ),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    leadingIcon = {
                        IconButton(onClick = { showCalculator = true }) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = stringResource(id = R.string.habayeb_calculator),
                                tint = dynamicThemeColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    trailingIcon = {
                        Text(
                            text = selectedTransactionCurrency,
                            color = dynamicThemeColor,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 12.dp),
                            fontSize = 14.sp
                        )
                    },
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                val formattedSelectedDate = remember(dateMillis) {
                    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale("ar"))
                    sdf.format(Date(dateMillis))
                }

                // Detail comments area with leading Calendar symbol
                OutlinedTextField(
                    value = descStr,
                    onValueChange = { descStr = it },
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.habayeb_tx_desc_optional),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                        unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                        focusedBorderColor = dynamicThemeColor,
                        unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outlineVariant else Color.LightGray.copy(alpha = 0.6f),
                        cursorColor = dynamicThemeColor
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 13.sp),
                    leadingIcon = {
                        Icon(Icons.Default.Menu, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formattedSelectedDate,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            IconButton(
                                onClick = {
                                    val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                                            calendar.set(Calendar.YEAR, selectedYear)
                                            calendar.set(Calendar.MONTH, selectedMonth)
                                            calendar.set(Calendar.DAY_OF_MONTH, selectedDayOfMonth)

                                            android.app.TimePickerDialog(
                                                context,
                                                { _, hourOfDay, minute ->
                                                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                                    calendar.set(Calendar.MINUTE, minute)
                                                    dateMillis = calendar.timeInMillis
                                                },
                                                calendar.get(Calendar.HOUR_OF_DAY),
                                                calendar.get(Calendar.MINUTE),
                                                false
                                            ).show()
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = stringResource(id = R.string.habayeb_tx_date),
                                    tint = dynamicThemeColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp, max = 56.dp)
                        .focusRequester(descFocusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))
                
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(4.dp))

                // Currency Radio buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val famousCurrencies = listOf(
                        Pair(stringResource(R.string.currency_yer), stringResource(R.string.currency_label_yer)),
                        Pair(stringResource(R.string.currency_usd), stringResource(R.string.currency_label_usd)),
                        Pair(stringResource(R.string.currency_sar), stringResource(R.string.currency_label_sar))
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
                                        applyExchangeRate = false
                                    }
                                    if (sym == currencySymbol) {
                                        applyExchangeRate = false
                                    }
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFFE91E63) else Color.DarkGray
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            // Custom Radio Button Circle
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, if (isSelected) Color(0xFFE91E63) else Color.Gray, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE91E63))
                                    )
                                }
                            }
                        }
                        if (index < famousCurrencies.size - 1) {
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                    }
                }

                if (isForeignSelected) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val hasStoredRate = com.example.ui.screens.habayeb.utils.ExchangeRateHelper.hasRate(settings.exchangeRatesJson, currencySymbol, selectedTransactionCurrency)
                                if (!applyExchangeRate) {
                                    if (hasStoredRate) {
                                        applyExchangeRate = true
                                    } else {
                                        tempRateStr = ""
                                        showRateSetupOverlay = true
                                    }
                                } else {
                                    applyExchangeRate = false
                                }
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        // Square (checkbox) on the right
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .border(1.dp, activeThemeColor, RoundedCornerShape(4.dp))
                                .background(if (applyExchangeRate) activeThemeColor else Color.Transparent, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (applyExchangeRate) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        // Text on the left of the square
                        Text(
                            text = stringResource(id = R.string.add_transaction_exchange_rate_prompt),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))





                Spacer(modifier = Modifier.height(8.dp))

                val handleActionClick = { type: String ->
                    val cleanAmountStr = com.example.ui.screens.habayeb.utils.CurrencyConfig.normalizeDigits(amountStr).trim()
                    val amount = cleanAmountStr.toDoubleOrNull() ?: 0.0
                    if (amount <= 0.0 && descStr.trim().isBlank()) {
                        Toast.makeText(context, context.getString(R.string.add_transaction_error_empty), Toast.LENGTH_SHORT).show()
                    } else if (amount < 0.0) {
                        Toast.makeText(context, context.getString(R.string.habayeb_toast_valid_amount), Toast.LENGTH_SHORT).show()
                    } else {
                        focusManager.clearFocus()
                        softwareKeyboardController?.hide()
                        executeSave(type)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        enabled = !isSaving,
                        onClick = {
                            handleActionClick(if (isLendOperationSelected) "OWED_BY_THEM" else "OWED_TO_THEM")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444), // Red
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.btn_new_debt),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Button(
                        enabled = !isSaving,
                        onClick = {
                            handleActionClick(if (isLendOperationSelected) "PAYMENT_BY_THEM" else "PAYMENT_TO_THEM")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981), // Green
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Text(
                            text = if (isLendOperationSelected) stringResource(id = R.string.btn_receive) else stringResource(id = R.string.btn_pay),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
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
                amountStr = value.toInt().toString()
                showCalculator = false
            },
            activeThemeColor = activeThemeColor,
            activeSubColor = activeSubColor
        )
    }
}
