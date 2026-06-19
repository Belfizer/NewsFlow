package com.app.newsapp.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.app.newsapp.R

/**
 * CategoryAdapter — populates the GridLayout with category items.
 *
 * Inflates item_category_card.xml for each category and sets:
 * - The correct category icon drawable
 * - The category name label
 * - A click listener that triggers the onCategoryClick callback
 *
 * The first item ("Top Stories") uses layout_columnSpan="2" (L6 requirement).
 */
class CategoryAdapter(
    private val categories: List<String>,
    private val onCategoryClick: (String) -> Unit
) {

    // Maps category name to its drawable resource
    private val categoryIcons = mapOf(
        "Top Stories" to R.drawable.ic_trending,
        "Technology" to R.drawable.ic_category_tech,
        "Sports" to R.drawable.ic_category_sports,
        "Business" to R.drawable.ic_category_business,
        "Health" to R.drawable.ic_category_health,
        "Science" to R.drawable.ic_category_science,
        "Entertainment" to R.drawable.ic_category_entertainment
    )

    /**
     * Clears the GridLayout and populates it dynamically.
     * "Top Stories" gets columnSpan=2 to fulfil the L6 requirement.
     */
    fun bindTo(grid: GridLayout) {
        grid.removeAllViews()
        val inflater = LayoutInflater.from(grid.context)

        categories.forEachIndexed { index, category ->
            val isTopStories = category.equals("Top Stories", ignoreCase = true)

            if (isTopStories) {
                // "Top Stories" — special chip spanning 2 columns (L6 REQUIREMENT)
                val chipView = LinearLayout(grid.context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = android.view.Gravity.CENTER
                    setBackgroundResource(R.drawable.bg_chip_active)
                    minimumHeight = (56 * grid.context.resources.displayMetrics.density).toInt()
                    val innerPadding = (12 * grid.context.resources.displayMetrics.density).toInt()
                    setPadding(innerPadding, innerPadding, innerPadding, innerPadding)
                }

                val label = TextView(grid.context).apply {
                    text = category
                    setTextColor(Color.WHITE)
                    textSize = 13f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    gravity = android.view.Gravity.CENTER
                }
                chipView.addView(label)
                chipView.setOnClickListener { onCategoryClick(category) }

                val params = GridLayout.LayoutParams().apply {
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 2, 2f)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    val margin = (4 * grid.context.resources.displayMetrics.density).toInt()
                    setMargins(margin, margin, margin, margin)
                }
                grid.addView(chipView, params)

            } else {
                // Regular category card
                val cardView = inflater.inflate(R.layout.item_category_card, grid, false)

                // Set icon
                val iconRes = categoryIcons[category] ?: R.drawable.ic_trending
                cardView.findViewById<ImageView>(R.id.ivCategoryIcon).setImageResource(iconRes)

                // Set name
                cardView.findViewById<TextView>(R.id.tvCategoryName).text = category

                // Click listener
                cardView.setOnClickListener { onCategoryClick(category) }

                val params = GridLayout.LayoutParams().apply {
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    val margin = (4 * grid.context.resources.displayMetrics.density).toInt()
                    setMargins(margin, margin, margin, margin)
                }
                grid.addView(cardView, params)
            }
        }
    }
}
