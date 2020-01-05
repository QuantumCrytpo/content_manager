package com.arasdecoraciones.aracontentmanager.util;

import android.os.AsyncTask;
import android.util.Log;

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

public class DatabaseHandler extends AsyncTask<Object, Integer, String> {

    private FirebaseFirestore database;
    private Map<String, Object> newItem;

    @Override
    protected void onPreExecute(){
        super.onPreExecute();
        database = FirebaseFirestore.getInstance();
    }

    @Override
    protected String doInBackground(Object... params){
        uploadNewItem((ByteArrayInputStream) params[0], (String[]) params[1]);
        return null;
    }



    public void uploadNewItem(ByteArrayInputStream inputStream, String... newDoc){
        addToFirestore(newDoc);
        uploadImage(inputStream);
        Log.d("UPL", "UPLOAD COMPLETED");
    }

    private void addToFirestore(final String... newDoc){
        final Map<String, Object> newMap = new HashMap<>();
        newMap.put("category", newDoc[0]);
        newMap.put("title", newDoc[1]);

        Log.d("DB", "In addFirestore: category - " + newDoc[0]);
        DocumentReference documentReference = database.collection("categorias")
                .document(newDoc[0]);
        documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                long count = (long) documentSnapshot.getData().get("count");
                String curr;
                int itemId;

                if (newDoc[0].equals("arreglos")) {
                    curr = "arreglos-";
                    itemId = 2000 + (int) count;
                } else if (newDoc[0].equals("decoraciones")) {
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
                newMap.put("opciones", Arrays.asList(newDoc[2]));

                newItem = newMap;

                database.collection("categorias")
                        .document((String) newItem.get("category"))
                        .collection("tile-info")
                        .document((String) newItem.get("imgSrc"))
                        .set(newItem)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.v("FSDB", "DATA ADDED: " + newItem.get("title"));
                            }
                        });

                newItem.put("des", newDoc[3]);
                String imgSrcForItem = newItem.get("imgSrc") + ".jpg";
                newItem.put("imgSrc", imgSrcForItem);
                newItem.remove("opciones");

                database.collection("categorias")
                        .document((String) newItem.get("category"))
                        .collection("item-info")
                        .document((String) newItem.get("imgSrc"))
                        .set(newItem)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.v("FSDB", "DATA ADDED!");
                            }
                        });

                //database.collection("categorias").document((String) newItem.get("category"))
                  //      .update("count", ++count);
            }
        });
    }

    private void uploadImage(ByteArrayInputStream imageStream){
        try {
            FTPSClient ftp = new FTPSClient();

            Log.d("FTP", "Connecting...");

            ftp.connect("ftp.arasdecoraciones.com");
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
                    Log.d("FTP", "Attempting to change directory: " + (String) newItem.get("category"));
                    ftp.cwd("images/" + (String) newItem.get("category"));
                    ftp.storeFile((String) newItem.get("imgSrc") + ".jpg", imageStream);
                    Log.d("FTP", "Uploaded: " + (String) newItem.get("imgSrc") + ".png");
                } else {
                    Log.d("FTP", "COULD NOT CHANGE DIRECTORY");
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }

    }
}
