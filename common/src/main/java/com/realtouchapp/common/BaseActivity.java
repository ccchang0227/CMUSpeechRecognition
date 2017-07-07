package com.realtouchapp.common;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;

import static android.widget.Toast.makeText;

public abstract class BaseActivity extends Activity implements RecognitionListener {
    private static final String TAG = BaseActivity.class.getSimpleName();

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    protected static final String KEY_CHINESE_SEARCH = "zh_tw";

    protected SpeechRecognizer recognizer;

    protected TextView tv_message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        tv_message = (TextView)findViewById(R.id.tv_message);
        tv_message.setText("-- Preparing the recognizer\n");

        View btn_clear_console = findViewById(R.id.btn_clear_console);
        btn_clear_console.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BaseActivity.this.tv_message.setText("");
            }
        });

        // Check if user has given permission to record audio
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }
        runRecognizerSetup();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                runRecognizerSetup();
            }
            else {
                finish();
            }
        }
    }

    // MARK - Private

    private void runRecognizerSetup() {
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(BaseActivity.this);
                    File assetDir = assets.syncAssets();
                    BaseActivity.this.setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    result.printStackTrace();

                    Log.e(BaseActivity.TAG, "Failed to init recognizer: " + result.getLocalizedMessage());
                    BaseActivity.this.tv_message.append("-- Failed to init recognizer " + result.getLocalizedMessage() + "\n");
                }
                else {
                    Log.e(BaseActivity.TAG, "Init recognizer successful");
                    BaseActivity.this.tv_message.append("-- Init recognizer successful\n");

                    startSearch(KEY_CHINESE_SEARCH);
                }
            }
        }.execute();
    }

    // MARK - Protected

    protected abstract void setupRecognizer(File assetsDir) throws IOException;

    protected void startSearch(String searchName) {
        recognizer.stop();

        recognizer.startListening(searchName);

        tv_message.append("-- Please say something...\n");
    }

    // MARK - RecognitionListener

    @Override
    public void onBeginningOfSpeech() {
        Log.e(TAG, "onBeginningOfSpeech");
        tv_message.append("-- onBeginningOfSpeech\n");
    }

    /**
     * We stop recognizer here to get a final result
     */
    @Override
    public void onEndOfSpeech() {
        Log.e(TAG, "onEndOfSpeech");
        tv_message.append("-- onEndOfSpeech\n");
    }

    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     */
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null) {
            return;
        }

        String text = hypothesis.getHypstr();
        tv_message.append("Recognized(onPartialResult): " + text + "\n");

        startSearch(KEY_CHINESE_SEARCH);
    }

    /**
     * This callback is called when we stop the recognizer.
     */
    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            String text = hypothesis.getHypstr();
            tv_message.append("Recognized(onResult): " + text + "\n");
            makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onError(Exception e) {
        e.printStackTrace();

        Log.e(TAG, "onError: " + e.getLocalizedMessage());
        tv_message.append("-- onError: " + e.getLocalizedMessage() + "\n");
    }

    @Override
    public void onTimeout() {
        Log.e(TAG, "onTimeout");
        startSearch(KEY_CHINESE_SEARCH);
    }

}
