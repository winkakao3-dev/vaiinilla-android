package com.vaiinilla.app.ui.screens

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.vaiinilla.app.domain.repository.SpaceAccount

/**
 * Ticket de la cuenta de una mesa o cancha. Es una cuenta para cobrar, no un comprobante fiscal (CFDI).
 * HTML en ancho de ticket de 80 mm: se imprime con la impresión del sistema o se guarda en PDF.
 */
fun buildAccountTicketHtml(
    spaceName: String,
    account: SpaceAccount,
    printedAt: String,
): String {
    val rows =
        account.orders.joinToString("") { order ->
            val who = order.clientName?.let { " · ${escapeHtml(it)}" }.orEmpty()
            val state = if (order.pending) "" else " (pagado)"
            "<tr><td class=\"l\">#${order.folio}$who$state<br><span class=\"s\">${escapeHtml(
                order.itemsSummary,
            )}</span></td>" +
                "<td class=\"r\">$${order.total}</td></tr>"
        }
    return """<!doctype html><html><head><meta charset="utf-8">
<style>
  @page { size: 80mm auto; margin: 4mm; }
  body { font-family: sans-serif; font-size: 12px; color: #111; }
  h1 { font-size: 16px; margin: 0 0 2px; }
  .m { color: #555; font-size: 11px; margin-bottom: 8px; }
  table { width: 100%; border-collapse: collapse; }
  td { padding: 4px 0; border-bottom: 1px dashed #bbb; vertical-align: top; }
  .r { text-align: right; white-space: nowrap; padding-left: 8px; }
  .s { color: #555; font-size: 11px; }
  .t td { border-bottom: none; font-weight: bold; font-size: 14px; padding-top: 8px; }
  .n { margin-top: 10px; color: #555; font-size: 10px; text-align: center; }
</style></head><body>
<h1>${escapeHtml(spaceName)}</h1>
<div class="m">${escapeHtml(printedAt)}</div>
<table>$rows
<tr class="t"><td class="l">Total</td><td class="r">$${account.total}</td></tr>
<tr><td class="l">Pagado</td><td class="r">$${account.paid}</td></tr>
<tr><td class="l">Por cobrar</td><td class="r">$${account.pending}</td></tr>
</table>
<div class="n">Cuenta para cobrar. No es un comprobante fiscal.</div>
</body></html>"""
}

internal fun escapeHtml(text: String): String =
    buildString(text.length) {
        for (c in text) {
            when (c) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(c)
            }
        }
    }

/** Abre el diálogo de impresión del sistema (impresora o "Guardar como PDF") con la cuenta. */
fun printAccountTicket(
    context: Context,
    spaceName: String,
    html: String,
) {
    val webView = WebView(context)
    webView.webViewClient =
        object : WebViewClient() {
            override fun onPageFinished(
                view: WebView,
                url: String?,
            ) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
                val jobName = "Cuenta $spaceName"
                printManager.print(
                    jobName,
                    view.createPrintDocumentAdapter(jobName),
                    PrintAttributes.Builder().build(),
                )
            }
        }
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
}
