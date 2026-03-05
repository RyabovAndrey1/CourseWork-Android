package ru.ryabov.mapper;

import ru.ryabov.dto.CommentCreateDto;
import ru.ryabov.dto.CommentDto;
import ru.ryabov.model.Comment;

public final class CommentMapper {
    private CommentMapper() {}

    public static CommentDto toDto(Comment c) {
        if (c == null) return null;
        String display = c.getAuthor() != null ? (c.getAuthor().getDisplayName() != null ? c.getAuthor().getDisplayName() : c.getAuthor().getUsername()) : null;
        return new CommentDto(
                c.getId(),
                c.getPost() != null ? c.getPost().getId() : null,
                c.getAuthor() != null ? c.getAuthor().getId() : null,
                display,
                c.getContent(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }

    public static Comment fromCreateDto(CommentCreateDto dto) {
        if (dto == null) return null;
        Comment c = new Comment();
        c.setContent(dto.content());
        c.setClientId(dto.clientId());
        return c;
    }
}
