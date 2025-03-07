package com.teguh.book.book;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.teguh.book.common.PageResponse;
import com.teguh.book.exception.OperationNotPermittedException;
import com.teguh.book.file.FileStorageService;
import com.teguh.book.history.BookTransactionHistory;
import com.teguh.book.history.BookTransactionHistoryRepository;
import com.teguh.book.user.User;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookService {
    private final BookMapper bookMapper;
    private final BookRepository bookRepository;
    private final BookTransactionHistoryRepository transactionHistoryRepository;
    private final FileStorageService fileStorageService;

    public Integer save(BookRequest request, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Book book = bookMapper.toBook(request);
        book.setOwner(user);

        return bookRepository.save(book).getId();
    }

    public BookResponse findById(Integer bookId) {
        return bookRepository.findById(bookId)
                .map(bookMapper::toBookResponse)
                .orElseThrow(() -> new EntityNotFoundException("No book found with ID::" + bookId));
    }

    public PageResponse<BookResponse> findAllBooks(int page, int size, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<Book> books = bookRepository.findAllDisplayableBooks(pageable, user.getId());
        List<BookResponse> bookResponse = mapToList(books.stream(), bookMapper::toBookResponse);
        return pageResponse(bookResponse, books);
    }

    public PageResponse<BookResponse> findAllBookByOwner(int page, int size, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<Book> books = bookRepository.findAll(BookSpecification.withOwnerId(user.getId()), pageable);
        List<BookResponse> bookResponse = mapToList(books.stream(), bookMapper::toBookResponse);

        return pageResponse(bookResponse, books);
    }

    public PageResponse<BorrowedBookResponse> findAllBorrowedBooks(
            int page, int size, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<BookTransactionHistory> allBorrowedBooks = transactionHistoryRepository
                .findAllBorrowedBooks(pageable, user.getId());

        List<BorrowedBookResponse> bookResponse = mapToList(allBorrowedBooks.stream(),
                bookMapper::toBorrowedBookResponse);

        return pageResponse(bookResponse, allBorrowedBooks);
    }

    public PageResponse<BorrowedBookResponse> findAllReturnedBooks(
            int page, int size, Authentication connectedUser) {

        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<BookTransactionHistory> allBorrowedBooks = transactionHistoryRepository
                .findAllReturnedBooks(pageable, user.getId());

        List<BorrowedBookResponse> bookResponse = mapToList(allBorrowedBooks.stream(),
                bookMapper::toBorrowedBookResponse);

        return pageResponse(bookResponse, allBorrowedBooks);
    }

    public Integer updateShareableStatus(Integer bookId, Authentication connectedUser) {
        Book book = findBookBy(bookId);

        User user = ((User) connectedUser.getPrincipal());
        if (!isEqualsOwnerAndUser(book, user)) {
            throwOperationNotPermitted("You cannot update other books shareable status");
        }

        book.setShareable(!book.isShareable());
        bookRepository.save(book);
        return bookId;
    }

    public Integer updateArchivedStatus(Integer bookId, Authentication connectedUser) {
        Book book = findBookBy(bookId);
        User user = ((User) connectedUser.getPrincipal());
        if (!isEqualsOwnerAndUser(book, user)) {
            throwOperationNotPermitted("You cannot update other books archived status");
        }
        book.setArchived(!book.isArchived());
        bookRepository.save(book);
        return bookId;
    }

    public Integer borrowBook(Integer bookId, Authentication connectedUser) {
        Book book = findBookBy(bookId);
        User user = ((User) connectedUser.getPrincipal());
        bookArchivedOrNotShareable(book, "The requested book cannot be borrowed since it is archived or not shareable");
        if (isEqualsOwnerAndUser(book, user)) {
            throwOperationNotPermitted("You cannot borrow your own book");
        }

        final boolean isAlreadyBorrowed = transactionHistoryRepository.isAlreadyBorrowedByUser(bookId, user.getId());
        if (isAlreadyBorrowed) {
            throwOperationNotPermitted("The requested book is already borrowed");
        }
        BookTransactionHistory transactionHistory = BookTransactionHistory
                .builder()
                .user(user)
                .book(book)
                .returned(false)
                .returnApproved(false)
                .build();

        return transactionHistoryRepository.save(transactionHistory).getId();
    }

    public Integer returnBorrowedBook(Integer bookId, Authentication connectedUser) {
        Book book = findBookBy(bookId);
        bookArchivedOrNotShareable(book, "The requested book cannot be borrowed since it is archived or not shareable");
        User user = ((User) connectedUser.getPrincipal());
        if (isEqualsOwnerAndUser(book, user)) {
            throwOperationNotPermitted("You cannot borrow or return your own book");
        }

        BookTransactionHistory transactionHistory = transactionHistoryRepository
                .findByBookIdAndUserId(bookId, user.getId())
                .orElseThrow(() -> new OperationNotPermittedException("You did not borrow this book"));

        transactionHistory.setReturned(true);
        return transactionHistoryRepository.save(transactionHistory).getId();
    }

    public Integer approveReturnBorrowedBook(Integer bookId, Authentication connectedUser) {
        Book book = findBookBy(bookId);
        bookArchivedOrNotShareable(book, "The requested book cannot be borrowed since it is archived or not shareable");
        User user = ((User) connectedUser.getPrincipal());
        if (isEqualsOwnerAndUser(book, user)) {
            throwOperationNotPermitted("You cannot borrow or return your own book");
        }

        BookTransactionHistory transactionHistory = transactionHistoryRepository
                .findByBookIdAndOwnerId(bookId, user.getId())
                .orElseThrow(() -> new OperationNotPermittedException(
                        "The Book is not return yet. You cannot approve its return"));

        transactionHistory.setReturnApproved(true);
        return transactionHistoryRepository.save(transactionHistory).getId();
    }

    public void uploadBookCoverPicture(MultipartFile file, Authentication connectedUser, Integer bookId) {
        Book book = findBookBy(bookId);
        User user = ((User) connectedUser.getPrincipal());
        String bookCover = fileStorageService.saveFile(file, user.getId());
        book.setCoverBook(bookCover);
        bookRepository.save(book);
    }

    private <T, P> PageResponse<T> pageResponse(List<T> content, Page<P> page) {
        return new PageResponse<T>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    private Book findBookBy(Integer bookId) {
        return bookRepository.findById(bookId).orElseThrow(
                () -> new EntityNotFoundException("No book found with ID::" + bookId));
    }

    private <T, R> List<R> mapToList(Stream<T> stream, Function<T, R> mapper) {
        return stream.map(mapper).toList();
    }

    private boolean isEqualsOwnerAndUser(Book book, User user) {
        return Objects.equals(book.getOwner().getId(), user.getId());
    }

    private void bookArchivedOrNotShareable(Book book, String message) {
        if (book.isArchived() || !book.isShareable()) {
            throwOperationNotPermitted(message);
        }
    }

    private void throwOperationNotPermitted(String message) {
        throw new OperationNotPermittedException(message);
    }
}
