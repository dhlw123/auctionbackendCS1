package com.auction.items;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auction.items.dto.PublishItemRequest;
import com.auction.items.dto.PublishItemResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@SecurityRequirement(name = "bearerAuth")
@RestController // Assumes every method in the the class will return data in the HTTP body
                // (response type application/json)
@RequestMapping("/items")
public class ItemController {
    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping("/")
    public ResponseEntity<PublishItemResponse> postMethodName(@Valid @RequestBody PublishItemRequest request) {

        PublishItemResponse response = itemService.publishItem(request);

        return ResponseEntity.ok().body(response);
    }

}
