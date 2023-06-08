package dev.work.client_mini_project

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.util.Random
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var connectButton: Button
    private lateinit var logTextView: TextView
    private lateinit var logEditText: EditText

    private val host = "100.72.0.228" // Replace with the actual server IP address
    private val port = 12345 // Replace with the actual server port

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.setStatusBarColor(this.getResources().getColor(R.color.transparent))
        connectButton = findViewById(R.id.button_connect)
        logTextView = findViewById(R.id.text_log)
        logEditText = findViewById(R.id.searchEditText)

        connectButton.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                connectToServer()
            }
        }
    }

    private suspend fun connectToServer() {
        log("Connecting to server: $host:$port")

            try {
                val clientSocket = Socket(host, port)

                log("Connected to server: $host:$port")

                val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                val writer = OutputStreamWriter(clientSocket.getOutputStream())

                // Perform Diffie-Hellman key exchange
                val p = 14327 // Prime number
                val g = 100 // Generator

                val private_key = Random().nextInt(p - 1) + 1
                val public_key = pow(g.toDouble(), private_key.toDouble(), p.toDouble()).toInt()

                log("Private key: $private_key")
                log("Public key: $public_key")

                // Send the public key to the server
                writer.write(public_key.toString() + "\n")
                writer.flush()

                // Receive the server's public key
                val their_public_key = reader.readLine().toInt()
                log("Server's public key: $their_public_key")

                // Compute the shared secret
                val shared_secret = pow(their_public_key.toDouble(), private_key.toDouble(), p.toDouble()).toInt()
                log("Shared secret: $shared_secret")

                // Send a message using the shared secret
                val message = logEditText.text.toString()
                val encryptedMessage = encryptMessage(message, shared_secret)
                log("Sending message: $message")
                writer.write(encryptedMessage + "\n")
                writer.flush()

                // Receive and decrypt the server's response
                val response = reader.readLine()
                val decryptedResponse = decryptMessage(response, shared_secret)
                log("Received response: $decryptedResponse")

                delay(2000)
                // Close the connection
                clientSocket.close()
                log("Connection closed")
            } catch (e: Exception) {
                log("Error: ${e.message}")
            }

    }

    private fun log(message: String) {
        runOnUiThread {
            logTextView.append("$message\n")
        }
    }

    private fun pow(base: Double, exponent: Double, modulus: Double): Double {
        var result = 1.0
        var b = base
        var e = exponent

        while (e > 0) {
            if (e % 2 == 1.0) {
                result = (result * b) % modulus
            }
            e /= 2
            b = (b * b) % modulus
        }

        return result
    }

    private fun encryptMessage(message: String, key: Int): String {
        val encryptedMessage = StringBuilder()
        for (i in message.indices) {
            val encryptedChar = message[i].toInt() xor key
            encryptedMessage.append(encryptedChar.toChar())
        }
        return encryptedMessage.toString()
    }

    private fun decryptMessage(encryptedMessage: String, key: Int): String {
        return encryptMessage(encryptedMessage, key) // XOR encryption is symmetric
    }
}