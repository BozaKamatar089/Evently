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

import com.example.evently.R;
import com.example.evently.data.event.EventRepositoryImpl;
import com.example.evently.data.favorites.FavoriteRepositoryImpl;
import com.example.evently.data.AppDependencies;
import com.example.evently.databinding.FragmentHomeBinding;
import com.example.evently.domain.event.EventRepository;
import com.example.evently.ui.adapters.EventAdapter;
import com.example.evently.ui.adapters.SkeletonAdapter;
import com.example.evently.ui.activities.EventDetailsActivity;
import com.example.evently.util.ErrorMapper;
import com.example.evently.viewmodel.HomeViewModel;
import com.example.evently.viewmodel.HomeViewModelFactory;
import com.example.evently.viewmodel.FavoriteActionViewModel;
import com.example.evently.viewmodel.FavoriteActionViewModelFactory;
import com.example.evently.viewmodel.SessionViewModel;
import com.example.evently.viewmodel.SessionViewModelFactory;
import com.example.evently.util.PendingActionManager;
import com.example.evently.util.AuthGate;
import com.example.evently.ui.auth.LoginActivity;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private EventAdapter adapter;
    private SkeletonAdapter skeletonAdapter;
    private FavoriteActionViewModel favoriteActionViewModel;
    private SessionViewModel sessionViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EventRepository eventRepository = new EventRepositoryImpl();
        viewModel = new ViewModelProvider(this,
                new HomeViewModelFactory(eventRepository))
                .get(HomeViewModel.class);
        favoriteActionViewModel = new ViewModelProvider(this,
                new FavoriteActionViewModelFactory(new FavoriteRepositoryImpl()))
                .get(FavoriteActionViewModel.class);
        sessionViewModel = new ViewModelProvider(requireActivity(),
                new SessionViewModelFactory(AppDependencies.sessionRepository()))
                .get(SessionViewModel.class);

        setupRecyclerView();
        setupSearch();
        observeViewModel();
        sessionViewModel.getState().observe(getViewLifecycleOwner(), this::renderSession);
    }

    private void setupRecyclerView() {
        adapter = new EventAdapter(event -> startActivity(EventDetailsActivity.createIntent(requireContext(), event.getId())),
                this::onFavoriteRequested);
        skeletonAdapter = new SkeletonAdapter();
        binding.recyclerViewEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewEvents.setAdapter(skeletonAdapter);

        binding.swipeRefreshHome.setOnRefreshListener(() -> {
            retryList();
        });
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setSearchQuery(s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void observeViewModel() {
        viewModel.getState().observe(getViewLifecycleOwner(), this::renderListState);
        favoriteActionViewModel.getFavoriteIds().observe(getViewLifecycleOwner(), adapter::setFavoriteIds);
        favoriteActionViewModel.getStreamError().observe(getViewLifecycleOwner(), failed -> { if(Boolean.TRUE.equals(failed)) viewModel.showAuxiliaryError("FAVORITE_LOAD_ERROR"); });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(),
                        ErrorMapper.toResId(error), Toast.LENGTH_SHORT).show();
            }
        });
        favoriteActionViewModel.getResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.getErrorCode() == null) {
                adapter.setFavorite(result.getEventId(), result.isFavorite());
                Toast.makeText(requireContext(), result.isFavorite() ? R.string.favorite_added : R.string.favorite_removed, Toast.LENGTH_SHORT).show();
            } else Toast.makeText(requireContext(), R.string.favorite_error, Toast.LENGTH_SHORT).show();
        });
    }

    private void renderSession(com.example.evently.domain.session.SessionState state) {
        if (state != null && state.isAuthenticated() && state.getUser() != null) {
            String name = state.getUser().getName();
            binding.tvHomeTitle.setText(R.string.home_hello);
            binding.tvHomeSubtitle.setText(name != null && !name.isEmpty()
                    ? getString(R.string.home_hello_name, name) : getString(R.string.home_user_subtitle));
            binding.btnHomeSignIn.setVisibility(View.GONE);
            favoriteActionViewModel.observeForUser(state.getUid());
        } else if (state != null && state.getStatus() == com.example.evently.domain.session.SessionState.Status.GUEST) {
            binding.tvHomeTitle.setText(R.string.home_hello);
            binding.tvHomeSubtitle.setText(R.string.home_guest_subtitle);
            binding.btnHomeSignIn.setText(R.string.auth_login);
            binding.btnHomeSignIn.setVisibility(View.VISIBLE);
            binding.btnHomeSignIn.setOnClickListener(v ->
                    startActivity(new android.content.Intent(requireContext(), LoginActivity.class)));
            favoriteActionViewModel.stopObserving();
        } else if (state != null && state.getStatus() == com.example.evently.domain.session.SessionState.Status.PROFILE_RECOVERY_FAILED) {
            binding.tvHomeTitle.setText(R.string.home_hello);
            binding.tvHomeSubtitle.setText(R.string.d1_session_error);
            binding.btnHomeSignIn.setText(R.string.action_retry);
            binding.btnHomeSignIn.setVisibility(View.VISIBLE);
            binding.btnHomeSignIn.setOnClickListener(v -> sessionViewModel.retry());
            favoriteActionViewModel.stopObserving();
        } else {
            binding.tvHomeTitle.setText(R.string.home_hello);
            binding.tvHomeSubtitle.setText(R.string.d1_session_loading);
            binding.btnHomeSignIn.setVisibility(View.GONE);
            favoriteActionViewModel.stopObserving();
        }
    }

    private void renderListState(com.example.evently.viewmodel.ListScreenState state) {
        if (state == null) return;
        boolean loading = state.getStatus() == com.example.evently.viewmodel.ListScreenState.Status.LOADING;
        binding.swipeRefreshHome.setRefreshing(false);
        binding.progressBarHome.setVisibility(View.GONE);
        binding.emptyStateHome.hide();
        if (loading) {
            binding.recyclerViewEvents.setAdapter(skeletonAdapter);
            binding.recyclerViewEvents.setVisibility(View.VISIBLE);
        } else if (state.getStatus() == com.example.evently.viewmodel.ListScreenState.Status.ERROR) {
            binding.recyclerViewEvents.setVisibility(View.GONE);
            binding.emptyStateHome.show(R.drawable.mascot_error, getString(R.string.d1_load_error_title),
                    getString(R.string.d1_load_error_body), R.string.action_retry, v -> retryList());
        } else {
            binding.recyclerViewEvents.setAdapter(adapter);
            adapter.submitList(state.getEvents());
            binding.recyclerViewEvents.setVisibility(state.getStatus() == com.example.evently.viewmodel.ListScreenState.Status.CONTENT ? View.VISIBLE : View.GONE);
            if (state.getStatus() == com.example.evently.viewmodel.ListScreenState.Status.EMPTY) {
                boolean searching = binding.etSearch.getText() != null
                        && !binding.etSearch.getText().toString().trim().isEmpty();
                binding.emptyStateHome.show(R.drawable.mascot_search,
                        getString(searching ? R.string.state_empty_search_title : R.string.state_empty_events_title),
                        getString(searching ? R.string.state_empty_search_body : R.string.state_empty_events_body));
            }
        }
    }
    private void retryList(){viewModel.refresh();com.example.evently.domain.session.SessionState s=sessionViewModel.getState().getValue();if(s!=null&&s.isAuthenticated())favoriteActionViewModel.observeForUser(s.getUid());}

    private void onFavoriteRequested(com.example.evently.data.model.Event event) {
        com.example.evently.domain.session.SessionState state = sessionViewModel.getState().getValue();
        if (state == null || state.getStatus() == com.example.evently.domain.session.SessionState.Status.LOADING_PROFILE) return;
        if (state.getStatus() == com.example.evently.domain.session.SessionState.Status.PROFILE_RECOVERY_FAILED) {
            sessionViewModel.retry(); return;
        }
        if (!state.isAuthenticated()) {
            AuthGate.requireSignIn(requireActivity(), PendingActionManager.ActionType.FAVORITE_TOGGLE, event.getId());
            return;
        }
        favoriteActionViewModel.toggle(event.getId());
    }

    @Override
    public void onStart() {
        super.onStart();
        viewModel.startListening();
        com.example.evently.domain.session.SessionState s=sessionViewModel.getState().getValue(); if(s!=null&&s.isAuthenticated())favoriteActionViewModel.observeForUser(s.getUid());
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
