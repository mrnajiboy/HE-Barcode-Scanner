package com.example.hebarcodescanner

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.activity.ComponentActivity

class PresetsActivity : ComponentActivity() {

    private lateinit var btnAddPreset: Button
    private lateinit var listPresets: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private var presets: List<Preset> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_presets)

        btnAddPreset = findViewById(R.id.btnAddPreset)
        listPresets = findViewById(R.id.listPresets)

        btnAddPreset.setOnClickListener {
            val intent = Intent(this, PresetEditActivity::class.java)
            startActivity(intent)
        }

        listPresets.setOnItemClickListener { _, _, position, _ ->
            val p = presets[position]
            val intent = Intent(this, PresetEditActivity::class.java)
            intent.putExtra("preset_id", p.id)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        presets = PresetStore.getAll(this)
        adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            presets.map { it.name }
        )
        listPresets.adapter = adapter
    }
}
