package org.metafetish.buttplug.server.bluetooth;

import android.support.annotation.NonNull;

import org.metafetish.buttplug.core.ButtplugEventHandler;
import org.metafetish.buttplug.core.ButtplugMessage;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.UUID;

public interface IBluetoothDeviceInterface {
    String getName();

    ListenableFuture<ButtplugMessage> writeValue(long aMsgId, UUID aCharacteristicIndex, byte[]
            aValue);

    ListenableFuture<ButtplugMessage> writeValue(long aMsgId, UUID aCharacteristicIndex, byte[]
            aValue, boolean aWriteWithResponse);

    String getAddress();

    @NonNull
    ButtplugEventHandler getDeviceRemoved();

    void disconnect();
}
