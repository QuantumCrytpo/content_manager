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
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;


import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AddActivity extends AppCompatActivity{

    private final int REQUEST_IMG_CODE = 202;
    private int currState = 0;
    private Button nxtBnt;
    private Button chooseButton;
    private Button uploadBtn;
    private Map<String, Object> newDoc;
    private Map<String, Object> arreglosOptions;
    private Map<String, Object> decoOptions;
    private Map<String, Object> postreOptions;
    private String[] arreglosArray;
    private String[] decoArray;
    private String[] postresArray;
    private FirebaseFirestore database;
    private boolean spinnerInitialized;
    private ImageView imgView;
    private static FTPSClient ftp;
    private ByteArrayInputStream inputStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);
        spinnerInitialized = false;
        nxtBnt = findViewById(R.id.nextButton);
        nxtBnt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextState();
            }
        });

        chooseButton = findViewById(R.id.chooseButton);
        chooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage(v);
            }
        });

        uploadBtn = findViewById(R.id.uploadButton);
        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImage();
            }
        });

        database = FirebaseFirestore.getInstance();

        setCategories();
    }

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


    private void nextState(){
        if(currState == 0){
            LinearLayout state0 = findViewById(R.id.stateOneLayout);
            state0.setVisibility(View.GONE);
            LinearLayout state1 =  findViewById(R.id.stateTwoLayout);
            state1.setVisibility(View.VISIBLE);
            validateInput();
        }
    }

    private boolean validateInput(){

        newDoc = new HashMap<>();

        EditText title = findViewById(R.id.titleInput);
        String titleIn = title.getText().toString();

        Spinner sp = findViewById(R.id.catSpinner);
        String cat = sp.getSelectedItem().toString().toLowerCase();
        newDoc.put("category", cat);
        newDoc.put("title", title.getText().toString());

        DocumentReference documentReference = database.collection("categorias")
                .document(cat);
        documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                long count =(long) documentSnapshot.getData().get("count");
                String curr = "";
                int itemId = 0;
                String opt = "";
                Spinner optsSp  = findViewById(R.id.optnsSpinner);
                String optSelected = optsSp.getSelectedItem().toString();
                if(newDoc.get("category").equals("arreglos")){
                    curr = "arreglos-";
                    itemId = 2000 + (int)count;
                    opt = (String) arreglosOptions.get(optSelected);
                }
                else if (newDoc.get("category").equals("decoraciones")){
                    curr = "deco-";
                    itemId = 1000 + (int)count;
                    opt = (String) decoOptions.get(optSelected);
                }
                else if(newDoc.get("category").equals("postres")){
                    curr = "postre-";
                    itemId = 3000 + (int)count;
                    opt = (String) postreOptions.get(optSelected);
                }

                if(count < 10){curr = curr + "00" + count;}
                else if(9 < count && count < 100){curr = curr + "0" + count;}
                else if(99 < count){ curr = curr + count;}

                newDoc.put("itemID",Integer.toString(itemId));
                newDoc.put("imgSrc", curr);
                newDoc.put("opciones", Arrays.asList(opt));

                Log.v("DDDD1111", curr + "-----opts: " + opt);
                addToFirestore(newDoc);
            }
        });

        return true;

    }

    private void addToFirestore(final Map<String, Object> newDocument){
        Log.v("DOC", "DOC: "  + newDocument.get("imgSrc") + ".." + newDocument.get("title"));
        database.collection("categorias")
                .document((String) newDocument.get("category"))
                .collection("tile-info")
                .document((String) newDoc.get("imgSrc"))
                .set(newDoc)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.v("FSDB", "DATA ADDED: " + newDocument.get("title"));
                    }
                });

        EditText description = findViewById(R.id.desInput);
        newDoc.put("des", description.getText().toString());
        newDoc.remove("opciones");

        database.collection("categorias")
                .document((String) newDocument.get("category"))
                .collection("item-info")
                .document((String) newDoc.get("imgSrc"))
                .set(newDoc)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.v("FSDB", "DATA ADDED!");
                    }
                });
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
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
            byte[] bitemap = bos.toByteArray();
            inputStream = new ByteArrayInputStream(bitemap);
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

    private void uploadImage() {
        try {
            FTPSClient ftp = new FTPSClient();
            // FTP LOGIN INFO


            ftp.enterLocalPassiveMode();
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                Log.v("FTP", "FTP refused to connect");

                System.exit(1);
            } else {
                Spinner sp = findViewById(R.id.catSpinner);
                String cat = "decoraciones";// sp.getSelectedItem().toString().toLowerCase();

                EditText title = findViewById(R.id.titleInput);
                String titleIn = title.getText().toString();
                Log.d("DOC", "CAT: " + cat  + "::T:" + titleIn);
                //ftp.cwd("")



                ftp.storeFile("PIC.png", inputStream);
            }
        } catch (IOException e){
            Log.d("FTP", "FAILED TO UPLOAD");
        }
    }
}
