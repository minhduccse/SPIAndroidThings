package com.example.minhduc.spiandroidthings;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.galarzaa.androidthings.Rc522;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;

public class MainActivity extends Activity {
    private static final String SPI_PORT = "SPI0.0";
    private static final String PIN_RESET = "BCM25";
    private static final String PIN_LED = "BCM2";
    private static final String PIN_GREEN = "BCM3";
    private static final String PIN_BLUE = "BCM4";

    private static final String TAG_GLOB = "RFID";

    private static final String Url = "http://demo1.chipfc.com/SensorValue/update?sensorid=7&sensorvalue=";

    private String[] memberIDList = {"1610800         ", "1612939         ", "1612483         ", "1613786         "};

    private int ledState = 2;
    private int blinkCounter = 0;

    boolean isFound = false;

    RfidWriteTask mRfidWriteTask;
    RfidReadTask mRfidReadTask;
    String resultsText = "";



    private Rc522 mRc522;

    private Handler mHandler = new Handler();

    private TextView mTagDetectedView;
    private TextView mTagUidView;
    private TextView mTagResultsView;
    private TextView mStatus;

    private Button button_write;
    private Button button_read;
    private Button button_stop;

    private SpiDevice spiDevice;

    private Gpio gpioReset;
    private Gpio mLedGpioRed;
    private Gpio mLedGpioGreen;
    private Gpio mLedGpioBlue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTagDetectedView = (TextView) findViewById(R.id.tag_read);
        mTagUidView = (TextView) findViewById(R.id.tag_uid);
        mTagResultsView = (TextView) findViewById(R.id.tag_results);
        mStatus = (TextView) findViewById(R.id.status);

        button_write = (Button) findViewById(R.id.button_write);
        button_read = (Button) findViewById(R.id.button_read);
        button_stop = (Button) findViewById(R.id.button_stop);

        mStatus.setVisibility(View.VISIBLE);

