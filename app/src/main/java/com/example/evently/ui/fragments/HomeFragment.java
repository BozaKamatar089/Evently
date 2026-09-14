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
import com.example.evently.domain.event.EventCategory;
import com.example.evently.domain.event.HomeDiscoveryState;
import com.example.evently.ui.adapters.EventAdapter;
import com.example.evently.ui.adapters.SkeletonAdapter;
import com.example.evently.ui.activities.EventDetailsActivity;
import com.example.evently.ui.views.EventCategoryLabels;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
        setupDiscoveryControls();
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

    private void setupDiscoveryControls() {
        binding.chipCategoryFilter.setOnClickListener(v -> showCategoryDialog());
        binding.chipDateFilter.setOnClickListener(v -> showDateDialog());
        binding.chipSortOrder.setOnClickListener(v -> showSortDialog());
        binding.chipResetDiscovery.setOnClickListener(v -> viewModel.resetDiscovery());
    }

    private void observeViewModel() {
        viewModel.getState().observe(getViewLifecycleOwner(), this::renderListState);
        viewModel.getDiscoveryState().observe(getViewLifecycleOwner(), this::renderDiscoveryControls);
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
                renderEmptyState();
            }
        }
    }

    private void renderEmptyState() {
        HomeViewModel.EmptyReason reason = viewModel.getEmptyReason().getValue();
        if (reason == HomeViewModel.EmptyReason.NO_UPCOMING) {
            binding.emptyStateHome.show(R.drawable.mascot_search,
                    getString(R.string.discovery_empty_upcoming_title),
                    getString(R.string.discovery_empty_upcoming_body),
                    R.string.discovery_show_all,
                    v -> viewModel.setDateFilter(HomeDiscoveryState.DateFilter.ALL));
        } else if (reason == HomeViewModel.EmptyReason.NO_SEARCH_RESULTS) {
            binding.emptyStateHome.show(R.drawable.mascot_search,
                    getString(R.string.state_empty_search_title),
                    getString(R.string.state_empty_search_body),
                    R.string.discovery_reset, v -> viewModel.resetDiscovery());
        } else if (reason == HomeViewModel.EmptyReason.NO_FILTER_RESULTS) {
            binding.emptyStateHome.show(R.drawable.mascot_search,
                    getString(R.string.discovery_empty_filter_title),
                    getString(R.string.discovery_empty_filter_body),
                    R.string.discovery_reset, v -> viewModel.resetDiscovery());
        } else {
            binding.emptyStateHome.show(R.drawable.mascot_search,
                    getString(R.string.state_empty_events_title),
                    getString(R.string.state_empty_events_body));
        }
    }

    private void renderDiscoveryControls(HomeDiscoveryState state) {
        if (state == null) return;
        String currentQuery = binding.etSearch.getText() == null
                ? "" : binding.etSearch.getText().toString();
        if (!currentQuery.equals(state.getQuery())) {
            binding.etSearch.setText(state.getQuery());
            binding.etSearch.setSelection(state.getQuery().length());
        }

        EventCategory category = state.getCategory();
        binding.chipCategoryFilter.setText(category == null
                ? getString(R.string.discovery_category_all)
                : getString(R.string.discovery_category_value,
                EventCategoryLabels.label(requireContext(), category)));
        binding.chipCategoryFilter.setChecked(category != null);

        binding.chipDateFilter.setText(dateChipText(state.getDateFilter()));
        binding.chipDateFilter.setChecked(state.getDateFilter() != HomeDiscoveryState.DateFilter.UPCOMING);
        binding.chipSortOrder.setText(state.getSortOrder() == HomeDiscoveryState.SortOrder.SOONEST
                ? getString(R.string.discovery_sort_soonest)
                : getString(R.string.discovery_sort_value, getString(R.string.discovery_sort_newest)));
        binding.chipSortOrder.setChecked(state.getSortOrder() != HomeDiscoveryState.SortOrder.SOONEST);
        binding.chipResetDiscovery.setVisibility(state.isDefault() ? View.GONE : View.VISIBLE);
        binding.tvHomeSection.setText(sectionText(state.getDateFilter()));
    }

    private String dateChipText(HomeDiscoveryState.DateFilter filter) {
        if (filter == HomeDiscoveryState.DateFilter.UPCOMING) {
            return getString(R.string.discovery_date_upcoming);
        }
        int label = filter == HomeDiscoveryState.DateFilter.TODAY
                ? R.string.discovery_date_today
                : filter == HomeDiscoveryState.DateFilter.THIS_WEEK
                ? R.string.discovery_date_this_week : R.string.discovery_date_all;
        return getString(R.string.discovery_date_value, getString(label));
    }

    private int sectionText(HomeDiscoveryState.DateFilter filter) {
        switch (filter) {
            case TODAY: return R.string.discovery_section_today;
            case THIS_WEEK: return R.string.discovery_section_this_week;
            case ALL: return R.string.discovery_section_all;
            default: return R.string.discovery_section_upcoming;
        }
    }

    private void showCategoryDialog() {
        HomeDiscoveryState state = viewModel.getDiscoveryState().getValue();
        EventCategory selected = state == null ? null : state.getCategory();
        EventCategory[] values = EventCategory.values();
        String[] categoryLabels = EventCategoryLabels.choices(requireContext());
        String[] labels = new String[categoryLabels.length + 1];
        labels[0] = getString(R.string.discovery_all);
        System.arraycopy(categoryLabels, 0, labels, 1, categoryLabels.length);
        int checked = selected == null ? 0 : selected.ordinal() + 1;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.discovery_category)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    viewModel.setCategoryFilter(which == 0 ? null : values[which - 1]);
                    dialog.dismiss();
                }).show();
    }

    private void showDateDialog() {
        HomeDiscoveryState state = viewModel.getDiscoveryState().getValue();
        int checked = state == null ? 0 : state.getDateFilter().ordinal();
        String[] labels = {
                getString(R.string.discovery_section_upcoming),
                getString(R.string.discovery_date_today),
                getString(R.string.discovery_date_this_week),
                getString(R.string.discovery_date_all)
        };
        HomeDiscoveryState.DateFilter[] values = HomeDiscoveryState.DateFilter.values();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.discovery_date)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    viewModel.setDateFilter(values[which]);
                    dialog.dismiss();
                }).show();
    }

    private void showSortDialog() {
        HomeDiscoveryState state = viewModel.getDiscoveryState().getValue();
        int checked = state == null ? 0 : state.getSortOrder().ordinal();
        String[] labels = {getString(R.string.discovery_soonest),
                getString(R.string.discovery_sort_newest)};
        HomeDiscoveryState.SortOrder[] values = HomeDiscoveryState.SortOrder.values();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.discovery_sort)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    viewModel.setSortOrder(values[which]);
                    dialog.dismiss();
                }).show();
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
