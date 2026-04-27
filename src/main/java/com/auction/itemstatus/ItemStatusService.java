package com.auction.itemstatus;

import java.util.List;

import org.springframework.stereotype.Service;

import com.auction.items.Item;
import com.auction.items.ItemRepository;
import com.auction.itemstatus.dto.ItemStatusGetResponse;

import jakarta.transaction.Transactional;

@Service
public class ItemStatusService {
    public final ItemStatusRepository itemStatusRepository;
    public final ItemRepository itemRepository;

    public ItemStatusService(ItemStatusRepository itemStatusRepository, ItemRepository itemRepository) {
        this.itemStatusRepository = itemStatusRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public ItemStatus saveStatus(ItemStatus itemStatus) {
        System.out.println("Status Created");
        itemStatusRepository.save(itemStatus);
        return itemStatus;
    }

    @Transactional
    public ItemStatus updateStatus(Item item, Double currentPrice, String username) {
        ItemStatus itemStatus = itemStatusRepository.findByItem(item);
        itemStatus.setCurrentPrice(currentPrice);
        itemStatus.setHighestBidUser(username);
        itemStatusRepository.save(itemStatus);
        return itemStatus;
    }

    @Transactional
    public ItemStatusGetResponse getStatusResponse(Long itemId) {
        ItemStatus itemStatus = itemStatusRepository.findByItem(itemRepository.getReferenceById(itemId));
        return new ItemStatusGetResponse(true, "Succesfully get item status", itemStatus);
    }

    public ItemStatus getItemStatus(Long itemId) {
        return itemStatusRepository.findByItem(itemRepository.getReferenceById(itemId));
    }

    @Transactional
    public List<ItemStatus> getAllItemStatus() {
        return itemStatusRepository.findAll();
    }

}
