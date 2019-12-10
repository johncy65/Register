package com.example.registrationapi

import android.content.Context
import android.os.AsyncTask
import android.widget.Toast
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

class HTTPTask(var context: Context): AsyncTask<String, Void, String>() {
    override fun doInBackground(vararg params: String?): String {
        if (params[0] == "post"){
            val param = params[1]
            val url = URL("http://104.155.168.205/api/register")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput  = true
            val outputStream = connection.outputStream
            if (param != null) {
                outputStream.write(param.toByteArray())
            }
            outputStream.flush()
            outputStream.close()
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                try {
                    val server_response = connection.inputStream.bufferedReader().readText()
                    return server_response
                }catch (e: Exception){
                    e.printStackTrace()
                }finally {
                    connection.disconnect()
                }
            } else {
                println("ERROR ${connection.responseCode}")
            }

            return ""

        }else{
            val url = URL("http://104.155.168.205/api/user/johncy65")
            val httpClient = url.openConnection() as HttpURLConnection
            if (httpClient.responseCode == HttpURLConnection.HTTP_OK) {
                try {
                    val server_response = httpClient.inputStream.bufferedReader().readText()
                    return server_response
                }catch (e: Exception){
                    e.printStackTrace()
                }finally {
                    httpClient.disconnect()
                }
            } else {
                println("ERROR ${httpClient.responseCode}")
            }

            return ""
        }
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
    }


}