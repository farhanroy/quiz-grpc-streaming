using System.Collections.Concurrent;
using Grpc.Core;
using GrpcServer;

namespace GrpcServer.Services;

public class QuizServiceImpl : QuizService.QuizServiceBase
{
    private static readonly List<Question> Questions = new()
    {
        new Question { Id = "Q1", Text = "Apa itu .NET?", Timestamp = DateTimeOffset.UtcNow.ToUnixTimeSeconds() },
        new Question { Id = "Q2", Text = "Apa itu gRPC?", Timestamp = DateTimeOffset.UtcNow.ToUnixTimeSeconds() }
    };

    private static readonly ConcurrentBag<Answer> Answers = new();

    public override async Task StreamQuestions(Empty request, IServerStreamWriter<Question> responseStream, ServerCallContext context)
    {
        foreach (var question in Questions)
        {
            await responseStream.WriteAsync(question);
            await Task.Delay(2000);
        }
    }

    public override async Task<Empty> SubmitAnswers(IAsyncStreamReader<Answer> requestStream, ServerCallContext context)
    {
        await foreach (var answer in requestStream.ReadAllAsync())
        {
            Console.WriteLine($"Jawaban dari {answer.StudentId} untuk {answer.QuestionId}: {answer.AnswerText}");
            Answers.Add(answer);
        }
        return new Empty();
    }
}
