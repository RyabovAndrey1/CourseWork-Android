package ru.ryabov.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import ru.ryabov.dto.IngredientDto;
import ru.ryabov.dto.PaginatedResponse;
import ru.ryabov.mapper.IngredientMapper;
import ru.ryabov.model.Ingredient;
import ru.ryabov.repository.IngredientRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ingredients")
public class IngredientController {

    private final IngredientRepository ingredientRepository;

    public IngredientController(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<IngredientDto>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "30") int pageSize,
            @RequestParam(required = false) String search
    ) {
        int pageIndex = Math.max(page - 1, 0);
        Pageable pageable = PageRequest.of(pageIndex, pageSize);
        Page<Ingredient> ingredients = (search != null && !search.isBlank())
                ? ingredientRepository.findByNameContainingIgnoreCase(search, pageable)
                : ingredientRepository.findAll(pageable);

        List<IngredientDto> results = ingredients.getContent().stream()
                .map(IngredientMapper::toDto)
                .collect(Collectors.toList());

        String next = null;
        if (ingredients.hasNext()) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/api/ingredients")
                    .queryParam("page", page + 1)
                    .queryParam("page_size", pageSize);
            if (search != null && !search.isBlank()) {
                builder.queryParam("search", search);
            }
            next = builder.build().toString();
        }

        return ResponseEntity.ok(new PaginatedResponse<>(results, next));
    }
}
