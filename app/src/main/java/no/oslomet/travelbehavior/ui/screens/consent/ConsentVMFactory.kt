package no.oslomet.travelbehavior.ui.screens.consent

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import no.oslomet.travelbehavior.data.ConsentRepositoryImpl

/**
 * Factory for creating ConsentViewModel with its required dependencies.
 */
class ConsentVMFactory(private val app: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ConsentViewModel(
            repo = ConsentRepositoryImpl(app.applicationContext)
        ) as T
    }
}
