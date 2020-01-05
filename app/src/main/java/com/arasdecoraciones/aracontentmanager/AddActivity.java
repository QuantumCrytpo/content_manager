package com.arasdecoraciones.aracontentmanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.arasdecoraciones.aracontentmanager.util.RequestHandler;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.apache.commons.net.io.CopyStreamAdapter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class AddActivity extends AppCompatActivity{

    private final int REQUEST_IMG_CODE = 202;
    private int currState = 0;
    private Button nextButtton, chooseButton, goHomeBtn, newOneBtn;
    private Map<String, Object> newDoc;
    private Map<String, Object> arreglosOptions;
    private Map<String, Object> decoOptions;
    private Map<String, Object> postreOptions;
    private String[] arreglosArray, decoArray, postresArray;
    private FirebaseFirestore database;
    private boolean spinnerInitialized;
    private ImageView imgView;
    private String[] inputArray;
    private ByteArrayInputStream inputStream;
    private ProgressBar progressBar;
    private int transferSize;
    private Handler hl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);
        spinnerInitialized = false;
        nextButtton = findViewById(R.id.nextButton);
        nextButtton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeState();
            }
        });

        chooseButton = findViewById(R.id.chooseButton);
        chooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage(v);
            }
        });

        progressBar = findViewById(R.id.progressBar);

        goHomeBtn = findViewById(R.id.goHomeBtn);
        goHomeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                returnHome();
            }
        });

        newOneBtn = findViewById(R.id.newOneBtn);
        newOneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), AddActivity.class);
                finish();
                startActivity(i);
            }
        });

        database = FirebaseFirestore.getInstance();

        setCategories();
    }

    /*
     * Description: Sets the spinner for selection category and option.
     *              Categories are fixed, while the options are fetched from
     *              the database.
     * 
     * @Param: None
     * @return: void
     */
    private void setCategories(){
        String[] cats = new String[]{"Arreglos", "Decoraciones", "Postres"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cats);
        Spinner sp = findViewById(R.id.catSpinner);
        sp.setAdapter(adapter);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if(!spinnerInitialized){spinnerInitialized = true;}
            else {
                String selectedItem = parent.getItemAtPosition(position).toString();
                Spinner optsSpinner = (Spinner) findViewById(R.id.optnsSpinner);
                ArrayAdapter<String> adapter1;
                if (selectedItem.toLowerCase().equals("arreglos")) {
                    adapter1 = new ArrayAdapter<String>(parent.getContext(), android.R.layout.simple_spinner_dropdown_item, arreglosArray);
                    optsSpinner.setAdapter(adapter1);
                } else if (selectedItem.toLowerCase().equals("decoraciones")) {
                    adapter1 = new ArrayAdapter<String>(parent.getContext(), android.R.layout.simple_spinner_dropdown_item, decoArray);
                    optsSpinner.setAdapter(adapter1);
                } else if (selectedItem.toLowerCase().equals("postres")) {
                    adapter1 = new ArrayAdapter<String>(parent.getContext(), android.R.layout.simple_spinner_dropdown_item, postresArray);
                    optsSpinner.setAdapter(adapter1);
                }
            }

                Log.v("DD", "SELECTED: " + parent.getItemAtPosition(position).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        CollectionReference docRef = database.collection("categorias");
        docRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    for(QueryDocumentSnapshot doc : task.getResult()){
                        if(doc.getId().equals("arreglos")){
                            arreglosOptions = (HashMap<String, Object>) doc.getData().get("options");

                            arreglosArray = new String[arreglosOptions.size()];
                            int i = 0;
                            for(String key: arreglosOptions.keySet()){
                                arreglosArray[i++] = key;
                            }
                        }
                        if(doc.getId().equals("decoraciones")){
                            decoOptions = (HashMap<String, Object>) doc.getData().get("options");

                            decoArray = new String[decoOptions.size()];
                            int i = 0;
                            for(String key: decoOptions.keySet()){
                                decoArray[i++] = key;
                            }
                        }
                        if(doc.getId().equals("postres")){
                            postreOptions = (HashMap<String, Object>) doc.getData().get("options");

                            postresArray = new String[postreOptions.size()];
                            int i = 0;
                            for(String key: postreOptions.keySet()){
                                postresArray[i++] = key;
                            }
                        }
                    }
                }
            }
        });
    }

    private void changeState(){
        if(currState == 0){
            int validationCode = validateInput();
            if(validationCode == 0) {
                LinearLayout state0 = findViewById(R.id.stateOneLayout);
                state0.setVisibility(View.GONE);
                LinearLayout state1 = findViewById(R.id.stateTwoLayout);
                state1.setVisibility(View.VISIBLE);
                nextButtton.setEnabled(false);
                this.currState += 1;
            }
        } else if(currState == 1){
            Log.d("DB", "CALLING UPLOAD");
            progressBar.setMax(100);
            nextButtton.setVisibility(View.GONE);
            hl = new Handler();
            CopyStreamAdapter streamListener = new CopyStreamAdapter() {

                @Override
                public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
                    //this method will be called every time some bytes are transferred
                    final int percent = (int)(totalBytesTransferred*100/transferSize);

                    hl.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("PRG", "Progress: " + percent);
                            progressBar.setProgress(percent);
                            if(percent == 100){
                                TextView t = findViewById(R.id.finalWaitMsg);
                                t.setText("Articulo Agregado!");
                            }
                        }
                    });
                }

            };

            RequestHandler handler = new RequestHandler(this.inputStream, streamListener, inputArray);
            Thread t1 = new Thread(handler);
            t1.start();

            LinearLayout state2 = findViewById(R.id.stateTwoLayout);
            state2.setVisibility(View.GONE);
            LinearLayout state3 = findViewById(R.id.stateThreeLayout);
            state3.setVisibility(View.VISIBLE);
        }
    }

    /*
     *
     *
     */
    private int validateInput(){
        EditText title = findViewById(R.id.titleInput);
        String titleIn = title.getText().toString();

        Spinner sp = findViewById(R.id.catSpinner);
        String categorySelected = sp.getSelectedItem().toString().toLowerCase();

        EditText desText = findViewById(R.id.desInput);
        String description = desText.getText().toString();

        Spinner optsSp  = findViewById(R.id.optnsSpinner);
        String optSelected = optsSp.getSelectedItem().toString();
        String optCode;

        if(categorySelected.equals("arreglos")){
            optCode = (String) arreglosOptions.get(optSelected);
        } else if(categorySelected.equals("decoraciones")){
            optCode = (String) decoOptions.get(optSelected);
        } else {
            optCode = (String) postreOptions.get(optSelected);
        }

        this.inputArray = new String[]{categorySelected, titleIn, optCode, description};

        return 0;
    }

    //State Two Methods

    private void chooseImage(View view){
        checkPermission();
        Intent imgChoose = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Start the Intent
        startActivityForResult(imgChoose, REQUEST_IMG_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(resultCode == RESULT_OK && requestCode == REQUEST_IMG_CODE && data != null){
            Uri selectedImage = data.getData();
            String[] pathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedImage, pathColumn, null, null, null);
            cursor.moveToFirst();
            String imgPath = cursor.getString(cursor.getColumnIndex(pathColumn[0]));

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(imgPath, options);

            imgView = findViewById(R.id.imageView);
            imgView.setImageBitmap(bitmap);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);

            progressBar.setMax(bos.size()/1024);
            this.transferSize = bos.size();

            byte[] bytemap = bos.toByteArray();

            inputStream = new ByteArrayInputStream(bytemap);

            nextButtton.setEnabled(true);
        }
    }

    private void checkPermission(){
        if(ContextCompat.checkSelfPermission(AddActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(AddActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    205);
        }
    }

    private void returnHome(){
        Intent i = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(i);
    }

}
