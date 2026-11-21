package com.wbnbwclll.voiceledger.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wbnbwclll.voiceledger.data.LedgerRepository
import com.wbnbwclll.voiceledger.ui.editor.EditorScreen
import com.wbnbwclll.voiceledger.ui.editor.EditorViewModel
import com.wbnbwclll.voiceledger.ui.ledger.LedgerScreen
import com.wbnbwclll.voiceledger.ui.ledger.LedgerViewModel

private object Routes {
    const val LEDGER = "ledger"
    const val ARG_ENTRY_ID = "entryId"
    const val EDITOR = "editor?entryId={entryId}"

    fun editor(entryId: Long): String = "editor?entryId=$entryId"
}

@Composable
fun LedgerNavHost(repository: LedgerRepository) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LEDGER) {
        composable(Routes.LEDGER) {
            val viewModel: LedgerViewModel = viewModel(factory = LedgerViewModel.factory(repository))
            val state by viewModel.uiState.collectAsState()
            LedgerScreen(
                state = state,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth,
                onKeywordChange = viewModel::onKeywordChange,
                onAddClick = { navController.navigate(Routes.editor(0L)) },
                onEntryClick = { id -> navController.navigate(Routes.editor(id)) },
                onDelete = viewModel::delete
            )
        }

        composable(
            route = Routes.EDITOR,
            arguments = listOf(
                navArgument(Routes.ARG_ENTRY_ID) {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getLong(Routes.ARG_ENTRY_ID) ?: 0L
            val viewModel: EditorViewModel = viewModel(
                factory = EditorViewModel.factory(repository, entryId)
            )
            val state by viewModel.uiState.collectAsState()
            EditorScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onDirectionChange = viewModel::onDirectionChange,
                onAmountChange = viewModel::onAmountChange,
                onCategoryChange = viewModel::onCategoryChange,
                onMerchantChange = viewModel::onMerchantChange,
                onNoteChange = viewModel::onNoteChange,
                onOccurredAtChange = viewModel::onOccurredAtChange,
                onSave = viewModel::save,
                onDelete = viewModel::delete
            )
        }
    }
}
