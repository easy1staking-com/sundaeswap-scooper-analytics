# SundaeSwap Scooper Analytics

## PSQL

Init local dev psql db

`createuser --superuser postgres`

`psql -U postgres`

Then create db:

```
CREATE USER sundaeswap PASSWORD 'password';

CREATE DATABASE sundaeswap_analytics WITH OWNER sundaeswap;
```

