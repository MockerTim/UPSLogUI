package asu.gubkin.ru.upslogui;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 *
 */
public class TemperatureChartFragment extends Fragment {

    private DbxAccountManager mDbxAcctMgr;

    private ArrayList<Date> datesList;

    private ArrayList<Integer> tempValuesList;

    private View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.temp_chart, container, false);

        mDbxAcctMgr = DbxAccountManager.getInstance(this.getActivity().getApplicationContext(), Constants.appKey, Constants.appSecret);

        updateData();

        return rootView;
    }

    public void onResume() {
        super.onResume();
        updateData();
    }

    /**
     *
     * @param chart
     */
    private void initChart(LineChart chart) {

        ArrayList<Entry> values = new ArrayList<>();

        for(int i = 0; i < tempValuesList.size(); i++) {
            values.add(new Entry((float)tempValuesList.get(i), i));
        }

        LineDataSet valuesSet = new LineDataSet(values, "T°");
        valuesSet.setColor(getResources().getColor(R.color.green));
        valuesSet.setCircleColor(getResources().getColor(R.color.green));
        valuesSet.setLineWidth(3);

        ArrayList<LineDataSet> tempDataSets = new ArrayList<>();
        tempDataSets.add(valuesSet);

        ArrayList<String> xVals = new ArrayList<>();

        for(Date d: datesList) {
            DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");
            String dStr = formatter.format(d);
            xVals.add(dStr);
        }

        LineData data = new LineData(xVals, tempDataSets);

        chart.setData(data);
    }


    /**
     *
     * @param fileName
     * @param dateFormat
     */
    private boolean initDateTempValueMap(String fileName, String dateFormat) {
        boolean res = false;

        if(mDbxAcctMgr.hasLinkedAccount()) {

            try {
                // Create DbxFileSystem for synchronized file access.
                DbxFileSystem dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());

                DbxFile upsLogFile = dbxFs.open(new DbxPath(fileName));
                try {
                    InputStream in = upsLogFile.getReadStream();

                    BufferedReader fIn = new BufferedReader(new InputStreamReader(in));

                    String lineStr;

                    datesList = new ArrayList<>();
                    tempValuesList = new ArrayList<>();

                    String pairStr;
                    while ((lineStr = fIn.readLine()) != null) {
                        pairStr = lineStr.substring(1, lineStr.lastIndexOf("]"));
                        String[] pairArray = pairStr.split(", ");

                        String someDateStr = pairArray[0].substring(1, pairArray[0].lastIndexOf("\""));
                        Integer someInt = Integer.parseInt(pairArray[1]);
                        DateFormat formatter = new SimpleDateFormat(dateFormat);
                        Date someDate = formatter.parse(someDateStr);

                        datesList.add(someDate);
                        tempValuesList.add(someInt);
                    }

                    res = true;
                } finally {
                    upsLogFile.close();
                }
            } catch (ParseException e) {
                System.err.println(e);
            } catch (IOException e) {
                System.err.println(e);
            }
        } else {
            res = false;
        }

        return res;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(Constants.D) Log.d(Constants.TAG, "[ForecastFragment.onOptionsItemSelected()]");

        boolean result = false;

        int id = item.getItemId();

        if(Constants.D) Log.d(Constants.TAG, "id = " + id);

        if(id == R.id.action_update) {
            if(Constants.D) Log.d(Constants.TAG, "Executing the FetchWeatherTask.");

            updateData();

            result = true;
        }

        if(Constants.D) Log.d(Constants.TAG, "result = " + result);

        if(Constants.D) Log.d(Constants.TAG, "<[ForecastFragment.onOptionsItemSelected()]");

        return result;
    }

    private void updateData() {
        FetchLogDataTask task = new FetchLogDataTask();

        String fileName = "logups22";
        String dateFormat = "yyyy/MM/dd HH:mm:ss z";

        task.execute(fileName, dateFormat);
    }

    public class FetchLogDataTask extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected void onPostExecute(Boolean result) {

            if(result) {

                LineChart chart = (LineChart) rootView.findViewById(R.id.temp_chart);

                initChart(chart);

                chart.invalidate();
            }
        }

        @Override
        protected Boolean doInBackground(String... params) {
            boolean result = initDateTempValueMap(params[0], params[1]);

            return result;
        }
    }
}