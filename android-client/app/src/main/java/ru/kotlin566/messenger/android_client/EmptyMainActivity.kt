package ru.kotlin566.messenger.android_client

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import ru.kotlin566.messenger.android_client.ui.login.LoginActivity

class EmptyMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO check authorization
        val loggedIn = false
        val newIndent: Intent
        if(loggedIn)
            newIndent = Intent(this, MainActivity::class.java)
        else
            newIndent = Intent(this, LoginActivity::class.java)
        startActivity(newIndent)
        finish()
    }
}
