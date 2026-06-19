package com.app.newsapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.app.newsapp.R
import com.app.newsapp.models.Article

/**
 * FeaturedAdapter — populates the horizontal breaking-news carousel.
 *
 * Unlike NewsAdapter this does NOT use RecyclerView.
 * It inflates item_featured_card.xml views and adds them directly
 * to the LinearLayout inside the HorizontalScrollView.
 *
 * This matches the Assignment 2 structure where the carousel container
 * is a horizontal LinearLayout.
 */
class FeaturedAdapter(
    private val articles: List<Article>,
    private val onItemClick: (Article) -> Unit
) {

    /**
     * Clears the container then inflates and adds one view per featured article.
     */
    fun bindTo(container: LinearLayout) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(container.context)

        articles.forEach { article ->
            val cardView = inflater.inflate(R.layout.item_featured_card, container, false)

            // Set dimensions for each card
            val params = LinearLayout.LayoutParams(
                container.context.resources.getDimensionPixelSize(R.dimen.featured_card_width),
                container.context.resources.getDimensionPixelSize(R.dimen.featured_card_height)
            )
            params.marginEnd = container.context.resources.getDimensionPixelSize(R.dimen.card_spacing)
            cardView.layoutParams = params

            // Bind article data
            cardView.findViewById<TextView>(R.id.tvFeaturedCategory).text =
                article.category.uppercase()
            cardView.findViewById<TextView>(R.id.tvFeaturedHeadline).text = article.title
            cardView.findViewById<TextView>(R.id.tvFeaturedReadTime).text = article.readTime
            cardView.findViewById<TextView>(R.id.tvFeaturedSource).text = article.source

            cardView.setOnClickListener { onItemClick(article) }
            container.addView(cardView)
        }
    }
}
