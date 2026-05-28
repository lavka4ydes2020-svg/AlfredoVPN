package com.v2ray.ang.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsChangeManager

class ServerPickerActivity : AppCompatActivity() {

    private data class ServerItem(
        val guid: String,
        val profile: ProfileItem,
        val flag: String,
        val displayName: String,
        val detail: String
    )

    private val allServers = mutableListOf<ServerItem>()
    private var filteredServers = mutableListOf<ServerItem>()
    private var selectedGuid: String? = null
    private var isRunning = false

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ServerAdapter
    private lateinit var etSearch: android.widget.EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server_picker)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        selectedGuid = MmkvManager.getSelectServer()
        isRunning = intent.getBooleanExtra("isRunning", false)

        loadServers()

        recycler = findViewById(R.id.recycler)
        recycler.layoutManager = LinearLayoutManager(this)

        adapter = ServerAdapter()
        recycler.adapter = adapter

        etSearch = findViewById(R.id.et_search)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        filter("")
    }

    private fun loadServers() {
        val allGuids = MmkvManager.decodeAllServerList()
        for (guid in allGuids) {
            val profile = MmkvManager.decodeServerConfig(guid) ?: continue
            if (profile.configType == EConfigType.POLICYGROUP) continue
            if (profile.remarks.isBlank()) continue

            val countryName = getCountryName(profile.server)
            val displayName = "$countryName — Premium"
            val configTypeName = when (profile.configType) {
                EConfigType.VLESS -> "VLESS+Reality"
                EConfigType.VMESS -> "VMess"
                EConfigType.SHADOWSOCKS -> "Shadowsocks"
                EConfigType.SOCKS -> "SOCKS"
                EConfigType.HTTP -> "HTTP"
                EConfigType.TROJAN -> "Trojan"
                EConfigType.WIREGUARD -> "WireGuard"
                EConfigType.HYSTERIA2 -> "Hysteria2"
                else -> profile.configType.name
            }
            val address = profile.server ?: ""
            val detail = if (address.isNotEmpty()) {
                "$address • $configTypeName"
            } else {
                configTypeName
            }
            val flag = getFlag(profile.server)

            allServers.add(ServerItem(guid, profile, flag, displayName, detail))
        }
    }

    private fun filter(query: String) {
        val q = query.lowercase().trim()
        filteredServers = if (q.isEmpty()) {
            allServers.toMutableList()
        } else {
            allServers.filter {
                it.displayName.lowercase().contains(q) ||
                it.detail.lowercase().contains(q) ||
                it.profile.remarks.lowercase().contains(q)
            }.toMutableList()
        }
        adapter.notifyDataSetChanged()
    }

    private fun selectServer(item: ServerItem) {
        if (item.guid == selectedGuid) {
            finish()
            return
        }
        MmkvManager.setSelectServer(item.guid)
        if (isRunning) {
            SettingsChangeManager.makeRestartService()
        }
        finish()
    }

    // ── Flag/country helpers (mirrors MainActivity) ──

    private fun getFlag(address: String?): String {
        if (address.isNullOrEmpty()) return "🌐"
        return when {
            address.contains("89.127") || address.contains("fornex") -> "🇸🇪"
            address.contains("79.137") || address.contains("aeza") -> "🇩🇪"
            address.contains("ru") || address.contains("mos") -> "🇷🇺"
            address.contains("nl") || address.contains("neth") -> "🇳🇱"
            address.contains("us") || address.contains("united") -> "🇺🇸"
            address.contains("jp") || address.contains("japan") -> "🇯🇵"
            address.contains("sg") || address.contains("singapore") -> "🇸🇬"
            address.contains("uk") || address.contains("london") || address.contains("gb") -> "🇬🇧"
            address.contains("fr") || address.contains("france") -> "🇫🇷"
            else -> "🌐"
        }
    }

    private fun getCountryName(address: String?): String {
        if (address.isNullOrEmpty()) return "Сервер"
        return when {
            address.contains("89.127") || address.contains("fornex") -> "Швеция"
            address.contains("79.137") || address.contains("aeza") -> "Германия"
            address.contains("ru") || address.contains("mos") -> "Россия"
            address.contains("nl") || address.contains("neth") -> "Нидерланды"
            address.contains("us") || address.contains("united") -> "США"
            address.contains("jp") || address.contains("japan") -> "Япония"
            address.contains("sg") || address.contains("singapore") -> "Сингапур"
            address.contains("uk") || address.contains("london") || address.contains("gb") -> "Великобритания"
            address.contains("fr") || address.contains("france") -> "Франция"
            else -> profileRemarksOrElse( address)
        }
    }

    /** Fallback: use profile remarks as display name when IP can't be geolocated. */
    private fun profileRemarksOrElse(address: String): String {
        val s = allServers.firstOrNull { it.profile.server == address }
        return s?.profile?.remarks ?: "Сервер"
    }

    // ── Adapter ──

    private inner class ServerAdapter :
        RecyclerView.Adapter<ServerAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val flag: TextView = view.findViewById(R.id.tv_flag)
            val name: TextView = view.findViewById(R.id.tv_name)
            val detail: TextView = view.findViewById(R.id.tv_detail)
            val ping: TextView = view.findViewById(R.id.tv_ping)
            val check: TextView = view.findViewById(R.id.tv_check)
            val indicator: View = view.findViewById(R.id.indicator)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_server_picker, parent, false)
            return VH(v)
        }

        override fun getItemCount() = filteredServers.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = filteredServers[position]
            val isSelected = item.guid == selectedGuid

            holder.flag.text = item.flag
            holder.name.text = item.displayName
            holder.detail.text = item.detail

            // Ping — try to get from affiliation info
            val aff = MmkvManager.decodeServerAffiliationInfo(item.guid)
            val pingMs = aff?.testDelayMillis ?: 0L
            if (pingMs > 0L) {
                holder.ping.text = "${pingMs} ms"
                holder.ping.visibility = View.VISIBLE
            } else {
                holder.ping.visibility = View.GONE
            }

            // Selection indicator
            if (isSelected) {
                holder.indicator.setBackgroundColor(0xFF059669.toInt())
                holder.check.visibility = View.VISIBLE
                holder.itemView.setBackgroundColor(0x0F059669)
            } else {
                holder.indicator.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                holder.check.visibility = View.GONE
                holder.itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }

            holder.itemView.setOnClickListener { selectServer(item) }
        }
    }
}
