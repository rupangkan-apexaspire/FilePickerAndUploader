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

object ContentUtils {

    fun getRealPathFromURI(context: Context, uri: Uri): String? {
        when {
            // DocumentProvider
            DocumentsContract.isDocumentUri(context, uri) -> {
                when {
                    // ExternalStorageProvider
                    isExternalStorageDocument(uri) -> {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":").toTypedArray()
                        val type = split[0]
                        // This is for checking Main Memory
                        return if ("primary".equals(type, ignoreCase = true)) {
                            if (split.size > 1) {
                                Environment.getExternalStorageDirectory()
                                    .toString() + "/" + split[1]
                            } else {
                                Environment.getExternalStorageDirectory().toString() + "/"
                            }
                            // This is for checking SD Card
                        } else {
                            "storage" + "/" + docId.replace(":", "/")
                        }
                    }
                    isDownloadsDocument(uri) -> {
                        val fileName = getFilePath(context, uri)
                        if (fileName != null) {
                            return Environment.getExternalStorageDirectory()
                                .toString() + "/Download/" + fileName
                        }
                        var id = DocumentsContract.getDocumentId(uri)
                        if (id.startsWith("raw:")) {
                            id = id.replaceFirst("raw:".toRegex(), "")
                            val file = File(id)
                            if (file.exists()) return id
                        }
                        val contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"),
                            java.lang.Long.valueOf(id)
                        )
                        return getDataColumn(context, contentUri, null, null)
                    }
                    isMediaDocument(uri) -> {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":").toTypedArray()
                        val type = split[0]
                        var contentUri: Uri? = null
                        when (type) {
                            "image" -> {
                                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            }
                            "video" -> {
                                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            }
                            "audio" -> {
                                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            }
                        }
                        val selection = "_id=?"
                        val selectionArgs = arrayOf(split[1])
                        return getDataColumn(context, contentUri, selection, selectionArgs)
                    }
                }
            }
            "content".equals(uri.scheme, ignoreCase = true) -> {
                // Return the remote address
                return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(
                    context,
                    uri,
                    null,
                    null
                )
            }
            "file".equals(uri.scheme, ignoreCase = true) -> {
                return uri.path
            }
        }
        return null
    }

    fun getDataColumn(
        context: Context, uri: Uri?, selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(
            column
        )
        try {
            if (uri == null) return null
            cursor = context.contentResolver.query(
                uri, projection, selection, selectionArgs,
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }


    fun getFilePath(context: Context, uri: Uri?): String? {
        var cursor: Cursor? = null
        val projection = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME
        )
        try {
            if (uri == null) return null
            cursor = context.contentResolver.query(
                uri, projection, null, null,
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                return cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

}


class MainActivity : AppCompatActivity(), UploadRequestBody.UploadCallback {

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

    val imagePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        val path = ContentUtils.getRealPathFromURI(this, it) ?: return@registerForActivityResult
        Log.d(TAG, "Path $path ")
        val file = File(path)
        Log.d(TAG, "File: ${file.absoluteFile} ")
        lifecycleScope.launch{
//            uploadFile(file)
        }
    }

    fun openImagePicker() {
        imagePicker.launch(arrayOf("*/*"))
    }



    var sActivityLauncher = registerForActivityResult(
        StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val uri = data!!.data
            val path = uri?.let { ContentUtils.getRealPathFromURI(this, it) } ?: return@registerForActivityResult
//            val inputStream: InputStream? = contentResolver.openInputStream(uri!!)
//            val file = File(uri!!.path)
            Log.d(TAG, "Path: $path")
            lifecycleScope.launch{
//                upload();
//                uploadFile(file)
            }
//            Log.d(TAG, "Uri : $uri File : $file ")
        }
    }

    fun openFile() {
        var intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

            // Optionally, specify a URI for the file that should appear in the
            // system file picker when it loads.
//            putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        }
//        intent = Intent.createChooser(intent, "Choose a file")
//        sActivityLauncher.launch(intent)
        startActivityForResult(intent, PICK_PDF_FILE)

    }

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
//                .enqueue(object: Callback<ResponseBody> {
//                    override fun onResponse(
//                        call: Call<ResponseBody>,
//                        response: Response<ResponseBody>
//                    ) {
//                        Log.d(TAG, "onResponse: $response")
//                        Log.d(TAG, "onResponse: ${response.body()}")
//                    }
//
//                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//                        TODO("Not yet implemented")
//                        Log.d(TAG, "onFailureCall: $call")
//                        Log.d(TAG, "onFailureThrowable: $t")
//
//                    }
//                })

        } catch (e: Exception) {
            e.printStackTrace()
        }


    }

    private fun openFileDialog(view: View) {
        var data = Intent(Intent.ACTION_OPEN_DOCUMENT)
        data.type = "*/*"
        data = Intent.createChooser(data, "Choose a file")
        sActivityLauncher.launch(data)
    }

//    override fun onActivityResult(
//        requestCode: Int, resultCode: Int, resultData: Intent?) {
//        super.onActivityResult(requestCode, resultCode, resultData)
//        if (requestCode == PICK_PDF_FILE
//            && resultCode == Activity.RESULT_OK) {
//            // The result data contains a URI for the document or directory that
//            // the user selected.
//            resultData?.data?.also { uri ->
//                // Perform operations on the document using its URI.
//                dumpMetaData(uri)
////                alterDocument(uri)
//                Log.d(TAG, "Uri : $uri ")
//            }
//        }
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
                    // Technically the column stores an int, but cursor.getString()
                    // will do the conversion automatically.
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


    @Throws(IOException::class)
    private fun readTextFromUri(uri: Uri): String {
        val stringBuilder = StringBuilder()
        contentResolver.openInputStream(uri)?.use { inputStream ->
//            BufferedReader(InputStreamReader(inputStream))
//                .use { reader ->
//                var line: String? = reader.readLine()
//                while (line != null) {
//                    stringBuilder.append(line)
//                    line = reader.readLine()
//                }
//            }
        }
        return stringBuilder.toString()
    }

    private fun alterDocument(uri: Uri) {
        try {
            contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use {
                    it.write(
                        ("Overwritten at ${System.currentTimeMillis()}\n")
                            .toByteArray()
                    )
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        const val REQUEST_CODE_PICK_IMAGE = 101
        const val REQUEST_CODE_PICK_PDF = 2
    }

    private fun upload() {
        TODO("Not yet implemented")
    }

    override fun onProgressUpdate(percentage: Int) {
        TODO("Not yet implemented")
        var progress = percentage
    }

//    /storage/self/primary/Download/0ca597392de1072e3f938aa3622c87b6.jpg

//    private suspend fun uploadFile(file: File) {
//        val requestBody: RequestBody = RequestBody.create(MediaType.parse("multipart/form-data"), file)
//        val title = MultipartBody.Part.createFormData("type", file.extension)
//        val part: MultipartBody.Part =
//            MultipartBody.Part.createFormData("file", file.name, requestBody)
//        val retrofit: Retrofit = RetrofitHelper.getInstance()
//        val retrofitApi: RetrofitApi = retrofit.create(RetrofitApi::class.java)
//        val call = retrofitApi.postFile(title, part)
//        call.enqueue(object: Callback<FilePostResponse> {
//            override fun onResponse(
//                call: Call<FilePostResponse>,
//                response: Response<FilePostResponse>
//            ) {
//                TODO("Not yet implemented")
//            }
//
//            override fun onFailure(call: Call<FilePostResponse>, t: Throwable) {
//                TODO("Not yet implemented")
//            }
//
//        })
//
//    }
}


