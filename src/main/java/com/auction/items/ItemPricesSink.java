package com.auction.items;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Thành phần quản lý luồng dữ liệu phản kháng (Reactive Stream) cho giá của các sản phẩm đấu giá.
 * Hỗ trợ cập nhật và theo dõi biến động giá của từng mặt hàng theo thời gian thực.
 */
@Component
public class ItemPricesSink {

    // Bản đồ lưu trữ các sink phát giá cho từng sản phẩm theo ID, sử dụng ConcurrentHashMap để an toàn đa luồng
    public final Map<Long, Sinks.Many<Double>> itemsPricesMap;

    public ItemPricesSink() {
        itemsPricesMap = new ConcurrentHashMap<Long, Sinks.Many<Double>>();
    }

    /**
     * Phát giá mới của sản phẩm tới tất cả các luồng đang lắng nghe.
     *
     * @param itemId Mã ID sản phẩm
     * @param price  Mức giá mới được cập nhật
     */
    public void publishPrice(Long itemId, Double price) {
        if (itemsPricesMap.containsKey(itemId)) {
            itemsPricesMap.get(itemId).tryEmitNext(price);
        }
    }

    /**
     * Lấy hoặc khởi tạo luồng dữ liệu (Flux) của một sản phẩm để theo dõi biến động giá.
     *
     * @param itemId Mã ID sản phẩm
     * @return Luồng Flux phát giá mới của sản phẩm
     */
    public Flux<Double> getPriceSink(Long itemId) {
        // Lấy sink hiện tại hoặc tạo mới nếu chưa tồn tại
        Sinks.Many<Double> sink = itemsPricesMap.computeIfAbsent(itemId,
                id -> Sinks.many().multicast().onBackpressureBuffer());

        // Trả về luồng Flux và đăng ký giải phóng tài nguyên khi luồng đóng
        return sink.asFlux()
                .doFinally(signalType -> {
                    itemsPricesMap.computeIfPresent(itemId, (id, existingSink) -> {
                        // Nếu không còn subscriber nào lắng nghe thì xóa sink để giải phóng bộ nhớ
                        if (existingSink.currentSubscriberCount() == 0) {
                            return null;
                        }
                        return existingSink;
                    });
                });
    }
}
