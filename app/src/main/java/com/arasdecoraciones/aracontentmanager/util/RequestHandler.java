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

public class RequestHandler implements Runnable {

    private FirebaseFirestore database;
    private static Map<String, Object> newDoc;
    private String[] newDocInfo;
    private ByteArrayInputStream imageInputStream;

    public RequestHandler(ByteArrayInputStream inputStream, String... newDocument) {
        this.newDocInfo = newDocument;
        this.imageInputStream = inputStream;
        database = FirebaseFirestore.getInstance();
    }

    public void uploadNewItem(ByteArrayInputStream inputStream){
        addToFirestore();
        uploadImage(inputStream);
        Log.d("UPL", "UPLOAD COMPLETED");
    }

    private void addToFirestore(){
        final Map<String, Object> newMap = new HashMap<>();
        newMap.put("category", this.newDocInfo[0]);
        newMap.put("title", this.newDocInfo[1]);

        Log.d("DB", "In addFirestore: category - " + this.newDocInfo[0]);
        DocumentReference documentReference = database.collection("categorias")
                .document(this.newDocInfo[0]);
        documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                long count = (long) documentSnapshot.getData().get("count");
                String curr;
                int itemId;

                if (newDocInfo[0].equals("arreglos")) {
                    curr = "arreglos-";
                    itemId = 2000 + (int) count;
                } else if (newDocInfo[0].equals("decoraciones")) {
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
                newMap.put("opciones", Arrays.asList(newDocInfo[2]));

                newDoc = newMap;

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

                newDoc.put("des", newDocInfo[3]);
                newDoc.remove("opciones");

                database.collection("categorias")
                        .document((String) newDoc.get("category"))
                        .collection("item-info")
                        .document(newDoc.get("imgSrc") + ".jpg")
                        .set(newDoc)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.v("FSDB", "DATA ADDED!");
                            }
                        });

                database.collection("categorias").document((String) newDoc.get("category"))
                        .update("count", ++count);
            }
        });
    }

    private void uploadImage(ByteArrayInputStream imageStream){
        try {
            FTPSClient ftp = new FTPSClient();

            Log.d("FTP", "Connecting...");


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
                    ftp.storeFile((String) newDoc.get("imgSrc") + ".jpg", imageStream);
                    Log.d("FTP", "Uploaded: " + (String) newDoc.get("imgSrc") + ".png");
                } else {
                    Log.d("FTP", "COULD NOT CHANGE DIRECTORY");
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        uploadNewItem(this.imageInputStream);
    }
}
