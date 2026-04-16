package cz.dzubera.callwarden.dialer

import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager

class MyConnectionService : ConnectionService() {

    override fun onCreateOutgoingConnection(
        phoneAccount: PhoneAccountHandle,
        request: ConnectionRequest
    ): Connection {

        val connection = object : Connection() {

            override fun onDisconnect() {
                setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
                destroy()
            }

            override fun onAbort() {
                destroy()
            }
        }

        connection.setAddress(request.address, TelecomManager.PRESENTATION_ALLOWED)
        connection.setInitializing()


        return connection
    }
}