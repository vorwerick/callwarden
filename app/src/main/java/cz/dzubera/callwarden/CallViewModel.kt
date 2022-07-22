package cz.dzubera.callwarden

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.util.*

class CallViewModel(val dataSource: DataSource) : ViewModel() {

    val callsLiveData = dataSource.getCallList()

    fun insertCall(call: Call) {
        dataSource.addCall(call)
    }

    fun setCalls(calls: List<Call>) {
        dataSource.setCallList(calls)
    }
}

class CallViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CallViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CallViewModel(
                dataSource = DataSource.getDataSource(context.resources)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}