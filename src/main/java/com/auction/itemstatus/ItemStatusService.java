package com.auction.itemstatus;

import com.auction.common.BaseObjectResponse;
import com.auction.items.Item;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service quản lý các nghiệp vụ cập nhật và kiểm tra trạng thái đấu giá của sản phẩm. */
@Service
public class ItemStatusService {
    private final ItemStatusRepository itemStatusRepository;

    public ItemStatusService(ItemStatusRepository itemStatusRepository) {
        this.itemStatusRepository = itemStatusRepository;
    }

    /**
     * Lưu thực thể ItemStatus mới vào cơ sở dữ liệu.
     *
     * @param itemStatus Đối tượng trạng thái sản phẩm
     * @return Đối tượng sau khi lưu thành công
     */
    @Transactional
    public ItemStatus saveStatus(ItemStatus itemStatus) {
        itemStatusRepository.save(itemStatus);
        return itemStatus;
    }

    /**
     * Cập nhật thông tin giá cược cao nhất hiện tại và tài khoản đặt cược. Áp dụng khóa ghi bi quan
     * để tránh việc ghi đè dữ liệu bị xung đột do tranh chấp tài nguyên (race condition).
     *
     * @param item Thực thể sản phẩm cần cập nhật
     * @param currentPrice Giá cược mới
     * @param username Tên tài khoản cược mới
     * @return Trạng thái sản phẩm sau khi cập nhật
     */
    @Transactional
    public ItemStatus updateStatus(Item item, Double currentPrice, String username) {
        ItemStatus itemStatus = itemStatusRepository.findByItemWithLock(item);
        itemStatus.setCurrentPrice(currentPrice);
        itemStatus.setHighestBidUser(username);
        itemStatusRepository.save(itemStatus);
        return itemStatus;
    }

    /**
     * Lấy trạng thái đấu giá của một sản phẩm dưới dạng BaseObjectResponse. Áp dụng khóa ghi để đọc
     * giá trị đồng nhất.
     */
    @Transactional
    public BaseObjectResponse<ItemStatus> getStatusResponse(Long itemId) {
        ItemStatus itemStatus = itemStatusRepository.findByItemWithLockByItemId(itemId);
        return new BaseObjectResponse<>(true, "Succesfully get item status", itemStatus);
    }

    /** Lấy thực thể ItemStatus trực tiếp theo ID sản phẩm, sử dụng khóa ghi bi quan. */
    @Transactional
    public ItemStatus getItemStatus(Long itemId) {
        return itemStatusRepository.findByItemWithLockByItemId(itemId);
    }

    /** Lấy toàn bộ danh sách trạng thái sản phẩm đấu giá (chỉ dùng cho mục đích phát triển). */
    @Transactional(readOnly = true)
    public List<ItemStatus> getAllItemStatus() {
        return itemStatusRepository.findAll();
    }


}
