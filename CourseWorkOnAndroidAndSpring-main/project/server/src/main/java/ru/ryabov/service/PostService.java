package ru.ryabov.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.ryabov.dto.PostCardDto;
import ru.ryabov.dto.PostCreateDto;
import ru.ryabov.dto.PostFilterRequest;
import ru.ryabov.dto.PostFullDto;
import ru.ryabov.dto.PostUpdateDto;
import ru.ryabov.model.PostStatus;

public interface PostService {
    Page<PostCardDto> getPostsPageByStatus(PostStatus status, Pageable pageable, PostFilterRequest filters);

    PostFullDto getFullPost(Long postId, Long currentUserId);

    PostCardDto create(PostCreateDto dto, Long currentUserId);

    default PostCardDto create(PostCreateDto dto) {
        return create(dto, null);
    }

    PostFullDto update(Long postId, PostUpdateDto dto, Long currentUserId);

    void delete(Long postId, Long currentUserId);

    Page<PostCardDto> getMyDrafts(Long currentUserId, Pageable pageable);
}
