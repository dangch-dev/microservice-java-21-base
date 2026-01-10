# MinIO Community + Docker – Hướng dẫn sử dụng chuẩn cho Backend

Tài liệu này hướng dẫn **tạo Access Key (user) cho Backend** khi dùng **MinIO Community Edition chạy bằng Docker**.  
Áp dụng cho các hệ thống **upload ảnh / video / attachment / backup**.

---

## 1. Thông tin giả định ban đầu

- Container MinIO tên: `minio`
- Endpoint:
  - Local host: `http://localhost:9000`
  - Trong docker network: `http://minio:9000`
- Root user:
  - User: `minio_root`
  - Password: `minio_root_password`
- Buckets đã tạo:
  - `attachments`
  - `attachments-tmp`

---

## 2. Vào container MinIO

```bash
docker exec -it minio sh
```

## 3. Khai báo alias cho MinIO (login bằng root)
```bash
mc alias set local http://localhost:9000 minio_root minio_root_password
```
Kết quả mong đợi:
```bash
Added `local` successfully.
```

## 4. Tạo user cho Backend (Access Key)
```bash
mc admin user add local app-user app-user-secret
```
Kết quả mong đợi:
```bash
Added `local` successfully.
```
Giải thích:
- `app-user` = Access Key
- `app-user-secret` = Secret Key

## 5. Gán quyền cho user (bắt buộc)
Gán quyền read/write (đơn giản trước)
```bash
mc admin policy attach local readwrite --user app-user
```

## 6. Test user vừa tạo 
Tạo alias bằng user mới
```bash
mc alias set app http://localhost:9000 app-user app-user-secret
```
List bucket
```bash
mc ls app
```
Nếu thấy:
```bash
attachments
attachments-tmp
```

