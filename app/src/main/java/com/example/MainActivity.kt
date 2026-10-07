package com.example

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.databinding.ActivityMainBinding
import com.example.ui.home.HomeFragment
import com.example.ui.movies.MoviesFragment
import com.example.ui.mylist.MyListFragment
import com.example.ui.search.SearchFragment
import com.example.ui.series.SeriesFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val homeFragment by lazy { HomeFragment() }
    private val moviesFragment by lazy { MoviesFragment() }
    private val seriesFragment by lazy { SeriesFragment() }
    private val searchFragment by lazy { SearchFragment() }
    private val myListFragment by lazy { MyListFragment() }

    private var activeFragment: Fragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFragments(savedInstanceState)
        setupNavigation()
        setupBackHandler()
    }

    private fun setupFragments(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, homeFragment, TAG_HOME)
                .commit()
            activeFragment = homeFragment
        }
    }

    private fun setupNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    switchFragment(homeFragment, TAG_HOME)
                    true
                }
                R.id.nav_movies -> {
                    switchFragment(moviesFragment, TAG_MOVIES)
                    true
                }
                R.id.nav_series -> {
                    switchFragment(seriesFragment, TAG_SERIES)
                    true
                }
                R.id.nav_search -> {
                    switchFragment(searchFragment, TAG_SEARCH)
                    true
                }
                R.id.nav_mylist -> {
                    switchFragment(myListFragment, TAG_MYLIST)
                    true
                }
                else -> false
            }
        }

        binding.btnHeaderSearch.setOnClickListener {
            binding.bottomNav.selectedItemId = R.id.nav_search
        }

        binding.btnHeaderDownloads.setOnClickListener {
            binding.bottomNav.selectedItemId = R.id.nav_mylist
        }
    }

    private fun switchFragment(targetFragment: Fragment, tag: String) {
        if (activeFragment == targetFragment) return

        val transaction = supportFragmentManager.beginTransaction()

        if (!targetFragment.isAdded) {
            transaction.hide(activeFragment).add(R.id.fragment_container, targetFragment, tag)
        } else {
            transaction.hide(activeFragment).show(targetFragment)
        }

        activeFragment = targetFragment
        transaction.commit()
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (activeFragment != homeFragment) {
                    binding.bottomNav.selectedItemId = R.id.nav_home
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    companion object {
        private const val TAG_HOME = "tag_home"
        private const val TAG_MOVIES = "tag_movies"
        private const val TAG_SERIES = "tag_series"
        private const val TAG_SEARCH = "tag_search"
        private const val TAG_MYLIST = "tag_mylist"
    }
}
