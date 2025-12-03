package cz.dzubera.callwarden.utils

object Config {

    const val API =
        "https://script.google.com/macros/s/AKfycbwlJTe91xtCLLSzzSFQ1MtHlMI3FMu7MoSEPpin9MT43FqGqp4BN140J-1AyXXX2y2n/exec"
    var signedOut = false

    const val PROJECTS_URL = "https://cron.ramisys.cz/mobil/projects/"
    const val SEND_INCOMING_CALL_URL = "https://cron.ramisys.cz/mobil/ring/"
    const val SEND_FIREBASE_TOKEN = "https://cron.ramisys.cz/mobil/firebase/"
    const val SEND_VERSION = "https://cron.ramisys.cz/mobil/version/"
    const val CALL_URL = "https://cron.ramisys.cz/mobil/call/"
}
