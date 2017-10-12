package com.example.android.ocr;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.os.Build.VERSION_CODES.O;

public class MainActivity extends AppCompatActivity {
    Bitmap image;
    private TessBaseAPI mTess;
    private String datapath = "";
    private String mCurrentPhotoPath;
    private Uri selectedImageUri;
    private ImageView imageView;
    private ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = (ImageView) findViewById(R.id.imageView);
        image = BitmapFactory.decodeResource(getResources(),R.drawable.test_image);
        progressBar = (ProgressBar) findViewById(R.id.prgoressBar);
        progressBar.setVisibility(View.GONE);
        datapath = getFilesDir() + "/tessaract/";
        checkFile(new File(datapath + "tessdata/"));
        String lang = "eng";
        mTess = new TessBaseAPI();
        mTess.init(datapath,lang);
        Button imageSelect = (Button) findViewById(R.id.select_image);
        imageSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setImage();
            }
        });
    }

    void setImage(){
        String items[] = {"Take Photo","Choose From Gallery","Close"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Add Photo");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which==0)
                    cameraIntent();
                else if (which==1)
                    galleryIntent();
                else  if (which==2)
                    dialog.dismiss();
            }
        });

        builder.show();
    }

    private void cameraIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.v("Error", "IO Exception");

            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileproviderOCR",
                        photoFile);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, 0);
            }
        }

    }

    private void galleryIntent(){
        Intent intent = new Intent();

        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, 1);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, 1);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (data.getData() != null) {
                selectedImageUri = data.getData();
            }
            if (requestCode == 0 && resultCode == RESULT_OK) {
                File f = new File(mCurrentPhotoPath);
                selectedImageUri = Uri.fromFile(f);
                try {
                    image = MediaStore.Images.Media.getBitmap(this.getContentResolver(),selectedImageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                imageView.setImageBitmap(image);

            }

            if (requestCode == 1) {

                try {
                    image = MediaStore.Images.Media.getBitmap(this.getContentResolver(),selectedImageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                imageView.setImageBitmap(image);
            }

        }

        }


    private File createImageFile() throws IOException {


        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "OCR");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                "IMG_"+ timeStamp + ".jpg");

         /*String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );*/


        mCurrentPhotoPath = mediaFile.getAbsolutePath();
        return mediaFile;
    }




    private void copyFiles() {
        try {
            //location we want the file to be at
            String filepath = datapath + "/tessdata/eng.traineddata";

            //get access to AssetManager
            AssetManager assetManager = getAssets();

            //open byte streams for reading/writing
            InputStream instream = assetManager.open("tessdata/eng.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);

            //copy the file to the location specified by filepath
            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }
            outstream.flush();
            outstream.close();
            instream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkFile(File dir) {
        //directory does not exist, but we can successfully create it
        if (!dir.exists()&& dir.mkdirs()){
            copyFiles();
        }
        //The directory exists, but there is no data file in it
        if(dir.exists()) {
            String datafilepath = datapath+ "/tessdata/eng.traineddata";
            File datafile = new File(datafilepath);
            if (!datafile.exists()) {
                copyFiles();
            }
        }
    }

    public void processImage(View view){
        Log.d("Test","Hello");
        new ImageProcess().execute();

    }
    @SuppressWarnings("deprecation")
    private class ImageProcess extends AsyncTask<Void,Void,String>{
        @Override
        protected String doInBackground(Void... voids) {
            //Using Tessaract
//            String OCRresult = null;
//            mTess.setImage(image);
//            OCRresult = mTess.getUTF8Text();
//            String number = "";
//            String regex = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
//            Pattern pattern = Pattern.compile(regex);
//            Matcher matcher = pattern.matcher(OCRresult);
//            while (matcher.find()){
//                String match = matcher.group(0);
//                if(!match.equals(""))
//                number += match+"\n\n";
//            }
//            return number;

            //Using Google Mobile Vision
            TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

            Log.v("Test", textRecognizer.isOperational()+"");
            if (!textRecognizer.isOperational()) {
                Log.w("Test", "Detector dependencies are not yet available.");

                // Check for low storage.  If there is low storage, the native library will not be
                // downloaded, so detection will not become operational.
                IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
                boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

                if (hasLowStorage) {
                    Toast.makeText(MainActivity.this, "Low Storage", Toast.LENGTH_LONG).show();
                    Log.w("Test", "Low Storage");
                }
            }
            Frame imageFrame = new Frame.Builder()
                    .setBitmap(image)
                    .build();
            SparseArray<TextBlock> textBlocks = textRecognizer.detect(imageFrame);
            String result = "";
            for (int i = 0; i < textBlocks.size(); i++) {
                TextBlock textBlock = textBlocks.get(textBlocks.keyAt(i));
                result +=textBlock.getValue()+'\n';

            }

            String url = "";

            String regex = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
            String regex2 = "(http://www.|https://www.|http://|https://)?[a-z0-9]+([-.]{1}[a-z0-9]+)*\\.[a-z]{2,5}(:[0-9]{1,5})?(/.*)?";
            Pattern pattern = Pattern.compile(regex2);
            Matcher matcher = pattern.matcher(result);
            while (matcher.find()){
                String match = matcher.group(0);
                if(!match.equals(""))
                    url += match+"\n\n";
            }
            return url;



        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            TextView OCRTextView = (TextView) findViewById(R.id.OCRTextView);
            OCRTextView.setText(s);
            progressBar.setVisibility(View.GONE);
        }
    }
}
