package cz.dzubera.callwarden.ui.activity

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
import cz.dzubera.callwarden.model.Call
import cz.dzubera.callwarden.service.BackgroundCallService
import cz.dzubera.callwarden.service.HttpRequest
import cz.dzubera.callwarden.service.db.CallEntity
import cz.dzubera.callwarden.service.db.PendingCallEntity
import cz.dzubera.callwarden.service.uploadCall
import cz.dzubera.callwarden.ui.CallAdapter
import cz.dzubera.callwarden.ui.CallViewModel
import cz.dzubera.callwarden.ui.CallViewModelFactory
import cz.dzubera.callwarden.utils.Config
import cz.dzubera.callwarden.utils.DateUtils
import cz.dzubera.callwarden.utils.PreferencesUtils
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

    var pendingRequests =0


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
                if (creds != null) {
                    GlobalScope.launch {
                        HttpRequest.getProjects(creds.domain, creds.user) {
                            if (it.code == 200) {
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
            R.id.menu_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                finish()

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
        builderSingle.setTitle("Vybrat projekt")
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
            PreferencesUtils.saveProjectName(
                this@MainActivity,
                App.projectStorage.getProject()!!.name
            )
            val id =App.projectStorage.getProject()!!.id
            if(id.isEmpty()){
                supportActionBar!!.subtitle = "Všechny projekty"
            } else {
                supportActionBar!!.subtitle = App.projectStorage.getProject()!!.name

            }
            App.cacheStorage.loadFromDatabase {  }
        }
        if (cancelable) {
            builderSingle.setNegativeButton(
                "Zpět"
            ) { dialog, which -> dialog.dismiss() }
        }



        builderSingle.show()
    }

    private fun showProjectEditDialog(callEntity: CallEntity) {

        val builderSingle = AlertDialog.Builder(this)
        builderSingle.setTitle("Změna projektu")
        builderSingle.setCancelable(true)
        val arrayAdapter =
            ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice)
        arrayAdapter.addAll(App.projectStorage.projects.map { it.name })
        builderSingle.setAdapter(
            arrayAdapter
        ) { _, p1 ->
            println(p1.toString())

            val selectedProject = App.projectStorage.projects[p1]
            callEntity.projectIdOld = String(callEntity.projectId!!.toByteArray())
            callEntity.projectId = selectedProject.id
            callEntity.projectName = selectedProject.name
            App.cacheStorage.editCallItem(
                Call(
                    callEntity.uid,
                    callEntity.userId!!,
                    callEntity.domainId!!,
                    callEntity.projectId ?: "-1",
                    callEntity.projectName ?: "<none>",
                    Call.Type.valueOf(callEntity.type!!),
                    Call.Direction.valueOf(callEntity.direction!!),
                    callEntity.phoneNumber!!,
                    callEntity.callStarted!!,
                    callEntity.callEnded!!,
                    callEntity.callAccepted
                )
            )

            GlobalScope.launch {
                App.appDatabase.taskCalls().update(callEntity)
                uploadCall(this@MainActivity, listOf(callEntity)) { success ->
                    if(!success){
                        val pendingEntity = PendingCallEntity(callEntity.callStarted)
                        GlobalScope.launch {
                            App.appDatabase.pendingCalls().insert(pendingEntity)
                        }
                    }
                }

            }

        }
        builderSingle.setNegativeButton(
            "Zpět"
        ) { dialog, which -> dialog.dismiss() }



        builderSingle.show()
    }

    private fun showAboutDialog() {
        // 1. Instantiate an <code><a href="/reference/android/app/AlertDialog.Builder.html">AlertDialog.Builder</a></code> with its constructor
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setMessage("RAMICALL " + BuildConfig.VERSION_NAME+"\n2023 RAMICORP s.r.o. \nVšechna práva vyhrazena")
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
        builder.setMessage("ID domény ${App.userSettingsStorage.credentials!!.domain}\nID uživatele ${App.userSettingsStorage.credentials!!.user}\nProjekt ${App.projectStorage.getProject()?.name ?: "<žádný>"}\nNeodeslaných požadavků: ${pendingRequests}")
            .setTitle("Uživatel").setPositiveButton(
                "Ok"
            ) { p0, p1 -> p0.dismiss() }

// 3. Get the <code><a href="/reference/android/app/AlertDialog.html">AlertDialog</a></code> from <code><a href="/reference/android/app/AlertDialog.Builder.html#create()">create()</a></code>
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

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
            val id =App.projectStorage.getProject()!!.id
            if(id.isEmpty()){
                supportActionBar!!.subtitle = "Všechny projekty"
            } else {
                supportActionBar!!.subtitle = App.projectStorage.getProject()!!.name

            }
        } else {
            showProjectDialog(false)
        }

        setContentView(R.layout.activity_main)

        val firstStart = PreferencesUtils.loadFirstStart(this)
        if (!firstStart) {
            showAutoRestartDialog()
            PreferencesUtils.saveFirstStart(this, true)
        }

        supportActionBar?.title = "Záznamy hovorů";
        val telephonyManager: TelephonyManager =
            getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        App.userSettingsStorage.credentials = credentials



        App.cacheStorage.registerObserver(::callObserver)


        val callAdapter = CallAdapter {
            GlobalScope.launch {
                val item = App.appDatabase.taskCalls().get(it)
                if (item != null) {
                    runOnUiThread { showProjectEditDialog(item) }
                }
            }

        }

        val recyclerView: RecyclerView = findViewById(R.id.call_list)

        val buttonFrom: Button = findViewById(R.id.buttonFrom)
        buttonFrom.setOnClickListener { showDateFromPicker() }
        val buttonTo: Button = findViewById(R.id.buttonTo)
        buttonTo.setOnClickListener { showDateToPicker() }



        recyclerView.adapter = callAdapter

        val emptyView: TextView = findViewById(R.id.call_list_empty_message)

        callViewModel.callsLiveData.observe(this) {
            println(it.size)
            it?.let {
                callAdapter.submitList(it)
                emptyView.visibility = when (it.isEmpty()) {
                    true -> View.VISIBLE
                    false -> View.GONE
                }
                callAdapter.notifyDataSetChanged()
            }
        }



        Intent(this, BackgroundCallService::class.java).also { intent ->
            startService(intent)
        }





    }

    private fun showAutoRestartDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Automatické spuštění")
        builder.setMessage("Chcete nastavit automatické spuštění po zapnutí telefonu?")
        builder.setPositiveButton("Ano") { dialog, which ->
            PreferencesUtils.saveAutoRestartValue(this@MainActivity, true)
            dialog.dismiss()
        }
        builder.setNegativeButton("Ne") { dialog, which ->
            PreferencesUtils.saveAutoRestartValue(this@MainActivity, false)
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun checkPendingCallsForSend() {


        GlobalScope.launch {
            val pendingCalls = App.appDatabase.pendingCalls().getAll()
            pendingRequests =pendingCalls.size
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

        App.cacheStorage.loadFromDatabase()

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

        App.cacheStorage.loadFromDatabase()
    }
}

