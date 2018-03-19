package org.metafetish.buttplug.apps.bluetoothdeviceemulator;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import org.metafetish.buttplug.server.bluetooth.devices.FleshlightLaunchBluetoothInfo;
import org.metafetish.buttplug.server.util.FleshlightHelper;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();

    private TextView status;
    private ImageView icon;
    private BluetoothGattServer bluetoothGattServer;
    private BluetoothGattCharacteristic tx;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private AdvertiseSettings advertiseSettings;
    private AdvertiseCallback advertiseCallback;
    private AdvertiseData advertiseData;
    private double lastPosition = 10;
    private byte[] lastCommand;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //TODO: Deal with device orientation changes

        this.status = findViewById(R.id.status);
        this.icon = findViewById(R.id.icon);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context
                .BLUETOOTH_SERVICE);

        if (bluetoothManager != null) {
            Log.d(TAG, "openGattServer()");
            this.bluetoothGattServer = bluetoothManager.openGattServer(this, new
                    BluetoothGattServerCallback() {
                        @Override
                        public void onConnectionStateChange(BluetoothDevice device, int status, int
                                newState) {
                            super.onConnectionStateChange(device, status, newState);
                            if (newState == BluetoothProfile.STATE_CONNECTED) {
                                MainActivity.this.status.setText(R.string.gatt_connected);
                                Log.d(TAG, "onConnectionStateChange(): STATE_CONNECTED");
                                MainActivity.this.bluetoothLeAdvertiser.stopAdvertising(MainActivity
                                        .this.advertiseCallback);
                            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                MainActivity.this.status.setText(R.string.advertising_start);
                                Log.d(TAG, "onConnectionStateChange(): STATE_DISCONNECTED");
                                MainActivity.this.bluetoothLeAdvertiser.startAdvertising(
                                        MainActivity.this.advertiseSettings,
                                        MainActivity.this.advertiseData,
                                        MainActivity.this.advertiseCallback);
                            }
                        }

                        @Override
                        public void onCharacteristicWriteRequest(BluetoothDevice device, int
                                requestId,
                                                                 BluetoothGattCharacteristic
                                                                         characteristic, boolean
                                                                         preparedWrite, boolean
                                                                         responseNeeded, int offset,
                                                                 byte[] value) {
                            super.onCharacteristicWriteRequest(device, requestId, characteristic,
                                    preparedWrite, responseNeeded, offset, value);
                            if (characteristic == MainActivity.this.tx) {
                                Log.d(TAG, "onCharacteristicWriteRequest(): " + Arrays.toString
                                        (value));
                                if (MainActivity.this.lastCommand != null) {
                                    MainActivity.this.lastPosition = MainActivity.this
                                            .lastCommand[0];
                                }
                                MainActivity.this.lastCommand = value;
                                MainActivity.this.animateIcon();
                            }
                            MainActivity.this.bluetoothGattServer.sendResponse(device, requestId,
                                    BluetoothGatt.GATT_SUCCESS, offset, value);
                        }
                    });
            FleshlightLaunchBluetoothInfo deviceInfo = new FleshlightLaunchBluetoothInfo();

            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            bluetoothAdapter.setName(deviceInfo.getNames().get(0));

            BluetoothGattService service = new BluetoothGattService(
                    deviceInfo.getServices().get(0),
                    BluetoothGattService.SERVICE_TYPE_PRIMARY);
            this.tx = new BluetoothGattCharacteristic(
                    deviceInfo.getCharacteristics().get(FleshlightLaunchBluetoothInfo.Chrs.Tx
                            .ordinal()),
                    BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PERMISSION_WRITE);
            BluetoothGattCharacteristic rx = new BluetoothGattCharacteristic(
                    deviceInfo.getCharacteristics().get(FleshlightLaunchBluetoothInfo.Chrs.Rx
                            .ordinal()),
                    BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_READ);
            BluetoothGattCharacteristic cmd = new BluetoothGattCharacteristic(
                    deviceInfo.getCharacteristics().get(FleshlightLaunchBluetoothInfo.Chrs.Cmd
                            .ordinal()),
                    BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PERMISSION_WRITE);
            service.addCharacteristic(this.tx);
            service.addCharacteristic(rx);
            service.addCharacteristic(cmd);

            this.bluetoothGattServer.addService(service);

            this.bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
            settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
            this.advertiseSettings = settingsBuilder.build();

            AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
            dataBuilder.setIncludeDeviceName(true);
            this.advertiseData = dataBuilder.build();

            if (this.bluetoothLeAdvertiser != null) {
                this.status.setText(R.string.advertising_start);
                Log.d(TAG, "startAdvertising()");
                this.advertiseCallback = new AdvertiseCallback() {
                    @Override
                    public void onStartFailure(int errorCode) {
                        super.onStartFailure(errorCode);

                        if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED) {
                            MainActivity.this.status.setText(R.string.advertising_success);
                            Log.d(TAG, "onStartFailure(): Already started");
                        } else {
                            MainActivity.this.status.setText(R.string.advertising_failure);
                            MainActivity.this.bluetoothLeAdvertiser.stopAdvertising(this);
                        }
                    }

                    @Override
                    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                        super.onStartSuccess(settingsInEffect);
                        MainActivity.this.status.setText(R.string.advertising_success);
                        Log.d(TAG, "onStartSuccess()");
                    }
                };
                this.bluetoothLeAdvertiser.startAdvertising(
                        this.advertiseSettings,
                        this.advertiseData,
                        this.advertiseCallback
                );
            }
        }
    }

    private void animateIcon() {
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                double newPosition = MainActivity.this.lastCommand[0];
                double speed = MainActivity.this.lastCommand[1];
                long duration = (long) FleshlightHelper.getDuration(Math.abs(newPosition / 100 -
                        MainActivity.this.lastPosition / 100), speed / 100);
                MainActivity.this.icon.animate().setDuration(duration).translationY(MainActivity
                        .this.fromDp(newPosition));
            }
        });
    }

    private float fromDp(double dp) {
        return (float) (dp * this.getResources().getDisplayMetrics().density);
    }
}