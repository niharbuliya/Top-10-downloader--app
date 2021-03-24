package com.example.top10downloader

import android.content.Context
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import java.net.URL
import kotlin.properties.Delegates

class FeedEntry {
    var name: String = ""
    var artist: String = ""
    var releaseDate: String = ""
    var summary: String = ""
    var imageURL: String = ""

    override fun toString(): String {
        return """
            Name = $name
            Artist = $artist
            ReleaseDate= $releaseDate
            ImageURL = $imageURL
        """.trimIndent()
    }
}

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    private var downloadData: DownloadData? =null
    private var feedURL: String =" http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml"
    private var feedLimit =10

    private var feedCachedURL = "INVALIDATED"
    private val STATE_URL = "feedURL"
    private val STATE_LIMIT = "feedLimit"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG,"onCreate called")

        if(savedInstanceState!=null){
            feedURL = savedInstanceState.getString(STATE_URL).toString()
            feedLimit = savedInstanceState.getInt(STATE_LIMIT)
        }
        downloadUrl(feedURL.format(feedLimit))
        Log.d(TAG, "onCreate: done")
    }
    private fun downloadUrl(feedURL: String){
        if(feedURL != feedCachedURL) {
            Log.d(TAG, "downloadURL starting AsyncTask")
            downloadData = DownloadData(this, findViewById(R.id.xmlListView))
            downloadData?.execute(feedURL)
            feedCachedURL = feedURL
            Log.d(TAG, "downloaduUrl done")
        }else{
            Log.d(TAG,"downloadURL - URL not changed")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.feeds_menu ,menu)

        if(feedLimit ==10){
            menu?.findItem(R.id.mnu10)?.isChecked = true
        }
        else{
            menu?.findItem(R.id.mnu25)?.isChecked = true
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {


        when (item.itemId){
            R.id.mnuFree ->
                feedURL = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml"

            R.id.mnuPaid ->
                feedURL = " http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/toppaidapplications/limit=%d/xml"

            R.id.mnuSongs ->
                feedURL = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topSongs/limit=%d/xml"
            R.id.mnu10,R.id.mnu25 ->{
                if(!item.isChecked){
                    item.isChecked = true
                    feedLimit =35 - feedLimit
                    Log.d(TAG, "onOptionsItemSelected:  ${item.title} setting feedLimit to ${feedLimit}")
                }else{
                    Log.d(TAG,"onOptionsItemSelected :  ${item.title} setting feedLimit unchanged")
                }
            }
            R.id.mnuRefresh -> feedCachedURL ="INVALIDATED"
            else ->
                return super.onOptionsItemSelected(item)
        }
        downloadUrl(feedURL.format(feedLimit))
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_URL,feedURL)
        outState.putInt(STATE_LIMIT,feedLimit)
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadData?.cancel(true)
    }

    companion object {
        private class DownloadData(context: Context, listView: ListView) : AsyncTask<String, Void, String>() {
            private val TAG = "DownloadData"

            var propContext: Context by Delegates.notNull()
            var propListView: ListView by Delegates.notNull()

            init {
                propContext = context
                propListView = listView
            }


            override fun onPostExecute(result: String?) {
                super.onPostExecute(result)
                val parseApplications = ParseApplications()
                if (result != null) {
                    parseApplications.parse(result)
                }

                val feedAdapter = FeedAdapter(propContext, R.layout.list_record, parseApplications.applications)
                propListView.adapter = feedAdapter
            }


            override fun doInBackground(vararg url: String?): String {
                Log.d(TAG, "doInBackground:starts with ${url[0]}")
                val rssFeed = downloadXML(url[0])
                if (rssFeed.isEmpty()) {
                    Log.e(TAG, "doInBackgroi=und :Error downloading")
                }
                return rssFeed
            }

            private fun downloadXML(urlPath: String?): String {
                return URL(urlPath).readText()
            }

        }

    }
}
