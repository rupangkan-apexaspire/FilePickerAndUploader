package com.example.testingpost

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.FileUtils
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.anggrayudi.storage.SimpleStorage
import com.anggrayudi.storage.extension.startActivityForResultSafely
import com.example.testingpost.api.RetrofitApi
import com.example.testingpost.api.RetrofitHelper
import com.example.testingpost.models.FilePostResponse
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.*
import java.io.File


class MainActivity : AppCompatActivity() {

    lateinit var selectFile: Button
    private val TAG: String = "ReposeMainActivity"
    private val storage = SimpleStorage(this)
    private val PICK_PDF_FILE = 2
    private var selectedImageUri: Uri? = null
//    val cResolver = applicationContext.contentResolver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        selectFile = findViewById(R.id.select_file)
        selectFile.setOnClickListener {
//            openFileDialog(it)
//            openFile()
//            openImagePicker()
            openPDFChooser()
        }

    }

//    val imagePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
//        val path = ContentUtils.getRealPathFromURI(this, it) ?: return@registerForActivityResult
//        Log.d(TAG, "Path $path ")
//        val file = File(path)
//        Log.d(TAG, "File: ${file.absoluteFile} ")
//        lifecycleScope.launch{
////            uploadFile(file)
//        }
//    }
//
//    fun openImagePicker() {
//        imagePicker.launch(arrayOf("*/*"))
//    }



//    var sActivityLauncher = registerForActivityResult(
//        StartActivityForResult()
//    ) { result ->
//        if (result.resultCode == RESULT_OK) {
//            val data = result.data
//            val uri = data!!.data
//            val path = uri?.let { ContentUtils.getRealPathFromURI(this, it) } ?: return@registerForActivityResult
////            val inputStream: InputStream? = contentResolver.openInputStream(uri!!)
////            val file = File(uri!!.path)
//            Log.d(TAG, "Path: $path")
//            lifecycleScope.launch{
////                upload();
////                uploadFile(file)
//            }
////            Log.d(TAG, "Uri : $uri File : $file ")
//        }
//    }

//    fun openFile() {
//        var intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
//            addCategory(Intent.CATEGORY_OPENABLE)
//            type = "*/*"
//            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
//
//            // Optionally, specify a URI for the file that should appear in the
//            // system file picker when it loads.
////            putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
//        }
//
//        startActivityForResult(intent, PICK_PDF_FILE)
//
//    }

    private fun openPDFChooser() {
//        Intent(Intent.ACTION_PICK).also {
//            it.type = "application/pdf"
////            it.type = "image/*"
////            val mimeTypes = arrayOf("image/jpeg", "image/png")
////            val mimeTypes = arrayOf("")
////            startActivityForResultSafely(REQUEST_CODE_PICK_PDF, it)
//            startActivityForResult(it, REQUEST_CODE_PICK_PDF)
//
//        }

        Intent(Intent.ACTION_PICK).also {
            it.type = "*/*"
//            val mimeTypes = arrayOf("image/jpeg", "image/png")
//            it.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            startActivityForResult(it, REQUEST_CODE_PICK_IMAGE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_PICK_IMAGE -> {
                    selectedImageUri = data?.data
                    lifecycleScope.launch{
                        uploadPDF()

                    }
                }
            }
        }
    }

    private fun uploadPDF() {
        val parcelFileDescriptor =
            contentResolver.openFileDescriptor(selectedImageUri!!, "r", null) ?: return

        val inputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)
        val file = File(cacheDir, contentResolver.getFileName(selectedImageUri!!))
        Log.d(TAG, "uploadPDF: ${file.absoluteFile}")
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)

        val retrofit: Retrofit = RetrofitHelper.getInstance()
        val retrofitApi: RetrofitApi = retrofit.create(RetrofitApi::class.java)
        val body = UploadRequestBody(file, "application/pdf")

        try {
            RetrofitApi().postFile(
                RequestBody.create("multipart/formdata".toMediaTypeOrNull(), file.extension),
                MultipartBody.Part.createFormData(
                    "file",
                    file.name,
                    body
                ))
                .enqueue(object : Callback<FilePostResponse> {
                override fun onFailure(call: Call<FilePostResponse>, t: Throwable) {
                    Log.d(TAG, "onFailureCall: $call")
                    Log.d(TAG, "onFailureThrowable: $t")
                }

                override fun onResponse(
                    call: Call<FilePostResponse>,
                    response: Response<FilePostResponse>
                ) {
                    Log.d(TAG, "onResponse: $response")
                    Log.d(TAG, "onResponse: ${response.body()}")
                }
            })

        } catch (e: Exception) {
            e.printStackTrace()
        }


    }

//    private fun openFileDialog(view: View) {
//        var data = Intent(Intent.ACTION_OPEN_DOCUMENT)
//        data.type = "*/*"
//        data = Intent.createChooser(data, "Choose a file")
//        sActivityLauncher.launch(data)
//    }

    @SuppressLint("Range")
    fun ContentResolver.getFileName(uri: Uri): String {
        val cursor: Cursor? = contentResolver.query(
            uri, null, null, null, null, null)

        var name = ""

        cursor?.use {
            if (it.moveToFirst()) {
                val displayName: String =
                    it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                Log.i(TAG, "Display Name: $displayName")
                val sizeIndex: Int = it.getColumnIndex(OpenableColumns.SIZE)
                val size: String = if (!it.isNull(sizeIndex)) {
                    it.getString(sizeIndex)
                } else {
                    "Unknown"
                }
                Log.i(TAG, "Size: $size")
                name = cursor.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }

        }

        return name
    }


//    @Throws(IOException::class)
//    private fun readTextFromUri(uri: Uri): String {
//        val stringBuilder = StringBuilder()
//        contentResolver.openInputStream(uri)?.use { inputStream ->
//            BufferedReader(InputStreamReader(inputStream))
//                .use { reader ->
//                var line: String? = reader.readLine()
//                while (line != null) {
//                    stringBuilder.append(line)
//                    line = reader.readLine()
//                }
//            }
//        }
//        return stringBuilder.toString()
//    }

//    private fun alterDocument(uri: Uri) {
//        try {
//            contentResolver.openFileDescriptor(uri, "w")?.use {
//                FileOutputStream(it.fileDescriptor).use {
//                    it.write(
//                        ("Overwritten at ${System.currentTimeMillis()}\n")
//                            .toByteArray()
//                    )
//                }
//            }
//        } catch (e: FileNotFoundException) {
//            e.printStackTrace()
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
//    }

    companion object {
        const val REQUEST_CODE_PICK_IMAGE = 101
        const val REQUEST_CODE_PICK_PDF = 2
    }


}


