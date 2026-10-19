package com.example.ui

import com.example.ui.theme.*
import android.content.Context
import android.content.res.Configuration
import java.util.Locale

data class LanguageOption(
    val nativeName: String,
    val engName: String,
    val code: String,
    val flag: String
)

object LanguageManager {

    val supportedLanguages = listOf(
        LanguageOption("English", "English", "en", "🇬🇧"),
        LanguageOption("हिंदी", "Hindi", "hi", "🇮🇳"),
        LanguageOption("বাংলা", "Bengali", "bn", "🇮🇳"),
        LanguageOption("मराठी", "Marathi", "mr", "🇮🇳"),
        LanguageOption("ਪੰਜਾਬੀ", "Punjabi", "pa", "🇮🇳"),
        LanguageOption("Français", "French", "fr", "🇫🇷"),
        LanguageOption("中文", "Chinese", "zh", "🇨🇳"),
        LanguageOption("اردو", "Urdu", "ur", "🇵🇰"),
        LanguageOption("日本語", "Japanese", "ja", "🇯🇵"),
        LanguageOption("Español", "Spanish", "es", "🇪🇸"),
        LanguageOption("Deutsch", "German", "de", "🇩🇪")
    )

    fun getLanguageCode(language: String): String {
        return when (language.trim().lowercase()) {
            "hindi", "हिंदी", "hi" -> "hi"
            "bengali", "বাংলা", "bn" -> "bn"
            "marathi", "मराठी", "mr" -> "mr"
            "punjabi", "ਪੰਜਾਬੀ", "pa" -> "pa"
            "french", "français", "fr" -> "fr"
            "chinese", "中文", "zh" -> "zh"
            "urdu", "اردو", "ur" -> "ur"
            "japanese", "日本語", "ja" -> "ja"
            "spanish", "español", "es" -> "es"
            "german", "deutsch", "de" -> "de"
            else -> "en"
        }
    }

