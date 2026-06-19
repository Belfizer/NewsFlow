package com.app.newsapp.fragments

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.app.newsapp.R
import com.app.newsapp.adapters.SavedArticleAdapter
import com.app.newsapp.database.DatabaseHelper
import com.app.newsapp.firebase.FirestoreHelper
import com.app.newsapp.models.SavedArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * SavedFragment — displays articles saved to SQLite.
 *
 * Assignment 4: reads from SQLite, supports search and date filters.
 * Assignment 5 F2: setupFirestoreSync() attaches a Firestore real-time listener that
 *   refreshes the local SQLite list whenever a bookmark changes on any device.
 */
class SavedFragment : Fragment() {

    private lateinit var radioGroupFilter: RadioGroup
    private lateinit var savedArticlesTable: TableLayout
    private lateinit var rvSavedArticles: androidx.recyclerview.widget.RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var etSavedSearch: EditText

    private lateinit var savedAdapter: SavedArticleAdapter
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_saved, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())

        bindViews(view)
        setupRecyclerView()
        setupRadioGroupFilter()
        setupSavedSearch()

        // F3: Load real saved articles from SQLite on IO thread
        loadSavedArticles()

        // F2: Attach Firestore real-time listener — refreshes list on any remote change
        setupFirestoreSync()
    }

    /**
     * F2 — Real-time Firestore listener.
     * Fires loadSavedArticles() on the main thread every time the bookmarks
     * subcollection changes in Firestore (e.g., from another device).
     */
    private fun setupFirestoreSync() {
        FirestoreHelper().listenToBookmarks { _ ->
            activity?.runOnUiThread {
                loadSavedArticles()
            }
        }
    }

    // Bug Fix 5: Reload SQLite data every time user navigates to this tab
    override fun onResume() {
        super.onResume()
        if (::savedAdapter.isInitialized) {
            loadSavedArticles()
        }
    }

    private fun bindViews(view: View) {
        radioGroupFilter = view.findViewById(R.id.radioGroupFilter)
        savedArticlesTable = view.findViewById(R.id.savedArticlesTable)
        rvSavedArticles = view.findViewById(R.id.rvSavedArticles)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)

        // Try to find search bar if it exists in the layout, otherwise skip
        etSavedSearch = view.findViewWithTag("savedSearch") ?: EditText(requireContext())
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        savedAdapter = SavedArticleAdapter { savedArticle ->
            // Open article detail
            val detailFragment = ArticleDetailFragment()
            val bundle = Bundle()
            bundle.putString("SAVED_ARTICLE_URL", savedArticle.url)
            bundle.putString("SAVED_ARTICLE_TITLE", savedArticle.title)
            bundle.putString("SAVED_ARTICLE_SOURCE", savedArticle.sourceName)
            bundle.putString("SAVED_ARTICLE_DESC", savedArticle.description)
            bundle.putString("SAVED_ARTICLE_DATE", savedArticle.publishedAt)
            bundle.putString("SAVED_ARTICLE_CATEGORY", savedArticle.category)
            detailFragment.arguments = bundle
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, detailFragment)
                .addToBackStack("detail")
                .commit()
        }
        rvSavedArticles.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            adapter = savedAdapter
            isNestedScrollingEnabled = false
        }
    }

    // ── F3: Load from SQLite ──────────────────────────────────────────────────

    /**
     * F3: Reads all saved articles on IO thread, updates UI on Main thread.
     */
    private fun loadSavedArticles() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val articles = dbHelper.getSavedArticles()
            withContext(Dispatchers.Main) {
                showArticles(articles)
                populateTableLayout(articles)
            }
        }
    }

    // ── F5: Search (LIKE) ─────────────────────────────────────────────────────

    /**
     * F5: TextWatcher on saved search bar — calls LIKE query on IO thread.
     */
    private fun setupSavedSearch() {
        etSavedSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    val results = if (query.isBlank()) {
                        dbHelper.getSavedArticles()
                    } else {
                        // F5: LIKE %query% on title and source_name columns
                        dbHelper.searchSavedArticles(query)
                    }
                    withContext(Dispatchers.Main) {
                        showArticles(results)
                        populateTableLayout(results)
                    }
                }
            }
        })
    }

    // ── F5: RadioGroup filter (ORDER BY) ──────────────────────────────────────

    /**
     * F5: RadioGroup — each option triggers a different database query:
     * All   → getSavedArticles() (saved_date DESC)
     * Today → getSavedArticlesSortedBy("saved_date", false) filtered to today
     * This Week → getSavedArticlesSortedBy("saved_date", false) filtered to this week
     */
    private fun setupRadioGroupFilter() {
        radioGroupFilter.setOnCheckedChangeListener { _, checkedId ->
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val articles = when (checkedId) {
                    R.id.rbFilterAll -> dbHelper.getSavedArticles()
                    R.id.rbFilterToday -> dbHelper.getSavedArticlesSortedBy("saved_date", false)
                        .filter { it.savedDate.startsWith(today) }
                    R.id.rbFilterYesterday -> {
                        val cal = java.util.Calendar.getInstance()
                        cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                        dbHelper.getSavedArticlesSortedBy("saved_date", false)
                            .filter { it.savedDate.startsWith(yesterday) }
                    }
                    R.id.rbFilterWeek -> {
                        val cal = java.util.Calendar.getInstance()
                        cal.add(java.util.Calendar.DAY_OF_YEAR, -7)
                        val weekAgo = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(cal.time)
                        // F5: getHistoryForDateRange — date range query
                        dbHelper.getSavedArticlesSortedBy("saved_date", false)
                            .filter { it.savedDate >= weekAgo }
                    }
                    else -> dbHelper.getSavedArticles()
                }
                withContext(Dispatchers.Main) {
                    showArticles(articles)
                    populateTableLayout(articles)
                }
            }
        }
    }

    // ── TableLayout ───────────────────────────────────────────────────────────

    /**
     * L7: Programmatically populates the TableLayout with saved article rows.
     * Columns: saved_date | title (30 chars max) | source_name
     * Alternating row backgrounds: #1A1A1A / #141414
     */
    private fun populateTableLayout(articles: List<SavedArticle>) {
        // Remove all rows except header (first 2 children = header + divider)
        val keepCount = minOf(2, savedArticlesTable.childCount)
        if (savedArticlesTable.childCount > keepCount) {
            savedArticlesTable.removeViews(keepCount, savedArticlesTable.childCount - keepCount)
        }

        val density = resources.displayMetrics.density
        val cellPadding = (8 * density).toInt()

        articles.forEachIndexed { index, article ->
            // Divider row
            val divider = View(requireContext()).apply {
                setBackgroundResource(R.drawable.bg_divider)
                layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, 1)
            }
            savedArticlesTable.addView(divider)

            // Data row
            val row = TableRow(requireContext()).apply {
                minimumHeight = (48 * density).toInt()
                setBackgroundColor(
                    if (index % 2 == 0) resources.getColor(R.color.colorSurface, null)
                    else resources.getColor(R.color.colorRowAlt, null)
                )
            }

            fun cell(text: String) = TextView(requireContext()).apply {
                this.text = text
                setTextColor(resources.getColor(R.color.colorTextSecondary, null))
                textSize = 12f
                setPadding(cellPadding, cellPadding, cellPadding, cellPadding)
            }

            row.addView(cell(article.savedDate.take(10)))
            row.addView(cell(article.title.take(30) + if (article.title.length > 30) "…" else ""))
            row.addView(cell(article.sourceName))
            savedArticlesTable.addView(row)
        }
    }

    // ── State helpers ─────────────────────────────────────────────────────────

    private fun showArticles(articles: List<SavedArticle>) {
        savedAdapter.updateList(articles)
        if (articles.isEmpty()) {
            rvSavedArticles.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE
        } else {
            rvSavedArticles.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
        }
    }
}
