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
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import android.os.ParcelFileDescriptor
import android.app.Activity
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.Charset


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val codeName = CodenameGenerator.generate()
    private val STRATEGY = Strategy.P2P_STAR
    private val TAG = "EtNearby"
    private val ENDPOINT_ID_EXTRA = "endpointId"
    private val READ_REQUEST_CODE = 42

    //    private val incomingPayloads = SimpleArrayMap<Long, NotificationCompat.Builder>()
    private val outgoingPayloads = SimpleArrayMap<Long, NotificationCompat.Builder>()

    private val incomingPayloads = SimpleArrayMap<String, Payload>()
    private val filePayloadFilenames = SimpleArrayMap<String, String>()
    private var idBuf : String ?= null

    private var mNotificationManager: NotificationManager? = null

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
        val notification = buildNotification(payload, false /*isIncoming*/)
        mNotificationManager?.notify(payload.id.toInt(), notification?.build())

        // Add it to the tracking list so we can update it.
        outgoingPayloads.put(payload.id, notification)
    }

    private fun buildNotification(payload: Payload, isIncoming: Boolean): NotificationCompat.Builder? {
        var notification = NotificationCompat.Builder(this)
        notification.setContentTitle(if (isIncoming) "Receiving..." else "Sending...")
                .setSmallIcon(R.mipmap.ic_launcher)
        var size = payload.asFile()?.size
        var indeterminate = false;
        if (size == -1L) {
            // This is a stream payload, so we don't know the size ahead of time.
            size = 100;
            indeterminate = true;
        }
        notification.setProgress(size?.toInt()!!, 0, indeterminate);
        return notification;
    }


    /**
     * Fires an intent to spin up the file chooser UI and select an image for
     * sending to endpointId.
     */
    private fun showImageChooser(endpointId: String) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        intent.putExtra(ENDPOINT_ID_EXTRA, endpointId)
        startActivityForResult(intent, READ_REQUEST_CODE)
        idBuf = endpointId
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                var endpointId = resultData.getStringExtra(ENDPOINT_ID_EXTRA)
                if (endpointId == null || "".equals(endpointId)) {
                    endpointId = idBuf
                }
                // The URI of the file selected by the user.
                val uri = resultData.data

                // Open the ParcelFileDescriptor for this URI with read access.
                val pfd = contentResolver.openFileDescriptor(uri!!, "r")
                val filePayload = Payload.fromFile(pfd!!)

                // Construct a simple message mapping the ID of the file payload to the desired filename.
                val payloadFilenameMessage = filePayload.id.toString() + ":" + uri.lastPathSegment

//                // Send this message as a bytes payload.
//                sendPayload(endpointId, Payload.fromBytes(payloadFilenameMessage.toByteArray(charset("UTF-8"))))
//
//                // Finally, send the file payload.
//                sendPayload(endpointId, filePayload)

                // Send this message as a bytes payload.
                Nearby.getConnectionsClient(applicationContext).sendPayload(
                        endpointId, Payload.fromBytes(payloadFilenameMessage.toByteArray(charset("UTF-8"))));

                // Finally, send the file payload.
                Nearby.getConnectionsClient(applicationContext).sendPayload(endpointId, filePayload);
            }
        }
    }


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
                    Log.d(TAG, "SEND DATA")
