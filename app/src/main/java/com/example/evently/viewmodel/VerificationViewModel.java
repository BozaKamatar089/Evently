package com.example.evently.viewmodel;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.evently.data.image.ImageUploader;
import com.example.evently.data.image.ImageUploadException;
import com.example.evently.data.model.VerificationRequest;
import com.example.evently.domain.verification.VerificationRepository;
import com.google.firebase.firestore.ListenerRegistration;

/** Owns the request listener and operations across configuration changes. */
public final class VerificationViewModel extends ViewModel {
    private final VerificationRepository repository;
    private final ImageUploader images;
    private final java.util.concurrent.Executor callbacks;
    private final MutableLiveData<AccountScreenState> state = new MutableLiveData<>(AccountScreenState.loading());
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> proof = new MutableLiveData<>();
    private final MutableLiveData<Uri> selectedProof = new MutableLiveData<>();
    private final MutableLiveData<Integer> uploadProgress = new MutableLiveData<>(0);
    private VerificationRequest request;
    private ListenerRegistration listener;
    private String uid, uploadId;
    private long generation, loadGeneration;
    private boolean resubmitting;
    public VerificationViewModel(VerificationRepository repository, ImageUploader images) {
        this(repository, images, com.google.android.gms.tasks.TaskExecutors.MAIN_THREAD);
    }
    public VerificationViewModel(VerificationRepository repository, ImageUploader images, java.util.concurrent.Executor callbacks) {
        this.repository=repository;this.images=images;this.callbacks=callbacks;
    }
    public LiveData<AccountScreenState> getState(){return state;}
    public LiveData<Boolean> getBusy(){return busy;}
    public LiveData<String> getError(){return error;}
    public LiveData<String> getProof(){return proof;}
    public LiveData<Uri> getSelectedProof(){return selectedProof;}
    public LiveData<Integer> getUploadProgress(){return uploadProgress;}
    public VerificationRequest getRequest(){return request;}
    public boolean isBusy(){return Boolean.TRUE.equals(busy.getValue());}
    public boolean showingForm(){return request==null || resubmitting;}
    public String getUid(){return uid;}
    public void bind(String nextUid) {
        if (nextUid.equals(uid) && listener!=null) return;
        if (!nextUid.equals(uid)) { clear(); uid=nextUid; }
        retry();
    }
    public void retry() {
        if(uid==null || isBusy())return;
        if(listener!=null)listener.remove();
        long token=++loadGeneration;
        state.setValue(AccountScreenState.loading()); error.setValue(null);
        listener=repository.listenMyRequest((value,failure)->{
            if(token!=loadGeneration)return;
            if(failure!=null){state.setValue(AccountScreenState.error("VERIFICATION_LOAD_ERROR"));return;}
            request=value;
            if(value!=null && !VerificationRequest.STATUS_REJECTED.equals(value.getStatus())) resubmitting=false;
            if(value!=null && !VerificationRequest.STATUS_PENDING.equals(value.getStatus())
                    && !VerificationRequest.STATUS_APPROVED.equals(value.getStatus())
                    && !VerificationRequest.STATUS_REJECTED.equals(value.getStatus())) {
                state.setValue(AccountScreenState.error("VERIFICATION_LOAD_ERROR")); return;
            }
            state.setValue(value==null?AccountScreenState.empty():AccountScreenState.content());
        });
    }
    public void resubmit(){
        if(isBusy() || request==null || !VerificationRequest.STATUS_REJECTED.equals(request.getStatus()))return;
        resubmitting=true; proof.setValue(null); selectedProof.setValue(null); uploadProgress.setValue(0);
        error.setValue(null); state.setValue(AccountScreenState.content());
    }
    public void upload(Uri uri){
        if(uri==null || uid==null || isBusy() || !showingForm())return;
        busy.setValue(true); proof.setValue(null); selectedProof.setValue(uri); uploadProgress.setValue(0); error.setValue(null);
        final long token=generation;
        uploadId=images.uploadImage(uri,new ImageUploader.OnUploadCallback(){
            public void onSuccess(String url){callbacks.execute(()->{if(token!=generation)return;uploadId=null;proof.setValue(url);uploadProgress.setValue(100);busy.setValue(false);});}
            public void onError(Exception e){callbacks.execute(()->{if(token!=generation)return;uploadId=null;
                error.setValue(uploadErrorCode(e));busy.setValue(false);});}
            public void onUploadProgress(int percent){callbacks.execute(()->{if(token==generation)uploadProgress.setValue(Math.max(0,Math.min(100,percent)));});}
        });
        if(uploadId==null && error.getValue()==null){busy.setValue(false);error.setValue("UPLOAD_ERROR");}
    }
    public void submit(String organization){
        if(uid==null || isBusy() || !showingForm() || organization.trim().isEmpty()
                || proof.getValue()==null || proof.getValue().isEmpty())return;
        busy.setValue(true);error.setValue(null); final long token=generation;
        repository.submitRequest(organization.trim(),proof.getValue()).addOnCompleteListener(callbacks, task->{
            if(token!=generation)return;
            busy.setValue(false);
            // The active listenMyRequest listener already delivers the created request;
            // re-subscribing here would duplicate that read.
            if(task.isSuccessful()){resubmitting=false;}
            else error.setValue("VERIFICATION_ERROR");
        });
    }
    public void cancel(){
        if(uid==null || isBusy() || request==null || !VerificationRequest.STATUS_PENDING.equals(request.getStatus()))return;
        busy.setValue(true);error.setValue(null);final long token=generation;
        repository.cancelRequest(uid).addOnCompleteListener(callbacks, task->{
            if(token!=generation)return;
            busy.setValue(false);
            // The active listenMyRequest listener already delivers the deletion (request=null
            // -> empty state); re-subscribing here would duplicate that read.
            if(task.isSuccessful()){proof.setValue(null);resubmitting=false;}
            else error.setValue("VERIFICATION_ERROR");
        });
    }
    public void clear(){
        generation++;loadGeneration++;
        if(listener!=null)listener.remove();listener=null;
        if(uploadId!=null)images.cancelUpload(uploadId);uploadId=null;
        uid=null;request=null;resubmitting=false;proof.setValue(null);selectedProof.setValue(null);
        uploadProgress.setValue(0);busy.setValue(false);error.setValue(null);
        state.setValue(AccountScreenState.loading());
    }
    @Override protected void onCleared(){clear();super.onCleared();}
    public static String uploadErrorCode(Exception error) {
        return error instanceof ImageUploadException
                && ((ImageUploadException) error).getKind() == ImageUploadException.Kind.CONFIGURATION_MISSING
                ? "UPLOAD_CONFIGURATION_MISSING" : "UPLOAD_ERROR";
    }
}
