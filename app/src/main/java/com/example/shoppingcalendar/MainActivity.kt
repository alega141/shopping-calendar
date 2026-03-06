package com.example.shoppingcalendar

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ShoppingItem(
    val name: String,
    val quantity: Int,
    val isBought: Boolean
)

class ShoppingViewModel : ViewModel() {
    private val itemsByDate = mutableStateMapOf<LocalDate, List<ShoppingItem>>()

    fun itemsFor(date: LocalDate): List<ShoppingItem> {
        return itemsByDate[date].orEmpty()
    }

    fun addItem(date: LocalDate, rawName: String) {
        val normalized = rawName.trim()
        if (normalized.isEmpty()) return

        val current = itemsByDate[date].orEmpty()
        val index = current.indexOfFirst { it.name.equals(normalized, ignoreCase = true) }

        val updated = if (index >= 0) {
            current.toMutableList().also { list ->
                val old = list[index]
                list[index] = old.copy(quantity = old.quantity + 1)
            }
        } else {
            current + ShoppingItem(name = normalized, quantity = 1, isBought = false)
        }

        itemsByDate[date] = updated
    }

    fun toggleBought(date: LocalDate, itemName: String) {
        val current = itemsByDate[date].orEmpty()
        val updated = current.map { item ->
            if (item.name == itemName) item.copy(isBought = !item.isBought) else item
        }
        itemsByDate[date] = updated
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ShoppingCalendarScreen()
                }
            }
        }
    }
}

@Composable
fun ShoppingCalendarScreen(viewModel: ShoppingViewModel = viewModel()) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var newItemName by remember { mutableStateOf("") }
    val context = LocalContext.current
    val formatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

    val items = viewModel.itemsFor(selectedDate)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Shopping Calendar",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Дата: ${selectedDate.format(formatter)}")
            Button(onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                    },
                    selectedDate.year,
                    selectedDate.monthValue - 1,
                    selectedDate.dayOfMonth
                ).show()
            }) {
                Text("Выбрать день")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = newItemName,
                onValueChange = { newItemName = it },
                label = { Text("Добавить товар") },
                singleLine = true
            )
            Button(onClick = {
                viewModel.addItem(selectedDate, newItemName)
                newItemName = ""
            }) {
                Text("Добавить")
            }
        }

        Text(
            text = if (items.isEmpty()) "Список на день пуст" else "Список покупок",
            style = MaterialTheme.typography.titleMedium
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(items, key = { it.name }) { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "${item.name} ×${item.quantity}")
                    Checkbox(
                        checked = item.isBought,
                        onCheckedChange = { viewModel.toggleBought(selectedDate, item.name) }
                    )
                }
            }
        }
    }
}