//                    var array : ByteArray = byteArrayOf('h'.toByte())
//                    var payload = Payload.fromBytes(array)
//                    sendPayload(endpointId, payload)
//                    showImageChooser(endpointId)
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
        override fun onPayloadReceived(payloadId: String?, payload: Payload?) {
            Log.d(TAG, "mPayloadCallback : onPayloadReceived -> " + payloadId + " : " + payload.toString())
            //            if (payload?.getType() === Payload.Type.BYTES) {
            //                // No need to track progress for bytes.
            //                return
            //            }
            //
            //            // Build and start showing the notification.
            //            val notification = buildNotification(payload!!, true /*isIncoming*/)
            //            mNotificationManager?.notify(payload.getId() as Int, notification?.build())
            //
            //            // Add it to the tracking list so we can update it.
            //            incomingPayloads.put(payload.getId(), notification)

            if (payload?.getType() === Payload.Type.BYTES) {
                val payloadFilenameMessage = String(payload.asBytes()!!, Charset.forName("UTF-8"))
                if (payloadId != null) {
                    addPayloadFilename(payloadFilenameMessage, payloadId)
                }
            } else if (payload?.getType() === Payload.Type.FILE) {
                // Add this to our tracking map, so that we can retrieve the payload later.
                incomingPayloads.put(payloadId, payload)
            }
        }

        /**
         * Extracts the payloadId and filename from the message and stores it in the
         * filePayloadFilenames map. The format is payloadId:filename.
         */
        private fun addPayloadFilename(payloadFilenameMessage : String, payloadId : String)
        {
            val colonIndex = payloadFilenameMessage.indexOf (':')
//            val payloadId = payloadFilenameMessage.substring (0, colonIndex)
            val filename = payloadFilenameMessage.substring (colonIndex + 1)
            filePayloadFilenames.put(payloadId, filename)
        }

        override fun onPayloadTransferUpdate(payloadId: String?, update: PayloadTransferUpdate?) {
            Log.d(TAG, "mPayloadCallback : onPayloadTransferUpdate -> " + payloadId + " : " + update.toString())
//            var notification: NotificationCompat.Builder? = null
//            if (incomingPayloads.containsKey(payloadId)) {
//                notification = incomingPayloads.get(payloadId)
//                if (update?.getStatus() !== PayloadTransferUpdate.Status.IN_PROGRESS) {
//                    // This is the last update, so we no longer need to keep track of this notification.
//                    incomingPayloads.remove(payloadId)
//                }
//            } else if (outgoingPayloads.containsKey(payloadId)) {
//                notification = outgoingPayloads.get(payloadId)
//                if (update?.getStatus() !== PayloadTransferUpdate.Status.IN_PROGRESS) {
//                    // This is the last update, so we no longer need to keep track of this notification.
//                    outgoingPayloads.remove(payloadId)
//                }
//            }
//
//            when (update?.getStatus()) {
//                PayloadTransferUpdate.Status.IN_PROGRESS -> {
//                    val size = update?.getTotalBytes()
//                    if (size == -1L) {
//                        // This is a stream payload, so we don't need to update anything at this point.
//                        return
//                    }
//                    notification?.setProgress(size.toInt(), update.getBytesTransferred().toInt(), false /* indeterminate */)
//                }
//                PayloadTransferUpdate.Status.SUCCESS ->
//                    // SUCCESS always means that we transferred 100%.
//                    notification
//                            ?.setProgress(100, 100, false /* indeterminate */)
//                            ?.setContentText("Transfer complete!")
//                PayloadTransferUpdate.Status.FAILURE -> notification
//                        ?.setProgress(0, 0, false)
//                        ?.setContentText("Transfer failed")
//            }
//
//            mNotificationManager?.notify(payloadId as Int, notification?.build())
            if (update?.getStatus() === PayloadTransferUpdate.Status.SUCCESS) {
                val payload = incomingPayloads.remove(payloadId)
                if (payload?.type == Payload.Type.FILE) {
                    // Retrieve the filename that was received in a bytes payload.
                    val newFilename = filePayloadFilenames.remove(payloadId)

                    val payloadFile = payload.asFile()?.asJavaFile()

                    // Rename the file.
                    payloadFile!!.renameTo(File(payloadFile.parentFile, newFilename))
                    copyFile(payloadFile.absolutePath, Environment.getExternalStorageDirectory().absolutePath + File.separator + payloadFile.name)
                    Toast.makeText(applicationContext, "接收成功", Toast.LENGTH_LONG).show()
                    Log.d(TAG, "mPayloadCallback : onPayloadTransferUpdate -> " + "接收成功")
                }
            }
        }

    }

    /**
     * 复制单个文件
     * @param oldPath String 原文件路径 如：c:/fqf.txt
     * @param newPath String 复制后路径 如：f:/fqf.txt
     * @return boolean
     */
    fun copyFile(oldPath: String, newPath: String) {
        try {
            var bytesum = 0
            var byteread = 0
            val oldfile = File(oldPath)
            if (oldfile.exists()) { //文件存在时
                val inStream = FileInputStream(oldPath) //读入原文件
                val fs = FileOutputStream(newPath)
                val buffer = ByteArray(1444)
                val length: Int
//                while ((byteread = inStream.read(buffer)) != -1) {
                byteread = inStream.read(buffer)
                while (byteread != -1) {
                    bytesum += byteread //字节数 文件大小
                    println(bytesum)
                    fs.write(buffer, 0, byteread)
                    byteread = inStream.read(buffer)
                }
                inStream.close()
            }
        } catch (e: Exception) {
            println("复制单个文件操作出错")
            e.printStackTrace()

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
                            })
        }

        override fun onEndpointLost(endpointId: String) {
            // A previously discovered endpoint has gone away.
            Log.d(TAG, "mEndpointDiscoveryCallback : onEndpointLost -> " + endpointId)
        }
    }
}
