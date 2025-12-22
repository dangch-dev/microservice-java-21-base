# Hướng dẫn test STOMP WebSocket bằng Postman

Dùng cho backend realtime (/ws) hoặc qua API Gateway (/ws/realtime/ws). Yêu cầu: gửi đúng frame STOMP (có byte NUL kết thúc), CONNECT trước khi SUBSCRIBE/SEND.

## 1) URL và header
- Nội bộ realtime: `ws://localhost:8084/ws`
- Qua gateway: `ws://localhost:8080/ws/realtime/ws`
- Token (nếu cần): thêm header `Authorization: Bearer <token>` (hoặc query `access_token` khi browser).
- Subprotocol: nếu Postman hỗ trợ, chọn `v12.stomp`.

## 2) Frame CONNECT (bắt buộc, gửi trước)
Chế độ Text (có dòng trống và NUL cuối):
```
CONNECT
accept-version:1.2
host:repo-realtime
heart-beat:0,0

^@   (NUL)
```
Nếu không gõ được NUL, dùng Binary/Hex và dán chuỗi:
```
434f4e4e4543540a6163636570742d76657273696f6e3a312e320a686f73743a7265706f2d7265616c74696d650a68656172742d626561743a302c300a0a00
```
Gửi và đợi thấy `CONNECTED` trong Response trước khi bước tiếp.

## 3) SUBSCRIBE (sau khi đã CONNECTED)
Text:
```
SUBSCRIBE
id:sub-chat
destination:/topic/chat

^@
```
Hex:
```
5355425343524942450a69643a7375622d636861740a64657374696e6174696f6e3a2f746f7069632f636861740a0a00
```

## 4) SEND message (client gửi)
Text:
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
- Mỗi tab/connection phải gửi CONNECT và nhận CONNECTED trước khi SUBSCRIBE/SEND.
- Phải có dòng trống trước byte NUL (kết thúc frame). Byte NUL là `00` trong Hex.
- `/topic/chat` cho phép anonymous; `/topic/notifications` yêu cầu token hợp lệ khi handshake. Bạn có thể thay destination trong SUBSCRIBE/SEND tương ứng.
