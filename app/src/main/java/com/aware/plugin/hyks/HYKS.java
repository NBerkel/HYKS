package com.aware.plugin.hyks;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.aware.Applications;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.providers.Scheduler_Provider;
import com.aware.ui.PermissionsHandler;
import com.aware.utils.Aware_TTS;
import com.aware.utils.Scheduler;

import org.json.JSONException;

import java.util.ArrayList;

import static java.lang.Math.round;

/**
 * Created by niels on 07/22/2016.
 */

public class HYKS extends AppCompatActivity {

    private TextView device_id;
    private Button join_study, set_settings, sync_data, set_schedule;

    private ArrayList<String> REQUIRED_PERMISSIONS = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_ui);

        //Intent aware = new Intent(this, Aware.class);
        //startService(aware);
        sendBroadcast(new Intent(Aware.ACTION_AWARE_PRIORITY_FOREGROUND));

        boolean permissions_ok = true;
        for (String p : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                permissions_ok = false;
                break;
            }
        }

        if (!permissions_ok) {
            Intent permissions = new Intent(this, PermissionsHandler.class);
            permissions.putExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS, REQUIRED_PERMISSIONS);
            permissions.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            permissions.putExtra(PermissionsHandler.EXTRA_REDIRECT_ACTIVITY,
                    getPackageName() + "/" + HYKS.class.getName());

            startActivity(permissions);
            finish();
        } else {
            Applications.isAccessibilityServiceActive(getApplicationContext());

            device_id = (TextView) findViewById(R.id.device_id);
            device_id.setText("UUID: " + Aware.getSetting(this, Aware_Preferences.DEVICE_ID));

            set_settings = (Button) findViewById(R.id.set_settings);
            set_settings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent settings = new Intent(getApplicationContext(), Settings.class);
                    startActivity(settings);
                }
            });

            join_study = (Button) findViewById(R.id.join_study);
            join_study.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // TODO : URL
                    //String url = "https://api.awareframework.com/index.php/webservice/index/123/123456789";

                    EditText edittext_study_url = (EditText) findViewById(R.id.edittext_study_url);
                    String url = String.valueOf(edittext_study_url.getText());
                    if (url.length() < 20) {
                        if (!url.matches("[0-9a-fA-F]{16}")) {
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    R.string.core_secret_id_error,
                                    Toast.LENGTH_LONG);
                            toast.show();
                            return;
                        }
                        if (!deviceidChecksumValid(url)) {
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    R.string.core_secret_id_checksum_error,
                                    Toast.LENGTH_LONG);
                            toast.show();
                            return;
                        }
                        url = "https://aware.koota.zgib.net/index.php/aware/v1/" + url.toLowerCase() + "?crt_sha256=436669a4920ac08623357aaca77c935bbc3bf3906a9c962eb822edff021fcc42&crt_url=https%3A%2F%2Fdata.koota.cs.aalto.fi%2Fstatic%2Fserver-aware.crt";
                    }
                    Aware.joinStudy(getApplicationContext(), url);

                    Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, true);

                    // Probes
                    Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_BATTERY, true);
                    Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_COMMUNICATION_EVENTS, true);
                    Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_CALLS, true);
                    Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_MESSAGES, true);
                    Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_SCREEN, true);
                    Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_APPLICATIONS, true);
                    Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_INSTALLATIONS, true);
                    Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_NOTIFICATIONS, true);
                    Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_LOCATION_GPS, true);
                    Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_LOCATION_GPS, 600);
                    Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_LOCATION_NETWORK, true);
                    Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_LOCATION_NETWORK, 600);
                    Aware.setSetting(getApplicationContext(), Aware_Preferences.MIN_LOCATION_GPS_ACCURACY, 0);
                    Aware.setSetting(getApplicationContext(), Aware_Preferences.LOCATION_EXPIRATION_TIME, 0);
                    Aware.setSetting(getApplicationContext(), Aware_Preferences.LOCATION_SAVE_ALL, true);

                    // Clear (local) data after it has been synced.
                    Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_CLEAN_OLD_DATA, 4);

                    // TODO: configure ambient noise
                    Aware.setSetting(getApplicationContext(), com.aware.plugin.ambient_noise.Settings.STATUS_PLUGIN_AMBIENT_NOISE, true);
                    Aware.setSetting(getApplicationContext(), com.aware.plugin.ambient_noise.Settings.PLUGIN_AMBIENT_NOISE_NO_RAW, true);
                    Aware.setSetting(getApplicationContext(), com.aware.plugin.ambient_noise.Settings.FREQUENCY_PLUGIN_AMBIENT_NOISE, 30); // in minutes
                    Aware.setSetting(getApplicationContext(), com.aware.plugin.ambient_noise.Settings.PLUGIN_AMBIENT_NOISE_SAMPLE_SIZE, 20); // in seconds
                    Aware.startPlugin(getApplicationContext(), "com.aware.plugin.ambient_noise");

                    Aware.startPlugin(getApplicationContext(), "com.aware.plugin.hyks");

                    Toast.makeText(getApplicationContext(), R.string.core_thanks_for_joining, Toast.LENGTH_SHORT).show();
                }
            });

            sync_data = (Button) findViewById(R.id.sync_data);
            sync_data.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent sync = new Intent(Aware.ACTION_AWARE_SYNC_DATA);
                    sendBroadcast(sync);

                    Toast.makeText(getApplicationContext(), R.string.core_syncing_data, Toast.LENGTH_SHORT).show();
                }
            });

            set_schedule = (Button) findViewById(R.id.set_schedule);
            set_schedule.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setSchedule();
                    Toast.makeText(getApplicationContext(), R.string.core_starting_schedule, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        sendBroadcast(new Intent(Aware.ACTION_AWARE_PRIORITY_FOREGROUND));
    }

    private void setSchedule() {

        // TODO: make these hours configurable
        int startHour = getResources().getInteger(R.integer.default_start_time);
        int endHour   = getResources().getInteger(R.integer.default_end_time);
        String startHourStr = Aware.getSetting(getApplicationContext(), Settings.START_HOUR);
        String endHourStr   = Aware.getSetting(getApplicationContext(), Settings.END_HOUR);
        if (startHourStr.length() > 0)  startHour = Integer.parseInt(startHourStr);
        if (endHourStr.length()   > 0)  endHour   = Integer.parseInt(endHourStr) - 1;

        // Morning schedule
        try{
            Scheduler.Schedule schedule_morning = new Scheduler.Schedule("schedule_morning");
            schedule_morning
                    .addHour(startHour)
                    .addHour(startHour + 2)
                    .setInterval(60)
                    .setActionType(Scheduler.ACTION_TYPE_BROADCAST)
                    .setActionIntentAction("ESM_MORNING_TRIGGERED");

            Scheduler.saveSchedule(this, schedule_morning);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Evening schedule
        try{
            Scheduler.Schedule schedule_evening = new Scheduler.Schedule("schedule_evening");
            schedule_evening
                    .addHour(endHour)
                    .setInterval(60)
                    .setActionType(Scheduler.ACTION_TYPE_BROADCAST)
                    .setActionIntentAction("ESM_EVENING_TRIGGERED");

            Scheduler.saveSchedule(this, schedule_evening);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Random schedule
        // Delete already existing random schedules
        Context context = getApplicationContext();
        context.getContentResolver().delete(Scheduler_Provider.Scheduler_Data.CONTENT_URI, Scheduler_Provider.Scheduler_Data.SCHEDULE_ID + " LIKE 'schedule_olo%'", null);
        // This second one is for backwards compatibility.
        context.getContentResolver().delete(Scheduler_Provider.Scheduler_Data.CONTENT_URI, Scheduler_Provider.Scheduler_Data.SCHEDULE_ID + " LIKE 'schedule_random%'", null);
        // Set new schedule
        int olo1_start = startHour + 2;
        int olo3_end   = endHour - 2;
        int olo2_start = (int) round( olo1_start + (olo3_end-olo1_start)/3.);
        int olo3_start = (int) round( olo1_start + 2*(olo3_end-olo1_start)/3.);
        int olo1_end   = olo2_start-2;
        int olo2_end   = olo3_start - 2;
        try{
            Scheduler.Schedule schedule_random1 = new Scheduler.Schedule("schedule_olo1");
            Scheduler.Schedule schedule_random2 = new Scheduler.Schedule("schedule_olo2");
            Scheduler.Schedule schedule_random3 = new Scheduler.Schedule("schedule_olo3");
            schedule_random1.random(1, 30).addHour(olo1_start).addHour(olo1_end).setActionType(Scheduler.ACTION_TYPE_BROADCAST).setActionIntentAction("ESM_RANDOM_TRIGGERED");
            schedule_random2.random(1, 30).addHour(olo2_start).addHour(olo2_end).setActionType(Scheduler.ACTION_TYPE_BROADCAST).setActionIntentAction("ESM_RANDOM_TRIGGERED");
            schedule_random3.random(1, 30).addHour(olo3_start).addHour(olo3_end).setActionType(Scheduler.ACTION_TYPE_BROADCAST).setActionIntentAction("ESM_RANDOM_TRIGGERED");
            Scheduler.saveSchedule(this, schedule_random1);
            Scheduler.saveSchedule(this, schedule_random2);
            Scheduler.saveSchedule(this, schedule_random3);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // PHQ9 schedule
        try{
            Scheduler.Schedule schedule_biweekly = new Scheduler.Schedule("schedule_biweekly");
            schedule_biweekly
                    .setInterval(40320)
                    .setActionType(Scheduler.ACTION_TYPE_BROADCAST)
                    .setActionIntentAction("ESM_PHQ_TRIGGERED");
            for (int hour=startHour ; hour <= endHour ; hour++) {
                schedule_biweekly.addHour(hour);
            }

            Scheduler.saveSchedule(this, schedule_biweekly);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        device_id.setText("UUID: " + Aware.getSetting(this, Aware_Preferences.DEVICE_ID));
        if (Aware.isStudy(getApplicationContext())) {
            join_study.setEnabled(false);
            set_settings.setEnabled(false);
        } else {
            sync_data.setVisibility(View.INVISIBLE);
        }
    }

    /*
     * The Koota checksuming algorithm.  This is a two-digit base16 lund algorithm, basically.
     */
    static public boolean deviceidChecksumValid(String device_id) {
        int factor = 2;
        int sum = 0;     // Running checksum
        int base = 16;
        int digits = 2;  // number of checksum digits
        int base2 = (int)Math.pow(base, digits);

        sum = Integer.parseInt(device_id.substring(device_id.length()-digits), base);
        device_id = device_id.substring(0, device_id.length()-digits);
        for (int i = device_id.length()-1 ; i >= 0 ; i--) {
            int addend = factor * Integer.parseInt(device_id.substring(i, i+1), base);
            factor = 2 - factor + 1;
            addend = (addend / base2) + (addend % base2);
            sum += addend;
        }
        int remainder = sum % base2;
        return remainder == 0;
    }
}
