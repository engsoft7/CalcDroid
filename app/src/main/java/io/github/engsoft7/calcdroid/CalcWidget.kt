package io.github.engsoft7.calcdroid

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.RemoteViews
import java.text.DecimalFormatSymbols

/**
 * Calculadora básica na tela inicial. Cada tecla manda um broadcast para cá;
 * a conta usa a mesma lógica do app (CalcState.press) e fica guardada nas
 * preferências do widget.
 */
class CalcWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = render(context, load(context))
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_KEY) {
            val key = intent.getStringExtra(EXTRA_KEY) ?: return
            val state = load(context).press(key)
            save(context, state)
            val manager = AppWidgetManager.getInstance(context)
            manager.updateAppWidget(ComponentName(context, CalcWidget::class.java), render(context, state))
        } else {
            super.onReceive(context, intent)
        }
    }

    companion object {
        private const val ACTION_KEY = "io.github.engsoft7.calcdroid.WIDGET_KEY"
        private const val EXTRA_KEY = "key"
        private const val PREFS = "calc_widget"

        // Tecla do layout → código usado na expressão (o mesmo do app).
        val KEYS = listOf(
            R.id.k_c to "C", R.id.k_bs to "⌫", R.id.k_pct to "%", R.id.k_div to "/",
            R.id.k_7 to "7", R.id.k_8 to "8", R.id.k_9 to "9", R.id.k_mul to "*",
            R.id.k_4 to "4", R.id.k_5 to "5", R.id.k_6 to "6", R.id.k_sub to "-",
            R.id.k_1 to "1", R.id.k_2 to "2", R.id.k_3 to "3", R.id.k_add to "+",
            R.id.k_0 to "0", R.id.k_dot to ".", R.id.k_eq to "=",
        )

        fun load(context: Context): CalcState {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return CalcState(
                expression = p.getString("expression", "") ?: "",
                lastExpression = p.getString("last", "") ?: "",
                justEvaluated = p.getBoolean("evaluated", false),
            )
        }

        fun save(context: Context, state: CalcState) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("expression", state.expression)
                .putString("last", state.lastExpression)
                .putBoolean("evaluated", state.justEvaluated)
                .apply()
        }

        /** Texto do visor e da linha de cima (conta anterior ou prévia do resultado). */
        fun displayTexts(context: Context, state: CalcState): Pair<String, String> {
            val locale = context.resources.configuration.locales[0]
            val sep = DecimalFormatSymbols.getInstance(locale).decimalSeparator
            val main = when {
                state.expression.isEmpty() -> "0"
                state.expression == ERROR_TEXT -> context.getString(R.string.error)
                else -> prettyExpression(state.expression, sep)
            }
            val top = if (state.justEvaluated) prettyExpression(state.lastExpression, sep)
            else state.preview()?.let { "= " + prettyExpression(it, sep) } ?: ""
            return main to top
        }

        private fun render(context: Context, state: CalcState): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_calc)
            val (main, top) = displayTexts(context, state)
            views.setTextViewText(R.id.widget_display, main)
            views.setTextViewText(R.id.widget_history, top)
            val locale = context.resources.configuration.locales[0]
            views.setTextViewText(R.id.k_dot, DecimalFormatSymbols.getInstance(locale).decimalSeparator.toString())
            views.setContentDescription(R.id.k_open, context.getString(R.string.widget_open))
            views.setContentDescription(R.id.k_bs, context.getString(R.string.key_backspace))
            KEYS.forEachIndexed { index, (id, key) ->
                val intent = Intent(context, CalcWidget::class.java)
                    .setAction(ACTION_KEY)
                    .putExtra(EXTRA_KEY, key)
                    // Uma URI por tecla: sem ela o sistema reaproveitaria o mesmo PendingIntent.
                    .setData(Uri.parse("calcdroid://widget/key/$index"))
                views.setOnClickPendingIntent(
                    id,
                    PendingIntent.getBroadcast(context, index, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                )
            }
            views.setOnClickPendingIntent(R.id.k_open, openAppIntent(context))
            return views
        }

        fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}

/** Atalho nas configurações rápidas: abre a calculadora de qualquer tela. */
class CalcTileService : TileService() {

    override fun onStartListening() {
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(CalcWidget.openAppIntent(this))
        } else {
            // Até o Android 13 este é o único jeito de abrir uma tela pelo atalho.
            @SuppressLint("StartActivityAndCollapseDeprecated")
            @Suppress("DEPRECATION")
            startActivityAndCollapse(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