        button_write.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRfidWriteTask = new RfidWriteTask(mRc522);
                mRfidWriteTask.execute();
                mStatus.setText(R.string.waiting);
            }
        });

        button_read.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRfidReadTask = new RfidReadTask(mRc522);
                mRfidReadTask.execute();
                mStatus.setText(R.string.waiting);
            }
        });

        button_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mRfidReadTask != null) mRfidReadTask.cancel(true);
                if(mRfidWriteTask != null) mRfidWriteTask.cancel(true);
                button_write.setEnabled(true);
                button_read.setEnabled(true);
            }
        });

        PeripheralManager pioService = PeripheralManager.getInstance();
        try {
            spiDevice = pioService.openSpiDevice(SPI_PORT);
            gpioReset = pioService.openGpio(PIN_RESET);
            mLedGpioRed = pioService.openGpio(PIN_LED);
            mLedGpioGreen = pioService.openGpio(PIN_GREEN);
            mLedGpioBlue = pioService.openGpio(PIN_BLUE);

            mLedGpioRed.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
            mLedGpioGreen.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
            mLedGpioBlue.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);

            mLedGpioRed.setActiveType(Gpio.ACTIVE_LOW);
            mLedGpioGreen.setActiveType(Gpio.ACTIVE_LOW);
            mLedGpioBlue.setActiveType(Gpio.ACTIVE_LOW);

            mRc522 = new Rc522(spiDevice, gpioReset);
            mRc522.setDebugging(true);
        } catch (IOException e) {
            Log.e(TAG_GLOB, "Error on opening GPIO ports.");
        }

        mHandler.post(mLedBlink);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (spiDevice != null) {
                spiDevice.close();
            }
            if (gpioReset != null) {
                gpioReset.close();
            }
            if (mLedGpioRed != null) {
                mLedGpioRed.close();
            }
            if (mLedGpioGreen != null) {
                mLedGpioGreen.close();
            }
            if (mLedGpioBlue != null) {
                mLedGpioBlue.close();
            }
        } catch (IOException e) {
            Log.e(TAG_GLOB, "Error on closing GPIO ports.");
        } finally {
            spiDevice = null;
            gpioReset = null;
            mLedGpioRed = null;
            mLedGpioGreen = null;
            mLedGpioBlue = null;
        }
    }

    private Runnable mLedBlink = new Runnable() {
        @Override
        public void run() {
            try {
                switch (ledState) {
                    case 0:
                        mLedGpioRed.setValue(false);
                        mLedGpioGreen.setValue(true);
                        mLedGpioBlue.setValue(false);
                        ledState = 2;
                        break;
                    case 1:
                        if (blinkCounter < 6) {
                            mLedGpioRed.setValue(true);
                            mLedGpioGreen.setValue(false);
                            mLedGpioBlue.setValue(false);
                            blinkCounter++;
                            ledState = 3;
                        }
                        else {
                            ledState = 2;
                            blinkCounter = 0;
                        }
                        break;
                    case 2:
                        mLedGpioRed.setValue(false);
                        mLedGpioGreen.setValue(false);
                        mLedGpioBlue.setValue(true);
                        break;
                    case 3:
                        mLedGpioRed.setValue(false);
                        mLedGpioGreen.setValue(false);
                        mLedGpioBlue.setValue(false);
                        ledState = 1;
                        break;
                    default:
                        ledState = 2;
                        break;
                }
            } catch (Exception e) {
                Log.w(TAG_GLOB, "Display LED error");
            }
            mHandler.postDelayed(this, 100);
        }
    };

    private class RfidWriteTask extends AsyncTask<Object, Object, Boolean> {
        private static final String TAG = "RfidWriteTask";
        private Rc522 rc522;

        RfidWriteTask(Rc522 rc522) {
            this.rc522 = rc522;
        }

        @Override
        protected void onPreExecute() {
            button_write.setEnabled(false);
            button_read.setEnabled(false);
            mTagResultsView.setVisibility(View.GONE);
            mTagDetectedView.setVisibility(View.GONE);
            mTagUidView.setVisibility(View.GONE);
            resultsText = "";
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            rc522.stopCrypto();
            while (true) {
                if(isCancelled()){
                    break;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return null;
                }
                //Check if a RFID tag has been found
                if (!rc522.request()) {
                    continue;
                }
                //Check for collision errors
                if (!rc522.antiCollisionDetect()) {
                    continue;
                }
                byte[] uuid = rc522.getUid();
                return rc522.selectTag(uuid);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if(isCancelled()){
                return;
            }
            if (!success) {
                mTagResultsView.setText(R.string.unknown_error);
                mStatus.setText(R.string.fail);
                return;
            }

            byte address_name = Rc522.getBlockAddress(15, 0);
            byte address_dob = Rc522.getBlockAddress(15, 1);
            byte address_id = Rc522.getBlockAddress(15, 2);

            writeToRFID("Tran Minh Duc", "08/06/1998", "1610800", address_name, address_dob, address_id);

        }
    }

    private class RfidReadTask extends AsyncTask<Object, Object, Boolean> {
        private static final String TAG = "RfidReadTask";
        private Rc522 rc522;

        RfidReadTask(Rc522 rc522) {
            this.rc522 = rc522;
        }

        @Override
        protected void onPreExecute() {
            button_write.setEnabled(false);
            button_read.setEnabled(false);
            mTagResultsView.setVisibility(View.GONE);
            mTagDetectedView.setVisibility(View.GONE);
            mTagUidView.setVisibility(View.GONE);
            resultsText = "";
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            rc522.stopCrypto();
            while (true) {
                if(isCancelled()){
                    break;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return null;
                }
                //Check if a RFID tag has been found
                if (!rc522.request()) {
                    continue;
                }
                //Check for collision errors
                if (!rc522.antiCollisionDetect()) {
                    continue;
                }
                byte[] uuid = rc522.getUid();
                return rc522.selectTag(uuid);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if(isCancelled()){
                return;
            }
            if (!success) {
                mTagResultsView.setText(R.string.unknown_error);
                mStatus.setText(R.string.fail);
                return;
            }

            byte address_name = Rc522.getBlockAddress(15, 0);
            byte address_dob = Rc522.getBlockAddress(15, 1);
            byte address_id = Rc522.getBlockAddress(15, 2);

            readFromRFID(address_name, address_dob, address_id);
        }
    }

    private void writeToRFID(String Name, String DOB, String ID, byte blockName, byte blockDOB, byte blockID){
        String tmp = Name;
        for (int i = Name.length(); i < 16; i++) {
            tmp = tmp + " ";
        }
        byte[] name = tmp.getBytes();

        tmp = DOB;
        for (int i = DOB.length(); i < 16; i++) {
            tmp = tmp + " ";
        }
        byte[] dob = tmp.getBytes();

        tmp = ID;
        for (int i = ID.length(); i < 16; i++) {
            tmp = tmp + " ";
        }
        byte[] id = tmp.getBytes();

        try {
            byte[] key = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

            boolean result = mRc522.authenticateCard(Rc522.AUTH_A, blockName, key);
            if (!result) {
                mTagResultsView.setText(R.string.authetication_error);
                mStatus.setText(R.string.fail);
                return;
            }
            result = mRc522.writeBlock(blockName, name);
            if (!result) {
                mTagResultsView.setText(R.string.write_error);
                mStatus.setText(R.string.fail);
                return;
            }

            result = mRc522.authenticateCard(Rc522.AUTH_A, blockDOB, key);
            if (!result) {
                mTagResultsView.setText(R.string.authetication_error);
                mStatus.setText(R.string.fail);
                return;
            }
            result = mRc522.writeBlock(blockDOB, dob);
            if (!result) {
                mTagResultsView.setText(R.string.write_error);
                mStatus.setText(R.string.fail);
                return;
            }

            result = mRc522.authenticateCard(Rc522.AUTH_A, blockID, key);
            if (!result) {
                mTagResultsView.setText(R.string.authetication_error);
                mStatus.setText(R.string.fail);
                return;
            }
            result = mRc522.writeBlock(blockID, id);
            if (!result) {
                mTagResultsView.setText(R.string.write_error);
                mStatus.setText(R.string.fail);
                return;
            }
            resultsText += "Sector written successfully";

            mRc522.stopCrypto();
            mTagResultsView.setText(resultsText);
            mStatus.setText(R.string.success);
        } finally {
            button_write.setEnabled(true);
            button_read.setEnabled(true);

            mTagUidView.setText(getString(R.string.tag_uid, mRc522.getUidString()));
            mTagResultsView.setVisibility(View.VISIBLE);
            mTagDetectedView.setVisibility(View.VISIBLE);
            mTagUidView.setVisibility(View.VISIBLE);
        }
    }

    private void readFromRFID(byte BlockName, byte BlockDOB, byte BlockID){

        try {
            byte[] key = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

            boolean result = mRc522.authenticateCard(Rc522.AUTH_A, BlockName, key);
            if (!result) {
                mTagResultsView.setText(R.string.authetication_error);
                mStatus.setText(R.string.fail);
                return;
            }
            result = mRc522.authenticateCard(Rc522.AUTH_A, BlockDOB, key);
            if (!result) {
                mTagResultsView.setText(R.string.authetication_error);
                mStatus.setText(R.string.fail);
                return;
            }
            result = mRc522.authenticateCard(Rc522.AUTH_A, BlockID, key);
            if (!result) {
                mTagResultsView.setText(R.string.authetication_error);
                mStatus.setText(R.string.fail);
                return;
            }

            isFound = true;

            byte[] bufferName = new byte[16];
            result = mRc522.readBlock(BlockName, bufferName);
            if (!result) {
                mTagResultsView.setText(R.string.read_error);
                return;
            }
            String ResultName = new String(bufferName);
            resultsText += "Name: " + "\t\t\t\t\t\t" + ResultName;

            byte[] bufferDOB = new byte[16];
            result = mRc522.readBlock(BlockDOB, bufferDOB);
            if (!result) {
                mTagResultsView.setText(R.string.read_error);
                return;
            }
            String ResultDOB = new String(bufferDOB);
            resultsText += "\nDate of birth: " + "\t\t" + ResultDOB;

            byte[] bufferID = new byte[16];
            result = mRc522.readBlock(BlockID, bufferID);
            if (!result) {
                mTagResultsView.setText(R.string.read_error);
                return;
            }
            String ResultID = new String(bufferID);
            resultsText += "\nID: " + "\t\t\t\t\t\t\t\t\t" + ResultID;

            mRc522.stopCrypto();

            if (isFound) {
                for (int i = 0; i < 4; i++) {
                    if (ResultID.equals(memberIDList[i])) {
                        ledState = 0;
                        isFound = false;
                        break;
                    }
                    blinkCounter = 0;
                }
                if(isFound) ledState = 1;
                isFound = false;
                Log.i(TAG_GLOB, "/" + ResultID + "/");
            }

            mTagResultsView.setText(resultsText);



            mStatus.setText(R.string.success);
        } finally {
            button_write.setEnabled(true);
            button_read.setEnabled(true);

            mTagUidView.setText(getString(R.string.tag_uid, mRc522.getUidString()));
            mTagResultsView.setVisibility(View.VISIBLE);
            mTagDetectedView.setVisibility(View.VISIBLE);
            mTagUidView.setVisibility(View.VISIBLE);
        }
    }
}
