package com.example.tipcalc

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tipcalc.ui.theme.TipCalcTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TipCalcTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TipCalcScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun TipCalcScreen(modifier: Modifier = Modifier) {
    var orderAmount by rememberSaveable { mutableStateOf("") }
    var dishCount by rememberSaveable { mutableStateOf("") }
    var tipPercent by rememberSaveable { mutableStateOf(0f) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    var showTotal by rememberSaveable { mutableStateOf(false) }


    val discountPercent = calculateDiscountPercent(
        dishCount.toIntOrNull() ?: 0
    )

    val amount = orderAmount.replace(',', '.').toDoubleOrNull()
    val discountAmount = if (
        amount != null && amount.isFinite() && amount >= 0
    ) {
        calculateDiscount(amount, discountPercent)
    } else {
        0.0
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Сумма заказа:",
                    modifier = Modifier.width(140.dp)
                )
                TextField(
                    value = orderAmount,
                    onValueChange = { orderAmount = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    )
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Количество блюд:",
                    modifier = Modifier.width(140.dp)
                )
                TextField(
                    value = dishCount,
                    onValueChange = { value ->
                        if (value.all { it in '0'..'9' }) {
                            dishCount = value
                        }
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )
            }

            Text(text = "Чаевые: ${tipPercent.toInt()}%")

            Slider(
                value = tipPercent,
                onValueChange = { value ->
                    val newPercent = (value / 5f).roundToInt() * 5

                    if (newPercent != tipPercent.toInt()) {
                        tipPercent = newPercent.toFloat()

                        val amount = orderAmount
                            .replace(',', '.')
                            .toDoubleOrNull()

                        snackbarJob?.cancel()
                        snackbarJob = scope.launch {
                            val message = if (
                                amount != null && amount.isFinite() && amount >= 0
                            ) {
                                val tip = calculateTip(amount, newPercent)
                                "Сумма чаевых: %.2f".format(tip)
                            } else {
                                "Введите корректную сумму заказа"
                            }

                            snackbarHostState.showSnackbar(message)
                        }
                    }
                },
                valueRange = 0f..25f,
                steps = 4
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0%")
                Text("25%")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Скидка:")

                listOf(3, 5, 7, 10).forEach { percent ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RadioButton(
                            selected = discountPercent == percent,
                            onClick = null
                        )
                        Text(text = "$percent%")
                    }
                }
            }

            Text(if (showTotal) "Итоговая сумма:" else "Сумма скидки:")

            TextField(
                value = "%.2f".format(
                    if (showTotal) {
                        calculateTotal(
                            orderAmount = amount ?: 0.0,
                            discountPercent = discountPercent,
                            tipPercent = tipPercent.toInt()
                        )
                    } else {
                        discountAmount
                    }
                ),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = { showTotal = true },
                enabled = amount != null &&
                        amount.isFinite() &&
                        amount > 0 &&
                        (dishCount.toIntOrNull() ?: 0) > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Итого")
            }

        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
fun TipCalcPreview() {
    TipCalcTheme {
        TipCalcScreen()
    }
}

fun calculateTip(orderAmount: Double, tipPercent: Int): Double {
    return orderAmount * tipPercent / 100.0
}

fun calculateDiscountPercent(dishCount: Int): Int {
    return when {
        dishCount in 1..2 -> 3
        dishCount in 3..5 -> 5
        dishCount in 6..10 -> 7
        dishCount > 10 -> 10
        else -> 0
    }
}

fun calculateDiscount(orderAmount: Double, discountPercent: Int): Double {
    return orderAmount * discountPercent / 100.0
}

fun calculateTotal(
    orderAmount: Double,
    discountPercent: Int,
    tipPercent: Int
): Double {
    val discount = calculateDiscount(orderAmount, discountPercent)
    val tip = calculateTip(orderAmount, tipPercent)
    return orderAmount - discount + tip
}