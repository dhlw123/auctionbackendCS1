package com.auction.itemstatus;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auction.common.BaseObjectResponse;
import com.auction.items.Item;

/**
 * Service quản lý các nghiệp vụ cập nhật và kiểm tra trạng thái đấu giá của sản phẩm.
 */
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
     * Cập nhật thông tin giá cược cao nhất hiện tại và tài khoản đặt cược.
     * Áp dụng khóa ghi bi quan để tránh việc ghi đè dữ liệu bị xung đột do tranh chấp tài nguyên (race condition).
     *
     * @param item         Thực thể sản phẩm cần cập nhật
     * @param currentPrice Giá cược mới
     * @param username     Tên tài khoản cược mới
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
     * Lấy trạng thái đấu giá của một sản phẩm dưới dạng BaseObjectResponse.
     * Áp dụng khóa ghi để đọc giá trị đồng nhất.
     */
    @Transactional
    public BaseObjectResponse<ItemStatus> getStatusResponse(Long itemId) {
        ItemStatus itemStatus = itemStatusRepository.findByItemWithLockByItemId(itemId);
        return new BaseObjectResponse<>(true, "Succesfully get item status", itemStatus);
    }

    /**
     * Lấy thực thể ItemStatus trực tiếp theo ID sản phẩm, sử dụng khóa ghi bi quan.
     */
    @Transactional
    public ItemStatus getItemStatus(Long itemId) {
        return itemStatusRepository.findByItemWithLockByItemId(itemId);
    }

    /**
     * Lấy toàn bộ danh sách trạng thái sản phẩm đấu giá (chỉ dùng cho mục đích phát triển).
     */
    @Transactional(readOnly = true)
    public List<ItemStatus> getAllItemStatus() {
        return itemStatusRepository.findAll();
    }

    /**
     * Kiểm tra xem phiên đấu giá của sản phẩm đã kết thúc hoặc bị hủy hay chưa.
     * Nếu thời gian kết thúc đã trôi qua so với hiện tại, hệ thống sẽ tự động cập nhật trạng thái thành "ENDED".
     *
     * @param itemId Mã ID sản phẩm đấu giá cần kiểm tra
     * @return true nếu phiên đấu giá đã kết thúc hoặc bị hủy, ngược lại trả về false
     */
    @Transactional
    public boolean auctionEndedOrNot(Long itemId) {
        ItemStatus itemStatus = itemStatusRepository.findByItemWithLockByItemId(itemId);

        // Nếu đã ở trạng thái ENDED hoặc CANCELED thì chắc chắn phiên đấu giá đã dừng
        if (itemStatus.getItemStatus().equals("ENDED") || itemStatus.getItemStatus().equals("CANCELED")) {
            return true;
        }
        // Nếu đã qua thời gian kết thúc dự kiến nhưng trạng thái chưa cập nhật, đổi sang ENDED và lưu lại DB
        else if (itemStatus.getEndTime() < Instant.now().toEpochMilli()) {
            itemStatus.setItemStatus("ENDED");
            itemStatusRepository.save(itemStatus);
            return true;
        }
        // Ngược lại, phiên đấu giá vẫn đang mở
        else {
            return false;
        }
    }
}
