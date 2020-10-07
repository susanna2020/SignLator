package com.example.signlator;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Scroller;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class SignToText extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {

    private static final int BUTTON_SIZE = 80;
    private static final int CLASSIFY_INTERVAL = 20;
    private static final String CAPTURE_BUTTON = "captureButton.png";
    private static final String DEBUG_BUTTON = "debugButton.png";
    private static final String EDGE_BUTTON = "edgeButton.png";
    private static final String HELP_BUTTON = "helpButton.png";
    private static final String SAVE_BUTTON = "saveButton.png";
    private static final String SPEAK_BUTTON = "blue_speaker.png";
    private static final String TAG = "RecognitionActivity";
    private static final String DEL_BUTTON = "reset.png";


    private Classifier classifier;
    private Mat frame;
    private Mat mRGBA;
    private JavaCameraView openCvCameraView;

    private LinearLayout buttonLayout;
    private LinearLayout debugLayout;
    private LinearLayout button2Layout;
    private LinearLayout speakerLayout;
    private LinearLayout resBtnLayout;
    private LinearLayout resetLayout;
    private TextView probTextView;
    private TextView resultTextView;
    private AlertDialog dialog;

    private Boolean isDebug = false;
    private Boolean isEdge = false;
    private Boolean isSave = false;

    private int counter = 0;

    String text = "";

//    ImageButton voice;
    TextToSpeech textToSpeech;
    EditText ed1 ;

    private BaseLoaderCallback baseloadercallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            if (status == BaseLoaderCallback.SUCCESS)
                openCvCameraView.enableView();
            else
                super.onManagerConnected(status);
        }
    };
//    public void voice(View view) {
//        String text = (String) resultTextView.getText();
//        int speech =textToSpeech.speak(text,TextToSpeech.QUEUE_FLUSH,null);
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR){
                    textToSpeech.setLanguage(Locale.ENGLISH);
                }
            }
        });

        final FrameLayout layout = new FrameLayout(this);
        layout.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(layout);

        int mCameraIndex = 0;
        openCvCameraView = new JavaCameraView(this, mCameraIndex);
        openCvCameraView.setCvCameraViewListener(SignToText.this);
        openCvCameraView.setVisibility(SurfaceView.VISIBLE);
        openCvCameraView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        layout.addView(openCvCameraView);

        buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        debugLayout = new LinearLayout(this);
        debugLayout.setOrientation(LinearLayout.HORIZONTAL);
        debugLayout.setVisibility(View.INVISIBLE);

//        debugLayout.addView(createButton(CAPTURE_BUTTON));
//        debugLayout.addView(createButton(SAVE_BUTTON));
//        debugLayout.addView(createButton(EDGE_BUTTON));
        buttonLayout.addView(debugLayout);
//        buttonLayout.addView(createButton(DEBUG_BUTTON));
        buttonLayout.addView(createButton(HELP_BUTTON));
        buttonLayout.setPadding(25, 25, 25, 25);
        buttonLayout.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP + Gravity.START));
        layout.addView(buttonLayout);

        FrameLayout.LayoutParams resultTextLayout = new FrameLayout.LayoutParams(1000,
                150,
                Gravity.BOTTOM + Gravity.CENTER_HORIZONTAL);
        resultTextLayout.bottomMargin = 20 ;

        resultTextView = new TextView(this);
        resultTextView.setBackgroundResource(R.color.background_light);

        resultTextView.setTextColor(0xAA145374);
        resultTextView.setTextSize(30f);
        resultTextView.setLayoutParams(resultTextLayout);
        layout.addView(resultTextView);


        button2Layout = new LinearLayout(this);
        button2Layout.setOrientation(LinearLayout.HORIZONTAL);
        speakerLayout = new LinearLayout(this);
        speakerLayout.setOrientation(LinearLayout.HORIZONTAL);
