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
import com.example.finalfirechat.adapter.FileAdapter;
import com.example.finalfirechat.databinding.FragmentFileBinding;
import com.example.finalfirechat.listener.OnFileSelectedListener;
import com.example.finalfirechat.viewmodel.FileViewModel;
import com.example.finalfirechat.viewmodel.UserViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class FileFragment extends Fragment implements OnFileSelectedListener {

    FragmentFileBinding binding;
    FileViewModel fileViewModel;
    ArrayList<File> fileArrayList;
    ArrayList<File> selectedFileList;
    Context context;

    File storage;
    String type;
    String self_uuid;
    String person_uuid;
    String person_name;
    String argPath;

    public FileFragment(Context context) {
        this.context = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFileBinding.inflate(inflater, container, false);

        Log.d("log9999", "onCreateView: starting fragment");

        init();

        fileViewModel.getFileArrayForFragment().observe(getActivity(), files -> {
            if (files != null) {
                selectedFileList = files;
                Log.d("log9999", "onCreateView: total file to fragment - " + files.size());
            }
        });
        fileViewModel.setFileArrayForActivity(null);
//        fileViewModel.setDone(true);

        storage = new File(Objects.requireNonNull(System.getenv("EXTERNAL_STORAGE")));
        argPath = getArguments().getString("path");
        type = getArguments().getString("type");
        self_uuid = getArguments().getString("self_uuid");
        person_uuid = getArguments().getString("person_uuid");
        person_name = getArguments().getString("person_name");

        if (argPath.equals("")) {
        }
        if (argPath != null && !argPath.equals("")) storage = new File(argPath);
        if (type != null && !type.equals("")) type = "documents";

        displayFile();

        binding.doneButton.setOnClickListener(view -> {
            fileViewModel.setFileArrayForActivity(selectedFileList);
            fileViewModel.setDone(true);
            Bundle bundle = new Bundle();
            bundle.putString("self_uuid", self_uuid);
            bundle.putString("person_uuid", person_uuid);
            bundle.putString("person_name", person_name);
            ChatFragment chatFragment = new ChatFragment(context);
            chatFragment.setArguments(bundle);
            getParentFragmentManager().beginTransaction().replace(R.id.frameLayout, chatFragment).addToBackStack(null).commit();
        });

        return binding.getRoot();
    }

    private void displayFile() {
        binding.fileRecycleView.setHasFixedSize(true);
        binding.fileRecycleView.setLayoutManager(new LinearLayoutManager(getContext()));
        fileArrayList = findFiles(storage);
        Log.d("log8888", "showFiles: size" + fileArrayList.size());
        HashMap<String, File> selectedFileMap = new HashMap<>();
        for (File singleFile : selectedFileList) {
            selectedFileMap.put(singleFile.getAbsolutePath(), singleFile);
        }
        binding.fileRecycleView.setAdapter(new FileAdapter(type, getContext(), fileArrayList, selectedFileMap, this));
    }

    private ArrayList<File> findFiles(File path) {
        ArrayList<File> arrayList = new ArrayList<>();
        File[] files = path.listFiles();
        if (files == null) return arrayList;
        for (File singleFile : files) {
            if (singleFile.isDirectory() && !singleFile.isHidden()) {
                arrayList.add(singleFile);
            } else {
                arrayList.add(singleFile);
                // TODO: 27/04/23 switch case
            }
        }
        return arrayList;
    }

    private void init() {
        fileArrayList = new ArrayList<>();
        selectedFileList = new ArrayList<>();
        fileViewModel = new ViewModelProvider(requireActivity()).get(FileViewModel.class);
    }

    @Override
    public void onFileClicked(File file) {
        if (file.isDirectory()) {
            fileViewModel.setFileArrayForActivity(selectedFileList);
            Bundle bundle = new Bundle();
            bundle.putString("type", type);
            bundle.putString("path", file.getAbsolutePath());
            bundle.putString("self_uuid", self_uuid);
            bundle.putString("person_uuid", person_uuid);
            bundle.putString("person_name", person_name);
            FileFragment fileFragment = new FileFragment(context);
            fileFragment.setArguments(bundle);
            getParentFragmentManager().beginTransaction().replace(R.id.frameLayout, fileFragment).addToBackStack(null).commit();
        } else {
            selectedFileList.add(file);
        }
    }

    @Override
    public void onFileLongClicked(File file, int position) {
        selectedFileList.add(file);
    }


}