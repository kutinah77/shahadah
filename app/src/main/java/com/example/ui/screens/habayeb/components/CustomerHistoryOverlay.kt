package com.example.ui.screens.habayeb.components

import androidx.compose.material3.MaterialTheme

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entities.HabayebCustomer
import com.example.data.local.entities.HabayebTransaction
import com.example.data.serialization.PdfReportGenerator
import com.example.ui.helper.formatCurrency
import com.example.ui.helper.getInitialColor
import com.example.ui.screens.habayeb.utils.HabayebRecurringManager
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CustomerHistoryOverlay(
    customer: HabayebCustomer,
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit,
    onAddTransaction: (HabayebCustomer, String) -> Unit,
    activeThemeColor: Color,
    activeSubColor: Color,
    currencySymbol: String,
    contentPadding: PaddingValues = PaddingValues()
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val customers by viewModel.habayebCustomersState.collectAsStateWithLifecycle()
    val activeCustomer = customers.find { it.id == customer.id } ?: customer

    val transactions by viewModel.habayebTransactionsState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isPrivacyMode by viewModel.isPrivacyModeEnabled.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var isPdfExporting by remember { mutableStateOf(false) }
    var showRateModifyDialog by remember { mutableStateOf(false) }
    var exchangeTxToModify by remember { mutableStateOf<HabayebTransaction?>(null) }
    var showRateSetupOverlay by remember { mutableStateOf(false) }
    var setupOverlayCurrency by remember { mutableStateOf("") }
    var setupOverlayInitialRate by remember { mutableStateOf("") }

    // Search state
    var txSearchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    // Advanced Filter states
    var dateFilterMode by remember { mutableStateOf(0) } // 0: All, 1: Today, 2: This Month, 3: Custom Date Range
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }
    var typeFilterMode by remember { mutableStateOf(0) } // 0: All, 1: Debts only, 2: Payments only
    var selectedCurrencyFilter by remember { mutableStateOf<String?>(null) }

    val showDatePicker = { initialTime: Long?, onDateSelected: (Long) -> Unit ->
        val calendar = java.util.Calendar.getInstance()
        if (initialTime != null) {
            calendar.timeInMillis = initialTime
        }
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.YEAR, year)
                    set(java.util.Calendar.MONTH, month)
                    set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                onDateSelected(selectedCal.timeInMillis)
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Customer transactions list filtered by client ID
    val allCustomerTxs = remember(transactions, activeCustomer) {
        transactions.filter { it.customerId == activeCustomer.id }.sortedBy { it.timestamp }
    }

    // Filtered by Search query (supporting description and amount filtering), date, and type filters
    val displayedTxs = remember(allCustomerTxs, txSearchQuery, dateFilterMode, customStartDate, customEndDate, typeFilterMode, selectedCurrencyFilter) {
        val calendar = java.util.Calendar.getInstance()
        val todayStart = calendar.apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis / 1000
        val todayEnd = todayStart + 86400

        val calendarMonth = java.util.Calendar.getInstance()
        val monthStart = calendarMonth.apply {
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis / 1000
        calendarMonth.add(java.util.Calendar.MONTH, 1)
        val monthEnd = calendarMonth.timeInMillis / 1000

        val searchDebtStr = context.getString(R.string.customer_history_search_debt)
        val searchPaymentStr = context.getString(R.string.customer_history_search_payment)
        
        val baseFiltered = allCustomerTxs.filter { tx ->
            // 1. Search Query filter
            val matchesSearch = if (txSearchQuery.isBlank()) {
                true
            } else {
                tx.description.contains(txSearchQuery, ignoreCase = true) ||
                tx.amount.toString().contains(txSearchQuery) ||
                tx.foreign_amount.toString().contains(txSearchQuery) ||
                (if (tx.type == "OWED_BY_THEM") searchDebtStr else searchPaymentStr).contains(txSearchQuery)
            }

            // 2. Date filter
            val matchesDate = when (dateFilterMode) {
                1 -> tx.timestamp in todayStart..todayEnd // Today
                2 -> tx.timestamp in monthStart..monthEnd // This Month
                3 -> { // Custom range
                    val startSec = (customStartDate ?: 0L) / 1000
                    val endSec = if (customEndDate != null) (customEndDate!! / 1000) + 86400 else Long.MAX_VALUE
                    tx.timestamp in startSec..endSec
                }
                else -> true // All
            }

            // 3. Type filter
            val matchesType = when (typeFilterMode) {
                1 -> tx.type == "OWED_BY_THEM" || tx.type == "OWED_TO_THEM" // Debts
                2 -> tx.type == "PAYMENT_BY_THEM" || tx.type == "PAYMENT_TO_THEM" // Payments
                else -> true // All
            }

            // 4. Currency filter
            val matchesCurrency = if (selectedCurrencyFilter != null) {
                val (txCurrency, _) = com.example.ui.screens.habayeb.utils.CurrencyConfig.getTransactionCurrencyAndAmount(tx, currencySymbol)
                txCurrency == selectedCurrencyFilter
            } else {
                true
            }

            matchesSearch && matchesDate && matchesType && matchesCurrency
        }
        baseFiltered.sortedByDescending { it.timestamp }
    }

    val currencyKeys = remember(allCustomerTxs, currencySymbol) {
        (listOf(currencySymbol) + allCustomerTxs.map { 
            com.example.ui.screens.habayeb.utils.CurrencyConfig.getTransactionCurrencyAndAmount(it, currencySymbol).first 
        }).distinct()
    }

    val owedByThemMap = remember(allCustomerTxs, currencyKeys, currencySymbol) {
        currencyKeys.associateWith { curr ->
            allCustomerTxs.filter { tx -> tx.type == "OWED_BY_THEM" }.sumOf { tx ->
                val (txCurrency, amountVal) = com.example.ui.screens.habayeb.utils.CurrencyConfig.getTransactionCurrencyAndAmount(tx, currencySymbol)
                if (txCurrency == curr) amountVal else 0.0
            }
        }
    }

    val paymentByThemMap = remember(allCustomerTxs, currencyKeys, currencySymbol) {
        currencyKeys.associateWith { curr ->
            allCustomerTxs.filter { tx -> tx.type == "PAYMENT_BY_THEM" }.sumOf { tx ->
                val (txCurrency, amountVal) = com.example.ui.screens.habayeb.utils.CurrencyConfig.getTransactionCurrencyAndAmount(tx, currencySymbol)
                if (txCurrency == curr) amountVal else 0.0
            }
        }
    }

    val owedToThemMap = remember(allCustomerTxs, currencyKeys, currencySymbol) {
        currencyKeys.associateWith { curr ->
            allCustomerTxs.filter { tx -> tx.type == "OWED_TO_THEM" }.sumOf { tx ->
                val (txCurrency, amountVal) = com.example.ui.screens.habayeb.utils.CurrencyConfig.getTransactionCurrencyAndAmount(tx, currencySymbol)
                if (txCurrency == curr) amountVal else 0.0
            }
        }
    }

    val paymentToThemMap = remember(allCustomerTxs, currencyKeys, currencySymbol) {
        currencyKeys.associateWith { curr ->
            allCustomerTxs.filter { tx -> tx.type == "PAYMENT_TO_THEM" }.sumOf { tx ->
                val (txCurrency, amountVal) = com.example.ui.screens.habayeb.utils.CurrencyConfig.getTransactionCurrencyAndAmount(tx, currencySymbol)
                if (txCurrency == curr) amountVal else 0.0
            }
        }
    }
    
    val netDebtMap = remember(currencyKeys, owedByThemMap, paymentByThemMap, owedToThemMap, paymentToThemMap) { 
        currencyKeys.associateWith { curr ->
            (owedByThemMap[curr] ?: 0.0) - (paymentByThemMap[curr] ?: 0.0) - (owedToThemMap[curr] ?: 0.0) + (paymentToThemMap[curr] ?: 0.0)
        }
    }

    // Default to main currency, or first available if main isn't there
    val primaryDisplayCurrency = if (currencyKeys.contains(currencySymbol)) currencySymbol else currencyKeys.firstOrNull() ?: currencySymbol
    val netDebt = netDebtMap[primaryDisplayCurrency] ?: 0.0

    // Calculate sequential running balances (chronological order)
    val runningBalances = remember(allCustomerTxs, currencySymbol) {
        val chronological = allCustomerTxs.sortedBy { it.timestamp }
        val balancesMap = mutableMapOf<String, Double>()
        val currentBalMap = mutableMapOf<String, Double>()
        for (tx in chronological) {
            val (txCurrency, amountVal) = com.example.ui.screens.habayeb.utils.CurrencyConfig.getTransactionCurrencyAndAmount(tx, currencySymbol)
            var currentBal = currentBalMap[txCurrency] ?: 0.0
            when (tx.type) {
                "OWED_BY_THEM" -> currentBal += amountVal
                "PAYMENT_BY_THEM" -> currentBal -= amountVal
                "OWED_TO_THEM" -> currentBal -= amountVal
                "PAYMENT_TO_THEM" -> currentBal += amountVal
            }
            currentBalMap[txCurrency] = currentBal
            balancesMap[tx.id] = currentBal
        }
        balancesMap
    }

    // Calculate sequential sequence numbers (chronological order)
    val txSequenceNumbers = remember(allCustomerTxs) {
        val chronological = allCustomerTxs.sortedBy { it.timestamp }
        chronological.mapIndexed { idx, tx -> tx.id to (idx + 1) }.toMap()
    }

    // Dialogs States
    var editingTransactionForDialog by remember { mutableStateOf<HabayebTransaction?>(null) }
    var showAddTransactionDialogFromHistory by remember { mutableStateOf<HabayebCustomer?>(null) }
    var defaultTransactionTypeFromHistory by remember { mutableStateOf("OWED_BY_THEM") }

    var transactionForOptionsDialog by remember { mutableStateOf<HabayebTransaction?>(null) }
    var transactionForAutoRepeatDialog by remember { mutableStateOf<HabayebTransaction?>(null) }

    var isTxMultiSelectActive by remember { mutableStateOf(false) }
    val selectedTxIds = remember { mutableStateListOf<String>() }
    var showDeleteBulkTxConfirmDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (isTxMultiSelectActive) {
            isTxMultiSelectActive = false
            selectedTxIds.clear()
        } else if (isSearchActive) {
            isSearchActive = false
            txSearchQuery = ""
        } else {
            onDismiss()
        }
    }

    var refreshRecurringTrigger by remember { mutableStateOf(0) }
    val activeRecurringTxIds = remember(activeCustomer.id, refreshRecurringTrigger, allCustomerTxs) {
        HabayebRecurringManager.getAllConfigs(context)
            .filter { it.isActive && it.customerId == activeCustomer.id }
            .map { it.originalTxId }
            .toSet()
    }

    LaunchedEffect(activeCustomer.id) {
        HabayebRecurringManager.checkAndExecuteRecurring(context, viewModel) { count ->
            Toast.makeText(context, context.getString(R.string.customer_history_toast_recurring_added, count, activeCustomer.name), Toast.LENGTH_LONG).show()
        }
    }

    var confirmDeleteCust by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editedNameStr by remember(activeCustomer.name) { mutableStateOf(activeCustomer.name) }
    var editedPhoneStr by remember(activeCustomer.phone) { mutableStateOf(activeCustomer.phone) }

    // Dialogs code
    if (confirmDeleteCust) {
        AlertDialog(
            onDismissRequest = { confirmDeleteCust = false },
            title = { Text(stringResource(id = R.string.habayeb_delete_account_title), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text(stringResource(id = R.string.habayeb_delete_account_confirm, activeCustomer.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteHabayebCustomer(activeCustomer.id)
                        Toast.makeText(context, context.getString(R.string.habayeb_toast_delete_success), Toast.LENGTH_SHORT).show()
                        confirmDeleteCust = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(id = R.string.habayeb_delete_yes), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteCust = false }) {
                    Text(stringResource(id = R.string.habayeb_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showEditNameDialog) {
        val editNameFocusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            editNameFocusRequester.requestFocus()
        }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text(stringResource(id = R.string.habayeb_edit_name_title), fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editedNameStr,
                        onValueChange = { editedNameStr = it },
                        label = { Text(stringResource(id = R.string.habayeb_account_name)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(editNameFocusRequester),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = activeThemeColor,
                            focusedLabelColor = activeThemeColor,
                            cursorColor = activeThemeColor
                        )
                    )

                    OutlinedTextField(
                        value = editedPhoneStr,
                        onValueChange = { editedPhoneStr = it },
                        label = { Text(stringResource(id = R.string.habayeb_phone_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = activeThemeColor,
                            focusedLabelColor = activeThemeColor,
                            cursorColor = activeThemeColor
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedNameStr.isNotBlank()) {
                            viewModel.updateHabayebCustomer(
                                activeCustomer.copy(
                                    name = editedNameStr.trim(),
                                    phone = editedPhoneStr.trim()
                                )
                            )
                            Toast.makeText(context, context.getString(R.string.habayeb_toast_update_success), Toast.LENGTH_SHORT).show()
                        }
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = activeThemeColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(id = R.string.habayeb_save_edit))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text(stringResource(id = R.string.habayeb_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Zero Hardcoded Helper functions for sending SMS statement or transaction SMS/WhatsApp
    fun triggerSmsStatement(customer: HabayebCustomer, debt: Double) {
        val debtStatus = when {
            debt > 0.0 -> context.getString(R.string.habayeb_statement_status_owed, formatCurrency(kotlin.math.abs(debt), currencySymbol))
            debt < 0.0 -> context.getString(R.string.habayeb_statement_status_to_them, formatCurrency(kotlin.math.abs(debt), currencySymbol))
            else -> context.getString(R.string.habayeb_statement_status_balanced)
        }
        val body = context.getString(R.string.habayeb_statement_title, customer.name) +
                debtStatus +
                context.getString(R.string.habayeb_statement_thanks)
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${customer.phone}")
                putExtra("sms_body", body)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, body)
            }
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.habayeb_statement_send)))
        }
    }

    fun triggerWhatsAppStatement(customer: HabayebCustomer, debt: Double) {
        val debtStatus = when {
            debt > 0.0 -> context.getString(R.string.habayeb_statement_status_owed, formatCurrency(kotlin.math.abs(debt), currencySymbol))
            debt < 0.0 -> context.getString(R.string.habayeb_statement_status_to_them, formatCurrency(kotlin.math.abs(debt), currencySymbol))
            else -> context.getString(R.string.habayeb_statement_status_balanced)
        }
        val body = context.getString(R.string.habayeb_statement_title, customer.name) +
                debtStatus +
                context.getString(R.string.habayeb_statement_thanks)
        try {
            val waUrl = "https://wa.me/${customer.phone.replace("+", "").replace(" ", "")}?text=${Uri.encode(body)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
            context.startActivity(intent)
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, body)
            }
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.habayeb_statement_send_whatsapp)))
        }
    }

    fun triggerSingleTxSms(tx: HabayebTransaction, customer: HabayebCustomer) {
        val txTypeAr = when (tx.type) {
            "OWED_BY_THEM" -> context.getString(R.string.habayeb_tx_type_owed_by)
            "PAYMENT_BY_THEM" -> context.getString(R.string.habayeb_tx_type_payment_by)
            "OWED_TO_THEM" -> context.getString(R.string.habayeb_tx_type_owed_to)
            "PAYMENT_TO_THEM" -> context.getString(R.string.habayeb_tx_type_payment_to)
            else -> context.getString(R.string.habayeb_tx_type_generic_move)
        }
        val dateStr = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar")).format(Date(tx.timestamp * 1000))
        val amountStr = if (tx.is_foreign) {
            "${tx.foreign_amount} ${tx.currency_code}" + if (tx.is_rate_calculated) context.getString(R.string.habayeb_equivalent_val, formatCurrency(tx.equivalent_amount, currencySymbol), tx.exchange_rate.toString()) else ""
        } else {
            formatCurrency(tx.amount, currencySymbol)
        }
        val currentDebtStatusStr = if (netDebt > 0) context.getString(R.string.habayeb_owed) else if (netDebt < 0) context.getString(R.string.habayeb_to_them) else context.getString(R.string.habayeb_balanced)
        val body = context.getString(R.string.habayeb_tx_sms_notice) +
                context.getString(R.string.habayeb_tx_client, customer.name) +
                context.getString(R.string.habayeb_tx_type_label, txTypeAr) +
                context.getString(R.string.habayeb_tx_amount_label, amountStr) +
                context.getString(R.string.habayeb_tx_details_label, tx.description.ifEmpty { context.getString(R.string.habayeb_no_notes) }) +
                context.getString(R.string.habayeb_tx_date_label, dateStr) +
                context.getString(R.string.habayeb_tx_total_bal, formatCurrency(kotlin.math.abs(netDebt), currencySymbol), currentDebtStatusStr)

        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${customer.phone}")
                putExtra("sms_body", body)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, body)
            }
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.habayeb_tx_send_notice)))
        }
    }

    fun triggerSingleTxWhatsApp(tx: HabayebTransaction, customer: HabayebCustomer) {
        val txTypeAr = when (tx.type) {
            "OWED_BY_THEM" -> context.getString(R.string.habayeb_tx_type_owed_by)
            "PAYMENT_BY_THEM" -> context.getString(R.string.habayeb_tx_type_payment_by)
            "OWED_TO_THEM" -> context.getString(R.string.habayeb_tx_type_owed_to)
            "PAYMENT_TO_THEM" -> context.getString(R.string.habayeb_tx_type_payment_to)
            else -> context.getString(R.string.habayeb_tx_type_generic_move)
        }
        val dateStr = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar")).format(Date(tx.timestamp * 1000))
        val amountStr = if (tx.is_foreign) {
            "${tx.foreign_amount} ${tx.currency_code}" + if (tx.is_rate_calculated) context.getString(R.string.habayeb_equivalent_val, formatCurrency(tx.equivalent_amount, currencySymbol), tx.exchange_rate.toString()) else ""
        } else {
            formatCurrency(tx.amount, currencySymbol)
        }
        val currentDebtStatusStr = if (netDebt > 0) context.getString(R.string.habayeb_owed) else if (netDebt < 0) context.getString(R.string.habayeb_to_them) else context.getString(R.string.habayeb_balanced)
        val body = context.getString(R.string.habayeb_tx_whatsapp_notice) +
                context.getString(R.string.habayeb_tx_client_bold, customer.name) +
                context.getString(R.string.habayeb_tx_type_label_bold, txTypeAr) +
                context.getString(R.string.habayeb_tx_amount_label_bold, amountStr) +
                context.getString(R.string.habayeb_tx_details_label_bold, tx.description.ifEmpty { context.getString(R.string.habayeb_no_notes) }) +
                context.getString(R.string.habayeb_tx_date_label_bold, dateStr) +
                context.getString(R.string.habayeb_tx_total_bal_bold, formatCurrency(kotlin.math.abs(netDebt), currencySymbol), currentDebtStatusStr)

        try {
            val waUrl = "https://wa.me/${customer.phone.replace("+", "").replace(" ", "")}?text=${Uri.encode(body)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
            context.startActivity(intent)
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, body)
            }
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.habayeb_tx_whatsapp_choose)))
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // 1. FULL SCREEN CUSTOM APP BAR WITH INTEGRATED SEARCH
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        CustomerHistoryTopBar(
                            customerName = activeCustomer.name,
                            customerPhone = activeCustomer.phone,
                            isSearchActive = isSearchActive,
                            txSearchQuery = txSearchQuery,
                            activeThemeColor = activeThemeColor,
                            isPdfExporting = isPdfExporting,
                            isPhoneAvailable = activeCustomer.phone.isNotBlank(),
                            onSearchQueryChange = { txSearchQuery = it },
                            onSearchClose = {
                                isSearchActive = false
                                txSearchQuery = ""
                            },
                            onSearchOpen = { isSearchActive = true },
                            onPdfExportClick = {
                                isPdfExporting = true
                                PdfReportGenerator.generateAndHandleCustomerPdfReportAsync(
                                    context,
                                    coroutineScope,
                                    activeCustomer,
                                    netDebt,
                                    allCustomerTxs,
                                    "SHARE",
                                    onFinished = { isPdfExporting = false }
                                )
                            },
                            onCsvExportClick = {
                                com.example.data.serialization.CsvReportGenerator.generateAndShareCsvReport(
                                    context,
                                    coroutineScope,
                                    activeCustomer,
                                    allCustomerTxs,
                                    currencySymbol
                                )
                            },
                            onWhatsAppClick = {
                                triggerWhatsAppStatement(activeCustomer, netDebt)
                            },
                            onDeleteClick = {
                                confirmDeleteCust = true
                            },
                            onEditClick = {
                                showEditNameDialog = true
                            },
                            onFilterClick = {
                                showFilterMenu = true
                            },
                            onDismiss = onDismiss
                        )

                        if (!isSearchActive) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                            CustomerSummaryCard(
                                activeCustomer = activeCustomer,
                                currencySymbol = currencySymbol,
                                netDebtMap = netDebtMap,
                                selectedCurrencyFilter = selectedCurrencyFilter,
                                onCurrencyFilterSelected = { selectedCurrencyFilter = it }
                            )
                        }
                    } // Close Column
                } // Close Surface


                // 3. TABLE GRID COLUMN HEADER STRIP
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.habayeb_col_date),
                            modifier = Modifier.weight(1.2f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(id = R.string.habayeb_col_details),
                            modifier = Modifier.weight(1.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(id = R.string.habayeb_col_amount),
                            modifier = Modifier.weight(1.2f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 4. THE HIGH-DENSITY HIGH-FIDELITY TRANSACTION LIST
                if (displayedTxs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (txSearchQuery.isEmpty()) stringResource(id = R.string.habayeb_no_tx_recorded) else stringResource(id = R.string.habayeb_no_search_results),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.Top,
                        contentPadding = PaddingValues(
                            top = 2.dp,
                            bottom = contentPadding.calculateBottomPadding() + 80.dp
                        )
                    ) {
                        items(displayedTxs, key = { it.id }) { tx ->
                            val parsedCurrencyInfo = remember(tx.description) {
                                com.example.ui.screens.habayeb.utils.CurrencyConfig.parseTransactionCurrency(tx.description, currencySymbol)
                            }
                            val cleanDescription = parsedCurrencyInfo.second

                            val isSelected = selectedTxIds.contains(tx.id)

                            // Historical running balance at this exact transaction
                            val currentHistBalance = runningBalances[tx.id] ?: 0.0

                            val hasActiveRecurring = tx.id in activeRecurringTxIds
                            val txSeqNo = txSequenceNumbers[tx.id] ?: 0
                            val parentTxSeq = remember(tx.linkedMainTxId, txSequenceNumbers) {
                                if (tx.linkedMainTxId != null) {
                                    txSequenceNumbers[tx.linkedMainTxId]
                                } else null
                            }

                            CustomerTransactionRow(
                                tx = tx,
                                currencySymbol = currencySymbol,
                                initialType = activeCustomer.initialType,
                                isSelected = isSelected,
                                isTxMultiSelectActive = isTxMultiSelectActive,
                                hasActiveRecurring = hasActiveRecurring,
                                txSeqNo = txSeqNo,
                                parentTxSeq = parentTxSeq,
                                currentHistBalance = currentHistBalance,
                                activeThemeColor = activeThemeColor,
                                onSelectToggle = {
                                    if (isSelected) selectedTxIds.remove(tx.id)
                                    else selectedTxIds.add(tx.id)
                                    if (selectedTxIds.isEmpty()) {
                                        isTxMultiSelectActive = false
                                    }
                                },
                                onLongClick = {
                                    if (!isTxMultiSelectActive) {
                                        isTxMultiSelectActive = true
                                        selectedTxIds.add(tx.id)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                },
                                onOptionsClick = {
                                    transactionForOptionsDialog = tx
                                },
                                onScheduleClick = {
                                    transactionForAutoRepeatDialog = tx
                                },
                                onExchangeRateClick = {
                                    exchangeTxToModify = tx
                                    showRateModifyDialog = true
                                }
                            )
                        }
                    }
                }
            }

            // --- Multi-Select Floating Bar ---
            AnimatedVisibility(
                visible = isTxMultiSelectActive,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = contentPadding.calculateBottomPadding() + 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .shadow(16.dp, RoundedCornerShape(30.dp), spotColor = Color.Black.copy(alpha = 0.1f))
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(30.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(30.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Cancel Button
                    IconButton(
                        onClick = {
                            isTxMultiSelectActive = false
                            selectedTxIds.clear()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.habayeb_cancel),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Selection Info & Select All
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                val allSelected = displayedTxs.isNotEmpty() && displayedTxs.all { selectedTxIds.contains(it.id) }
                                if (allSelected) {
                                    selectedTxIds.clear()
                                } else {
                                    displayedTxs.forEach { if (!selectedTxIds.contains(it.id)) selectedTxIds.add(it.id) }
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        val allSelected = displayedTxs.isNotEmpty() && displayedTxs.all { selectedTxIds.contains(it.id) }
                        Icon(
                            imageVector = if (allSelected) Icons.Default.Check else Icons.Default.List,
                            contentDescription = stringResource(id = R.string.habayeb_all_selected),
                            tint = if (allSelected) activeThemeColor else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (allSelected) stringResource(id = R.string.habayeb_all_selected) else stringResource(id = R.string.habayeb_items_selected, selectedTxIds.size),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Delete Button
                    IconButton(
                        onClick = {
                            if (selectedTxIds.isNotEmpty()) {
                                showDeleteBulkTxConfirmDialog = true
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(if (isDark) Color(0xFF3E1F1F) else Color(0xFFFEF2F2), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.habayeb_delete),
                            tint = if (isDark) Color(0xFFEF5350) else Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 6. DELUXE PERSISTENT FLOATING ADDING ACTION BUTTON
            AnimatedVisibility(
                visible = !isTxMultiSelectActive,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomStart) // BottomStart = Right side in RTL
                    .padding(
                        bottom = contentPadding.calculateBottomPadding() + 16.dp,
                        start = 20.dp
                    )
            ) {
                FloatingActionButton(
                    onClick = {
                        val defaultType = if (netDebt >= 0.0) "OWED_BY_THEM" else "OWED_TO_THEM"
                        onAddTransaction(activeCustomer, defaultType)
                    },
                    containerColor = activeThemeColor,
                    contentColor = Color.White,
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(id = R.string.habayeb_add_tx_desc),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }

    // Secondary editing dialog if triggered inside customer details lists
    if (showAddTransactionDialogFromHistory != null) {
        AddTransactionPopup(
            customer = showAddTransactionDialogFromHistory!!,
            viewModel = viewModel,
            initialSelectedType = defaultTransactionTypeFromHistory,
            editingTransaction = editingTransactionForDialog,
            onDismiss = {
                showAddTransactionDialogFromHistory = null
                editingTransactionForDialog = null
            },
            activeThemeColor = activeThemeColor,
            activeSubColor = activeSubColor
        )
    }

    if (transactionForOptionsDialog != null) {
        val isRecurringOriginal = transactionForOptionsDialog!!.id in activeRecurringTxIds
        val optTx = transactionForOptionsDialog!!
        val parentSeq = if (optTx.linkedMainTxId != null) txSequenceNumbers[optTx.linkedMainTxId] else null
        TransactionOptionsDialog(
            transaction = optTx,
            customerName = activeCustomer.name,
            onDismiss = { transactionForOptionsDialog = null },
            onEdit = {
                editingTransactionForDialog = transactionForOptionsDialog
                defaultTransactionTypeFromHistory = transactionForOptionsDialog!!.type
                showAddTransactionDialogFromHistory = activeCustomer
                transactionForOptionsDialog = null
            },
            onDelete = {
                val txId = transactionForOptionsDialog!!.id
                viewModel.deleteHabayebTransaction(txId)
                HabayebRecurringManager.deleteConfigForTransaction(context, txId)
                Toast.makeText(context, context.getString(R.string.habayeb_toast_delete_tx_success), Toast.LENGTH_SHORT).show()
                refreshRecurringTrigger++
                transactionForOptionsDialog = null
            },
            onAutoRepeat = {
                transactionForAutoRepeatDialog = transactionForOptionsDialog
                transactionForOptionsDialog = null
            },
            activeThemeColor = activeThemeColor,
            activeSubColor = activeSubColor,
            isRecurringOriginal = isRecurringOriginal,
            onDeleteAutoRepeat = {
                val txId = transactionForOptionsDialog!!.id
                HabayebRecurringManager.deleteConfigForTransaction(context, txId)
                Toast.makeText(context, context.getString(R.string.habayeb_toast_stop_recurring_success), Toast.LENGTH_SHORT).show()
                refreshRecurringTrigger++
                transactionForOptionsDialog = null
            },
            parentSeqNumber = parentSeq
        )
    }

    if (transactionForAutoRepeatDialog != null) {
        RecurringTransactionPopup(
            transaction = transactionForAutoRepeatDialog!!,
            customerName = activeCustomer.name,
            onDismiss = { 
                transactionForAutoRepeatDialog = null 
                refreshRecurringTrigger++
            },
            activeThemeColor = activeThemeColor,
            activeSubColor = activeSubColor
        )
    }

    if (showDeleteBulkTxConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteBulkTxConfirmDialog = false },
            title = { Text(stringResource(id = R.string.habayeb_confirm_delete_txs), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text(stringResource(id = R.string.habayeb_confirm_delete_txs_msg, selectedTxIds.size)) },
            confirmButton = {
                Button(
                    onClick = {
                        val idsToDelete = selectedTxIds.toList()
                        idsToDelete.forEach { txId ->
                            viewModel.deleteHabayebTransaction(txId)
                            HabayebRecurringManager.deleteConfigForTransaction(context, txId)
                        }
                        Toast.makeText(context, context.getString(R.string.habayeb_toast_delete_bulk_success), Toast.LENGTH_SHORT).show()
                        selectedTxIds.clear()
                        isTxMultiSelectActive = false
                        refreshRecurringTrigger++
                        showDeleteBulkTxConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFFEF5350) else Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(id = R.string.habayeb_delete), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteBulkTxConfirmDialog = false }) {
                    Text(stringResource(id = R.string.habayeb_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showFilterMenu) {
        @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
        ModalBottomSheet(
            onDismissRequest = { showFilterMenu = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.habayeb_smart_filter),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Date Filter Segment
                Text(
                    text = stringResource(id = R.string.habayeb_filter_date),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val dateModes = listOf(
                    0 to stringResource(id = R.string.habayeb_filter_all_time),
                    1 to stringResource(id = R.string.habayeb_filter_today),
                    2 to stringResource(id = R.string.habayeb_filter_month),
                    3 to stringResource(id = R.string.habayeb_filter_custom)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dateModes.forEach { (mode, label) ->
                        val isSelected = dateFilterMode == mode
                        val chipBg = if (isSelected) activeThemeColor else MaterialTheme.colorScheme.outlineVariant
                        val chipText = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(chipBg)
                                .clickable {
                                    dateFilterMode = mode
                                    if (mode == 3 && customStartDate == null) {
                                        showDatePicker(customStartDate) { start ->
                                            customStartDate = start
                                            showDatePicker(customEndDate ?: start) { end ->
                                                customEndDate = end
                                            }
                                        }
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = chipText)
                        }
                    }
                }
                
                if (dateFilterMode == 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val startStr = customStartDate?.let { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(it)) } ?: "..."
                        val endStr = customEndDate?.let { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(it)) } ?: "..."
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant)
                                .clickable { showDatePicker(customStartDate) { customStartDate = it } }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Event, contentDescription = null, tint = activeThemeColor, modifier = Modifier.size(14.dp))
                                Text(startStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        Text(stringResource(id = R.string.habayeb_to_text), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant)
                                .clickable { showDatePicker(customEndDate ?: customStartDate) { customEndDate = it } }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Event, contentDescription = null, tint = activeThemeColor, modifier = Modifier.size(14.dp))
                                Text(endStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Type Filter Segment
                Text(
                    text = stringResource(id = R.string.habayeb_filter_by_type),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val typeModes = listOf(
                    0 to stringResource(id = R.string.habayeb_filter_all),
                    1 to stringResource(id = R.string.habayeb_filter_type_debts),
                    2 to stringResource(id = R.string.habayeb_filter_type_payments)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    typeModes.forEach { (mode, label) ->
                        val isSelected = typeFilterMode == mode
                        val chipBg = if (isSelected) activeThemeColor else MaterialTheme.colorScheme.outlineVariant
                        val chipText = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(chipBg)
                                .clickable { typeFilterMode = mode }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = chipText)
                        }
                    }
                }
            }
        }
    }

    // Centered, optimized, and unified Centralized Exchange Rate input triggering logic
    if (showRateModifyDialog && exchangeTxToModify != null) {
        val tx = exchangeTxToModify!!
        
        androidx.compose.ui.window.Dialog(onDismissRequest = { 
            showRateModifyDialog = false 
            showRateSetupOverlay = false
        }) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .width(260.dp)
                        .padding(8.dp)
                ) {
                    androidx.compose.animation.Crossfade(targetState = showRateSetupOverlay, label = "RateModifyTransition") { isSetup ->
                        if (isSetup) {
                            androidx.activity.compose.BackHandler {
                                showRateSetupOverlay = false
                            }
                            ExchangeRateSetupContent(
                                currencySymbol = currencySymbol,
                                selectedCurrency = setupOverlayCurrency,
                                initialRateStr = setupOverlayInitialRate,
                                activeThemeColor = activeThemeColor,
                                onDismiss = {
                                    showRateSetupOverlay = false
                                },
                                onConfirm = { newRate ->
                                    val settings = viewModel.settingsState.value
                                    val newSettings = settings.copy(
                                        exchangeRatesJson = com.example.ui.screens.habayeb.utils.ExchangeRateHelper.setRate(settings.exchangeRatesJson, currencySymbol, setupOverlayCurrency, newRate)
                                    )
                                    viewModel.saveSettings(newSettings)
                                    viewModel.updateTransactionExchangeRate(tx.id, newRate, true)
                                    showRateSetupOverlay = false
                                    showRateModifyDialog = false
                                    exchangeTxToModify = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        val settings = viewModel.settingsState.value
                                        val hasStoredRate = com.example.ui.screens.habayeb.utils.ExchangeRateHelper.hasRate(settings.exchangeRatesJson, currencySymbol, tx.currency_code)
                                        
                                        if (hasStoredRate) {
                                            val storedRate = com.example.ui.screens.habayeb.utils.ExchangeRateHelper.getRate(settings.exchangeRatesJson, currencySymbol, tx.currency_code)
                                            viewModel.updateTransactionExchangeRate(tx.id, storedRate, true)
                                            showRateModifyDialog = false
                                            exchangeTxToModify = null
                                        } else {
                                            setupOverlayCurrency = tx.currency_code
                                            setupOverlayInitialRate = ""
                                            showRateSetupOverlay = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1.2f).height(38.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(stringResource(id = R.string.habayeb_activate_exchange), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                
                                Button(
                                    onClick = {
                                        viewModel.updateTransactionExchangeRate(tx.id, tx.exchange_rate, false)
                                        showRateModifyDialog = false
                                        exchangeTxToModify = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(stringResource(id = R.string.habayeb_deactivate_exchange), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
