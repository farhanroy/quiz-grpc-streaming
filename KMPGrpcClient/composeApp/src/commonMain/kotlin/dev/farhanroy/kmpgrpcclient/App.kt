package dev.farhanroy.kmpgrpcclient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.timortel.kmpgrpc.core.Channel
import io.github.timortel.kmpgrpc.core.metadata.Metadata
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import quiz.Quiz
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
@Preview
fun App() {
    val channel = Channel.Builder
        .forAddress("192.168.1.3", 5056)
        .usePlaintext()
        .build()

    MaterialTheme {
        QuizApp(channel)
    }
}

@Composable
fun QuizApp(channel: Channel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "role_selection") {
        composable("role_selection") {
            RoleSelectionScreen(navController)
        }
        composable("dosen") {
            DosenScreen(channel)
        }
        composable("mahasiswa") {
            MahasiswaScreen(channel)
        }
    }
}

@Composable
fun RoleSelectionScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Pilih Peran Anda",
            fontSize = 24.sp,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        Button(
            onClick = { navController.navigate("dosen") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(text = "Dosen", fontSize = 18.sp)
        }
        Button(
            onClick = { navController.navigate("mahasiswa") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(text = "Mahasiswa", fontSize = 18.sp)
        }
    }
}

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
@Composable
fun DosenScreen(channel: Channel) {
    val scope = rememberCoroutineScope()
    val stub = remember { Quiz.QuizServiceStub(channel) }
    var questionText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<Quiz.QuizMessage>() }
    val userId = remember { Uuid.random().toString() }
    val requestFlow = remember { MutableSharedFlow<Quiz.QuizMessage>(replay = 0, extraBufferCapacity = 10) }

    LaunchedEffect(Unit) {
        val responseFlow = stub.ConductQuiz(requestFlow, Metadata.empty())
        responseFlow.catch { e ->
            messages.add(
                Quiz.QuizMessage(
                    type = Quiz.MessageType.FEEDBACK,
                    sender_id = "system",
                    sender_role = "system",
                    quiz_id = "",
                    question_id = "",
                    content = "Error: ${e.message}",
                    timestamp = Clock.System.now().toEpochMilliseconds(),
                    unknownFields = emptyList()
                )
            )
        }.collect { response ->
            messages.add(response)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Dosen: Buat Kuis",
            fontSize = 24.sp,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = questionText,
            onValueChange = { questionText = it },
            label = { Text("Masukkan Pertanyaan") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                scope.launch {
                    requestFlow.emit(
                        Quiz.QuizMessage(
                            type = Quiz.MessageType.QUESTION,
                            sender_id = userId,
                            sender_role = "dosen",
                            quiz_id = "quiz_${Uuid.random()}",
                            question_id = "q_${Uuid.random()}",
                            content = questionText,
                            timestamp = Clock.System.now().toEpochMilliseconds(),
                            unknownFields = emptyList()
                        )
                    )
                    questionText = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Kirim Pertanyaan")
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(messages) { message ->
                Text(
                    text = "${message.sender_role}: ${message.content} (${message.type})",
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
@Composable
fun MahasiswaScreen(channel: Channel) {
    val scope = rememberCoroutineScope()
    val stub = remember { Quiz.QuizServiceStub(channel) }
    var answerText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<Quiz.QuizMessage>() }
    val userId = remember { Uuid.random().toString() }
    val requestFlow = remember { MutableSharedFlow<Quiz.QuizMessage>(replay = 0, extraBufferCapacity = 10) }

    // Start collecting gRPC responses
    LaunchedEffect(Unit) {
        val responseFlow = stub.ConductQuiz(requestFlow, Metadata.empty())
        responseFlow.catch { e ->
            messages.add(
                Quiz.QuizMessage(
                    type = Quiz.MessageType.FEEDBACK,
                    sender_id = "system",
                    sender_role = "system",
                    quiz_id = "",
                    question_id = "",
                    content = "Error: ${e.message}",
                    timestamp = Clock.System.now().toEpochMilliseconds(),
                    unknownFields = emptyList()
                )
            )
        }.collect { response ->
            messages.add(response)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Mahasiswa: Jawab Kuis",
            fontSize = 24.sp,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Input for answer
        TextField(
            value = answerText,
            onValueChange = { answerText = it },
            label = { Text("Masukkan Jawaban") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Send answer button
        Button(
            onClick = {
                scope.launch {
                    val message = Quiz.QuizMessage(
                        type = Quiz.MessageType.ANSWER,
                        sender_id = userId,
                        sender_role = "mahasiswa",
                        quiz_id = "quiz_${Uuid.random()}",
                        question_id = "q_${Uuid.random()}",
                        content = answerText,
                        timestamp = Clock.System.now().toEpochMilliseconds(),
                        unknownFields = emptyList()
                    )
                    println("Emitting message: $message")
                    requestFlow.emit(message)
                    answerText = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Kirim Jawaban")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Display received messages
        LazyColumn {
            items(messages) { message ->
                Text(
                    text = "${message.sender_role}: ${message.content} (${message.type})",
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}