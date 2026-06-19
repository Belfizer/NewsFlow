package com.app.newsapp.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.app.newsapp.R
import com.app.newsapp.adapters.CategoryAdapter
import com.app.newsapp.adapters.FeaturedAdapter
import com.app.newsapp.adapters.NewsAdapter
import com.app.newsapp.database.DatabaseHelper
import com.app.newsapp.models.DataSource
import com.app.newsapp.models.NewsArticle
import com.app.newsapp.repository.NewsRepository
import com.app.newsapp.repository.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * HomeFragment — main news feed screen.
 *
 * Assignment 4 updates:
 * F1: Loads live articles via NewsRepository (Retrofit + coroutines)
 * F4: API module — completely separate from SQLite saved/history
 * F5: Real-time search calls repository.searchArticles() when query >= 3 chars;
 *     category chip calls repository.getByCategory()
 *
 * Fallback: on any API error shows Snackbar and falls back to dummy DataSource.
 */
class HomeFragment : Fragment() {

    // Views
    private lateinit var tvGreeting: TextView
    private lateinit var etSearch: EditText
    private lateinit var breakingCarouselContainer: LinearLayout
    private lateinit var categoryGrid: GridLayout
    private lateinit var rvLatestNews: RecyclerView

    // Adapters / repository
    private lateinit var newsAdapter: NewsAdapter
    private val repository = NewsRepository()
    private lateinit var dbHelper: DatabaseHelper

    // Fallback data from DataSource (Assignment 3 dummy data)
    private val fallbackArticles by lazy { DataSource.getArticles() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_home, container, false)

