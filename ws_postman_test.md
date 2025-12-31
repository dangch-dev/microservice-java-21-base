# Hướng dẫn test STOMP WebSocket bằng Postman

Dành cho backend realtime (`/ws`) hoặc qua API Gateway (`/ws/realtime/ws`). Cần gửi đúng frame STOMP (kết thúc bằng byte NUL), CONNECT trước khi SUBSCRIBE/SEND.

## 1) URL và header
- Trực tiếp realtime: `ws://localhost:8084/ws`
- Qua gateway: `ws://localhost:8080/ws/realtime/ws`
- Token (nếu cần): header `Authorization: Bearer <token>` (hoặc query `access_token`).
- Subprotocol: nếu Postman hỗ trợ, chọn `v12.stomp`.

## 2) Frame CONNECT (bắt buộc, gửi trước)
Text (có dòng trống và NUL cuối):
```
CONNECT
accept-version:1.2
host:repo-realtime
heart-beat:0,0

^@   (NUL)
```
Hex nếu cần dán binary:
```
434f4e4e4543540a6163636570742d76657273696f6e3a312e320a686f73743a7265706f2d7265616c74696d650a68656172742d626561743a302c300a0a00
```
Gửi và đợi nhận `CONNECTED` trước khi bước tiếp.

## 3) SUBSCRIBE
Chat public:
```
SUBSCRIBE
id:sub-chat
destination:/topic/chat

^@
```

Notification theo user (server push qua `convertAndSendToUser(..., "/queue/notifications")`):
```
SUBSCRIBE
id:sub-noti
destination:/user/queue/notifications

^@
```
Hex:
```
5355425343524942450a69643a7375622d6e6f74690a64657374696e6174696f6e3a2f757365722f71756575652f6e6f74696669636174696f6e730a0a00
```

UNSUBSCRIBE (khi thoát kênh noti):
```
UNSUBSCRIBE
id:sub-noti

^@
```

Giải thích `/user/queue/notifications`: đây là user-destination. Server gán userId từ token, map ra kênh riêng từng session. Client chỉ subscribe; client không thể gửi message đến user khác qua đích này.

## 4) SEND message (client gửi)
```
SEND
destination:/topic/chat
content-type:text/plain

hello from client A
^@
```
Hex:
```
53454e440a64657374696e6174696f6e3a2f746f7069632f636861740a636f6e74656e742d747970653a746578742f706c61696e0a0a68656c6c6f2066726f6d20636c69656e74204100
```

## Lưu ý
- Mỗi connection phải CONNECT và nhận CONNECTED trước khi SUBSCRIBE/SEND.
- Luôn có dòng trống trước byte NUL (`00` hex).
- `/topic/chat` không yêu cầu auth; `/user/queue/notifications` yêu cầu token hợp lệ và userId khớp với event được push. Client không gửi noti vào đích này; chỉ server push theo user.
- CONNECT: nên giữ `accept-version` (ví dụ 1.2) và `host` (ví dụ repo-realtime). `heart-beat` có thể bỏ; auth header bắt buộc nếu kênh bảo vệ.
- SUBSCRIBE: `id` phải duy nhất và dùng lại khi `UNSUBSCRIBE`; `destination` là kênh server publish; `ack` tùy chọn (mặc định auto).
