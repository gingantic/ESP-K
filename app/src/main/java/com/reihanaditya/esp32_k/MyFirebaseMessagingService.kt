package com.reihanaditya.esp32_k

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private var TAG = "MyFirebaseMessagingService"

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token : $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        Log.d(TAG, "FROM : " + remoteMessage.from)

        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data : " + remoteMessage.data)
            val device_id = remoteMessage.data["device_id"].toString()

            if (remoteMessage.notification != null) {
                Log.d(TAG,"Message body : "+ remoteMessage.notification!!.body)
                NotificationHandler().warningNotification(this,remoteMessage.notification!!.title!!,remoteMessage.notification!!.body!!, device_id)
            }
        }
    }

    private fun sendNotification(body: String?) {
        val intent = Intent(this,MainActivity::class.java)
        //If set, and the activity being launched is already running in the current task,
        //then instead of launching a new instance of that activity, all of the other activities
        // on top of it will be closed and this Intent will be delivered to the (now on top)
        // old activity as a new Intent.
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra("Notification",body)

        val pendingIntent = PendingIntent.getActivity(this,0,intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE/*Flag indicating that this PendingIntent can be used only once.*/)
        val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this,"Notification")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Push Notification FCM")
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(notificationSound)
            .setContentIntent(pendingIntent)

        val notificationManager: NotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0,notificationBuilder.build())
    }
}