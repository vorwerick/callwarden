package cz.dzubera.callwarden.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import cz.dzubera.callwarden.App
import cz.dzubera.callwarden.R
import cz.dzubera.callwarden.model.Call
import java.text.SimpleDateFormat

class AnalyticsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.analytics_acitivity)

        supportActionBar?.title = "Analytiky";
        val id =App.projectStorage.getProject()!!.id
        if(id.isEmpty()){
            supportActionBar!!.subtitle = "Všechny projekty"
        } else {
            supportActionBar!!.subtitle = App.projectStorage.getProject()!!.name

        }

        val analytics1 = findViewById<TextView>(R.id.analytics_text_1)
        val analytics2 = findViewById<TextView>(R.id.analytics_text_2)
        val analytics3 = findViewById<TextView>(R.id.analytics_text_3)
        val analytics4 = findViewById<TextView>(R.id.analytics_text_4)

        val analyticsSum1 = findViewById<TextView>(R.id.analytics_text_sum1)
        val analyticsSum2 = findViewById<TextView>(R.id.analytics_text_sum2)

        val analyticsDate = findViewById<TextView>(R.id.analytics_date)



        App.cacheStorage.loadFromDatabase {
            runOnUiThread {
                var accepted = 0
                var declined = 0
                var called = 0
                var dialing = 0

                it.forEach { call ->
                    if (call.direction == Call.Direction.INCOMING) {
                        if (call.dur > 0) {
                            accepted++
                        } else {
                            declined++
                        }
                    } else if (call.direction == Call.Direction.OUTGOING) {
                        if (call.dur > 0) {
                            called++
                        } else {
                            dialing++
                        }
                    }
                }

                analyticsDate.text =
                    SimpleDateFormat("dd.MM.yyyy").format(App.dateFrom) + " - " + SimpleDateFormat("dd.MM.yyyy").format(
                        App.dateTo
                    )

                analytics1.text = "Počet přijatých hovorů: $accepted"
                analytics2.text = "Počet nepřijatých hovorů: $declined"
                analytics3.text = "Počet odchozích spojených hovorů: $called"
                analytics4.text = "Počet odchozích nespojených hovorů: $dialing"

                analyticsSum1.text = "Počet pohybů: " + (accepted + declined + called + dialing)
                analyticsSum2.text = "Počet spojených hovorů: " + (accepted + called)
            }
        }


        val backButton = findViewById<Button>(R.id.button_back)
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }


    }
}