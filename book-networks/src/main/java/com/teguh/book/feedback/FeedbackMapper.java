package com.teguh.book.feedback;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.teguh.book.book.Book;

@Service
public class FeedbackMapper {

    public Feedback toFeedback(FeedbackRequest request) {
        return Feedback
                .builder()
                .rating(request.rating())
                .comment(request.comment())
                .book(
                        Book
                                .builder()
                                .id(request.bookId())
                                .archived(false)
                                .shareable(false)
                                .build())
                .build();
    }

    public FeedbackResponse toFeedbackResponse(Feedback feedback, Integer userId) {
        return FeedbackResponse
                .builder()
                .rating(feedback.getRating())
                .command(feedback.getComment())
                .ownFeedback(Objects.equals(feedback.getCreatedBy(), userId))
                .build();
    }

}
