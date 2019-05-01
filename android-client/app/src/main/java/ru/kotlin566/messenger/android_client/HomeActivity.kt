package ru.kotlin566.messenger.android_client

import android.app.ActivityOptions
import android.os.Bundle
import android.support.v7.app.AppCompatActivity;
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_create_chat.toolbar as chatCreateToolbar

import kotlinx.android.synthetic.main.activity_main.*
import android.content.Intent
import android.widget.LinearLayout

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        fab.setOnClickListener {
            val k = Intent(this, CreateChat::class.java)
            startActivity(k, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }

//        val layout = findViewById<LinearLayout>(R.id.messagesDisplay)
//        val child = layoutInflater.inflate(R.layout.my_message_view_item, null)
//        child.
//        layout.addView(child)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
