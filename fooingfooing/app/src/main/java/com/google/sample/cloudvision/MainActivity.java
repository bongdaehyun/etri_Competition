package com.google.sample.cloudvision;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    private static final String CLOUD_VISION_API_KEY = BuildConfig.API_KEY;
    public static final String FILE_NAME = "temp.jpg";
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";
    private static final int MAX_LABEL_RESULTS = 10;
    private static final int MAX_DIMENSION = 1200;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int GALLERY_PERMISSIONS_REQUEST = 0;
    private static final int GALLERY_IMAGE_REQUEST = 1;
    public static final int CAMERA_PERMISSIONS_REQUEST = 2;
    public static final int CAMERA_IMAGE_REQUEST = 3;
    public String translated_str = "";
    private TextView mImageDetails;
    private ImageView mMainImage;

    public String[] testArray;

    public static ArrayList<String> item = new ArrayList<String>();
    public ArrayList<String> typeitem = new ArrayList<String>();

    public static ArrayList<String> result = new ArrayList<String>();

    //-----
    public ArrayList<MainList> mainLists = new ArrayList<MainList>();
    public RecyclerAdapter mAdapter = new RecyclerAdapter(mainLists);
    private CustomDialog customDialog;
    static public int pos;

    private TextView manual;
    private ProgressDialog progressDialog;
    public void loading() {
        //로딩
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        progressDialog = new ProgressDialog(MainActivity.this);
                        progressDialog.setIndeterminate(true);
                        progressDialog.setMessage("잠시만 기다려 주세요");
                        progressDialog.show();
                    }
                }, 0);
    }

    public void loadingEnd() {
        new android.os.Handler().postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                    }
                }, 0);
    }
    public class MainList{
        public String mName;
        public String mCategory;
        public MainList(String name, String category){
            this.mName = name;
            this.mCategory = category;
        }
        public String getmName() { return mName; }
        public String getmCategory() { return mCategory; }
    }
    public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder>{

        ArrayList<MainList> mainLists = null;
        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
            TextView tvName;
            TextView tvCategory;
            public ViewHolder(View view) {
                super(view);
                view.setOnClickListener(this);
                tvCategory = view.findViewById(R.id.category);
                tvName = view.findViewById(R.id.name);
            }

            @Override
            public void onClick(View view) {
                customDialog = new CustomDialog(MainActivity.this, positiveListener);
                pos = getAdapterPosition() ;
                customDialog.show();

            }
        }

        public RecyclerAdapter(ArrayList<MainList> mainList){
            this.mainLists = mainList;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_row, parent, false);
            return new ViewHolder(v);

        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.tvCategory.setText(mainLists.get(position).mCategory);
            holder.tvName.setText(mainLists.get(position).mName);
        }

        @Override
        public int getItemCount() {
            return mainLists.size();
        }


    }


    //-----

    static public class Morpheme {
        final String text;
        final String type;
        Integer count;
        public Morpheme (String text, String type, Integer count) {
            this.text = text;
            this.type = type;
            this.count = count;
        }
    }
    static public class NameEntity {
        final String text;
        final String type;
        Integer count;
        public NameEntity (String text, String type, Integer count) {
            this.text = text;
            this.type = type;
            this.count = count;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        manual = (TextView)findViewById(R.id.manualTx);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {

            //manual.setVisibility(view.INVISIBLE);
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder
                    .setMessage(R.string.dialog_select_prompt)
                    .setPositiveButton(R.string.dialog_select_gallery, (dialog, which) -> startGalleryChooser())
                    .setNegativeButton(R.string.dialog_select_camera, (dialog, which) -> startCamera());
            builder.create().show();
        });

        mMainImage = findViewById(R.id.main_image);

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    private View.OnClickListener positiveListener = new View.OnClickListener() {
        public void onClick(View v) {
            Toast.makeText(getApplicationContext(), "확인버튼이 눌렸습니다.",Toast.LENGTH_SHORT).show();
            customDialog.dismiss();
        }
    };

    public class WikiQA extends AsyncTask<String, Void, String>{

        @Override
        protected void onPreExecute() { //
            super.onPreExecute();
        }
        @Override
        protected String doInBackground(String... strings) {

            for(int i=0; i<item.size();i++) {
                String openApiURL = "http://aiopen.etri.re.kr:8000/WikiQA";
                String accessKey = "38417bd0-cce1-44e6-97a3-202548a006f6";    // 발급받은 API Key
                String type = "ENGINE_TYPE";            // 분석할 문단 데이터
                String question = item.get(i);          // 질문 데이터
                Gson gson = new Gson();

                Map<String, Object> request = new HashMap<>();
                Map<String, String> argument = new HashMap<>();

                argument.put("question", question);
                argument.put("type", type);

                request.put("access_key", accessKey);
                request.put("argument", argument);


                URL url;
                Integer responseCode = null;
                String responBody = null;

                try {
                    url = new URL(openApiURL);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("POST");
                    con.setDoOutput(true);

                    DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                    wr.write(gson.toJson(request).getBytes("UTF-8"));
                    wr.flush();
                    wr.close();

                    if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {

                        System.out.println("들어는 왔음");
                        return null;
                    }
                    BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));

                    String line;
                    String page = "";

                    // 라인을 받아와 합친다.
                    while ((line = reader.readLine()) != null) {
                        page += line;
                    }
                    System.out.println("진짜! "+i +"번쨰: "+ page);

                    JsonParser jsonParser = new JsonParser();
                    JsonElement jsonElement = jsonParser.parse(page);

                    String res = jsonElement.getAsJsonObject().get("return_object").getAsJsonObject().get("WiKiInfo").getAsJsonObject().get("IRInfo").getAsJsonArray().get(0).getAsJsonObject().get("sent").toString();
                    result.add(res);
                    System.out.println("TEST: "+res);

                } catch (MalformedURLException e) {
                    e.printStackTrace();

                } catch (IOException e) {
                    //e.printStackTrace();
                    e.getMessage();

                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            loadingEnd();
            RecyclerView mRecyclerView = (RecyclerView)findViewById(R.id.recycler_view);
            mRecyclerView.setAdapter(mAdapter);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
            mRecyclerView.setHasFixedSize(true);
        }
    }

    public class NLPApi extends AsyncTask<String, Void, String>{

        // 분석할 텍스트 데이터
        @Override
        protected void onPreExecute() { //Background 작업 시작전에 UI 작업을 진행 한다.
            super.onPreExecute();
        }
        @Override
        protected String doInBackground(String... strings) { //Background 작업을 진행 한다.

            for(int i=0; i<testArray.length; i++) {
                System.out.println("들어오고 나서 확인:"+testArray[i]);
                String openApiURL = "http://aiopen.etri.re.kr:8000/WiseNLU";
                String accessKey = "38417bd0-cce1-44e6-97a3-202548a006f6";    // 발급받은 API Key
                String analysisCode = "ner";   // 언어 분석 코드
                String text = testArray[i];           // 분석할 텍스트 데이터
                System.out.println(translated_str + "2"+text);
                Gson gson = new Gson();
                //System.out.println(text);
                Map<String, Object> request = new HashMap<>();
                Map<String, String> argument = new HashMap<>();

                argument.put("analysis_code", analysisCode);
                argument.put("text", text);

                request.put("access_key", accessKey);
                request.put("argument", argument);


                URL url;
                Integer responseCode = null;
                //String responBody = null;
                try {
                    url = new URL(openApiURL);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("POST");
                    con.setDoOutput(true);

                    DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                    wr.write(gson.toJson(request).getBytes("UTF-8"));
                    wr.flush();
                    wr.close();
                    if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        return null;
                    }
                    BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));

                    String line;
                    String page = "";


                    // 라인을 받아와 합친다.
                    while ((line = reader.readLine()) != null) {
                        page += line;
                    }

                    JsonParser jsonParser = new JsonParser();
                    JsonElement jsonElement = jsonParser.parse(page);


                    String check = jsonElement.getAsJsonObject().get("return_object").getAsJsonObject().get("sentence").getAsJsonArray().get(0).getAsJsonObject().get("NE").getAsJsonArray().toString();
                    //System.out.println("에러검사: "+check);
                    if(!check.equals("[]"))
                    {
                        String type = jsonElement.getAsJsonObject().get("return_object").getAsJsonObject().get("sentence").getAsJsonArray().get(0).getAsJsonObject().get("NE").getAsJsonArray().get(0).getAsJsonObject().get("type").toString();


                        if(type.equals("\"MT_CHEMICAL\"") || type.equals("\"TMM_DRUG\"") || type.equals("\"FD_MEDICINE\"")||type.equals("\"CV_FOOD\"")||type.equals("\"CV_DRINK\"")||type.equals("\"MT_ELEMENT\"")||type.equals("\"MT_METAL\""))
                        {

                            String item1 = jsonElement.getAsJsonObject().get("return_object").getAsJsonObject().get("sentence").getAsJsonArray().get(0).getAsJsonObject().get("NE").getAsJsonArray().get(0).getAsJsonObject().get("text").toString();

                            item.add(jsonElement.getAsJsonObject().get("return_object").getAsJsonObject().get("sentence").getAsJsonArray().get(0).getAsJsonObject().get("NE").getAsJsonArray().get(0).getAsJsonObject().get("text").toString());
                            //System.out.println(item[i] + "아이템 배열 확인");
                            typeitem.add(type);
                            mainLists.add(new MainList(jsonElement.getAsJsonObject().get("return_object").getAsJsonObject().get("sentence").getAsJsonArray().get(0).getAsJsonObject().get("NE").getAsJsonArray().get(0).getAsJsonObject().get("text").toString(),type));
                        }

                    }

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
/*            for(int k = 0 ; k<item.length; k++)
            {
                System.out.println(item[k]);
            }*/

            for(int i = 0; i < item.size(); i++)
            {
                System.out.println("아이템 배열 확인"+item.get(i));
            }



            return null;
        }

        @Override
        protected void onPostExecute(String s) { //Background 작업이 끝난 후 UI 작업을 진행 한다.
            super.onPostExecute(s);

            WikiQA asyn3 = new WikiQA();
            asyn3.execute();

        }
    }
    //ASYNCTASK
    public class NaverTranslateTask extends AsyncTask<String, Void, String> {

        public String resultText;
        //Naver
        String clientId = "FPgU5QCP8a5lY_GcWLAw";//애플리케이션 클라이언트 아이디값";
        String clientSecret = "XE9wRkfBS6";//애플리케이션 클라이언트 시크릿값";
        //언어선택도 나중에 사용자가 선택할 수 있게 옵션 처리해 주면 된다.
        String sourceLang = "en";
        String targetLang = "ko";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        @Override
        protected String doInBackground(String... strings) {
            String sourceText = strings[0];
            try {
                //String text = URLEncoder.encode("만나서 반갑습니다.", "UTF-8");
                String text = URLEncoder.encode(sourceText, "UTF-8");
                //String apiURL = "https://openapi.naver.com/v1/language/translate";
                String apiURL = "https://openapi.naver.com/v1/papago/n2mt";
                URL url = new URL(apiURL);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("X-Naver-Client-Id", clientId);
                con.setRequestProperty("X-Naver-Client-Secret", clientSecret);
                // post request
                String postParams = "source=" + sourceLang + "&target=" + targetLang + "&text=" + text;
                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(postParams);
                wr.flush();
                wr.close();
                int responseCode = con.getResponseCode();
                BufferedReader br;
                if (responseCode == 200) { // 정상 호출
                    br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                } else {  // 에러 발생
                    br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                }
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = br.readLine()) != null) {
                    response.append(inputLine);
                }
                br.close();
                //System.out.println(response.toString());
                return response.toString();

            } catch (Exception e) {
                //System.out.println(e);
                Log.d("error", e.getMessage());
                return null;
            }
        }

        //번역된 결과를 받아서 처리
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //결과를 tvResult 뷰에 집어 넣는다.

            Gson gson = new GsonBuilder().create();
            JsonParser parser = new JsonParser();
            JsonElement rootObj = parser.parse(s.toString())
                    //원하는 데이터 까지 찾아 들어간다.
                    .getAsJsonObject().get("message")
                    .getAsJsonObject().get("result");
            //안드로이드 객체에 담기
            TranslatedItem items = gson.fromJson(rootObj.toString(), TranslatedItem.class);
            //Log.d("result", items.getTranslatedText());
            //번역결과를 텍스트뷰에 넣는다.


            String temp = items.getTranslatedText();
            //testArray = temp.split("\n");
            split(temp);
            translated_str = items.getTranslatedText(); // 전역변수에 번역된 결과 저장
            System.out.println(translated_str +"1");
            item.clear();
            NLPApi asyncTask2 = new NLPApi();
            asyncTask2.execute();


        }

        public void split(String str)
        {
            testArray = str.split("\n");
            /*for(int i=0; i<testArray.length; i++) {
                testArray[i] = testArray[i].replace("식사 섬유", "'식이섬유'");
                System.out.println("짤렸음! " + testArray[i]);
            }*/
        }
        private class TranslatedItem {
            String translatedText;

            public String getTranslatedText() {
                return translatedText;
            }
        }
    }

    public void startGalleryChooser() {
        if (PermissionUtils.requestPermission(this, GALLERY_PERMISSIONS_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select a photo"),
                    GALLERY_IMAGE_REQUEST);
        }
    }

    public void startCamera() {
        if (PermissionUtils.requestPermission(
                this,
                CAMERA_PERMISSIONS_REQUEST,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, CAMERA_IMAGE_REQUEST);
        }
    }

    public File getCameraFile() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(dir, FILE_NAME);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            uploadImage(data.getData());
        } else if (requestCode == CAMERA_IMAGE_REQUEST && resultCode == RESULT_OK) {
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            uploadImage(photoUri);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, CAMERA_PERMISSIONS_REQUEST, grantResults)) {
                    startCamera();
                }
                break;
            case GALLERY_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, GALLERY_PERMISSIONS_REQUEST, grantResults)) {
                    startGalleryChooser();
                }
                break;
        }
    }

    public void uploadImage(Uri uri) {
        if (uri != null) {
            try {
                // scale the image to save on bandwidth
                Bitmap bitmap =
                        scaleBitmapDown(
                                MediaStore.Images.Media.getBitmap(getContentResolver(), uri),
                                MAX_DIMENSION);

                callCloudVision(bitmap);
                manual.setVisibility(View.INVISIBLE);
                mMainImage.setImageBitmap(bitmap);

            } catch (IOException e) {
                Log.d(TAG, "Image picking failed because " + e.getMessage());
                Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(TAG, "Image picker gave us a null image.");
            Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
        }
    }

    private Vision.Images.Annotate prepareAnnotationRequest(Bitmap bitmap) throws IOException {
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        VisionRequestInitializer requestInitializer =
                new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                    /**
                     * We override this so we can inject important identifying fields into the HTTP
                     * headers. This enables use of a restricted cloud platform API key.
                     */
                    @Override
                    protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                            throws IOException {
                        super.initializeVisionRequest(visionRequest);

                        String packageName = getPackageName();
                        visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);

                        String sig = PackageManagerUtils.getSignature(getPackageManager(), packageName);

                        visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
                    }
                };

        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
        builder.setVisionRequestInitializer(requestInitializer);

        Vision vision = builder.build();

        BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                new BatchAnnotateImagesRequest();
        batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
            AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

            // Add the image
            Image base64EncodedImage = new Image();
            // Convert the bitmap to a JPEG
            // Just in case it's a format that Android understands but Cloud Vision
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();

            // Base64 encode the JPEG
            base64EncodedImage.encodeContent(imageBytes);
            annotateImageRequest.setImage(base64EncodedImage);

            // add the features we want
            annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                Feature textDetection = new Feature();
                textDetection.setType("TEXT_DETECTION");
                textDetection.setMaxResults(MAX_LABEL_RESULTS);
                add(textDetection);
            }});

            // Add the list of one thing to the request
            add(annotateImageRequest);
        }});

        Vision.Images.Annotate annotateRequest =
                vision.images().annotate(batchAnnotateImagesRequest);
        // Due to a bug: requests to Vision API containing large images fail when GZipped.
        annotateRequest.setDisableGZipContent(true);
        Log.d(TAG, "created Cloud Vision request object, sending request");

        return annotateRequest;
    }

    public class LableDetectionTask extends AsyncTask<Object, Void, String> {
        private final WeakReference<MainActivity> mActivityWeakReference;
        private Vision.Images.Annotate mRequest;

        LableDetectionTask(MainActivity activity, Vision.Images.Annotate annotate) {
            mActivityWeakReference = new WeakReference<>(activity);
            mRequest = annotate;
        }

        @Override
        protected String doInBackground(Object... params) {
            try {
                Log.d(TAG, "created Cloud Vision request object, sending request");
                BatchAnnotateImagesResponse response = mRequest.execute();
                return convertResponseToString(response);

            } catch (GoogleJsonResponseException e) {
                Log.d(TAG, "failed to make API request because " + e.getContent());
            } catch (IOException e) {
                Log.d(TAG, "failed to make API request because of other IOException " +
                        e.getMessage());
            }
            return "Cloud Vision API request failed. Check logs for details.";
        }

        protected void onPostExecute(String resultData) {
            MainActivity activity = mActivityWeakReference.get();
            if (activity != null && !activity.isFinishing()) {
                mainLists.clear();
                item.clear();
                result.clear();
                NaverTranslateTask asyncTask = new NaverTranslateTask();
                asyncTask.execute(resultData);
            }
        }
    }

    private void callCloudVision(final Bitmap bitmap) {
        // Switch text to loading
        //mImageDetails.setText(R.string.loading_message);
        loading();
        // Do the real work in an async task, because we need to use the network anyway
        try {
            AsyncTask<Object, Void, String> labelDetectionTask = new LableDetectionTask(this, prepareAnnotationRequest(bitmap));
            labelDetectionTask.execute();
        } catch (IOException e) {
            Log.d(TAG, "failed to make API request because of other IOException " +
                    e.getMessage());
        }
    }

    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    private static String convertResponseToString(BatchAnnotateImagesResponse response) {
        StringBuilder message = new StringBuilder("");

        List<EntityAnnotation> labels = response.getResponses().get(0).getTextAnnotations();
        if (labels != null) {
            message.append(labels.get(0).getDescription());
            Log.i("textcheck",""+message.toString());
        } else {
            message.append("nothing");
        }

        return message.toString();
    }

}