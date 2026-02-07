package com.quantum.engine

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.quantum.engine.launcher.LauncherActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * SplashActivity - Pantalla de inicio con logo
 */
class SplashActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Mostrar splash por 2 segundos
        lifecycleScope.launch {
            delay(2000)
            
            // Ir al launcher
            startActivity(Intent(this@SplashActivity, LauncherActivity::class.java))
            finish()
        }
    }
}
