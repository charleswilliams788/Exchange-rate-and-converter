package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.auth.AuthHelper
import com.example.data.model.ConversionDirection
import com.example.ui.components.AccountOverviewCard
import com.example.ui.components.AccountTransactionsList
import com.example.ui.components.AuthScreenCard
import com.example.ui.components.ConvertBalanceDialog
import com.example.ui.components.EmailVerificationCard
import com.example.ui.components.CurrencyConverterCard
import com.example.ui.components.CustomRateDialog
import com.example.ui.components.DenominationCard
import com.example.ui.components.GoogleSignInCard
import com.example.ui.components.HistorySection
import com.example.ui.components.ProfileDialog
import com.example.ui.components.QuickPresetsRow
import com.example.ui.components.QuickReferenceTable
import com.example.ui.components.RateAlertDialog
import com.example.ui.components.RateHeaderCard
import com.example.ui.components.ReceiveMoneyDialog
import com.example.ui.components.SendMoneyDialog
import com.example.ui.theme.NairaGold
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    currencyViewModel: CurrencyViewModel,
    walletViewModel: WalletViewModel,
    modifier: Modifier = Modifier
) {
    val currencyUiState by currencyViewModel.uiState.collectAsState()
    val walletUiState by walletViewModel.uiState.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Rates & Converter, 1: Account & Transfers

    var showRateAlertDialog by remember { mutableStateOf(false) }
    var showRateSettingsDialog by remember { mutableStateOf(false) }
    var showSendMoneyDialog by remember { mutableStateOf(false) }
    var showReceiveMoneyDialog by remember { mutableStateOf(false) }
    var showConvertBalanceDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    // Display status notifications
    LaunchedEffect(currencyUiState.statusNotification) {
        currencyUiState.statusNotification?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            currencyViewModel.dismissNotification()
        }
    }

    LaunchedEffect(walletUiState.statusMessage) {
        walletUiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            walletViewModel.dismissStatusMessage()
        }
    }

    // Auto sign-in check on initial screen load
    LaunchedEffect(Unit) {
        AuthHelper.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = coroutineScope
        )
    }

    val copyToClipboard: (String) -> Unit = { text ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Currency Info", text)
        clipboard.setPrimaryClip(clip)
        coroutineScope.launch {
            snackbarHostState.showSnackbar("Copied $text to clipboard")
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CurrencyExchange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Veltro",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (walletUiState.currentUser != null) {
                                    walletUiState.userAccount?.accountTag ?: "Veltro Wallet Active"
                                } else {
                                    "Your Money. Without Borders."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Push Notification Alert Button
                    IconButton(
                        onClick = { showRateAlertDialog = true },
                        modifier = Modifier.testTag("btn_open_alert_dialog")
                    ) {
                        BadgedBox(
                            badge = {
                                if (currencyUiState.isAlertEnabled) {
                                    Badge(containerColor = NairaGold)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (currencyUiState.isAlertEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                contentDescription = "Rate Alerts",
                                tint = if (currencyUiState.isAlertEnabled) NairaGold else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Market settings button
                    IconButton(
                        onClick = { showRateSettingsDialog = true },
                        modifier = Modifier.testTag("btn_top_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Market Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // User Profile and Sign Out buttons if authenticated
                    if (walletUiState.isAuthenticated) {
                        IconButton(
                            onClick = { showProfileDialog = true },
                            modifier = Modifier.testTag("btn_top_profile")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "User Profile",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = {
                                walletViewModel.signOut(credentialManager)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Signed out successfully")
                                }
                            },
                            modifier = Modifier.testTag("btn_sign_out")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Sign Out",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Navigation Tabs: Rates & Converter vs. Working Account
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CurrencyExchange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Converter", fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("tab_converter")
                )

                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Send & Receive", fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("tab_account")
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 680.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (selectedTabIndex == 0) {
                        // TAB 0: CONVERTER & REAL-TIME RATES
                        if (currencyUiState.isAlertEnabled) {
                            item {
                                ActiveAlertBanner(
                                    targetRate = currencyUiState.targetAlertRate,
                                    condition = currencyUiState.alertCondition,
                                    onClick = { showRateAlertDialog = true }
                                )
                            }
                        }

                        // Rate Header Card (Live market ticker)
                        item {
                            RateHeaderCard(
                                effectiveRate = currencyUiState.effectiveRate,
                                officialRate = currencyUiState.officialRate,
                                rateMode = currencyUiState.rateMode,
                                parallelSpreadPercent = currencyUiState.parallelSpreadPercent,
                                lastUpdatedText = currencyUiState.lastUpdatedText,
                                isOffline = currencyUiState.isOffline,
                                isRefreshing = currencyUiState.isRefreshing,
                                onRefreshClick = { currencyViewModel.fetchLiveRates(isManualRefresh = true) },
                                onModeSelect = { currencyViewModel.onRateModeSelected(it) },
                                onOpenRateSettings = { showRateSettingsDialog = true }
                            )
                        }

                        // Converter Card ($ ⇄ ₦)
                        item {
                            CurrencyConverterCard(
                                direction = currencyUiState.direction,
                                inputAmount = currencyUiState.inputAmount,
                                calculatedAmount = currencyUiState.calculatedAmount,
                                onInputChange = { currencyViewModel.onInputAmountChanged(it) },
                                onDirectionToggle = { currencyViewModel.onDirectionToggled() },
                                onCopyResult = copyToClipboard,
                                onSaveConversion = {
                                    currencyViewModel.saveConversionHistory()
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Saved conversion to history")
                                    }
                                }
                            )
                        }

                        // Quick Presets Row
                        item {
                            QuickPresetsRow(
                                direction = currencyUiState.direction,
                                onPresetClick = { currencyViewModel.onPresetSelected(it) }
                            )
                        }

                        // Banknote Breakdown
                        item {
                            val nairaAmount = if (currencyUiState.direction == ConversionDirection.USD_TO_NGN) {
                                currencyUiState.calculatedAmount
                            } else {
                                currencyUiState.inputAmount.toDoubleOrNull() ?: 0.0
                            }
                            DenominationCard(nairaAmount = nairaAmount)
                        }

                        // Quick Rate Reference Table
                        item {
                            QuickReferenceTable(
                                rate = currencyUiState.effectiveRate,
                                onSelectAmount = { usdAmount ->
                                    if (currencyUiState.direction != ConversionDirection.USD_TO_NGN) {
                                        currencyViewModel.onDirectionToggled()
                                    }
                                    currencyViewModel.onPresetSelected(usdAmount)
                                }
                            )
                        }

                        // Conversion History
                        item {
                            HistorySection(
                                history = currencyUiState.recentConversions,
                                onSelectRecord = { record ->
                                    if (record.fromCurrency == "USD" && currencyUiState.direction != ConversionDirection.USD_TO_NGN) {
                                        currencyViewModel.onDirectionToggled()
                                    } else if (record.fromCurrency == "NGN" && currencyUiState.direction != ConversionDirection.NGN_TO_USD) {
                                        currencyViewModel.onDirectionToggled()
                                    }
                                    currencyViewModel.onInputAmountChanged(record.fromAmount.toString())
                                },
                                onClearHistory = { currencyViewModel.clearHistory() }
                            )
                        }
                    } else {
                        // TAB 1: WORKING ACCOUNT, SEND & RECEIVE
                        val user = walletUiState.currentUser
                        val account = walletUiState.userAccount
                        val pendingVerificationUser = walletUiState.pendingVerificationUser

                        if (pendingVerificationUser != null && !walletUiState.isEmailVerified) {
                            item {
                                EmailVerificationCard(
                                    email = pendingVerificationUser.email,
                                    isLoading = walletUiState.isAuthLoading,
                                    cooldownSeconds = walletUiState.resendCooldownSeconds,
                                    errorMessage = walletUiState.authErrorMessage,
                                    successMessage = walletUiState.authSuccessMessage,
                                    onCheckStatus = { walletViewModel.checkVerificationStatus() },
                                    onResendEmail = { walletViewModel.resendVerification() },
                                    onVerifyOtp = { otp -> walletViewModel.verifyOtp(otp) },
                                    onCancelOrSignOut = { walletViewModel.cancelPendingVerification() }
                                )
                            }
                        } else if (account == null) {
                            item {
                                AuthScreenCard(
                                    isLoading = walletUiState.isAuthLoading,
                                    errorMessage = walletUiState.authErrorMessage,
                                    successMessage = walletUiState.authSuccessMessage,
                                    onSignIn = { email, pass ->
                                        walletViewModel.signIn(email, pass)
                                    },
                                    onSignUp = { email, pass, name ->
                                        walletViewModel.signUp(email, pass, name)
                                    },
                                    onForgotPassword = { email ->
                                        walletViewModel.sendPasswordReset(email)
                                    },
                                    onQuickDemoAccess = {
                                        walletViewModel.useDemoAccount()
                                    }
                                )
                            }
                        } else {
                            // Working Account Overview Card
                            item {
                                AccountOverviewCard(
                                    account = account,
                                    userEmail = user?.email ?: account.email,
                                    cryptoBalances = walletUiState.cryptoBalances,
                                    onSendClick = { showSendMoneyDialog = true },
                                    onReceiveClick = { showReceiveMoneyDialog = true },
                                    onConvertClick = { showConvertBalanceDialog = true },
                                    onAddFundsClick = { },
                                    onCopyAccount = copyToClipboard,
                                    onEditProfileClick = { showProfileDialog = true }
                                )
                            }

                            // Real-time Account Transactions & Activity
                            item {
                                AccountTransactionsList(
                                    transactions = walletUiState.transactions,
                                    currentUserId = user?.uid ?: account.userId
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Rate Alert Dialog
    if (showRateAlertDialog) {
        RateAlertDialog(
            isAlertEnabled = currencyUiState.isAlertEnabled,
            targetAlertRate = currencyUiState.targetAlertRate,
            alertCondition = currencyUiState.alertCondition,
            currentRate = currencyUiState.effectiveRate,
            onToggleAlert = { enable ->
                currencyViewModel.toggleAlert(enable, context)
            },
            onSaveTarget = { target, condition ->
                currencyViewModel.setTargetAlertRate(target)
                currencyViewModel.setAlertCondition(condition)
                currencyViewModel.toggleAlert(true, context)
            },
            onSendTestNotification = {
                currencyViewModel.sendTestAlertNotification(context)
            },
            onDismiss = { showRateAlertDialog = false }
        )
    }

    // Rate Settings Dialog
    if (showRateSettingsDialog) {
        CustomRateDialog(
            officialRate = currencyUiState.officialRate,
            currentSpread = currencyUiState.parallelSpreadPercent,
            currentCustomRate = currencyUiState.customRate,
            onSaveSpread = { currencyViewModel.onUpdateParallelSpread(it) },
            onSaveCustomRate = { currencyViewModel.onUpdateCustomRate(it) },
            onDismiss = { showRateSettingsDialog = false }
        )
    }

    // Send Money Dialog
    if (showSendMoneyDialog && walletUiState.userAccount != null) {
        SendMoneyDialog(
            account = walletUiState.userAccount!!,
            currentExchangeRate = currencyUiState.effectiveRate,
            onSend = { recipientAccount, recipientName, recipientBank, amount, currency, note ->
                walletViewModel.sendMoney(
                    recipientAccount = recipientAccount,
                    recipientName = recipientName,
                    recipientBank = recipientBank,
                    amount = amount,
                    currency = currency,
                    exchangeRate = currencyUiState.effectiveRate,
                    note = note
                )
            },
            onDismiss = { showSendMoneyDialog = false }
        )
    }

    // Receive Money Dialog
    if (showReceiveMoneyDialog && walletUiState.userAccount != null) {
        ReceiveMoneyDialog(
            account = walletUiState.userAccount!!,
            onCopyText = copyToClipboard,
            onSubmitDeposit = { crypto, network, amt ->
                walletViewModel.submitSimulatedDeposit(crypto, network, amt)
            },
            onDismiss = { showReceiveMoneyDialog = false }
        )
    }

    // Convert Balance Dialog
    if (showConvertBalanceDialog && walletUiState.userAccount != null) {
        ConvertBalanceDialog(
            account = walletUiState.userAccount!!,
            exchangeRate = currencyUiState.effectiveRate,
            onConvert = { fromCurr, toCurr, amt ->
                walletViewModel.convertOwnBalance(
                    fromCurrency = fromCurr,
                    toCurrency = toCurr,
                    amount = amt,
                    exchangeRate = currencyUiState.effectiveRate
                )
            },
            onDismiss = { showConvertBalanceDialog = false }
        )
    }

    // Profile Dialog
    if (showProfileDialog && walletUiState.userAccount != null) {
        ProfileDialog(
            account = walletUiState.userAccount!!,
            onSaveProfile = { name, tag, bank ->
                walletViewModel.updateProfile(name, tag, bank)
            },
            onSignOut = {
                showProfileDialog = false
                walletViewModel.signOut(credentialManager)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Signed out successfully")
                }
            },
            onCopyText = copyToClipboard,
            onDismiss = { showProfileDialog = false }
        )
    }
}

@Composable
fun ActiveAlertBanner(
    targetRate: Double,
    condition: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val symbol = if (condition == "GREATER_OR_EQUAL") "≥" else "≤"
    val formattedTarget = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(targetRate)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("banner_active_alert")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Alert Active: Notify when 1 USD $symbol ₦$formattedTarget",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Text(
                text = "Edit",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

