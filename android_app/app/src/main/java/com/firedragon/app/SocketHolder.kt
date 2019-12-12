package com.firedragon.app

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket

/**
 * Wrapper for java socket formation and communication
 * Similar class is implemented in server
 * Together they block abstraction from TCP segments
 *
 * i.e. When send(data) is called at client,
 * the data is padded with headers
 * so that it can be extracted as is when the server calls receive()
 *
 * and vice versa
 *
 * Ex: data        = hello\n
 * send(data) is called at client
 * receive() is called at server
 * server gets hello\n irrespective of what else exists in server receive buffer
 */
object SocketHolder {
    // Must be same for client and server
    // Cannot be a digit
    // Must not be an empty string
    // Cannot be empty string or char
    private const val HEAD_BODY_DELIMITER = '\n'

    // Max number of digits in length of body received
    // Must be same for client and server
    private const val R_BODY_LIMIT = 10

    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null
    private var bypassMode = false

    fun connect(ipAddress: String, port: Int) {
        // Create a new socket
        socket = Socket(ipAddress, port)
        // Keep track of it's input and output streams
        writer = BufferedWriter(OutputStreamWriter(socket!!.getOutputStream()))
        reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
    }

    fun bypass() {
        bypassMode = true
    }

    /**
     * Takes the param and writes it and then flushes it using the BufferedOutputStream
     * Packet Format = body_length + (delimiter) + body
     */
    fun send(body: String): Boolean {
        if (bypassMode) {
            return true
        }
        val dataLength = body.length
        val packetBuilder = StringBuilder()
        packetBuilder.append(dataLength.toString())
            .append(HEAD_BODY_DELIMITER)
            .append(body)
        writer!!.write(packetBuilder.toString())
        writer!!.flush()
        return true
    }

    /**
     * Expected Packet Format = body_length + (delimiter) + body + '\n'
     * Returns body
     */
    fun receive(): String? {
        if (bypassMode) {
            return ""
        }
        var noDigits = 0
        val bodyLengthBuilder = StringBuilder()
        // Handles (nothing)(delimiter) case (not req if format is proper)
        bodyLengthBuilder.append('0')
        // Finds body length
        var letter: Char
        while (true) {
            letter = reader!!.read().toChar()
            if (letter == HEAD_BODY_DELIMITER)
                break
            bodyLengthBuilder.append(letter)
            // Limits body size
            noDigits++
            if (noDigits > R_BODY_LIMIT)
                return null
        }
        var bodyLength = Integer.parseInt(bodyLengthBuilder.toString())
        // Extracts the body
        val bodyBuilder = StringBuilder()
        var chunk: String
        while (bodyLength > 0) {
            // This will at most read total body
            chunk = reader!!.readLine()
            bodyBuilder.append(chunk).append('\n')
            // + 1 is for the extra \n which is silently removed in readline()
            bodyLength -= chunk.length + 1
        }
        val bodyWithNewline = bodyBuilder.toString()
        // Removes the last newline appended for reading purpose

        return bodyWithNewline.substring(0, bodyWithNewline.length - 1)
    }

    /**
     * Closes the socket if not already closed
     */
    fun close() {
        if (bypassMode) {
            return
        }
        // If the socket is not already closed close it
        if (!socket!!.isClosed)
            socket!!.close()
    }
}
