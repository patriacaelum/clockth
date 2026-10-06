package com.example.clockth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clockth.data.AlarmRepository
import com.example.clockth.ui.editor.AlarmEditorScreen
import com.example.clockth.ui.editor.AlarmEditorViewModel
import com.example.clockth.ui.list.AlarmListScreen
import com.example.clockth.ui.list.AlarmListViewModel
import com.example.clockth.ui.theme.ClockthTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as ClockthApp).container.repository
        setContent {
            ClockthTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ClockthAppNav(repository = repo)
                }
            }
        }
    }
}

@Composable
private fun ClockthAppNav(repository: AlarmRepository) {
    var editorId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editorSession by rememberSaveable { mutableIntStateOf(0) }
    var editing by rememberSaveable { mutableStateOf(false) }

    if (!editing) {
        val listViewModel: AlarmListViewModel = viewModel(
            factory = AlarmListViewModel.factory(repository),
        )
        AlarmListScreen(
            viewModel = listViewModel,
            onAdd = {
                editorId = null
                editorSession += 1
                editing = true
            },
            onEdit = { id ->
                editorId = id
                editorSession += 1
                editing = true
            },
        )
    } else {
        BackHandler { editing = false }
        val editorViewModel: AlarmEditorViewModel = viewModel(
            key = "editor-$editorSession",
            factory = AlarmEditorViewModel.factory(repository, editorId),
        )
        AlarmEditorScreen(
            viewModel = editorViewModel,
            onBack = { editing = false },
        )
    }
}
