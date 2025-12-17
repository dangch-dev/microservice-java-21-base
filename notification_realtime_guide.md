## Hướng dẫn Notification & Realtime (Phương án A)

### Kiến trúc tổng quát
- **Producer services** (vd: repo-identity-api, tương lai order/chat/...): bắn `NotificationEvent` bằng `NotificationPublisher` (Kafka, topic mặc định `notification.events`, key = `userId`).
- **repo-notification** (consumer group `notification-service`): nhận Kafka, lưu DB, cung cấp REST API list/unread/mark.
- **repo-realtime** (consumer group `realtime-noti`): nhận Kafka, đẩy WebSocket/STOMP tới browser. Không qua Redis.
- **common-framework**: chứa contract `NotificationEvent`, `ResourceType`, `NotificationPublisher` để mọi service tái dùng.

Luồng: Service phát sinh sự kiện → Kafka `notification.events` → repo-notification lưu + trả REST → repo-realtime push WS `/user/queue/notifications`. FE có fallback HTTP (list/unread/mark) và realtime WS song song.

### Contract NotificationEvent
```java
public record NotificationEvent(
    String userId,           // bắt buộc, key Kafka, định tuyến WS
    String topic,            // e.g. "ticket.assigned"
    String title,            // tiêu đề ngắn
    String message,          // mô tả ngắn
    ResourceType resourceType, // enum: TICKET, OTHER (mở rộng thêm tùy domain)
    String resourceId,       // id tài nguyên (FE tự build URL/route)
    Map<String, Object> payload, // dữ liệu phụ (JSON)
    String dedupeKey         // khóa khử trùng lặp; null nếu không cần
)
```
- `dedupeKey` (tối đa 150 ký tự): nếu trùng, repo-notification trả lại bản cũ và không tạo bản mới.
- `resourceId` được ưu tiên hơn việc gửi URL sẵn; FE tự định tuyến.

### Cách publish từ service bất kỳ
1. **Thêm dependency** `repo-common-framework` (đã share từ parent).
2. **Cấu hình Kafka** (bootstrap servers, topic nếu muốn đổi): `notification.publisher.topic`.
3. **Inject và gọi**:
```java
@Service
@RequiredArgsConstructor
public class SampleService {
    private final NotificationPublisher publisher;

    public void notifyUser(String userId, String ticketId) {
        NotificationEvent event = new NotificationEvent(
            userId,
            "ticket.assigned",
            "Ticket assigned",
            "Ticket ABC assigned to you",
            ResourceType.TICKET,
            ticketId,
            Map.of("ticketId", ticketId),
            "ticket:" + ticketId + ":assigned:" + userId
        );
        publisher.publish(event);
    }
}
```

### REST API (repo-notification)
- Header bắt buộc: `X-User-Id` (service gateway set từ token).
- `GET /notifications?page=0&size=20` → danh sách + `unreadCount`.
- `GET /notifications/unread-count` → số chưa đọc.
- `POST /notifications/{id}/seen` → đánh dấu đã xem.
- `POST /notifications/{id}/read` → đánh dấu đã đọc (tự set seen).
- `POST /notifications/seen/all` → đánh dấu đã xem tất cả.

### WebSocket/STOMP (repo-realtime)
- Endpoint: `ws://<host>:8084/ws` (STOMP). Cho phép mọi origin (có thể siết lại khi deploy).
- Subscribe: `/user/queue/notifications`.
- Payload push:
```json
{
  "event": "notification.created",
  "data": { /* NotificationEvent JSON */ }
}
```
Các event khác (từ REST mark) hiện chỉ lưu DB; nếu cần push thêm trạng thái, bổ sung consumer hoặc gửi thêm event qua Kafka.

### Liên kết giữa các service
- **common-framework**: định nghĩa contract + publisher.
- **producer service (vd identity)**: dùng `NotificationPublisher` → Kafka.
- **repo-notification**: consume cùng topic để lưu & cung cấp REST.
- **repo-realtime**: consume cùng topic để đẩy WS tới đúng user.
- FE: kết nối WS để nhận realtime, đồng thời dùng REST để load lịch sử / fallback khi offline.

### Cấu hình mặc định (có thể override)
- Topic: `notification.events`
- Kafka group: `notification-service` (repo-notification), `realtime-noti` (repo-realtime)
- Ports: repo-notification `8083`, repo-realtime `8084`

### Checklist nhanh khi tích hợp service mới
- [ ] Đã khai báo dependency `repo-common-framework`.
- [ ] Đã có cấu hình Kafka (bootstrap servers, topic nếu đổi).
- [ ] Dùng `NotificationPublisher` với `userId` hợp lệ.
- [ ] Gửi `resourceId` thay vì URL; FE tự route.
- [ ] Thiết lập `dedupeKey` nếu cần chống trùng sự kiện.

