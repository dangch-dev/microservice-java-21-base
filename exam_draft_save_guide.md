# Huong dan FE - API /api/assessment/exams/{examId}/draft/save

## 1) Endpoint
- Method: `POST /api/assessment/exams/{examId}/draft/save`
- Auth: admin role (`ROLE_ADMIN`)
- Response: `ApiResponse<Void>` (success=true, data=null)

## 2) Truoc khi goi
- Draft phai ton tai. Goi `PUT /api/assessment/exams/{examId}/edit` truoc de tao/lay draft.
- Neu draft khong ton tai: tra ve `E420` (422) "Draft exam version does not exist".

## 3) Body request (camelCase)
- Bat buoc co it nhat 1 trong 2: `metadata` hoac `changes`.

```json
{
  "metadata": {
    "name": "Exam A",
    "description": "Cap nhat nhap",
    "durationMinutes": 60,
    "shuffleQuestions": false,
    "shuffleOptions": false
  },
  "changes": [
    {
      "questionId": "q_001",
      "questionOrder": 1,
      "changeType": "ADD",
      "type": "SINGLE_CHOICE",
      "questionContent": { ... },
      "gradingRules": { ... }
    }
  ]
}
```

## 4) ChangeType va truong bat buoc
`changeType` chi nhan 1 gia tri duy nhat.

- `ADD`
  - Bat buoc: `questionId`, `questionOrder`, `type`, `questionContent`, `gradingRules`
- `EDIT`
  - Bat buoc: `questionId`, `type`, `questionContent`, `gradingRules`
  - Co the cap nhat `questionOrder` trong `EDIT`
- `DELETE`
  - Bat buoc: `questionId`

Luu y:
- `questionId` do FE tu sinh, phai duy nhat trong draft.
- `questionOrder` la so nguyen duong.

## 5) Supported question type
- `SINGLE_CHOICE`
- `MULTIPLE_CHOICE`
- `SHORT_TEXT`
- `MATCHING`
- `FILL_BLANKS`
- `ESSAY`
- `FILE_UPLOAD`

`ORDERING` va `TRUE_FALSE` da bi loai bo.

## 6) FileMeta cho files
Moi `files` trong question content dung format sau:

```json
{
  "fileId": "01...",
  "filename": "image.png",
  "mimeType": "image/png",
  "sizeBytes": 12345
}
```

## 7) Quy tac chung
- `schema_version` khong can gui (BE tu set = 1).
- `gradingRules.max_points` neu co thi phai `> 0`.
- `manual` chi ap dung cho `ESSAY` va `FILE_UPLOAD`. Cac type khac se bi bo qua.
- Tong `rubric.max_points` khong duoc vuot `gradingRules.max_points` (neu co).

## 8) Vi du day du theo tung loai

### 8.1) SINGLE_CHOICE
**questionContent**
```json
{
  "prompt": {
    "content": "Chon 1 dap an dung?",
    "files": []
  },
  "options": [
    { "id": "A", "content": "Dap an A", "files": [] },
    { "id": "B", "content": "Dap an B", "files": [] },
    { "id": "C", "content": "Dap an C", "files": [] }
  ]
}
```
**gradingRules**
```json
{
  "max_points": 1,
  "choice": {
    "correct_option_ids": ["A"]
  }
}
```

### 8.2) MULTIPLE_CHOICE
**questionContent**
```json
{
  "prompt": {
    "content": "Chon nhieu dap an dung?",
    "files": []
  },
  "options": [
    { "id": "A", "content": "Dap an A", "files": [] },
    { "id": "B", "content": "Dap an B", "files": [] },
    { "id": "C", "content": "Dap an C", "files": [] }
  ]
}
```
**gradingRules**
```json
{
  "max_points": 2,
  "choice": {
    "correct_option_ids": ["A", "C"]
  }
}
```

### 8.3) SHORT_TEXT
**questionContent**
```json
{
  "prompt": {
    "content": "Viet dap an ngan?",
    "files": []
  }
}
```
**gradingRules**
```json
{
  "max_points": 2,
  "short_text": {
    "match_method": "exact",
    "accepted": ["Ha Noi", "Hanoi"]
  }
}
```
`match_method` chi nhan `exact` hoac `contains`.

### 8.4) MATCHING
**questionContent**
```json
{
  "prompt": {
    "content": "Noi cap dung?",
    "files": []
  },
  "matching": {
    "left_items": [
      { "id": "L1", "content": "Paris", "files": [] },
      { "id": "L2", "content": "Tokyo", "files": [] }
    ],
    "right_items": [
      { "id": "R1", "content": "Phap", "files": [] },
      { "id": "R2", "content": "Nhat Ban", "files": [] }
    ]
  }
}
```
**gradingRules**
```json
{
  "max_points": 2,
  "matching": {
    "pairs": [
      { "left_id": "L1", "right_id": "R1" },
      { "left_id": "L2", "right_id": "R2" }
    ],
    "scheme": "per_pair"
  }
}
```
`scheme` chi nhan `per_pair` hoac `all_or_nothing`.

### 8.5) FILL_BLANKS (text)
**questionContent**
```json
{
  "prompt": {
    "content": "Dien vao cho trong?",
    "files": []
  },
  "blanks": {
    "input_kind": "text",
    "word_bank": []
  }
}
```
**gradingRules**
```json
{
  "max_points": 2,
  "fill_blanks": {
    "blanks": [
      {
        "blank_id": "blank_1",
        "accepted": ["Java"],
        "match_method": "exact"
      },
      {
        "blank_id": "blank_2",
        "accepted": ["Spring"],
        "match_method": "exact"
      }
    ],
    "scheme": "per_pair"
  }
}
```
- `input_kind = text`: `word_bank` neu gui se bi bo qua.
- `correct_option_ids` neu gui se bi bo qua.

### 8.6) FILL_BLANKS (select)
**questionContent**
```json
{
  "prompt": {
    "content": "Chon dap an cho tung o?",
    "files": []
  },
  "blanks": {
    "input_kind": "select",
    "word_bank": [
      { "id": "W1", "content": "Java" },
      { "id": "W2", "content": "Spring" }
    ]
  }
}
```
**gradingRules**
```json
{
  "max_points": 2,
  "fill_blanks": {
    "blanks": [
      { "blank_id": "blank_1", "correct_option_ids": ["W1"] },
      { "blank_id": "blank_2", "correct_option_ids": ["W2"] }
    ],
    "scheme": "per_pair"
  }
}
```
- `input_kind = select`: `accepted`/`match_method` neu gui se bi bo qua.

### 8.7) ESSAY
**questionContent**
```json
{
  "prompt": {
    "content": "Trinh bay ve Java 21?",
    "files": []
  }
}
```
**gradingRules**
```json
{
  "max_points": 5,
  "manual": {
    "auto_mode": false,
    "rubric": [
      { "id": "R1", "label": "Dung y chinh", "max_points": 3 },
      { "id": "R2", "label": "Trinh bay ro", "max_points": 2 }
    ]
  }
}
```

### 8.8) FILE_UPLOAD
**questionContent**
```json
{
  "prompt": {
    "content": "Upload file bai lam?",
    "files": []
  },
  "file_upload": {
    "allowed_mime_types": ["application/pdf"],
    "max_files": 1
  }
}
```
**gradingRules**
```json
{
  "max_points": 5,
  "manual": {
    "auto_mode": false,
    "rubric": [
      { "id": "R1", "label": "Noi dung day du", "max_points": 5 }
    ]
  }
}
```


