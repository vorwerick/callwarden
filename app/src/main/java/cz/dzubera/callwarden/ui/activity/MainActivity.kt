package cz.dzubera.callwarden.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.messaging.FirebaseMessaging
import cz.dzubera.callwarden.App
import cz.dzubera.callwarden.BuildConfig
import cz.dzubera.callwarden.R
import cz.dzubera.callwarden.model.Call
import cz.dzubera.callwarden.service.HttpRequest
import cz.dzubera.callwarden.service.db.CallEntity
import cz.dzubera.callwarden.storage.ProjectObject
import cz.dzubera.callwarden.ui.CallAdapter
import cz.dzubera.callwarden.ui.CallViewModel
import cz.dzubera.callwarden.ui.CallViewModelFactory
import cz.dzubera.callwarden.utils.Config
import cz.dzubera.callwarden.utils.DateUtils
import cz.dzubera.callwarden.utils.PreferencesUtils
import cz.dzubera.callwarden.utils.startSynchronization
import cz.dzubera.callwarden.utils.uploadCall
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar


@DelicateCoroutinesApi
class MainActivity : AppCompatActivity() {
    private val callViewModel by viewModels<CallViewModel> {
        CallViewModelFactory(this)
    }

    var pendingRequests = 0
    lateinit var launcher: ActivityResultLauncher<Intent>


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

