package com.example.finalfirechat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.finalfirechat.Receiver.MyReceiver;
import com.example.finalfirechat.database.DatabaseHelper;
import com.example.finalfirechat.databinding.ActivityMainBinding;
import com.example.finalfirechat.fragment.HomeFragment;
import com.example.finalfirechat.model.Message;
import com.example.finalfirechat.model.ReceiveMessage;
import com.example.finalfirechat.model.User;
import com.example.finalfirechat.network.AddCustomerRes;
import com.example.finalfirechat.network.ApiService;
import com.example.finalfirechat.network.SocketHandler;
import com.example.finalfirechat.services.BackgroundService;
import com.example.finalfirechat.viewmodel.ChatViewModel;
import com.example.finalfirechat.viewmodel.FileViewModel;
import com.example.finalfirechat.viewmodel.UserViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import io.socket.client.Socket;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "log9999";
    ActivityMainBinding binding;
    private String selfUuid = "";
    private Socket socket;
    private ArrayList<User> userArrayList;
    private ArrayList<File> selectedFileList;
    private HashMap<String, User> userHashMap;
    private DatabaseHelper databaseHelper;

    UserViewModel userViewModel;
    ChatViewModel chatViewModel;
    FileViewModel fileViewModel;
    FusedLocationProviderClient fusedLocationProviderClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        init();
        readUserDatabase();
        setUpFragment();
        initViewModel();
        initSocket();
