package cl.duoc.gestionoportunidades.nativo

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import cl.duoc.gestionoportunidades.MainActivity
import cl.duoc.gestionoportunidades.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object Recordatorios {
    const val CANAL_ID = "recordatorios_seguimiento"
    const val EXTRA_ID = "oportunidad_id"
    const val EXTRA_TITULO = "titulo"
    const val EXTRA_TEXTO = "texto"
    private val HORA_AVISO: LocalTime = LocalTime.of(9, 0)

    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ID,
                context.getString(R.string.canal_recordatorios_nombre),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = context.getString(R.string.canal_recordatorios_descripcion) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    fun tienePermiso(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun programarParaFecha(context: Context, oportunidadId: Long, fecha: LocalDate, titulo: String, texto: String) {
        val zona = ZoneId.systemDefault()
        val objetivo = fecha.atTime(HORA_AVISO).atZone(zona).toInstant().toEpochMilli()
        val minimo = System.currentTimeMillis() + 60_000
        programar(context, oportunidadId, maxOf(objetivo, minimo), titulo, texto)
    }

    fun programarPrueba(context: Context, oportunidadId: Long, titulo: String, texto: String, segundos: Int = 10) =
        programar(context, oportunidadId, System.currentTimeMillis() + segundos * 1000L, titulo, texto)

    private fun programar(context: Context, oportunidadId: Long, enMillis: Long, titulo: String, texto: String) {
        val intent = Intent(context, RecordatorioReceiver::class.java).apply {
            putExtra(EXTRA_ID, oportunidadId)
            putExtra(EXTRA_TITULO, titulo)
            putExtra(EXTRA_TEXTO, texto)
        }
        val pending = PendingIntent.getBroadcast(
            context, oportunidadId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmas = context.getSystemService(AlarmManager::class.java)
        alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, enMillis, pending)
    }

    @SuppressLint("MissingPermission")
    fun mostrar(context: Context, oportunidadId: Long, titulo: String, texto: String) {
        if (!tienePermiso(context)) return
        crearCanal(context)
        val abrirApp = PendingIntent.getActivity(
            context, oportunidadId.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_ID, oportunidadId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificacion = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(oportunidadId.toInt(), notificacion)
        } catch (e: SecurityException) {
        }
    }
}
