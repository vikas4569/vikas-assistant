package com.vikas.assistant

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.SmsManager

sealed class Cmd {
    data class Open(val app: String) : Cmd()
    data class Close(val app: String) : Cmd()
    data class Call(val who: String) : Cmd()
    data class Sms(val who: String, val body: String) : Cmd()
    data class Search(val q: String) : Cmd()
    object Unknown : Cmd()
}

object Parser {
    private val openW = listOf("open", "kholo", "khol do", "start", "खोलो", "खोल दो", "ओपन करो", "ओपन")
    private val closeW = listOf("close", "band karo", "band kar do", "बंद करो", "बंद कर दो", "क्लोज करो", "क्लोज")
    private val callW = listOf("call karo", "call kar do", "call", "कॉल करो", "कॉल कर दो", "कॉल", "phone karo")
    private val searchW = listOf("search karo", "search", "google karo", "सर्च करो", "सर्च", "खोजो")

    private fun strip(s: String, words: List<String>): String {
        var r = s
        for (w in words.sortedByDescending { it.length }) r = r.replace(Regex("(?i)(^|\\s)${Regex.escape(w)}(?=\\s|$)"), " ")
        return r.replace(Regex("\\s+"), " ").trim()
    }
    private fun has(s: String, words: List<String>) = words.any { Regex("(?i)(^|\\s)${Regex.escape(it)}(?=\\s|$)").containsMatchIn(s) }

    fun parse(text: String): Cmd {
        val s = text.trim()
        Regex("(?i)^(?:send\\s+)?(?:message|msg|sms|मैसेज)\\s+(?:to\\s+)?(\\S+)\\s+(?:bhejo\\s+|भेजो\\s+)?(.+)$").find(s)
            ?.let { return Cmd.Sms(it.groupValues[1], it.groupValues[2]) }
        if (has(s, searchW)) return Cmd.Search(strip(s, searchW)).takeIf { it.q.isNotBlank() } ?: Cmd.Unknown
        if (has(s, callW)) return Cmd.Call(strip(s, callW)).takeIf { it.who.isNotBlank() } ?: Cmd.Unknown
        if (has(s, closeW)) return Cmd.Close(strip(s, closeW)).takeIf { it.app.isNotBlank() } ?: Cmd.Unknown
        if (has(s, openW)) return Cmd.Open(strip(s, openW)).takeIf { it.app.isNotBlank() } ?: Cmd.Unknown
        return Cmd.Unknown
    }
}

object Executor {
    data class AppInfo(val label: String, val pkg: String)

    fun findApp(ctx: Context, name: String): AppInfo? {
        val pm = ctx.packageManager
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(i, 0).map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
        val n = name.lowercase().trim()
        return apps.firstOrNull { it.label.lowercase() == n } ?: apps.firstOrNull { it.label.lowercase().contains(n) }
    }

    fun openApp(ctx: Context, name: String): String {
        val app = findApp(ctx, name) ?: return "'$name' naam ka app nahi mila"
        val li = ctx.packageManager.getLaunchIntentForPackage(app.pkg) ?: return "${app.label} open nahi ho sakta"
        ctx.startActivity(li.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return "${app.label} open ho gaya"
    }

    fun closeApp(ctx: Context, name: String): String {
        val app = findApp(ctx, name) ?: return "'$name' naam ka app nahi mila"
        if (app.pkg == ctx.packageName) return "Apne aap ko close nahi karunga"
        return if (AssistantService.forceStop(ctx, app.pkg)) "${app.label} close kar raha hoon..."
        else "Pehle Accessibility service ON karo (button niche hai)"
    }

    fun resolveNumber(ctx: Context, who: String): String? {
        if (who.replace(Regex("[\\s\\-+]"), "").all { it.isDigit() } && who.any { it.isDigit() }) return who.replace(" ", "")
        val c = ctx.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?", arrayOf("%$who%"), null)
        c.use { if (it != null && it.moveToFirst()) return it.getString(0) }
        return null
    }

    fun call(ctx: Context, number: String): String {
        ctx.startActivity(Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return "Call laga raha hoon: $number"
    }

    fun sms(ctx: Context, number: String, body: String): String {
        val sm = ctx.getSystemService(SmsManager::class.java)
        sm.sendMultipartTextMessage(number, null, sm.divideMessage(body), null, null)
        return "Message bhej diya: $number"
    }

    fun search(ctx: Context, q: String): String {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(q))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return "Search: $q"
    }
}
