package com.example.filetransferapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.ServerSocket
import java.net.Socket

class MainActivity : AppCompatActivity() {
    private val TCP_PORT = 5000
    private val UDP_PORT = 5001
    private lateinit var statusTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusTextView = findViewById(R.id.statusTextView)
        val startTcpButton = findViewById<Button>(R.id.startTcpServerButton)
        val startUdpButton = findViewById<Button>(R.id.startUdpServerButton)

        requestPermissions()

        startTcpButton.setOnClickListener {
            statusTextView.text = "Starting TCP server..."
            startTcpServer()
        }

        startUdpButton.setOnClickListener {
            statusTextView.text = "Starting UDP server..."
            startUdpServer()
        }
    }

    private fun requestPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            permissions.entries.forEach {
                if (!it.value) {
                    Log.e("Permission", "Permission denied: ${it.key}")
                }
            }
        }

        permissionLauncher.launch(requiredPermissions)
    }

    private fun startTcpServer() {
        Thread {
            try {
                val serverSocket = ServerSocket(TCP_PORT)
                Log.d("TCP Server", "Listening on port $TCP_PORT")

                while (true) {
                    val clientSocket: Socket = serverSocket.accept()
                    Log.d("TCP Server", "Client connected")

                    val inputStream: InputStream = clientSocket.getInputStream()
                    val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "received_file_tcp.txt")
                    val outputStream = FileOutputStream(file)

                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }

                    outputStream.close()
                    inputStream.close()
                    clientSocket.close()
                    Log.d("TCP Server", "File received successfully")
                    runOnUiThread {
                        statusTextView.text = "TCP File received: ${file.absolutePath}"
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    statusTextView.text = "TCP Server error: ${e.message}"
                }
            }
        }.start()
    }

    private fun startUdpServer() {
        Thread {
            try {
                val udpSocket = DatagramSocket(UDP_PORT)
                Log.d("UDP Server", "Listening on port $UDP_PORT")

                val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "received_file_udp.txt")
                val outputStream = FileOutputStream(file)

                val buffer = ByteArray(4096)
                val packet = DatagramPacket(buffer, buffer.size)

                while (true) {
                    udpSocket.receive(packet)
                    outputStream.write(packet.data, 0, packet.length)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    statusTextView.text = "UDP Server error: ${e.message}"
                }
            }
        }.start()
    }
}
