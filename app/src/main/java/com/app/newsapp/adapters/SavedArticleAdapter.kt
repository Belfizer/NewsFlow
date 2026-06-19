package com.app.newsapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.newsapp.R
import com.app.newsapp.models.SavedArticle

/**
 * SavedArticleAdapter — RecyclerView adapter for the saved articles list in SavedFragment.
 *
 * Inflates item_news_card.xml and binds SavedArticle fields.
 * onItemClick lambda passes the tapped SavedArticle back to SavedFragment.
 */
class SavedArticleAdapter(
    private val onItemClick: (SavedArticle) -> Unit
) : RecyclerView.Adapter<SavedArticleAdapter.SavedViewHolder>() {

    private var articles: List<SavedArticle> = emptyList()

    inner class SavedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNewsHeadline: TextView = itemView.findViewById(R.id.tvNewsHeadline)
        val tvNewsSource: TextView = itemView.findViewById(R.id.tvNewsSource)
        val tvNewsTimeAgo: TextView = itemView.findViewById(R.id.tvNewsTimeAgo)
        val tvNewsCategory: TextView = itemView.findViewById(R.id.tvNewsCategory)
        val tvReadTimeBadge: TextView = itemView.findViewById(R.id.tvReadTimeBadge)
        val tvBottomChip: TextView = itemView.findViewById(R.id.tvBottomChip)
        val ivBookmark: android.widget.ImageView = itemView.findViewById(R.id.ivBookmark)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news_card, parent, false)
        return SavedViewHolder(view)
    }

    override fun onBindViewHolder(holder: SavedViewHolder, position: Int) {
        val article = articles[position]
        holder.tvNewsHeadline.text = article.title
        holder.tvNewsSource.text = article.sourceName
        holder.tvNewsTimeAgo.text = article.savedDate
        holder.tvNewsCategory.text = article.category.uppercase()
        holder.tvReadTimeBadge.text = "Saved"
        holder.tvBottomChip.text = article.sourceName
        
        // Everything in the Saved tab is saved, so force the icon to red
        holder.ivBookmark.setColorFilter(holder.itemView.context.getColor(R.color.colorPrimary))
        
        holder.itemView.setOnClickListener { onItemClick(article) }
    }

    override fun getItemCount(): Int = articles.size

    fun updateList(newList: List<SavedArticle>) {
        articles = newList
        notifyDataSetChanged()
    }
}
