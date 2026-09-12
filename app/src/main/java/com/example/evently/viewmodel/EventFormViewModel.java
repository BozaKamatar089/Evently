package com.example.evently.viewmodel;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.evently.data.image.ImageUploader;
import com.example.evently.data.model.Event;
import com.example.evently.domain.event.EventRepository;

/** Retains event form operations so rotation cannot duplicate them or strand callbacks in an Activity. */
public final class EventFormViewModel extends ViewModel {
    public enum Status { IDLE, UPLOADING, SAVING, SAVE_SUCCESS, UPLOAD_ERROR, SAVE_ERROR }
    private final EventRepository events;
    private final ImageUploader images;
    private final java.util.concurrent.Executor callbacks;
    private final MutableLiveData<Status> status = new MutableLiveData<>(Status.IDLE);
    private final MutableLiveData<Integer> uploadProgress = new MutableLiveData<>();
    private final MutableLiveData<String> imageUrl = new MutableLiveData<>("");
    private String uploadId;
    private String ownerUid;
    private Uri selectedImage;
    private long generation;
    private long selectedDateLong;
    private long originalDateLong;
    private Event lastEvent;
    private boolean lastEdit;

    public EventFormViewModel(EventRepository events, ImageUploader images) {
        this(events, images, com.google.android.gms.tasks.TaskExecutors.MAIN_THREAD);
    }
    public EventFormViewModel(EventRepository events, ImageUploader images, java.util.concurrent.Executor callbacks) {
        this.events = events; this.images = images; this.callbacks = callbacks;
    }
    public LiveData<Status> getStatus() { return status; }
    public LiveData<Integer> getUploadProgress() { return uploadProgress; }
    public LiveData<String> getImageUrl() { return imageUrl; }
    public boolean isBusy() { Status s=status.getValue(); return s==Status.UPLOADING || s==Status.SAVING; }
    public long getSelectedDateLong() { return selectedDateLong; }
    public void initializeDate(long value) { if(selectedDateLong==0){selectedDateLong=value; originalDateLong=value;} }
    public long selectDate(long utcDay) {
        selectedDateLong=com.example.evently.util.EventFormPolicy.preserveTimeWhenSameDay(originalDateLong,utcDay);
        return selectedDateLong;
    }
    public void initializeImage(@Nullable String value) {
        if (imageUrl.getValue() == null || imageUrl.getValue().isEmpty()) imageUrl.setValue(value == null ? "" : value);
    }
    public void bindOwner(@NonNull String uid) {
        if (ownerUid == null) ownerUid=uid;
        else if (!ownerUid.equals(uid)) cancelAndClear();
    }
    public void upload(@NonNull Uri uri) {
        if (status.getValue()==Status.SAVING) return;
        selectedImage=uri;
        if (uploadId != null) images.cancelUpload(uploadId);
        status.setValue(Status.UPLOADING);
        long requestGeneration=++generation;
        uploadId=images.uploadImage(uri, new ImageUploader.OnUploadCallback() {
            public void onSuccess(@NonNull String url) {
                callbacks.execute(() -> {
                if(requestGeneration!=generation)return;
                uploadId=null;
                if(url.trim().isEmpty()) status.setValue(Status.UPLOAD_ERROR);
                else { imageUrl.setValue(url); status.setValue(Status.IDLE); }
                });
            }
            public void onError(@NonNull Exception error) { callbacks.execute(() -> { if(requestGeneration!=generation)return; uploadId=null; status.setValue(Status.UPLOAD_ERROR); }); }
            public void onUploadProgress(int percent) { callbacks.execute(() -> { if(requestGeneration==generation) uploadProgress.setValue(percent); }); }
        });
        if (uploadId == null) status.setValue(Status.UPLOAD_ERROR);
    }
    public void save(@NonNull Event event, boolean edit) {
        if (isBusy()) return;
        lastEvent=event; lastEdit=edit;
        status.setValue(Status.SAVING);
        long requestGeneration=++generation;
        if (edit) {
            events.updateEvent(event.getId(), event)
                    .addOnSuccessListener(callbacks, v -> {if(requestGeneration==generation)status.setValue(Status.SAVE_SUCCESS);})
                    .addOnFailureListener(callbacks, e -> {if(requestGeneration==generation)status.setValue(Status.SAVE_ERROR);});
        } else {
            events.createEvent(event)
                    .addOnSuccessListener(callbacks, v -> {if(requestGeneration==generation)status.setValue(Status.SAVE_SUCCESS);})
                    .addOnFailureListener(callbacks, e -> {if(requestGeneration==generation)status.setValue(Status.SAVE_ERROR);});
        }
    }
    public boolean canRetrySave(){ return status.getValue()==Status.SAVE_ERROR && lastEvent!=null; }
    public boolean canRetryUpload(){ return status.getValue()==Status.UPLOAD_ERROR && selectedImage!=null; }
    public void retryUpload(){ if(canRetryUpload()) upload(selectedImage); }
    public Uri getSelectedImage(){ return selectedImage; }
    public void retrySave(){ if(canRetrySave()){ status.setValue(Status.IDLE); save(lastEvent,lastEdit); } }
    public void acknowledgeError() { Status s=status.getValue(); if(s==Status.SAVE_ERROR||s==Status.UPLOAD_ERROR) status.setValue(Status.IDLE); }
    public void cancelAndClear() {
        generation++;
        if(uploadId!=null) images.cancelUpload(uploadId);
        uploadId=null; ownerUid=null; selectedImage=null; imageUrl.setValue(""); selectedDateLong=0; originalDateLong=0;
        lastEvent=null; status.setValue(Status.IDLE);
    }
    @Override protected void onCleared() { generation++; if(uploadId!=null) images.cancelUpload(uploadId); super.onCleared(); }
}
