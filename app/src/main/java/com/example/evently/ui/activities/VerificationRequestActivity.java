package com.example.evently.ui.activities;

import android.os.Bundle;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.example.evently.R;
import com.example.evently.data.AppDependencies;
import com.example.evently.data.image.CloudinaryImageUploader;
import com.example.evently.data.model.VerificationRequest;
import com.example.evently.data.verification.VerificationRepositoryImpl;
import com.example.evently.databinding.ActivityVerificationRequestBinding;
import com.example.evently.domain.session.SessionState;
import com.example.evently.viewmodel.AccountScreenState;
import com.example.evently.viewmodel.SessionViewModel;
import com.example.evently.viewmodel.SessionViewModelFactory;
import com.example.evently.viewmodel.VerificationViewModel;

public class VerificationRequestActivity extends AppCompatActivity {
    private ActivityVerificationRequestBinding binding;
    private SessionViewModel session;
    private VerificationViewModel model;
    private boolean ready;
    private final ActivityResultLauncher<String> pickProof = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> { if(ready && uri!=null) model.upload(uri); });

    @Override protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        binding=ActivityVerificationRequestBinding.inflate(getLayoutInflater());setContentView(binding.getRoot());
        model=new ViewModelProvider(this,new ViewModelProvider.Factory(){
            @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> type){
                return type.cast(new VerificationViewModel(new VerificationRepositoryImpl(),new CloudinaryImageUploader()));
            }
        }).get(VerificationViewModel.class);
        session=new ViewModelProvider(this,new SessionViewModelFactory(AppDependencies.sessionRepository())).get(SessionViewModel.class);
        binding.toolbar.setNavigationOnClickListener(v->finish());
        binding.btnSelectProof.setOnClickListener(v->pickProof.launch("image/*"));
        binding.btnSubmitRequest.setOnClickListener(v->{
            String name=binding.etOrgName.getText()==null?"":binding.etOrgName.getText().toString().trim();
            binding.tilOrgName.setError(name.isEmpty()?getString(R.string.auth_error_empty_fields):null);
            if(ready && !name.isEmpty())model.submit(name);
        });
        binding.btnCancelRequest.setOnClickListener(v->{if(ready)model.cancel();});
        binding.btnRequestAgain.setOnClickListener(v->model.resubmit());
        model.getState().observe(this,ignored->render());
        model.getBusy().observe(this,ignored->render());
        model.getError().observe(this,ignored->render());
        model.getProof().observe(this,url->{
            if(url!=null)Glide.with(this).load(url).error(R.drawable.ic_event).into(binding.ivProofPreview);
            else if(model.getSelectedProof().getValue()==null)Glide.with(this).clear(binding.ivProofPreview);
            render();
        });
        model.getSelectedProof().observe(this,uri->{
            if(uri!=null && model.getProof().getValue()==null)
                Glide.with(this).load(uri).error(R.drawable.ic_event).into(binding.ivProofPreview);
            render();
        });
        model.getUploadProgress().observe(this,ignored->render());
        session.getState().observe(this,value->{
            ready=value!=null && value.isAuthenticated();
            if(ready){
                if(model.getUid()!=null && !model.getUid().equals(value.getUid())){model.clear();binding.etOrgName.setText("");finish();return;}
                model.bind(value.getUid());
            } else if(value!=null && value.isGuest()){model.clear();finish();return;}
            render();
        });
        session.start();
    }
    private void render(){
        if(binding==null || session==null)return;
        hideContent(); binding.stateVerification.hide(); binding.progressBar.setVisibility(View.GONE);
        SessionState current=session.getState().getValue();
        if(!ready){
            if(current!=null && current.getStatus()==SessionState.Status.PROFILE_RECOVERY_FAILED)showError(true);
            else binding.progressBar.setVisibility(View.VISIBLE);
            return;
        }
        AccountScreenState state=model.getState().getValue();
        if(state==null || state.getStatus()==AccountScreenState.Status.LOADING){binding.progressBar.setVisibility(View.VISIBLE);return;}
        if(state.getStatus()==AccountScreenState.Status.ERROR){showError(false);return;}
        VerificationRequest request=model.getRequest();
        boolean form=model.showingForm();
        if(form)binding.cardRequestForm.setVisibility(View.VISIBLE);
        else if(VerificationRequest.STATUS_PENDING.equals(request.getStatus()))binding.cardStatusPending.setVisibility(View.VISIBLE);
        else if(VerificationRequest.STATUS_APPROVED.equals(request.getStatus()))binding.cardStatusApproved.setVisibility(View.VISIBLE);
        else if(VerificationRequest.STATUS_REJECTED.equals(request.getStatus())){
            binding.cardStatusRejected.setVisibility(View.VISIBLE);binding.btnRequestAgain.setVisibility(View.VISIBLE);
            String reason=request.getRejectionReason();
            binding.tvRejectionReason.setText(reason==null||reason.isEmpty()?getString(R.string.verification_rejected_body):reason);
        }
        boolean busy=model.isBusy(); boolean proof=model.getProof().getValue()!=null;
        boolean selected=model.getSelectedProof().getValue()!=null;
        binding.documentSurface.setActivated(selected);
        int uploadProgress=model.getUploadProgress().getValue()==null?0:model.getUploadProgress().getValue();
        binding.progressBar.setVisibility(busy?View.VISIBLE:View.GONE);
        binding.btnSubmitRequest.setEnabled(form && !busy && proof);
        binding.btnSelectProof.setEnabled(!busy);binding.btnCancelRequest.setEnabled(!busy);binding.btnRequestAgain.setEnabled(!busy);
        binding.ivProofPreview.setVisibility(form&&selected?View.VISIBLE:View.GONE);
        binding.tvUploadPrompt.setVisibility(!selected?View.VISIBLE:View.GONE);
        binding.tvUploadStatus.setVisibility(form&&selected?View.VISIBLE:View.GONE);
        binding.progressUpload.setVisibility(form&&selected&&busy&&!proof?View.VISIBLE:View.GONE);
        binding.progressUpload.setProgressCompat(uploadProgress,true);
        if(selected&&busy&&!proof)binding.tvUploadStatus.setText(getString(R.string.verification_uploading,uploadProgress));
        else if(proof)binding.tvUploadStatus.setText(R.string.verification_uploaded);
        else if(selected)binding.tvUploadStatus.setText(R.string.verification_document_selected);
        binding.btnSelectProof.setText(proof?R.string.verification_change_proof:R.string.verification_select_proof);
        binding.tvVerificationActionError.setVisibility(model.getError().getValue()!=null?View.VISIBLE:View.GONE);
        String error=model.getError().getValue();
        binding.tvVerificationActionError.setText("UPLOAD_CONFIGURATION_MISSING".equals(error)
                ?R.string.verification_upload_config_missing:"UPLOAD_ERROR".equals(error)
                ?R.string.verification_upload_failed:R.string.verification_error);
    }
    private void hideContent(){
        binding.cardRequestForm.setVisibility(View.GONE);binding.cardStatusPending.setVisibility(View.GONE);
        binding.cardStatusApproved.setVisibility(View.GONE);binding.cardStatusRejected.setVisibility(View.GONE);
        binding.btnRequestAgain.setVisibility(View.GONE);binding.tvVerificationActionError.setVisibility(View.GONE);
    }
    private void showError(boolean profile){
        binding.stateVerification.show(0,getString(R.string.d1_verification_load_error_title),
                getString(R.string.d1_verification_load_error_body),R.string.action_retry,v->{if(profile)session.retry();else model.retry();});
    }
}
