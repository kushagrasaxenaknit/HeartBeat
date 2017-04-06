package com.kushagrasaxena.heartbeat;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;


import android.view.View;

import android.widget.TextView;
import android.widget.Toast;

import com.codekidlabs.storagechooser.StorageChooser;

import android.widget.EditText;

import com.nihaskalam.progressbuttonlibrary.CircularProgressButton;


import java.io.File;
import java.io.FileInputStream;

import java.io.InputStreamReader;

import java.text.SimpleDateFormat;

import java.util.Date;


import au.com.bytecode.opencsv.CSVReader;
import es.dmoral.toasty.Toasty;

public class MainActivity extends AppCompatActivity {
    CircularProgressButton file, getHeartRate;
    String fileimage;
    double threshold = 0.28;//default threshold value
    private TextView tvMainSelectedCate;
    private EditText editText;
    private boolean isFile = false; //to check if file is selected or not

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        file = (CircularProgressButton) findViewById(R.id.file);

        getHeartRate = (CircularProgressButton) findViewById(R.id.task);
        editText = (EditText) findViewById(R.id.editText);


        //Called when Open File IS pressed
        file.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final StorageChooser filechooser = new StorageChooser.Builder()
                        .withActivity(MainActivity.this)
                        .withFragmentManager(getSupportFragmentManager())
                        .allowCustomPath(true).allowAddFolder(true)
                        .withMemoryBar(true).setType(StorageChooser.FILE_PICKER)
                        .build();
                filechooser.show();
                filechooser.setOnSelectListener(new StorageChooser.OnSelectListener() {
                    @Override
                    public void onSelect(String s) {
                        file.showProgress();
                        fileimage = s;
                        isFile = true;
                    }
                });
            }
        });

        //called when get heart beat is pressed
        getHeartRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tvMainSelectedCate.setText("");   // clear text view
                if (isFile) {
                    result(fileimage);
                } else {
                    Toasty.error(getApplicationContext(), "No file selected ?", Toast.LENGTH_LONG).show();
                }
            }
        });

        tvMainSelectedCate = (TextView) findViewById(R.id.tvMainSelectedCate);


    }

    //checks if file has .csv extension
    public void result(String path) {
        if (path.substring(path.length() - 3, path.length()).compareTo("csv") == 0) {
            Toasty.error(getApplicationContext(), "Getting Heart Beat ..", Toast.LENGTH_LONG).show();
            getHeartBeat(path);
        } else {
            Toasty.error(getApplicationContext(), "File Format is not .csv", Toast.LENGTH_LONG).show();
        }


    }

    //this function calculate heart beat by calculating time difference between two successive peaks in ecg curve
    public void getHeartBeat(String path) {
        if (editText.getText().toString().trim().length() != 0)//change threshold if user has given new value
        {
            threshold = Double.valueOf(editText.getText().toString().trim());
        }
        String next[] = {};
        try {
            FileInputStream inputStream = new FileInputStream(new File(path));
            InputStreamReader csvStreamReader = new InputStreamReader(inputStream);


            CSVReader reader = new CSVReader(csvStreamReader);
            next = reader.readNext();  // to ignore header line of .csv file

            String lastPeak = "", currentPeak = "";
            double previousEcgValue = 0.0, maxEcgValue = threshold, ecgValue = 0.0;

            while (true) {
                next = reader.readNext();
                if (next != null) {
                    String currentTime = next[0].trim().substring(1, next[0].length() - 2);//ignores []' present in timestamp value

                    if (next[1].trim().compareTo("-") == 0)//checks if data is missing it is replaced by last present ecg value
                    {
                        ecgValue = previousEcgValue;
                    } else {
                        ecgValue = Double.valueOf(next[1]);
                    }


                    if (ecgValue > threshold)//if ecg value greater than threshold than check for peaks
                    {
                        if (ecgValue > maxEcgValue) {
                            maxEcgValue = ecgValue;
                            currentPeak = currentTime;

                        }

                    }
                    if (ecgValue < 0.0)//if ecg goes -ve then check for two peaks time difference
                    {
                        if (currentPeak != "") {
                            if (lastPeak != "") {
                                double timeDifference = getTimeDifference(lastPeak, currentPeak);
                                timeDifference = timeDifference / 1000;  //to convert time in seconds
                                long numOfBeats= (long) (60.0 / timeDifference);//this gives number of heart beats in one minute


                                tvMainSelectedCate.setText(tvMainSelectedCate.getText() + "\n" + numOfBeats);

                                lastPeak = currentPeak;
                                currentPeak = "";
                                maxEcgValue = threshold;
                            } else {
                                lastPeak = currentPeak;
                                currentPeak = "";
                                maxEcgValue = threshold;
                            }
                        }
                    }


                    previousEcgValue = ecgValue;


                } else {  //to terminate infinite while loop when reach end of file
                    break;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            Toasty.error(getApplicationContext(), "file error", Toast.LENGTH_LONG).show();
        }

    }


    @Override
    public void onDestroy() {
        // Don't forget to shutdown
        super.onDestroy();
    }


    //returns time difference in milliseconds between two timestamps
    private long getTimeDifference(String one, String two) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS dd/MM/yyyy");
        Date d1 = null;
        Date d2 = null;
        long diff = 0;
        try {
            d1 = format.parse(one);
            d2 = format.parse(two);

            //in milliseconds
            diff = d2.getTime() - d1.getTime();


        } catch (Exception e) {
            e.printStackTrace();
        }
        return diff;
    }

}