package com.rajit.fileuploadcoroutineexample.util

import androidx.core.net.toUri
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

class FileUploadManager {

    private val _storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val _storageRef: StorageReference = _storage.reference

    private var uploadJob: Job? = null
    private var uploadTask: UploadTask? = null
    private var currentProgress = 0.00

    fun uploadFile(
        file: File,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
        fileProgressIndicator: LinearProgressIndicator
    ) {

        // Cancel any ongoing upload task
        uploadTask?.cancel()

        // Cancel any ongoing upload job
        uploadJob?.cancel()

        // Start a new coroutine for the file upload
        uploadJob = CoroutineScope(Dispatchers.IO).launch {

            try {

                // Get a reference to the file in Firebase Storage
                val fileRef: StorageReference = _storageRef.child(file.name)

                // Upload the file
                uploadTask = fileRef.putFile(file.toUri())

                // Updating the progress bar with OnProgressListener
                uploadTask?.addOnProgressListener {
                    currentProgress = (100.0 * it.bytesTransferred) / it.totalByteCount
                    fileProgressIndicator.progress = currentProgress.roundToInt()
                }

                // Wait for the upload to complete (or be cancelled)
                uploadTask?.await()

                // Notify success on the main thread
                withContext(Dispatchers.Main) {
                    onSuccess()
                }

            } catch (e: StorageException) {

                withContext(Dispatchers.Main) {
                    // Reset file upload progress indicator on catching StorageException
                    resetFileUploadProgressIndicator(fileProgressIndicator)
                    onFailure(e)
                }

            } catch (e: Exception) {

                // Check if the exception is not due to cancellation of coroutine
                if (e !is CancellationException) {
                    // Notify failure on the main thread
                    withContext(Dispatchers.Main) {
                        onFailure(e)
                    }

                }

            }

        }

    }

    fun cancelUpload(indicator: LinearProgressIndicator) {
        // Cancel the ongoing upload uploadTask
        uploadTask?.cancel()

        resetFileUploadProgressIndicator(indicator)
    }

    private fun resetFileUploadProgressIndicator(indicator: LinearProgressIndicator) {
        // Reset the currentProgress to 0
        currentProgress = 0.00

        // Reflect the currentProgress changes in Progress Indicator
        indicator.progress = currentProgress.roundToInt()
    }

}