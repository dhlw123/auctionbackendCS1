package com.auction.items;

import java.util.List;

import org.springframework.stereotype.Service;

import com.auction.common.BaseResponse;
import com.auction.items.dto.*;
import com.auction.items.exceptions.ItemException;
import com.auction.users.User;
import com.auction.users.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public ItemService(ItemRepository itemRepository, UserRepository userRepository) {
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BaseItemResponse publishItem(PublishItemRequest request) {
        User user = userRepository.findByUsername(request.sellerUsername())
                .orElseThrow(() -> new ItemException(false, "There is no seller with that username."));
        Item item = itemRepository.save(new Item(user, request.title()));
        return new BaseItemResponse(true, "Created new item.", item);
    }

    // Consider if you want to do a Cascading Deletion here
    @Transactional
    public BaseResponse deleteItem(Long itemId) {
        itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemException(false, "There is no Item with that ID"));
        itemRepository.deleteById(itemId);
        return new BaseResponse(true, "Item " + itemId + " was deleted");
    }

    @Transactional
    public BaseItemResponse getItem(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemException(false, "This Item Id does not exist"));
        return new BaseItemResponse(true, "Successfully get Item", item);
    }

    @Transactional
    public GetItemsResponse getItems() {
        List<Item> items = itemRepository.findAll();
        return new GetItemsResponse(true, "Successfully get all items", items);
    }
}
