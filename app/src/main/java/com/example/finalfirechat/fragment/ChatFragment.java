package com.example.finalfirechat.fragment;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.example.finalfirechat.R;
import com.example.finalfirechat.adapter.MessageAdapter;
import com.example.finalfirechat.databinding.FragmentChatBinding;
import com.example.finalfirechat.model.Message;
import com.example.finalfirechat.model.ReceiveMessage;
import com.example.finalfirechat.viewmodel.ChatViewModel;

import java.util.ArrayList;

public class ChatFragment extends Fragment {

    Context context;
    private FragmentChatBinding binding;
    String self_uuid;
    String person_uuid;
    String person_name;

    ChatViewModel chatViewModel;
    ArrayList<Message> messageArrayList;
    MessageAdapter adapter;

    Dialog dialog;

    String SEND_TYPE = "text";

    public ChatFragment(Context context) {
        this.context = context;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);

        init();

        // TODO: 26/04/23 send get pre existing messages from database command
        chatViewModel.getMessagesList().observe(getActivity(), messages -> {
            for (int i = 0; i < messages.size(); i++) {
                if (messages.get(i).getSenderId().equals(self_uuid) || messages.get(i).getSenderId().equals(person_uuid)){
                    messageArrayList.addAll(messages);
                    adapter.notifyDataSetChanged();
                }
            }
        });
        chatViewModel.setReceiveMessage(new ReceiveMessage(null, person_uuid, null, null));

        binding.chatSendBtn.setOnClickListener(v -> {
            String msg = binding.edtMessage.getText().toString();
            binding.edtMessage.setText("");
            Message message = new Message(msg, self_uuid, String.valueOf(System.currentTimeMillis()), SEND_TYPE);
            messageArrayList.add(message);
            adapter.notifyDataSetChanged();
            chatViewModel.setReceiveMessage(new ReceiveMessage(msg, person_uuid, String.valueOf(System.currentTimeMillis()), SEND_TYPE));
        });

        binding.chatAttach.setOnClickListener(v->chatAttach());

        return binding.getRoot();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void chatAttach() {

        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottom_sheet_layout);

        LinearLayoutCompat photos = dialog.findViewById(R.id.photos);
        LinearLayoutCompat videos = dialog.findViewById(R.id.videos);
        LinearLayoutCompat location = dialog.findViewById(R.id.location);
        LinearLayoutCompat document = dialog.findViewById(R.id.documents);
        LinearLayoutCompat audio = dialog.findViewById(R.id.audios);
        LinearLayoutCompat contact = dialog.findViewById(R.id.contacts);

        photos.setOnClickListener(view -> {
            SEND_TYPE = "photos";
            chooseFile("photos");
        });
        videos.setOnClickListener(view -> {
            SEND_TYPE = "videos";
            chooseFile("videos");
        });
        location.setOnClickListener(view -> {
            SEND_TYPE = "location";
            messageArrayList.add(new Message("Your live location has been send", self_uuid, String.valueOf(System.currentTimeMillis()), "location"));
            adapter.notifyDataSetChanged();
            chatViewModel.setReceiveMessage(new ReceiveMessage("", person_uuid, String.valueOf(System.currentTimeMillis()), "location"));
        });
        document.setOnClickListener(view -> {
            SEND_TYPE = "documents";
            chooseFile("documents");
        });
        audio.setOnClickListener(view -> {
            SEND_TYPE = "audios";
            chooseFile("audios");
        });
        contact.setOnClickListener(view -> {
            SEND_TYPE = "contacts";
//            chooseFile("contacts");
        });

        SEND_TYPE = "photos";
        chooseFile("photos");
    }

    private void chooseFile(String type) {
        dialog.dismiss();
        boolean sdCard = false;
        Bundle bundle = new Bundle();
        bundle.putString("type", type);
        if(!sdCard) bundle.putString("path", System.getenv("EXTERNAL_STORAGE"));
        else bundle.putString("path", "");
        FileFragment fileFragment = new FileFragment(context);
        fileFragment.setArguments(bundle);
        getParentFragmentManager().beginTransaction().replace(R.id.frameLayout, fileFragment).addToBackStack(null).commit();
    }

    private void init() {
        messageArrayList = new ArrayList<>();

        self_uuid = getArguments().getString("self_uuid");
        person_uuid = getArguments().getString("person_uuid");
        person_name = getArguments().getString("person_name");

        binding.chatUserName.setText(person_name);
        binding.chatBackBtn.setOnClickListener(v -> getActivity().onBackPressed());

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setStackFromEnd(true);
        adapter = new MessageAdapter(context, messageArrayList, self_uuid);
        binding.chatRecyclerView.setLayoutManager(linearLayoutManager);
        binding.chatRecyclerView.setAdapter(adapter);

        chatViewModel = new ViewModelProvider(requireActivity()).get(ChatViewModel.class);
    }
}