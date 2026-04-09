package cz.dzubera.callwarden

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp


class DialerActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            NumberPadDialer()
        }


    }
}

@Composable
fun NumberPadDialer() {
    val context = LocalContext.current
    var phoneNumber by remember { mutableStateOf("") }

    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Simple Dialer", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))

                Text(phoneNumber, style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(16.dp))

                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("*", "0", "#")
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { number ->
                                Button(
                                    onClick = { phoneNumber += number },
                                    modifier = Modifier.size(80.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(number, style = MaterialTheme.typography.headlineSmall)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { phoneNumber = phoneNumber.dropLast(1) },
                        modifier = Modifier.height(60.dp)
                    ) {
                        Text("⌫", style = MaterialTheme.typography.headlineSmall)
                    }

                    Button(
                        onClick = {
                            if (phoneNumber.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_CALL).apply {
                                    data = Uri.parse("tel:$phoneNumber")
                                }
                                context.startActivity(intent)
                            }
                        }, modifier = Modifier.height(60.dp)
                    ) {
                        Text("Call", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
    }
}