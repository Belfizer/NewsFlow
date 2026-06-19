package com.app.newsapp.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.helper.widget.Flow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.app.newsapp.R
import com.app.newsapp.database.DatabaseHelper
import com.app.newsapp.firebase.FirestoreHelper
import com.app.newsapp.models.NewsArticle
import com.app.newsapp.models.NewsSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * ArticleDetailFragment — full article reading view.
 *
 * Bug Fix 4: "Read Full Article" opens the real URL in the device browser via Intent.ACTION_VIEW.
 * Bug Fix 5: Bookmark button does a real SQLite write/delete; icon reflects saved state on open.
 * Assignment 5 F2: Bookmark also syncs to Firestore (syncBookmarkToFirestore / removeBookmarkFromFirestore).
 * Assignment 5 Feature 2: TTS reader — speaker button reads the article title + body aloud.
 */
class ArticleDetailFragment : Fragment(), TextToSpeech.OnInitListener {

    private lateinit var tvArticleCategory: TextView
    private lateinit var tvArticleHeadline: TextView
    private lateinit var tvArticleSource: TextView
    private lateinit var tvArticleTimeAgo: TextView
    private lateinit var tvArticleReadTime: TextView
    private lateinit var tvArticleBody: TextView
    private lateinit var btnReadFull: Button
    private lateinit var btnBookmarkOutline: FrameLayout
    private lateinit var btnTts: ImageView
    private lateinit var tagCloudContainer: ConstraintLayout
    private lateinit var tagFlow: Flow

    private lateinit var dbHelper: DatabaseHelper
    private val firestoreHelper = FirestoreHelper()

    // Feature 2: Text-to-Speech
    private var tts: TextToSpeech? = null
    private var isSpeaking = false

