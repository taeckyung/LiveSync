package com.terry00123.livesync

import android.app.Application
import android.bluetooth.*
import android.widget.Toast
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.util.Log
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import java.util.*
import android.bluetooth.BluetoothDevice
import android.content.IntentFilter




class Bluetooth {
    val mBlueToothAdapter = BluetoothAdapter.getDefaultAdapter()
    val standardRSSI = -69
    val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)

    val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val deviceName = device.name
                val deviceHardwareAddress = device.address // MAC address
                val deviceRssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)

                if(deviceName != null)
                {
                    if(!BLE_DB().duplicate(deviceHardwareAddress))
                    {
                        val text = "$deviceHardwareAddress $deviceRssi"
                        BLE_DB().append(1, text)
                        BLE_DB().append(2, deviceName)
                    }
                    else{
                        BLE_DB().update_rssi(deviceHardwareAddress, deviceRssi)
                    }
                }

                Log.w("Device Name: ", "device $deviceName")
                Log.w("deviceHardwareAddress ", "hard$deviceHardwareAddress, rssi: $deviceRssi")
            }
        }
    }


    fun initialize(context: Context){
        BLE_DB().initialize()
        if(!mBlueToothAdapter.isEnabled)
        {
            Toast.makeText(context,"Please, make sure that bluetooth is enabled", Toast.LENGTH_LONG).show()
        }
        val discovery = mBlueToothAdapter.startDiscovery()
        Log.i("Discovery", "$discovery")
    }


    fun rssiTodis(rssi: Short): Double{
        return Math.pow(10.toDouble(), (standardRSSI - rssi.toDouble())/ 20)
    }
}