package ru.ryabov.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import ru.ryabov.dto.CommentCreateDto;
import ru.ryabov.dto.CommentDto;
import ru.ryabov.dto.CommentUpdateDto;
import ru.ryabov.mapper.CommentMapper;
import ru.ryabov.model.Comment;
import ru.ryabov.model.Post;
import ru.ryabov.model.User;
import ru.ryabov.repository.CommentRepository;
import ru.ryabov.repository.PostRepository;
import ru.ryabov.repository.UserRepository;
import ru.ryabov.security.Roles;
import ru.ryabov.service.CommentService;

@Service
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public CommentServiceImpl(CommentRepository commentRepository,
                              PostRepository postRepository,
                              UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public CommentDto create(Long postId, CommentCreateDto dto, Long currentUserId) {
        if (currentUserId == null) throw new AccessDeniedException("Требуется авторизация");
        Post post = postRepository.findById(postId).orElseThrow(() -> new EntityNotFoundException("Post not found: " + postId));
        User author = userRepository.findById(currentUserId).orElseThrow(() -> new EntityNotFoundException("User not found: " + currentUserId));

        // idempotent by clientId if provided
        if (dto.clientId() != null && !dto.clientId().isBlank()) {
            var existing = commentRepository.findByAuthorIdAndClientId(currentUserId, dto.clientId());
            if (existing.isPresent()) {
                Comment ex = existing.get();
                ex.setContent(dto.content());
                Comment saved = commentRepository.save(ex);
                return CommentMapper.toDto(saved);
            }
        }

        Comment comment = CommentMapper.fromCreateDto(dto);
        comment.setPost(post);
        comment.setAuthor(author);

        Comment saved = commentRepository.save(comment);

        // update comments count on post
        if (post.getCommentsCount() == null) post.setCommentsCount(0);
        post.setCommentsCount(post.getCommentsCount() + 1);
        postRepository.save(post);

        return CommentMapper.toDto(saved);
    }

    @Override
    @Transactional
    public CommentDto update(Long commentId, CommentUpdateDto dto, Long currentUserId) {
        Comment existing = commentRepository.findById(commentId).orElseThrow(() -> new EntityNotFoundException("Comment not found: " + commentId));
        if (currentUserId == null) throw new AccessDeniedException("Требуется авторизация");
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new EntityNotFoundException("User not found: " + currentUserId));
        boolean isAdmin = isAdmin(currentUser);
        if (!existing.getAuthor().getId().equals(currentUserId) && !isAdmin) {
            throw new AccessDeniedException("Недостаточно прав для изменения комментария");
        }
        existing.setContent(dto.content());
        Comment saved = commentRepository.save(existing);
        return CommentMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long commentId, Long currentUserId) {
        Comment existing = commentRepository.findById(commentId).orElseThrow(() -> new EntityNotFoundException("Comment not found: " + commentId));
        if (currentUserId == null) throw new AccessDeniedException("Требуется авторизация");
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new EntityNotFoundException("User not found: " + currentUserId));
        boolean isAdmin = isAdmin(currentUser);
        if (!existing.getAuthor().getId().equals(currentUserId) && !isAdmin) {
            throw new AccessDeniedException("Недостаточно прав для удаления комментария");
        }

        Post post = existing.getPost();
        commentRepository.deleteById(commentId);

        if (post != null) {
            Integer cnt = post.getCommentsCount();
            post.setCommentsCount(cnt == null ? 0 : Math.max(0, cnt - 1));
            postRepository.save(post);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> listByPost(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(CommentMapper::toDto)
                .collect(Collectors.toList());
    }

    private boolean isAdmin(User user) {
        return user != null && Roles.ADMIN.equalsIgnoreCase(user.getRole());
    }
}