    // Resolved article fields (unified from both Bundle paths)
    private var articleUrl: String = ""
    private var articleTitle: String = ""
    private var articleSource: String = ""
    private var articleDesc: String = ""
    private var articleDate: String = ""
    private var articleCategory: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_article_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())
        bindViews(view)
        resolveArticleFromBundle()

        // Bug Fix 4: Read Full Article opens real browser
        setupReadFullArticleButton(view)

        // Bug Fix 5: Bookmark syncs with SQLite on open + writes on click
        setupBookmark()

        // Feature 2: TTS reader
        tts = TextToSpeech(requireContext(), this)
        setupTtsButton()

        // F3: Log this article as read in reading_history
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            if (articleUrl.isNotBlank()) {
                dbHelper.addToHistory(articleUrl, 0)
            }
        }
    }

    private fun bindViews(view: View) {
        tvArticleCategory = view.findViewById(R.id.tvArticleCategory)
        tvArticleHeadline = view.findViewById(R.id.tvArticleHeadline)
        tvArticleSource   = view.findViewById(R.id.tvArticleSource)
        tvArticleTimeAgo  = view.findViewById(R.id.tvArticleTimeAgo)
        tvArticleReadTime = view.findViewById(R.id.tvArticleReadTime)
        tvArticleBody     = view.findViewById(R.id.tvArticleBody)
        btnReadFull       = view.findViewById(R.id.btnReadFull)
        btnBookmarkOutline = view.findViewById(R.id.btnBookmarkOutline)
        btnTts            = view.findViewById(R.id.btnTts)
        tagCloudContainer = view.findViewById(R.id.tagCloudContainer)
        tagFlow           = view.findViewById(R.id.tagFlow)
    }

    /**
     * Resolves article data from either:
     * 1. NEWS_ARTICLE (live API flow from HomeFragment / SearchFragment)
     * 2. Individual SAVED_ARTICLE_* strings (SQLite flow from SavedFragment)
     */
    private fun resolveArticleFromBundle() {
        val args = arguments ?: return

        @Suppress("DEPRECATION")
        val newsArticle = args.getSerializable("NEWS_ARTICLE") as? NewsArticle

        if (newsArticle != null) {
            val realUrl = newsArticle.url ?: ""
            val titleStr = newsArticle.title ?: "Untitled"
            
            // If API didn't provide a URL (e.g. dummy/featured data), generate a local ID so SQLite can save it
            articleUrl = if (realUrl.isNotBlank()) realUrl else "local_${titleStr.hashCode()}"
            
            articleTitle = titleStr
            articleSource = newsArticle.source?.name ?: ""
            articleDesc = newsArticle.description ?: "Full article available at source."
            articleDate = newsArticle.publishedAt ?: ""
            articleCategory = "NEWS"
        } else {
            val savedUrl = args.getString("SAVED_ARTICLE_URL", "")
            val savedTitle = args.getString("SAVED_ARTICLE_TITLE", "")
            
            articleUrl = if (savedUrl.isNotBlank()) savedUrl else "local_${savedTitle.hashCode()}"
            articleTitle = savedTitle
            articleSource = args.getString("SAVED_ARTICLE_SOURCE", "")
            articleDesc = args.getString("SAVED_ARTICLE_DESC", "")
            articleDate = args.getString("SAVED_ARTICLE_DATE", "")
            articleCategory = args.getString("SAVED_ARTICLE_CATEGORY", "")
        }

        tvArticleCategory.text = articleCategory.uppercase()
        tvArticleHeadline.text = articleTitle
        tvArticleSource.text = articleSource
        tvArticleTimeAgo.text = articleDate.take(10)
        tvArticleReadTime.text = estimateReadTime(articleDesc)
        tvArticleBody.text = articleDesc

        buildTagCloud(listOf(articleCategory, articleSource).filter { it.isNotBlank() })
    }

    // ── Bug Fix 4: Open real browser URL ─────────────────────────────────────────

    private fun setupReadFullArticleButton(view: View) {
        btnReadFull.setOnClickListener {
            // Only attempt to open real web links, not our generated local IDs
            if (articleUrl.startsWith("http", ignoreCase = true)) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(articleUrl))
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Snackbar.make(view, "No browser found on this device", Snackbar.LENGTH_SHORT).show()
                }
            } else {
                Snackbar.make(view, "Full article link not available", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    // ── Bug Fix 5: Real SQLite bookmark save/delete + F2 Firestore sync ────────

    private fun setupBookmark() {
        if (articleUrl.isBlank()) return

        // Sync icon with saved state immediately on open
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val isSaved = dbHelper.isArticleSaved(articleUrl)
            withContext(Dispatchers.Main) {
                updateBookmarkIcon(isSaved)
            }
        }

        btnBookmarkOutline.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val currentlySaved = dbHelper.isArticleSaved(articleUrl)
                if (currentlySaved) {
                    val deleted = dbHelper.deleteSavedArticle(articleUrl)
                    // F2: also remove from Firestore
                    firestoreHelper.removeBookmarkFromFirestore(articleUrl)
                    withContext(Dispatchers.Main) {
                        if (deleted > 0) {
                            updateBookmarkIcon(false)
                            Snackbar.make(requireView(), "Removed from saved articles", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    val articleToSave = NewsArticle(
                        title = articleTitle,
                        description = articleDesc,
                        url = articleUrl,
                        urlToImage = null,
                        publishedAt = articleDate,
                        source = NewsSource(id = null, name = articleSource)
                    )
                    val rowId = dbHelper.saveArticle(articleToSave, articleCategory)
                    // F2: also sync to Firestore
                    firestoreHelper.syncBookmarkToFirestore(articleToSave, articleCategory)
                    withContext(Dispatchers.Main) {
                        if (rowId > 0) {
                            updateBookmarkIcon(true)
                            Snackbar.make(requireView(), "✓ Saved to your articles", Snackbar.LENGTH_SHORT).show()
                        } else {
                            Snackbar.make(requireView(), "Failed to save article", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun updateBookmarkIcon(isSaved: Boolean) {
        val bookmarkIcon = btnBookmarkOutline.getChildAt(0) as? ImageView ?: return
        if (isSaved) {
            bookmarkIcon.setImageResource(R.drawable.ic_bookmark_filled)
            bookmarkIcon.setColorFilter(Color.parseColor("#E63946"))
        } else {
            bookmarkIcon.setImageResource(R.drawable.ic_bookmark)
            bookmarkIcon.setColorFilter(Color.parseColor("#606060"))
        }
    }

    // ── Tag Cloud (L10) ───────────────────────────────────────────────────────

    private fun buildTagCloud(tags: List<String>) {
        val staticTagIds = listOf(
            R.id.tagPolitics, R.id.tagWorld, R.id.tagEconomy,
            R.id.tagBreaking, R.id.tagAnalysis, R.id.tagEurope
        )
        staticTagIds.forEach { id ->
            tagCloudContainer.findViewById<TextView>(id)?.visibility = View.GONE
        }

        val dynamicTagIds = mutableListOf<Int>()
        tags.forEach { tag ->
            val tagView = TextView(requireContext()).apply {
                id = View.generateViewId()
                text = tag
                setTextColor(resources.getColor(R.color.colorPrimary, null))
                textSize = 12f
                setBackgroundResource(R.drawable.bg_tag)
                val hPad = (12 * resources.displayMetrics.density).toInt()
                val vPad = (6 * resources.displayMetrics.density).toInt()
                setPadding(hPad, vPad, hPad, vPad)
            }
            val params = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            tagCloudContainer.addView(tagView, params)
            dynamicTagIds.add(tagView.id)
        }
        if (dynamicTagIds.isNotEmpty()) {
            tagFlow.referencedIds = dynamicTagIds.toIntArray()
        }
    }

    private fun estimateReadTime(text: String?): String {
        val words = text?.split(" ")?.size ?: 50
        val minutes = maxOf(1, words / 40)
        return "$minutes min read"
    }

    // ── Feature 2: Text-to-Speech Article Reader ──────────────────────────────

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setSpeechRate(0.9f)
            tts?.setPitch(1.0f)
        }
    }

    private fun setupTtsButton() {
        btnTts.setOnClickListener {
            if (isSpeaking) {
                tts?.stop()
                isSpeaking = false
                btnTts.setColorFilter(Color.parseColor("#606060"))
            } else {
                val textToRead = "${articleTitle}. ${articleDesc}"
                tts?.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, "NEWS_TTS")
                isSpeaking = true
                btnTts.setColorFilter(Color.parseColor("#E63946"))
            }
        }
    }

    override fun onDestroyView() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroyView()
    }
}
