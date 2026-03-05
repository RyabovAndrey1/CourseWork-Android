package ru.ryabov.service;

import java.util.List;

import ru.ryabov.dto.CommentCreateDto;
import ru.ryabov.dto.CommentDto;
import ru.ryabov.dto.CommentUpdateDto;

public interface CommentService {
    CommentDto create(Long postId, CommentCreateDto dto, Long currentUserId);
    CommentDto update(Long commentId, CommentUpdateDto dto, Long currentUserId);
    void delete(Long commentId, Long currentUserId);
    List<CommentDto> listByPost(Long postId);
}
