package com.example.evently.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.navigation.Navigation;

import com.example.evently.R;
import com.example.evently.data.event.EventRepositoryImpl;
import com.example.evently.data.registration.RegistrationRepositoryImpl;
import com.example.evently.data.favorites.FavoriteRepositoryImpl;
import com.example.evently.data.AppDependencies;
import com.example.evently.databinding.FragmentMyEventsBinding;
import com.example.evently.domain.event.EventRepository;
import com.example.evently.domain.registration.RegistrationRepository;
import com.example.evently.ui.activities.EventDetailsActivity;
import com.example.evently.ui.adapters.EventAdapter;
import com.example.evently.ui.adapters.SkeletonAdapter;
import com.example.evently.viewmodel.MyEventsViewModel;
import com.example.evently.viewmodel.MyEventsViewModelFactory;
import com.example.evently.viewmodel.SessionViewModel;
import com.example.evently.viewmodel.SessionViewModelFactory;
import com.example.evently.viewmodel.FavoriteActionViewModel;
import com.example.evently.viewmodel.FavoriteActionViewModelFactory;
import com.example.evently.util.PendingActionManager;
import com.example.evently.util.AuthGate;
import com.example.evently.ui.auth.LoginActivity;
import com.example.evently.data.model.User;

public class MyEventsFragment extends Fragment {

    private FragmentMyEventsBinding binding;
    private MyEventsViewModel viewModel;
    private EventAdapter adapter;
    private SessionViewModel sessionViewModel;
    private FavoriteActionViewModel favoriteActionViewModel;
    private SkeletonAdapter skeletonAdapter;
    private boolean sessionRetry;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMyEventsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EventRepository eventRepository = new EventRepositoryImpl();
        RegistrationRepository registrationRepository = new RegistrationRepositoryImpl();
        viewModel = new ViewModelProvider(this,
                new MyEventsViewModelFactory(eventRepository, registrationRepository))
                .get(MyEventsViewModel.class);
        sessionViewModel = new ViewModelProvider(requireActivity(),
                new SessionViewModelFactory(AppDependencies.sessionRepository()))
                .get(SessionViewModel.class);
        favoriteActionViewModel = new ViewModelProvider(this,
                new FavoriteActionViewModelFactory(new FavoriteRepositoryImpl()))
                .get(FavoriteActionViewModel.class);

        setupRecyclerView();
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new EventAdapter(event -> startActivity(EventDetailsActivity.createIntent(requireContext(), event.getId())),
                this::onFavoriteRequested);
        binding.recyclerViewMyEvents.setAdapter(adapter);
        skeletonAdapter = new SkeletonAdapter();
        binding.recyclerViewMyEvents.setLayoutManager(new LinearLayoutManager(requireContext()));

