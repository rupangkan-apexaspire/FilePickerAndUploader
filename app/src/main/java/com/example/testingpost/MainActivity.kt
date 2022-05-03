package com.example.testingpost

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.FileUtils
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.anggrayudi.storage.SimpleStorage
import com.anggrayudi.storage.callback.StorageAccessCallback
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.DocumentFileCompat.getStorageId
import com.anggrayudi.storage.file.StorageType
import com.anggrayudi.storage.file.getStorageId
import com.anggrayudi.storage.media.MediaStoreCompat
import com.example.testingpost.api.RetrofitApi
import com.example.testingpost.api.RetrofitHelper
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.File
import java.io.InputStream


class MainActivity : AppCompatActivity() {

    lateinit var selectFile: Button
    private val TAG: String = "ReposeMainActivity"
    private val storage = SimpleStorage(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        selectFile.setOnClickListener {
            storage.requestStorageAccess(storage.requestCodeStorageAccess)
        }
//        val video = MediaStoreCompat.fromFileName(applicationContext,
//            com.anggrayudi.storage.media.MediaType.DOWNLOADS, "a")
//        Log.d(TAG, "onCreate: $video ")
        selectFile = findViewById(R.id.select_file)
        selectFile.setOnClickListener{
//            openFileDialog(it)
        }

    }

    private fun setupSimpleStorage() {
        storage.storageAccessCallback = object : StorageAccessCallback {
            override fun onRootPathNotSelected(
                requestCode: Int,
                rootPath: String,
                uri: Uri,
                selectedStorageType: StorageType,
                expectedStorageType: StorageType
            ) {
                MaterialDialog(this@MainActivity)
                    .message(text = "Please select $rootPath")
                    .negativeButton(android.R.string.cancel)
                    .positiveButton {
                        val initialRoot = if (expectedStorageType.isExpected(selectedStorageType)) selectedStorageType else expectedStorageType
                        storage.requestStorageAccess(storage.requestCodeStorageAccess, initialRoot, expectedStorageType)
                    }.show()
            }

            override fun onCanceledByUser(requestCode: Int) {
                Toast.makeText(baseContext, "Canceled by user", Toast.LENGTH_SHORT).show()
            }

            override fun onExpectedStorageNotSelected(
                requestCode: Int,
                selectedFolder: DocumentFile,
                selectedStorageType: StorageType,
                expectedBasePath: String,
                expectedStorageType: StorageType
            ) {
                TODO("Not yet implemented")
            }

            override fun onStoragePermissionDenied(requestCode: Int) {
                /*
                Request runtime permissions for Manifest.permission.WRITE_EXTERNAL_STORAGE
                and Manifest.permission.READ_EXTERNAL_STORAGE
                */
            }

            override fun onRootPathPermissionGranted(requestCode: Int, root: DocumentFile) {
                Toast.makeText(baseContext, "Storage access has been granted for ${root.getStorageId(baseContext)}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        storage.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        storage.onRestoreInstanceState(savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Mandatory for Activity, but not for Fragment & ComponentActivity
        storage.onActivityResult(requestCode, resultCode, data)
    }

//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        // Mandatory for Activity, but not for Fragment & ComponentActivity
//        storage.onRequestPermissionsResult(requestCode, permissions, grantResults)
//    }

//    var sActivityLauncher = registerForActivityResult(
//        StartActivityForResult()
//    ) { result ->
//        if (result.resultCode == RESULT_OK) {
//            val data = result.data
//            val uri = data!!.data
////            val inputStream: InputStream? = contentResolver.openInputStream(uri!!)
//            val file = File(uri!!.path)
//
////
////            val mimeType: String? = intent.data?.let { returnUri ->
////                contentResolver.getType(returnUri)
////            }
//
////            intent.data?.let { returnUri ->
////                contentResolver.query(returnUri, null, null, null, null)
////            }?.use { cursor ->
////                /*
////                 * Get the column indexes of the data in the Cursor,
////                 * move to the first row in the Cursor, get the data,
////                 * and display it.
////                 */
////                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
////                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
////                cursor.moveToFirst()
////                Log.d(TAG, "$sizeIndex $nameIndex")
////            }
//
//            lifecycleScope.launch{
////                upload();
//                uploadFile(file)
//            }
//
//            Log.d(TAG, "Uri : $uri File : $file ")
//        }
//    }

    private fun upload() {
        TODO("Not yet implemented")
    }

//    /storage/self/primary/Download/0ca597392de1072e3f938aa3622c87b6.jpg

    private suspend fun uploadFile(file: File) {
        val requestBody: RequestBody = RequestBody.create(MediaType.parse("multipart/form-data"), file)
        val title = MultipartBody.Part.createFormData("type", file.extension)
        val part: MultipartBody.Part = MultipartBody.Part.createFormData("file", file.name, requestBody)
        val retrofit: Retrofit = RetrofitHelper.getInstance()
        val retrofitApi: RetrofitApi =  retrofit.create(RetrofitApi::class.java)
        val call = retrofitApi.postFile(title, part)
        call.enqueue(object: Callback<RequestBody> {
            override fun onResponse(call: Call<RequestBody>, response: Response<RequestBody>) {
                Toast.makeText(applicationContext, "Success" , Toast.LENGTH_SHORT).show()

            }

            override fun onFailure(call: Call<RequestBody>, t: Throwable) {
                Toast.makeText(applicationContext, "Error" , Toast.LENGTH_SHORT).show()

            }

        })


    }

//    private fun openFileDialog(view: View) {
//        var data = Intent(Intent.ACTION_OPEN_DOCUMENT)
//        data.type = "*/*"
//        data = Intent.createChooser(data, "Choose a file")
//        sActivityLauncher.launch(data)
//    }
}