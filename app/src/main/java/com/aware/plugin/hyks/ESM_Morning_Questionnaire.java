package com.aware.plugin.hyks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.aware.ESM;
import com.aware.providers.ESM_Provider;
import com.aware.ui.esms.ESMFactory;
import com.aware.ui.esms.ESM_Freetext;
import com.aware.ui.esms.ESM_Likert;
import com.aware.ui.esms.ESM_Radio;

import org.json.JSONException;

import java.util.Calendar;

/**
 * Created by niels on 10/03/2017.
 */

public class ESM_Morning_Questionnaire extends BroadcastReceiver {

    boolean morningAnsweredToday = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        if(!intent.getAction().equals("ESM_MORNING_TRIGGERED")) {
            return;
        }

        Log.d("Niels", "Trigger morning received");

        morningAnsweredToday = false;

        // Check if morning questionnaire has been answered
        try {
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY,0);
            today.set(Calendar.MINUTE,0);
            today.set(Calendar.SECOND,0);

            Cursor esm_data = context.getContentResolver().query(ESM_Provider.ESM_Data.CONTENT_URI, null,
                    ESM_Provider.ESM_Data.TIMESTAMP + ">=" + today.getTimeInMillis() +
                            " AND " + ESM_Provider.ESM_Data.TRIGGER + " LIKE 'Morning'" +
                            " AND " + ESM_Provider.ESM_Data.STATUS + "=" + ESM.STATUS_ANSWERED, null, null);
            if (esm_data != null) {
                if (esm_data.getCount() > 1) {
                    // Morning has been answered today
                    morningAnsweredToday = true;
                }
            }
            else Log.d("Niels", "cursor is null");
            if(esm_data != null && !esm_data.isClosed()) {
                esm_data.close();
            }
        }
        catch (NullPointerException e) {
            e.printStackTrace();
        }

        // Time to launch an ESM
        if (!morningAnsweredToday) {
            buildMorningESM(context);
        }
    }

    private void buildMorningESM(Context context) {
        try {
            ESMFactory factory = new ESMFactory();

            ESM_Radio morning_1 = new ESM_Radio();
            morning_1.addRadio("0-15 min")
                    .addRadio("15-30 min")
                    .addRadio("30-60 min")
                    .addRadio("yli 60 min")
                    .setTitle(context.getResources().getString(R.string.morning_1))
                    .setTrigger("Morning")
                    .setSubmitButton("OK")
                    .setInstructions("");
            factory.addESM(morning_1);

            ESM_Radio morning_2 = new ESM_Radio();
            morning_2.addRadio("0-1")
                    .addRadio("2-3")
                    .addRadio("4-5")
                    .addRadio("enemmän kuin 5")
                    .setTitle(context.getResources().getString(R.string.morning_2))
                    .setTrigger("Morning")
                    .setSubmitButton("OK")
                    .setInstructions("");
            factory.addESM(morning_2);

            ESM_Radio morning_3 = new ESM_Radio();
            morning_3.addRadio("0-15 min")
                    .addRadio("15-30 min")
                    .addRadio("30-60 min")
                    .addRadio("yli 60 min")
                    .setTitle(context.getResources().getString(R.string.morning_3))
                    .setTrigger("Morning")
                    .setSubmitButton("OK")
                    .setInstructions("");
            factory.addESM(morning_3);

            ESM_Likert morning_4 = new ESM_Likert();
            morning_4.setLikertMax(7)
                    .setLikertMaxLabel("KYLLÄ")
                    .setLikertMinLabel("EI")
                    .setLikertStep(1)
                    .setTitle(context.getResources().getString(R.string.morning_4))
                    .setInstructions("")
                    .setSubmitButton("OK");
            factory.addESM(morning_4);

            ESM_Freetext morning_5 = new ESM_Freetext();
            morning_5.setTitle(context.getResources().getString(R.string.morning_5))
                    .setInstructions("")
                    .setSubmitButton("OK");
            factory.addESM(morning_5);

            ESM_Freetext morning_6 = new ESM_Freetext();
            morning_6.setTitle(context.getResources().getString(R.string.morning_6))
                    .setInstructions("")
                    .setSubmitButton("OK");
            factory.addESM(morning_6);

            //Queue them
            Intent queueESM = new Intent(ESM.ACTION_AWARE_QUEUE_ESM);
            queueESM.putExtra(ESM.EXTRA_ESM, factory.build());
            context.sendBroadcast(queueESM);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
