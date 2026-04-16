package cz.dzubera.callwarden.dialer

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class CallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Probudit obrazovku a ukázat se nad zámkem
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            )
        }

        // U moderních verzí (Android 10+) je potřeba ještě požádat o odemčení klávesnice (pokud není heslo)
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager.requestDismissKeyguard(this, null)
        }

        setContent {
            val call = CallHolder.currentCall
            val service = CallHolder.inCallService
            val audioState = CallHolder.audioState.value

            val uiState = CallUiState(
                number = call?.details?.handle?.schemeSpecificPart ?: "Unknown",
                isMuted = audioState?.isMuted ?: false,
                isSpeakerOn = audioState?.route == CallAudioState.ROUTE_SPEAKER,
                callState = when(call?.state) {
                    Call.STATE_ACTIVE -> "Active"
                    Call.STATE_RINGING -> "Ringing"
                    Call.STATE_DIALING -> "Dialing"
                    else -> "Connecting"
                }
            )

            CallScreen(
                modifier = Modifier,
                s = uiState,
                onHangUp = {
                    call?.disconnect()
                    finish()
                },
                onMute = {
                    service?.setMuted(!uiState.isMuted)
                },
                onSpeaker = {
                    val newRoute = if (uiState.isSpeakerOn) {
                        CallAudioState.ROUTE_WIRED_OR_EARPIECE
                    } else {
                        CallAudioState.ROUTE_SPEAKER
                    }
                    service?.setAudioRoute(newRoute)
                },
                onHold = {
                    if (call?.state == Call.STATE_HOLDING) call.unhold() else call?.hold()
                },
                onAnswer = {
                    call?.answer(VideoProfile.STATE_AUDIO_ONLY)
                }
            )
        }
    }
}

data class CallUiState(
    val number: String = "",
    val contactName: String? = null,
    val callState: String = "Connecting",
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isOnHold: Boolean = false
)
@Composable
fun CallScreen(
    modifier: Modifier,
    s: CallUiState,
    onHangUp: () -> Unit,
    onAnswer: () -> Unit, // Přidáno pro příchozí hovory
    onMute: () -> Unit,
    onSpeaker: () -> Unit,
    onHold: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F)) // Temnější černá pro modernější vzhled
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 64.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- TOP INFO ---
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = s.contactName ?: s.number,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = s.callState.uppercase(),
                    fontSize = 14.sp,
                    letterSpacing = 2.sp,
                    color = if (s.callState == "Active") Color(0xFF4CAF50) else Color.LightGray
                )
            }

            // --- MIDDLE (Avatar/Status) ---
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(Color(0xFF252525), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Zde by mohla být ikona osoby nebo první písmeno kontaktu
                Text(s.number.take(1), color = Color.White, fontSize = 48.sp)
            }

            // --- CONTROLS ---
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Horní řada tlačítek (Mute, Hold, Speaker)
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CallActionButton(
                        label = "Mute",
                        icon = if (s.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        isActive = s.isMuted,
                        onClick = onMute
                    )

                    CallActionButton(
                        label = if (s.isOnHold) "Unhold" else "Hold",
                        icon = if (s.isOnHold) Icons.Default.PlayArrow else Icons.Default.Pause,
                        isActive = s.isOnHold,
                        onClick = onHold
                    )

                    CallActionButton(
                        label = "Speaker",
                        icon = Icons.Default.VolumeUp,
                        isActive = s.isSpeakerOn,
                        onClick = onSpeaker
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Spodní řada (End Call / Answer)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Tlačítko ukončení (vždy přítomno)
                    FloatingActionButton(
                        onClick = onHangUp,
                        containerColor = Color(0xFFE53935),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "End Call", modifier = Modifier.size(32.dp))
                    }

                    // Pokud hovor zvoní, ukaž i tlačítko přijetí
                    if (s.callState == "Ringing") {
                        Spacer(modifier = Modifier.width(48.dp))
                        FloatingActionButton(
                            onClick = onAnswer,
                            containerColor = Color(0xFF43A047),
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Answer", modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CallActionButton(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (isActive) Color.White else Color(0xFF252525),
            contentColor = if (isActive) Color.Black else Color.White,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = label)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}