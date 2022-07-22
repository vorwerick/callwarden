package cz.dzubera.callwarden

data class Call(
    val id: Long,
    var type: Type,
    val direction: Direction,
    val phoneNumber: String,
    val callStarted: Long,
    val callEnded: Long,
    val callAccepted: Long?
){

    var dur: String = ""

    enum class Direction {
        INCOMING, OUTGOING
    }

    enum class Type {
        MISSED, ACCEPTED, CALLBACK, DIALED
    }
}