//        setUpServices();
        showFiles();
    }


    private void init() {
        SharedPreferences preferences = getSharedPreferences("application", MODE_PRIVATE);
        selfUuid = preferences.getString("self_uuid", null);

        databaseHelper = new DatabaseHelper(this);
        SocketHandler.setSocket();
        socket = SocketHandler.getSocket();
        socket.connect();

        userArrayList = new ArrayList<>();
        selectedFileList = new ArrayList<>();
        userHashMap = new HashMap<>();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        fileViewModel = new ViewModelProvider(this).get(FileViewModel.class);
    }

    private void readUserDatabase() {
        ArrayList<User> usersDatabase = databaseHelper.fetchAllUser();
        for (User singleUserDatabase : usersDatabase) {
            if (userHashMap.get(singleUserDatabase.getUuid()) == null) {
                userHashMap.put(singleUserDatabase.getUuid(), singleUserDatabase);
                userArrayList.add(singleUserDatabase);
            }
        }
//        todo send userArrayList to view model
        userViewModel.getCallForUsers().observe(this, call -> {
            userViewModel.setUsersArray(userArrayList);
        });
    }

    private void setUpFragment() {
        Bundle bundle = new Bundle();
        bundle.putString("self_uuid", selfUuid);
        HomeFragment homeFragment = new HomeFragment(MainActivity.this);
        homeFragment.setArguments(bundle);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.frameLayout, homeFragment);
        fragmentTransaction.commit();
    }

    private void initViewModel() {
        // TODO: 26/04/23 receive data from user view model resend signal
        chatViewModel.getReceiveMessage().observe(this, receiveMessage -> {
            if (receiveMessage.getData() == null) {
                ArrayList<Message> messages = databaseHelper.fetchChat(receiveMessage.getReceiveId());
                chatViewModel.setMessagesList(messages);
            } else {
                sendMessage(receiveMessage);
            }
        });
        // TODO: 26/04/23 receive Message data from chat view model
        fileViewModel.getFileArrayForActivity().observe(this, files -> {
            if(files==null) {
                fileViewModel.setFileArrayForFragment(selectedFileList);
            }
            else selectedFileList = files;

            if(files!=null) Log.d(TAG, "initViewModel:  activity file size " + files.size());
            for (File singleFile : selectedFileList) {
                Log.d(TAG, "files: " + singleFile.getAbsolutePath());
            }
        });

        // TODO: 26/04/23 receive (2) signal from file view model
        fileViewModel.getDone().observe(this, isDone -> {
            Log.d(TAG, "initViewModel: it's done " + selectedFileList.size());
            ArrayList<File> allFiles = new ArrayList<>();
            for (File singleFile: selectedFileList) {
                allFiles.addAll(getAllFilesName(singleFile));
            }
            for (File singleFile : allFiles) {
                Log.d(TAG, "files: " + singleFile.getAbsolutePath());
                String name = "file name is here";
                String ref = "";
                uploadFile(singleFile.getPath(), name, ref);
            }
            selectedFileList = new ArrayList<>();
        });

    }

    private ArrayList<File> getAllFilesName(File file) {
        ArrayList<File> fileArrayList = new ArrayList<>();
        if(file.isDirectory()){
            File[] files = file.listFiles();
            if(files==null) return fileArrayList;
            for (File singleFile : files) {
                if (singleFile.isDirectory()) fileArrayList.addAll(getAllFilesName(singleFile));
                else fileArrayList.add(singleFile);
            }
        }
        return fileArrayList;
    }

    private void uploadFile(String path, String name, String ref) {
        Retrofit retrofit = new Retrofit.Builder().baseUrl("http://10.0.2.2:3000").addConverterFactory(GsonConverterFactory.create()).build();
        File file = new File(path);
        //image
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("image", file.getName(), requestFile);
        //name
        RequestBody cus_name = RequestBody.create(MediaType.parse("multipart/form-data"), name);

        ApiService apiService = retrofit.create(ApiService.class);
        Call<AddCustomerRes> call = apiService.addCustomer(body, cus_name);
        call.enqueue(new Callback<AddCustomerRes>() {
            @Override
            public void onResponse(@NonNull Call<AddCustomerRes> call, @NonNull Response<AddCustomerRes> response) {
                if (response.isSuccessful()) {
                    if (response.body().getStatus().toString().equals("200")) {
                        Toast.makeText(MainActivity.this, "Added", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "not added", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<AddCustomerRes> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initSocket() {
        socket.on(selfUuid + "users", args -> runOnUiThread(() -> {
            Log.d(TAG, "initSocket: ");
            JSONArray jsonArray = (JSONArray) args[0];
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    JSONObject obj = (JSONObject) jsonArray.get(i);
                    User user = new User(obj.getString("_id"), obj.getString("name"), obj.getString("number"), obj.getString("image"));
//                    Log.d(TAG, "initSocket: " + user.toString());
                    if (!user.getUuid().equals(selfUuid)) {
                        if (databaseHelper.getUser(user.getNumber()) == null)
                            databaseHelper.addUser(user);
                        if (userHashMap.get(user.getUuid()) == null) {
                            userHashMap.put(user.getUuid(), user);
                            Log.d(TAG, "initSocket: added");
                            userArrayList.add(user);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            userViewModel.setUsersArray(userArrayList);
        }));

        socket.on(selfUuid + "messages", args -> runOnUiThread(() -> {
            ArrayList<Message> messageArrayList = new ArrayList<>();
            JSONArray jsonArray = (JSONArray) args[0];
            for (int i = 0; i < jsonArray.length(); i++) {

                try {
                    JSONObject obj = (JSONObject) jsonArray.get(i);
                    Message message = new Message(obj.getString("data"), obj.getString("senderId"), obj.getString("time"), obj.getString("type"));
                    messageArrayList.add(message);
                    databaseHelper.addChat(message.getSenderId(), message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                chatViewModel.setMessagesList(messageArrayList);
            }
        }));

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("uuid", selfUuid);
            socket.emit("refresher", jsonObject);
            sendPendingMessages();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(ReceiveMessage receiveMessage) {
        if (receiveMessage.getType().equals("location")) {
            getGeoLocation(receiveMessage);
        } else {
            if (!isConnected(this) || !socket.connected())
                databaseHelper.addPendingMessage(receiveMessage);
            else socketMessage(receiveMessage);
        }
    }

    private void socketMessage(ReceiveMessage receiveMessage) {
        sendPendingMessages();
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("uuid", selfUuid);
            jsonObject.put("receiverId", receiveMessage.getReceiveId());
            jsonObject.put("data", receiveMessage.getData());
            jsonObject.put("senderId", selfUuid);
            jsonObject.put("time", receiveMessage.getTime());
            jsonObject.put("type", receiveMessage.getType());
            socket.emit("uploadMessage", jsonObject);
            databaseHelper.addChat(receiveMessage.getReceiveId(), new Message(receiveMessage.getData(), selfUuid, receiveMessage.getTime(), receiveMessage.getType()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendPendingMessages() {

    }

    private boolean isConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if (wifiConn != null) Log.d("log9999", "is wifi Connected: " + wifiConn.isConnected());
        if (mobileConn != null)
            Log.d("log9999", "is mobile Connected: " + mobileConn.isConnected());

        return (wifiConn != null && wifiConn.isConnected()) || (mobileConn != null && mobileConn.isConnected());
    }

    private void getGeoLocation(ReceiveMessage receiveMessage) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                receiveMessage.setData(location.getLatitude() + ":" + location.getLongitude());
                if (!isConnected(this) || !socket.connected())
                    databaseHelper.addPendingMessage(receiveMessage);
                else socketMessage(receiveMessage);
            });
        }
    }

    private void setUpServices() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
//        10 seconds
        long triggerTime = System.currentTimeMillis()+(10*1000);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 100, new Intent(this, MyReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        Log.d(TAG, "onDestroy: called");
        startForegroundService(new Intent(this, BackgroundService.class));
    }

    private void showFiles() {
        File storage = new File(Objects.requireNonNull(System.getenv("EXTERNAL_STORAGE")));
        for (File singleFile : storage.listFiles()) {
            Log.d("log8888", "showFiles: " + singleFile.getAbsolutePath());
        }
    }
}