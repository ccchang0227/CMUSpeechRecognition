package com.realtouchapp.cmuspeech2;

import com.realtouchapp.common.BaseActivity;

import java.io.File;
import java.io.IOException;

import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

public class MainActivity extends BaseActivity {

    @Override
    protected void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "zh_broadcastnews_ptm256_8000"))
                .setDictionary(new File(assetsDir, "zh_broadcastnews_utf8.dic"))
                .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                .getRecognizer();
        recognizer.addListener(this);

        /* In your application you might not need to add all those searches.
         * They are added here for demonstration. You can leave just one.
         */

        // Create language model search
        File languageModel = new File(assetsDir, "zh_broadcastnews_64000_utf8.lm.bin");
        recognizer.addNgramSearch(KEY_CHINESE_SEARCH, languageModel);

//        File languageModel = new File(assetsDir, "keyword_list");
//        recognizer.addKeywordSearch(KEY_CHINESE_SEARCH, languageModel);

        // Create keyword-activation search.
//        recognizer.addKeyphraseSearch(KEY_CHINESE_SEARCH, "測試");
    }

}
