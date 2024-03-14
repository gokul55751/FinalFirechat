package com.example.finalfirechat;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.example.finalfirechat.databinding.ActivityCreateProfileBinding;
import com.example.finalfirechat.network.SocketHandler;

import org.json.JSONObject;

import java.util.UUID;

import io.socket.client.Socket;

public class CreateProfileActivity extends AppCompatActivity {
    ActivityCreateProfileBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String uuid = UUID.randomUUID().toString();

        SocketHandler.setSocket();
        Socket socket = SocketHandler.getSocket();
        socket.connect();

        binding.saveButton.setOnClickListener(v->{
            try{
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("uuid", uuid);
                jsonObject.put("name", binding.edtName.getText().toString());
                jsonObject.put("number", binding.edtNumber.getText().toString());
                jsonObject.put("image", binding.edtImage.getText().toString());
                socket.emit("createProfile", jsonObject);
            }catch (Exception e){}
        });

        socket.on(uuid+"createProfile", args -> runOnUiThread(() -> {
            JSONObject jsonObject = (JSONObject) args[0];
            try{
                String selfUuid = jsonObject.getString("uuid");
                if(selfUuid!=null){
                    SharedPreferences sharedPreferences = getSharedPreferences("application", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("self_uuid", selfUuid);
                    editor.apply();
                    startActivity(new Intent(CreateProfileActivity.this, MainActivity.class));
                    finish();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }));
    }
}