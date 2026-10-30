package com.example.medtracker

import android.app.*
import android.content.*
import android.os.Build
import java.time.*

object Reminders {
    const val CHANNEL = "medication_reminders"
    fun scheduleAll(context: Context) {
        val manager=context.getSystemService(AlarmManager::class.java)
        val store=Store(context)
        for(m in store.medicines()) {
            for((index,t) in m.times.withIndex()) {
                val parts=t.split(":"); if(parts.size!=2) continue
                val clock=try { LocalTime.of(parts[0].toInt(),parts[1].toInt()) } catch (_:Exception) { continue }
                val now=ZonedDateTime.now(); var next=now.toLocalDate().atTime(clock).atZone(ZoneId.systemDefault())
                if(!next.isAfter(now)) next=next.plusDays(1)
                val intent=Intent(context,ReminderReceiver::class.java).putExtra("medId",m.id).putExtra("slot",index)
                val pending=PendingIntent.getBroadcast(context, (m.id+":"+index).hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.toInstant().toEpochMilli(),pending)
            }
        }
    }
}
class ReminderReceiver: android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id=intent.getStringExtra("medId") ?: return
        val m=Store(context).medicines().firstOrNull { it.id==id } ?: return
        val nm=context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(Reminders.CHANNEL,"Medication reminders",NotificationManager.IMPORTANCE_HIGH))
        val open=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val warning=Store(context).safety(m)
        val body=if(warning==null) "Scheduled dose: ${m.dose}" else "Scheduled time reached. Check before taking: $warning"
        val notification=Notification.Builder(context,Reminders.CHANNEL).setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("${m.name} reminder").setContentText(body).setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(open).setAutoCancel(true).build()
        try { nm.notify((id+intent.getIntExtra("slot",0)).hashCode(),notification) } catch (_:SecurityException) { }
        Reminders.scheduleAll(context)
    }
}
class BootReceiver: android.content.BroadcastReceiver() { override fun onReceive(context: Context,intent: Intent) { Reminders.scheduleAll(context) } }