            R.id.sync -> {
                if (ContextCompat.checkSelfPermission(
                        this, Manifest.permission.READ_CALL_LOG
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    //show toast
                    Toast.makeText(
                        this@MainActivity,     ContextCompat.getString(this, R.string.sync_running), Toast.LENGTH_SHORT
                    ).show()
                    //get calls from history
                    startSynchronization(this@MainActivity) {
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity, it, Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    true
                } else {
                    // show toast you need permissions
                    Toast.makeText(this@MainActivity,     ContextCompat.getString(this, R.string.no_permission), Toast.LENGTH_SHORT).show()
                    false
                }


            }

            R.id.analytics -> {
                val intent = Intent(this, AnalyticsActivity::class.java)
                startActivity(intent)
                finish()

                true
            }

            R.id.menu_set_default_dialer -> {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
                    if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) && !roleManager.isRoleHeld(
                            RoleManager.ROLE_DIALER
                        )
                    ) {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                        launcher.launch(intent)
                    }
                } else {
                    // starší Android
                    val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                        putExtra(
                            TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName
                        )
                    }
                    startActivity(intent)
                }
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

    private fun checkPermissions(): Boolean {
        return if (isPermissionsGranted() != PackageManager.PERMISSION_GRANTED) {
            false
        } else {
            true
        }
    }


    // Check permissions status
    private fun isPermissionsGranted(): Int {
        // PERMISSION_GRANTED : Constant Value: 0
        // PERMISSION_DENIED : Constant Value: -1
        var counter = 0;
        for (permission in LoginActivity.permissionList) {
            counter += ContextCompat.checkSelfPermission(this, permission)
        }
        return counter
    }


    private fun navigateToSetting() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showProjectDialog(cancelable: Boolean) {
        val builderSingle = AlertDialog.Builder(this)
        builderSingle.setTitle(
            ContextCompat.getString(this, R.string.project_select_title)
        )
        builderSingle.setCancelable(cancelable)
        val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice)
        arrayAdapter.addAll(App.projectStorage.projects.map { it.name })
        builderSingle.setAdapter(
            arrayAdapter
        ) { _, p1 ->
            println(p1.toString())
            val project = App.projectStorage.projects[p1]
            App.projectStorage.setProject(this, project)

            supportActionBar!!.subtitle = project.name

            startSynch()

        }
        if (cancelable) {
            builderSingle.setNegativeButton(
                ContextCompat.getString(this, R.string.button_back)
            ) { dialog, _ -> dialog.dismiss() }
        }
        builderSingle.show()
    }

    private fun showProjectEditDialog(callEntity: CallEntity) {

        val builderSingle = AlertDialog.Builder(this)
        builderSingle.setTitle(ContextCompat.getString(this, R.string.project_change_title))
        builderSingle.setCancelable(true)
        val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice)
        arrayAdapter.addAll(App.projectStorage.projects.map { it.name })
        builderSingle.setAdapter(
            arrayAdapter
        ) { _, p1 ->
            println(p1.toString())

            val selectedProject = App.projectStorage.projects[p1]
            callEntity.projectIdOld = String(callEntity.projectId!!.toByteArray())
            callEntity.projectId = selectedProject.id
            callEntity.projectName = selectedProject.name


            GlobalScope.launch {

                uploadCall(this@MainActivity, listOf(callEntity)) { success ->
                    if (success) {
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
                        App.appDatabase.taskCalls().update(callEntity)
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                ContextCompat.getString(
                                    this@MainActivity,
                                    R.string.project_changed
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                ContextCompat.getString(
                                    this@MainActivity,
                                    R.string.project_change_failed
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

            }

        }
        builderSingle.setNegativeButton(
            ContextCompat.getString(this, R.string.button_back)
        ) { dialog, _ -> dialog.dismiss() }



        builderSingle.show()
    }

    private fun showAboutDialog() {
        // 1. Instantiate an <code><a href="/reference/android/app/AlertDialog.Builder.html">AlertDialog.Builder</a></code> with its constructor
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setMessage("RAMICALL " + BuildConfig.VERSION_NAME + "(" + BuildConfig.VERSION_CODE + ")" + "\n2023 RAMICORP s.r.o. \nVšechna práva vyhrazena")
            .setTitle(ContextCompat.getString(this, R.string.about_title)).setPositiveButton(
                ContextCompat.getString(this, R.string.about_ok)
            ) { p0, _ -> p0.dismiss() }

// 3. Get the <code><a href="/reference/android/app/AlertDialog.html">AlertDialog</a></code> from <code><a href="/reference/android/app/AlertDialog.Builder.html#create()">create()</a></code>
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    @SuppressLint("SimpleDateFormat")
    private fun showUserDialog() {

        val project = App.projectStorage.getProject(this)

        val lastSyncTime = PreferencesUtils.loadLastSyncDate(this)
        var syncDateTime = SimpleDateFormat("HH:mm").format(lastSyncTime)
        if (lastSyncTime == 0L) {
            syncDateTime = ContextCompat.getString(this, R.string.never)
        }
        // 1. Instantiate an <code><a href="/reference/android/app/AlertDialog.Builder.html">AlertDialog.Builder</a></code> with its constructor
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setMessage(
            "ID: ${App.userSettingsStorage.credentials!!.domain}\nUživatel: ${App.userSettingsStorage.credentials!!.user}\nProjekt: ${project?.name ?: "Nebyl vybrán"}\n" + "Poslední synchronizace: $syncDateTime"
        ).setTitle((ContextCompat.getString(this, R.string.user_title))).setPositiveButton(
            (ContextCompat.getString(this, R.string.about_ok))
        ) { p0, _ -> p0.dismiss() }

// 3. Get the <code><a href="/reference/android/app/AlertDialog.html">AlertDialog</a></code> from <code><a href="/reference/android/app/AlertDialog.Builder.html#create()">create()</a></code>
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }

    private fun openUrl(context: Context, url: String) {

        val intent = Intent(context, NotificationActivity::class.java).apply {
            putExtra("url", url)
            extras?.putString("url", url)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        ContextCompat.startActivity(context, intent, null)


        /*
        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(this, Uri.parse(url))

         */
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        launcher = registerForActivityResult(
            StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(
                    this,
                    (ContextCompat.getString(this, R.string.default_dialer_set)),
                    Toast.LENGTH_SHORT
                ).show()
                // uživatel nastavil aplikaci jako default dialer
            } else {
                Toast.makeText(
                    this,
                    (ContextCompat.getString(this, R.string.default_dialer_failed)),
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
        // start alarm
        // AlarmUtils.scheduleAlarm(applicationContext)

        val credentials = PreferencesUtils.loadCredentials(this)

        if (credentials == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
            PreferencesUtils.save(this@MainActivity, "firebase_token", fcmToken)
            credentials?.let {
                // HttpRequest.sendToken(credentials.domain, credentials.user, fcmToken)
            }

        }


        val project = App.projectStorage.getProject(this)
        supportActionBar!!.subtitle = "---"
        project?.let {
            supportActionBar!!.subtitle = it.name

        }
        setContentView(R.layout.activity_main)

        val url = intent?.extras?.getString("url") ?: intent?.getStringExtra("url")
        if (url != null) {
            findViewById<CardView>(R.id.showCallerDetail).visibility = View.VISIBLE
            findViewById<CardView>(R.id.showCallerDetail).setOnClickListener {
                openUrl(this, url)
                findViewById<CardView>(R.id.showCallerDetail).visibility = View.GONE
            }
        }

        supportActionBar?.title = (ContextCompat.getString(this, R.string.call_records))

        App.userSettingsStorage.credentials = credentials

        App.cacheStorage.registerObserver(::callObserver)

        val callAdapter = CallAdapter({
            GlobalScope.launch {
                val item = App.appDatabase.taskCalls().get(it)
                if (item != null) {
                    runOnUiThread { showProjectEditDialog(item) }
                }
            }
        }, { calls ->
            findViewById<TextView>(R.id.call_list_result_count).text =
                "${
                    (ContextCompat.getString(
                        this,
                        R.string.results_count,
                    ))
                } " + calls.size.toString()
        })

        val recyclerView: RecyclerView = findViewById(R.id.call_list)

        val buttonFrom: Button = findViewById(R.id.buttonFrom)
        buttonFrom.setOnClickListener { showDateFromPicker() }
        val buttonTo: Button = findViewById(R.id.buttonTo)
        buttonTo.setOnClickListener { showDateToPicker() }
        val buttonProject: Button = findViewById(R.id.buttonProject)
        buttonProject.setOnClickListener { selectProjectFilter() }
        if (App.projectFilter == null) {
            buttonProject.text = (ContextCompat.getString(this, R.string.all_projects))
        } else {
            buttonProject.text = App.projectFilter!!.name
        }

        val buttonCallType: Button = findViewById(R.id.buttonCallType)
        buttonCallType.setOnClickListener { selectCallTypeFilter() }
        val income = App.callTypeFilter[0]
        val outcome = App.callTypeFilter[1]
        val accepted = App.callTypeFilter[2]
        val unaccepted = App.callTypeFilter[3]
        if (income && outcome && accepted && unaccepted) {
            buttonCallType.text = (ContextCompat.getString(this, R.string.all))
        } else if (!income && !outcome && !accepted && !unaccepted) {
            buttonCallType.text = (ContextCompat.getString(this, R.string.none))
        } else {
            val sb = StringBuilder()
            if (income) {
                sb.append("${(ContextCompat.getString(this, R.string.incoming))} ")
            }
            if (outcome) {
                sb.append("${(ContextCompat.getString(this, R.string.outgoing))} ")
            }
            if (accepted) {
                sb.append("${(ContextCompat.getString(this, R.string.accepted))} ")
            }
            if (unaccepted) {
                sb.append("${(ContextCompat.getString(this, R.string.unaccepted))} ")
            }
            buttonCallType.text = sb.toString()
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
            PreferencesUtils.saveFirstStart(this, true)
            showSettingDialog()
        }

    }


    // Show alert dialog to request permissions
    private fun showSettingDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle((ContextCompat.getString(this, R.string.settings_title)))
        builder.setMessage((ContextCompat.getString(this, R.string.settings_message)))
        builder.setPositiveButton(
            (ContextCompat.getString(
                this,
                R.string.settings_button
            ))
        ) { _, _ -> navigateToSetting() }
        builder.setNeutralButton(
            (ContextCompat.getString(
                this,
                R.string.cancel
            ))
        ) { dialog, _ -> dialog.cancel() }
        val dialog = builder.create()
        dialog.show()
    }

    private fun selectCallTypeFilter() {
        val builderSingle = AlertDialog.Builder(this)
        builderSingle.setTitle(ContextCompat.getString(this, R.string.filter_call_type))
        builderSingle.setCancelable(true)
        val stringArray = mutableListOf<String>()
        ContextCompat.getString(this, R.string.incoming)
        stringArray.add(ContextCompat.getString(this, R.string.incoming))
        stringArray.add(ContextCompat.getString(this, R.string.outgoing))
        stringArray.add(ContextCompat.getString(this, R.string.accepted))
        stringArray.add(ContextCompat.getString(this, R.string.unaccepted))
        val checkedItems = App.callTypeFilter.toBooleanArray()
        builderSingle.setMultiChoiceItems(
            stringArray.toTypedArray(), checkedItems
        ) { _, which, isChecked ->
            App.callTypeFilter[which] = isChecked
            App.cacheStorage.loadFromDatabase { }
            val buttonCallType: Button = findViewById(R.id.buttonCallType)
            val income = App.callTypeFilter[0]
            val outcome = App.callTypeFilter[1]
            val accepted = App.callTypeFilter[2]
            val unaccepted = App.callTypeFilter[3]
            if (income && outcome && accepted && unaccepted) {
                buttonCallType.text = ContextCompat.getString(this, R.string.all)
            } else if (!income && !outcome && !accepted && !unaccepted) {
                buttonCallType.text = ContextCompat.getString(this, R.string.none)
            } else {
                val sb = StringBuilder()
                if (income) {
                    sb.append("${ContextCompat.getString(this, R.string.incoming)} ")
                }
                if (outcome) {
                    sb.append("${ContextCompat.getString(this, R.string.outgoing)} ")
                }
                if (accepted) {
                    sb.append("${ContextCompat.getString(this, R.string.accepted)} ")
                }
                if (unaccepted) {
                    sb.append("${ContextCompat.getString(this, R.string.unaccepted)} ")
                }
                buttonCallType.text = sb.toString()
            }
        }

        builderSingle.setNegativeButton(
            ContextCompat.getString(this, R.string.button_back)
        ) { dialog, _ -> dialog.dismiss() }
        builderSingle.show()
    }

    private fun selectProjectFilter() {
        val builderSingle = AlertDialog.Builder(this)
        builderSingle.setTitle(ContextCompat.getString(this, R.string.select_filter))
        builderSingle.setCancelable(true)
        val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice)
        val filters =
            App.projectStorage.projects.toMutableList().also { it.add(0, ProjectObject("", "")) }
        arrayAdapter.addAll(filters.map {
            if (it.id == "") {
                ContextCompat.getString(this, R.string.all_projects)
            } else {
                it.name

            }
        })
        builderSingle.setAdapter(
            arrayAdapter
        ) { _, p1 ->
            println(p1.toString())
            val p = filters[p1]
            val buttonProject: Button = findViewById(R.id.buttonProject)
            if (p.id == "") {
                App.projectFilter = null
                buttonProject.text = ContextCompat.getString(this, R.string.all_projects)
            } else {
                App.projectFilter = p
                buttonProject.text = App.projectFilter!!.name
            }

            App.cacheStorage.loadFromDatabase { }
        }
        builderSingle.setNegativeButton(
            ContextCompat.getString(this, R.string.button_back)
        ) { dialog, _ -> dialog.dismiss() }
        builderSingle.show()
    }


    fun startSynch() {
        val lastSyncTime = PreferencesUtils.loadLastSyncDate(this)
        val diff = System.currentTimeMillis() - lastSyncTime
        if (diff > 3600000) {
            startSynchronization(this@MainActivity) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity, it, Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        startSynch()


        updateButtons()
        val buttonProject: Button = findViewById(R.id.buttonProject)
        buttonProject.setOnClickListener { selectProjectFilter() }
        if (App.projectFilter == null) {
            buttonProject.text = ContextCompat.getString(this, R.string.all_projects)
        } else {
            buttonProject.text = App.projectFilter!!.name
        }
        App.cacheStorage.loadFromDatabase()

        val project = App.projectStorage.getProject(this)
        if (project == null) {
            val creds = App.userSettingsStorage.credentials
            if (creds != null) {
                GlobalScope.launch {
                    HttpRequest.getProjects(creds.domain, creds.user) {
                        if (it.code == 200) {
                            runOnUiThread {
                                if (App.projectStorage.projects.size == 1) {
                                    App.projectStorage.setProject(
                                        this@MainActivity, App.projectStorage.projects[0]
                                    )
                                } else {
                                    showProjectDialog(false)
                                }
                            }
                        }
                    }
                }
            }

        }

        //checkPendingCallsForSend()

        getVersionState({

        }, {
            showNewVersionDialog(it) {

            }
        }, {
            showNewVersionDialog(it) {

            }
            //  showUpdateNeededDialog(it)
        })

    }


    override fun onDestroy() {
        super.onDestroy()
        App.cacheStorage.unregisterObserver(::callObserver)
    }


    private fun callObserver(list: List<Call>) {
        //val mockList = MockCallRecords.getMockCallRecords()
        callViewModel.setCalls(list)
    }


    fun showNewVersionDialog(url: String?, done: () -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setTitle(ContextCompat.getString(this, R.string.update_available_title))
        builder.setMessage(ContextCompat.getString(this, R.string.update_available_description))
        url?.let {
            builder.setPositiveButton(
                ContextCompat.getString(
                    this,
                    R.string.button_update
                )
            ) { dialog, which ->
                openUrlInCustomTab(this, url)
                dialog.dismiss()
                done()

            }
        }
        builder.setNegativeButton(
            ContextCompat.getString(
                this,
                R.string.button_back
            )
        ) { dialog, which -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.show()
    }

    fun showUpdateNeededDialog(url: String?) {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setTitle(ContextCompat.getString(this, R.string.update_available_title))
        builder.setMessage(ContextCompat.getString(this, R.string.update_available_description))
        url?.let {
            builder.setPositiveButton(
                ContextCompat.getString(
                    this,
                    R.string.button_update
                )
            ) { dialog, which ->
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(it)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(browserIntent)
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    fun getVersionState(
        ok: () -> Unit, newVersion: (String?) -> Unit, updateNeeded: (String?) -> Unit
    ) {
        val credentials = PreferencesUtils.loadCredentials(this)
        credentials?.let {
            HttpRequest.sendVersion(it.domain, it.user) { status ->
                println("KOKO: + ${status.url} ${status.state}")
                runOnUiThread {
                    status.state?.let { s ->
                        when (status.getState()) {
                            HttpRequest.VersionState.States.NEW_VERSION -> {
                                newVersion(status.url)
                            }

                            HttpRequest.VersionState.States.UPDATE_NEEDED -> {
                                updateNeeded(status.url)
                            }

                            else -> {
                                ok()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun openUrlInCustomTab(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(
                this,
                ContextCompat.getString(this, R.string.browser_cannot_be_opened),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showDateFromPicker() {
        val c = Calendar.getInstance()
        c.time = App.dateFrom

        val dpd = DatePickerDialog(this, { _, year, monthOfYear, dayOfMonth ->

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

    private fun showDateToPicker() {
        val c = Calendar.getInstance()
        c.time = App.dateTo

        val dpd = DatePickerDialog(this, { _, year, monthOfYear, dayOfMonth ->

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


    private fun updateButtons() {
        val buttonFrom: Button = findViewById(R.id.buttonFrom)
        buttonFrom.text = SimpleDateFormat("d.M.yyyy").format(App.dateFrom)
        val buttonTo: Button = findViewById(R.id.buttonTo)
        buttonTo.text = SimpleDateFormat("d.M.yyyy").format(App.dateTo)
        val buttonProjects = findViewById<Button>(R.id.buttonProject)
        buttonProjects.text =
            App.projectStorage.getProject(this)?.name ?: ContextCompat.getString(this, R.string.all)
        App.cacheStorage.loadFromDatabase()
    }
}
