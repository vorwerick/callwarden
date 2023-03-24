package cz.dzubera.callwarden.activity

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.database.Cursor
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
import cz.dzubera.callwarden.utils.Config
import cz.dzubera.callwarden.utils.DateUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


@DelicateCoroutinesApi
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
            R.id.change_project -> {
                val creds = App.userSettingsStorage.credentials
                if(creds != null){
                    GlobalScope.launch {
                        HttpRequest.getProjects(creds.domain, creds.user){
                            if(it.code == 200){
                                runOnUiThread { showProjectDialog(true) }
                            }
                        }
                    }
                   return true
                }
                return false

            }
            R.id.menu_user -> {
                showUserDialog()
                true
            }

            R.id.analytics -> {
                val intent = Intent(this, AnalyticsActivity::class.java)
                startActivity(intent)
                finish()

                true
            }
            R.id.signOut -> {
                PreferencesUtils.clearCredentials(this)
                Config.signedOut = true
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showProjectDialog(cancelable: Boolean) {
        val builderSingle = AlertDialog.Builder(this)
        builderSingle.setTitle("Vybrat skupinu")
        builderSingle.setCancelable(cancelable)
        val arrayAdapter =
            ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice)
        arrayAdapter.addAll(App.projectStorage.projects.map { it.name })
        builderSingle.setAdapter(
            arrayAdapter
        ) { _, p1 ->
            println(p1.toString())
            App.projectStorage.setProject(App.projectStorage.projects[p1])
            PreferencesUtils.saveProjectId(this@MainActivity, App.projectStorage.getProject()!!.id)
            supportActionBar!!.subtitle = App.projectStorage.getProject()!!.name
        }
        if (cancelable) {
            builderSingle.setNegativeButton(
                "Zpět"
            ) { dialog, which -> dialog.dismiss() }
        }



        builderSingle.show()
    }

    private fun showAboutDialog() {
        // 1. Instantiate an <code><a href="/reference/android/app/AlertDialog.Builder.html">AlertDialog.Builder</a></code> with its constructor
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setMessage("Vytvořil Aleš Džubera\n" + "Verze " + BuildConfig.VERSION_NAME)
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
        builder.setMessage("ID domény ${App.userSettingsStorage.credentials!!.domain}\nID uživatele ${App.userSettingsStorage.credentials!!.user}\nProjekt ${App.projectStorage.getProject()?.name ?: "<žádný>"}")
            .setTitle("Uživatel").setPositiveButton(
                "Ok"
            ) { p0, p1 -> p0.dismiss() }

// 3. Get the <code><a href="/reference/android/app/AlertDialog.html">AlertDialog</a></code> from <code><a href="/reference/android/app/AlertDialog.Builder.html#create()">create()</a></code>
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val credentials = PreferencesUtils.loadCredentials(this)

        if (credentials == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        val projectId = PreferencesUtils.loadProjectId(this)
        if (projectId != null) {
            val projectObject = App.projectStorage.projects.find { it.id == projectId }
            if (projectObject != null) {
                App.projectStorage.setProject(projectObject)
            }
        }

        setContentView(R.layout.activity_main)

        supportActionBar?.title = "Záznamy hovorů";
        val telephonyManager: TelephonyManager =
            getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        App.userSettingsStorage.credentials = credentials



        App.cacheStorage.registerObserver(::callObserver)

        App.cacheStorage.notifyItems()


        val callAdapter = CallAdapter()

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


        App.cacheStorage.notifyItems()

        Intent(this, BackgroundCallService::class.java).also { intent ->
            startService(intent)
        }

        if (App.projectStorage.getProject() != null) {
            supportActionBar!!.subtitle = App.projectStorage.getProject()!!.name
        } else {
            supportActionBar!!.subtitle = "<žádný projekt>"
        }


    }

    private fun checkPendingCallsForSend() {


        GlobalScope.launch {
            val pendingCalls = App.appDatabase.pendingCalls().getAll()
            if (pendingCalls.isEmpty()) {
                return@launch
            }
            println("Pending calls to upload: " + pendingCalls.size.toString())
            val callEntities = pendingCalls.mapNotNull { App.appDatabase.taskCalls().get(it.uid) }
            uploadCall(this@MainActivity, callEntities) { success ->
                if (success) {
                    GlobalScope.launch {
                        App.appDatabase.pendingCalls().deleteAll()
                    }
                }

            }
        }
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

        if (App.projectStorage.getProject() == null) {
            showProjectDialog(false)
        }

        checkPendingCallsForSend()

    }

    override fun onDestroy() {
        super.onDestroy()
        App.cacheStorage.unregisterObserver(::callObserver)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                App.dateFrom = DateUtils.atStartOfDay(App.toDate(c))
            } else {
                App.dateFrom = App.toDate(c)
            }
            updateButtons()


        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))

        dpd.show()
    }

    fun showDateToPicker() {
        val c = Calendar.getInstance()
        c.time = App.dateTo

        val dpd = DatePickerDialog(this, { view, year, monthOfYear, dayOfMonth ->

            c.set(year, monthOfYear, dayOfMonth)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                App.dateTo = DateUtils.atEndOfDay(App.toDate(c))
            } else {
                App.dateTo = App.toDate(c)
            }
            updateButtons()

        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))

        dpd.show()
    }


    fun updateButtons() {
        val buttonFrom: Button = findViewById(R.id.buttonFrom)
        buttonFrom.text = SimpleDateFormat("d.M.yyyy").format(App.dateFrom)
        val buttonTo: Button = findViewById(R.id.buttonTo)
        buttonTo.text = SimpleDateFormat("d.M.yyyy").format(App.dateTo)

        GlobalScope.launch {
            App.cacheStorage.loadFromDatabase()
            App.cacheStorage.notifyItems()
            /*  val items = App.transmissionService.getAndRemovePendingItems().forEach {
                  print("CALL: " + it.callStarted)
                  sendCallToInternet(it)
              }*/
        }
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

