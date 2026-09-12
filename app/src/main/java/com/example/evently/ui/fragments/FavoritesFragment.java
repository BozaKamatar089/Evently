package com.example.evently.ui.fragments;

import android.content.Intent;
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
import com.example.evently.data.favorites.FavoriteRepositoryImpl;
import com.example.evently.data.AppDependencies;
import com.example.evently.databinding.FragmentFavoritesBinding;
import com.example.evently.domain.event.EventRepository;
import com.example.evently.domain.favorites.FavoriteRepository;
import com.example.evently.ui.activities.EventDetailsActivity;
import com.example.evently.ui.adapters.EventAdapter;
import com.example.evently.ui.adapters.SkeletonAdapter;
import com.example.evently.viewmodel.FavoritesViewModel;
import com.example.evently.viewmodel.FavoritesViewModelFactory;
import com.example.evently.viewmodel.SessionViewModel;
import com.example.evently.viewmodel.SessionViewModelFactory;
import com.example.evently.viewmodel.FavoriteActionViewModel;
import com.example.evently.viewmodel.FavoriteActionViewModelFactory;

public class FavoritesFragment extends Fragment {

    private FragmentFavoritesBinding binding;
    private FavoritesViewModel viewModel;
    private EventAdapter adapter;
    private SessionViewModel sessionViewModel;
    private FavoriteActionViewModel favoriteActionViewModel;
    private SkeletonAdapter skeletonAdapter;
    private boolean sessionRetry;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFavoritesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EventRepository eventRepository = new EventRepositoryImpl();
        FavoriteRepository favoriteRepository = new FavoriteRepositoryImpl();
        viewModel = new ViewModelProvider(this,
                new FavoritesViewModelFactory(eventRepository, favoriteRepository))
                .get(FavoritesViewModel.class);
        sessionViewModel = new ViewModelProvider(requireActivity(),
                new SessionViewModelFactory(AppDependencies.sessionRepository()))
                .get(SessionViewModel.class);
        favoriteActionViewModel = new ViewModelProvider(this,
                new FavoriteActionViewModelFactory(favoriteRepository))
                .get(FavoriteActionViewModel.class);

        setupRecyclerView();
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new EventAdapter(event -> startActivity(EventDetailsActivity.createIntent(requireContext(), event.getId())),
                this::onFavoriteRequested);
        binding.recyclerViewFavorites.setAdapter(adapter);
        skeletonAdapter = new SkeletonAdapter();
        binding.recyclerViewFavorites.setLayoutManager(new LinearLayoutManager(requireContext()));

        binding.swipeRefreshFavorites.setOnRefreshListener(() -> {
            com.example.evently.domain.session.SessionState state = sessionViewModel.getState().getValue();
            if (state != null && state.isAuthenticated()) viewModel.refresh(state.getUid());
            else binding.swipeRefreshFavorites.setRefreshing(false);
        });
    }

    private void observeViewModel() {
        viewModel.getState().observe(getViewLifecycleOwner(), this::renderListState);
        favoriteActionViewModel.getResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.getErrorCode() == null) adapter.setFavorite(result.getEventId(), result.isFavorite());
            else Toast.makeText(requireContext(), R.string.favorite_error, Toast.LENGTH_SHORT).show();
        });
        favoriteActionViewModel.getFavoriteIds().observe(getViewLifecycleOwner(), adapter::setFavoriteIds);
        favoriteActionViewModel.getStreamError().observe(getViewLifecycleOwner(), failed -> { if(Boolean.TRUE.equals(failed)) viewModel.showAuxiliaryError("FAVORITE_LOAD_ERROR"); });
        sessionViewModel.getState().observe(getViewLifecycleOwner(), state -> {
            if (state != null && state.isAuthenticated()) {
                sessionRetry = false;
                viewModel.startListening(state.getUid()); favoriteActionViewModel.observeForUser(state.getUid());
            } else if (state != null && state.getStatus() == com.example.evently.domain.session.SessionState.Status.GUEST) {
                sessionRetry = false; viewModel.startListening(""); favoriteActionViewModel.stopObserving();
            } else if (state != null && state.getStatus() == com.example.evently.domain.session.SessionState.Status.PROFILE_RECOVERY_FAILED) {
                sessionRetry = true; favoriteActionViewModel.stopObserving(); viewModel.showSessionError(state.getErrorCode());
            } else { sessionRetry = true; favoriteActionViewModel.stopObserving(); viewModel.showSessionLoading(); }
        });
    }

    private void renderListState(com.example.evently.viewmodel.ListScreenState state) {
        if (state == null) return;
        binding.swipeRefreshFavorites.setRefreshing(false); binding.progressBarFavorites.setVisibility(View.GONE);
        binding.emptyStateFavorites.hide(); adapter.submitList(state.getEvents());
        if (state.getStatus() != com.example.evently.viewmodel.ListScreenState.Status.LOADING)
            binding.recyclerViewFavorites.setAdapter(adapter);
        binding.recyclerViewFavorites.setVisibility(state.getStatus() == com.example.evently.viewmodel.ListScreenState.Status.CONTENT ? View.VISIBLE : View.GONE);
        if (state.getStatus() == com.example.evently.viewmodel.ListScreenState.Status.LOADING) { binding.recyclerViewFavorites.setAdapter(skeletonAdapter); binding.recyclerViewFavorites.setVisibility(View.VISIBLE); }
        else if (state.getStatus() == com.example.evently.viewmodel.ListScreenState.Status.GUEST)
            binding.emptyStateFavorites.show(R.drawable.mascot_happy, getString(R.string.favorites_guest_title), getString(R.string.favorites_guest_body), R.string.favorites_guest_cta, v -> navigateToLogin());
        else if (state.getStatus() == com.example.evently.viewmodel.ListScreenState.Status.EMPTY)
            binding.emptyStateFavorites.show(R.drawable.mascot_happy, getString(R.string.state_empty_favorites_title), getString(R.string.state_empty_favorites_body), R.string.state_empty_favorites_cta, v -> navigateToHome());
        else if (state.getStatus() == com.example.evently.viewmodel.ListScreenState.Status.ERROR)
            binding.emptyStateFavorites.show(R.drawable.mascot_error, getString(R.string.d1_load_error_title), getString(R.string.d1_load_error_body), R.string.action_retry, v -> retry());
    }
    private void retry() { if(sessionRetry){sessionViewModel.retry();return;} com.example.evently.domain.session.SessionState s=sessionViewModel.getState().getValue(); if(s!=null&&s.isAuthenticated()){ viewModel.refresh(s.getUid()); favoriteActionViewModel.observeForUser(s.getUid());} }

    private void navigateToHome() {
        Navigation.findNavController(requireView()).navigate(R.id.nav_home);
    }

    private void onFavoriteRequested(com.example.evently.data.model.Event event) {
        com.example.evently.domain.session.SessionState state = sessionViewModel.getState().getValue();
        if (state != null && state.isAuthenticated()) favoriteActionViewModel.toggle(event.getId());
    }

    private void navigateToLogin() {
        startActivity(new Intent(requireContext(), com.example.evently.ui.auth.LoginActivity.class));
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
