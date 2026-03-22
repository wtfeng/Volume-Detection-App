package com.example.volumedetection

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.preference.PreferenceManager
import java.util.*

object LanguageHelper {
    private const val PREF_LANGUAGE = "pref_language"
    private const val LANGUAGE_SYSTEM = "system"
    private const val LANGUAGE_CHINESE = "zh"
    private const val LANGUAGE_ENGLISH = "en"
    
    fun setLocale(context: Context, languageCode: String) {
        val locale = when (languageCode) {
            LANGUAGE_CHINESE -> Locale.CHINESE
            LANGUAGE_ENGLISH -> Locale.ENGLISH
            else -> getSystemLocale()
        }
        
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
        
        // 保存语言偏好
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(PREF_LANGUAGE, languageCode)
            .apply()
    }
    
    fun wrapContext(context: Context): Context {
        val language = getSavedLanguage(context)
        if (language == LANGUAGE_SYSTEM) {
            return context
        }
        
        val locale = when (language) {
            LANGUAGE_CHINESE -> Locale.CHINESE
            LANGUAGE_ENGLISH -> Locale.ENGLISH
            else -> getSystemLocale()
        }
        
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }
    
    fun getSavedLanguage(context: Context): String {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(PREF_LANGUAGE, LANGUAGE_SYSTEM) ?: LANGUAGE_SYSTEM
    }
    
    fun applySavedLanguage(context: Context) {
        val language = getSavedLanguage(context)
        if (language != LANGUAGE_SYSTEM) {
            setLocale(context, language)
        }
    }
    
    private fun getSystemLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Resources.getSystem().configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            Resources.getSystem().configuration.locale
        }
    }
    
    fun getCurrentLanguageDisplayName(context: Context): String {
        val language = getSavedLanguage(context)
        return when (language) {
            LANGUAGE_CHINESE -> context.getString(R.string.language_chinese)
            LANGUAGE_ENGLISH -> context.getString(R.string.language_english)
            else -> context.getString(R.string.language_system_default)
        }
    }
    
    fun getAvailableLanguages(): List<LanguageItem> {
        return listOf(
            LanguageItem(LANGUAGE_SYSTEM, R.string.language_system_default),
            LanguageItem(LANGUAGE_CHINESE, R.string.language_chinese),
            LanguageItem(LANGUAGE_ENGLISH, R.string.language_english)
        )
    }
    
    data class LanguageItem(val code: String, val nameResId: Int)
}