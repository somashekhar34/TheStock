package com.google.thestock;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
public class MainActivity extends AppCompatActivity implements Runnable {
    private static final String TF_MODEL_FILENAME = "file:///android_asset/goog1_tf_frozen.pb";
    private static final String INPUT_NODE_NAME_TF = "Placeholder";
    private static final String OUTPUT_NODE_NAME_TF = "preds";
    private static final int SEQ_LEN = 20;
    private TensorFlowInferenceInterface mInferenceInterface;
    private Button mButtonTF;
    private TextView mTextView;
    private boolean mUseTFModel;
    private String mResult;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mButtonTF = findViewById(R.id.tfbutton);
        mTextView = findViewById(R.id.textview);
        mTextView.setMovementMethod(new ScrollingMovementMethod());
        mButtonTF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUseTFModel = true;
                Thread thread = new Thread(MainActivity.this);
                thread.start();
            }
        });
    }
    @Override
    public void run() {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        mTextView.setText("Getting data...");
                    }
                });
        float[] floatValues  = new float[SEQ_LEN];

        try {
            URL url = new URL("https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=amzn&apikey=4SOSJM2XCRIB5IUS&datatype=csv&outputsize=compact");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            Scanner s = new Scanner(in).useDelimiter("\\n");
            mResult = "Last 20 Days:\n";
            if (s.hasNext()) s.next(); // get rid of the first title line
            List<String> priceList = new ArrayList<>();
            while (s.hasNext()) {
                String line = s.next();
                String[] items = line.split(",");
                priceList.add(items[4]);
            }

            for (int i=0; i<SEQ_LEN; i++)
                mResult += priceList.get(SEQ_LEN-i-1) + "\n";
            Log.d(">>>>", mResult);


            for (int i=0; i<SEQ_LEN; i++) {
                if (mUseTFModel)
                    floatValues[i] =  Float.parseFloat(priceList.get(SEQ_LEN-i-1));
            }
            AssetManager assetManager = getAssets();
            mInferenceInterface = new TensorFlowInferenceInterface(assetManager,  TF_MODEL_FILENAME );

            mInferenceInterface.feed( INPUT_NODE_NAME_TF , floatValues, 1, SEQ_LEN, 1);

            float[] predictions = new float[mUseTFModel ? SEQ_LEN : 1];

            mInferenceInterface.run(new String[] { OUTPUT_NODE_NAME_TF }, false);
            mInferenceInterface.fetch( OUTPUT_NODE_NAME_TF , predictions);
            if (mUseTFModel) {
                mResult += "\nPrediction with TF RNN model:\n" + predictions[SEQ_LEN - 1];
                Log.d(">>>", "" + predictions[SEQ_LEN - 1]);
            }

            runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            mTextView.setText(mResult);

                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
            return;
        } }


}

