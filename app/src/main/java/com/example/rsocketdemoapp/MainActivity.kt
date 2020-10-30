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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonDecodingException
import kotlin.time.ExperimentalTime
import kotlin.time.minutes
import kotlin.time.seconds

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MessagesRecyclerViewAdapter
    private var messagesList: MutableList<Message> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        setupRecyclerView()
        setupSpinnerView()
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
        // for emulator connecting to local host, need to use 10.0.2.2
        val rSocket: RSocket = client.rSocket("ws://10.0.2.2:8080/tweetsocket")
        println("Connected to web socket")

        //request stream
        val requestPayload = Payload(
                data = "{\"author\" : \"$author\"}",
                metadata = "tweets.by.author".length.toChar() + "tweets.by.author"
        )
        val stream: Flow<Payload> = rSocket.requestStream(requestPayload)

        //take 5 values and print response
        stream.take(5).collect { payload: Payload ->
            //{"id":"d090b7de-1da7-4010-85db-2931f651e7ee","author":"Linus Torvalds","body":"Talk is cheap. Show me the code.","date":"2003-04-02"}
            try {
                val data = payload.data.readText()
                println(data)
                val message = Json(JsonConfiguration.Stable)
                    .parse(Message.serializer(), data)
                messagesList.add(message)
                println(messagesList.size)
                adapter.setDataSource(messagesList)
            } catch (exception: JsonDecodingException) {
                println(exception.toString())
            }
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
        messagesList.clear()
        GlobalScope.launch {
            retrieveMessages(author as String)
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        // Another interface callback
    }
}