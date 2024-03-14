package com.example.finalfirechat.fragment;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.finalfirechat.R;
import com.example.finalfirechat.adapter.MainUserAdapter;
import com.example.finalfirechat.databinding.FragmentHomeBinding;
import com.example.finalfirechat.listener.OnUserClickedListener;
import com.example.finalfirechat.model.User;
import com.example.finalfirechat.viewmodel.UserViewModel;

import java.util.ArrayList;
import java.util.HashMap;

public class HomeFragment extends Fragment implements OnUserClickedListener {

    private static final String TAG = "log9999";
    private FragmentHomeBinding binding;
    Context context;
    ArrayList<User> userArrayList = new ArrayList<>();
    UserViewModel userViewModel;
    private HashMap<String, User> userHashMap = new HashMap<>();
    private String selfUuid = "";


    public HomeFragment(Context context) {
        this.context = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        selfUuid = getArguments().getString("self_uuid");

        binding.mainRecycleView.setLayoutManager(new LinearLayoutManager(context));
        MainUserAdapter adapter = new MainUserAdapter(context, userArrayList, this);
        binding.mainRecycleView.setAdapter(adapter);

        UserViewModel userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        userViewModel.getUsersArray().observe(getActivity(), userArray -> {
            for (int i = 0; i < userArray.size(); i++) {
                if (userHashMap.get(userArray.get(i).getUuid()) == null) {
                    userHashMap.put(userArray.get(i).getUuid(), userArray.get(i));
                    userArrayList.add(userArray.get(i));
                }
            }
            adapter.notifyDataSetChanged();
        });

        userViewModel.setCallForUsers();

        return binding.getRoot();
    }

    @Override
    public void onUserClicked(String uuid, String name) {
        Bundle bundle = new Bundle();
        bundle.putString("self_uuid", selfUuid);
        bundle.putString("person_uuid", uuid);
        bundle.putString("person_name", name);
        ChatFragment chatFragment = new ChatFragment(context);
        chatFragment.setArguments(bundle);
        getParentFragmentManager().beginTransaction().replace(R.id.frameLayout, chatFragment).addToBackStack(null).commit();
    }
}