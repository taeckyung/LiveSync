package com.terry00123.livesync

import android.util.Log
import java.io.File

class BLE_DB {
    val dirPath = "sdcard/LiveSync"
    val filePath = dirPath + "/BLE_data.txt"
    val scanned_name = dirPath + "/BLE_names.txt"
    val dirFile = File(filePath)
    val dirNameFile = File(scanned_name)

    fun initialize(){
        if (!File(dirPath).exists()) {
            File(dirPath).mkdir()
        }
        val NameOutput = dirNameFile.outputStream()
        val fileOutput = dirFile.outputStream()
        NameOutput.write("".toByteArray())
        fileOutput.write("".toByteArray())

        NameOutput.close()
        fileOutput.close()
    }

    fun find_max_distance(): Double{
        var max = 0.0
        if(BLE_DB().DBisEmpty())
        {
            return max
        }

        val RSSI = BLE_DB().get_Rssis()

        for (rssi in RSSI)
        {
            val dis = Bluetooth().rssiTodis(rssi)
            if(dis >= max)
            {
                max = dis
            }
        }
        if(max >= 50)
        {
            max = find_max_distance()
        }

        return max
    }
    fun max_distance_of(Contained_string: String): Double{
        var max = 0.0
        if(DBisEmpty())
        {
            return max
        }
        val names = get_names()
        val rssis = get_Rssis()

        for(i in 0..(names.size-1))
        {
            if(names[i].contains(Contained_string) && max <= Bluetooth().rssiTodis(rssis[i]))
            {
                max = Bluetooth().rssiTodis(rssis[i])
            }
        }

        return max
    }

    fun is_Synced(target_addr: String): Boolean{
        val names = get_names()
        val addrs = get_Addrs()
        for(i in 0..(names.size-1))
        {
            if(target_addr == addrs[i])
            {
                if(names[i].contains("Synced"))
                {
                    return true
                }
                return false
            }
        }
        //invalid target_addr case
        return false
    }

    fun change_Synced(target_addr: String){
        val index = get_index_from_addr(target_addr)
        val names = get_names()
        change_Name(target_addr, "[Synced]${names[index]}")
    }
    fun change_Name(target_addr: String, name: String){
        val names = get_names()
        val addrs = get_Addrs()
        var result=""
        for(i in 0..(names.size-1))
        {
            if(target_addr != addrs[i])
            {
                result = result + "${names[i]}\n"
            }
            else
            {
                result = result + "$name\n"
            }
        }

        dirNameFile.writeText(result)
    }


    fun get_names(): Array<String>{
        val DBtext = dirNameFile.readText()
        val DB = DBtext.split("\n").toTypedArray()
        val result = Array<String>(DB.size-1){ i -> ""}
        for(i in 0..(DB.size-2))
        {
            result[i] = DB[i]
        }

        return result
    }
    fun get_Addrs(): Array<String>{
        val DBtext = dirFile.readText()
        val DB = DBtext.split(" ", "\n")
        if(DBisEmpty())
        {
            return arrayOf("")
        }

        var result = Array<String>(DB.size /2 +1){ i -> ""}

        for(i in 0 ..(DB.size / 2-1) )
        {
            result[i] = DB[2 * i]
        }
        return result
    }
    fun get_Rssis(): Array<Short>{
        val DBtext = dirFile.readText()
        val DB = DBtext.split(" ", "\n")
        if(DBisEmpty())
        {
            return arrayOf(0)
        }

        var result = Array<Short>(DB.size /2 +1){ i -> 0}

        for(i in 0 ..(DB.size / 2-1) )
        {
            result[i] = DB[2 * i +1].toShort()
        }
        return result
    }

    fun append(target: Int, text: String?) {
        val Real_Text = text + "\n"
        if(target == 1)
        {
            dirFile.appendText(Real_Text)
        }
        else if(target == 2)
        {
            dirNameFile.appendText(Real_Text)
        }
    }
    fun update_rssi(addr: String, rssi: Short){
        val DBtext=dirFile.readText()
        val DB = DBtext.split(" ", "\n")

        var result_text_DB = ""

        for(i in 0..((DB.size / 2)-1) )
        {
            if( DB[i * 2] != addr)
            {
                result_text_DB = result_text_DB +DB[i*2] +" "+ DB[i*2+1]+"\n"
            }
            else
            {
                result_text_DB = result_text_DB +DB[i*2] +" $rssi\n"
            }
        }

        dirFile.writeText(result_text_DB)
    }
    fun delete(index: Int){
        val DBtext=dirFile.readText()
        val DB = DBtext.split(" ", "\n")

        val Nametext = dirNameFile.readText()
        val Names = Nametext.split(" ", "\n")
        var result_text_DB = ""
        var result_text_name = ""

        for(i in 0..((DB.size / 2)-1) )
        {
            if( i != index)
            {
                result_text_DB = result_text_DB +DB[i*2] +" "+ DB[i*2+1]+"\n"
                result_text_name = "${Names[i]}\n"
            }
        }

        dirFile.writeText(result_text_DB)
        dirNameFile.writeText(result_text_name)
    }

    fun duplicate(addr: String?): Boolean {
        if(DBisEmpty())
        {
            return false
        }
        val ADDRS = get_Addrs()
        for(ADDR in ADDRS)
        {
            Log.e("  contains", "ADDR: $ADDR, addr: $addr, duplicated?: ${addr == ADDR}")
            if(addr == ADDR)
            {
                return true
            }
        }
        return false
    }


    private fun DBisEmpty(): Boolean{
        val DBtext = dirFile.readText()
        val DB = DBtext.split(" ", "\n")
        return (DB[0]=="")
    }
    private fun get_index_from_addr(addr: String): Int{
        val addrs = get_Addrs()
        for(i in (0..addrs.size-1))
        {
            if(addr == addrs[i])
            {
                return i
            }
        }
        return -1
    }

}