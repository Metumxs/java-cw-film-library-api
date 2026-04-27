## JWT Keys Generation

Before running the application, generate RSA keys.  
If keys are missing or invalid, the application will fail to start.

### 1. Go to project root
```bash
cd path/to/project
```

### 2. Create secrets folder
```bash
mkdir secrets
```

### 3. Generate keys
```bash
openssl genrsa -out secrets/private.pem 2048
openssl rsa -in secrets/private.pem -pubout -out secrets/public.pem
```

### 4. Result
```
secrets/
├── private.pem
└── public.pem
```

---

## Environment Setup

Create a `.env` file based on the provided example:

```bash
cp .env.example .env
```

---

## Run with Docker

### 1. Prerequisites
- `.env` file created
- `secrets/` folder with RSA keys

### 2. Start
```bash
docker compose up --build
```

### 3. Stop
```bash
docker compose down
```

---

## Notes
- Do not commit `.env` or `secrets/`
- JWT key paths must match container paths