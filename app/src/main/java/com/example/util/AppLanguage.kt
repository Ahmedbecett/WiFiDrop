package com.example.util

enum class Language(val code: String, val displayName: String, val flag: String, val isRtl: Boolean) {
    ARABIC("ar", "العربية", "🇩🇿", true),
    ENGLISH("en", "English", "🇬🇧", false),
    FRENCH("fr", "Français", "🇫🇷", false),
    SPANISH("es", "Español", "🇪🇸", false)
}

object AppLanguage {
    var currentLanguage: Language = Language.ARABIC

    private val translations = mapOf(
        "app_name" to mapOf(
            Language.ARABIC to "WiFiDrop",
            Language.ENGLISH to "WiFiDrop",
            Language.FRENCH to "WiFiDrop",
            Language.SPANISH to "WiFiDrop"
        ),
        "app_tagline" to mapOf(
            Language.ARABIC to "نقل سريع وآمن للملفات عبر شبكة Wi-Fi المحلية",
            Language.ENGLISH to "Fast & Local Wi-Fi File Transfer",
            Language.FRENCH to "Transfert de fichiers Wi-Fi local et rapide",
            Language.SPANISH to "Transferencia rápida de archivos por Wi-Fi local"
        ),
        "nav_home" to mapOf(
            Language.ARABIC to "النقل",
            Language.ENGLISH to "Transfer",
            Language.FRENCH to "Transfert",
            Language.SPANISH to "Transferir"
        ),
        "nav_files" to mapOf(
            Language.ARABIC to "الملفات",
            Language.ENGLISH to "Files",
            Language.FRENCH to "Fichiers",
            Language.SPANISH to "Archivos"
        ),
        "nav_history" to mapOf(
            Language.ARABIC to "السجل",
            Language.ENGLISH to "History",
            Language.FRENCH to "Historique",
            Language.SPANISH to "Historial"
        ),
        "nav_storage" to mapOf(
            Language.ARABIC to "التخزين",
            Language.ENGLISH to "Storage",
            Language.FRENCH to "Stockage",
            Language.SPANISH to "Almacenamiento"
        ),
        "nav_settings" to mapOf(
            Language.ARABIC to "الإعدادات",
            Language.ENGLISH to "Settings",
            Language.FRENCH to "Paramètres",
            Language.SPANISH to "Ajustes"
        ),
        "action_send" to mapOf(
            Language.ARABIC to "إرسال ملفات",
            Language.ENGLISH to "Send Files",
            Language.FRENCH to "Envoyer",
            Language.SPANISH to "Enviar"
        ),
        "action_send_desc" to mapOf(
            Language.ARABIC to "إرسال صور وفيديوهات ومستندات لجهاز آخر",
            Language.ENGLISH to "Send photos, videos, & documents to another device",
            Language.FRENCH to "Envoyer photos, vidéos et documents",
            Language.SPANISH to "Enviar fotos, videos y documentos a otro dispositivo"
        ),
        "action_receive" to mapOf(
            Language.ARABIC to "استقبال ملفات",
            Language.ENGLISH to "Receive Files",
            Language.FRENCH to "Recevoir",
            Language.SPANISH to "Recibir"
        ),
        "action_receive_desc" to mapOf(
            Language.ARABIC to "تحويل الهاتف إلى مستقبل للملفات من الكمبيوتر أو الهاتف",
            Language.ENGLISH to "Turn phone into a receiver for PC or other phones",
            Language.FRENCH to "Transformer le téléphone en récepteur",
            Language.SPANISH to "Convertir el teléfono en receptor de archivos"
        ),
        "action_connect" to mapOf(
            Language.ARABIC to "ربط جهاز",
            Language.ENGLISH to "Connect Device",
            Language.FRENCH to "Connecter appareil",
            Language.SPANISH to "Conectar dispositivo"
        ),
        "action_connect_desc" to mapOf(
            Language.ARABIC to "ربط عبر QR Code أو العنوان المحلي أو رمز PIN",
            Language.ENGLISH to "Pair via QR Code, Local IP, or 6-digit PIN",
            Language.FRENCH to "Associer via QR Code, IP Locale ou PIN",
            Language.SPANISH to "Emparejar vía código QR, IP local o PIN"
        ),
        "server_status_active" to mapOf(
            Language.ARABIC to "جهاز الاستقبال متصل وجاهز",
            Language.ENGLISH to "Receiver is Active & Ready",
            Language.FRENCH to "Le récepteur est actif et prêt",
            Language.SPANISH to "Receptor activo y listo"
        ),
        "server_status_inactive" to mapOf(
            Language.ARABIC to "جهاز الاستقبال متوقف",
            Language.ENGLISH to "Receiver is Inactive",
            Language.FRENCH to "Le récepteur est inactif",
            Language.SPANISH to "Receptor inactivo"
        ),
        "start_receiver" to mapOf(
            Language.ARABIC to "تشغيل الاستقبال",
            Language.ENGLISH to "Start Receiver",
            Language.FRENCH to "Démarrer récepteur",
            Language.SPANISH to "Iniciar receptor"
        ),
        "stop_receiver" to mapOf(
            Language.ARABIC to "إيقاف الاستقبال",
            Language.ENGLISH to "Stop Receiver",
            Language.FRENCH to "Arrêter récepteur",
            Language.SPANISH to "Detener receptor"
        ),
        "web_address" to mapOf(
            Language.ARABIC to "عنوان المتصفح (للكمبيوتر)",
            Language.ENGLISH to "Web Browser Address (PC/Mac)",
            Language.FRENCH to "Adresse navigateur Web (PC/Mac)",
            Language.SPANISH to "Dirección web en navegador (PC/Mac)"
        ),
        "open_in_browser_hint" to mapOf(
            Language.ARABIC to "افتح هذا الرابط في Chrome أو Edge أو Firefox على جهاز الكمبيوتر",
            Language.ENGLISH to "Open this link in Chrome, Edge, Safari, or Firefox on your PC",
            Language.FRENCH to "Ouvrez ce lien dans Chrome, Edge ou Firefox sur votre PC",
            Language.SPANISH to "Abre este enlace en Chrome, Edge o Firefox en tu PC"
        ),
        "security_pin" to mapOf(
            Language.ARABIC to "رمز الأمان PIN",
            Language.ENGLISH to "Security PIN Code",
            Language.FRENCH to "Code PIN de sécurité",
            Language.SPANISH to "Código PIN de seguridad"
        ),
        "pin_required_hint" to mapOf(
            Language.ARABIC to "أدخل هذا الرمز في المتصفح للسماح بالاتصال",
            Language.ENGLISH to "Enter this PIN in the browser to authorize connection",
            Language.FRENCH to "Entrez ce PIN sur le navigateur pour autoriser",
            Language.SPANISH to "Ingresa este PIN en el navegador para conectar"
        ),
        "connected_devices" to mapOf(
            Language.ARABIC to "الأجهزة المتصلة",
            Language.ENGLISH to "Connected Devices",
            Language.FRENCH to "Appareils connectés",
            Language.SPANISH to "Dispositivos conectados"
        ),
        "no_connected_devices" to mapOf(
            Language.ARABIC to "لا توجد أجهزة متصلة حالياً",
            Language.ENGLISH to "No devices connected yet",
            Language.FRENCH to "Aucun appareil connecté",
            Language.SPANISH to "Ningún dispositivo conectado"
        ),
        "nearby_devices" to mapOf(
            Language.ARABIC to "الأجهزة القريبة المكتشفة",
            Language.ENGLISH to "Nearby Discovered Devices",
            Language.FRENCH to "Appareils découverts à proximité",
            Language.SPANISH to "Dispositivos cercanos descubiertos"
        ),
        "searching_devices" to mapOf(
            Language.ARABIC to "جارٍ البحث عن أجهزة على نفس الشبكة...",
            Language.ENGLISH to "Searching for devices on same Wi-Fi...",
            Language.FRENCH to "Recherche d'appareils sur le même Wi-Fi...",
            Language.SPANISH to "Buscando dispositivos en la misma red Wi-Fi..."
        ),
        "tap_to_connect" to mapOf(
            Language.ARABIC to "انقر للإرسال لهذا الجهاز",
            Language.ENGLISH to "Tap to send to this device",
            Language.FRENCH to "Appuyez pour envoyer",
            Language.SPANISH to "Toca para enviar"
        ),
        "select_files" to mapOf(
            Language.ARABIC to "تحديد ملفات",
            Language.ENGLISH to "Select Files",
            Language.FRENCH to "Sélectionner fichiers",
            Language.SPANISH to "Seleccionar archivos"
        ),
        "total_selected" to mapOf(
            Language.ARABIC to "إجمالي الملفات المحددة",
            Language.ENGLISH to "Total Selected Files",
            Language.FRENCH to "Fichiers sélectionnés",
            Language.SPANISH to "Archivos seleccionados"
        ),
        "start_transfer" to mapOf(
            Language.ARABIC to "🚀 بدء النقل",
            Language.ENGLISH to "🚀 Start Transfer",
            Language.FRENCH to "🚀 Démarrer le transfert",
            Language.SPANISH to "🚀 Iniciar transferencia"
        ),
        "cancel_transfer" to mapOf(
            Language.ARABIC to "إلغاء النقل",
            Language.ENGLISH to "Cancel Transfer",
            Language.FRENCH to "Annuler transfert",
            Language.SPANISH to "Cancelar transferencia"
        ),
        "speed" to mapOf(
            Language.ARABIC to "السرعة",
            Language.ENGLISH to "Speed",
            Language.FRENCH to "Vitesse",
            Language.SPANISH to "Velocidad"
        ),
        "remaining" to mapOf(
            Language.ARABIC to "الوقت المتبقي",
            Language.ENGLISH to "Time Remaining",
            Language.FRENCH to "Temps restant",
            Language.SPANISH to "Tiempo restante"
        ),
        "hotspot_mode" to mapOf(
            Language.ARABIC to "وضع نقطة الاتصال (Hotspot)",
            Language.ENGLISH to "Hotspot Mode",
            Language.FRENCH to "Mode Point d'accès",
            Language.SPANISH to "Modo Zona Wi-Fi"
        ),
        "hotspot_desc" to mapOf(
            Language.ARABIC to "استخدم Hotspot لنقل الملفات عندما لا توجد شبكة Wi-Fi مشتركة بدون إنترنت",
            Language.ENGLISH to "Use personal hotspot to transfer files offline when no Wi-Fi router is available",
            Language.FRENCH to "Utilisez le point d'accès pour transférer sans routeur",
            Language.SPANISH to "Usa la zona Wi-Fi para transferir sin conexión a internet"
        ),
        "open_hotspot_settings" to mapOf(
            Language.ARABIC to "فتح إعدادات Hotspot",
            Language.ENGLISH to "Open Hotspot Settings",
            Language.FRENCH to "Ouvrir paramètres point d'accès",
            Language.SPANISH to "Abrir ajustes de Zona Wi-Fi"
        ),
        "clean_cache" to mapOf(
            Language.ARABIC to "تنظيف الذاكرة المؤقتة",
            Language.ENGLISH to "Clear Transfer Cache",
            Language.FRENCH to "Vider le cache",
            Language.SPANISH to "Limpiar caché de transferencia"
        ),
        "cache_cleaned" to mapOf(
            Language.ARABIC to "تم تنظيف الملفات المؤقتة بنجاح",
            Language.ENGLISH to "Cache cleared successfully!",
            Language.FRENCH to "Cache nettoyé avec succès!",
            Language.SPANISH to "¡Caché limpiada con éxito!"
        ),
        "privacy_policy" to mapOf(
            Language.ARABIC to "سياسة الخصوصية",
            Language.ENGLISH to "Privacy Policy",
            Language.FRENCH to "Politique de confidentialité",
            Language.SPANISH to "Política de privacidad"
        ),
        "contact_support" to mapOf(
            Language.ARABIC to "الدعم والتواصل",
            Language.ENGLISH to "Support & Contact",
            Language.FRENCH to "Support et contact",
            Language.SPANISH to "Soporte y contacto"
        ),
        "about" to mapOf(
            Language.ARABIC to "حول التطبيق",
            Language.ENGLISH to "About WiFiDrop",
            Language.FRENCH to "À propos",
            Language.SPANISH to "Acerca de"
        ),
        "auto_accept_files" to mapOf(
            Language.ARABIC to "قبول الملفات تلقائياً",
            Language.ENGLISH to "Auto-accept Incoming Files",
            Language.FRENCH to "Accepter fichiers automatiquement",
            Language.SPANISH to "Aceptar archivos automáticamente"
        ),
        "ask_before_receiving" to mapOf(
            Language.ARABIC to "طلب التأكيد قبل استقبال الملفات",
            Language.ENGLISH to "Ask Confirmation Before Receiving",
            Language.FRENCH to "Demander confirmation avant de recevoir",
            Language.SPANISH to "Pedir confirmación antes de recibir"
        ),
        "require_pin" to mapOf(
            Language.ARABIC to "حماية برمز PIN",
            Language.ENGLISH to "Protect with Security PIN",
            Language.FRENCH to "Protéger par code PIN",
            Language.SPANISH to "Proteger con código PIN"
        ),
        "refresh_pin" to mapOf(
            Language.ARABIC to "تجديد رمز PIN",
            Language.ENGLISH to "Regenerate PIN",
            Language.FRENCH to "Régénérer le PIN",
            Language.SPANISH to "Regenerar PIN"
        ),
        "clear_history" to mapOf(
            Language.ARABIC to "مسح السجل بالكامل",
            Language.ENGLISH to "Clear Transfer History",
            Language.FRENCH to "Effacer tout l'historique",
            Language.SPANISH to "Borrar todo el historial"
        ),
        "watch_ad_perk" to mapOf(
            Language.ARABIC to "⭐ مكافأة الجلسة (إعلان اختياري)",
            Language.ENGLISH to "⭐ Session Boost (Optional Reward)",
            Language.FRENCH to "⭐ Boost de session (Optionnel)",
            Language.SPANISH to "⭐ Mejora de sesión (Recompensa opcional)"
        ),
        "premium_version" to mapOf(
            Language.ARABIC to "WiFiDrop Pro",
            Language.ENGLISH to "WiFiDrop Pro",
            Language.FRENCH to "WiFiDrop Pro",
            Language.SPANISH to "WiFiDrop Pro"
        ),
        "incoming_file_request" to mapOf(
            Language.ARABIC to "طلب استقبال ملف جديد",
            Language.ENGLISH to "Incoming File Transfer Request",
            Language.FRENCH to "Demande de transfert de fichier entrant",
            Language.SPANISH to "Solicitud de transferencia de archivo entrante"
        ),
        "accept" to mapOf(
            Language.ARABIC to "قبول",
            Language.ENGLISH to "Accept",
            Language.FRENCH to "Accepter",
            Language.SPANISH to "Aceptar"
        ),
        "decline" to mapOf(
            Language.ARABIC to "رفض",
            Language.ENGLISH to "Decline",
            Language.FRENCH to "Refuser",
            Language.SPANISH to "Rechazar"
        ),
        "copied" to mapOf(
            Language.ARABIC to "تم النسخ للحافظة!",
            Language.ENGLISH to "Copied to clipboard!",
            Language.FRENCH to "Copié dans le presse-papiers!",
            Language.SPANISH to "¡Copiado al portapapeles!"
        )
    )

    fun getString(key: String): String {
        return translations[key]?.get(currentLanguage)
            ?: translations[key]?.get(Language.ENGLISH)
            ?: key
    }
}
