package com.aiselp.autox.apkbuilder

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.getStringOrNull

class SignDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, 1) {
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(
            """
        CREATE TABLE $DB_TABLE (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            path TEXT,
            name TEXT,
            keyStorePassword TEXT,
            alias TEXT,
            password TEXT
        )
"""
        );
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

    }

    fun insertData(apkKeyStore: ApkKeyStore) {
        writableDatabase.use {
            it.insert(DB_TABLE, null, apkKeyStore.toContentValues())
        }
    }

    private fun transformToApkKeyStore(cursor: Cursor): ApkKeyStore {
        return ApkKeyStore(
            path = cursor.getStringOrNull(1),
            name = cursor.getStringOrNull(2),
            keyStorePassword = cursor.getStringOrNull(3),
            alias = cursor.getStringOrNull(4),
            password = cursor.getStringOrNull(5)
        )
    }

    private fun ApkKeyStore.toContentValues(): ContentValues {
        return ContentValues().also {
            it.put("path", path)
            it.put("name", name)
            it.put("keyStorePassword", keyStorePassword)
            it.put("alias", alias)
            it.put("password", password)
        }
    }

    fun updateData(apkKeyStore: ApkKeyStore) {
        writableDatabase.use {
            it.update(
                DB_TABLE,
                apkKeyStore.toContentValues(),
                "path = ?",
                arrayOf(apkKeyStore.path)
            )
        }
    }
    fun deleteData(apkKeyStore: ApkKeyStore) {
        writableDatabase.use {
            it.delete(DB_TABLE, "path = ?", arrayOf(apkKeyStore.path))
        }
    }

    fun queryPath(path: String): ApkKeyStore? {
        readableDatabase.use {
            val cursor = it.query(DB_TABLE, null, "path = ?", arrayOf(path), null, null, null)
            if (cursor != null && cursor.count > 0) {
                cursor.moveToFirst()
                return transformToApkKeyStore(cursor)
            } else return null
        }
    }

    fun queryAllData(): List<ApkKeyStore> {
        readableDatabase.use {
            val cursor = it.query(DB_TABLE, null, null, null, null, null, null)
            if (cursor != null) {
                val apkKeyStores = mutableListOf<ApkKeyStore>()
                cursor.use {
                    while (cursor.moveToNext()) {
                        apkKeyStores.add(transformToApkKeyStore(cursor))
                    }
                }
                return apkKeyStores
            } else return emptyList()
        }
    }

    companion object {
        private const val DB_NAME = "sign.db"
        private const val DB_TABLE = "apk_key_store"
    }
}