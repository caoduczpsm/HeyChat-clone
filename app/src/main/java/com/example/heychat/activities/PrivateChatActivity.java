package com.example.heychat.activities;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.example.heychat.R;
import com.example.heychat.adapters.PrivateChatAdapter;
import com.example.heychat.listeners.MessageListener;
import com.example.heychat.models.ChatMessage;
import com.example.heychat.models.RoomChat;
import com.example.heychat.ultilities.Constants;
import com.example.heychat.ultilities.PreferenceManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class PrivateChatActivity extends BaseActivity implements MessageListener {

    private FirebaseFirestore database;
    private RoomChat roomChat;
    private PreferenceManager preferenceManager;
    private List<ChatMessage> chatMessages;
    private PrivateChatAdapter chatAdapter;

    private AppCompatImageView imageBack;
    private RecyclerView chatRecyclerView;
    private EditText inputMessage;
    private CardView layoutSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_chat);
        init();
        setListener();
        listenMessages();
    }


    private void setListener() {

        layoutSend.setOnClickListener(v -> sendMessage());

        imageBack.setOnClickListener(view -> {

            deleteData();

            preferenceManager.remove(Constants.KEY_COLLECTION_ROOM);

            showToast("Delete All Message Successfully");
            Intent intent = new Intent(getApplicationContext(), MainActivity2.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void deleteData(){
        database.collection(Constants.KEY_COLLECTION_ROOM)
                .document(roomChat.id)
                .delete();

        database.collection(Constants.KEY_COLLECTION_ROOM)
                .document(roomChat.id)
                .collection(Constants.KEY_ROOM_MEMBER)
                .get()
                .addOnCompleteListener(task -> {
                    for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                        database.collection(Constants.KEY_COLLECTION_ROOM)
                                .document(roomChat.id)
                                .collection(Constants.KEY_ROOM_MEMBER)
                                .document(queryDocumentSnapshot.getId())
                                .delete();
                    }
                });
        database.collection(Constants.KEY_COLLECTION_ROOM)
                .get()
                .addOnCompleteListener(task -> {
                    for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                        if (Integer.parseInt(String.valueOf(queryDocumentSnapshot.get(Constants.KEY_AMOUNT_OF_ROOM))) == 0) {
                            database.collection(Constants.KEY_COLLECTION_ROOM)
                                    .document(queryDocumentSnapshot.getId())
                                    .delete();
                        }
                    }
                });

        database.collection(Constants.KEY_COLLECTION_PRIVATE_CHAT)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, roomChat.id)
                .get()
                .addOnCompleteListener(task -> {
                    for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                        database.collection(Constants.KEY_COLLECTION_PRIVATE_CHAT)
                                .document(queryDocumentSnapshot.getId())
                                .delete();
                    }
                });
    }

    private void init() {
        database = FirebaseFirestore.getInstance();
        roomChat = (RoomChat) getIntent().getSerializableExtra(Constants.KEY_COLLECTION_ROOM);
        preferenceManager = new PreferenceManager(getApplicationContext());
        imageBack = findViewById(R.id.imageBack);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        inputMessage = findViewById(R.id.inputeMessage);
        layoutSend = findViewById(R.id.layoutSend);


        chatMessages = new ArrayList<>();
        chatAdapter = new PrivateChatAdapter(
                chatMessages,
                preferenceManager.getString(Constants.KEY_USER_ID),
                this
        );
        chatRecyclerView.setAdapter(chatAdapter);
        chatRecyclerView.setItemAnimator(null);


    }

    private void sendMessage() {
        if (!inputMessage.getText().toString().equals("")){
            HashMap<String, Object> message = new HashMap<>();
            message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            message.put(Constants.KEY_RECEIVER_ID, roomChat.id);
            message.put(Constants.KEY_MESSAGE, inputMessage.getText().toString());
            message.put(Constants.KEY_TIMESTAMP, new Date());
            database.collection(Constants.KEY_COLLECTION_PRIVATE_CHAT).add(message);
            try {
                JSONArray tokens = new JSONArray();
                tokens.put(preferenceManager.getString(Constants.KEY_COLLECTION_ROOM));

                JSONObject data = new JSONObject();
                data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME));
                data.put(Constants.KEY_FCM_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                data.put(Constants.KEY_MESSAGE, inputMessage.getText().toString());

                JSONObject body = new JSONObject();
                body.put(Constants.REMOTE_MSG_DATA, data);
                body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

//                sendNotification(body.toString());

            } catch (Exception e) {
                //showToast(e.getMessage());
            }
        }


        inputMessage.setText(null);
    }

    private void listenMessages() {

        database.collection(Constants.KEY_COLLECTION_PRIVATE_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, roomChat.id)
                .addSnapshotListener(eventListener);


        database.collection(Constants.KEY_COLLECTION_PRIVATE_CHAT)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, roomChat.id)
                .addSnapshotListener(eventListener);

    }

    @SuppressLint("NotifyDataSetChanged")
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            int count = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dataObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }
            }

            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dataObject.compareTo(obj2.dataObject));
            if (count == 0) {
                chatAdapter.notifyDataSetChanged();
            } else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
                chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }

        }
    };

    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("dd MMMM, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }


    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }


    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onMessageSelection(Boolean isSelected, int position, List<ChatMessage> lastMessages, ChatMessage chatMessage) {
        final Dialog dialog = openDialog(R.layout.layout_dialog_message_selection);
        assert dialog != null;
        TextView textMessage = dialog.findViewById(R.id.textMessage);
        TextView textDateTime = dialog.findViewById(R.id.textDateTime);
        TextView textSeenMessage = dialog.findViewById(R.id.textSeenMessage);
        RelativeLayout layoutTranslate = dialog.findViewById(R.id.relativeLayoutTranslate);
        RelativeLayout layoutCopy = dialog.findViewById(R.id.relativeLayoutCopy);
        RelativeLayout layoutMultipleSelection = dialog.findViewById(R.id.relativeLayoutMultipleSelection);
        RelativeLayout layoutDelete = dialog.findViewById(R.id.relativeLayoutDelete);
        ImageView imageCheck = dialog.findViewById(R.id.imageCheck);

        textMessage.setText(lastMessages.get(position).message);
        textDateTime.setText(lastMessages.get(position).dateTime);
        if (lastMessages.get(position).isSeen){
            textSeenMessage.setText("Seen");
            imageCheck.setVisibility(View.VISIBLE);
        } else {
            textSeenMessage.setText("Delivered");
            imageCheck.setVisibility(View.GONE);
        }

        layoutTranslate.setOnClickListener(view -> {
            TranslatorOptions options;
            if (Objects.equals(preferenceManager.getString(Constants.KEY_LANGUAGE), "VI")) {
                options = new TranslatorOptions.Builder()
                        .setSourceLanguage(TranslateLanguage.ENGLISH)
                        .setTargetLanguage(TranslateLanguage.VIETNAMESE)
                        .build();
            } else {
                options = new TranslatorOptions.Builder()
                        .setSourceLanguage(TranslateLanguage.VIETNAMESE)
                        .setTargetLanguage(TranslateLanguage.ENGLISH)
                        .build();
            }

            Translator englishVITranslator = Translation.getClient(options);

            getLifecycle().addObserver(englishVITranslator);


            englishVITranslator.downloadModelIfNeeded().addOnSuccessListener(unused -> englishVITranslator.translate(textMessage.getText().toString())
                    .addOnSuccessListener(textMessage::setText)
                    .addOnFailureListener(e -> showToast(e.getMessage()))).addOnFailureListener(e -> showToast(e.getMessage()));

            showToast("Translated the message");
        });

        layoutCopy.setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("message", textMessage.getText().toString());
            clipboard.setPrimaryClip(clip);
            showToast("Copied the message!");
            dialog.dismiss();
        });

        layoutMultipleSelection.setOnClickListener(view -> {

        });

        layoutDelete.setOnClickListener(view -> {
            chatMessages.remove(position);
            chatAdapter.notifyItemRemoved(position);
            chatAdapter.notifyItemChanged(position);
            chatAdapter.notifyDataSetChanged();
            chatAdapter.notifyItemRangeInserted(0, chatMessages.size());
            updateDataOnFB(chatMessage.id);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateDataOnFB(String key){
        database.collection(Constants.KEY_COLLECTION_CHAT_GROUPS)
                .document(key)
                .delete()
                .addOnSuccessListener(unused -> showToast("Delete Message Successfully!"))
                .addOnFailureListener(e -> showToast(e.getMessage()));
    }


    private Dialog openDialog(int layout) {
        final Dialog dialog = new Dialog(getApplicationContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(layout);
        dialog.setCancelable(true);
        Window window = dialog.getWindow();
        if (window == null) {
            return null;
        }
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        WindowManager.LayoutParams windowAttributes = window.getAttributes();
        windowAttributes.gravity = Gravity.BOTTOM;
        window.setAttributes(windowAttributes);

        return dialog;
    }


    @Override
    public void finishActivity(int requestCode) {
        super.finishActivity(requestCode);
        deleteData();
    }
}