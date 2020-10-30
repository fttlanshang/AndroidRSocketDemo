package com.example.rsocketdemoapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rsocketdemoapp.adapter.MessagesRecyclerViewAdapter
import com.example.rsocketdemoapp.data.Message
import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.WebSockets
import io.ktor.util.KtorExperimentalAPI
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.core.RSocketClientSupport
import io.rsocket.kotlin.core.rSocket
import io.rsocket.kotlin.keepalive.KeepAlive
import io.rsocket.kotlin.payload.Payload
import io.rsocket.kotlin.payload.PayloadMimeType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlin.time.ExperimentalTime
import kotlin.time.minutes
import kotlin.time.seconds

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MessagesRecyclerViewAdapter
    private var messagesList: List<Message> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        setupRecyclerView()
        setupSpinnerView()
    }

    @ExperimentalTime
    override fun onResume() {
        super.onResume()
        runBlocking {
            retrieveMessages()
        }
    }

    private fun setupSpinnerView() {
        val spinner: Spinner = findViewById(R.id.author_spinner)
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
                this,
                R.array.authors_array,
                android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner.adapter = adapter
        }
        spinner.onItemSelectedListener = this
    }

    @ExperimentalTime
    private suspend fun retrieveMessages(author: String = "linustorvalds") {
        //create ktor client
        val client = HttpClient() {
            install(WebSockets){}
            install(RSocketClientSupport) {
                keepAlive = KeepAlive(
                        interval = 60.seconds,
                        maxLifetime = 2.minutes
                )
                payloadMimeType = PayloadMimeType(
                        data = "application/json",
                        metadata = "message/x.rsocket.routing.v0"
                )
            }
        }

        //connect to some url
        val rSocket: RSocket = client.rSocket("ws://10.0.2.2:8080/tweetsocket")
        println("Connected to web socket")

        //request stream
        val requestPayload = Payload(
                data = "{\"author\" : \"linustorvalds\"}",
                metadata = "tweets.by.author".length.toChar() + "tweets.by.author"
        )
        val stream: Flow<Payload> = rSocket.requestStream(requestPayload)

        //take 5 values and print response
        stream.take(5).collect { payload: Payload ->
            println(payload.data.readText())
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.messages)
        recyclerView.layoutManager = LinearLayoutManager(applicationContext)

        adapter = MessagesRecyclerViewAdapter()
        recyclerView.adapter = adapter
        adapter.setDataSource(messagesList)
    }

    @ExperimentalTime
    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        // An item was selected. You can retrieve the selected item using
        val author = parent.getItemAtPosition(pos)

    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        // Another interface callback
    }
}