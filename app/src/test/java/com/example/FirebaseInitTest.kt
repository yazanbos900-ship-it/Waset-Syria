package com.example

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import com.example.data.repository.AuthRepository
import com.example.data.local.AppDatabase

@RunWith(AndroidJUnit4::class)
@org.robolectric.annotation.Config(sdk = [33])
class FirebaseInitTest {
    @Test
    fun testMainActivityLaunch() {
        try {
            org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup().get()
            println("MainActivity launched successfully")
        } catch (e: Exception) {
            println("MainActivity launch failed: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    @Test
    fun testFirebaseInit() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = AppDatabase.getDatabase(context)
        val repo = AuthRepository(db.userDao(), context)
        println("Auth repo created")
        
        try {
            val result = repo.checkUsernameAvailability("test")
            println("Result: $result")
        } catch (e: Exception) {
            println("Error: ${e.message}")
        }
    }
}
