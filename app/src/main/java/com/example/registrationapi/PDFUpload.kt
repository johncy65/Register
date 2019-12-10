package com.example.registrationapi

import android.net.Uri
import android.os.AsyncTask
import android.widget.Toast
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException

class PDFUpload(var context : MainActivity): AsyncTask<Uri, Void, String>() {
    override fun doInBackground(vararg params: Uri?): String {
        val client = OkHttpClient()
        val bytearay = context.contentResolver.openInputStream(params[0])?.buffered()?.use { it.readBytes() }
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("doc", "${System.currentTimeMillis()}.pdf",
                bytearay!!.toRequestBody("application/pdf".toMediaTypeOrNull(), 0, bytearay.size)
            )
            .build()

        val request = Request.Builder()
            .url("http://104.155.168.205/api/upload/pdf")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            return response.body!!.string()
        }
        return ""

    }
    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
    }
}
