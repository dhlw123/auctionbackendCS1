package com.auction.itemstatus;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auction.itemstatus.dto.ItemStatusGetResponse;

@RestController
@RequestMapping("/item")
public class ItemStatusController {
    public final ItemStatusService itemStatusService;

    public ItemStatusController(ItemStatusService itemStatusService) {
        this.itemStatusService = itemStatusService;
    }

    @GetMapping("/status/{itemId}")
    public ResponseEntity<ItemStatusGetResponse> getItemStatus(@PathVariable Long itemId) {
        ItemStatusGetResponse response = itemStatusService.getStatusResponse(itemId);
        return ResponseEntity.ok().body(response);
    }

    // Development API
    @GetMapping("status/all")
    public ResponseEntity<List<ItemStatus>> getItemStatuses() {
        return ResponseEntity.ok(itemStatusService.getAllItemStatus());

    }
}
