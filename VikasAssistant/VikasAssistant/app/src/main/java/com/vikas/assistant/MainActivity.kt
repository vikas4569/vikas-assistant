package com.vikas.assistant

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.view.inputmethod.EditorInfo
import android.widget.*

class MainActivity : Activity() {
    private lateinit var log: TextView
    private lateinit var input: EditText
    private val SPEECH = 1

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad * 2, pad, pad) }
        log = TextView(this).apply {
            textSize = 16f
            text = "Examples:\n• WhatsApp kholo\n• Chrome band karo\n• Rahul ko call karo\n• message Rahul kal milte hain\n• search Python tutorial\n"
        }
        val sv = ScrollView(this).apply { addView(log) }
        input = EditText(this).apply { hint = "Command likho..."; imeOptions = EditorInfo.IME_ACTION_SEND; setSingleLine() }
        val mic = Button(this).apply { text = "🎤 Bolo"; setOnClickListener { listen() } }
        val run = Button(this).apply { text = "Run"; setOnClickListener { handle(input.text.toString()); input.setText("") } }
        val a11y = Button(this).apply { text = "Accessibility ON karo (app close ke liye)"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } }
        input.setOnEditorActionListener { _, _, _ -> run.performClick(); true }
        root.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f))
        listOf(input, mic, run, a11y).forEach { root.addView(it, LinearLayout.LayoutParams(-1, -2)) }
        setContentView(root)
        requestPermissions(arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.SEND_SMS, Manifest.permission.READ_CONTACTS), 0)
    }

    private fun say(s: String) { log.append("\n$s") }
    private fun granted(p: String) = checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED

    private fun listen() {
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
        try { startActivityForResult(i, SPEECH) } catch (e: Exception) { say("⚠️ Speech service nahi mili, text se likho") }
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (req == SPEECH && res == RESULT_OK)
            data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { handle(it) }
    }

    private fun confirm(msg: String, ok: () -> Unit) {
        AlertDialog.Builder(this).setMessage(msg).setPositiveButton("Haan") { _, _ -> ok() }.setNegativeButton("Nahi", null).show()
    }

    private fun handle(text: String) {
        if (text.isBlank()) return
        say("\n🗣 $text")
        when (val c = Parser.parse(text)) {
            is Cmd.Open -> say(Executor.openApp(this, c.app))
            is Cmd.Close -> confirm("${c.app} close karun?") { say(Executor.closeApp(this, c.app)) }
            is Cmd.Search -> say(Executor.search(this, c.q))
            is Cmd.Call -> {
                if (!granted(Manifest.permission.CALL_PHONE)) return say("Call permission do")
                val n = Executor.resolveNumber(this, c.who) ?: return say("'${c.who}' contact nahi mila")
                confirm("${c.who} ($n) ko call karun?") { say(Executor.call(this, n)) }
            }
            is Cmd.Sms -> {
                if (!granted(Manifest.permission.SEND_SMS)) return say("SMS permission do")
                val n = Executor.resolveNumber(this, c.who) ?: return say("'${c.who}' contact nahi mila")
                confirm("${c.who} ($n) ko bhejun:\n\"${c.body}\"?") { say(Executor.sms(this, n, c.body)) }
            }
            Cmd.Unknown -> say("Samajh nahi aaya. Example: 'WhatsApp kholo'")
        }
    }
}
