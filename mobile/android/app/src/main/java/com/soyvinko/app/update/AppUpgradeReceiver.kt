package com.soyvinko.app.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.soyvinko.app.BuildConfig
import com.soyvinko.app.R

/**
 * Recibe el broadcast del sistema ACTION_MY_PACKAGE_REPLACED cuando una nueva
 * versión de SoyVinko ha sido instalada con éxito sobre la anterior.
 * 
 * Se encarga de:
 * 1. Intentar reiniciar la app automáticamente para que no se quede cerrada.
 * 2. Enviar una notificación con prioridad alta avisando que la actualización terminó.
 */
class AppUpgradeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.i("SoyVinko", "Nueva versión instalada con éxito: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")

            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("just_updated", true)
            }

            // 1. Intentar relanzar la actividad principal directamente
            if (launchIntent != null) {
                try {
                    context.startActivity(launchIntent)
                } catch (e: Exception) {
                    Log.w("SoyVinko", "Reinicio directo de actividad bloqueado por el sistema", e)
                }
            }

            // 2. Notificación en barra de estado para asegurar que el usuario esté informado
            notifyUpgradeSuccess(context, launchIntent)
        }
    }

    private fun notifyUpgradeSuccess(context: Context, launchIntent: Intent?) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channelId = "app_updates"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Actualizaciones de SoyVinko",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Avisos sobre la instalación de actualizaciones de la aplicación"
                }
                nm.createNotificationChannel(channel)
            }

            val pendingIntent = launchIntent?.let {
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                PendingIntent.getActivity(context, 1001, it, flags)
            }

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("¡Actualización completada con éxito!")
                .setContentText("SoyVinko se actualizó a la versión ${BuildConfig.VERSION_NAME}. Toca para abrir.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            nm.notify(1001, notification)
        } catch (e: Exception) {
            Log.w("SoyVinko", "No se pudo mostrar la notificación de actualización", e)
        }
    }
}
