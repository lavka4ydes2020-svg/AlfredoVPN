package com.v2ray.ang.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.text.InputType
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.databinding.ActivityDomainBypassBinding
import com.v2ray.ang.extension.toastSuccess
import com.v2ray.ang.handler.MmkvManager
import org.json.JSONArray

class DomainBypassActivity : HelperBaseActivity() {
    private val binding by lazy { ActivityDomainBypassBinding.inflate(layoutInflater) }

    private val domains = mutableListOf<String>()
    private lateinit var adapter: DomainBypassAdapter

    companion object {
        val DEFAULT_DOMAINS = setOf(
            "sberbank.ru", "online.sberbank.ru", "tinkoff.ru", "id.tinkoff.ru",
            "vtb.ru", "alfabank.ru",
            "gosuslugi.ru", "esia.gosuslugi.ru", "nalog.ru", "lkfl2.nalog.ru",
            "mos.ru", "zakupki.gov.ru",
            "mts.ru", "megafon.ru", "beeline.ru", "tele2.ru", "yota.ru",
            "tbank.ru", "sbermobile.ru",
            "rt.ru", "domru.ru", "ttk.ru", "ufanet.ru",
            "vk.com", "ok.ru", "dzen.ru",
            "ya.ru", "yandex.ru", "mail.ru",
            "rutube.ru", "ivi.ru", "kinopoisk.ru",
            "wildberries.ru", "ozon.ru", "market.yandex.ru",
            "hh.ru", "2gis.ru", "disk.yandex.ru", "music.yandex.ru",
            "bitrix24.ru", "domclick.ru", "drom.ru",
            "pochta.ru", "russianpost.ru", "rzd.ru", "avito.ru"
        )

        private val DOMAIN_REGEX by lazy {
            try {
                Regex("^([a-z0-9]([a-z0-9\\-]*[a-z0-9])?\\.)+[a-z]{2,}$")
            } catch (_: Exception) {
                Regex(".") // fallback: matches any non-empty
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentViewWithToolbar(binding.root, showHomeAsUp = true, title = getString(R.string.domain_bypass_section_label))

        loadDomains()
        setupRecyclerView()
        setupFab()
        updateUi()
    }

    override fun onPause() {
        super.onPause()
        saveDomains()
    }

    private fun loadDomains() {
        val stored = MmkvManager.decodeSettingsString(AppConfig.PREF_BYPASS_DOMAINS)
        val initialized = MmkvManager.decodeSettingsBool(AppConfig.PREF_BYPASS_INITIALIZED, false)
        if (stored.isNullOrEmpty() || stored == "[]") {
            domains.addAll(DEFAULT_DOMAINS)
            saveDomains()
            MmkvManager.encodeSettings(AppConfig.PREF_BYPASS_INITIALIZED, true)
        } else {
            val jsonArray = JSONArray(stored)
            domains.clear()
            for (i in 0 until jsonArray.length()) {
                domains.add(jsonArray.getString(i).lowercase())
            }
            if (!initialized) {
                MmkvManager.encodeSettings(AppConfig.PREF_BYPASS_INITIALIZED, true)
            }
        }
    }

    private fun saveDomains() {
        val jsonArray = JSONArray()
        for (domain in domains) {
            jsonArray.put(domain)
        }
        MmkvManager.encodeSettings(AppConfig.PREF_BYPASS_DOMAINS, jsonArray.toString())
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = DomainBypassAdapter(domains) { position ->
            val removed = domains.removeAt(position)
            adapter.notifyItemRemoved(position)
            adapter.notifyItemRangeChanged(position, domains.size)
            updateUi()
            saveDomains()
            toastSuccess(getString(R.string.domain_bypass_deleted, removed.replace("%", "%%")))
        }
        binding.recyclerView.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            showAddDomainDialog()
        }
    }

    private fun showAddDomainDialog() {
        val input = EditText(this).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.MarginLayoutParams.MATCH_PARENT,
                ViewGroup.MarginLayoutParams.WRAP_CONTENT
            )
            setPadding(48, 24, 48, 16)
            textSize = 16f
            hint = getString(R.string.domain_bypass_add_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.domain_bypass_add_dialog_title))
            .setMessage(getString(R.string.domain_bypass_add_dialog_subtitle))
            .setView(input)
            .setPositiveButton(getString(R.string.domain_bypass_add_button)) { _, _ ->
                val domain = input.text.toString().trim().lowercase()
                if (domain.isEmpty() || !isValidDomain(domain)) {
                    Toast.makeText(this, getString(R.string.domain_bypass_invalid), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (domains.contains(domain)) {
                    Toast.makeText(this, getString(R.string.domain_bypass_duplicate), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                domains.add(domain)
                adapter.notifyItemInserted(domains.size - 1)
                binding.recyclerView.post {
                    binding.recyclerView.scrollToPosition(domains.size - 1)
                }
                updateUi()
                saveDomains()
                toastSuccess(getString(R.string.domain_bypass_added, domain.replace("%", "%%")))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun updateUi() {
        val count = domains.size
        binding.tvDomainCount.text = count.toString()
        if (count == 0) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    private fun isValidDomain(s: String): Boolean {
        return DOMAIN_REGEX.matches(s)
    }

    private inner class DomainBypassAdapter(
        private val items: List<String>,
        private val onDelete: (Int) -> Unit
    ) : RecyclerView.Adapter<DomainBypassAdapter.ViewHolder>() {

        private val viewDomainId = View.generateViewId()
        private val viewDeleteId = View.generateViewId()

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvDomain: TextView = view.findViewById(viewDomainId)
            val btnDelete: TextView = view.findViewById(viewDeleteId)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val card = MaterialCardView(parent.context).apply {
                layoutParams = android.view.ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, dpToPx(2), 0, dpToPx(2))
                }
                radius = dpToPx(12).toFloat()
                cardElevation = dpToPx(1).toFloat()
                setContentPadding(dpToPx(12), dpToPx(10), dpToPx(8), dpToPx(10))
                setCardBackgroundColor(0xFFFFFFFF.toInt())
            }

            val row = LinearLayout(card.context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                gravity = Gravity.CENTER_VERTICAL
            }

            val tvDomain = TextView(card.context).apply {
                id = viewDomainId
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
                textSize = 14f
                setTextColor(0xFF111827.toInt())
            }

            val btnDelete = TextView(card.context).apply {
                id = viewDeleteId
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(32),
                    dpToPx(32)
                ).apply {
                    marginStart = dpToPx(8)
                }
                text = "\u2715"
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(0xFFEF4444.toInt())
            }

            row.addView(tvDomain)
            row.addView(btnDelete)
            card.addView(row)

            return ViewHolder(card).also { vh ->
                btnDelete.setOnClickListener {
                    val pos = vh.bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        onDelete(pos)
                    }
                }
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.tvDomain.text = items[position]
        }

        override fun getItemCount(): Int = items.size
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
