package id.dinusrealm.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.dinusrealm.app.core.i18n.LocalAppLanguage
import id.dinusrealm.app.core.i18n.tr
import id.dinusrealm.app.core.model.BillingItem
import id.dinusrealm.app.core.model.PaymentBankMethod
import id.dinusrealm.app.core.network.ApiResult
import id.dinusrealm.app.core.repo.DinusRepository
import id.dinusrealm.app.ui.components.CurvedHeroHeader
import id.dinusrealm.app.ui.components.FruitBadge
import id.dinusrealm.app.ui.components.KeyValueRow
import id.dinusrealm.app.ui.components.SorbetCard
import id.dinusrealm.app.ui.components.SorbetEmptyState
import id.dinusrealm.app.ui.components.SorbetLoadingBox
import id.dinusrealm.app.ui.components.SorbetMessageStrip
import id.dinusrealm.app.ui.components.SorbetSegmentTabs
import id.dinusrealm.app.ui.components.formatRupiah
import id.dinusrealm.app.ui.components.renderHtmlPlain
import id.dinusrealm.app.ui.components.tabular
import id.dinusrealm.app.ui.theme.DinusIcons
import id.dinusrealm.app.ui.theme.DinusTheme
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun BillingScreen(
    repo: DinusRepository,
    onBack: () -> Unit
) {
    val p = DinusTheme.palette
    val lang = LocalAppLanguage.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val cachedBills by repo.billingItems.collectAsState()
    val cachedMethods by repo.paymentMethods.collectAsState()
    var tabIdx by remember { mutableIntStateOf(0) }
    var regBills by remember(cachedBills) { mutableStateOf(cachedBills.filter { it.category == "Reguler" }) }
    var nonRegBills by remember(cachedBills) { mutableStateOf(cachedBills.filter { it.category != "Reguler" }) }
    var bankMethods by remember(cachedMethods) { mutableStateOf(cachedMethods) }
    var notesPlain by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var copyFeedback by remember { mutableStateOf<String?>(null) }

    fun load() {
        isLoading = true
        scope.launch {
            coroutineScope {
                val r = async { repo.api.getRegularBilling() }
                val nr = async { repo.api.getNonRegularBilling() }
                val info = async { repo.api.getPaymentInfo() }
                val rRes = r.await()
                val nrRes = nr.await()
                if (rRes is ApiResult.Success) regBills = rRes.data
                if (nrRes is ApiResult.Success) nonRegBills = nrRes.data
                (info.await() as? ApiResult.Success)?.let {
                    bankMethods = it.data.methods
                    notesPlain = renderHtmlPlain(it.data.notesHtml)
                }
                if (rRes is ApiResult.Success || nrRes is ApiResult.Success) {
                    repo.db.replaceBillingSnapshots(regBills + nonRegBills)
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    val allUnpaid = (regBills + nonRegBills).filter { !it.isPaid && it.remainingAmount > 0L }
    val totalDue = allUnpaid.sumOf { it.remainingAmount }
    val isClear = totalDue <= 0L

    val copyVa: (String, String) -> Unit = { bank, va ->
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("VA $bank", va))
        copyFeedback = tr(lang, "$bank VA ($va) copied!", "Nomor VA $bank ($va) disalin!")
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CurvedHeroHeader(
            title = tr("Billing & Virtual Accounts", "Tagihan & Pembayaran VA"),
            subtitle = if (isClear) tr("No active unpaid balance", "Tidak ada tunggakan aktif")
                else tr("Please settle before the due date", "Segera lunasi sebelum batas jatuh tempo"),
            gradientStart = if (isClear) p.mint else p.strawberry,
            gradientEnd = if (isClear) p.mint else p.strawberry,
            onBack = onBack,
            rightStat = if (isClear) tr("PAID", "LUNAS") else formatRupiah(totalDue),
            rightStatLabel = if (isClear) tr("Status", "Status Keuangan") else tr("Remaining Due", "Sisa Tagihan")
        )

        Spacer(Modifier.height(6.dp))
        SorbetSegmentTabs(
            listOf(
                tr("Regular", "Reguler"),
                tr("Non-Regular", "Non-Reguler"),
                "${tr("Virtual Account", "Virtual Account")} (${bankMethods.size})"
            ),
            tabIdx,
            { tabIdx = it },
            accent = p.strawberry
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!copyFeedback.isNullOrBlank()) {
                item {
                    SorbetMessageStrip(
                        message = copyFeedback!!,
                        accent = p.mint,
                        softBg = p.mintSoft,
                        onDismiss = { copyFeedback = null }
                    )
                }
            }

            if (isLoading && regBills.isEmpty() && bankMethods.isEmpty()) {
                item { SorbetLoadingBox(tr("Loading billing & VA info...", "Memuat info tagihan & VA..."), p.strawberry) }
            } else if (tabIdx == 2) {
                if (bankMethods.isEmpty() && notesPlain.isBlank()) {
                    item {
                        SorbetEmptyState(
                            icon = DinusIcons.Wallet,
                            accent = p.berry,
                            softBg = p.berrySoft,
                            title = tr("Virtual Accounts empty", "Virtual Account kosong"),
                            subtitle = tr("No VA data returned from server yet.", "Data VA belum tersedia dari server.")
                        )
                    }
                } else {
                    items(bankMethods.size) { i ->
                        BankVaCard(method = bankMethods[i], onCopy = copyVa, onOpenGuide = { url ->
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                        })
                    }
                    if (notesPlain.isNotBlank()) {
                        item {
                            SorbetCard {
                                Text(tr("BIKU Payment Notes", "Catatan Pembayaran BIKU"), style = MaterialTheme.typography.titleMedium, color = p.ink)
                                Spacer(Modifier.height(8.dp))
                                Text(notesPlain, style = MaterialTheme.typography.bodyMedium, color = p.inkMuted)
                            }
                        }
                    }
                }
            } else {
                val shown = if (tabIdx == 0) regBills else nonRegBills
                if (shown.isEmpty()) {
                    item {
                        SorbetEmptyState(
                            icon = DinusIcons.CheckCircle,
                            accent = p.mint,
                            softBg = p.mintSoft,
                            title = tr("No bills found", "Belum ada tagihan"),
                            subtitle = tr("No active invoices in this category.", "Tidak ada tagihan aktif pada kategori ini."),
                            actionLabel = tr("View Virtual Accounts", "Lihat Virtual Account"),
                            onAction = { tabIdx = 2 }
                        )
                    }
                } else {
                    items(shown.size) { i ->
                        BillDetailCard(bill = shown[i], bankMethods = bankMethods, onCopyVa = copyVa)
                    }
                }
            }
        }
    }
}

@Composable
private fun BillDetailCard(
    bill: BillingItem,
    bankMethods: List<PaymentBankMethod>,
    onCopyVa: (String, String) -> Unit
) {
    val p = DinusTheme.palette
    val accent = if (bill.isPaid) p.mint else p.strawberry
    val soft = if (bill.isPaid) p.mintSoft else p.strawberrySoft

    SorbetCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(bill.title, style = MaterialTheme.typography.titleMedium, color = p.ink, modifier = Modifier.weight(1f))
            FruitBadge(bill.statusText, accent, soft)
        }
        if (bill.infoText.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(bill.infoText, style = MaterialTheme.typography.labelMedium, color = p.inkMuted)
        }
        Spacer(Modifier.height(10.dp))
        KeyValueRow(tr("Total Billed", "Total Tagihan"), formatRupiah(bill.totalAmount))
        KeyValueRow(tr("Paid", "Terbayar"), formatRupiah(bill.paidAmount), valueColor = p.mint)
        KeyValueRow(tr("Remaining Balance", "Sisa Belum Terbayar"), formatRupiah(bill.remainingAmount), valueColor = accent)
        if (bill.viaText.isNotBlank()) KeyValueRow(tr("Via", "Via"), bill.viaText)
        if (bill.dueDate.isNotBlank()) KeyValueRow(tr("Date", "Tanggal"), bill.dueDate)

        if (bill.breakdown.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(tr("Fee Breakdown", "Rincian Biaya"), style = MaterialTheme.typography.labelMedium, color = p.inkMuted)
            Spacer(Modifier.height(4.dp))
            bill.breakdown.forEach { (label, amt) ->
                KeyValueRow(label, formatRupiah(amt))
            }
        }

        if (bankMethods.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(tr("Quick Copy Virtual Account", "Salin Cepat Virtual Account"), style = MaterialTheme.typography.labelMedium, color = p.inkMuted)
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                bankMethods.forEach { m ->
                    Surface(
                        onClick = { onCopyVa(m.bankName, m.vaNumber) },
                        shape = RoundedCornerShape(14.dp),
                        color = p.surfaceVariant,
                        border = BorderStroke(1.dp, p.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(m.bankName, style = MaterialTheme.typography.labelSmall, color = p.inkMuted)
                                Text(m.vaNumber, style = tabular(MaterialTheme.typography.bodyMedium).copy(fontWeight = FontWeight.SemiBold), color = p.ink)
                            }
                            Icon(DinusIcons.Copy, contentDescription = tr("Copy VA", "Salin VA"), tint = p.ink, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BankVaCard(
    method: PaymentBankMethod,
    onCopy: (String, String) -> Unit,
    onOpenGuide: (String) -> Unit
) {
    val p = DinusTheme.palette
    SorbetCard(onClick = { onCopy(method.bankName, method.vaNumber) }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(method.bankName, style = MaterialTheme.typography.titleMedium, color = p.ink)
                    if (method.validationNote.isNotBlank()) {
                        FruitBadge(method.validationNote, p.mint, p.mintSoft)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    method.vaNumber,
                    style = tabular(MaterialTheme.typography.titleLarge),
                    color = p.ink
                )
                if (method.accountName.isNotBlank()) {
                    Text("${tr("Acct.", "a.n.")} ${method.accountName}", style = MaterialTheme.typography.labelMedium, color = p.inkMuted)
                }
            }
            Icon(DinusIcons.Copy, contentDescription = tr("Copy VA", "Salin VA"), tint = p.ink, modifier = Modifier.size(22.dp))
        }
        if (method.tutorialUrl.startsWith("http")) {
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { onOpenGuide(method.tutorialUrl) }) {
                Text(tr("Open Payment Guide", "Buka Panduan Bayar"), color = p.ink, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
