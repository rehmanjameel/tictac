package io.xconn.tictackotlin

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.xconn.xconn.Client
import io.xconn.xconn.Session
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class App: Application() {
    init {
        context = this
    }

    @SuppressLint("StaticFieldLeak")
    companion object {
        private var context: Context? = null
        lateinit var session: Session

        private lateinit var sharedPref: SharedPreferences

        fun applicationContext(): Context {
            return context!!.applicationContext
        }

        val PREFS_NAME = "sharedPrefs"
        val KEY_LOGGED_IN = "login_key"
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        val myContext: Context = applicationContext()
        Log.e("Check ", "yes")

        sharedPref = myContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        GlobalScope.launch {
            session = connect()
        }
    }

    private suspend fun connect(): Session {
        val client = Client()

        return client.connect("ws://192.168.0.224:8080/ws", "realm1")
    }

    override fun onTerminate() {
        super.onTerminate()


    }

    fun saveString(KEY_NAME: String, text: String) {

        Log.e("Check ", "Here")

        val editor: SharedPreferences.Editor = sharedPref.edit()

        editor.putString(KEY_NAME, text)

        editor.apply()
    }

    fun saveInt(KEY_NAME: String, value: Int) {
        val editor: SharedPreferences.Editor = sharedPref.edit()

        editor.putInt(KEY_NAME, value)

        editor.apply()
    }

    fun saveLoginOrBoolean(value: String, status: Boolean) {

        val editor: SharedPreferences.Editor = sharedPref.edit()

        editor.putBoolean(value, status)

        editor.apply()
    }

    fun getValueString(KEY_NAME: String): String? {

        return sharedPref.getString(KEY_NAME, null)

    }

    fun getValueInt(KEY_NAME: String): Int {

        return sharedPref.getInt(KEY_NAME, 0)
    }


    fun getValueBoolean(value: String): Boolean {

        return sharedPref.getBoolean(value, false)

    }

    fun logoutOrClearSharedPreference() {
        val editor: SharedPreferences.Editor = sharedPref.edit()
        editor.clear().apply()
    }
}