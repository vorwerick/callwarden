package cz.dzubera.callwarden.model

data class Call(
    val id: Long,
    val userId: String,
    val domainId: String,
    val projectId: String,
    val projectName: String,
    val duration: Int,
    val direction: Direction,
    val phoneNumber: String,
    val callStarted: Long,
    val callEnded: Long,
    val callAccepted: Long?
) {

    enum class Direction {
        INCOMING, OUTGOING
    }

    override fun toString(): String {
        return StringBuilder().append("record: ").append(phoneNumber).append(" duration: ")
            .append(duration.toString()).append(" dir: ").append(direction.name).toString()
    }
}

