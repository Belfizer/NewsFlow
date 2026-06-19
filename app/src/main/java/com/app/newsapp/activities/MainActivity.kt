package com.app.newsapp.activities

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.app.newsapp.R
import com.app.newsapp.fragments.HomeFragment
import com.app.newsapp.fragments.ProfileFragment
import com.app.newsapp.fragments.SavedFragment
import com.app.newsapp.fragments.SearchFragment

/**
 * MainActivity — pure container shell.
 *
 * F1 REQUIREMENT: Receives USER_NAME from LoginActivity via Intent Extra.
 * F4 REQUIREMENT: Manages fragment switching using supportFragmentManager.beginTransaction().replace()
 *                 — no activity restart, no Jetpack Navigation Component.
 *
 * Bug Fix 1: Search tab now loads SearchFragment; Profile tab loads ProfileFragment.
 */
class MainActivity : AppCompatActivity() {

    private val accentRed = Color.parseColor("#E63946")
    private val inactiveGrey = Color.parseColor("#606060")

    // Bottom nav tab containers
    private lateinit var tabHome: LinearLayout
    private lateinit var tabSearch: LinearLayout
    private lateinit var tabSavedWrapper: android.widget.FrameLayout
    private lateinit var tabProfile: LinearLayout

    // Tab icons
    private lateinit var ivTabHome: ImageView
    private lateinit var ivTabSearch: ImageView
    private lateinit var ivTabSaved: ImageView
    private lateinit var ivTabProfile: ImageView

    // Tab labels
    private lateinit var tvTabHome: TextView
    private lateinit var tvTabSearch: TextView
    private lateinit var tvTabSaved: TextView
    private lateinit var tvTabProfile: TextView

    private var userName: String = "Reader"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        userName = intent.getStringExtra("USER_NAME") ?: "Reader"

        bindBottomNavViews()
        setupBottomNavListeners()

        if (savedInstanceState == null) {
            loadHomeFragment()
        }

        // F3: Request notification permission on Android 13+
        requestNotificationPermission()
    }

    /** F3: Runtime notification permission request for Android 13+ (API 33+). */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    private fun bindBottomNavViews() {
        val bottomNav = findViewById<android.view.View>(R.id.bottomNavInclude)

        tabHome = bottomNav.findViewById(R.id.tabHome)
        tabSearch = bottomNav.findViewById(R.id.tabSearch)
        tabSavedWrapper = bottomNav.findViewById(R.id.tabSavedWrapper)
        tabProfile = bottomNav.findViewById(R.id.tabProfile)

        ivTabHome = bottomNav.findViewById(R.id.ivTabHome)
        ivTabSearch = bottomNav.findViewById(R.id.ivTabSearch)
        ivTabSaved = bottomNav.findViewById(R.id.ivTabSaved)
        ivTabProfile = bottomNav.findViewById(R.id.ivTabProfile)

        tvTabHome = bottomNav.findViewById(R.id.tvTabHome)
        tvTabSearch = bottomNav.findViewById(R.id.tvTabSearch)
        tvTabSaved = bottomNav.findViewById(R.id.tvTabSaved)
        tvTabProfile = bottomNav.findViewById(R.id.tvTabProfile)
    }

    private fun setupBottomNavListeners() {
        tabHome.setOnClickListener {
            loadHomeFragment()
            setActiveTab(0)
        }

        // Bug Fix 1: Search tab loads dedicated SearchFragment
        tabSearch.setOnClickListener {
            switchFragment(SearchFragment())
            setActiveTab(1)
        }

        tabSavedWrapper.setOnClickListener {
            switchFragment(SavedFragment())
            setActiveTab(2)
        }

        // Bug Fix 1: Profile tab correctly loads ProfileFragment
        tabProfile.setOnClickListener {
            val profileFragment = ProfileFragment()
            val bundle = Bundle()
            bundle.putString("USER_NAME", userName)
            profileFragment.arguments = bundle
            switchFragment(profileFragment)
            setActiveTab(3)
        }
    }

    private fun loadHomeFragment() {
        val homeFragment = HomeFragment()
        val bundle = Bundle()
        bundle.putString("USER_NAME", userName)
        homeFragment.arguments = bundle
        switchFragment(homeFragment)
        setActiveTab(0)
    }

    fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun setActiveTab(index: Int) {
        listOf(ivTabHome, ivTabSearch, ivTabSaved, ivTabProfile).forEach {
            it.setColorFilter(inactiveGrey)
        }
        listOf(tvTabHome, tvTabSearch, tvTabSaved, tvTabProfile).forEach {
            it.setTextColor(inactiveGrey)
        }

        when (index) {
            0 -> { ivTabHome.setColorFilter(accentRed); tvTabHome.setTextColor(accentRed) }
            1 -> { ivTabSearch.setColorFilter(accentRed); tvTabSearch.setTextColor(accentRed) }
            2 -> { ivTabSaved.setColorFilter(accentRed); tvTabSaved.setTextColor(accentRed) }
            3 -> { ivTabProfile.setColorFilter(accentRed); tvTabProfile.setTextColor(accentRed) }
        }
    }
}
