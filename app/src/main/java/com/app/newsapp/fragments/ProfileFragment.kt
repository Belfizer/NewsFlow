package com.app.newsapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.app.newsapp.R
import com.app.newsapp.activities.LoginActivity
import com.app.newsapp.activities.ReadingStatsActivity
import com.app.newsapp.database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ProfileFragment — user profile and reading stats.
 *
 * Assignment 4 updates:
 * Pulls real stats from SQLite:
 *   - Articles read count = reading_history row count
 *   - Saved count = saved_articles row count
 *
 * USER_NAME still received via arguments Bundle from MainActivity (F1).
 */
class ProfileFragment : Fragment() {

    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var tvStatArticlesCount: TextView
    private lateinit var tvStatSavedCount: TextView
    private lateinit var tvStatCategoryValue: TextView
    private lateinit var switchNotifications: Switch
    private lateinit var switchAutoSave: Switch
    private lateinit var settingRowEditProfile: View
    private lateinit var settingRowShareApp: View

    private lateinit var dbHelper: DatabaseHelper

    // Stats cached for passing to ReadingStatsActivity
    private var cachedHistoryCount = 0
    private var cachedSavedCount = 0
    private var cachedTopCategory = "Technology"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())

        bindViews(view)
        setupProfileInfo()
        loadRealStats()
        setupSettingsRows()
    }

    private fun bindViews(view: View) {
        tvProfileName = view.findViewById(R.id.tvProfileName)
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail)
        tvStatArticlesCount = view.findViewById(R.id.tvStatArticlesCount)
        tvStatSavedCount = view.findViewById(R.id.tvStatSavedCount)
        tvStatCategoryValue = view.findViewById(R.id.tvStatCategoryValue)
        switchNotifications = view.findViewById(R.id.switchNotifications)
        switchAutoSave = view.findViewById(R.id.switchAutoSave)
        settingRowEditProfile = view.findViewById(R.id.settingRowEditProfile)
        settingRowShareApp = view.findViewById(R.id.settingRowShareApp)
    }

    private fun setupProfileInfo() {
        val userName = arguments?.getString("USER_NAME")
            ?: activity?.intent?.getStringExtra("USER_NAME")
            ?: "Reader"
        tvProfileName.text = userName
        tvProfileEmail.text = "${userName.lowercase()}@newsflow.com"
    }

    /**
     * F3 + F4: Reads real counts from SQLite on IO thread, updates UI on Main.
     * Completely offline — no API call.
     */
    private fun loadRealStats() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val savedCount = dbHelper.getSavedArticles().size
            val historyCount = dbHelper.getReadingHistory().size

            val topCategory = dbHelper.getSavedArticles()
                .groupBy { it.category }
                .maxByOrNull { it.value.size }
                ?.key ?: "Tech"

            withContext(Dispatchers.Main) {
                cachedHistoryCount = historyCount
                cachedSavedCount = savedCount
                cachedTopCategory = topCategory

                tvStatArticlesCount.text = historyCount.toString()
                tvStatSavedCount.text = savedCount.toString()
                tvStatCategoryValue.text = topCategory
            }
        }
    }

    private fun setupSettingsRows() {
        switchNotifications.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            val msg = if (isChecked) "Notifications enabled" else "Notifications disabled"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        switchAutoSave.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            val msg = if (isChecked) "Auto-save enabled" else "Auto-save disabled"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        settingRowEditProfile.setOnClickListener {
            // F4: Launch Compose Reading Stats screen
            val userName = arguments?.getString("USER_NAME")
                ?: activity?.intent?.getStringExtra("USER_NAME")
                ?: "User"
            val intent = Intent(requireContext(), ReadingStatsActivity::class.java).apply {
                putExtra("ARTICLES_READ", cachedHistoryCount)
                putExtra("SAVED_COUNT", cachedSavedCount)
                putExtra("TOP_CATEGORY", cachedTopCategory)
                putExtra("USER_NAME", userName)
            }
            startActivity(intent)
        }

        settingRowShareApp.setOnClickListener {
            // F1: Logout — sign out from Firebase and return to Login screen
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
    }
}
