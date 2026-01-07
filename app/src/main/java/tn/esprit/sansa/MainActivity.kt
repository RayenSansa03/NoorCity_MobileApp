package tn.esprit.sansa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import tn.esprit.sansa.ui.navigation.AppNavigationWithModernBar
import tn.esprit.sansa.ui.theme.SansaTheme
import tn.esprit.sansa.ui.viewmodels.SettingsViewModel
import tn.esprit.sansa.ui.utils.SansaLocalization
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.PaymentConfiguration
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Initialisation de Stripe avec la clé fournie par l'utilisateur
        PaymentConfiguration.init(
            applicationContext,
            "pk_test_51SmQcvDxGWBSGImQKuRY1oie8VBWDQD8suP009GNyWFpsQuf9qcstYTiJbBx5Oo8DiIrrq9vS3pu1DJGBy0wA3mT00dHjcpeDB"
        )

        enableEdgeToEdge()

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            SansaTheme(
                darkTheme = settingsViewModel.isDarkMode,
                highContrast = settingsViewModel.isHighContrast,
                textSize = settingsViewModel.textSize
            ) {
                SansaLocalization(language = settingsViewModel.selectedLanguage) {
                    AppNavigationWithModernBar(
                        modifier = Modifier.fillMaxSize(),
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}