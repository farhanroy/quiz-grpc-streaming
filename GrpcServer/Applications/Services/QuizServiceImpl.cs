using System.Collections.Concurrent;
using Grpc.Core;
using GrpcServer;
using Quiz.Grpc;

namespace GrpcServer.Applications.Services;

public class QuizServiceImpl : QuizService.QuizServiceBase
{
      private static readonly ConcurrentDictionary<string, List<QuizMessage>> MessageStore = new();

    public override async Task ConductQuiz(
        IAsyncStreamReader<QuizMessage> requestStream,
        IServerStreamWriter<QuizMessage> responseStream,
        ServerCallContext context)
    {
        var connectionId = Guid.NewGuid().ToString();
        MessageStore[connectionId] = new List<QuizMessage>();

        Console.WriteLine($"Client connected: {connectionId}");

        // Jalankan dua task paralel:
        var receiveTask = Task.Run(async () =>
        {
            await foreach (var message in requestStream.ReadAllAsync())
            {
                MessageStore[connectionId].Add(message);

                Console.WriteLine($"[RECEIVED] {message.SenderRole}({message.SenderId}) → {message.Type}: {message.Content}");

                // Jika pesan masuk dari mahasiswa, server bisa kirim feedback balik
                if (message.Type == MessageType.Question && message.SenderRole == "dosen")
                {
                    var feedback = new QuizMessage
                    {
                        Type = MessageType.Question,
                        SenderId = "dosen-system",
                        SenderRole = "dosen",
                        QuestionId = message.QuestionId,
                        Content = $"Soal dari {message.SenderId}.",
                        Timestamp = DateTimeOffset.UtcNow.ToUnixTimeSeconds()
                    };

                    await responseStream.WriteAsync(feedback);
                    Console.WriteLine($"[SENT] feedback untuk {message.SenderId}");
                }
            }
        });

        await receiveTask;
        Console.WriteLine($"Client disconnected: {connectionId}");
    }
}
