package et.nearbyconnectionstest

import android.app.NotificationManager
import android.content.ContentValues.TAG
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.annotation.NonNull
import android.support.v4.app.NotificationCompat
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.Strategy.P2P_STAR
import com.google.android.gms.tasks.OnSuccessListener
import android.support.v4.util.SimpleArrayMap
import com.google.android.gms.nearby.connection.Payload






class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val codeName = CodenameGenerator.generate()
    private val STRATEGY = Strategy.P2P_STAR
    private val TAG = "EtNearby"

    private val incomingPayloads = SimpleArrayMap<Long, NotificationCompat.Builder>()
    private val outgoingPayloads = SimpleArrayMap<Long, NotificationCompat.Builder>()

    private var mNotificationManager : NotificationManager ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }


    override fun onClick(v: View?) {
        when (v) {
            bt_advertise_star -> advertiseDevices()
            bt_discover_star -> discoverDecices()
        }
    }

    private fun discoverDecices() {
        Nearby.getConnectionsClient(applicationContext).startDiscovery(
                packageName,
                mEndpointDiscoveryCallback,
                DiscoveryOptions(STRATEGY))
                .addOnSuccessListener {
                    // We're discovering!
                }
                .addOnFailureListener(
                        object : OnFailureListener {
                            override fun onFailure(e: Exception) {
                                // We were unable to start discovering.
                            }
                        })
    }

    private fun advertiseDevices() {
        Nearby.getConnectionsClient(this).startAdvertising(
                codeName,
                packageName,
                mConnectionLifecycleCallback,
                AdvertisingOptions(STRATEGY))
                .addOnSuccessListener {
                    // We're advertising!
                }
                .addOnFailureListener(
                        object : OnFailureListener {
                            override fun onFailure(e: Exception) {
                                // We were unable to start advertising.
                            }
                        })
    }

    private fun sendPayload(endpointId: String, payload: Payload) {
        if (payload.type == Payload.Type.BYTES) {
            // No need to track progress for bytes.
            return
        }

        // Build and start showing the notification.
//        val notification = buildNotification(payload, false /*isIncoming*/)
//        mNotificationManager?.notify(payload.id.toInt(), notification?.build())
//
//        // Add it to the tracking list so we can update it.
//        outgoingPayloads.put(payload.id, notification)
    }

//    private fun buildNotification(payload: Payload, isIncoming: Boolean): NotificationCompat.Builder? {
//        var notification = NotificationCompat.Builder(this)
//        notification.setContentTitle(if (isIncoming) "Receiving..."  "Sending...")
//        int size = payload.getSize();
//        boolean indeterminate = false;
//        if (size == -1) {
//            // This is a stream payload, so we don't know the size ahead of time.
//            size = 100;
//            indeterminate = true;
//        }
//        notification.setProgress(size, 0, indeterminate);
//        return notification;
//    }


    private val mConnectionLifecycleCallback = object : ConnectionLifecycleCallback() {

        override fun onConnectionInitiated(
                endpointId: String, connectionInfo: ConnectionInfo) {
            // Automatically accept the connection on both sides.
            Nearby.getConnectionsClient(applicationContext).acceptConnection(endpointId, mPayloadCallback)
            Log.d(TAG, "mConnectionLifecycleCallback : onConnectionInitiated")
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                }
            }// We're connected! Can now start sending and receiving data.
            // The connection was rejected by one or both sides.
            // The connection broke before it was able to be accepted.
            Log.d(TAG, "mConnectionLifecycleCallback : onConnectionResult -> connectionsStatusCode:" + result.status.statusCode + " id:" + endpointId)
        }

        override fun onDisconnected(endpointId: String) {
            // We've been disconnected from this endpoint. No more data can be
            // sent or received.
            Log.d(TAG, "mConnectionLifecycleCallback : onDisconnected -> " + endpointId)
        }
    }

    private val mPayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(p0: String?, p1: Payload?) {
            Log.d(TAG, "mPayloadCallback : onPayloadReceived -> " + p0 + " : " + p1.toString())
        }

        override fun onPayloadTransferUpdate(p0: String?, p1: PayloadTransferUpdate?) {
            Log.d(TAG, "mPayloadCallback : onPayloadTransferUpdate -> " + p0 + " : " + p1.toString())
        }

    }

    private val mEndpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, discoveredEndpointInfo: DiscoveredEndpointInfo) {
            // An endpoint was found!
            Log.d(TAG, "mEndpointDiscoveryCallback : onEndpointFound -> " + endpointId + " : " + discoveredEndpointInfo.endpointName)
            Nearby.getConnectionsClient(applicationContext).requestConnection(
                    codeName,
                    endpointId,
                    mConnectionLifecycleCallback)
                    .addOnSuccessListener(
                             {
                                 // We successfully requested a connection. Now both sides
                                 // must accept before the connection is established.
                                 Log.d(TAG, "mEndpointDiscoveryCallback : OnSuccessListener -> succeed")
                            })
                    .addOnFailureListener(
                            {
                                // Nearby Connections failed to request the connection.
                                Log.d(TAG, "mEndpointDiscoveryCallback : OnFailureListener -> failed")
                            });
        }

        override fun onEndpointLost(endpointId: String) {
            // A previously discovered endpoint has gone away.
            Log.d(TAG, "mEndpointDiscoveryCallback : onEndpointLost -> " + endpointId)
        }
    }
}
