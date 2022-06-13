package com.example.facedectt;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;

import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    JavaCameraView javaCameraView;
    File cascFile;
    CascadeClassifier faceDetector;
    private Mat mRgba,mGrey;


    private static final int MY_CAMERA_REQUEST_CODE = 100;
    int activeCamera = CameraBridgeViewBase.CAMERA_ID_BACK;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(MainActivity.this,new String[]
                {Manifest.permission.WRITE_EXTERNAL_STORAGE},200);


        OpenCVLoader.initDebug();
        javaCameraView =(JavaCameraView) findViewById(R.id.javaCamView);



        // checking if the permission has already been granted
        String TAG = "MainActivity";
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permissions granted");
            initializeCamera(javaCameraView, activeCamera);
        } else {
            // prompt system dialog
            Log.d(TAG, "Permission prompt");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
        }
        ////////to here


        if(!OpenCVLoader.initDebug())
        {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0,this,baseCallback);

        }
        else
        {
            try {
                baseCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        javaCameraView.setCvCameraViewListener(this);


        if(OpenCVLoader.initDebug()){
            Toast.makeText(getApplicationContext(), "OpenCV loaded successfully :)", Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(getApplicationContext(), "Could not load OpenCV!!", Toast.LENGTH_LONG).show();
        }
    }


    private String getAbsolutePath(Uri uri){
        String[] proj ={MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(uri,proj,null,null,null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
            return  cursor.getString(column_index);

    }
    public void detectFace (Mat img){
        Imgproc.cvtColor(img,img,Imgproc.COLOR_RGB2BGRA);
        Mat matrix = img.clone();
        CascadeClassifier cascadeClassifier =new CascadeClassifier();

        try {
            InputStream is= this.getResources().openRawResource(R.raw.lbpcascade_frontalcatface);
            File cascadeDir =getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeDirFile = new File(cascadeDir,"lbpcascade_frontalcatface.xml");

            FileOutputStream os= new FileOutputStream(mCascadeDirFile);

            byte[] buffer=new byte[4096];
            int bytesRead;

            while ((bytesRead=is.read(buffer))!=-1)
            {
                os.write(buffer,0,bytesRead);
            }
            is.close();
            os.close();

            cascadeClassifier =new CascadeClassifier(mCascadeDirFile.getAbsolutePath());

        }
        catch (Exception e){
            Log.e("OpenCVActivity","error loading cascade",e);
        }

        MatOfRect faceArray = new MatOfRect();
        cascadeClassifier.detectMultiScale(matrix,faceArray);
         int numFaces = faceArray.toArray().length;

         for(Rect face :faceArray.toArray()){
             Imgproc.rectangle(matrix,
                     new Point(face.x,face.y)
                     ,new Point(face.x+face.width,face.y+face.height)
             ,new Scalar(0,0,255),3);
         }
         Mat finalMatrix = matrix.clone();
        Bitmap bitmap = Bitmap.createBitmap(finalMatrix.cols(),finalMatrix.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(finalMatrix,bitmap);
//        imageView2.setImageBitmap(bitmap);

        Toast.makeText(getApplicationContext(),numFaces+"face/s found!",Toast.LENGTH_SHORT).show();
    }

    //////////////////////////////////////////
    // callback to be executed after the user has given approval or rejection via system prompt
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // camera can be turned on
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                initializeCamera(javaCameraView, activeCamera);
            } else {
                // camera will stay off
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeCamera(JavaCameraView javaCameraView, int activeCamera){
        javaCameraView.setCameraPermissionGranted();
        javaCameraView.setCameraIndex(activeCamera);
        javaCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
    }
    ///////////////////////////////

    @Override
    public void onCameraViewStarted(int width, int height) {

        mRgba =new Mat();
        mGrey =new Mat();
    }

    @Override
    public void onCameraViewStopped() {

        mRgba.release();
        mGrey.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        mRgba= inputFrame.rgba();
        mGrey=inputFrame.gray();

        // detect face

        MatOfRect faceDetections=new MatOfRect();
        faceDetector.detectMultiScale(mRgba,faceDetections);

        for (Rect rect: faceDetections.toArray())
        {
            Imgproc.rectangle(mRgba, new Point(rect.x,rect.y),
                    new Point(rect.x+ rect.width,rect.y + rect.height),
                    new Scalar(255,0,0));
        }
        return mRgba;
    }

    private BaseLoaderCallback baseCallback =new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) throws IOException {
            switch (status)
            {
                case LoaderCallbackInterface.SUCCESS:
                {
                    InputStream is= getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
                    File cascadeDir =getDir("cascade", Context.MODE_PRIVATE);
                    cascFile =new File(cascadeDir,"haarcascade_frontalface_alt2.xml");
                    FileOutputStream fos= new FileOutputStream(cascFile);

                    byte[] buffer=new byte[4096];
                    int bytesRead;

                    while ((bytesRead=is.read(buffer))!=-1)
                    {
                        fos.write(buffer,0,bytesRead);
                    }
                    is.close();
                    fos.close();

                    faceDetector =new CascadeClassifier(cascFile.getAbsolutePath());

                    if(faceDetector.empty())
                    {
                        faceDetector=null;
                    }
                    else
                        cascadeDir.delete();
                    javaCameraView.enableView();
                }
                break;

                default: {
                    super.onManagerConnected(status);
                }
                break;


            }



        }
    };



}