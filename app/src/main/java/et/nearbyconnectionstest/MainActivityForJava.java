package et.nearbyconnectionstest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.SimpleArrayMap;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * Created by William on 2018/2/12.
 */

public class MainActivityForJava extends Activity implements OnClickListener {

    private String codeName = CodeNameGeneratorForJava.generate();
    private Strategy STRATEGY = Strategy.P2P_STAR;
    private String TAG = "EtNearby";
    private String ENDPOINT_ID_EXTRA = "endpointId";
    private int READ_REQUEST_CODE = 42;
    private final SimpleArrayMap<String, Payload> incomingPayloads = new SimpleArrayMap<>();
    private final SimpleArrayMap<String, String> filePayloadFilenames = new SimpleArrayMap<>();

    private Button btAdvertiseStar, btAdvertiseStop, btDiscoverStar, btDiscoverStop;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
        initListener();
    }

    public void initView() {
        btAdvertiseStar = findViewById(R.id.bt_advertise_star);
        btAdvertiseStar.setOnClickListener(this);
        btAdvertiseStop = findViewById(R.id.bt_advertise_stop);
        btAdvertiseStop.setOnClickListener(this);
        btDiscoverStar = findViewById(R.id.bt_discover_star);
        btDiscoverStar.setOnClickListener(this);
        btDiscoverStop = findViewById(R.id.bt_discover_stop);
        btDiscoverStop.setOnClickListener(this);

    }

    public void initData() {

    }

    public void initListener() {

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_advertise_star:
                startAdvertising();
                break;
            case R.id.bt_advertise_stop:
                break;
            case R.id.bt_discover_star:
                startDiscovery();
                break;
            case R.id.bt_discover_stop:
                break;
        }
    }



    private void startAdvertising() {
        Toast.makeText(getApplicationContext(), "startAdvertising", Toast.LENGTH_LONG).show();
        Nearby.getConnectionsClient(this).startAdvertising(
                codeName,
                getPackageName(),
                mConnectionLifecycleCallback,
                new AdvertisingOptions(STRATEGY))
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unusedResult) {
                                // We're advertising!
                                Toast.makeText(getApplicationContext(), "We're advertising!", Toast.LENGTH_LONG).show();

                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // We were unable to start advertising.
                                Toast.makeText(getApplicationContext(), "We were unable to start advertising.", Toast.LENGTH_LONG).show();

                            }
                        });
    }

    private void startDiscovery() {
        Toast.makeText(getApplicationContext(), "startDiscovery", Toast.LENGTH_LONG).show();

        Nearby.getConnectionsClient(this).startDiscovery(
                getPackageName(),
                mEndpointDiscoveryCallback,
                new DiscoveryOptions(STRATEGY))
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unusedResult) {
                                // We're discovering!
                                Toast.makeText(getApplicationContext(), "We're discovering!", Toast.LENGTH_LONG).show();
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // We were unable to start discovering.
                                Toast.makeText(getApplicationContext(), "We were unable to start discovering.", Toast.LENGTH_LONG).show();
                            }
                        });
    }

    /**
     * Fires an intent to spin up the file chooser UI and select an image for
     * sending to endpointId.
     */
    private void showImageChooser(String endpointId) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(ENDPOINT_ID_EXTRA, endpointId);
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                String endpointId = resultData.getStringExtra(ENDPOINT_ID_EXTRA);

                // The URI of the file selected by the user.
                Uri uri = resultData.getData();

                // Open the ParcelFileDescriptor for this URI with read access.
                ParcelFileDescriptor pfd = null;
                try {
                    pfd = getContentResolver().openFileDescriptor(uri, "r");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                Payload filePayload = Payload.fromFile(pfd);

                // Construct a simple message mapping the ID of the file payload to the desired filename.
                String payloadFilenameMessage = filePayload.getId() + ":" + uri.getLastPathSegment();

                // Send this message as a bytes payload.
                try {
                    Nearby.getConnectionsClient(MainActivityForJava.this).sendPayload(
                            endpointId, Payload.fromBytes(payloadFilenameMessage.getBytes("UTF-8")));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                // Finally, send the file payload.
                Nearby.getConnectionsClient(MainActivityForJava.this).sendPayload(endpointId, filePayload);
            }
        }
    }

    private final EndpointDiscoveryCallback mEndpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(
                        String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo) {
                    // An endpoint was found!
                    Log.d(TAG, "mEndpointDiscoveryCallback : onEndpointFound -> " + endpointId + " : " + discoveredEndpointInfo.getEndpointName());
                    Nearby.getConnectionsClient(MainActivityForJava.this).requestConnection(
                            codeName,
                            endpointId,
                            mConnectionLifecycleCallback)
                            .addOnSuccessListener(
                                    new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unusedResult) {
                                            // We successfully requested a connection. Now both sides
                                            // must accept before the connection is established.
                                            Log.d(TAG, "mEndpointDiscoveryCallback : OnSuccessListener -> succeed");
                                        }
                                    })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Nearby Connections failed to request the connection.
                                            Log.d(TAG, "mEndpointDiscoveryCallback : OnFailureListener -> failed ：" + e.fillInStackTrace());
                                        }
                                    });
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    // A previously discovered endpoint has gone away.
                    Log.d(TAG, "mEndpointDiscoveryCallback : onEndpointLost -> " + endpointId);
                }
            };


    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {

                @Override
                public void onConnectionInitiated(
                        String endpointId, ConnectionInfo connectionInfo) {
                    // Automatically accept the connection on both sides.
                    Log.d(TAG, "mConnectionLifecycleCallback : onConnectionInitiated");
                    Toast.makeText(getApplicationContext(), "mConnectionLifecycleCallback : onConnectionInitiated", Toast.LENGTH_LONG).show();
                    Nearby.getConnectionsClient(MainActivityForJava.this).acceptConnection(endpointId, mPayloadCallback);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    Log.d(TAG, "mConnectionLifecycleCallback : onConnectionResult -> connectionsStatusCode:" + result.getStatus().getStatusCode()+ " id:" + endpointId);
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            // We're connected! Can now start sending and receiving data.
                            Log.d(TAG, "SEND DATA");
//                            sendTxtFile(endpointId);
//                            showImageChooser(endpointId);
//                            Nearby.getConnectionsClient(MainActivityForJava.this).sendPayload(endpointId, Payload.fromBytes("1111".getBytes()));
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            // The connection was rejected by one or both sides.
                            break;
                        case ConnectionsStatusCodes.STATUS_ERROR:
                            // The connection broke before it was able to be accepted.
                            break;
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    // We've been disconnected from this endpoint. No more data can be
                    // sent or received.
                    Log.d(TAG, "mConnectionLifecycleCallback : onDisconnected -> " + endpointId);
                }
            };

    private PayloadCallback mPayloadCallback = new PayloadCallback() {

        @Override
        public void onPayloadReceived(String payloadId, Payload payload) {
//            byte[] bytes = payload.asBytes();
//            String s = new String(bytes);
//            Log.d(TAG, "mPayloadCallback : onPayloadReceived -> " + payloadId + " : " + s);
//            incomingPayloads.put(payloadId, payload);

            if (payload.getType() == Payload.Type.BYTES){
                String payloadFilenameMessage = new String(payload.asBytes(), Charset.forName("UTF-8"));
                if (payloadId != null) {
                    addPayloadFilename(payloadFilenameMessage, payloadId);
                }
            } else if (payload.getType() == Payload.Type.FILE){
                // Add this to our tracking map, so that we can retrieve the payload later.
                incomingPayloads.put(payloadId, payload);
            }
        }

        @Override
        public void onPayloadTransferUpdate(String payloadId, PayloadTransferUpdate update) {
            Log.d(TAG, "mPayloadCallback : onPayloadTransferUpdate -> " + payloadId + " : " + update.toString());
            if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS){
                //seems file will come there first,so need to limit this
                if (incomingPayloads.size() > 0) {
                    Payload payload = incomingPayloads.remove(payloadId);
                    if (payload.getType() == Payload.Type.FILE){
                        // Retrieve the filename that was received in a bytes payload.
                        String newFilename = filePayloadFilenames.remove(payloadId);

                        File payloadFile = payload.asFile().asJavaFile();

                        // Rename the file.
                        payloadFile.renameTo(new File(payloadFile.getParentFile(), newFilename));
//                    copyFile(payloadFile.absolutePath, Environment.getExternalStorageDirectory().absolutePath + File.separator + payloadFile.name)
                        Toast.makeText(MainActivityForJava.this, "接收成功", Toast.LENGTH_LONG).show();
                        Log.d(TAG, "mPayloadCallback : onPayloadTransferUpdate -> " + "接收FILE成功");
                    } else if (payload.getType() == Payload.Type.BYTES) {
                        Log.d(TAG, "mPayloadCallback : onPayloadTransferUpdate -> " + "接收BYTES成功 : " + update.toString());
                    }
                }
            }
        }



        /**
         * Extracts the payloadId and filename from the message and stores it in the
         * filePayloadFilenames map. The format is payloadId:filename.
         */
        private void addPayloadFilename(String payloadFilenameMessage, String payloadId) {
            int colonIndex = payloadFilenameMessage.lastIndexOf('/');
//            val payloadId = payloadFilenameMessage.substring (0, colonIndex)
            String filename = payloadFilenameMessage.substring(colonIndex + 1);
            filePayloadFilenames.put(payloadId, filename);
        }

    };

    public void sendTxtFile (String endpointId) {

        // Open the ParcelFileDescriptor for this URI with read access.
        ParcelFileDescriptor pfd = null;
        File file = new File("/sdcard/sdl_log.txt");
        try {
            Log.d(TAG, "文件是否存在：" + file.exists());
            pfd = getContentResolver().openFileDescriptor(Uri.fromFile(file), "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Payload filePayload = Payload.fromFile(pfd);

        // Construct a simple message mapping the ID of the file payload to the desired filename.
        String payloadFilenameMessage = filePayload.getId() + ":" + file.getAbsolutePath();//uri.getLastPathSegment();

        // Send this message as a bytes payload.
        try {
            Nearby.getConnectionsClient(MainActivityForJava.this).sendPayload(
                    endpointId, Payload.fromBytes(payloadFilenameMessage.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // Finally, send the file payload.
        Nearby.getConnectionsClient(MainActivityForJava.this).sendPayload(endpointId, filePayload);
    }
//
//    /**
//     * 复制单个文件
//     * @param oldPath String 原文件路径 如：c:/fqf.txt
//     * @param newPath String 复制后路径 如：f:/fqf.txt
//     * @return boolean
//     */
//    private void copyFile(String oldPath, String newPath) throws FileNotFoundException {
//        try {
//            int  bytesum = 0;
//            int byteread = 0;
//            File oldfile = new File(oldPath);
//            if (oldfile.exists()) { //文件存在时
//                FileInputStream inStream = new FileInputStream(oldPath); //读入原文件
//                FileOutputStream fs = new FileOutputStream(newPath);
//                int buffer = ByteArray[1444];
//                int length;
////                while ((byteread = inStream.read(buffer)) != -1) {
//                        byteread = inStream.read(buffer)
//                while (byteread != -1) {
//                    bytesum += byteread //字节数 文件大小
//                    println(bytesum)
//                    fs.write(buffer, 0, byteread)
//                    byteread = inStream.read(buffer)
//                }
//                inStream.close()
//            }
//        } catch (e: Exception) {
//            Log.d(TAG,"复制单个文件操作出错")
//            e.printStackTrace()
//
//        }
//
//    }
}
