package com.app.newsapp.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.app.newsapp.models.NewsArticle
import com.app.newsapp.models.ReadingHistory
import com.app.newsapp.models.SavedArticle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DatabaseHelper — SQLiteOpenHelper managing newsflow.db.
 *
 * F2 REQUIREMENT:
 *   - Table 1: saved_articles (AUTOINCREMENT PK)
 *   - Table 2: reading_history (AUTOINCREMENT PK + FOREIGN KEY → saved_articles.article_id)
 *   - onConfigure() enables PRAGMA foreign_keys=ON
 *
 * F3 REQUIREMENT:
 *   - Full CRUD: save, read all, read by ID, delete, isArticleSaved
 *   - Reading history: add, read all, delete one, clear all
 *
 * F5 REQUIREMENT:
 *   - searchSavedArticles() — rawQuery with LIKE %query% on title + source
 *   - getSavedArticlesSortedBy() — ORDER BY with dynamic column + direction
 *   - getHistoryForDateRange() — WHERE read_date BETWEEN startDate AND endDate
 */
class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        const val DB_NAME = "newsflow.db"
        const val DB_VERSION = 2

        // ── saved_articles ─────────────────────────────
        const val TABLE_SAVED = "saved_articles"
        const val COL_ID = "id"
        const val COL_ARTICLE_ID = "article_id"
        const val COL_TITLE = "title"
        const val COL_SOURCE_NAME = "source_name"
        const val COL_DESCRIPTION = "description"
        const val COL_URL = "url"
        const val COL_PUBLISHED_AT = "published_at"
        const val COL_CATEGORY = "category"
        const val COL_SAVED_DATE = "saved_date"

        // ── reading_history ────────────────────────────
        const val TABLE_HISTORY = "reading_history"
        const val COL_HIST_ID = "id"
        const val COL_HIST_ARTICLE_ID = "article_id"
        const val COL_HIST_READ_DATE = "read_date"
        const val COL_HIST_DURATION = "read_duration_seconds"

        // ── users ──────────────────────────────────────
        const val TABLE_USERS = "users"
    }

    // ── Date helper ────────────────────────────────────────────────────────────
    private fun nowIso(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

    private fun todayDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    /**
     * F2: Enable foreign-key enforcement before the database is used.
     */
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.execSQL("PRAGMA foreign_keys=ON")
    }

    /**
     * F2: Creates both tables with AUTOINCREMENT primary keys and FOREIGN KEY constraint.
     */
    override fun onCreate(db: SQLiteDatabase) {
        // Table 1 — saved_articles
        db.execSQL(
            """
            CREATE TABLE $TABLE_SAVED (
                $COL_ID           INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ARTICLE_ID   TEXT UNIQUE NOT NULL,
                $COL_TITLE        TEXT NOT NULL,
                $COL_SOURCE_NAME  TEXT,
                $COL_DESCRIPTION  TEXT,
                $COL_URL          TEXT,
                $COL_PUBLISHED_AT TEXT,
                $COL_CATEGORY     TEXT,
                $COL_SAVED_DATE   TEXT NOT NULL
            )
            """.trimIndent()
        )

        // Table 2 — reading_history with FOREIGN KEY
        db.execSQL(
            """
            CREATE TABLE $TABLE_HISTORY (
                $COL_HIST_ID          INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_HIST_ARTICLE_ID  TEXT NOT NULL,
                $COL_HIST_READ_DATE   TEXT NOT NULL,
                $COL_HIST_DURATION    INTEGER DEFAULT 0,
                FOREIGN KEY ($COL_HIST_ARTICLE_ID) REFERENCES $TABLE_SAVED($COL_ARTICLE_ID)
            )
            """.trimIndent()
        )

        // Table 3 — users (Authentication)
        db.execSQL(
            """
            CREATE TABLE $TABLE_USERS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                full_name TEXT NOT NULL,
                email TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL
            )
            """.trimIndent()
        )

        // Seed default user
        val defaultUser = ContentValues().apply {
            put("full_name", "Hamza")
            put("email", "hamza.naeem180@gmail.com")
            put("password", "hamza123")
        }
        db.insertWithOnConflict(TABLE_USERS, null, defaultUser, SQLiteDatabase.CONFLICT_IGNORE)
    }

    /**
     * F2: Drop and recreate tables on schema version bump.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SAVED")
        onCreate(db)
    }

    // ── CRUD: saved_articles ───────────────────────────────────────────────────

    /**
     * F3: Inserts a NewsArticle into saved_articles.
     * Uses the article's URL as the unique article_id.
     * Returns the new row ID, or -1 if insert failed (e.g. already saved).
     */
    fun saveArticle(article: NewsArticle, category: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ARTICLE_ID, article.url ?: return -1L)
            put(COL_TITLE, article.title ?: "")
            put(COL_SOURCE_NAME, article.source?.name ?: "")
            put(COL_DESCRIPTION, article.description ?: "")
            put(COL_URL, article.url ?: "")
            put(COL_PUBLISHED_AT, article.publishedAt ?: "")
            put(COL_CATEGORY, category)
            put(COL_SAVED_DATE, todayDate())
        }
        return db.insertWithOnConflict(TABLE_SAVED, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    /**
     * F3: Returns all saved articles ordered by saved_date descending (newest first).
     */
    fun getSavedArticles(): List<SavedArticle> {
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_SAVED, null, null, null, null, null, "$COL_SAVED_DATE DESC"
        )
        return cursor.use { mapCursorToSavedArticles(it) }
    }

    /**
     * F3: Looks up a single saved article by its article_id (URL).
     */
    fun getSavedArticleById(articleId: String): SavedArticle? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_SAVED, null,
            "$COL_ARTICLE_ID = ?", arrayOf(articleId),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) mapCursorToSavedArticles(it).firstOrNull() else null
        }
    }

    /**
     * F3: Deletes a saved article by article_id.
     * Bug Fix: Must delete child rows in reading_history first to avoid FOREIGN KEY constraint crash.
     * Returns number of rows deleted (1 = success, 0 = not found).
     */
    fun deleteSavedArticle(articleId: String): Int {
        val db = writableDatabase
        db.delete(TABLE_HISTORY, "$COL_HIST_ARTICLE_ID = ?", arrayOf(articleId))
        return db.delete(TABLE_SAVED, "$COL_ARTICLE_ID = ?", arrayOf(articleId))
    }

    /**
     * F3: Returns true if the given article_id exists in saved_articles.
     */
    fun isArticleSaved(articleId: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_SAVED, arrayOf(COL_ID),
            "$COL_ARTICLE_ID = ?", arrayOf(articleId),
            null, null, null
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    // ── CRUD: reading_history ──────────────────────────────────────────────────

    /**
     * F3: Inserts a reading history entry.
     * articleId is the URL string.
     * durationSeconds defaults to 0 — can be updated later.
     * Bug Fix: Wraps in try-catch because FOREIGN KEY requires the article to be saved first.
     */
    fun addToHistory(articleId: String, durationSeconds: Int = 0): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_HIST_ARTICLE_ID, articleId)
            put(COL_HIST_READ_DATE, nowIso())
            put(COL_HIST_DURATION, durationSeconds)
        }
        return try {
            db.insertOrThrow(TABLE_HISTORY, null, values)
        } catch (e: Exception) {
            // Foreign key constraint failure (article not in saved_articles)
            -1L
        }
    }

    /**
     * F3: Returns full reading history ordered by read_date descending.
     */
    fun getReadingHistory(): List<ReadingHistory> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HISTORY, null, null, null, null, null, "$COL_HIST_READ_DATE DESC"
        )
        return cursor.use { mapCursorToHistory(it) }
    }

    /**
     * F3: Deletes a single reading history entry by its primary key id.
     */
    fun deleteHistory(id: Int): Int {
        val db = writableDatabase
        return db.delete(TABLE_HISTORY, "$COL_HIST_ID = ?", arrayOf(id.toString()))
    }

    /**
     * F3: Deletes all reading history rows.
     */
    fun clearAllHistory(): Int {
        val db = writableDatabase
        return db.delete(TABLE_HISTORY, null, null)
    }

    // ── F5: Dynamic SQL Queries ────────────────────────────────────────────────

    /**
     * F5: LIKE query — searches title and source_name columns for a substring.
     * Pattern: WHERE title LIKE '%query%' OR source_name LIKE '%query%'
     */
    fun searchSavedArticles(query: String): List<SavedArticle> {
        val db = readableDatabase
        val pattern = "%$query%"
        val cursor = db.rawQuery(
            """
            SELECT * FROM $TABLE_SAVED
            WHERE $COL_TITLE LIKE ? OR $COL_SOURCE_NAME LIKE ?
            ORDER BY $COL_SAVED_DATE DESC
            """.trimIndent(),
            arrayOf(pattern, pattern)
        )
        return cursor.use { mapCursorToSavedArticles(it) }
    }

    /**
     * F5: ORDER BY query — sorts saved articles by any valid column name.
     * column: one of saved_date, title, source_name, category
     * ascending: true = ASC, false = DESC
     */
    fun getSavedArticlesSortedBy(column: String, ascending: Boolean): List<SavedArticle> {
        val db = readableDatabase
        // Whitelist valid columns to prevent SQL injection
        val safeColumn = when (column) {
            COL_TITLE, COL_SOURCE_NAME, COL_CATEGORY, COL_SAVED_DATE, COL_PUBLISHED_AT -> column
            else -> COL_SAVED_DATE
        }
        val direction = if (ascending) "ASC" else "DESC"
        val cursor = db.query(
            TABLE_SAVED, null, null, null, null, null, "$safeColumn $direction"
        )
        return cursor.use { mapCursorToSavedArticles(it) }
    }

    /**
     * F5: Date-range filter on reading_history.
     * startDate and endDate should be "yyyy-MM-dd" formatted strings.
     */
    fun getHistoryForDateRange(startDate: String, endDate: String): List<ReadingHistory> {
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT * FROM $TABLE_HISTORY
            WHERE $COL_HIST_READ_DATE BETWEEN ? AND ?
            ORDER BY $COL_HIST_READ_DATE DESC
            """.trimIndent(),
            arrayOf(startDate, endDate)
        )
        return cursor.use { mapCursorToHistory(it) }
    }

    // ── Cursor mappers ─────────────────────────────────────────────────────────

    private fun mapCursorToSavedArticles(cursor: Cursor): List<SavedArticle> {
        val list = mutableListOf<SavedArticle>()
        while (cursor.moveToNext()) {
            list.add(
                SavedArticle(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                    articleId = cursor.getString(cursor.getColumnIndexOrThrow(COL_ARTICLE_ID)),
                    title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)),
                    sourceName = cursor.getString(cursor.getColumnIndexOrThrow(COL_SOURCE_NAME)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)),
                    url = cursor.getString(cursor.getColumnIndexOrThrow(COL_URL)),
                    publishedAt = cursor.getString(cursor.getColumnIndexOrThrow(COL_PUBLISHED_AT)),
                    category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)),
                    savedDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_SAVED_DATE))
                )
            )
        }
        return list
    }

    private fun mapCursorToHistory(cursor: Cursor): List<ReadingHistory> {
        val list = mutableListOf<ReadingHistory>()
        while (cursor.moveToNext()) {
            list.add(
                ReadingHistory(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_HIST_ID)),
                    articleId = cursor.getString(cursor.getColumnIndexOrThrow(COL_HIST_ARTICLE_ID)),
                    readDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_HIST_READ_DATE)),
                    readDurationSeconds = cursor.getInt(cursor.getColumnIndexOrThrow(COL_HIST_DURATION))
                )
            )
        }
        return list
    }

    // ── Authentication (Users Table) ───────────────────────────────────────────

    fun getUserByCredentials(email: String, password: String): String? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf("full_name"),
            "email = ? AND password = ?",
            arrayOf(email.trim().lowercase(), password.trim()),
            null, null, null
        )
        return if (cursor.moveToFirst()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow("full_name"))
            cursor.close()
            name
        } else {
            cursor.close()
            null
        }
    }

    fun emailExists(email: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf("id"),
            "email = ?",
            arrayOf(email.trim().lowercase()),
            null, null, null
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }
}