//        speakerLayout.setVisibility(View.INVISIBLE);
        button2Layout.addView(speakerLayout);
        button2Layout.addView(createButton(SPEAK_BUTTON));
        button2Layout.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM + Gravity.END)

        );
        button2Layout.setPadding(0,0,40,20);
        layout.addView(button2Layout);

        resBtnLayout = new LinearLayout(this);
        resBtnLayout.setOrientation(LinearLayout.HORIZONTAL);
        resetLayout = new LinearLayout(this);
        resetLayout.setOrientation(LinearLayout.HORIZONTAL);
        resBtnLayout.addView(resetLayout);
        resBtnLayout.addView(createButton(DEL_BUTTON));
        resBtnLayout.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM + Gravity.END)
        );
        resBtnLayout.setPadding(0,0,160,20);
        layout.addView(resBtnLayout);


        dialog = new AlertDialog.Builder(this)
                .setTitle("Help")
                .setMessage("Make sure sign is fully inside green box.  For best results, use in a well lit area with an empty background.")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        int windowVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_FULLSCREEN;
                        getWindow().getDecorView().setSystemUiVisibility(windowVisibility);
                    }
                })
                .create();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "Connected camera.");
            baseloadercallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        } else {
            Log.d(TAG, "Camera not connected.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseloadercallback);
        }

        int windowVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        getWindow().getDecorView().setSystemUiVisibility(windowVisibility);

        try {
            classifier = new Classifier(this);
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize classifier", e);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (openCvCameraView != null) {
            openCvCameraView.disableView();
        }
        if (classifier != null) {
            classifier.close();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (openCvCameraView != null) {
            openCvCameraView.disableView();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRGBA = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        if (mRGBA != null) mRGBA.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        float mh = mRGBA.height();
        float cw = (float) Resources.getSystem().getDisplayMetrics().widthPixels;
        float scale = mh / cw * 0.7f;

        mRGBA = inputFrame.rgba();
        frame = classifier.processMat(mRGBA);

        if (!isDebug) {
            if (counter == CLASSIFY_INTERVAL) {
                runInterpreter();
                counter = 0;
            } else {
                counter++;
            }
        }

        Imgproc.rectangle(mRGBA,
                new Point(mRGBA.cols() / 2f - (mRGBA.cols() * scale / 2),
                        mRGBA.rows() / 2f - (mRGBA.cols() * scale / 2)),
                new Point(mRGBA.cols() / 2f + (mRGBA.cols() * scale / 2),
                        mRGBA.rows() / 2f + (mRGBA.cols() * scale / 2)),
                new Scalar(0, 255, 0), 1);
        if (isEdge) {
            mRGBA = classifier.debugMat(mRGBA);
        }

        System.gc();
        return mRGBA;
    }


    @Override
    public void onClick(View view) {
        switch ((String) view.getTag()) {
            case HELP_BUTTON:
                dialog.show();
                TextView textView = dialog.findViewById(android.R.id.message);
                assert textView != null;
                textView.setScroller(new Scroller(this));
                textView.setVerticalScrollBarEnabled(true);
                textView.setMovementMethod(new ScrollingMovementMethod());
                break;
            case SPEAK_BUTTON:
                String newText = (String) resultTextView.getText();
                int speech =textToSpeech.speak(newText,TextToSpeech.QUEUE_ADD,null);
                break;

            case DEL_BUTTON:
                resultTextView.setText("");
                text = "" ;
                break;

        }
    }


    private ImageButton createButton(String tag) {
        ImageButton button = new ImageButton(this);
        button.setTag(tag);

        try {
            InputStream stream = getAssets().open(tag);
            Bitmap bmp = BitmapFactory.decodeStream(stream);
            button.setImageBitmap(Bitmap.createScaledBitmap(bmp, BUTTON_SIZE, BUTTON_SIZE, false));
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }

        button.setPadding(25, 25, 25, 25);
        button.getBackground().setAlpha(0);
        button.setOnClickListener(this);
        return button;
    }

    private void setButton(String tag, Boolean isOn) {
        String path = tag;
        if (isOn) {
            path = path.substring(0, path.length() - 4) + "On.png";
        }
        try {
            InputStream stream = getAssets().open(path);
            Bitmap bmp = BitmapFactory.decodeStream(stream);
            ImageButton button = buttonLayout.findViewWithTag(tag);
            button.setImageBitmap(Bitmap.createScaledBitmap(bmp, 80, 80, false));
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    private void runInterpreter() {
        if (classifier != null) {
            classifier.classifyMat(frame);
            switch (classifier.getResult()) {
                case "SPACE":
                    text += " ";
                    break;
                case "BACKSPACE":
                    text = text.substring(0, text.length() - 1);
                    break;
                case "NOTHING":
                    break;
                default:
                    text += classifier.getResult();
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    resultTextView.setText(text);
                }
            });
            Log.d(TAG, "Guess: " + classifier.getResult() + " Probability: " + classifier.getProbability());
        }
    }
}