    fun applyAppLocale(context: Context, language: String) {
        val code = getLanguageCode(language)
        val locale = Locale(code)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    private val translations = mapOf(
        // Dock & Navigation
        "Dashboard" to mapOf(
            "en" to "Dashboard", "es" to "Inicio", "hi" to "डैशबोर्ड", "fr" to "Tableau de bord", "de" to "Übersicht", "ja" to "ダッシュボード"
        ),
        "Expenses" to mapOf(
            "en" to "Expenses", "es" to "Gastos", "hi" to "खर्च", "fr" to "Dépenses", "de" to "Ausgaben", "ja" to "支出"
        ),
        "Analytics" to mapOf(
            "en" to "Analytics", "es" to "Análisis", "hi" to "विश्लेषण", "fr" to "Analyses", "de" to "Analysen", "ja" to "分析"
        ),
        "Calendar" to mapOf(
            "en" to "Calendar", "es" to "Calendario", "hi" to "कैलेण्डर", "fr" to "Calendrier", "de" to "Kalender", "ja" to "カレンダー"
        ),
        "Settings" to mapOf(
            "en" to "Settings", "es" to "Ajustes", "hi" to "सेटिंग्स", "fr" to "Paramètres", "de" to "Einstellungen", "ja" to "設定"
        ),
        "Add" to mapOf(
            "en" to "Add", "es" to "Añadir", "hi" to "जोड़ें", "fr" to "Ajouter", "de" to "Hinzufügen", "ja" to "追加"
        ),

        // Dashboard & Overview Labels
        "Total Balance" to mapOf(
            "en" to "Total Balance", "es" to "Saldo Total", "hi" to "कुल शेष", "fr" to "Solde Total", "de" to "Gesamtsaldo", "ja" to "総残高"
        ),
        "Savings Goals" to mapOf(
            "en" to "Savings Goals", "es" to "Metas de Ahorro", "hi" to "बचत लक्ष्य", "fr" to "Objectifs d'épargne", "de" to "Sparziele", "ja" to "貯蓄 goal"
        ),
        "Create Goal" to mapOf(
            "en" to "Create Goal", "es" to "Crear Meta", "hi" to "लक्ष्य बनाएं", "fr" to "Créer un objectif", "de" to "Ziel erstellen", "ja" to "目標作成"
        ),
        "Add Savings Goal" to mapOf(
            "en" to "Add Savings Goal", "es" to "Añadir Meta de Ahorro", "hi" to "बचत लक्ष्य जोड़ें", "fr" to "Ajouter un objectif d'épargne", "de" to "Sparziel hinzufügen", "ja" to "貯蓄目標を追加"
        ),
        "Edit Savings Goal" to mapOf(
            "en" to "Edit Savings Goal", "es" to "Editar Meta de Ahorro", "hi" to "बचत लक्ष्य संपादित करें", "fr" to "Modifier l'objectif", "de" to "Sparziel bearbeiten", "ja" to "貯蓄目標を編集"
        ),
        "Deposit Money" to mapOf(
            "en" to "Deposit Money", "es" to "Depositar Dinero", "hi" to "पैसे जमा करें", "fr" to "Déposer de l'argent", "de" to "Geld einzahlen", "ja" to "入金する"
        ),
        "Total Income" to mapOf(
            "en" to "Total Income", "es" to "Ingresos Totales", "hi" to "कुल आय", "fr" to "Revenus Totaux", "de" to "Gesamteinnahmen", "ja" to "総収入"
        ),
        "Total Expenses" to mapOf(
            "en" to "Total Expenses", "es" to "Gastos Totales", "hi" to "कुल खर्च", "fr" to "Dépenses Totales", "de" to "Gesamtausgaben", "ja" to "総支出"
        ),
        "Budget Status" to mapOf(
            "en" to "Budget Status", "es" to "Estado del Presupuesto", "hi" to "बजट स्थिति", "fr" to "État du budget", "de" to "Budgetstatus", "ja" to "予算状況"
        ),
        "Adjust Budget" to mapOf(
            "en" to "Adjust Budget", "es" to "Ajustar Presupuesto", "hi" to "बजट समायोजित करें", "fr" to "Ajuster le budget", "de" to "Budget anpassen", "ja" to "予算調整"
        ),
        "Recent Activity" to mapOf(
            "en" to "Recent Activity", "es" to "Actividad Reciente", "hi" to "हाल की गतिविधि", "fr" to "Activité récente", "de" to "Letzte Aktivitäten", "ja" to "最近の履歴"
        ),
        "Quick Actions" to mapOf(
            "en" to "Quick Actions", "es" to "Acciones Rápidas", "hi" to "त्वरित कार्रवाई", "fr" to "Actions rapides", "de" to "Schnellaktionen", "ja" to "クイック操作"
        ),

        // Sidebar & Settings Drawer Labels
        "Settings & Preferences" to mapOf(
            "en" to "Settings & Preferences", "es" to "Ajustes y Preferencias", "hi" to "सेटिंग्स और प्राथमिकताएं", "fr" to "Paramètres & Préférences", "de" to "Einstellungen & Präferenzen", "ja" to "設定と設定項目"
        ),
        "App Configuration" to mapOf(
            "en" to "App Configuration", "es" to "Configuración de App", "hi" to "ऐप कॉन्फ़िगरेशन", "fr" to "Configuration de l'app", "de" to "App-Konfiguration", "ja" to "アプリ設定"
        ),
        "Data and Storage" to mapOf(
            "en" to "Data and Storage", "es" to "Datos y Almacenamiento", "hi" to "डेटा और स्टोरेज", "fr" to "Données et Stockage", "de" to "Daten & Speicher", "ja" to "データとストレージ"
        ),
        "Storage stats & clear cache" to mapOf(
            "en" to "Storage stats & clear cache", "es" to "Estadísticas de almacenamiento", "hi" to "स्टोरेज आँकड़े", "fr" to "Statistiques de stockage", "de" to "Speicherstatistiken", "ja" to "ストレージ統計"
        ),
        "Theme and Language" to mapOf(
            "en" to "Theme and Language", "es" to "Tema e Idioma", "hi" to "थीम और भाषा", "fr" to "Thème et Langue", "de" to "Design & Sprache", "ja" to "テーマと言語"
        ),
        "Dark theme & translations" to mapOf(
            "en" to "Dark theme & translations", "es" to "Modo oscuro y traducciones", "hi" to "डार्क मोड और अनुवाद", "fr" to "Mode sombre & traductions", "de" to "Dunkles Design & Übersetzungen", "ja" to "ダークテーマと翻訳"
        ),
        "Data Export & Reports" to mapOf(
            "en" to "Data Export & Reports", "es" to "Exportar Datos e Informes", "hi" to "डेटा निर्यात और रिपोर्ट", "fr" to "Exportation de données & Rapports", "de" to "Datenexport & Berichte", "ja" to "データ出力とレポート"
        ),
        "Export transactions to CSV or PDF" to mapOf(
            "en" to "Export transactions to CSV or PDF", "es" to "Exportar transacciones a CSV o PDF", "hi" to "लेन-देन को CSV या PDF में निर्यात करें", "fr" to "Exporter las transactions en CSV ou PDF", "de" to "Transaktionen als CSV oder PDF exportieren", "ja" to "取引履歴をCSVまたはPDFで出力"
        ),
        "FAQ and Help" to mapOf(
            "en" to "FAQ and Help", "es" to "Preguntas y Ayuda", "hi" to "प्रश्न और सहायता", "fr" to "FAQ et Aide", "de" to "FAQ & Hilfe", "ja" to "FAQとヘルプ"
        ),
        "FAQ & Help Support" to mapOf(
            "en" to "FAQ & Help Support", "es" to "Preguntas y Soporte de Ayuda", "hi" to "प्रश्न और सहायता समर्थन", "fr" to "FAQ & Support d'Aide", "de" to "FAQ & Hilfe-Support", "ja" to "FAQとサポート"
        ),
        "Guides, formulas & help docs" to mapOf(
            "en" to "Guides, formulas & help docs", "es" to "Guías, fórmulas y documentos", "hi" to "गाइड, सूत्र और सहायता दस्तावेज", "fr" to "Guides, formules & documents", "de" to "Anleitungen, Formeln & Hilfe", "ja" to "ガイドとヘルプドキュメント"
        ),
        "Personal Account" to mapOf(
            "en" to "Personal Account", "es" to "Cuenta Personal", "hi" to "व्यक्तिगत खाता", "fr" to "Compte personnel", "de" to "Persönliches Konto", "ja" to "個人アカウント"
        ),
        "Language Preference" to mapOf(
            "en" to "Language Preference", "es" to "Preferencia de Idioma", "hi" to "भाषा प्राथमिकता", "fr" to "Préférence de langue", "de" to "Sprachauswahl", "ja" to "言語設定"
        ),
        "Appearance & Dark Mode" to mapOf(
            "en" to "Appearance & Dark Mode", "es" to "Apariencia y Modo Oscuro", "hi" to "दिखावट और डार्क मोड", "fr" to "Apparence et Mode Sombre", "de" to "Erscheinungsbild & Dunkelmodus", "ja" to "外観とダークモード"
        ),
        "Dark Theme Mode" to mapOf(
            "en" to "Dark Theme Mode", "es" to "Modo de Tema Oscuro", "hi" to "डार्क थीम मोड", "fr" to "Mode Thème Sombre", "de" to "Dunkler Designmodus", "ja" to "ダークテーマモード"
        ),
        "Color Palette Presets" to mapOf(
            "en" to "Color Palette Presets", "es" to "Paletas de Colores", "hi" to "रंग पैलेट प्रीसेट", "fr" to "Palettes de couleurs", "de" to "Farbpaletten-Presets", "ja" to "カラーパレット"
        ),
        "Disk and network usage" to mapOf(
            "en" to "Disk and network usage", "es" to "Uso de disco y red", "hi" to "डिस्क और नेटवर्क उपयोग", "fr" to "Utilisation du disque et du réseau", "de" to "Speicher- und Netzwerknutzung", "ja" to "ディスクとネットワークの使用量"
        ),
        "Storage Usage" to mapOf(
            "en" to "Storage Usage", "es" to "Uso de Almacenamiento", "hi" to "स्टोरेज उपयोग", "fr" to "Utilisation du stockage", "de" to "Speichernutzung", "ja" to "ストレージ使用量"
        ),
        "Data Usage" to mapOf(
            "en" to "Data Usage", "es" to "Uso de Datos", "hi" to "डेटा उपयोग", "fr" to "Utilisation des données", "de" to "Datennutzung", "ja" to "データ使用量"
        ),
        "Local Offline Storage" to mapOf(
            "en" to "Local Offline Storage", "es" to "Almacenamiento Local Offline", "hi" to "लोकल ऑफलाइन स्टोरेज", "fr" to "Stockage local hors ligne", "de" to "Lokaler Offline-Speicher", "ja" to "ローカルオフラインストレージ"
        ),
        "Refresh Disk Usage Stats" to mapOf(
            "en" to "Refresh Disk Usage Stats", "es" to "Actualizar Estadísticas de Disco", "hi" to "डिस्क उपयोग आँकड़े ताज़ा करें", "fr" to "Actualiser les statistiques du disque", "de" to "Speicherstatistiken aktualisieren", "ja" to "ディスク使用量の再読み込み"
        ),
        "Export Statements" to mapOf(
            "en" to "Export Statements", "es" to "Exportar Estados de Cuenta", "hi" to "विवरण निर्यात करें", "fr" to "Exporter les relevés", "de" to "Auszüge exportieren", "ja" to "明細の出力"
        ),
        "Export analytics, stats, CSV & PDF reports" to mapOf(
            "en" to "Export analytics, stats, CSV & PDF reports", "es" to "Exportar análisis, estadísticas, informes CSV y PDF", "hi" to "विश्लेषण, आंकड़े, सीएसवी और पीडीएफ रिपोर्ट निर्यात करें", "fr" to "Exporter analyses, statistiques, rapports CSV et PDF", "de" to "Analysen, Statistiken, CSV- und PDF-Berichte exportieren", "ja" to "分析、統計、CSVおよびPDFレポートの出力"
        ),
        "Date Range Filter" to mapOf(
            "en" to "Date Range Filter", "es" to "Filtro de Rango de Fechas", "hi" to "तिथि सीमा फ़िल्टर", "fr" to "Filtre de plage de dates", "de" to "Datumsbereichsfilter", "ja" to "日付範囲フィルター"
        ),
        "Change Range" to mapOf(
            "en" to "Change Range", "es" to "Cambiar Rango", "hi" to "सीमा बदलें", "fr" to "Changer la plage", "de" to "Bereich ändern", "ja" to "範囲変更"
        ),
        "Transaction Type" to mapOf(
            "en" to "Transaction Type", "es" to "Tipo de Transacción", "hi" to "लेन-देन का प्रकार", "fr" to "Type de transaction", "de" to "Transaktionstyp", "ja" to "取引タイプ"
        ),
        "Full Statement" to mapOf(
            "en" to "Full Statement", "es" to "Estado Completo", "hi" to "पूरा विवरण", "fr" to "Relevé complet", "de" to "Vollständiger Auszug", "ja" to "全明細"
        ),
        "Income Only" to mapOf(
            "en" to "Income Only", "es" to "Solo Ingresos", "hi" to "केवल आय", "fr" to "Revenus seulement", "de" to "Nur Einnahmen", "ja" to "収入のみ"
        ),
        "Expense Only" to mapOf(
            "en" to "Expense Only", "es" to "Solo Gastos", "hi" to "केवल खर्च", "fr" to "Dépenses seulement", "de" to "Nur Ausgaben", "ja" to "支出のみ"
        ),
        "Search transactions..." to mapOf(
            "en" to "Search transactions...", "es" to "Buscar transacciones...", "hi" to "लेन-देन खोजें...", "fr" to "Rechercher des transactions...", "de" to "Transaktionen suchen...", "ja" to "取引を検索..."
        ),
        "Filter" to mapOf(
            "en" to "Filter", "es" to "Filtrar", "hi" to "फ़िल्टर", "fr" to "Filtrer", "de" to "Filtern", "ja" to "フィルター"
        ),
        "Date Range" to mapOf(
            "en" to "Date Range", "es" to "Rango de Fechas", "hi" to "तिथि सीमा", "fr" to "Plage de dates", "de" to "Datumsbereich", "ja" to "日付範囲"
        ),
        "Export Formatted PDF Report" to mapOf(
            "en" to "Export Formatted PDF Report", "es" to "Exportar Informe PDF", "hi" to "PDF रिपोर्ट निर्यात करें", "fr" to "Exporter Rapport PDF", "de" to "PDF-Bericht exportieren", "ja" to "PDFレポート出力"
        ),
        "Export CSV File" to mapOf(
            "en" to "Export CSV File", "es" to "Exportar Archivo CSV", "hi" to "CSV फ़ाइल निर्यात करें", "fr" to "Exporter Fichier CSV", "de" to "CSV-Datei exportieren", "ja" to "CSV出力"
        ),
        "Currency & Rates" to mapOf(
            "en" to "Currency & Rates", "hi" to "मुद्रा और दरें", "bn" to "মুদ্রা ও বিনিময় হার", "mr" to "चलन आणि दर", "pa" to "ਮੁਦਰਾ ਅਤੇ ਦਰਾਂ", "fr" to "Devises & Taux", "zh" to "货币与汇率", "ur" to "کرنسی اور شرح مبادلہ", "ja" to "通貨と為替レート"
        ),
        "Transaction Preferences" to mapOf(
            "en" to "Transaction Preferences", "hi" to "लेन-देन प्राथमिकताएं", "bn" to "লেনদেন পছন্দসমূহ", "mr" to "व्यवहार प्राधान्ये", "pa" to "ਲੈਣ-ਦੇਣ ਤਰਜੀਹਾਂ", "fr" to "Préférences de transaction", "zh" to "交易偏好设置", "ur" to "لین دین کی ترجیحات", "ja" to "取引設定"
        ),
        "Bills & Reminders" to mapOf(
            "en" to "Bills & Reminders", "hi" to "बिल और अनुस्मारक", "bn" to "বিল ও অনুস্মারক", "mr" to "बिले आणि स्मरणपत्रे", "pa" to "ਬਿੱਲ ਅਤੇ ਰੀਮਾਈਂਡਰ", "fr" to "Factures & Rappels", "zh" to "账单与提醒", "ur" to "بل اور یاد دہانیاں", "ja" to "請求書とリマインダー"
        ),
        "Budgets" to mapOf(
            "en" to "Budgets", "hi" to "बजट", "bn" to "বাজেট", "mr" to "अंदाजपत्रक", "pa" to "ਬਜਟ", "fr" to "Budgets", "zh" to "预算", "ur" to "بجٹ", "ja" to "予算"
        ),
        "Financial Calculators" to mapOf(
            "en" to "Financial Calculators", "hi" to "वित्तीय कैलकुलेटर", "bn" to "আর্থিক ক্যালকুলেটর", "mr" to "आर्थिक गणकयंत्र", "pa" to "ਵਿੱਤੀ ਕੈਲਕੁਲੇਟਰ", "fr" to "Calculateurs financiers", "zh" to "财务计算器", "ur" to "مالیاتی کیلکولیٹر", "ja" to "財務計算機"
        ),
        "Password & Security" to mapOf(
            "en" to "Password & Security", "hi" to "पासवर्ड और सुरक्षा", "bn" to "পাসওয়ার্ড ও নিরাপত্তা", "mr" to "पासवर्ड आणि सुरक्षा", "pa" to "ਪਾਸਵਰਡ ਅਤੇ ਸੁਰੱਖਿਆ", "fr" to "Mot de passe & Sécurité", "zh" to "密码与安全", "ur" to "پاس ورڈ اور سیکیورٹی", "ja" to "パスワードとセキュリティ"
        ),
        "Privacy Settings" to mapOf(
            "en" to "Privacy Settings", "hi" to "गोपनीयता सेटिंग्स", "bn" to "গোপনীয়তা সেটিংস", "mr" to "गोपनीयता सेटिंग्ज", "pa" to "ਗੋਪਨੀਯਤਾ ਸੈਟਿੰਗਾਂ", "fr" to "Paramètres de confidentialité", "zh" to "隐私设置", "ur" to "رازداری کی ترتیبات", "ja" to "プライバシー設定"
        ),
        "What's New & About" to mapOf(
            "en" to "What's New & About", "hi" to "नया क्या है और परिचय", "bn" to "নতুন কি এবং বিবরণ", "mr" to "नवीन काय आणि परिचय", "pa" to "ਨਵਾਂ ਕੀ ਹੈ ਅਤੇ ਬਾਰੇ", "fr" to "Nouveautés & À propos", "zh" to "新功能与关于", "ur" to "نیا کیا ہے اور تعارف", "ja" to "最新情報とアプリ情報"
        ),
        "No matching settings" to mapOf(
            "en" to "No matching settings", "hi" to "कोई मिलती-जुलती सेटिंग नहीं", "bn" to "কোনো মিলযুক্ত সেটিংস নেই", "mr" to "कोणतीही जुळणारी सेटिंग नाही", "pa" to "ਕੋਈ ਮੇਲ ਖਾਂਦੀ ਸੈਟਿੰਗ ਨਹੀਂ", "fr" to "Aucun paramètre correspondant", "zh" to "没有匹配的设置", "ur" to "کوئی مماثل سیٹنگ نہیں", "ja" to "一致する設定がありません"
        ),
        "Try searching for a different keyword or category" to mapOf(
            "en" to "Try searching for a different keyword or category", "hi" to "किसी अन्य कीवर्ड या श्रेणी से खोजें", "fr" to "Essayez de rechercher un autre mot-clé", "zh" to "尝试搜索其他关键词或分类", "ja" to "別のキーワードまたはカテゴリで検索してください"
        ),
        "Day Streak" to mapOf(
            "en" to "Day Streak", "hi" to "दिन की स्ट्रीक", "bn" to "দিনের স্ট্রিক", "mr" to "दिवसांची स्ट्रीक", "pa" to "ਦਿਨ ਦੀ ਲੜੀ", "fr" to "Jours consécutifs", "zh" to "天连续打卡", "ur" to "دن کا تسلسل", "ja" to "日連続記録"
        ),
        "100% Offline" to mapOf(
            "en" to "100% Offline", "hi" to "100% ऑफ़लाइन", "bn" to "১০০% অফলাইন", "mr" to "१००% ऑफलाइन", "pa" to "100% ਔਫਲਾਈਨ", "fr" to "100% hors ligne", "zh" to "100% 离线", "ur" to "100% آف لائن", "ja" to "100% オフライン"
        ),
        "Stable & Secure" to mapOf(
            "en" to "Stable & Secure", "hi" to "स्थिर और सुरक्षित", "bn" to "স্থিতিশীল ও নিরাপদ", "mr" to "स्थिर आणि सुरक्षित", "pa" to "ਸਥਿਰ ਅਤੇ ਸੁਰੱਖਿਅਤ", "fr" to "Stable et sécurisé", "zh" to "稳定与安全", "ur" to "مستحکم اور محفوظ", "ja" to "安定・安全"
        ),
        "User" to mapOf(
            "en" to "User", "hi" to "उपयोगकर्ता", "bn" to "ব্যবহারকারী", "mr" to "वापरकर्ता", "pa" to "ਵਰਤੋਂਕਾਰ", "fr" to "Utilisateur", "zh" to "用户", "ur" to "صارف", "ja" to "ユーザー"
        )
    )

    fun tr(key: String, language: String): String {
        val code = getLanguageCode(language)
        val keyTranslations = translations[key] ?: return key
        return keyTranslations[code] ?: keyTranslations["en"] ?: key
    }
}
