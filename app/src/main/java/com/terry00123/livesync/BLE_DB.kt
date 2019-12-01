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
    fun DBisEmpty(): Boolean{
        val DBtext = dirFile.readText()
        val DB = DBtext.split(" ", "\n")
        return (DB[0]=="")
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

    fun contains(addr: String?): Boolean {
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

}