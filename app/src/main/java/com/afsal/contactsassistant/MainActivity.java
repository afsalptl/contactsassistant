package com.afsal.contactsassistant;

import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.os.Environment;
import android.os.RemoteException;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.database.Cursor;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MEDIA";
    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv = (TextView) findViewById(R.id.TextView01);

        Button view = (Button)findViewById(R.id.btnbackup);
        Button add = (Button)findViewById(R.id.btnrestore);

        view.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                new backup_Async().execute();
                Log.i("Contacts Assistant", "Backup generated");
            }
        });

        add.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                new restore_Async().execute();
                Log.i("Contacts Assistant", "Restore Completed");
            }
        });
    }


    private void backup() {

        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

        String contact_string = "";

        if (cur != null && cur.getCount() > 0)
        {
            while (cur.moveToNext())
            {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                if (Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?", new String[]{id}, null);
                    while (pCur != null && pCur.moveToNext())
                    {
                        String phoneNo = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        String temp = name + "\n" + phoneNo + "\n";
                        contact_string = contact_string + temp;
                    }
                    if(pCur!=null)
                        pCur.close();
                }
            }
        }
        if(cur!=null)
        cur.close();
        writeToSDFile(contact_string);
    }

    public void restore()
    {
        String name,number;
        final File file = new File(Environment.getExternalStorageDirectory()
                .getAbsolutePath(), "/download/myData.txt");

        try {
            FileInputStream fin = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fin));

            while(true){
                try {
                    name = reader.readLine();
                        if(name==null)
                            break;
                    number = reader.readLine();
                    createContact(name,number);

                } catch (IOException ex) {
                    Log.i("FILE", "reading error");
                }
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i("FILE", "FIle Not Found");
        }
    }

    private void checkExternalMedia(){
        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // Can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // Can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Can't read or write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }
        //tv.append("\n\nExternal Media: readable="
                //+ mExternalStorageAvailable + " writable=" + mExternalStorageWriteable);
    }

    private void writeToSDFile(String str){

        File root = android.os.Environment.getExternalStorageDirectory();
        //tv.append("\nExternal file system root: " + root);

        File dir = new File (root.getAbsolutePath() + "/download");
        dir.mkdirs();
        File file = new File(dir, "myData.txt");

        try {
            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);
            pw.print(str);
            pw.flush();
            pw.close();
            f.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i(TAG, "File Not Found");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createContact(String name, String phone) {
        ContentResolver cr = getContentResolver();

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, "accountname@gmail.com")
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, "com.google")
                .build());
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build());
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_HOME)
                .build());


        try {
            cr.applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

private class backup_Async extends AsyncTask<String, Void, String> {
    ProgressDialog progress;

    @Override
    protected String doInBackground(String... params) {
            backup();
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        // execution of result of Long time consuming operation
        super.onPostExecute(result);
        progress.dismiss();
        Toast.makeText(MainActivity.this, "Backup file Generated",Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onPreExecute() {
        // Things to be done before execution of long running operation. For
        progress = new ProgressDialog(MainActivity.this);
        progress.setCancelable(false);
        progress.setMessage("Generating Backup...");
        progress.show();
    }
}


    private class restore_Async extends AsyncTask<String, Void, String> {
        ProgressDialog progress;

        @Override
        protected String doInBackground(String... params) {
            restore();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            // execution of result of Long time consuming operation
            super.onPostExecute(result);
            progress.dismiss();
            Toast.makeText(MainActivity.this, "Contacts restored",Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onPreExecute() {
            // Things to be done before execution of long running operation. For

            progress = new ProgressDialog(MainActivity.this);
            progress.setCancelable(false);
            progress.setMessage("Restoring Backup...");
            progress.show();
        }
    }



}