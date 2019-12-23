package com.arasdecoraciones.aracontentmanager.util;

import android.util.Log;
import android.widget.EditText;
import android.widget.Spinner;

import com.arasdecoraciones.aracontentmanager.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RequestHandler {

    private FirebaseFirestore database;
    private static Map<String, Object> newDoc;
    private String itemDes;

    public RequestHandler(){
        database = FirebaseFirestore.getInstance();
    }
    /*
     * Description: Checks if input is valid and saves for uploading later.
     *
     *
     */
    public int validateInput(final String category, String title, final String option, String des){

        final Map<String, Object> newMap = new HashMap<>();
        itemDes = des;
        newMap.put("category", category);
        newMap.put("title", title);

        Log.d("DB", "In validateInput: category - " + category);
        DocumentReference documentReference = database.collection("categorias")
                .document(category);
        documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                long count = (long) documentSnapshot.getData().get("count");
                String curr;
                int itemId;

                if (category.equals("arreglos")) {
                    curr = "arreglos-";
                    itemId = 2000 + (int) count;
                } else if (category.equals("decoraciones")) {
                    curr = "deco-";
                    itemId = 1000 + (int) count;
                } else {
                    curr = "postre-";
                    itemId = 3000 + (int) count;
                }

                if (count < 10) {
                    curr = curr + "00" + count;
                } else if (9 < count && count < 100) {
                    curr = curr + "0" + count;
                } else if (99 < count) {
                    curr = curr + count;
                }

                newMap.put("itemID", Integer.toString(itemId));
                newMap.put("imgSrc", curr);
                newMap.put("opciones", Arrays.asList(option));

                newDoc = newMap;
            }
        });

        return 0;
    }

    public void uploadNewItem(ByteArrayInputStream inputStream){
        addToFirestore();
        uploadImage(inputStream);
        Log.d("UPL", "UPLOAD COMPLETED");
    }

    private void addToFirestore(){
        database.collection("categorias")
                .document((String) newDoc.get("category"))
                .collection("tile-info")
                .document((String) newDoc.get("imgSrc"))
                .set(newDoc)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.v("FSDB", "DATA ADDED: " + newDoc.get("title"));
                    }
                });

        newDoc.put("des", itemDes);
        newDoc.remove("opciones");

        database.collection("categorias")
                .document((String) newDoc.get("category"))
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

    private void uploadImage(ByteArrayInputStream imageStream){
        try {
            FTPSClient ftp = new FTPSClient();

            Log.d("FTP", "Connecting...");
            ftp.connect("ftp.arasdecoraciones.com", 21);
            ftp.login("mediaupload@arasdecoraciones.com", "btkzsat2?");


            ftp.enterLocalPassiveMode();
            ftp.setFileType(FTP.BINARY_FILE_TYPE);

            int replyCode = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                ftp.disconnect();
                Log.v("FTP", "FTP refused to connect");

                System.exit(1);
            } else {
                if (true){//ftp.changeWorkingDirectory("images/" +  + "/")) {
                    Log.d("FTP", "Attempting to change directory: " + (String) newDoc.get("category"));
                    ftp.cwd("images/" + (String) newDoc.get("category"));
                    ftp.storeFile((String) newDoc.get("imgSrc") + ".png", imageStream);
                    Log.d("FTP", "Uploaded: " + (String) newDoc.get("imgSrc") + ".png");
                } else {
                    Log.d("FTP", "COULD NOT CHANGE DIRECTORY");
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }

    }
}
