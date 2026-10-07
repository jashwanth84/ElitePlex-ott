package com.example.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ElitePlexApplication
import com.example.R
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.MovieItem
import com.example.databinding.FragmentHomeBinding
import com.example.databinding.ItemContentSectionBinding
import com.example.ui.details.MovieDetailsActivity
import com.example.ui.player.PlayerActivity
import com.example.ui.viewmodel.HomeUiState
import com.example.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by lazy {
        val app = requireActivity().application as ElitePlexApplication
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(app.movieRepository, app.searchRepository) as T
            }
        })[HomeViewModel::class.java]
    }

    private lateinit var continueWatchingAdapter: ContinueWatchingAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupContinueWatching()
        setupListeners()
        observeData()
    }

    private fun setupContinueWatching() {
        continueWatchingAdapter = ContinueWatchingAdapter { historyItem ->
            val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_CONTENT_ID, historyItem.tmdbId)
                putExtra(PlayerActivity.EXTRA_MEDIA_TYPE, historyItem.type)
                putExtra(PlayerActivity.EXTRA_TITLE, historyItem.title)
                putExtra(PlayerActivity.EXTRA_SEASON, historyItem.seasonNumber)
                putExtra(PlayerActivity.EXTRA_EPISODE, historyItem.episodeNumber)
                putExtra(PlayerActivity.EXTRA_RESUME_POS, historyItem.playbackPosition)
            }
            startActivity(intent)
        }
        binding.rvContinueWatching.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = continueWatchingAdapter
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setColorSchemeResources(R.color.accent_gold)
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadHomeCatalog()
        }

        binding.btnRetry.setOnClickListener {
            viewModel.loadHomeCatalog()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collectLatest { state ->
                        renderState(state)
                    }
                }
                launch {
                    viewModel.watchHistory.collectLatest { history ->
                        if (history.isNotEmpty()) {
                            binding.layoutContinueWatching.visibility = View.VISIBLE
                            continueWatchingAdapter.submitList(history)
                        } else {
                            binding.layoutContinueWatching.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun renderState(state: HomeUiState) {
        when (state) {
            is HomeUiState.Loading -> {
                binding.pbLoading.visibility = View.VISIBLE
                binding.layoutError.visibility = View.GONE
                if (!binding.swipeRefresh.isRefreshing) {
                    binding.layoutContent.visibility = View.GONE
                }
            }
            is HomeUiState.Success -> {
                binding.pbLoading.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
                binding.layoutContent.visibility = View.VISIBLE
                binding.swipeRefresh.isRefreshing = false

                setupHeroSection(state.heroItems)
                setupContentSections(state)
            }
            is HomeUiState.Error -> {
                binding.pbLoading.visibility = View.GONE
                binding.layoutError.visibility = View.VISIBLE
                binding.layoutContent.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                binding.tvErrorMessage.text = state.message
            }
        }
    }

    private fun setupHeroSection(heroItems: List<MovieItem>) {
        if (heroItems.isEmpty()) {
            binding.heroViewPager.visibility = View.GONE
            return
        }
        binding.heroViewPager.visibility = View.VISIBLE
        val adapter = HeroBannerAdapter(
            items = heroItems,
            onWatchClick = { item ->
                val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                    putExtra(PlayerActivity.EXTRA_CONTENT_ID, item.displayId)
                    putExtra(PlayerActivity.EXTRA_MEDIA_TYPE, if (item.isTvSeries) "tv" else "movie")
                    putExtra(PlayerActivity.EXTRA_TITLE, item.displayTitle)
                }
                startActivity(intent)
            },
            onDetailsClick = { item ->
                openDetails(item)
            }
        )
        binding.heroViewPager.adapter = adapter
    }

    private fun setupContentSections(state: HomeUiState.Success) {
        binding.sectionsContainer.removeAllViews()

        val sections = listOf(
            "Trending Now" to state.trending,
            "Popular Movies" to state.popularMovies,
            "Popular TV Series" to state.popularTv,
            "Trending Anime & Animation" to state.trendingAnime,
            "Top Rated Movies" to state.topMovies,
            "Top Rated TV Series" to state.topTv
        )

        for ((title, items) in sections) {
            if (items.isNotEmpty()) {
                addSectionView(title, items)
            }
        }
    }

    private fun addSectionView(title: String, items: List<MovieItem>) {
        val sectionBinding = ItemContentSectionBinding.inflate(
            layoutInflater, binding.sectionsContainer, false
        )
        sectionBinding.tvSectionTitle.text = title

        val adapter = HorizontalMovieAdapter { item ->
            openDetails(item)
        }
        sectionBinding.rvSectionItems.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            this.adapter = adapter
        }
        adapter.submitList(items)

        binding.sectionsContainer.addView(sectionBinding.root)
    }

    private fun openDetails(item: MovieItem) {
        val intent = Intent(requireContext(), MovieDetailsActivity::class.java).apply {
            putExtra(MovieDetailsActivity.EXTRA_ID, item.displayId)
            putExtra(MovieDetailsActivity.EXTRA_IS_TV, item.isTvSeries)
            putExtra(MovieDetailsActivity.EXTRA_TITLE, item.displayTitle)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
