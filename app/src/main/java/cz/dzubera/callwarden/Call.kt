package cz.dzubera.callwarden

data class Call(
    val id: Long,
    val userId: String,
    val domainId: String,
    val projectId: String,
    val projectName: String,
    var type: Type,
    val direction: Direction,
    val phoneNumber: String,
    val callStarted: Long,
    val callEnded: Long,
    val callAccepted: Long?
){

    var dur: Int = -1

    enum class Direction {
        INCOMING, OUTGOING
    }

    enum class Type {
        MISSED, ACCEPTED, CALLBACK, DIALED
    }
}

