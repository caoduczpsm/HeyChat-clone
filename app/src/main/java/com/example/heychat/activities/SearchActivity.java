package com.example.heychat.activities;

import static com.gun0912.tedpermission.provider.TedPermissionProvider.context;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import com.example.heychat.R;
import com.example.heychat.adapters.ChatBottomSheetFragment;
import com.example.heychat.adapters.ChatGroupBottomSheetFragment;
import com.example.heychat.adapters.GroupAdapter;
import com.example.heychat.adapters.UsersAdapter;
import com.example.heychat.databinding.ActivitySearchBinding;
import com.example.heychat.listeners.GroupListener;
import com.example.heychat.listeners.UserListener;
import com.example.heychat.models.Group;
import com.example.heychat.models.User;
import com.example.heychat.ultilities.Constants;
import com.example.heychat.ultilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;


public class SearchActivity extends AppCompatActivity implements UserListener, GroupListener {

    private ActivitySearchBinding binding;
    FirebaseFirestore database;
    private UsersAdapter usersAdapter;
    private List<User> users;
    private List<Group> groups;
    private GroupAdapter groupAdapter;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(this);
        setListeners();
        users = new ArrayList<>();
        groups = new ArrayList<>();
        usersAdapter = new UsersAdapter(users, this);
        groupAdapter = new GroupAdapter(groups, this);
        binding.userRecyclerView.setAdapter(usersAdapter);
    }

    private void searchGroup(String searchText) {
        binding.userRecyclerView.setAdapter(groupAdapter);
        loading(true);
        database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_GROUP)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    if (task.isSuccessful() && task.getResult() != null) {
                        groups.clear();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            if (queryDocumentSnapshot.getString(Constants.KEY_GROUP_NAME).contains(searchText) && !searchText.isEmpty() && searchText != "") {
                                Log.d("SearchTAG", queryDocumentSnapshot.getId());
                                Group group = new Group();
                                group.name = queryDocumentSnapshot.getString(Constants.KEY_GROUP_NAME);
                                group.image = queryDocumentSnapshot.getString(Constants.KEY_GROUP_IMAGE);
//                                group.token = queryDocumentSnapshot.getString(Constants.KEY_GROUP_OWNER);
                                group.id = queryDocumentSnapshot.getId();
                                groups.add(group);
                            }

                        }
                        for (Group group: groups){
                            database.collection(Constants.KEY_COLLECTION_USER).document(preferenceManager.getString(Constants.KEY_USER_ID))
                                    .collection(Constants.KEY_GROUP_ID).document(group.id).get().addOnSuccessListener(documentSnapshot -> {
                                        if (!documentSnapshot.exists() || !documentSnapshot.getBoolean("Owner") == true){
                                            groups.remove(group);
                                        }
                                        binding.textErrorMessage.setVisibility(View.GONE);
                                        groupAdapter.notifyDataSetChanged();
                                        if (groups.size() <= 0) {
                                            showErrorMessage();
                                        }
                                    });
                        }

                    } else {
                        showErrorMessage();
                    }
                });
    }

    private void searchUser(String searchText) {
        binding.userRecyclerView.setAdapter(usersAdapter);
        loading(true);
        database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USER)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    if (task.isSuccessful() && task.getResult() != null) {
                        users.clear();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            if (queryDocumentSnapshot.getString(Constants.KEY_EMAIL).equals(searchText) && !searchText.isEmpty() && !queryDocumentSnapshot.getString(Constants.KEY_EMAIL).equals(preferenceManager.getString(Constants.KEY_EMAIL))) {
                                User user = new User();
                                user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                                user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                                user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                                user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                                user.id = queryDocumentSnapshot.getId();
                                users.add(user);
                            }
                        }
                        binding.textErrorMessage.setVisibility(View.GONE);
                        usersAdapter.notifyDataSetChanged();
                        if (users.size() <= 0) {
//                            searchGroup(searchText);
                            showErrorMessage();
                        }
                    } else {
                        showErrorMessage();
                    }
                });
    }


    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());

        binding.edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try {
                    double number = Double.parseDouble(binding.edtSearch.getText().toString().trim());
                    binding.userRecyclerView.setAdapter(groupAdapter);
                    searchUser(binding.edtSearch.getText().toString().trim());
                } catch (NumberFormatException e) {
                    binding.userRecyclerView.setAdapter(usersAdapter);
                    if (binding.edtSearch.getText().length() > 0){
                        groups.clear();
                        Log.d("SearchTAG", "group");
                        searchGroup(binding.edtSearch.getText().toString().trim());
                    } else {
                        Log.d("SearchTAG", "null");
                        groups.clear();
                        groupAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    private void showErrorMessage() {
        binding.textErrorMessage.setText(String.format("%s", getString(R.string.No_user)));
        binding.textErrorMessage.setVisibility(View.VISIBLE);
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onUserClicker(User user) {
        ChatBottomSheetFragment bottomSheetFragment = ChatBottomSheetFragment.newInstance(user);
        bottomSheetFragment.show(getSupportFragmentManager(), bottomSheetFragment.getTag());
    }

    @Override
    public void onGroupClicker(Group group) {
        Group groupIntent = new Group();
        groupIntent.id = group.id;
        groupIntent.image = group.image;
        groupIntent.name = group.name;
        //group = (Group) getIntent().getSerializableExtra(Constants.KEY_GROUP);
        database.collection(Constants.KEY_COLLECTION_GROUP).document(group.id)
                .collection(Constants.KEY_GROUP_MEMBER).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<String> members = new ArrayList<>();
                    for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots) {
                        String member = queryDocumentSnapshot.getId();
                        members.add(member);
                    }
                    groupIntent.member = members;
                    ChatGroupBottomSheetFragment bottomSheetDialog = ChatGroupBottomSheetFragment.newInstance(groupIntent);
                    bottomSheetDialog.show(getSupportFragmentManager(), bottomSheetDialog.getTag());
                });
    }
}