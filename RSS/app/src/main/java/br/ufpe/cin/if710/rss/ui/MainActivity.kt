package br.ufpe.cin.if710.rss.ui

import android.app.Activity
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import br.ufpe.cin.if710.rss.util.GetRssFeedAsynTask
import br.ufpe.cin.if710.rss.domain.ItemListViewHolder
import br.ufpe.cin.if710.rss.R
import br.ufpe.cin.if710.rss.db.SQLiteRSSHelper
import br.ufpe.cin.if710.rss.domain.ItemRSS
import br.ufpe.cin.if710.rss.util.GetRssFeedService
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import java.io.IOException

class MainActivity : Activity() {
    private lateinit var preferences: SharedPreferences
    private lateinit var dbHelper: SQLiteRSSHelper

    val layoutManager = LinearLayoutManager(this)
    lateinit var intentFilter: IntentFilter
    val receiver = object: BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            if (i?.action?.equals(GetRssFeedService.COMPLETED_DOWNLOAD) == true) {
                printRSS()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        dbHelper = SQLiteRSSHelper.getInstance(this)
        intentFilter = IntentFilter(GetRssFeedService.COMPLETED_DOWNLOAD)
    }

    // estamos triggando o getRssFeed e definindo o callback a ser executado
    override fun onResume() {
        super.onResume()
        try {
            val rssFeed = preferences.getString("rss_feed", getString(R.string.default_rss_feed))
            val service = Intent(applicationContext, GetRssFeedService::class.java)
            service.data = Uri.parse(rssFeed)
            registerReceiver(receiver, intentFilter)
            startService(service)
            printRSS()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(applicationContext, RssFeedPreferenceActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(receiver)
    }

    fun printRSS() {
        val items = dbHelper.items
        val recyclerAdapter = ItemRSSListAdapter(items)
        conteudoRSS.apply {
            setHasFixedSize(true)
            layoutManager = this@MainActivity.layoutManager
            adapter = recyclerAdapter
        }
    }

    // adapter para executar o mapeamento do formar saído do fetch para a tela
    inner class ItemRSSListAdapter(private val itemsRSS: List<ItemRSS>): RecyclerView.Adapter<ItemListViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemListViewHolder {
            val view = layoutInflater.inflate(R.layout.itemlista, parent, false)
            return ItemListViewHolder(view)
        }

        override fun getItemCount(): Int {
            return itemsRSS.size
        }

        override fun onBindViewHolder(holder: ItemListViewHolder, i: Int) {
            val item = itemsRSS[i]
            holder.itemData.text = item.pubDate
            holder.itemTitulo.text = item.title
            holder.itemTitulo.onClick {
                val uri = Uri.parse(itemsRSS[i].link)
                dbHelper.markAsRead(uri.toString())
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        }
    }
}
