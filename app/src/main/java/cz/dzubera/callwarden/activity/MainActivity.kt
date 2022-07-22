package cz.dzubera.callwarden.activity

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import cz.dzubera.callwarden.*
import io.sentry.Sentry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private val callViewModel by viewModels<CallViewModel> {
        CallViewModelFactory(this)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_about -> {
                showAboutDialog()
                true
            }
            R.id.menu_user -> {
                showUserDialog()
                true
            }
            R.id.menu_url -> {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://docs.google.com/spreadsheets/d/1VhwBDR4XjCSH13asJAcyaLz3XEBIQDFYCZUDq4w3P3I/edit#gid=0")
                )
                startActivity(browserIntent)
                true
            }
            R.id.analytics -> {
                val intent = Intent(this, AnalyticsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.signOut -> {
                getSharedPreferences("XXX", Context.MODE_PRIVATE).edit()
                    .putString("userName", "")
                    .putString("userNumber", "").apply()
                Config.signedOut = true
                val intent = Intent(this, SignInActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAboutDialog() {
        // 1. Instantiate an <code><a href="/reference/android/app/AlertDialog.Builder.html">AlertDialog.Builder</a></code> with its constructor
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setMessage("Vytvořil Aleš Džubera\n" + "Verze " + BuildConfig.VERSION_NAME + " " + BuildConfig.BUILD_TYPE + "\n" + "Číslo sestavení " + BuildConfig.VERSION_CODE + "\n" + "Identifikátor " + BuildConfig.APPLICATION_ID)
            .setTitle("O aplikaci").setPositiveButton(
                "Ok"
            ) { p0, p1 -> p0.dismiss() }

// 3. Get the <code><a href="/reference/android/app/AlertDialog.html">AlertDialog</a></code> from <code><a href="/reference/android/app/AlertDialog.Builder.html#create()">create()</a></code>
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun showUserDialog() {


        // 1. Instantiate an <code><a href="/reference/android/app/AlertDialog.Builder.html">AlertDialog.Builder</a></code> with its constructor
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setMessage("Jméno ${App.userSettingsStorage.userName}\nTelefon ${App.userSettingsStorage.userNumber}")
            .setTitle("Uživatel").setPositiveButton(
                "Ok"
            ) { p0, p1 -> p0.dismiss() }

// 3. Get the <code><a href="/reference/android/app/AlertDialog.html">AlertDialog</a></code> from <code><a href="/reference/android/app/AlertDialog.Builder.html#create()">create()</a></code>
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val telephonyManager: TelephonyManager =
            getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        App.userSettingsStorage.userName =
            getSharedPreferences("XXX", Context.MODE_PRIVATE).getString("userName", null) ?: ""
        App.userSettingsStorage.userNumber =
            getSharedPreferences("XXX", Context.MODE_PRIVATE).getString("userNumber", null) ?: ""



        App.cacheStorage.registerObserver(::callObserver)

        App.cacheStorage.notifyItems()


        val callAdapter = CallAdapter { call -> adapterOnClick(call) }

        val recyclerView: RecyclerView = findViewById(R.id.call_list)

        val buttonFrom: Button = findViewById(R.id.buttonFrom)
        buttonFrom.setOnClickListener { showDateFromPicker() }
        val buttonTo: Button = findViewById(R.id.buttonTo)
        buttonTo.setOnClickListener { showDateToPicker() }



        recyclerView.adapter = callAdapter

        val emptyView: TextView = findViewById(R.id.call_list_empty_message)

        callViewModel.callsLiveData.observe(this, {
            println(it.size)
            it?.let {
                callAdapter.submitList(it)
                emptyView.visibility = when (it.isEmpty()) {
                    true -> View.VISIBLE
                    false -> View.GONE
                }
                callAdapter.notifyDataSetChanged()
            }
        })

        val mURL =
            URL(Config.API)

        GlobalScope.launch(Dispatchers.IO) {
            with(mURL.openConnection() as HttpURLConnection) {
                // optional default is GET
                requestMethod = "GET"

                println("URL : $url")
                println("Response Code : $responseCode")

                BufferedReader(InputStreamReader(inputStream)).use {
                    val response = StringBuffer()

                    var inputLine = it.readLine()
                    while (inputLine != null) {
                        response.append(inputLine)
                        inputLine = it.readLine()
                    }
/*
                    println("Response : $response")
*/
                }
            }
        }


        App.cacheStorage.notifyItems()

        Intent(this, Deamon::class.java).also { intent ->
            startService(intent)
        }


        /*    val mediaUri = CallLog.Calls.CONTENT_URI
            Log.d(
                "PhoneService", "The Encoded path of the media Uri is "
                        + mediaUri.encodedPath
            )
            val custObser = CustomContentObserver(Handler.createAsync(Looper.myLooper()!!), contentResolver)
            contentResolver.registerContentObserver(mediaUri, false, custObser)
    */
    }


    override fun onResume() {
        super.onResume()
        updateButtons()
        GlobalScope.launch {
            App.cacheStorage.loadFromDatabase()
            App.cacheStorage.notifyItems()
          /*  val items = App.transmissionService.getAndRemovePendingItems().forEach {
                print("CALL: " + it.callStarted)
                sendCallToInternet(it)
            }*/
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        App.cacheStorage.unregisterObserver(::callObserver)
    }


    private fun adapterOnClick(call: Call) {

    }


    private fun callObserver(list: List<Call>) {
        callViewModel.setCalls(list)
    }

    override fun onBackPressed() {
        finish()
    }

    fun showDateFromPicker() {
        val c = Calendar.getInstance()
        c.time = App.dateFrom

        val dpd = DatePickerDialog(this, { view, year, monthOfYear, dayOfMonth ->

            c.set(year, monthOfYear, dayOfMonth)
            App.dateFrom = App.toDate(c)
            updateButtons()


        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))

        dpd.show()
    }

    fun showDateToPicker() {
        val c = Calendar.getInstance()
        c.time = App.dateTo

        val dpd = DatePickerDialog(this, { view, year, monthOfYear, dayOfMonth ->

            c.set(year, monthOfYear, dayOfMonth)
            App.dateTo = App.toDate(c)
            updateButtons()

        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))

        dpd.show()
    }

    fun updateButtons() {
        val buttonFrom: Button = findViewById(R.id.buttonFrom)
        buttonFrom.text = SimpleDateFormat("d.M.yyyy").format(App.dateFrom)
        val buttonTo: Button = findViewById(R.id.buttonTo)
        buttonTo.text = SimpleDateFormat("d.M.yyyy").format(App.dateTo)
        App.cacheStorage.filter()
    }


    @SuppressLint("Range")
    private fun getContactList(): MutableList<Pair<String, String>> {
        val mm: MutableList<Pair<String, String>> = mutableListOf()
        val cr = contentResolver
        val cur: Cursor? = cr.query(
            ContactsContract.Contacts.CONTENT_URI,
            null, null, null, null
        )
        if ((cur?.count ?: 0) > 0) {
            while (cur != null && cur.moveToNext()) {
                val id: String = cur.getString(
                    cur.getColumnIndex(ContactsContract.Contacts._ID)
                )
                val name: String = cur.getString(
                    cur.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME
                    )
                )
                if (cur.getInt(
                        cur.getColumnIndex(
                            ContactsContract.Contacts.HAS_PHONE_NUMBER
                        )
                    ) > 0
                ) {
                    val pCur: Cursor? = cr.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        arrayOf(id),
                        null
                    )
                    while (pCur?.moveToNext() == true) {
                        val phoneNo: String = pCur.getString(
                            pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER
                            )
                        )
                        Log.i("XXX", "Name: $name")
                        Log.i("XXX", "Phone Number: $phoneNo")
                        mm.add(phoneNo to name)
                    }


                    pCur?.close()
                }
            }
        }
        cur?.close()
        return mm
    }

    fun showContacts() {
        val builderSingle = AlertDialog.Builder(this)
        builderSingle.setTitle("Vyberte kontakt")

        val x = getContactList()

        val arrayAdapter =
            ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice)
        x.forEach {
            arrayAdapter.add(it.second)
        }

        builderSingle.setNegativeButton(
            "cancel"
        ) { dialog, which -> dialog.dismiss() }

        builderSingle.setAdapter(
            arrayAdapter
        ) { dialog, which ->
            val strName = arrayAdapter.getItem(which)
            val entry = x.first { it.second == strName }
            val uri = "tel:${entry.first}".toUri()
            startActivity(Intent(Intent.ACTION_CALL, uri))
            dialog.dismiss()
        }
        builderSingle.show()
    }
}

