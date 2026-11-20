package com.medilife.ai.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.medilife.ai.MediLifeApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DoseActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medId = intent.getLongExtra(EXTRA_MED, -1)
        val at = intent.getLongExtra(EXTRA_AT, -1)
        val status = intent.getStringExtra(EXTRA_STATUS) ?: return
        NotificationManagerCompat.from(context).cancel(intent.getIntExtra(EXTRA_NOTIF, 0))
        if (medId < 0 || at < 0) return
        val pending = goAsync()
        val repo = (context.applicationContext as MediLifeApp).container.repo
        CoroutineScope(Dispatchers.IO).launch {
            try { repo.logDose(medId, at, status) } finally { pending.finish() }
        }
    }

    companion object {
        const val EXTRA_MED = "med"; const val EXTRA_AT = "at"; const val EXTRA_STATUS = "status"; const val EXTRA_NOTIF = "notif"
    }
}
