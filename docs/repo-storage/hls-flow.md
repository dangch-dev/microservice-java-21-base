# HLS Pipeline (repo-storage)

## Summary
- HLS is generated asynchronously after upload.
- Playlist and segments are stored in MinIO under `files/{fileId}/hls/`.
- API returns `202 Accepted` while HLS is not ready.

## Flow
1. `POST /api/storage/file/upload` saves the file metadata and uploads the object.
2. If the file is video, a message is published to Kafka topic `storage.file-hls`.
3. `FileHlsListener` consumes the message and calls `FileHlsService.processHls`.
4. `processHls` runs ffmpeg, uploads:
   - `files/{fileId}/hls/index.m3u8`
   - `files/{fileId}/hls/seg_XXX.ts`
5. `files.hls_status` is updated to `READY` (or `FAILED` on error).

## Read HLS
- `GET /api/storage/file/{id}/hls/index.m3u8`
- `GET /api/storage/file/{id}/hls/{segment}`

If HLS is not ready, these endpoints return `202 Accepted`.