        binding.swipeRefreshMyEvents.setOnRefreshListener(() -> {
            com.example.evently.domain.session.SessionState state = sessionViewModel.getState().getValue();
            if (state != null && state.isAuthenticated()) viewModel.refresh(state.getUid());
            else binding.swipeRefreshMyEvents.setRefreshing(false);
        });
        binding.btnCreateEvent.setOnClickListener(v ->
                startActivity(new android.content.Intent(requireContext(), com.example.evently.ui.activities.EventManagerActivity.class)));
        favoriteActionViewModel.getResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.getErrorCode() == null) adapter.setFavorite(result.getEventId(), result.isFavorite());
            else Toast.makeText(requireContext(), R.string.favorite_error, Toast.LENGTH_SHORT).show();
        });
    }

    private void observeViewModel() {
        viewModel.getState().observe(getViewLifecycleOwner(), this::renderListState);
        favoriteActionViewModel.getFavoriteIds().observe(getViewLifecycleOwner(), adapter::setFavoriteIds);
        favoriteActionViewModel.getStreamError().observe(getViewLifecycleOwner(), failed->{if(Boolean.TRUE.equals(failed))viewModel.showAuxiliaryError("FAVORITE_LOAD_ERROR");});
        sessionViewModel.getState().observe(getViewLifecycleOwner(), state -> {
            if (state != null && state.isAuthenticated()) {
                sessionRetry = false;
                viewModel.startListening(state.getUid());
                favoriteActionViewModel.observeForUser(state.getUid());
                User user = state.getUser();
                boolean organizer = user != null && user.isVerified() && User.ROLE_ORGANIZER.equals(user.getRole());
                binding.btnCreateEvent.setVisibility(organizer ? View.VISIBLE : View.GONE);
            } else if (state != null && state.getStatus() == com.example.evently.domain.session.SessionState.Status.GUEST) {
                sessionRetry=false; viewModel.startListening(""); favoriteActionViewModel.stopObserving(); binding.btnCreateEvent.setVisibility(View.GONE);
            } else if(state!=null&&state.getStatus()==com.example.evently.domain.session.SessionState.Status.PROFILE_RECOVERY_FAILED){
                sessionRetry=true; viewModel.showSessionError(state.getErrorCode()); favoriteActionViewModel.stopObserving(); binding.btnCreateEvent.setVisibility(View.GONE);
            } else { sessionRetry=true; viewModel.showSessionLoading(); favoriteActionViewModel.stopObserving(); binding.btnCreateEvent.setVisibility(View.GONE); }
        });
    }

    private void renderListState(com.example.evently.viewmodel.ListScreenState state) {
        if(state==null)return; binding.swipeRefreshMyEvents.setRefreshing(false); binding.progressBarMyEvents.setVisibility(View.GONE);
        binding.emptyStateMyEvents.hide(); adapter.submitList(state.getEvents());
        if(state.getStatus()!=com.example.evently.viewmodel.ListScreenState.Status.LOADING)binding.recyclerViewMyEvents.setAdapter(adapter);
        binding.recyclerViewMyEvents.setVisibility(state.getStatus()==com.example.evently.viewmodel.ListScreenState.Status.CONTENT?View.VISIBLE:View.GONE);
        if(state.getStatus()==com.example.evently.viewmodel.ListScreenState.Status.LOADING){binding.recyclerViewMyEvents.setAdapter(skeletonAdapter);binding.recyclerViewMyEvents.setVisibility(View.VISIBLE);}
        else if(state.getStatus()==com.example.evently.viewmodel.ListScreenState.Status.GUEST) binding.emptyStateMyEvents.show(R.drawable.mascot_happy,getString(R.string.my_events_guest_title),getString(R.string.my_events_guest_body),R.string.my_events_guest_cta,v->navigateToLogin());
        else if(state.getStatus()==com.example.evently.viewmodel.ListScreenState.Status.EMPTY) binding.emptyStateMyEvents.show(R.drawable.mascot_happy,getString(R.string.state_empty_my_events_title),getString(R.string.state_empty_my_events_body),R.string.state_empty_my_events_cta,v->navigateToHome());
        else if(state.getStatus()==com.example.evently.viewmodel.ListScreenState.Status.ERROR) binding.emptyStateMyEvents.show(R.drawable.mascot_error,getString(R.string.d1_load_error_title),getString(R.string.d1_load_error_body),R.string.action_retry,v->retry());
    }
    private void retry(){if(sessionRetry){sessionViewModel.retry();return;}com.example.evently.domain.session.SessionState s=sessionViewModel.getState().getValue();if(s!=null&&s.isAuthenticated()){viewModel.refresh(s.getUid());favoriteActionViewModel.observeForUser(s.getUid());}}

    private void navigateToHome() {
        Navigation.findNavController(requireView()).navigate(R.id.nav_home);
    }

    private void onFavoriteRequested(com.example.evently.data.model.Event event) {
        com.example.evently.domain.session.SessionState state = sessionViewModel.getState().getValue();
        if (state == null || !state.isAuthenticated()) {
            AuthGate.requireSignIn(requireActivity(), PendingActionManager.ActionType.FAVORITE_TOGGLE, event.getId());
            return;
        }
        favoriteActionViewModel.toggle(event.getId());
    }

    private void navigateToLogin() {
        startActivity(new android.content.Intent(requireContext(), LoginActivity.class));
    }

    @Override
    public void onStart() {
        super.onStart();
        com.example.evently.domain.session.SessionState state = sessionViewModel.getState().getValue();
        if (state != null && state.isAuthenticated()) { viewModel.startListening(state.getUid()); favoriteActionViewModel.observeForUser(state.getUid()); }
    }

    @Override
    public void onStop() {
        viewModel.stopListening();
        favoriteActionViewModel.stopObserving();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        viewModel.stopListening(); favoriteActionViewModel.stopObserving();
        super.onDestroyView();
        binding = null;
    }
}
