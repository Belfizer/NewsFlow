package com.app.newsapp.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.app.newsapp.R
import com.app.newsapp.activities.ScanHeadlineActivity
import com.app.newsapp.adapters.NewsAdapter
import com.app.newsapp.models.NewsArticle
import com.app.newsapp.repository.NewsRepository
import com.app.newsapp.repository.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * SearchFragment — dedicated search screen.
 *
 * Bug Fix 2:
 * - Opens with keyboard popped automatically
 * - Completely dark empty background before any search
 * - Results appear in RecyclerView only after pressing Search/Enter
 * - Shows "No results found 😔" when API returns empty list
 * - Uses NewsRepository.searchArticles() on Dispatchers.IO
 */
class SearchFragment : Fragment() {

    private lateinit var etSearchInput: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var tvSearchNoResults: TextView
    private lateinit var tvSearchPrompt: TextView
    private lateinit var btnScanHeadline: ImageView

    private lateinit var searchAdapter: NewsAdapter
    private val repository = NewsRepository()

    // Feature 1: ActivityResult launcher for ScanHeadlineActivity
    private val scanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val scanned = result.data?.getStringExtra(ScanHeadlineActivity.RESULT_KEY)
            if (!scanned.isNullOrBlank()) {
                etSearchInput.setText(scanned)
                hideKeyboard()
                performSearch(scanned)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_search, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupRecyclerView()

        // Bug Fix 2: Auto-focus search bar and pop keyboard immediately
        etSearchInput.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etSearchInput, InputMethodManager.SHOW_IMPLICIT)

        // Trigger search on keyboard "Search" action
        etSearchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = etSearchInput.text.toString().trim()
                if (query.isNotEmpty()) {
                    hideKeyboard()
                    performSearch(query)
                }
                true
            } else false
        }
    }

    override fun onResume() {
        super.onResume()
        if (::searchAdapter.isInitialized) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val dbHelper = com.app.newsapp.database.DatabaseHelper(requireContext())
                val savedUrls = dbHelper.getSavedArticles().map { it.articleId }.toSet()
                withContext(Dispatchers.Main) {
                    searchAdapter.updateSavedUrls(savedUrls)
                }
            }
        }
    }

    private fun bindViews(view: View) {
        etSearchInput      = view.findViewById(R.id.etSearchInput)
        progressBar        = view.findViewById(R.id.searchProgressBar)
        rvSearchResults    = view.findViewById(R.id.rvSearchResults)
        tvSearchNoResults  = view.findViewById(R.id.tvSearchNoResults)
        tvSearchPrompt     = view.findViewById(R.id.tvSearchPrompt)
        btnScanHeadline    = view.findViewById(R.id.btnScanHeadline)

        // Feature 1: launch camera scanner on button tap
        btnScanHeadline.setOnClickListener {
            scanLauncher.launch(
                android.content.Intent(requireContext(), ScanHeadlineActivity::class.java)
            )
        }
    }

    private fun setupRecyclerView() {
        searchAdapter = NewsAdapter(
            onItemClick = { article -> onArticleClick(article) },
            onBookmarkClick = { article, isSaving -> handleBookmarkClick(article, isSaving) }
        )
        rvSearchResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchAdapter
        }
    }

    private fun handleBookmarkClick(article: NewsArticle, isSaving: Boolean) {
        val dbHelper = com.app.newsapp.database.DatabaseHelper(requireContext())
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

    /**
     * Calls NewsRepository.searchArticles() on IO thread.
     * Shows ProgressBar during load, RecyclerView on success, or error Snackbar.
     */
    private fun performSearch(query: String) {
        // Show spinner, hide everything else
        progressBar.visibility = View.VISIBLE
        rvSearchResults.visibility = View.GONE
        tvSearchNoResults.visibility = View.GONE
        tvSearchPrompt.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val result = repository.searchArticles(query)
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                when (result) {
                    is Result.Success -> {
                        if (result.data.isEmpty()) {
                            tvSearchNoResults.visibility = View.VISIBLE
                            rvSearchResults.visibility = View.GONE
                        } else {
                            searchAdapter.updateList(result.data)
                            rvSearchResults.visibility = View.VISIBLE
                            tvSearchNoResults.visibility = View.GONE
                        }
                    }
                    is Result.Error -> {
                        tvSearchNoResults.visibility = View.VISIBLE
                        Snackbar.make(
                            requireView(),
                            "No results found for: $query",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                    else -> {}
                }
            }
        }
    }

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

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }
}
