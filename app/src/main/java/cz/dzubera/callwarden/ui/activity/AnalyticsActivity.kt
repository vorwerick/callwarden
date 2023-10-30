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

        val analytics1 = findViewById<TextView>(R.id.analytics_text_1)
        val analytics2 = findViewById<TextView>(R.id.analytics_text_2)
        val analytics3 = findViewById<TextView>(R.id.analytics_text_3)
        val analytics4 = findViewById<TextView>(R.id.analytics_text_4)

        val analyticsSum1 = findViewById<TextView>(R.id.analytics_text_sum1)
        val analyticsSum2 = findViewById<TextView>(R.id.analytics_text_sum2)

        val analyticsDate = findViewById<TextView>(R.id.analytics_date)
        val analyticsProject = findViewById<TextView>(R.id.analytics_project)
        if ( App.projectFilter == null) {
            analyticsProject.text =  "Všechny projekty"
        } else {
            analyticsProject.text =   App.projectFilter!!.name
        }



        App.cacheStorage.loadFromDatabase {
            runOnUiThread {
                var accepted = 0
                var declined = 0
                var called = 0
                var dialing = 0

                it.forEach { call ->
                    if (call.direction == Call.Direction.INCOMING) {
                        if (call.duration <= 0) {
                            declined++
                        } else {
                            accepted++
                        }
                    } else {
                        if (call.duration <= 0) {
                           dialing++
                        } else {
                            called++
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