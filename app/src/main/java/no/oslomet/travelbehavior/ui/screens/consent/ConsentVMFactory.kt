package no.oslomet.travelbehavior.ui.screens.consent

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import no.oslomet.travelbehavior.data.ConsentRepository

class ConsentVMFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ConsentViewModel(ConsentRepository(app.applicationContext)) as T
    }
}
