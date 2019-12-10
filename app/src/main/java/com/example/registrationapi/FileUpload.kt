package com.example.registrationapi

import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import android.widget.Toast
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import android.provider.MediaStore
import android.graphics.Bitmap
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream


class FileUpload(var context : MainActivity): AsyncTask<Uri, Void, String>(){
    val client = OkHttpClient()
    override fun doInBackground(vararg params: Uri?): String {
        var requestBody : MultipartBody? = null
        if (context.camera){
        requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "image", "${System.currentTimeMillis()}.jpg",
                File(params[0]?.path).asRequestBody("application/octet-stream".toMediaTypeOrNull())
            )
            .build()
        }else{
            val bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), params[0])
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val bytearay = stream.toByteArray()
            requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image", "${System.currentTimeMillis()}.jpg",
                    bytearay.toRequestBody("image/*jpg".toMediaTypeOrNull(), 0, bytearay.size)
                )
                .build()
        }

        val request = Request.Builder()
            .url("http://104.155.168.205/api/upload")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            return response.body!!.string()
        }

        return "Error"
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
    }


}