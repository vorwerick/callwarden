package cz.dzubera.callwarden.ui.activity

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import cz.dzubera.callwarden.App
import cz.dzubera.callwarden.BuildConfig
import cz.dzubera.callwarden.R
import cz.dzubera.callwarden.model.Call
import cz.dzubera.callwarden.service.HttpRequest
import cz.dzubera.callwarden.service.db.CallEntity
import cz.dzubera.callwarden.service.db.PendingCallEntity
import cz.dzubera.callwarden.service.uploadCall
import cz.dzubera.callwarden.ui.CallAdapter
import cz.dzubera.callwarden.ui.CallViewModel
import cz.dzubera.callwarden.ui.CallViewModelFactory
import cz.dzubera.callwarden.utils.Config
import cz.dzubera.callwarden.utils.DateUtils
import cz.dzubera.callwarden.utils.PowerSaveUtils
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

    var pendingRequests = 0


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
                navigateToSetting()
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

    private fun navigateToSetting() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
        finish()
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
            val id = App.projectStorage.getProject()!!.id
            if (id.isEmpty()) {
                supportActionBar!!.subtitle = "<žádný>"
            } else {
                supportActionBar!!.subtitle = App.projectStorage.getProject()!!.name

            }

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
                    callEntity.callDuration!!,
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
                    if (!success) {
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
        builder.setMessage("RAMICALL " + BuildConfig.VERSION_NAME + "\n2023 RAMICORP s.r.o. \nVšechna práva vyhrazena")
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
            val id = App.projectStorage.getProject()?.id
            if (id.isNullOrEmpty()) {
                supportActionBar!!.subtitle = "<žádný>"
            } else {
                supportActionBar!!.subtitle = App.projectStorage.getProject()!!.name

            }
        }

        setContentView(R.layout.activity_main)

        supportActionBar?.title = "Záznamy hovorů";

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
        val buttonProject: Button = findViewById(R.id.buttonProject)
        buttonProject.setOnClickListener { selectProjectFilter() }
        if (App.projectFilter == null) {
            buttonProject.text = "Všechny projekty"
        } else {
            buttonProject.text = App.projectFilter!!.name
        }

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

        // only for first start
        // go to setting for optimization
        if (!PreferencesUtils.loadFirstStart(this)) {
            PreferencesUtils.saveFirstStart(this, true);
            showSettingDialog()
        }
    }


    // Show alert dialog to request permissions
    private fun showSettingDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Nastavení")
        builder.setMessage("Pro správné fungování aplikace je potřeba zkontrolovat nastavení.")
        builder.setPositiveButton("Přejít do nastavení") { dialog, which -> navigateToSetting() }
        builder.setNeutralButton("Zrušit") { dialog, which -> { } }
        val dialog = builder.create()
        dialog.show()
    }

    private fun selectProjectFilter() {
        val builderSingle = AlertDialog.Builder(this)
        builderSingle.setTitle("Vybrat filtr")
        builderSingle.setCancelable(true)
        val arrayAdapter =
            ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice)
        arrayAdapter.addAll(App.projectStorage.projects.map {
            if (it.id == "") {
                "Všechny projekty"
            } else {
                it.name

            }
        })
        builderSingle.setAdapter(
            arrayAdapter
        ) { _, p1 ->
            println(p1.toString())
            val p = App.projectStorage.projects[p1]
            val buttonProject: Button = findViewById(R.id.buttonProject)
            if (p.id == "") {
                App.projectFilter = null
                buttonProject.text = "Všechny projekty"
            } else {
                App.projectFilter = p
                buttonProject.text = App.projectFilter!!.name
            }

            App.cacheStorage.loadFromDatabase { }
        }
        builderSingle.setNegativeButton(
            "Zpět"
        ) { dialog, which -> dialog.dismiss() }
        builderSingle.show()
    }

    private fun checkPendingCallsForSend() {


        GlobalScope.launch {
            val pendingCalls = App.appDatabase.pendingCalls().getAll()
            pendingRequests = pendingCalls.size
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
        val buttonProject: Button = findViewById(R.id.buttonProject)
        buttonProject.setOnClickListener { selectProjectFilter() }
        if (App.projectFilter == null) {
            buttonProject.text = "Všechny projekty"
        } else {
            buttonProject.text = App.projectFilter!!.name
        }
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
        val buttonProjects = findViewById<Button>(R.id.buttonProject)
        buttonProjects.text = App.projectStorage.getProject()?.name ?: "Všechny"
        App.cacheStorage.loadFromDatabase()
    }
}

