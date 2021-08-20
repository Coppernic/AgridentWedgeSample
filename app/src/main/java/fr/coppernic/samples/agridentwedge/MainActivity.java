package fr.coppernic.samples.agridentwedge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import fr.coppernic.sdk.core.Defines;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {
    private static final String AGRIDENT_WEDGE = "fr.coppernic.tools.cpcagridentwedge";

    TextView tvDataReadValue;
    EditText etDataRead;
    private final BroadcastReceiver agridentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Clears keyboard wedge edit text
            etDataRead.setText("");
            if (intent.getAction().equals(Defines.IntentDefines.ACTION_AGRIDENT_SUCCESS)) {
                // Displays data read in the intent edit text
                String dataRead = intent.getStringExtra(Defines.Keys.KEY_BARCODE_DATA);
                tvDataReadValue.setText(dataRead);
            } else if (intent.getAction().equals(Defines.IntentDefines.ACTION_AGRIDENT_ERROR)) {
                // Displays no data read in intent edit text
                tvDataReadValue.setText(R.string.no_data_read);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvDataReadValue = findViewById(R.id.tvDataReadValue);
        etDataRead = findViewById(R.id.etDataRead);
        findViewById(R.id.fab).setOnClickListener(this::startAgridentWedge);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_tune) {
            startActivity(new Intent(this, SampleActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Registers agrident wedge intent receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Defines.IntentDefines.ACTION_AGRIDENT_SUCCESS);
        intentFilter.addAction(Defines.IntentDefines.ACTION_AGRIDENT_ERROR);
        registerReceiver(agridentReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        // Unregisters agrident wedge receiver
        unregisterReceiver(agridentReceiver);
        Timber.d("onStop");
        super.onStop();
    }

    void startAgridentWedge(View v) {
        // Checks if Agrident Wedge is installed, if not, displays an error message
        if (!isAppInstalled(this, AGRIDENT_WEDGE)) {
            Snackbar.make(v, "Error: Agrident Wedge is not installed", Snackbar.LENGTH_SHORT).show();

            return;
        }

        // Starts Agrident wedge
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(AGRIDENT_WEDGE);
        if (launchIntent != null) {
            startActivity(launchIntent);//null pointer check in case package name was not found
        }
    }

    /**
     * Checks if an application is installed on the device
     *
     * @param context     A context
     * @param packageName Application Id
     * @return True if application is installed
     */
    private boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
