package com.shanemaglangit.instagramdownloader

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.shanemaglangit.instagramdownloader.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set an on click listener to the download button
        binding.buttonDownload.setOnClickListener {
            val url = binding.editInput.text.toString()
            binding.editInput.setText("")

            // Check if the entered url is valid
            if (url.isEmpty()) {
                binding.layoutEditInput.error = "URL cannot be empty"
            } else if (!validateUrl(url)) {
                binding.layoutEditInput.error = "Invalid URL entered"
            } else {
                // Check the permission before downloading the image
                val permissionGranted = checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)

                if (permissionGranted) {
                    binding.layoutEditInput.error = ""
                    download(url)
                } else {
                    requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
    }

    /**
     * Function for validating if the url is in a correct format and pointing
     * into a valid Instagram post.
     */
    private fun validateUrl(url: String) =
        (url.startsWith("https://") || url.startsWith("http://")) &&
                (url.contains("instagram.com/p/") || url.contains("instagram.com/tv/"))

    /**
     * Function for downloading the media file from the given URL
     */
    private fun download(url: String) {
        uiScope.launch {
            val mediaUrl = withContext(Dispatchers.IO) { getMediaUrl(url) }

            if (mediaUrl == null) {
                // Show an error message if the media file isn't loaded properly
                Snackbar.make(
                    binding.root,
                    "Couldn't load the media file properly",
                    Snackbar.LENGTH_SHORT
                ).show()
            } else {
                // Create the download manager based of the media url
                val request = DownloadManager.Request(Uri.parse(mediaUrl)).apply {
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        UUID.randomUUID().toString()
                    )
                }

                // Perform the download
                val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                downloadManager.enqueue(request)
            }
        }
    }

    /**
     * Function for scraping the media file from the provided URL
     */
    private fun getMediaUrl(url: String): String? {
        val userAgent =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36"
        val page = Jsoup.connect(url).userAgent(userAgent).get()

        return when (page.select("meta[name=medium]").first().attr("content")) {
            "video" -> page.select("meta[property=og:video]").first().attr("content")
            "image" -> page.select("meta[property=og:image]").first().attr("content")
            else -> null
        }
    }

    /**
     * Overrides the onDestroy callback of the activity to cancel any job when the app is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}