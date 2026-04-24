package com.auction.items;

import com.auction.common.BaseResponse;
import com.auction.items.dto.BaseItemResponse;
import com.auction.items.dto.GetItemsResponse;
import com.auction.items.dto.PublishItemRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<BaseItemResponse> postItem(@Valid @RequestBody PublishItemRequest request) {

        BaseItemResponse response = itemService.publishItem(request);

        return ResponseEntity.ok().body(response);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<BaseResponse> deleteItem(@PathVariable Long itemId) {
        BaseResponse response = itemService.deleteItem(itemId);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<BaseItemResponse> getItem(@PathVariable Long itemId) {
        BaseItemResponse response = itemService.getItem(itemId);
        return ResponseEntity.ok().body(response);
    }

    // This API is only for development. Remove this in prod.
    @GetMapping("/all")
    public ResponseEntity<GetItemsResponse> getItems() {
        GetItemsResponse response = itemService.getItems();
        return ResponseEntity.ok().body(response);
    }
}
