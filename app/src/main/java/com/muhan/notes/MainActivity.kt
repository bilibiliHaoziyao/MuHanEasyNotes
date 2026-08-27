package com.muhan.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.muhan.notes.ui.NotesApp
import com.muhan.notes.ui.theme.MuHanTheme

class MainActivity : ComponentActivity() {

    private val viewModel: NoteViewModel by viewModels { NoteViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MuHanTheme {
                NotesApp(viewModel = viewModel)
            }
        }
    }
}
