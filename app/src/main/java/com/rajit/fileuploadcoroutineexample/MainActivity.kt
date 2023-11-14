package com.rajit.fileuploadcoroutineexample

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.rajit.fileuploadcoroutineexample.databinding.ActivityMainBinding
import com.rajit.fileuploadcoroutineexample.util.FileUploadManager
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var _binding: ActivityMainBinding
    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>
    private var selectedFileUri: Uri? = null
    private val fileUploadManager = FileUploadManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        _binding.uploadFileBtn.isEnabled = false

        filePickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    // Handle the selected file URI from the result data
                    val data: Intent? = result.data

                    selectedFileUri = data?.data
                    enableUploadFileHideChooseFileBtn()
                }
            }

        _binding.apply {

            chooseFileBtn.setOnClickListener {
                chooseFileUsingFilePicker()
            }

            uploadFileBtn.setOnClickListener {
                // Upload will only start if the selectedFileUri is not null
                // Might be null, when the user directly tries to upload a file without picking it
                if (selectedFileUri != null) {
                    val file = getFileFromUri(selectedFileUri!!)
                    if (file != null) {
                        uploadFile(file)
                        showCancelEnableUploadBtn()
                    }
                }
            }

            cancelUploadBtn.setOnClickListener {
                // Cancel the File Upload
                cancelUpload(_binding.fileUploadProgressIndicator)

                // Handling the visibility of ChooseFileBtn, CancelUploadBtn & UploadFileButton
                showChooseFileBtn()
            }

        }

    }

    private fun showChooseFileBtn() {
        _binding.cancelUploadBtn.visibility = View.GONE
        _binding.chooseFileBtn.visibility = View.VISIBLE
        _binding.uploadFileBtn.isEnabled = false
    }

    private fun showCancelEnableUploadBtn() {
        _binding.apply {
            cancelUploadBtn.visibility = View.VISIBLE
            uploadFileBtn.isEnabled = false
        }
    }

    private fun enableUploadFileHideChooseFileBtn() {
        _binding.apply {
            _binding.chooseFileBtn.visibility = View.GONE
            _binding.uploadFileBtn.isEnabled = true
        }
    }

    // Getting a File object from the File URI
    private fun getFileFromUri(uri: Uri): File? {
        val outputFile = File.createTempFile("temp_file", ".pdf", cacheDir)
        contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(outputFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return outputFile
    }

    private fun chooseFileUsingFilePicker() {

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }

        filePickerLauncher.launch(intent)
    }

    private fun uploadFile(file: File) {
        fileUploadManager.uploadFile(
            file,
            onSuccess = {
                Toast.makeText(applicationContext, "File Upload Success", Toast.LENGTH_SHORT).show()
                showChooseFileBtn()
            },
            onFailure = {
                Toast.makeText(
                    applicationContext,
                    "File Upload Failed!!! ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
            },
            _binding.fileUploadProgressIndicator
        )
    }

    private fun cancelUpload(lin: LinearProgressIndicator) {

        Toast.makeText(applicationContext, "Cancel Called from MainActivity", Toast.LENGTH_SHORT).show()

        // Call the cancel upload function from the FileUploadManager
        fileUploadManager.cancelUpload(lin)
    }

}