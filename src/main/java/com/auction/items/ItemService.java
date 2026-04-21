package com.auction.items;

import org.springframework.stereotype.Service;

import com.auction.items.dto.PublishItemRequest;
import com.auction.items.dto.PublishItemResponse;
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
    public PublishItemResponse publishItem(PublishItemRequest request) {
        User user = userRepository.findByUsername(request.sellerUsername())
                .orElseThrow(() -> new ItemException(false, "There is no seller with that username."));
        Item item = itemRepository.save(new Item(user, request.title()));
        return new PublishItemResponse(true, "Created new item.", item);
    }
}
