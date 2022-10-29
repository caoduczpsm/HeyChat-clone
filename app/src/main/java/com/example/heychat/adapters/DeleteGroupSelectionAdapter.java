package com.example.heychat.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heychat.R;
import com.example.heychat.listeners.GroupMemberListener;
import com.example.heychat.listeners.UserSelectionListener;
import com.example.heychat.models.User;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class DeleteGroupSelectionAdapter extends RecyclerView.Adapter<DeleteGroupSelectionAdapter.GroupMemberSelectionViewHolder>{

    private final List<User> users;
    private final GroupMemberListener groupMemberListener;

    public DeleteGroupSelectionAdapter(List<User> users, GroupMemberListener groupMemberListener) {
        this.users = users;
        this.groupMemberListener = groupMemberListener;
    }

    @NonNull
    @Override
    public GroupMemberSelectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GroupMemberSelectionViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_container_user_selection,
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull GroupMemberSelectionViewHolder holder, int position) {
        holder.bindUserSelection(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public List<User> getSelectedUser(){
        List<User> selectedUser = new ArrayList<>();
        for (User user : users){
            if (user.isSelected){
                selectedUser.add(user);
            }
        }
        return selectedUser;
    }

    class GroupMemberSelectionViewHolder extends RecyclerView.ViewHolder{

        ConstraintLayout layoutUserSelection;
        CircleImageView  imageProfile;
        TextView textName, textEmail;
        ImageView imageSelected;
        View viewBackground;

        GroupMemberSelectionViewHolder(@NonNull View itemView){
            super(itemView);
            layoutUserSelection = itemView.findViewById(R.id.layoutUserSelection);
            imageProfile = itemView.findViewById(R.id.imageProfile);
            textName = itemView.findViewById(R.id.textName);
            textEmail = itemView.findViewById(R.id.textEmail);
            imageSelected = itemView.findViewById(R.id.imageSelected);
            viewBackground = itemView.findViewById(R.id.viewBackground);
        }

        void bindUserSelection(final User user){
            imageProfile.setImageBitmap(getUserImage(user.image));
            textName.setText(user.name);
            textEmail.setText(user.email);
            if (user.isSelected){
                viewBackground.setBackgroundResource(R.drawable.background_user_selected);
                imageSelected.setVisibility(View.VISIBLE);
            } else {
                viewBackground.setBackgroundResource(R.drawable.user_selection_background);
                imageSelected.setVisibility(View.GONE);
            }
            layoutUserSelection.setOnClickListener(view -> {
                if (user.isSelected){
                    viewBackground.setBackgroundResource(R.drawable.user_selection_background);
                    imageSelected.setVisibility(View.GONE);
                    user.isSelected = false;
                    if (getSelectedUser().size() == 0){
                        groupMemberListener.onGroupMemberSelection(false);
                    }
                } else{
                    viewBackground.setBackgroundResource(R.drawable.background_user_selected);
                    imageSelected.setVisibility(View.VISIBLE);
                    user.isSelected = true;
                    groupMemberListener.onGroupMemberSelection(true);
                }
            });
        }

        private Bitmap getUserImage(String encodedImage){
            byte[] bytes = new byte[0];
            if (encodedImage != null){
                bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            }

            return BitmapFactory.decodeByteArray(bytes,0, bytes.length);
        }

    }
}
