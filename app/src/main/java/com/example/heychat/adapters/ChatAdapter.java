package com.example.heychat.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.heychat.databinding.ItemContainerReceivedMessageBinding;
import com.example.heychat.databinding.ItemContainerSentMessageBinding;
import com.example.heychat.listeners.MessageListener;
import com.example.heychat.models.ChatMessage;
import com.example.heychat.ultilities.Constants;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public List<ChatMessage> chatMessages;
    private Bitmap receiverProfileImage;
    private final String senderId;
    public MessageListener messageListener;

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_SENT_IMAGE = 3;
    private static final int VIEW_TYPE_RECEIVED_IMAGE = 4;

    public void setReceiverProfileImage(Bitmap bitmap) {
        receiverProfileImage = bitmap;
    }

    public ChatAdapter(List<ChatMessage> chatMessages, Bitmap receiverProfileImage, String senderId, MessageListener messageListener) {

        this.receiverProfileImage = receiverProfileImage;
        this.senderId = senderId;
        this.chatMessages = chatMessages;
        this.messageListener = messageListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT || viewType == VIEW_TYPE_SENT_IMAGE) {
            return new SentMessageViewHolder(
                    ItemContainerSentMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        } else {
            return new ReceivedMessageViewHolder(
                    ItemContainerReceivedMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).setTextData(chatMessages.get(position));
            if (position == chatMessages.size() - 1) {
                ((SentMessageViewHolder) holder).binding.textSeenMessage.setVisibility(View.VISIBLE);
            } else {
                ((SentMessageViewHolder) holder).binding.textSeenMessage.setVisibility(View.GONE);
            }
            if (chatMessages.get(position).isSeen)
                ((SentMessageViewHolder) holder).binding.textSeenMessage.setText("Seen");
            else
                ((SentMessageViewHolder) holder).binding.textSeenMessage.setText("Delivered");

        } else if (getItemViewType(position) == VIEW_TYPE_RECEIVED) {
            ((ReceivedMessageViewHolder) holder).setTextData(chatMessages.get(position), receiverProfileImage);
        } else if (getItemViewType(position) == VIEW_TYPE_SENT_IMAGE) {
            ((SentMessageViewHolder) holder).setImageData(chatMessages.get(position));
        } else if (getItemViewType(position) == VIEW_TYPE_RECEIVED_IMAGE) {
            ((ReceivedMessageViewHolder) holder).setImageData(chatMessages.get(position), receiverProfileImage);
        }

    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        if (chatMessages.get(position).type != null) {
            if (chatMessages.get(position).senderId.equals(senderId)) {
                if (chatMessages.get(position).type.equals(Constants.MESSAGE_TEXT)) {
                    return VIEW_TYPE_SENT;
                } else if (chatMessages.get(position).type.equals(Constants.MESSAGE_IMAGE)) {
                    return VIEW_TYPE_SENT_IMAGE;
                }

            } else {
                if (chatMessages.get(position).type.equals(Constants.MESSAGE_IMAGE)) {
                    return VIEW_TYPE_RECEIVED_IMAGE;
                }
            }
        }

        if (chatMessages.get(position).model.equals("receiver")) {
            if (position != 0) {
                if (chatMessages.get(position - 1).model.equals("sender")) {
                    chatMessages.get(position).lastReceiver = true;
                }
            }
        }

        if (chatMessages.get(0).model.equals("receiver"))
            chatMessages.get(0).lastReceiver = true;

        return VIEW_TYPE_RECEIVED;
    }

    class SentMessageViewHolder extends RecyclerView.ViewHolder{

        private final ItemContainerSentMessageBinding binding;

        SentMessageViewHolder(ItemContainerSentMessageBinding itemContainerSentMessageBinding) {
            super(itemContainerSentMessageBinding.getRoot());
            binding = itemContainerSentMessageBinding;


            binding.layoutMessage.setOnClickListener(view ->
                    messageListener.onMessageSelection(chatMessages.get(getAdapterPosition()).isSelected,
                            getAdapterPosition(), chatMessages, chatMessages.get(getAdapterPosition())));

        }



        void setTextData(ChatMessage chatMessage) {
            binding.textMessage.setText(chatMessage.message);
            binding.textMessage.setVisibility(ViewGroup.VISIBLE);
            binding.textDateTime.setText(chatMessage.dateTime);

        }

        void setImageData(ChatMessage chatMessage) {
            binding.layoutMessage.setBackground(null);
            binding.imageMessage.setImageBitmap(getUserImage(chatMessage.message));
            binding.imageMessage.setVisibility(ViewGroup.VISIBLE);
            binding.textDateTime.setText(chatMessage.dateTime);

        }

        private Bitmap getUserImage(String encodedImage) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }

    }


    class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {

        private final ItemContainerReceivedMessageBinding binding;

        ReceivedMessageViewHolder(ItemContainerReceivedMessageBinding itemContainerReceivedMessageBinding) {
            super(itemContainerReceivedMessageBinding.getRoot());
            binding = itemContainerReceivedMessageBinding;


            binding.layoutMessage.setOnClickListener(view ->
                    messageListener.onMessageSelection(chatMessages.get(getAdapterPosition()).isSelected,
                            getAdapterPosition(), chatMessages, chatMessages.get(getAdapterPosition())));

        }

        void setTextData(ChatMessage chatMessage, Bitmap receiverProfileImage) {
            binding.textMessage.setText(chatMessage.message);
            binding.textMessage.setVisibility(View.VISIBLE);
            binding.textDateTime.setText(chatMessage.dateTime);

            if (receiverProfileImage != null) {
                binding.imageProfile.setImageBitmap(receiverProfileImage);
            }
            if (chatMessage.lastReceiver) {
                binding.imageProfile.setVisibility(View.VISIBLE);
            } else {
                binding.imageProfile.setVisibility(View.GONE);
            }

        }

        void setImageData(ChatMessage chatMessage, Bitmap receiverProfileImage) {
            binding.layoutMessage.setBackground(null);
            binding.imageMessage.setImageBitmap(getUserImage(chatMessage.message));
            binding.imageMessage.setVisibility(View.VISIBLE);
            binding.textDateTime.setText(chatMessage.dateTime);

            if (receiverProfileImage != null) {
                binding.imageProfile.setImageBitmap(receiverProfileImage);
            }
        }

        private Bitmap getUserImage(String encodedImage) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }

    }

}
