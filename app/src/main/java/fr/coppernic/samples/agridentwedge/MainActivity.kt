package fr.coppernic.samples.agridentwedge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import fr.coppernic.samples.agridentwedge.databinding.ActivityMainBinding
import fr.coppernic.sdk.core.Defines
import fr.coppernic.sdk.utils.helpers.OsHelper
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    var tvDataReadValue: TextView? = null
    var etDataRead: EditText? = null
    private val agridentReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Clears keyboard wedge edit text
            etDataRead!!.setText("")
            if (intent.action == Defines.IntentDefines.ACTION_AGRIDENT_SUCCESS) {
                // Displays data read in the intent edit text
                val dataRead = intent.getStringExtra(Defines.Keys.KEY_BARCODE_DATA)
                tvDataReadValue!!.text = dataRead
            } else if (intent.action == Defines.IntentDefines.ACTION_AGRIDENT_ERROR) {
                // Displays no data read in intent edit text
                tvDataReadValue!!.setText(R.string.no_data_read)
            }
        }
    }
    private var binding: ActivityMainBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(
            layoutInflater
        )
        val view: View = binding!!.root
        setContentView(view)
        val toolbar = binding!!.toolbar
        setSupportActionBar(toolbar)
    }

    override fun onResume() {
        super.onResume()
        tvDataReadValue = binding!!.include.tvDataReadValue
        etDataRead = binding!!.include.etDataRead
        binding!!.fab.setOnClickListener { view -> startAgridentWedge(view) }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_tune -> startActivity(Intent(this, SampleActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        // Registers agrident wedge intent receiver
        val intentFilter = IntentFilter()
        intentFilter.addAction(Defines.IntentDefines.ACTION_AGRIDENT_SUCCESS)
        intentFilter.addAction(Defines.IntentDefines.ACTION_AGRIDENT_ERROR)
        registerReceiver(agridentReceiver, intentFilter)
    }

    override fun onStop() {
        // Unregisters agrident wedge receiver
        unregisterReceiver(agridentReceiver)
        Timber.d("onStop")
        super.onStop()
    }

    fun startAgridentWedge(v: View?) {
        // Checks if Agrident Wedge is installed, if not, displays an error message

        val servicePackageName = OsHelper.getSystemServicePackage(this, AGRIDENT_WEDGE)

        if (!isAppInstalled(this, servicePackageName)) {
            Snackbar.make(v!!, "Error: Agrident Wedge is not installed", Snackbar.LENGTH_SHORT)
                .show()
            return
        }

        // Starts Agrident wedge
        val launchIntent = packageManager.getLaunchIntentForPackage(servicePackageName)
        launchIntent?.let { startActivity(it) }
    }

    /**
     * Checks if an application is installed on the device
     *
     * @param context     A context
     * @param packageName Application Id
     * @return True if application is installed
     */
    private fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    companion object {
        private const val AGRIDENT_WEDGE = "fr.coppernic.tools.cpcagridentwedge"
    }
}