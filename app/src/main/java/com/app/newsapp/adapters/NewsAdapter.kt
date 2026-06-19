package com.app.newsapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.newsapp.R
import com.app.newsapp.models.NewsArticle

/**
 * NewsAdapter — RecyclerView Adapter now bound to the live API model (NewsArticle).
 *
 * F1 REQUIREMENT: Displays articles fetched from NewsAPI via Retrofit.
 * F3 REQUIREMENT: Custom ViewHolder, inflates item_news_card.xml.
 * F5 REQUIREMENT: filter() for real-time title search, filterByCategory() for chip clicks.
 *
 * Both local (DataSource) fallback and live API data use this adapter —
 * the adapter works with NewsArticle throughout Assignment 4.
 */
class NewsAdapter(
    private val onItemClick: (NewsArticle) -> Unit,
    private val onBookmarkClick: ((NewsArticle, Boolean) -> Unit)? = null
) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    // Master list — never mutated, always re-assigned
    private var masterList: List<NewsArticle> = emptyList()

    // Displayed list — filtered subset of masterList
    private var displayList: List<NewsArticle> = emptyList()

    // Bug Fix: Persist saved UI state during scrolling
    private var savedUrls: MutableSet<String> = mutableSetOf()

    fun updateSavedUrls(urls: Set<String>) {
        savedUrls.clear()
        savedUrls.addAll(urls)
        notifyDataSetChanged()
    }

    /**
     * F3: ViewHolder holding references to all views in item_news_card.xml
     */
    inner class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNewsHeadline: TextView = itemView.findViewById(R.id.tvNewsHeadline)
        val tvNewsSource: TextView = itemView.findViewById(R.id.tvNewsSource)
        val tvNewsTimeAgo: TextView = itemView.findViewById(R.id.tvNewsTimeAgo)
        val tvNewsCategory: TextView = itemView.findViewById(R.id.tvNewsCategory)
        val tvReadTimeBadge: TextView = itemView.findViewById(R.id.tvReadTimeBadge)
        val tvBottomChip: TextView = itemView.findViewById(R.id.tvBottomChip)
        val ivBookmark: ImageView = itemView.findViewById(R.id.ivBookmark)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news_card, parent, false)
        return NewsViewHolder(view)
    }

    /**
     * F3: Binds NewsArticle fields to the ViewHolder.
     * Handles nulls gracefully — API fields are all nullable.
     */
    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val article = displayList[position]

        holder.tvNewsHeadline.text = article.title ?: "No title"
        holder.tvNewsSource.text = article.source?.name ?: "Unknown"
        holder.tvNewsTimeAgo.text = formatPublishedAt(article.publishedAt)
        holder.tvNewsCategory.text = ""          // not in API response; left blank
        holder.tvReadTimeBadge.text = estimateReadTime(article.description)
        holder.tvBottomChip.text = article.source?.name ?: ""

        val realUrl = article.url ?: ""
        val titleStr = article.title ?: "Untitled"
        val articleUrl = if (realUrl.isNotBlank()) realUrl else "local_${titleStr.hashCode()}"

        val isSaved = savedUrls.contains(articleUrl)
        if (isSaved) {
            holder.ivBookmark.setColorFilter(holder.itemView.context.getColor(R.color.colorPrimary))
        } else {
            holder.ivBookmark.clearColorFilter()
        }

        holder.itemView.setOnClickListener {
            onItemClick(article)
        }

        // Bookmark icon — triggers callback to save/delete in SQLite and updates UI optimistically
        holder.ivBookmark.setOnClickListener {
            val currentlySaved = savedUrls.contains(articleUrl)
            
            // Optimistic UI update to prevent spam-click issues
            if (currentlySaved) {
                savedUrls.remove(articleUrl)
                holder.ivBookmark.clearColorFilter()
            } else {
                savedUrls.add(articleUrl)
                holder.ivBookmark.setColorFilter(holder.itemView.context.getColor(R.color.colorPrimary))
            }

            // Tell fragment to do DB write
            onBookmarkClick?.invoke(article, !currentlySaved)
        }
    }

    override fun getItemCount(): Int = displayList.size

    /**
     * Replaces the full data set and resets any active filters.
     * Called from HomeFragment after a successful API response.
     */
    fun updateList(articles: List<NewsArticle>) {
        masterList = articles
        displayList = articles.toList()
        notifyDataSetChanged()
    }

    // Keep submitList as an alias so existing code compiles
    fun submitList(articles: List<NewsArticle>) = updateList(articles)

    /**
     * F5: Filters displayed list by title substring (case-insensitive).
     * Blank query resets to the full master list.
     */
    fun filter(query: String) {
        displayList = if (query.isBlank()) {
            masterList.toList()
        } else {
            masterList.filter { article ->
                article.title?.contains(query.trim(), ignoreCase = true) == true
            }
        }
        notifyDataSetChanged()
    }

    /**
     * F5: Filters displayed list by source name (used as a proxy for category
     * since NewsAPI top-headlines doesn't return a category per article).
     * "Top Stories" resets the filter.
     */
    fun filterByCategory(category: String) {
        displayList = if (category.equals("Top Stories", ignoreCase = true) || category.isBlank()) {
            masterList.toList()
        } else {
            masterList.filter { article ->
                article.source?.name?.contains(category, ignoreCase = true) == true
            }
        }
        notifyDataSetChanged()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Converts ISO 8601 publishedAt to a human-readable relative string. */
    private fun formatPublishedAt(publishedAt: String?): String {
        if (publishedAt == null) return "Recently"
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val date = sdf.parse(publishedAt) ?: return "Recently"
            val diffMs = System.currentTimeMillis() - date.time
            val diffH = diffMs / (1000 * 60 * 60)
            when {
                diffH < 1  -> "Just now"
                diffH < 24 -> "$diffH hours ago"
                diffH < 48 -> "Yesterday"
                else       -> "${diffH / 24} days ago"
            }
        } catch (e: Exception) {
            "Recently"
        }
    }

    /** Estimates reading time from description word count (~200 wpm average). */
    private fun estimateReadTime(description: String?): String {
        val words = description?.split(" ")?.size ?: 50
        val minutes = maxOf(1, words / 40)
        return "$minutes min"
    }
}