    private var isCategoriesExpanded = false
    private lateinit var tvCategorySeeAll: android.widget.TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())

        bindViews(view)
        setupGreeting()
        setupCategoryGrid()
        setupNewsRecyclerView()
        setupSearch()

        // F1: Load live headlines on start
        loadTopHeadlines()
        loadFeaturedArticles()
    }

    override fun onResume() {
        super.onResume()
        if (::newsAdapter.isInitialized) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val savedUrls = dbHelper.getSavedArticles().map { it.articleId }.toSet()
                withContext(Dispatchers.Main) {
                    newsAdapter.updateSavedUrls(savedUrls)
                }
            }
        }
    }

    private fun bindViews(view: View) {
        tvGreeting = view.findViewById(R.id.tvGreeting)
        etSearch = view.findViewById(R.id.etSearch)
        breakingCarouselContainer = view.findViewById(R.id.breakingCarouselContainer)
        categoryGrid = view.findViewById(R.id.categoryGrid)
        rvLatestNews = view.findViewById(R.id.rvLatestNews)
        tvCategorySeeAll = view.findViewById(R.id.tvCategorySeeAll)
    }

    private fun setupGreeting() {
        val userName = arguments?.getString("USER_NAME") ?: "Reader"
        tvGreeting.text = "Good Morning, $userName \uD83D\uDCF0"
    }

    // ── F1: API calls ──────────────────────────────────────────────────────────

    /**
     * F1: Fetches US top headlines on Dispatchers.IO,
     * updates the RecyclerView adapter on Dispatchers.Main.
     */
    private fun loadTopHeadlines() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val result = repository.getTopHeadlines()
            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> newsAdapter.updateList(result.data)
                    is Result.Error   -> showErrorFallback(result.message)
                    else -> {}
                }
            }
        }
    }

    /**
     * F1: Fetches articles for a specific category, triggered by category chip click.
     */
    private fun loadByCategory(category: String) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val result = repository.getByCategory(category)
            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> newsAdapter.updateList(result.data)
                    is Result.Error   -> showErrorFallback(result.message)
                    else -> {}
                }
            }
        }
    }

    /**
     * F1 + F5: Searches the NewsAPI when query length >= 3 characters.
     */
    private fun searchArticlesFromApi(query: String) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val result = repository.searchArticles(query)
            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> newsAdapter.updateList(result.data)
                    is Result.Error   -> {
                        // Fall back to local filter on the current list
                        newsAdapter.filter(query)
                    }
                    else -> {}
                }
            }
        }
    }

    // ── Featured carousel (uses DataSource as featured doesn't map from API) ──

    private fun loadFeaturedArticles() {
        // Convert DataSource articles → NewsArticle wrappers for the carousel
        val featured = DataSource.getFeaturedArticles()
        val asApiArticles = featured.map { a ->
            NewsArticle(
                title = a.title,
                description = a.description,
                url = null,
                urlToImage = null,
                publishedAt = null,
                source = com.app.newsapp.models.NewsSource(id = null, name = a.source)
            )
        }
        // FeaturedAdapter still works with its own item_featured_card.xml layout
        // We re-use the DataSource.getFeaturedArticles() for the carousel visual
        val featuredAdapter = FeaturedAdapter(featured) { article ->
            // Convert legacy Article → NewsArticle for bundle passing
            onArticleClick(
                NewsArticle(
                    title = article.title,
                    description = article.description,
                    url = null,
                    urlToImage = null,
                    publishedAt = null,
                    source = com.app.newsapp.models.NewsSource(id = null, name = article.source)
                )
            )
        }
        featuredAdapter.bindTo(breakingCarouselContainer)
    }

    // ── Category grid ──────────────────────────────────────────────────────────

    private fun setupCategoryGrid() {
        val allCategories = DataSource.getCategories()
        
        // Hide the last 2 categories if not expanded
        val displayCategories = if (isCategoriesExpanded) {
            allCategories
        } else {
            allCategories.dropLast(2)
        }

        val categoryAdapter = CategoryAdapter(displayCategories) { selectedCategory ->
            if (selectedCategory.equals("Top Stories", ignoreCase = true)) {
                loadTopHeadlines()
            } else {
                loadByCategory(selectedCategory)
            }
        }
        categoryAdapter.bindTo(categoryGrid)

        // Update See All button text and click listener
        tvCategorySeeAll.text = if (isCategoriesExpanded) "Show Less" else "See All"
        tvCategorySeeAll.setOnClickListener {
            isCategoriesExpanded = !isCategoriesExpanded
            setupCategoryGrid() // Re-render grid with new state
        }
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────

    private fun setupNewsRecyclerView() {
        newsAdapter = NewsAdapter(
            onItemClick = { article -> onArticleClick(article) },
            onBookmarkClick = { article, isSaving -> handleBookmarkClick(article, isSaving) }
        )
        rvLatestNews.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = newsAdapter
            isNestedScrollingEnabled = false
        }
    }

    // ── Bug Fix 5: Actual SQLite bookmark logic for feed cards ────────────────

    private fun handleBookmarkClick(article: NewsArticle, isSaving: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val realUrl = article.url ?: ""
            val titleStr = article.title ?: "Untitled"
            val articleUrl = if (realUrl.isNotBlank()) realUrl else "local_${titleStr.hashCode()}"

            if (isSaving) {
                val articleToSave = NewsArticle(
                    title = titleStr,
                    description = article.description ?: "Full article available at source.",
                    url = articleUrl,
                    urlToImage = null,
                    publishedAt = article.publishedAt ?: "",
                    source = article.source
                )
                dbHelper.saveArticle(articleToSave, "NEWS")
                withContext(Dispatchers.Main) {
                    Snackbar.make(requireView(), "✓ Saved to your articles", Snackbar.LENGTH_SHORT).show()
                }
            } else {
                dbHelper.deleteSavedArticle(articleUrl)
                withContext(Dispatchers.Main) {
                    Snackbar.make(requireView(), "Removed from saved articles", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────

    /**
     * F5: TextWatcher — triggers API search when query has >= 3 chars.
     * Shorter queries fall back to client-side adapter.filter().
     */
    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                if (query.length >= 3) {
                    searchArticlesFromApi(query)
                } else {
                    newsAdapter.filter(query)
                }
            }
        })
    }

    // ── Article click → ArticleDetailFragment (F2) ────────────────────────────

    private fun onArticleClick(article: NewsArticle) {
        val bundle = Bundle()
        bundle.putSerializable("NEWS_ARTICLE", article as java.io.Serializable)

        val detailFragment = ArticleDetailFragment()
        detailFragment.arguments = bundle

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, detailFragment)
            .addToBackStack("detail")
            .commit()
    }

    // ── Error handling ────────────────────────────────────────────────────────

    /**
     * On API failure: shows a Snackbar, loads dummy DataSource articles so the
     * screen is never blank. No crash — graceful degradation.
     */
    private fun showErrorFallback(message: String) {
        Snackbar.make(requireView(), "Could not load news: $message", Snackbar.LENGTH_LONG).show()
        // Load dummy data as fallback
        val dummyAsApi = fallbackArticles.map { a ->
            NewsArticle(
                title = a.title,
                description = a.description,
                url = null,
                urlToImage = null,
                publishedAt = null,
                source = com.app.newsapp.models.NewsSource(id = null, name = a.source)
            )
        }
        newsAdapter.updateList(dummyAsApi)
    }
}
