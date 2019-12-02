package com.terry00123.livesync

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.util.Log
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.IntentFilter
import android.os.Handler
import kotlin.math.max
import kotlin.math.pow


class Bluetooth (private val context: Context) {
    private val mBlueToothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val handler = Handler()

    private val standardRSSI = -69
    private var oldName = ""
    private val syncedName = "SYNCHRONIZED_"

    private class BluetoothInfo(var name: String, var rssi: Short)
    private val deviceMap = mutableMapOf<String, BluetoothInfo>()

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            if (BluetoothDevice.ACTION_FOUND == action) {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        ?: return

                val deviceName = device.name
                val deviceMAC = device.address
                val deviceRSSI = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)

                if(deviceName != null) {
                    deviceMap[deviceMAC] = BluetoothInfo(deviceName, deviceRSSI)
                }

                Log.i("LiveSync_Bluetooth", "Device Name: $deviceName, MAC: $deviceMAC, RSSI: $deviceRSSI")
            }
        }
    }
    private val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)

    private val startDiscovery = object : Runnable {
        override fun run() {
            try {
                mBlueToothAdapter.startDiscovery()
            } finally {
                handler.postDelayed(this, 12000)
            }
        }
    }

    init {
        if(!mBlueToothAdapter.isEnabled) {
            mBlueToothAdapter.enable()
        }

        oldName = mBlueToothAdapter.name

        context.registerReceiver(mReceiver, filter)

        startDiscovery.run()
    }

    fun release() {
        mBlueToothAdapter.cancelDiscovery()
        handler.removeCallbacks(startDiscovery)
    }

    fun setSynchronized() {
        mBlueToothAdapter.name = syncedName + oldName

        val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300) // Maximum is 300
        }
        context.startActivity(discoverableIntent)
    }

    fun setUnSynchronized() {
        mBlueToothAdapter.name = oldName
/*
        val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0)
        }
        context.startActivity(discoverableIntent)
 */
    }

    fun getMaxPropDelay() : Int /* Milliseconds */ {
        var maxRSSI = 0
        for ((key, value) in deviceMap) {
            if (value.name.contains(syncedName)) {
                maxRSSI = max(maxRSSI, value.rssi.toInt())
            }
        }
        Log.i("LiveSync_Bluetooth", "getMaxPropDelay: ${distanceToDelay(rssiToDistance(maxRSSI.toShort()))}")
        return distanceToDelay(rssiToDistance(maxRSSI.toShort()))
    }

    private fun rssiToDistance(rssi: Short): Double  /* Meters */ {
        return 10.toDouble().pow((standardRSSI - rssi.toDouble()) / 20)
    }

    private fun distanceToDelay(dist: Double) : Int /* Milliseconds */ {
        return (dist * 1000 / 343).toInt()
    }

}