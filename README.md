JWT Keys Generation

Before running the application, generate RSA keys.
If keys are missing or invalid, the application will fail to start.

1. Go to project root:
   cd path/to/project

2. Create secrets folder:
   mkdir secrets

3. Generate keys:
   openssl genrsa -out secrets/private.pem 2048
   openssl rsa -in secrets/private.pem -pubout -out secrets/public.pem

4. Result:
   secrets/
      private.pem
      public.pem